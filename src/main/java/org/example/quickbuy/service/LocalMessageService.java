package org.example.quickbuy.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.example.quickbuy.entity.LocalMessage;
import org.example.quickbuy.mapper.LocalMessageMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class LocalMessageService {
    
    @Autowired
    private LocalMessageMapper localMessageMapper;
    
    /**
     * 保存消息到本地消息表
     */
    public void saveMessage(SeckillMessage message, String topic, String status) {
        try {
            LocalMessage localMessage = new LocalMessage();
            localMessage.setMessageId(message.getMessageId());
            localMessage.setTopic(topic);
            localMessage.setContent(JSON.toJSONString(message));
            localMessage.setStatus(status);
            localMessage.setCreateTime(new Date());
            localMessage.setRetryTimes(0);
            
            localMessageMapper.insert(localMessage);
        } catch (Exception e) {
            log.error("保存消息到本地消息表异常: messageId={}", message.getMessageId(), e);
            throw new RuntimeException("保存消息失败", e);
        }
    }
    
    /**
     * 更新消息状态
     */
    public void updateMessageStatus(String messageId, String status) {
        try {
            localMessageMapper.updateStatus(messageId, status, new Date());
        } catch (Exception e) {
            log.error("更新消息状态异常: messageId={}", messageId, e);
            throw new RuntimeException("更新消息状态失败", e);
        }
    }
    
    /**
     * 增加重试次数
     */
    public void incrementRetryTimes(String messageId) {
        try {
            localMessageMapper.incrementRetryTimes(messageId);
        } catch (Exception e) {
            log.error("增加重试次数异常: messageId={}", messageId, e);
            throw new RuntimeException("增加重试次数失败", e);
        }
    }
} 