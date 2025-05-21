package org.example.quickbuy.constant;

import lombok.Getter;

/**
 * 秒杀结果枚举
 */
@Getter
public enum SeckillResult {
    SUCCESS(0, "秒杀成功"),
    ALREADY_PURCHASED(1, "您已经抢购过该商品"),
    ACTIVITY_NOT_STARTED(2, "活动未开始"),
    ACTIVITY_ENDED(3, "活动已结束"),
    STOCK_NOT_ENOUGH(4, "商品库存不足"),
    ACTIVITY_NOT_EXIST(5, "活动不存在"),
    SYSTEM_ERROR(6, "系统异常，请稍后重试");

    private final int code;
    private final String message;

    SeckillResult(int code, String message) {
        this.code = code;
        this.message = message;
    }
} 