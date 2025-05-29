//package org.example.quickbuy.mq.dlq;
//
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.example.quickbuy.mq.SeckillMessage;
//import org.example.quickbuy.service.RedisService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
///**
// * 秒杀消息死信队列消费者
// * 处理多次重试失败的秒杀消息
// */
//@Slf4j
//@Component
//@RocketMQMessageListener(
//    topic = "%DLQ%seckill-consumer-group",
//    consumerGroup = "seckill-dlq-consumer-group"
//)
//public class SeckillDLQConsumer implements RocketMQListener<SeckillMessage> {
//
//    @Autowired
//    private RedisService redisService;
//
//    @Override
//    public void onMessage(SeckillMessage message) {
//        log.error("接收到秒杀死信消息，需要人工处理: {}, 时间: {}", message, LocalDateTime.now());
//
//        try {
//            handleSeckillDLQMessage(message);
//        } catch (Exception e) {
//            log.error("处理秒杀死信消息失败: {}", e.getMessage(), e);
//        }
//    }
//
//    private void handleSeckillDLQMessage(SeckillMessage message) {
//        log.warn("秒杀死信消息处理 - 用户ID: {}, 活动ID: {}, 可能需要人工干预创建订单",
//            message.getUserId(), message.getActivityId());
//
//        try {
////            // 1. 恢复用户秒杀资格
////            restoreUserSeckillEligibility(message);
////
////            // 2. 恢复Redis库存
////            restoreRedisStock(message);
//
////            // 3. 记录死信处理日志
////            recordDLQHandling(message);
//
//            // 4. 发送告警通知（这里只是记录日志，实际可以对接钉钉/企微等）
//            sendAlert(message);
//
//        } catch (Exception e) {
//            log.error("死信补偿处理失败: {}", e.getMessage(), e);
//        }
//    }
//
////    /**
////     * 恢复用户秒杀资格
////     */
////    private void restoreUserSeckillEligibility(SeckillMessage message) {
////        try {
////            String userKey = "seckill:user:" + message.getActivityId() + ":" + message.getUserId();
////            redisService.delete(userKey);
////            log.info("恢复用户秒杀资格成功: 用户ID={}, 活动ID={}",
////                message.getUserId(), message.getActivityId());
////        } catch (Exception e) {
////            log.error("恢复用户秒杀资格失败", e);
////        }
////    }
////
////    /**
////     * 恢复Redis库存
////     */
////    private void restoreRedisStock(SeckillMessage message) {
////        try {
////            String stockKey = "seckill:stock:" + message.getActivityId();
////            redisService.increment(stockKey, 1);
////            log.info("恢复Redis库存成功: 活动ID={}, 恢复数量=1", message.getActivityId());
////        } catch (Exception e) {
////            log.error("恢复Redis库存失败", e);
////        }
////    }
//
////    /**
////     * 记录死信处理日志
////     */
////    private void recordDLQHandling(SeckillMessage message) {
////        // TODO: 可以将死信消息记录到专门的表中，便于后续分析和处理
////        log.warn("死信处理记录: 消息={}, 处理时间={}", message, LocalDateTime.now());
////    }
//
//    /**
//     * 发送告警通知
//     */
//    private void sendAlert(SeckillMessage message) {
//        // TODO: 对接告警系统，如钉钉、企微、邮件等
//        log.error("【告警】秒杀消息处理失败需要人工介入: 用户ID={}, 活动ID={}, 时间={}",
//            message.getUserId(), message.getActivityId(), LocalDateTime.now());
//    }
//}