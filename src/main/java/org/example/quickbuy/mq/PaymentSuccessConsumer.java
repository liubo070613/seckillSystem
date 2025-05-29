package org.example.quickbuy.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "payment-success-topic",
    consumerGroup = "payment-success-consumer-group",
    consumeMode = ConsumeMode.ORDERLY,
    maxReconsumeTimes = 3
)
public class PaymentSuccessConsumer implements RocketMQListener<SeckillMessage> {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Override
    public void onMessage(SeckillMessage message) {

        //todo 更新库存实时的并发问题
        log.info("接收到支付成功消息: {}, 接收时间: {}", message, LocalDateTime.now());
        
        try {
            // 1. 参数校验
            if (message == null) {
                log.error("接收到空消息");
                return;
            }
            
            if (message.getActivityId() == null || message.getStock() == null) {
                log.error("消息参数不完整: activityId={}, stock={}", 
                    message.getActivityId(), message.getStock());
                return;
            }
            
            if (message.getStock() <= 0) {
                log.error("库存数量异常: stock={}", message.getStock());
                return;
            }

            // 2. 更新数据库库存
            int updatedRows = seckillActivityMapper.updateStock(message.getActivityId(), message.getStock());
            
            if (updatedRows == 0) {
                log.warn("库存更新失败，可能活动不存在: activityId={}", message.getActivityId());
                // 这里可以根据业务需求决定是否重试
                // 如果活动不存在，通常不需要重试
                return;
            }
            
            log.info("数据库库存更新成功: activityId={}, 扣减数量={}, 更新行数={}", 
                message.getActivityId(), message.getStock(), updatedRows);
                
        } catch (Exception e) {
            log.error("处理支付成功消息失败，消息: {}, 错误: {}", message, e.getMessage(), e);
            // 抛出异常，触发重试机制
            // 数据库操作失败通常需要重试
            throw new RuntimeException("支付成功消息处理失败: " + e.getMessage(), e);
        }
    }
} 