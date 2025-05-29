package org.example.quickbuy.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private SeckillProducer seckillProducer;


    @Override
    @Transactional
    public void initSeckillActivity(SeckillActivityDTO activityDTO) {
        // 创建秒杀活动实体
        SeckillActivity activity = new SeckillActivity();
        BeanUtils.copyProperties(activityDTO, activity);

        // 设置初始状态
        activity.setStatus(SeckillStatus.NOT_STARTED);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());

        // 保存到数据库
        seckillActivityMapper.insert(activity);

        // 缓存活动信息和库存
        redisService.cacheSeckillActivity(activity);
        redisService.cacheSeckillStock(activity.getId(), activity.getStock());
    }

    @Override
    public SeckillResult seckill(Long userId, Long activityId) {
        try {
            // 1. 获取活动信息
            SeckillActivity activity = getActivity(activityId);
            if (activity == null) {
                return SeckillResult.ACTIVITY_NOT_EXIST;
            }

            // 2. 检查活动状态
            LocalDateTime now = LocalDateTime.now();
            // 2.1 如果活动已手动结束，直接返回
            if (activity.getStatus() == SeckillStatus.ENDED) {
                return SeckillResult.ACTIVITY_ENDED;
            }
            // 2.2 检查时间状态
            if (now.isBefore(activity.getStartTime())) {
                updateActivityStatus(activity, SeckillStatus.NOT_STARTED);
                return SeckillResult.ACTIVITY_NOT_STARTED;
            }
            if (now.isAfter(activity.getEndTime())) {
                updateActivityStatus(activity, SeckillStatus.ENDED);
                return SeckillResult.ACTIVITY_ENDED;
            }
            // 2.3 更新为进行中状态
            updateActivityStatus(activity, SeckillStatus.IN_PROGRESS);

            // 3. 执行秒杀
            Long stock = redisService.executeSeckillScript(activityId, userId);
            if (stock == null) {
                return SeckillResult.SYSTEM_ERROR;
            }
            if (stock == -2) {
                return SeckillResult.ACTIVITY_NOT_EXIST;
            }
            if (stock == -3) {
                return SeckillResult.REPEAT_SECKILL;
            }
            if (stock < 0) {
                return SeckillResult.STOCK_NOT_ENOUGH;
            }

            // 4. 发送秒杀消息
            sendSeckillMessage(userId, activity);

            return SeckillResult.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return SeckillResult.SYSTEM_ERROR;
        }
    }

    @Override
    public Integer checkSeckillStatus(Long activityId) {
        SeckillActivity activity = getActivity(activityId);
        if (activity == null) {
            return null;
        }
        return activity.getStatus();
    }

    @Override
    @Transactional
    public void endSeckillActivity(Long activityId) {
        // 1. 获取活动信息
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在");
        }

        // 2. 更新活动状态
        updateActivityStatus(activity, SeckillStatus.ENDED);

        // 3. 清理Redis数据
        redisService.clearSeckillActivity(activityId);
    }

    /**
     * 获取并检查活动信息
     */
    private SeckillActivity getActivity(Long activityId) {
        // 1. 先查Redis缓存
        SeckillActivity activity = redisService.getSeckillActivity(activityId);
        if (activity != null) {
            return activity;
        }

        // 2. 缓存未命中，尝试获取分布式锁
        String lockKey = redisService.getActivityLockKey(activityId);
        String requestId = UUID.randomUUID().toString();
        
        try {
            // 尝试获取锁，等待100ms，锁过期时间10s
            boolean locked = redisService.tryLock(lockKey, requestId, 10, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败，说明其他线程正在重建缓存，等待100ms后重试
                Thread.sleep(100);
                return getActivity(activityId);
            }

            // 3. 获取锁成功，再次检查缓存（双重检查）
            activity = redisService.getSeckillActivity(activityId);
            if (activity != null) {
                return activity;
            }

            // 4. 查询数据库
            activity = seckillActivityMapper.selectById(activityId);
            if (activity != null) {
                // 5. 重建缓存
                redisService.cacheSeckillActivity(activity);
                redisService.cacheSeckillStock(activity.getId(), activity.getStock());
            }
            
            return activity;
            
        } catch (Exception e) {
            log.error("获取活动信息异常: activityId={}", activityId, e);
            // 发生异常时，直接查询数据库
            return seckillActivityMapper.selectById(activityId);
        } finally {
            // 6. 释放锁
            try {
                redisService.releaseLock(lockKey, requestId);
            } catch (Exception e) {
                log.error("释放分布式锁异常: lockKey={}, requestId={}", lockKey, requestId, e);
            }
        }
    }

    /**
     * 更新活动状态
     */
    private void updateActivityStatus(SeckillActivity activity, Integer status) {
        if (activity.getStatus() != status) {
            activity.setStatus(status);
            activity.setUpdateTime(LocalDateTime.now());
            seckillActivityMapper.updateStatus(activity.getId(), status);
            redisService.cacheSeckillActivity(activity);
        }
    }

    /**
     * 发送秒杀消息
     */
    private void sendSeckillMessage(Long userId, SeckillActivity activity) {
        SeckillMessage message = new SeckillMessage();
        message.setUserId(userId);
        message.setActivityId(activity.getId());
        message.setProductId(activity.getProductId());
        message.setStock(1); // 假设每次秒杀1件商品
        seckillProducer.sendSeckillMessage(message);
    }
}