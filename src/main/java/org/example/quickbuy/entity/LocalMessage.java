package org.example.quickbuy.entity;

import lombok.Data;
import java.util.Date;

/**
 * 本地消息表实体类
 * 用于记录和管理消息队列中的消息状态，确保消息的可靠性投递
 */
@Data
public class LocalMessage {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 消息ID，全局唯一标识符
     */
    private String messageId;
    
    /**
     * 消息主题，表示消息的类型或分类
     */
    private String topic;
    
    /**
     * 消息内容，通常为JSON格式的字符串
     */
    private String content;
    
    /**
     * 消息状态
     * PENDING-待发送，SENT-已发送，FAILED-发送失败，RETRYING-重试中，RETRY_FAILED-重试失败
     */
    private String status;
    
    /**
     * 重试次数
     */
    private Integer retryTimes;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
} 