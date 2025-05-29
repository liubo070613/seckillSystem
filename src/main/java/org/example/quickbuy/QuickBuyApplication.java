package org.example.quickbuy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 启用定时任务调度
public class QuickBuyApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickBuyApplication.class, args);
    }

}
