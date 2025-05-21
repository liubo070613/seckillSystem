package org.example.quickbuy.service.impl;

import org.example.quickbuy.constant.OrderStatus;
import org.example.quickbuy.entity.Order;
import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.mapper.OrderMapper;
import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.example.quickbuy.mq.SeckillProducer;
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

    @Autowired
    private SeckillProducer seckillProducer;

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
        // 查询订单状态
        Order order = orderMapper.selectByOrderNo(orderNo);
        return order != null && order.getStatus() == OrderStatus.PAID.getCode();
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

        // 4. 发送支付成功消息
        SeckillMessage message = new SeckillMessage();
        message.setUserId(order.getUserId());
        message.setActivityId(order.getActivityId());
        message.setProductId(order.getProductId());
        message.setOrderNo(orderNo);
        // 库存数量为1，因为秒杀商品每个用户只能买一个
        message.setStock(1);
        seckillProducer.sendPaymentSuccessMessage(message);
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