package org.example.quickbuy.config;

public class RedisKeyConfig {
    // 商品库存key
    public static final String PRODUCT_STOCK_KEY = "product:stock:%s";
    
    // 商品详情key
    public static final String PRODUCT_INFO_KEY = "product:info:%s";
    
    // 秒杀活动key
    public static final String SECKILL_ACTIVITY_KEY = "seckill:activity:%s";
    
    // 用户秒杀资格key（防止重复秒杀）
    public static final String USER_SECKILL_QUALIFY_KEY = "seckill:qualify:%s:%s";
    
    // 秒杀订单key
    public static final String SECKILL_ORDER_KEY = "seckill:order:%s";
    
    // 秒杀商品库存key
    public static final String SECKILL_STOCK_KEY = "seckill:stock:%s";

    /** 活动分布式锁前缀 */
    public static final String ACTIVITY_LOCK_PREFIX = "seckill:lock:activity:";
} 