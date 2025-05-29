package org.example.quickbuy;

import org.example.quickbuy.entity.User;
import org.example.quickbuy.mapper.UserMapper;
import org.example.quickbuy.mq.SeckillMessage;
import org.example.quickbuy.mq.SeckillProducer;
import org.example.quickbuy.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
class QuickBuyApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillProducer seckillProducer;

    @Test
    void contextLoads() {

//        User user = new User();
//        user.setUsername("test");
//        user.setPassword("123456");
//        user.setPhone("13800138000");
//        user.setEmail("test@example.com");
//        user.setStatus(1);
//        user.setCreateTime(LocalDateTime.now());
//        user.setUpdateTime(LocalDateTime.now());
//        userMapper.insert(user);

//        System.out.println(redisService.rollbackSeckillStock(3L, 1, 1L));

//        System.out.println("发送时间: " + LocalDateTime.now());

//        seckillProducer.sendOrderTimeoutMessage(new SeckillMessage(), 3);
//        seckillProducer.sendOrderTimeoutMessageWithDelay(new SeckillMessage(), 5000);
    }

}
