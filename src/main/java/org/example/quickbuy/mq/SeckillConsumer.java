package org.example.quickbuy.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
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
        log.info("接收到秒杀消息: {}, 接收时间: {}", message, LocalDateTime.now());
        
        try {
            // 1. 参数校验
            if (message == null) {
                log.error("接收到空消息");
                return;
            }
            
            if (message.getUserId() == null || message.getActivityId() == null) {
                log.error("消息参数不完整: userId={}, activityId={}", 
                    message.getUserId(), message.getActivityId());
                return;
            }

            // 2. 创建订单
            String orderNo = orderService.createOrder(message.getUserId(), message.getActivityId());
            if (orderNo == null || orderNo.isEmpty()) {
                log.error("订单创建失败，返回订单号为空");
                throw new RuntimeException("订单创建失败");
            }
            
            message.setOrderNo(orderNo);
            log.info("订单创建成功: {}", orderNo);

            // 3. 发送订单超时消息（30分钟延时）
            try {
                seckillProducer.sendOrderTimeoutMessage(message, 16); // 16对应30分钟
                log.info("订单超时消息发送成功，订单号: {}", orderNo);
            } catch (Exception e) {
                log.error("发送订单超时消息失败，订单号: {}", orderNo, e);
                // 这里可以考虑是否要抛出异常触发重试
                // 如果订单已创建，但超时消息发送失败，可能需要人工干预
                throw new RuntimeException("发送超时消息失败", e);
            }
            
            log.info("秒杀消息处理完成，订单号: {}", orderNo);
            
        } catch (IllegalArgumentException e) {
            // 参数异常，不重试
            log.error("参数异常，不重试: {}", e.getMessage(), e);
            return;
        } catch (Exception e) {
            log.error("处理秒杀消息失败，消息: {}, 错误: {}", message, e.getMessage(), e);
            // 抛出异常，触发重试机制
            throw new RuntimeException("秒杀消息处理失败: " + e.getMessage(), e);
        }
    }
} 