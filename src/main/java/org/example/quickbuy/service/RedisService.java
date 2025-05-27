package org.example.quickbuy.service;

import org.example.quickbuy.config.RedisKeyConfig;
import org.example.quickbuy.entity.Product;
import org.example.quickbuy.entity.SeckillActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis服务接口
 */
public interface RedisService {
    /**
     * 缓存商品信息
     */
    void cacheProduct(Product product);

    /**
     * 缓存商品库存
     */
    void cacheProductStock(Long productId, Integer stock);

    /**
     * 缓存秒杀活动信息
     */
    void cacheSeckillActivity(SeckillActivity activity);

    /**
     * 缓存秒杀库存
     */
    void cacheSeckillStock(Long activityId, Integer stock);

    /**
     * 设置用户秒杀资格
     */
    void setUserSeckillQualify(Long userId, Long activityId);

    /**
     * 检查用户是否有秒杀资格
     */
    boolean checkUserSeckillQualify(Long userId, Long activityId);

    /**
     * 获取商品信息
     */
    Product getProduct(Long productId);

    /**
     * 获取秒杀活动信息
     */
    SeckillActivity getSeckillActivity(Long activityId);

    /**
     * 获取商品库存
     */
    Integer getProductStock(Long productId);

    /**
     * 获取秒杀库存
     */
    Integer getSeckillStock(Long activityId);

    /**
     * 执行秒杀脚本
     * @param activityId 活动ID
     * @param script Lua脚本
     * @param userId 用户ID
     * @return 剩余库存，-1表示库存不足，-2表示活动不存在，-3表示重复秒杀
     */
    Long executeSeckillScript(Long activityId, RedisScript<Long> script, Long userId);

    /**
     * 清理秒杀活动数据
     */
    void clearSeckillActivity(Long activityId);
} 