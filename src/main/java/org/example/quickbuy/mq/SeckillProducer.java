package org.example.quickbuy.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        rocketMQTemplate.convertAndSend(SECKILL_TOPIC, message);
    }

    /**
     * 发送订单超时消息
     * @param message 秒杀消息
     * @param delayLevel 延时级别（1-18，对应1s, 5s, 10s, 30s, 1m, 2m, 3m, 4m, 5m, 6m, 7m, 8m, 9m, 10m, 20m, 30m, 1h, 2h）
     */
    public void sendOrderTimeoutMessage(SeckillMessage message, int delayLevel) {
        rocketMQTemplate.syncSendDelayTimeLevel(ORDER_TIMEOUT_TOPIC, message, delayLevel);
    }

    /**
     * 发送支付成功消息
     */
    public void sendPaymentSuccessMessage(SeckillMessage message) {
        rocketMQTemplate.convertAndSend(PAYMENT_SUCCESS_TOPIC, message);
    }
} 