package org.example.quickbuy.mq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.example.quickbuy.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "seckill-topic",
    consumerGroup = "seckill-consumer-group"
)
public class SeckillConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private SeckillProducer seckillProducer;

    @Override
    public void onMessage(SeckillMessage message) {
        try {
            // 1. 创建订单
            String orderNo = orderService.createOrder(message.getUserId(), message.getActivityId());
            message.setOrderNo(orderNo);

            // 2. 发送15分钟延时消息，用于订单超时取消
            // 延时级别15对应20分钟，这里使用14对应10分钟，实际项目中应该使用15
            seckillProducer.sendOrderTimeoutMessage(message, 14);
        } catch (Exception e) {
            // 处理异常，可以考虑重试或记录日志
            e.printStackTrace();
        }
    }
} 