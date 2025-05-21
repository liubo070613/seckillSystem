package org.example.quickbuy.controller;

import org.example.quickbuy.service.SeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SeckillOrderService seckillOrderService;

    @PostMapping("/{productId}")
    public ResponseEntity<String> seckill(@PathVariable Long productId, @RequestParam Long userId) throws IOException {
        String stockKey = "seckill:stock:" + productId;
        
        // 加载Lua脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(new String(new ClassPathResource("scripts/seckill.lua").getInputStream().readAllBytes()));
        script.setResultType(Long.class);

        // 执行Lua脚本
        Long result = redisTemplate.execute(script, Collections.singletonList(stockKey));
        
        if (result == null || result < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("秒杀已结束或库存不足");
        }

        // 异步处理订单
        seckillOrderService.createOrderAsync(productId, userId);

        return ResponseEntity.ok("秒杀成功，订单处理中");
    }

    // 初始化商品库存接口（实际项目中应该由管理员调用）
    @PostMapping("/init/{productId}")
    public ResponseEntity<String> initStock(@PathVariable Long productId, @RequestParam Integer stock) {
        String stockKey = "seckill:stock:" + productId;
        redisTemplate.opsForValue().set(stockKey, stock);
        return ResponseEntity.ok("库存初始化成功");
    }
} 