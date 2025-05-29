package org.example.quickbuy.task;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.example.quickbuy.entity.LocalMessage;
import org.example.quickbuy.mapper.LocalMessageMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.example.quickbuy.mq.SeckillProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class MessageRetryTask {
    
    private static final int MAX_RETRY_TIMES = 3; // 最大重试次数
    
    @Autowired
    private LocalMessageMapper localMessageMapper;
    
    @Autowired
    private SeckillProducer seckillProducer;

    @Autowired
    private MessageCompensationTask  messageCompensationTask;
    
    /**
     * 定时任务：重试发送失败的消息
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void retryFailedMessages() {
        try {
            // 查询需要重试的消息
            List<LocalMessage> messages = localMessageMapper.selectFailedMessages();
            
            for (LocalMessage message : messages) {
                try {
                    // 检查重试次数
                    if (message.getRetryTimes() >= MAX_RETRY_TIMES) {
                        // 超过最大重试次数，标记为最终失败
                        localMessageMapper.updateStatus(message.getMessageId(), "FINAL_FAILED", new Date());
                        SeckillMessage seckillMessage = JSON.parseObject(message.getContent(), SeckillMessage.class);
                        messageCompensationTask.compensateSeckillMessage(seckillMessage);
                        log.error("消息重试次数超过限制，标记为最终失败: messageId={}, retryTimes={}", 
                            message.getMessageId(), message.getRetryTimes());
                        continue;
                    }
                    
                    // 重试发送消息
                    retrySendMessage(message);
                } catch (Exception e) {
                    log.error("重试发送消息失败: messageId={}", message.getMessageId(), e);
                }
            }
        } catch (Exception e) {
            log.error("重试失败消息异常", e);
        }
    }
    
    /**
     * 重试发送消息
     */
    private void retrySendMessage(LocalMessage message) {
        try {
            // 解析消息内容
            SeckillMessage seckillMessage = JSON.parseObject(message.getContent(), SeckillMessage.class);
            
            // 更新消息状态为重试中
            localMessageMapper.updateStatus(message.getMessageId(), "RETRYING", new Date());
            
            // 增加重试次数
            localMessageMapper.incrementRetryTimes(message.getMessageId());
            
            // 根据topic调用不同的发送方法
            switch (message.getTopic()) {
                case "seckill-topic":
                    seckillProducer.sendSeckillMessage(seckillMessage);
                    break;
                case "order-timeout-topic":
                    // 如果有订单相关的消息生产者，在这里调用
                    // orderProducer.sendOrderMessage(orderMessage);
                    break;
                case "payment-success-topic":
                    // 如果有支付相关的消息生产者，在这里调用
                    // paymentProducer.sendPaymentMessage(paymentMessage);
                    break;
                default:
                    throw new RuntimeException("不支持的消息主题: " + message.getTopic());
            }
            
            // 更新消息状态为已发送
            localMessageMapper.updateStatus(message.getMessageId(), "SENT", new Date());
            
            log.info("重试发送消息成功: messageId={}, topic={}, retryTimes={}", 
                message.getMessageId(), message.getTopic(), message.getRetryTimes() + 1);
                
        } catch (Exception e) {
            // 更新消息状态为重试失败
            localMessageMapper.updateStatus(message.getMessageId(), "RETRY_FAILED", new Date());
            throw new RuntimeException("重试发送消息失败", e);
        }
    }
} 