package org.example.quickbuy.service;

import lombok.extern.slf4j.Slf4j;
import org.example.quickbuy.constant.OrderStatus;
import org.example.quickbuy.entity.Order;
import org.example.quickbuy.mapper.OrderMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.example.quickbuy.mq.SeckillProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息补偿服务
 * 定时检查未处理的订单，进行补偿处理
 */
@Slf4j
@Service
public class MessageCompensationService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private SeckillProducer seckillProducer;

    /**
     * 定时检查超时未支付的订单
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void checkTimeoutOrders() {
        try {
            log.info("开始检查超时订单补偿，时间: {}", LocalDateTime.now());
            
            // 查询超时未支付的订单（创建时间超过35分钟且状态为待支付）
            List<Order> timeoutOrders = getTimeoutOrders();
            
            if (timeoutOrders.isEmpty()) {
                log.debug("没有发现超时订单");
                return;
            }
            
            log.info("发现 {} 个超时订单需要补偿处理", timeoutOrders.size());
            
            for (Order order : timeoutOrders) {
                try {
                    // 构建补偿消息
                    SeckillMessage compensationMessage = buildCompensationMessage(order);
                    
                    // 重新发送超时处理消息，1秒后立即处理
                    seckillProducer.sendOrderTimeoutMessage(compensationMessage, 1);
                    
                    log.info("发送订单超时补偿消息成功: 订单号={}, 用户ID={}, 活动ID={}", 
                        order.getOrderNo(), order.getUserId(), order.getActivityId());
                        
                } catch (Exception e) {
                    log.error("发送订单超时补偿消息失败: 订单号={}, 错误={}", 
                        order.getOrderNo(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("检查超时订单补偿失败", e);
        }
    }

    
    /**
     * 查询超时未支付的订单
     */
    private List<Order> getTimeoutOrders() {
        // 查询创建时间超过35分钟且状态为待支付的订单
        // 这里给延时消息留5分钟的缓冲时间
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(35);
        return orderMapper.selectTimeoutOrders(timeoutThreshold, OrderStatus.PENDING_PAYMENT.getCode());
    }
    
    /**
     * 构建补偿消息
     */
    private SeckillMessage buildCompensationMessage(Order order) {
        SeckillMessage message = new SeckillMessage();
        message.setUserId(order.getUserId());
        message.setActivityId(order.getActivityId());
        message.setProductId(order.getProductId());
        message.setOrderNo(order.getOrderNo());
        message.setStock(1); // 秒杀商品数量固定为1
        return message;
    }
} 