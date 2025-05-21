package org.example.quickbuy.service.impl;

import jakarta.annotation.PostConstruct;
import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.example.quickbuy.service.RedisService;
import org.example.quickbuy.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    private DefaultRedisScript<Long> seckillScript;

    @PostConstruct
    public void init() {
        // 初始化Lua脚本
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
    }

    @Override
    @Transactional
    public void initSeckillActivity(SeckillActivity activity) {
        // 设置初始状态
        activity.setStatus(0); // 未开始
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        
        // 保存到数据库
        seckillActivityMapper.insert(activity);
        
        // 缓存秒杀活动信息
        redisService.cacheSeckillActivity(activity);
        // 缓存秒杀库存
        redisService.cacheSeckillStock(activity.getId(), activity.getStock());
    }

    @Override
    public boolean seckill(Long userId, Long activityId) {
        // 1. 检查用户是否已经秒杀过
        if (redisService.checkUserSeckillQualify(userId, activityId)) {
            return false;
        }

        // 2. 检查秒杀活动状态
        if (!checkSeckillStatus(activityId)) {
            return false;
        }

        // 3. 获取秒杀活动信息
        SeckillActivity activity = redisService.getSeckillActivity(activityId);
        if (activity == null) {
            // 如果Redis中没有，从数据库获取
            activity = seckillActivityMapper.selectById(activityId);
            if (activity == null) {
                return false;
            }
            // 重新缓存
            redisService.cacheSeckillActivity(activity);
        }

        // 4. 执行秒杀（使用Lua脚本保证原子性）
        String stockKey = String.format("seckill:stock:%s", activityId);
        Long result = redisTemplate.execute(seckillScript, Collections.singletonList(stockKey));
        if (result == null || result < 0) {
            return false;
        }

        // 5. 设置用户秒杀资格
        redisService.setUserSeckillQualify(userId, activityId);

        // 6. 异步更新数据库库存
        // TODO: 使用消息队列异步更新数据库库存
        seckillActivityMapper.updateStock(activityId, result.intValue());

        // 7. 异步创建订单
        // TODO: 调用订单服务创建订单

        return true;
    }

    @Override
    public boolean checkSeckillStatus(Long activityId) {
        // 先从Redis获取
        SeckillActivity activity = redisService.getSeckillActivity(activityId);
        if (activity == null) {
            // Redis中没有，从数据库获取
            activity = seckillActivityMapper.selectById(activityId);
            if (activity == null) {
                return false;
            }
            // 重新缓存
            redisService.cacheSeckillActivity(activity);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isActive = now.isAfter(activity.getStartTime()) && now.isBefore(activity.getEndTime());
        
        // 更新活动状态
        if (isActive && activity.getStatus() != 1) {
            activity.setStatus(1);
            seckillActivityMapper.updateStatus(activityId, 1);
            redisService.cacheSeckillActivity(activity);
        } else if (!isActive && activity.getStatus() != 2) {
            activity.setStatus(2);
            seckillActivityMapper.updateStatus(activityId, 2);
            redisService.cacheSeckillActivity(activity);
        }

        return isActive;
    }
}