package org.example.quickbuy.controller;

import org.example.quickbuy.constant.SeckillResult;
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

    @PostMapping("/{activityId}/seckill")
    public ResponseEntity<String> seckill(@PathVariable Long activityId, @RequestParam Long userId) throws IOException {
        SeckillResult result = seckillService.seckill(userId, activityId);
        return result == SeckillResult.SUCCESS ?
            ResponseEntity.ok(result.getMessage()) :
            ResponseEntity.badRequest().body(result.getMessage());
    }

    @GetMapping("/{activityId}/status")
    public ResponseEntity<String> checkStatus(@PathVariable Long activityId) {
        boolean isActive = seckillService.checkSeckillStatus(activityId);
        return ResponseEntity.ok(isActive ? "活动进行中" : "活动已结束");
    }
} 