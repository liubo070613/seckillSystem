package org.example.quickbuy.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MessageIdempotentUtil {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String MESSAGE_IDEMPOTENT_KEY = "message:idempotent:";
    private static final long MESSAGE_EXPIRE_TIME = 24; // 消息幂等性检查的过期时间（小时）
    
    /**
     * 检查消息是否重复消费
     * @param messageId 消息ID
     * @return true表示消息已处理过，false表示消息未处理过
     */
    public boolean hasMessageProcessed(String messageId) {
        String key = MESSAGE_IDEMPOTENT_KEY + messageId;
        try {
            // 使用SETNX命令实现幂等性检查
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", MESSAGE_EXPIRE_TIME, TimeUnit.HOURS);
            return Boolean.FALSE.equals(result);
        } catch (Exception e) {
            log.error("检查消息幂等性异常: messageId={}", messageId, e);
            // 发生异常时，为了安全起见，认为消息已处理过
            return true;
        }
    }
    
    /**
     * 标记消息已处理
     * @param messageId 消息ID
     */
    public void markMessageProcessed(String messageId) {
        String key = MESSAGE_IDEMPOTENT_KEY + messageId;
        try {
            redisTemplate.opsForValue().set(key, "1", MESSAGE_EXPIRE_TIME, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("标记消息已处理异常: messageId={}", messageId, e);
        }
    }
    
    /**
     * 删除消息处理记录
     * @param messageId 消息ID
     */
    public void removeMessageProcessed(String messageId) {
        String key = MESSAGE_IDEMPOTENT_KEY + messageId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除消息处理记录异常: messageId={}", messageId, e);
        }
    }
} 