package org.example.quickbuy.mq;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.constant.OrderStatus;
import org.example.quickbuy.service.OrderService;
import org.example.quickbuy.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RocketMQMessageListener(
    topic = "order-timeout-topic",
    consumerGroup = "order-timeout-consumer-group"
)
public class OrderTimeoutConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisService redisService;

    @Override
    public void onMessage(SeckillMessage message) {
        try {
            // 1. 检查订单状态
            Integer orderStatus = orderService.getOrderStatus(message.getOrderNo());
            if (orderStatus == null) {
                System.out.println("订单不存在: " + message.getOrderNo());
                return;
            }

            // 2. 如果订单已支付，直接返回
            if (orderStatus == OrderStatus.PAID.getCode()) {
                System.out.println("订单已支付，无需处理: " + message.getOrderNo());
                return;
            }

            // 3. 取消未支付订单
            orderService.cancelOrder(message.getOrderNo());

            // 4. 回滚Redis库存并恢复用户秒杀资格
            Long stock = redisService.rollbackSeckillStock(message.getActivityId(), message.getStock(), message.getUserId());
            if (stock == -2) {
                System.out.println("活动不存在，无法回滚库存: " + message.getActivityId());
            }
        } catch (Exception e) {
            // 处理异常，可以考虑重试或记录日志
            e.printStackTrace();
        }
    }
} 