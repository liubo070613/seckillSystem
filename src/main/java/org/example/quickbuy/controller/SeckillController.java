package org.example.quickbuy.controller;

import org.example.quickbuy.entity.SeckillActivity;
import org.example.quickbuy.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 初始化秒杀活动
     */
    @PostMapping("/activity/init")
    public ResponseEntity<String> initSeckillActivity(@RequestBody SeckillActivity activity) {
        seckillService.initSeckillActivity(activity);
        return ResponseEntity.ok("秒杀活动初始化成功");
    }

    /**
     * 执行秒杀
     */
    @PostMapping("/{activityId}")
    public ResponseEntity<String> seckill(
            @PathVariable Long activityId,
            @RequestParam Long userId) throws IOException {
        boolean success = seckillService.seckill(userId, activityId);
        if (success) {
            return ResponseEntity.ok("秒杀成功，订单处理中");
        } else {
            return ResponseEntity.badRequest().body("秒杀失败，请稍后重试");
        }
    }

    /**
     * 检查秒杀活动状态
     */
    @GetMapping("/{activityId}/status")
    public ResponseEntity<String> checkSeckillStatus(@PathVariable Long activityId) {
        boolean canSeckill = seckillService.checkSeckillStatus(activityId);
        if (canSeckill) {
            return ResponseEntity.ok("活动进行中，可以秒杀");
        } else {
            return ResponseEntity.ok("活动未开始或已结束");
        }
    }
} 