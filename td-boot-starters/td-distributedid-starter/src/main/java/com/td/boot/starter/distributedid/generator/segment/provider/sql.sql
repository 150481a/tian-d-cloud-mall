-- SQL 腳本示例 (MySQL)
CREATE TABLE `t_segment_id_biz` (
`biz_key` VARCHAR(64) NOT NULL COMMENT '業務鍵',
`max_id` BIGINT NOT NULL COMMENT '當前最大 ID 值',
`step` INT NOT NULL DEFAULT 1000 COMMENT '號段步長',
`description` VARCHAR(255) NULL COMMENT '描述',
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`biz_key`)
) COMMENT='號段模式ID配置表';

-- 插入示例數據
INSERT INTO `t_segment_id_biz` (`biz_key`, `max_id`, `step`, `description`) VALUES ('order_id', 0, 1000, '訂單ID');
INSERT INTO `t_segment_id_biz` (`biz_key`, `max_id`, `step`, `description`) VALUES ('user_id', 0, 500, '用戶ID');