package org.example.quickbuy.mq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "order-timeout-topic",
    consumerGroup = "order-timeout-consumer-group"
)
public class OrderTimeoutConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String SECKILL_STOCK_KEY = "seckill:stock:";

    @Override
    public void onMessage(SeckillMessage message) {
        try {
            // 1. 检查订单状态，如果未支付则取消订单
            orderService.cancelOrder(message.getOrderNo());

            // 2. 回滚Redis库存
            String stockKey = SECKILL_STOCK_KEY + message.getActivityId();
            redisTemplate.opsForValue().increment(stockKey, message.getStock());
        } catch (Exception e) {
            // 处理异常，可以考虑重试或记录日志
            e.printStackTrace();
        }
    }
} 