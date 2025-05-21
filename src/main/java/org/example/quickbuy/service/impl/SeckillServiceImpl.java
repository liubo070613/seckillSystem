package org.example.quickbuy.service.impl;

import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.service.RedisService;
import org.example.quickbuy.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void initSeckillActivity(SeckillActivity activity) {
        // 缓存秒杀活动信息
        redisService.cacheSeckillActivity(activity);
        // 缓存秒杀库存
        redisService.cacheSeckillStock(activity.getId(), activity.getStock());
    }

    @Override
    public boolean seckill(Long userId, Long activityId) throws IOException {
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
            return false;
        }

        // 4. 执行秒杀（使用Lua脚本保证原子性）
        String stockKey = String.format("seckill:stock:%s", activityId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(new String(new ClassPathResource("scripts/seckill.lua").getInputStream().readAllBytes()));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(stockKey));
        if (result == null || result < 0) {
            return false;
        }

        // 5. 设置用户秒杀资格
        redisService.setUserSeckillQualify(userId, activityId);

        // 6. 异步创建订单
        // TODO: 调用订单服务创建订单

        return true;
    }

    @Override
    public boolean checkSeckillStatus(Long activityId) {
        SeckillActivity activity = redisService.getSeckillActivity(activityId);
        if (activity == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(activity.getStartTime()) && now.isBefore(activity.getEndTime());
    }
}