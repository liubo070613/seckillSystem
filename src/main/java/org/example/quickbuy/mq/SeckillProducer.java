package org.example.quickbuy.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class SeckillProducer {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static final String SECKILL_TOPIC = "seckill-topic";
    private static final String ORDER_TIMEOUT_TOPIC = "order-timeout-topic";
    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";

    /**
     * 发送秒杀消息
     */
    public void sendSeckillMessage(SeckillMessage message) {
        // 生成消息ID
        message.setMessageId(UUID.randomUUID().toString());
        
        Message<SeckillMessage> msg = MessageBuilder.withPayload(message).build();
        rocketMQTemplate.syncSend(SECKILL_TOPIC, msg);
        
        log.info("发送秒杀消息成功: messageId={}, userId={}, activityId={}", 
            message.getMessageId(), message.getUserId(), message.getActivityId());
    }

    /**
     * 发送支付成功消息
     */
    public void sendPaymentSuccessMessage(SeckillMessage message) {
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(PAYMENT_SUCCESS_TOPIC, message);
            System.out.println("发送支付成功消息: " + sendResult);
        } catch (Exception e) {
            System.err.println("发送支付成功消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送订单超时消息（延时消息）
     * @param message 消息内容
     * @param delayLevel 延时级别（1-18）
     *                   1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m, 9=5m, 10=6m,
     *                   11=7m, 12=8m, 13=9m, 14=10m, 15=20m, 16=30m, 17=1h, 18=2h
     */
    public void sendOrderTimeoutMessage(SeckillMessage message, int delayLevel) {
        try {
            Message<SeckillMessage> msg = MessageBuilder
                    .withPayload(message)
                    .build();
            
            SendResult sendResult = rocketMQTemplate.syncSend(ORDER_TIMEOUT_TOPIC, msg,3000, delayLevel);
            System.out.println("发送订单超时消息成功，延时级别: " + delayLevel + ", 结果: " + sendResult);
        } catch (Exception e) {
            System.err.println("发送订单超时消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 发送订单超时消息（指定延时时间，单位：毫秒）
     * @param message 消息内容
     * @param delayTimeMillis 延时时间（毫秒）
     */
    public void sendOrderTimeoutMessageWithDelay(SeckillMessage message, long delayTimeMillis) {
        try {
            SendResult sendResult = rocketMQTemplate.syncSendDelayTimeMills(ORDER_TIMEOUT_TOPIC, message, delayTimeMillis);
            System.out.println("发送订单超时消息成功，延时: " + delayTimeMillis + "ms, 结果: " + sendResult);
        } catch (Exception e) {
            System.err.println("发送订单超时消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 