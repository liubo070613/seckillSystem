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
    consumerGroup = "seckill-consumer-group",
    selectorExpression = "seckill-tag"
)
public class SeckillConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Override
    public void onMessage(SeckillMessage message) {
        try {
            // 1. 创建订单
            String orderNo = orderService.createOrder(message.getUserId(), message.getActivityId());
            message.setOrderNo(orderNo);

            // 2. 等待支付结果
            boolean paid = orderService.waitForPayment(orderNo);
            if (paid) {
                // 3. 支付成功后更新库存
                //todo 怎么保证并发安全
                seckillActivityMapper.updateStock(message.getActivityId(), message.getStock());
            } else {
                // 4. 支付失败，回滚库存
                // TODO: 实现redis库存回滚逻辑
            }
        } catch (Exception e) {
            // 处理异常，可以考虑重试或记录日志
            e.printStackTrace();
        }
    }
} 