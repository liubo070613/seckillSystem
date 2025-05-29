package org.example.quickbuy.mq.dlq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.mq.SeckillMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单超时死信队列消费者
 * 处理多次重试失败的消息
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "%DLQ%order-timeout-consumer-group", // 死信队列topic格式
    consumerGroup = "order-timeout-dlq-consumer-group"
)
public class OrderTimeoutDLQConsumer implements RocketMQListener<SeckillMessage> {

    @Override
    public void onMessage(SeckillMessage message) {
        log.error("接收到订单超时死信消息，需要人工处理: {}, 时间: {}", message, LocalDateTime.now());

        // 这里可以进行人工干预处理：
        // 1. 发送告警通知
        // 2. 记录到数据库
        // 3. 发送到监控系统
        // 4. 或者进行特殊的业务处理

        try {
            // 记录到数据库或发送告警
            handleDLQMessage(message);
        } catch (Exception e) {
            log.error("处理死信消息失败: {}", e.getMessage(), e);
        }
    }

    private void handleDLQMessage(SeckillMessage message) {
        // 实现具体的死信处理逻辑
        // 比如：发送钉钉告警、记录到特殊表、发送邮件等
        log.warn("死信消息处理 - 订单号: {}, 用户ID: {}, 活动ID: {}",
            message.getOrderNo(), message.getUserId(), message.getActivityId());
    }
}