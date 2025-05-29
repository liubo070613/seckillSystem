package org.example.quickbuy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.quickbuy.entity.LocalMessage;
import java.util.Date;
import java.util.List;

@Mapper
public interface LocalMessageMapper {
    
    /**
     * 插入消息
     */
    int insert(LocalMessage message);
    
    /**
     * 更新消息状态
     */
    int updateStatus(@Param("messageId") String messageId, 
                    @Param("status") String status,
                    @Param("updateTime") Date updateTime);
    
    /**
     * 查询失败的消息
     */
    List<LocalMessage> selectFailedMessages();
    
    /**
     * 根据消息ID查询消息
     */
    LocalMessage selectByMessageId(@Param("messageId") String messageId);
    
    /**
     * 增加重试次数
     */
    int incrementRetryTimes(@Param("messageId") String messageId);
    
    /**
     * 查询最终失败的消息
     */
    List<LocalMessage> selectFinalFailedMessages(@Param("startTime") Date startTime);
} 