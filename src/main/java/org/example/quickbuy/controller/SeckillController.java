package org.example.quickbuy.controller;

import org.example.quickbuy.constant.SeckillResult;
import org.example.quickbuy.constant.SeckillStatus;
import org.example.quickbuy.dto.SeckillActivityDTO;
import org.example.quickbuy.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @PostMapping("/init")
    public ResponseEntity<String> initSeckillActivity(@RequestBody SeckillActivityDTO activityDTO) {
        seckillService.initSeckillActivity(activityDTO);
        return ResponseEntity.ok("秒杀活动初始化成功");
    }

    @PostMapping("/{activityId}/end")
    public ResponseEntity<String> endSeckillActivity(@PathVariable Long activityId) {
        seckillService.endSeckillActivity(activityId);
        return ResponseEntity.ok("秒杀活动已结束");
    }

    @PostMapping("/execute")
    public ResponseEntity<String> executeSeckill(@RequestParam Long userId, @RequestParam Long activityId) throws IOException {
        SeckillResult result = seckillService.seckill(userId, activityId);
        if (result == SeckillResult.SUCCESS) {
            return ResponseEntity.ok(result.getMessage());
        }
        return ResponseEntity.badRequest().body(result.getMessage());
    }

    @GetMapping("/{activityId}/status")
    public ResponseEntity<String> checkStatus(@PathVariable Long activityId) {
        Integer status = seckillService.checkSeckillStatus(activityId);
        if (status == null) {
            return ResponseEntity.ok("活动不存在");
        }
        return ResponseEntity.ok(SeckillStatus.getStatusDesc(status));
    }
} 