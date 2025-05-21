package org.example.quickbuy.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivity {
    public static final int STATUS_NOT_STARTED = 0;  // 未开始
    public static final int STATUS_IN_PROGRESS = 1;  // 进行中
    public static final int STATUS_ENDED = 2;        // 已结束

    private Long id;
    private Long productId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 