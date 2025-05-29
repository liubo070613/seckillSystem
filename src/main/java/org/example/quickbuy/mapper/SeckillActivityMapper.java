package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
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
    SeckillActivity selectById(Long id);

    /**
     * 更新秒杀活动状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新秒杀活动库存
     */
    int updateStock(@Param("activityId") Long activityId, @Param("stock") Integer stock);

    /**
     * 查询活动的当前库存
     * @param activityId 活动ID
     * @return 当前库存数量
     */
    Integer getStock(@Param("activityId") Long activityId);
} 