package org.example.quickbuy.service;

public interface OrderService {
    /**
     * 创建订单
     * @return 订单号
     */
    String createOrder(Long userId, Long activityId);


    /**
     * 支付订单
     */
    void payOrder(String orderNo);

    /**
     * 取消订单
     */
    void cancelOrder(String orderNo);
} 