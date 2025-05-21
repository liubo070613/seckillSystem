package org.example.quickbuy.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long productId;
    private BigDecimal amount;
    private Integer status;  // 0-待支付 1-已支付 2-已取消
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime payTime;
} 