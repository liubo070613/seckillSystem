package org.example.quickbuy.mq;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SeckillProducer {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static final String SECKILL_TOPIC = "seckill-topic";
    private static final String SECKILL_TAG = "seckill-tag";

    public void sendSeckillMessage(SeckillMessage message) {
        rocketMQTemplate.convertAndSend(SECKILL_TOPIC + ":" + SECKILL_TAG, message);
    }
} 