package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.quickbuy.entity.Order;

@Mapper
public interface OrderMapper {
    int insert(Order order);
    Order selectByOrderNo(@Param("orderNo") String orderNo);
} 