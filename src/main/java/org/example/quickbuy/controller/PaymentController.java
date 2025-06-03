package org.example.quickbuy.controller;

import org.example.quickbuy.mapper.SeckillActivityMapper;
import org.example.quickbuy.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    /**
     * 支付订单
     * @param orderNo 订单号
     * @return 支付结果
     */
    @PostMapping("/pay/{orderNo}")
    public String payOrder(@PathVariable String orderNo) {
        try {
            orderService.payOrder(orderNo);
            return "支付成功";
        } catch (Exception e) {
            return "支付失败：" + e.getMessage();
        }
    }

    /**
     * 查询订单支付状态
     * @param orderNo 订单号
     * @return 订单状态
     */
    @GetMapping("/status/{orderNo}")
    public String getPaymentStatus(@PathVariable String orderNo) {
        try {
            boolean paid = orderService.waitForPayment(orderNo);
            return paid ? "已支付" : "未支付";
        } catch (Exception e) {
            return "查询失败：" + e.getMessage();
        }
    }

    @PostMapping("/updateStock/{activityId}")
    public void updateStock(@PathVariable Long activityId) {
        try {
            seckillActivityMapper.updateStock(activityId);
        } catch (Exception e) {
            throw new RuntimeException("更新库存失败: " + e.getMessage(), e);
        }
    }
} 