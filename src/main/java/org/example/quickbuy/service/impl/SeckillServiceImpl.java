package org.example.quickbuy.service.impl;

import jakarta.annotation.PostConstruct;
import org.example.quickbuy.constant.SeckillResult;
import org.example.quickbuy.constant.SeckillStatus;
import org.example.quickbuy.dto.SeckillActivityDTO;
import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.example.quickbuy.mq.SeckillProducer;
import org.example.quickbuy.service.RedisService;
import org.example.quickbuy.service.SeckillService;
import org.springframework.beans.BeanUtils;
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

    @Autowired
    private SeckillProducer seckillProducer;

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
    public void initSeckillActivity(SeckillActivityDTO activityDTO) {
        // 创建秒杀活动实体
        SeckillActivity activity = new SeckillActivity();
        // 使用BeanUtils复制属性
        BeanUtils.copyProperties(activityDTO, activity);
        
        // 设置其他属性
        activity.setStatus(SeckillStatus.NOT_STARTED); // 未开始
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
    public SeckillResult seckill(Long userId, Long activityId) {
        try {
            // 1. 检查用户是否已经秒杀过
            if (redisService.checkUserSeckillQualify(userId, activityId)) {
                return SeckillResult.ALREADY_PURCHASED;
            }

            // 2. 获取秒杀活动信息
            //todo: 缓存击穿问题,让一个线程去更新缓存，其他线程等待
            SeckillActivity activity = redisService.getSeckillActivity(activityId);
            if (activity == null) {
                // 如果Redis中没有，从数据库获取
                activity = seckillActivityMapper.selectById(activityId);
                if (activity == null) {
                    return SeckillResult.ACTIVITY_NOT_EXIST;
                }
                // 重新缓存
                redisService.cacheSeckillActivity(activity);
            }

            // 3. 检查秒杀活动状态
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(activity.getStartTime())) {
                return SeckillResult.ACTIVITY_NOT_STARTED;
            }
            if (now.isAfter(activity.getEndTime())) {
                return SeckillResult.ACTIVITY_ENDED;
            }

            // 4. 执行秒杀（使用Lua脚本保证原子性）
            String stockKey = String.format("seckill:stock:%s", activityId);
            Long result = redisTemplate.execute(seckillScript, Collections.singletonList(stockKey));
            if (result == null || result < 0) {
                return SeckillResult.STOCK_NOT_ENOUGH;
            }

            // 5. 设置用户秒杀资格
            redisService.setUserSeckillQualify(userId, activityId);

            // 6. 发送消息到RocketMQ
            SeckillMessage message = new SeckillMessage();
            message.setUserId(userId);
            message.setActivityId(activityId);
            message.setProductId(activity.getProductId());
            message.setStock(result.intValue());
            seckillProducer.sendSeckillMessage(message);

            return SeckillResult.SUCCESS;
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return SeckillResult.SYSTEM_ERROR;
        }
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
        
        // 根据时间自动更新活动状态
        if (now.isBefore(activity.getStartTime())) {
            // 未开始
            if (activity.getStatus() != SeckillStatus.NOT_STARTED) {
                activity.setStatus(SeckillStatus.NOT_STARTED);
                seckillActivityMapper.updateStatus(activityId, SeckillStatus.NOT_STARTED);
                redisService.cacheSeckillActivity(activity);
            }
            return false;
        } else if (now.isAfter(activity.getEndTime())) {
            // 已结束
            if (activity.getStatus() != SeckillStatus.ENDED) {
                activity.setStatus(SeckillStatus.ENDED);
                seckillActivityMapper.updateStatus(activityId, SeckillStatus.ENDED);
                redisService.cacheSeckillActivity(activity);
            }
            return false;
        } else {
            // 进行中
            if (activity.getStatus() != SeckillStatus.IN_PROGRESS) {
                activity.setStatus(SeckillStatus.IN_PROGRESS);
                seckillActivityMapper.updateStatus(activityId, SeckillStatus.IN_PROGRESS);
                redisService.cacheSeckillActivity(activity);
            }
            return true;
        }
    }

    @Override
    @Transactional
    public void endSeckillActivity(Long activityId) {
        // 1. 获取活动信息
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在");
        }

        // 2. 更新活动状态为已结束
        activity.setStatus(SeckillStatus.ENDED);
        activity.setUpdateTime(LocalDateTime.now());
        seckillActivityMapper.updateStatus(activityId, SeckillStatus.ENDED);

        // 3. 更新Redis缓存
        redisService.cacheSeckillActivity(activity);

        // 4. 清理Redis中的库存信息
        String stockKey = String.format("seckill:stock:%s", activityId);
        redisTemplate.delete(stockKey);
    }
}