package org.example.quickbuy.service.impl;

import org.example.quickbuy.constant.OrderStatus;
import org.example.quickbuy.entity.Order;
import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.mapper.OrderMapper;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.example.quickbuy.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Override
    @Transactional
    public String createOrder(Long userId, Long activityId) {
        // 1. 获取秒杀活动信息
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new RuntimeException("秒杀活动不存在");
        }

        // 2. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setActivityId(activityId);
        order.setProductId(activity.getProductId());
        order.setAmount(activity.getSeckillPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 3. 保存订单
        orderMapper.insert(order);

        return order.getOrderNo();
    }

    @Override
    public boolean waitForPayment(String orderNo) {
        // 模拟等待支付结果，实际项目中应该对接支付系统
        try {
            // 等待30秒
            TimeUnit.SECONDS.sleep(30);
            
            // 查询订单状态
            Order order = orderMapper.selectByOrderNo(orderNo);
            return order != null && order.getStatus() == OrderStatus.PAID.getCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    @Transactional
    public void payOrder(String orderNo) {
        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 2. 检查订单状态
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT.getCode()) {
            throw new RuntimeException("订单状态不正确");
        }

        // 3. 更新订单状态
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        orderMapper.updateStatus(orderNo, OrderStatus.PAID.getCode());
        orderMapper.updatePayTime(orderNo, order.getPayTime());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        // 1. 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 2. 检查订单状态
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT.getCode()) {
            throw new RuntimeException("订单状态不正确");
        }

        // 3. 更新订单状态
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateStatus(orderNo, OrderStatus.CANCELLED.getCode());
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }
} 