package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.quickbuy.entity.Order;
import java.time.LocalDateTime;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单
     */
    int insert(Order order);

    /**
     * 根据订单号查询订单
     */
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("orderNo") String orderNo, @Param("status") Integer status);

    /**
     * 更新支付时间
     */
    int updatePayTime(@Param("orderNo") String orderNo, @Param("payTime") LocalDateTime payTime);
} 