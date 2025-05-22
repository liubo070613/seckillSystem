package org.example.quickbuy.constant;

import lombok.Getter;

/**
 * 秒杀结果枚举
 */
@Getter
public enum SeckillResult {
    SUCCESS(0, "秒杀成功"),
    ACTIVITY_NOT_EXIST(1, "活动不存在"),
    ACTIVITY_NOT_STARTED(2, "活动未开始"),
    ACTIVITY_ENDED(3, "活动已结束"),
    STOCK_NOT_ENOUGH(4, "库存不足"),
    REPEAT_SECKILL(5, "重复秒杀"),
    SYSTEM_ERROR(6, "系统错误");

    private final int code;
    private final String message;

    SeckillResult(int code, String message) {
        this.code = code;
        this.message = message;
    }
} 