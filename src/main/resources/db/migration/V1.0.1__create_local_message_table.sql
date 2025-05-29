CREATE TABLE local_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    topic VARCHAR(64) NOT NULL COMMENT '消息主题',
    content TEXT NOT NULL COMMENT '消息内容',
    status VARCHAR(20) NOT NULL COMMENT '消息状态：PENDING-待发送，SENT-已发送，FAILED-发送失败，RETRYING-重试中，RETRY_FAILED-重试失败，FINAL_FAILED-最终失败',
    retry_times INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_create_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表'; 