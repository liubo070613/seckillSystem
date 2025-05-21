package org.example.quickbuy.constant;

/**
 * 秒杀活动状态常量
 */
public class SeckillStatus {
    /**
     * 未开始
     */
    public static final int NOT_STARTED = 0;

    /**
     * 进行中
     */
    public static final int IN_PROGRESS = 1;

    /**
     * 已结束
     */
    public static final int ENDED = 2;

    /**
     * 获取状态描述
     */
    public static String getStatusDesc(int status) {
        switch (status) {
            case NOT_STARTED:
                return "未开始";
            case IN_PROGRESS:
                return "进行中";
            case ENDED:
                return "已结束";
            default:
                return "未知状态";
        }
    }
} 