package org.example.quickbuy.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.constant.OrderStatus;
import org.example.quickbuy.service.OrderService;
import org.example.quickbuy.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-timeout-topic",
    consumerGroup = "order-timeout-consumer-group",
    consumeMode = ConsumeMode.CONCURRENTLY, // 并发消费，订单超时消息通常不需要严格顺序
    maxReconsumeTimes = 3 // 最大重试次数
)
public class OrderTimeoutConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisService redisService;

    @Override
    public void onMessage(SeckillMessage message) {
        log.info("接收到订单超时消息: {}, 接收时间: {}", message, LocalDateTime.now());
        
        try {
            // 1. 参数校验
            if (message == null || message.getOrderNo() == null) {
                log.error("消息参数异常: {}", message);
                return; // 参数异常直接返回，不重试
            }

            // 2. 检查订单状态
            Integer orderStatus = orderService.getOrderStatus(message.getOrderNo());
            if (orderStatus == null) {
                log.warn("订单不存在: {}", message.getOrderNo());
                return; // 订单不存在，直接返回
            }
            
            // 3. 如果订单已支付，直接返回
            if (orderStatus.equals(OrderStatus.PAID.getCode())) {
                log.info("订单已支付，无需处理: {}", message.getOrderNo());
                return;
            }

            // 4. 如果订单已取消，直接返回
            if (orderStatus.equals(OrderStatus.CANCELLED.getCode())) {
                log.info("订单已取消，无需处理: {}", message.getOrderNo());
                return;
            }

            // 5. 取消未支付订单
            orderService.cancelOrder(message.getOrderNo());
            log.info("订单取消成功: {}", message.getOrderNo());

            // 6. 回滚Redis库存并恢复用户秒杀资格
            Long stock = redisService.rollbackSeckillStock(
                message.getActivityId(), 
                message.getStock(), 
                message.getUserId()
            );
            
            if (stock == -2) {
                log.warn("活动不存在，无法回滚库存: {}", message.getActivityId());
            } else {
                log.info("redis库存回滚成功，活动ID: {}, 回滚数量: {}, 当前库存: {}",
                    message.getActivityId(), message.getStock(), stock);
            }
            
            log.info("订单超时处理完成: {}", message.getOrderNo());
            
        } catch (Exception e) {
            log.error("处理订单超时消息失败，消息: {}, 错误: {}", message, e.getMessage(), e);
            // 抛出异常，触发重试机制
            throw new RuntimeException("订单超时处理失败: " + e.getMessage(), e);
        }
    }
} 