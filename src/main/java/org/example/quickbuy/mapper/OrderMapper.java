package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.quickbuy.entity.Order;
import java.time.LocalDateTime;
import java.util.List;

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

    //根据订单更新订单
    int updateOrder(@Param("order") Order order);

    /**
     * 查询超时未支付的订单
     * @param timeoutThreshold 超时时间阈值
     * @param status 订单状态
     * @return 超时订单列表
     */
    List<Order> selectTimeoutOrders(@Param("timeoutThreshold") LocalDateTime timeoutThreshold,
                                  @Param("status") Integer status);
} 