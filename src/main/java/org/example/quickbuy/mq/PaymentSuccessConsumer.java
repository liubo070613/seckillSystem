package org.example.quickbuy.mq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "payment-success-topic",
    consumerGroup = "payment-success-consumer-group"
)
public class PaymentSuccessConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Override
    public void onMessage(SeckillMessage message) {
        try {
            //todo: 是否有安全性问题
            // 支付成功后更新数据库库存
            seckillActivityMapper.updateStock(message.getActivityId(), message.getStock());
        } catch (Exception e) {
            // 处理异常，可以考虑重试或记录日志
            e.printStackTrace();
        }
    }
} 