package org.example.quickbuy;

import org.example.quickbuy.entity.User;
import org.example.quickbuy.mapper.UserMapper;
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

    }

}
