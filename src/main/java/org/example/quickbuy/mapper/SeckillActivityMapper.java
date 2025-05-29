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
    @Select("SELECT * FROM seckill_activity WHERE id = #{id} AND status = 1")
    SeckillActivity selectById(Long id);

    /**
     * 更新秒杀活动状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新秒杀活动库存
     */
    @Update("UPDATE seckill_activity SET available_stock = available_stock - #{stock} WHERE id = #{activityId} AND available_stock >= #{stock}")
    int updateStock(@Param("activityId") Long activityId, @Param("stock") Integer stock);

    /**
     * 查询活动的当前库存
     * @param activityId 活动ID
     * @return 当前库存数量
     */
    @Select("SELECT available_stock FROM seckill_activity WHERE id = #{activityId}")
    Integer getStock(@Param("activityId") Long activityId);
} 