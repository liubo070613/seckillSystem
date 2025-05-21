package org.example.quickbuy.service;

import org.example.quickbuy.config.RedisKeyConfig;
import org.example.quickbuy.entity.Product;
import org.example.quickbuy.entity.SeckillActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存商品信息
    public void cacheProduct(Product product) {
        String key = String.format(RedisKeyConfig.PRODUCT_INFO_KEY, product.getId());
        redisTemplate.opsForValue().set(key, product, 24, TimeUnit.HOURS);
    }

    // 缓存商品库存
    public void cacheProductStock(Long productId, Integer stock) {
        String key = String.format(RedisKeyConfig.PRODUCT_STOCK_KEY, productId);
        redisTemplate.opsForValue().set(key, stock);
    }

    // 缓存秒杀活动信息
    public void cacheSeckillActivity(SeckillActivity activity) {
        String key = String.format(RedisKeyConfig.SECKILL_ACTIVITY_KEY, activity.getId());
        redisTemplate.opsForValue().set(key, activity);
    }

    // 缓存秒杀库存
    public void cacheSeckillStock(Long activityId, Integer stock) {
        String key = String.format(RedisKeyConfig.SECKILL_STOCK_KEY, activityId);
        redisTemplate.opsForValue().set(key, stock);
    }

    // 设置用户秒杀资格
    public void setUserSeckillQualify(Long userId, Long activityId) {
        String key = String.format(RedisKeyConfig.USER_SECKILL_QUALIFY_KEY, userId, activityId);
        redisTemplate.opsForValue().set(key, 1, 24, TimeUnit.HOURS);
    }

    // 检查用户是否有秒杀资格
    public boolean checkUserSeckillQualify(Long userId, Long activityId) {
        String key = String.format(RedisKeyConfig.USER_SECKILL_QUALIFY_KEY, userId, activityId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 获取商品信息
    public Product getProduct(Long productId) {
        String key = String.format(RedisKeyConfig.PRODUCT_INFO_KEY, productId);
        return (Product) redisTemplate.opsForValue().get(key);
    }

    // 获取秒杀活动信息
    public SeckillActivity getSeckillActivity(Long activityId) {
        String key = String.format(RedisKeyConfig.SECKILL_ACTIVITY_KEY, activityId);
        return (SeckillActivity) redisTemplate.opsForValue().get(key);
    }

    // 获取商品库存
    public Integer getProductStock(Long productId) {
        String key = String.format(RedisKeyConfig.PRODUCT_STOCK_KEY, productId);
        return (Integer) redisTemplate.opsForValue().get(key);
    }

    // 获取秒杀库存
    public Integer getSeckillStock(Long activityId) {
        String key = String.format(RedisKeyConfig.SECKILL_STOCK_KEY, activityId);
        return (Integer) redisTemplate.opsForValue().get(key);
    }
} 