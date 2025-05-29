package org.example.quickbuy.mq;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.example.quickbuy.service.LocalMessageService;
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

    @Autowired
    private LocalMessageService localMessageService;

    private static final String SECKILL_TOPIC = "seckill-topic";
    private static final String ORDER_TIMEOUT_TOPIC = "order-timeout-topic";
    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success-topic";

    /**
     * 通用消息发送方法
     *
     * @param message    消息内容
     * @param topic      消息主题
     * @param delayLevel 延时级别（可选，默认0表示非延时消息）
     */
    private void sendMessage(SeckillMessage message, String topic, int delayLevel) {

        boolean isRetry = message.getMessageId() != null;
        try {

            // 1. 生成消息ID（非重试消息才生成新ID）
            if (!isRetry) {
                message.setMessageId(UUID.randomUUID().toString());
            }

            // 2. 构建消息
            Message<SeckillMessage> msg = MessageBuilder
                    .withPayload(message)
                    .setHeader("messageId", message.getMessageId())
                    .build();

            // 3. 同步发送，等待发送结果
            SendResult sendResult;
            if (delayLevel > 0) {
                sendResult = rocketMQTemplate.syncSend(topic, msg, 3000, delayLevel);
            } else {
                sendResult = rocketMQTemplate.syncSend(topic, msg, 3000);
            }

            // 4. 检查发送结果
            if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                throw new RuntimeException("消息发送失败: " + sendResult.getSendStatus());
            }

            // 5. 记录到本地消息表（非重试消息才记录）
            if (!isRetry) {
                localMessageService.saveMessage(message, topic, "SENT");
            }

            log.info("发送消息成功: messageId={}, topic={}, isRetry={}",
                    message.getMessageId(), topic, isRetry);

        } catch (Exception e) {
            log.error("发送消息异常: messageId={}, topic={}", message.getMessageId(), topic, e);
            // 6. 记录失败消息（非重试消息才记录）
            if (!isRetry) {
                localMessageService.saveMessage(message, topic, "FAILED");
            }
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 发送秒杀消息
     */
    public void sendSeckillMessage(SeckillMessage message) {
        sendMessage(message, SECKILL_TOPIC, 0);
    }

    /**
     * 发送支付成功消息
     */
    public void sendPaymentSuccessMessage(SeckillMessage message) {
        sendMessage(message, PAYMENT_SUCCESS_TOPIC, 0);
    }

    /**
     * 发送订单超时消息（延时消息）
     *
     * @param message    消息内容
     * @param delayLevel 延时级别（1-18）
     *                   1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m, 9=5m, 10=6m,
     *                   11=7m, 12=8m, 13=9m, 14=10m, 15=20m, 16=30m, 17=1h, 18=2h
     */
    public void sendOrderTimeoutMessage(SeckillMessage message, int delayLevel) {
        sendMessage(message, ORDER_TIMEOUT_TOPIC, delayLevel);
    }

    /**
     * 发送订单超时消息（指定延时时间，单位：毫秒）
     *
     * @param message         消息内容
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