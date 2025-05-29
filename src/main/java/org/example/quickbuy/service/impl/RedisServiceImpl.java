package org.example.quickbuy.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.quickbuy.config.RedisKeyConfig;
import org.example.quickbuy.entity.Product;
import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.service.DistributedLockManager;
import org.example.quickbuy.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DistributedLockManager lockManager;

    private DefaultRedisScript<Long> seckillScript;
    private DefaultRedisScript<Long> rollbackStockScript;

    @PostConstruct
    public void init() {

        // 初始化秒杀Lua脚本
        seckillScript = new DefaultRedisScript<>();
        try {
            String script = StreamUtils.copyToString(
                    new ClassPathResource("scripts/seckill.lua").getInputStream(),
                    StandardCharsets.UTF_8
            );
            seckillScript.setScriptText(script);
            seckillScript.setResultType(Long.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load seckill script", e);
        }

        // 初始化回滚库存脚本
        rollbackStockScript = new DefaultRedisScript<>();
        try {
            String script = StreamUtils.copyToString(
                    new ClassPathResource("scripts/rollback_stock.lua").getInputStream(),
                    StandardCharsets.UTF_8
            );
            rollbackStockScript.setScriptText(script);
            rollbackStockScript.setResultType(Long.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load rollback stock script", e);
        }
    }

    @Override
    public void cacheProduct(Product product) {
        String key = String.format(RedisKeyConfig.PRODUCT_INFO_KEY, product.getId());
        redisTemplate.opsForValue().set(key, product, 24, TimeUnit.HOURS);
    }

    @Override
    public void cacheProductStock(Long productId, Integer stock) {
        String key = String.format(RedisKeyConfig.PRODUCT_STOCK_KEY, productId);
        redisTemplate.opsForValue().set(key, stock);
    }

    @Override
    public void cacheSeckillActivity(SeckillActivity activity) {
        String key = String.format(RedisKeyConfig.SECKILL_ACTIVITY_KEY, activity.getId());
        redisTemplate.opsForValue().set(key, activity);
    }

    @Override
    public void cacheSeckillStock(Long activityId, Integer stock) {
        String key = String.format(RedisKeyConfig.SECKILL_STOCK_KEY, activityId);
        redisTemplate.opsForValue().set(key, stock);
    }

    @Override
    public void setUserSeckillQualify(Long userId, Long activityId) {
        String key = String.format(RedisKeyConfig.USER_SECKILL_QUALIFY_KEY, userId, activityId);
        redisTemplate.opsForValue().set(key, true, 24, TimeUnit.HOURS);
    }

    @Override
    public boolean checkUserSeckillQualify(Long userId, Long activityId) {
        String key = String.format(RedisKeyConfig.USER_SECKILL_QUALIFY_KEY, userId, activityId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public Product getProduct(Long productId) {
        String key = String.format(RedisKeyConfig.PRODUCT_INFO_KEY, productId);
        return (Product) redisTemplate.opsForValue().get(key);
    }

    @Override
    public SeckillActivity getSeckillActivity(Long activityId) {
        String key = String.format(RedisKeyConfig.SECKILL_ACTIVITY_KEY, activityId);
        return (SeckillActivity) redisTemplate.opsForValue().get(key);
    }

    @Override
    public Integer getProductStock(Long productId) {
        String key = String.format(RedisKeyConfig.PRODUCT_STOCK_KEY, productId);
        return (Integer) redisTemplate.opsForValue().get(key);
    }

    @Override
    public Integer getSeckillStock(Long activityId) {
        String key = String.format(RedisKeyConfig.SECKILL_STOCK_KEY, activityId);
        return (Integer) redisTemplate.opsForValue().get(key);
    }

    @Override
    public Long executeSeckillScript(Long activityId,  Long userId) {
        return redisTemplate.execute(seckillScript, Collections.singletonList(activityId.toString()), userId);
    }

    @Override
    public void clearSeckillActivity(Long activityId) {
        String activityKey = String.format(RedisKeyConfig.SECKILL_ACTIVITY_KEY, activityId);
        String stockKey = String.format(RedisKeyConfig.SECKILL_STOCK_KEY, activityId);
        redisTemplate.delete(Arrays.asList(activityKey, stockKey));
    }

    @Override
    public Long rollbackSeckillStock(Long activityId, Integer stock, Long userId) {
        return redisTemplate.execute(rollbackStockScript, 
            Collections.singletonList(activityId.toString()), 
            stock, userId);
    }

    @Override
    public boolean tryLock(String lockKey, String requestId, long expireTime, TimeUnit timeUnit) {
        return lockManager.tryLock(lockKey, requestId, expireTime, timeUnit);
    }

    @Override
    public boolean releaseLock(String lockKey, String requestId) {
        return lockManager.releaseLock(lockKey, requestId);
    }

    @Override
    public String getActivityLockKey(Long activityId) {
        return RedisKeyConfig.ACTIVITY_LOCK_PREFIX + activityId;
    }
} 