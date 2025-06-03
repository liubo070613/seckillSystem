# 秒杀系统实战

## 缓存

数据库中有商品表，活动表（搞秒杀活动的商品），订单表，用户表

将活动商品的信息以及库存缓存到`redis`中，缓存预热，秒杀活动开启时将其加入到`redis`中

```java
public static final String SECKILL_ACTIVITY_KEY = "seckill:activity:%s";
public static final String SECKILL_STOCK_KEY = "seckill:stock:%s";
```

## 秒杀过程

通过`用户id`和`活动id`请求[秒杀服务](src/main/java/org/example/quickbuy/service/impl/SeckillServiceImpl.java)

### 第一步，获取活动信息

在这个为了避免大量脏请求导致缓存穿透问题引入了[布隆过滤器](src/main/java/org/example/quickbuy/util/BloomFilter.java)，先经过布隆过滤器

然后查`redis`缓存，若命中直接返回，若没有命中则需要重建缓存。

重建缓存可能会导致缓存击穿的问题，故需要引入分布式锁让获取到锁的线程去重建缓存

分布式锁的实现[DistributedLockManager](src/main/java/org/example/quickbuy/service/DistributedLockManager.java),使用`redis`的`setnx+expire`，同时实现了看门狗机制

```java
private SeckillActivity getActivity(Long activityId) {
  // 1. 先检查布隆过滤器
  if (!bloomFilter.mightContain(ACTIVITY_BLOOM_FILTER_KEY, String.valueOf(activityId))) {
      return null;
  }

  // 2. 查Redis缓存
  SeckillActivity activity = redisService.getSeckillActivity(activityId);
  if (activity != null) {
      return activity;
  }

  // 3. 缓存未命中，尝试获取分布式锁
  String lockKey = redisService.getActivityLockKey(activityId);
  String requestId = UUID.randomUUID().toString();

  try {
      // 尝试获取锁，等待100ms，锁过期时间10s
      boolean locked = redisService.tryLock(lockKey, requestId, 10, TimeUnit.SECONDS);
      if (!locked) {
          // 获取锁失败，说明其他线程正在重建缓存，等待100ms后重试
          Thread.sleep(100);
          return getActivity(activityId);
      }

      // 4. 获取锁成功，再次检查缓存（双重检查）
      activity = redisService.getSeckillActivity(activityId);
      if (activity != null) {
          return activity;
      }

      // 5. 查询数据库
      activity = seckillActivityMapper.selectById(activityId);
      if (activity != null) {
          // 6. 重建缓存
          redisService.cacheSeckillActivity(activity);
          redisService.cacheSeckillStock(activity.getId(), activity.getStock());
      }

      return activity;

  } catch (Exception e) {
      log.error("获取活动信息异常: activityId={}", activityId, e);
      // 发生异常时，直接查询数据库
      return seckillActivityMapper.selectById(activityId);
  } finally {
      // 8. 释放锁
      try {
          redisService.releaseLock(lockKey, requestId);
      } catch (Exception e) {
          log.error("释放分布式锁异常: lockKey={}, requestId={}", lockKey, requestId, e);
      }
  }
}
```

需要注意的点：

- 双重检查：查询缓存，没有命中尝试获取锁，获取到锁再次查找缓存，还是没有命中才去查询数据库重建缓存，可以避免重复重建缓存。

- 使用过期时间防止死锁，同时使用看门狗机制给锁续期，避免提前释放锁

- 使用UUID作为请求标识，作为分布式锁的`value`

- 释放锁和给锁续期时需要先判断分布式锁的`value`与请求标识是否相等再做具体操作，需要使用`lua`脚本保证原子性

### 第二步，检查活动的状态

正在活动中商品才可以秒杀

### 第三步，执行秒杀

- 使用`redis`实现预减库存以及并发安全，`redis`是单线程执行的。实际库存扣减在支付之后。
- 使用`redis`缓存用户`key（seckill:user:activityId:userId)`，实现一人一单。

 `seckill.lua`的内容

```lua
-- 获取库存
local stockKey = string.format("seckill:stock:%s", KEYS[1])
local stock = redis.call('get', stockKey)
-- 如果key不存在，返回-2表示活动不存在
if not stock then
    return -2
end

-- 检查用户是否已获得秒杀资格
local userKey = string.format("seckill:user:%s:%s", KEYS[1], ARGV[1])
if redis.call('exists', userKey) == 1 then
    return -3  -- 返回-3表示重复秒杀
end

-- 转换为数字
stock = tonumber(stock)
-- 如果库存小于等于0，返回-1表示库存不足
if stock <= 0 then
    return -1
end

-- 扣减库存
redis.call('decr', stockKey)
-- 记录用户秒杀资格
redis.call('set', userKey, 1, 'EX', 86400)  -- 设置24小时过期
-- 返回剩余库存
return stock - 1 
```

 秒杀逻辑为：先获取活动商品库存，若还有库存，检查是否有秒杀资格，若有则扣减库存，同时记录用户秒杀资格。

