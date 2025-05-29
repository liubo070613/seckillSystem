package org.example.quickbuy.task;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.example.quickbuy.entity.LocalMessage;
import org.example.quickbuy.mapper.LocalMessageMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.example.quickbuy.mq.SeckillProducer;
import org.example.quickbuy.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class MessageCompensationTask {

    @Autowired
    private RedisService  redisService;

    /**
     * 补偿秒杀消息
     */
    public void compensateSeckillMessage(SeckillMessage message) {

        // 回滚库存和恢复秒杀资格
        Long stock = redisService.rollbackSeckillStock(
                message.getActivityId(),
                message.getStock(),
                message.getUserId()
        );

        if (stock == -2) {
            log.warn("活动不存在，无法回滚库存: {}", message.getActivityId());
        } else {
            log.info("redis库存回滚成功，活动ID: {}, 回滚数量: {}, 当前库存: {}",
                    message.getActivityId(), message.getStock(), stock);
        }

        log.info("执行秒杀消息补偿: messageId={}", message.getMessageId());
    }

} 