package org.example.quickbuy.service.impl;

import org.example.quickbuy.config.RedisKeyConfig;
import org.example.quickbuy.entity.Product;
import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
    public boolean isUserSeckilled(Long userId, Long activityId) {
        String key = String.format(RedisKeyConfig.USER_SECKILL_QUALIFY_KEY, userId, activityId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public Long executeSeckillScript(Long activityId, DefaultRedisScript<Long> script) {
        String stockKey = String.format(RedisKeyConfig.SECKILL_STOCK_KEY, activityId);
        return redisTemplate.execute(script, Collections.singletonList(stockKey));
    }

    @Override
    public void clearSeckillActivity(Long activityId) {
        String activityKey = String.format(RedisKeyConfig.SECKILL_ACTIVITY_KEY, activityId);
        String stockKey = String.format(RedisKeyConfig.SECKILL_STOCK_KEY, activityId);
        redisTemplate.delete(Arrays.asList(activityKey, stockKey));
    }
} 