### 第四步，发送秒杀消息

集成`rocketmq`实现异步下单，把消息发送到`SECKILL_TOPIC`，[seckillProducer](src/main/java/org/example/quickbuy/mq/SeckillProducer.java)封装了发送消息的方法

```java
SeckillMessage message = new SeckillMessage();
message.setUserId(userId);
message.setActivityId(activity.getId());
seckillProducer.sendSeckillMessage(message);
```

消息记录`用户id`，`活动id`

到这一步就会秒杀就结束了



## 异步下单

创建一个消费者监听`SECKILL_TOPIC`，消费秒杀消息

```java
@Component
@RocketMQMessageListener(
    topic = "seckill-topic",
    consumerGroup = "seckill-consumer-group",
    consumeMode = ConsumeMode.ORDERLY,
    maxReconsumeTimes = 3
)
public class SeckillConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SeckillProducer seckillProducer;

    @Override
    public void onMessage(SeckillMessage message) {
     
      // 3. 创建订单
      String orderNo = orderService.createOrder(message.getUserId(), message.getActivityId());

      // 4. 发送订单超时消息（30分钟延时）
      seckillProducer.sendOrderTimeoutMessage(message, 16);
           
}
    
```

在这里，消费者消费秒杀消息去创建订单，订单的状态是待支付。随后发送一个延时消息到`ORDER_TIMEOUT_TOPIC`实现订单的超时自动取消



## 异步更新库存

用户支付之后会往`PAYMENT_SUCCESS_TOPIC`中发送消息，消费者监听这个`topic`实现异步更新库存

使用乐观锁来更新库存，通过`redis`预减库存，实际下单和更新库存的数量并不大，故可以使用乐观锁来实现。



## 订单超时自动取消

消费者监听`order-timeout-topic`，如果监听到超时消息，则判断订单的状态，若状态为待支付则取消订单，同时需要回滚`redis`中的库存以及用户的秒杀资格。否则无需处理。`lua`脚本见[rollback_stock.lua](src/main/resources/scripts/rollback_stock.lua)



## RocketMQ使用中的一些问题

### 重试队列机制

当消费者处理消息失败并抛出异常时，`RocketMQ`会：

1. 第一步：将消息发送到重试队列，重试队列无需自己定义

   - 重试队列名称：`%RETRY%{ConsumerGroup}`
   
   
      - 例如：`%RETRY%order-timeout-consumer-group`
   

2. 第二步：根据重试次数设置延时

   ```
      重试次数    延时间隔
      1          10秒
      2          30秒  
      3          1分钟
      4          2分钟
      5          3分钟
      6          4分钟
      ...
      16         2小时（最大间隔）
   ```

3. 第三步：到达延时时间后，重新投递消息

**重试过程详解**

```java
@RocketMQMessageListener(
    topic = "order-timeout-topic",
    consumerGroup = "order-timeout-consumer-group",
    consumeMode = ConsumeMode.CONCURRENTLY, // 并发消费，订单超时消息通常不需要严格顺序
    maxReconsumeTimes = 3 // 最大重试次数
)
```

假设有一条消息处理失败：

1. 第1次失败：消息进入 `%RETRY%order-timeout-consumer-group`，10秒后重新投递
2. 第2次失败：再次进入重试队列，30秒后重新投递
3. 第3次失败：再次进入重试队列，1分钟后重新投递
4. 第4次失败（超过`maxReconsumeTimes=3`）：消息进入死信队列 `%DLQ%order-timeout-consumer-group`，不再自动重试。

```java
@RocketMQMessageListener(
    topic = "%DLQ%order-timeout-consumer-group", // 死信队列topic格式
    consumerGroup = "order-timeout-dlq-consumer-group"
)
```

可以创建一个消费者监听死信队列，当监听有消息到来时，应发送告警通知相关人员，把死信消息保存到数据库，便于后续的分析和处理。



### 消息幂等性

这样，我们就实现了一个完整的消息幂等性处理机制。主要特点：

消息ID机制：每条消息都有唯一的ID，使用UUID生成消息ID，消息ID用于幂等性检查

幂等性检查：使用Redis的SETNX命令，设置合理的过期时间，异常时认为该消息已被处理
