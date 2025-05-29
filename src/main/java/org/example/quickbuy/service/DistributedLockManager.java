package org.example.quickbuy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class DistributedLockManager {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 存储锁的续期任务
    private final Map<String, ScheduledFuture<?>> renewalTasks = new ConcurrentHashMap<>();
    
    // 续期线程池
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r);
        thread.setName("lock-renewal-thread");
        thread.setDaemon(true);
        return thread;
    });
    
    /**
     * 尝试获取分布式锁
     * @param lockKey 锁的key
     * @param requestId 请求标识
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime, TimeUnit timeUnit) {
        try {
            // 使用SETNX命令实现分布式锁
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, expireTime, timeUnit);
            if (Boolean.TRUE.equals(result)) {
                // 获取锁成功，启动续期任务
                startRenewalTask(lockKey, requestId, expireTime, timeUnit);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁异常: lockKey={}, requestId={}", lockKey, requestId, e);
            return false;
        }
    }
    
    /**
     * 释放分布式锁
     * @param lockKey 锁的key
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String requestId) {
        try {
            // 停止续期任务
            stopRenewalTask(lockKey);
            
            // 使用Lua脚本保证原子性
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                          "return redis.call('del', KEYS[1]) " +
                          "else " +
                          "return 0 " +
                          "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), requestId);
            
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("释放分布式锁异常: lockKey={}, requestId={}", lockKey, requestId, e);
            return false;
        }
    }
    
    /**
     * 启动续期任务
     */
    private void startRenewalTask(String lockKey, String requestId, long expireTime, TimeUnit timeUnit) {
        // 计算续期间隔（过期时间的1/3）
        long renewalInterval = timeUnit.toMillis(expireTime) / 3;
        
        // 创建续期任务
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                // 检查锁是否还存在
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                              "return redis.call('expire', KEYS[1], ARGV[2]) " +
                              "else " +
                              "return 0 " +
                              "end";
                
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
                Long result = redisTemplate.execute(redisScript, 
                    Collections.singletonList(lockKey), 
                    requestId, 
                    String.valueOf(timeUnit.toSeconds(expireTime)));
                
                if (result == null || result == 0) {
                    // 锁不存在或续期失败，停止续期任务
                    stopRenewalTask(lockKey);
                    log.warn("锁续期失败，停止续期任务: lockKey={}, requestId={}", lockKey, requestId);
                }
            } catch (Exception e) {
                log.error("锁续期异常: lockKey={}, requestId={}", lockKey, requestId, e);
                stopRenewalTask(lockKey);
            }
        }, renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
        
        // 保存续期任务
        renewalTasks.put(lockKey, future);
    }
    
    /**
     * 停止续期任务
     */
    private void stopRenewalTask(String lockKey) {
        ScheduledFuture<?> future = renewalTasks.remove(lockKey);
        if (future != null) {
            future.cancel(false);
        }
    }
} 