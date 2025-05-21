package org.example.quickbuy.service;

import org.example.quickbuy.dto.SeckillActivityDTO;
import org.example.quickbuy.entity.SeckillActivity;

import java.io.IOException;

public interface SeckillService {
    /**
     * 初始化秒杀活动
     */
    void initSeckillActivity(SeckillActivityDTO activityDTO);

    /**
     * 执行秒杀
     * @return 秒杀结果
     */
    boolean seckill(Long userId, Long activityId) throws IOException;

    /**
     * 检查秒杀活动状态
     * @return true-可以秒杀，false-不可秒杀
     */
    boolean checkSeckillStatus(Long activityId);

    /**
     * 结束秒杀活动
     */
    void endSeckillActivity(Long activityId);
} 