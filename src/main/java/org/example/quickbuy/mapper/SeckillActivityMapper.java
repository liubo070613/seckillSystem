package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.quickbuy.entity.SeckillActivity;

@Mapper
public interface SeckillActivityMapper {
    /**
     * 插入秒杀活动
     */
    int insert(SeckillActivity activity);

    /**
     * 根据ID查询秒杀活动
     */
    SeckillActivity selectById(@Param("id") Long id);

    /**
     * 更新秒杀活动状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新秒杀活动库存
     */
    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);
} 