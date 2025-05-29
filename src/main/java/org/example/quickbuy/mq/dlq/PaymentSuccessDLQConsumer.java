package org.example.quickbuy.mq.dlq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.example.quickbuy.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 支付成功消息死信队列消费者
 * 处理多次重试失败的支付成功消息
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "%DLQ%payment-success-consumer-group",
    consumerGroup = "payment-success-dlq-consumer-group"
)
public class PaymentSuccessDLQConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private RedisService redisService;

    @Override
    public void onMessage(SeckillMessage message) {
        log.error("接收到支付成功死信消息，需要人工处理: {}, 时间: {}", message, LocalDateTime.now());

        try {
            handlePaymentSuccessDLQMessage(message);
        } catch (Exception e) {
            log.error("处理支付成功死信消息失败: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentSuccessDLQMessage(SeckillMessage message) {
        log.warn("支付成功死信消息处理 - 订单号: {}, 活动ID: {}, 库存数量: {}, 需要人工核对数据库库存",
            message.getOrderNo(), message.getActivityId(), message.getStock());

        try {

//            // 3. 记录死信处理日志
//            recordDLQHandling(message);

            // 4. 发送告警通知
            sendAlert(message);

        } catch (Exception e) {
            log.error("支付成功死信补偿处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录死信处理日志
     */
    private void recordDLQHandling(SeckillMessage message) {
        // TODO: 可以将死信消息记录到专门的表中，便于后续分析和处理
        log.warn("支付成功死信处理记录: 消息={}, 处理时间={}", message, LocalDateTime.now());
    }

    /**
     * 发送告警通知
     */
    private void sendAlert(SeckillMessage message) {
        // TODO: 对接告警系统，如钉钉、企微、邮件等
        log.error("【告警】支付成功消息处理失败需要人工介入: 订单号={}, 活动ID={}, 时间={}",
            message.getOrderNo(), message.getActivityId(), LocalDateTime.now());
    }
}