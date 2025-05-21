package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.quickbuy.entity.Product;

@Mapper
public interface ProductMapper {
    Product selectById(@Param("id") Long id);
    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);
} 