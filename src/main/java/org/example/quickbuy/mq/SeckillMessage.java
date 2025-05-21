package org.example.quickbuy.mq;

import lombok.Data;
import java.io.Serializable;

@Data
public class SeckillMessage implements Serializable {
    private Long userId;
    private Long activityId;
    private Long productId;
    private Integer stock;
    private String orderNo;
} 