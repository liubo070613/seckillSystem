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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private RedisService redisService;

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
            // 1. 检查用户是否重复秒杀
            if (redisService.isUserSeckilled(userId, activityId)) {
                return SeckillResult.REPEAT_SECKILL;
            }

            // 2. 获取并检查活动状态
            SeckillActivity activity = getAndCheckActivity(activityId);
            if (activity == null) {
                return SeckillResult.ACTIVITY_NOT_EXIST;
            }

            // 3. 执行秒杀
            Long stock = redisService.executeSeckillScript(activityId, seckillScript);
            if (stock == null || stock < 0) {
                return SeckillResult.STOCK_NOT_ENOUGH;
            }

            // 4. 记录用户秒杀资格
            redisService.setUserSeckillQualify(userId, activityId);

            // 5. 发送秒杀消息
            sendSeckillMessage(userId, activity, stock);

            return SeckillResult.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return SeckillResult.SYSTEM_ERROR;
        }
    }

    @Override
    public Integer checkSeckillStatus(Long activityId) {
        SeckillActivity activity = getAndCheckActivity(activityId);
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
    private SeckillActivity getAndCheckActivity(Long activityId) {
        SeckillActivity activity = redisService.getSeckillActivity(activityId);
        if (activity == null) {
            activity = seckillActivityMapper.selectById(activityId);
            if (activity != null) {
                redisService.cacheSeckillActivity(activity);
            }
        }
        return activity;
    }

    /**
     * 检查活动时间状态
     */
    private SeckillResult checkActivityTimeStatus(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            updateActivityStatus(activity, SeckillStatus.NOT_STARTED);
            return SeckillResult.ACTIVITY_NOT_STARTED;
        }
        if (now.isAfter(activity.getEndTime())) {
            updateActivityStatus(activity, SeckillStatus.ENDED);
            return SeckillResult.ACTIVITY_ENDED;
        }
        updateActivityStatus(activity, SeckillStatus.IN_PROGRESS);
        return SeckillResult.SUCCESS;
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
    private void sendSeckillMessage(Long userId, SeckillActivity activity, Long stock) {
        SeckillMessage message = new SeckillMessage();
        message.setUserId(userId);
        message.setActivityId(activity.getId());
        message.setProductId(activity.getProductId());
        message.setStock(stock.intValue());
        seckillProducer.sendSeckillMessage(message);
    }
}