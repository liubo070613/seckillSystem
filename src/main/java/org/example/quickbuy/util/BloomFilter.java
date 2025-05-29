package org.example.quickbuy.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class BloomFilter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 布隆过滤器的key前缀
    private static final String BLOOM_FILTER_PREFIX = "bloom:filter:";
    
    // 布隆过滤器的大小（预计元素数量的10倍）
    private static final long BLOOM_FILTER_SIZE = 1000000L;
    
    // 哈希函数的个数
    private static final int HASH_FUNCTIONS = 5;
    
    /**
     * 添加元素到布隆过滤器
     * @param key 布隆过滤器的key
     * @param value 要添加的值
     */
    public void add(String key, String value) {
        String bloomKey = BLOOM_FILTER_PREFIX + key;
        for (int i = 0; i < HASH_FUNCTIONS; i++) {
            long hash = hash(value, i);
            redisTemplate.opsForValue().setBit(bloomKey, hash, true);
        }
    }
    
    /**
     * 批量添加元素到布隆过滤器
     * @param key 布隆过滤器的key
     * @param values 要添加的值列表
     */
    public void addAll(String key, List<String> values) {
        String bloomKey = BLOOM_FILTER_PREFIX + key;
        for (String value : values) {
            for (int i = 0; i < HASH_FUNCTIONS; i++) {
                long hash = hash(value, i);
                redisTemplate.opsForValue().setBit(bloomKey, hash, true);
            }
        }
    }
    
    /**
     * 检查元素是否在布隆过滤器中
     * @param key 布隆过滤器的key
     * @param value 要检查的值
     * @return 如果可能存在返回true，如果一定不存在返回false
     */
    public boolean mightContain(String key, String value) {
        String bloomKey = BLOOM_FILTER_PREFIX + key;
        for (int i = 0; i < HASH_FUNCTIONS; i++) {
            long hash = hash(value, i);
            Boolean bit = redisTemplate.opsForValue().getBit(bloomKey, hash);
            if (Boolean.FALSE.equals(bit)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 删除布隆过滤器
     * @param key 布隆过滤器的key
     */
    public void delete(String key) {
        String bloomKey = BLOOM_FILTER_PREFIX + key;
        redisTemplate.delete(bloomKey);
    }
    
    /**
     * 计算哈希值
     * @param value 输入值
     * @param index 哈希函数索引
     * @return 哈希值
     */
    private long hash(String value, int index) {
        long hash = 0;
        for (char c : value.toCharArray()) {
            hash = hash * 31 + c;
        }
        return Math.abs(hash + index * hash) % BLOOM_FILTER_SIZE;
    }
} 