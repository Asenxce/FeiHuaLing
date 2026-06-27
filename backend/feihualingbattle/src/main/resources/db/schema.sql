-- ============================================================
-- 飞花令对战系统 — 数据库初始化脚本
-- 数据库: feihualingbattle
-- 字符集: utf8mb4  排序规则: utf8mb4_general_ci
-- 引擎: InnoDB
-- ============================================================

CREATE DATABASE IF NOT EXISTS `feihualingbattle`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `feihualingbattle`;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_user` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(50)     NOT NULL,
    `password`      VARCHAR(100)    NOT NULL,
    `nickname`      VARCHAR(50)     NOT NULL DEFAULT '',
    `identity_code` VARCHAR(8)      NOT NULL,
    `avatar_url`    VARCHAR(255)    NOT NULL DEFAULT '',
    `email`         VARCHAR(100)    DEFAULT NULL,
    `phone`         VARCHAR(20)     DEFAULT NULL,
    `bio`           VARCHAR(500)    DEFAULT NULL,
    `total_score`   INT             NOT NULL DEFAULT 0,
    `win_count`     INT             NOT NULL DEFAULT 0,
    `lose_count`    INT             NOT NULL DEFAULT 0,
    `draw_count`    INT             NOT NULL DEFAULT 0,
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_identity_code` (`identity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. 诗词主表（含全文索引）
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_poetry` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `content`         VARCHAR(500)    NOT NULL                COMMENT '诗句内容(简体无标点)',
    `author`          VARCHAR(50)     NOT NULL DEFAULT ''     COMMENT '作者',
    `title`           VARCHAR(500)    NOT NULL DEFAULT ''     COMMENT '诗题/词牌名',
    `dynasty`         VARCHAR(20)     NOT NULL DEFAULT ''     COMMENT '朝代',
    `poetry_type`     VARCHAR(20)     NOT NULL DEFAULT '唐诗' COMMENT '类型',
    `content_length`  SMALLINT        UNSIGNED                COMMENT '字符长度',
    `is_verified`     TINYINT         UNSIGNED DEFAULT 1      COMMENT '0-未验证 1-已验证',
    `usage_count`     INT             UNSIGNED DEFAULT 0      COMMENT '被使用次数',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `master_id`       BIGINT          DEFAULT NULL            COMMENT '关联t_poetry_master.id',
    PRIMARY KEY (`id`),
    INDEX `idx_author` (`author`),
    INDEX `idx_title` (`title`),
    INDEX `idx_usage_count` (`usage_count`),
    INDEX `idx_master_id` (`master_id`),
    -- 全文索引：content 单字段（MATCH ... AGAINST 查询）
    FULLTEXT INDEX `ft_content` (`content`) WITH PARSER ngram,
    -- 全文索引：content + author + title 多字段联合搜索
    FULLTEXT INDEX `ft_content_author_title` (`content`, `author`, `title`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. 诗词完整主表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_poetry_master` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
    `title`                    VARCHAR(500) NOT NULL,
    `author`                   VARCHAR(50)  NOT NULL,
    `dynasty`                  VARCHAR(20)  NOT NULL DEFAULT '',
    `poetry_type`              VARCHAR(20)  NOT NULL DEFAULT '唐诗',
    `full_content_traditional` TEXT         DEFAULT NULL COMMENT '繁体完整内容',
    `full_content_simplified`  TEXT         DEFAULT NULL COMMENT '简体完整内容',
    `poem_structure`           JSON         DEFAULT NULL COMMENT '诗词结构JSON',
    `line_count`               INT          DEFAULT NULL,
    `total_chars`              INT          DEFAULT NULL,
    `is_verified`              TINYINT      UNSIGNED DEFAULT 1,
    `usage_count`              INT          UNSIGNED DEFAULT 0,
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_title` (`title`),
    INDEX `idx_author` (`author`),
    INDEX `idx_dynasty` (`dynasty`),
    INDEX `idx_poetry_type` (`poetry_type`),
    INDEX `idx_usage_count` (`usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. 诗词关键字索引表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_poetry_keyword` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `poetry_id`   BIGINT       NOT NULL,
    `keyword`     VARCHAR(1)   NOT NULL COMMENT '关键字(单字)',
    `position`    INT          NOT NULL COMMENT '位置(从1开始)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_poetry_id` (`poetry_id`),
    INDEX `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. 关键字字典表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_keyword_dictionary` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `keyword`      VARCHAR(1)   NOT NULL,
    `type`         VARCHAR(20)  NOT NULL COMMENT 'color/number',
    `category`     VARCHAR(50)  DEFAULT NULL,
    `usage_count`  INT          NOT NULL DEFAULT 0,
    `is_active`    BOOLEAN      NOT NULL DEFAULT TRUE,
    `sort_order`   INT          NOT NULL DEFAULT 0,
    `description`  VARCHAR(200) DEFAULT NULL,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword_type` (`keyword`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 6. 对战表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_battle` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `battle_id`         VARCHAR(64)  NOT NULL,
    `battle_type`       VARCHAR(20)  NOT NULL              COMMENT 'ai/friend',
    `battle_sub_type`   VARCHAR(20)  DEFAULT NULL          COMMENT 'ONE_VS_ONE/MULTI',
    `game_mode`         VARCHAR(50)  NOT NULL,
    `keyword`           VARCHAR(10)  DEFAULT NULL,
    `keyword2`          VARCHAR(10)  DEFAULT NULL,
    `keyword_position`  INT          DEFAULT NULL,
    `color_keyword`     VARCHAR(10)  DEFAULT NULL,
    `number_keyword`    VARCHAR(10)  DEFAULT NULL,
    `forbidden_word`    VARCHAR(10)  DEFAULT NULL,
    `creator_id`        BIGINT       NOT NULL,
    `opponent_id`       BIGINT       DEFAULT NULL,
    `room_id`           VARCHAR(64)  DEFAULT NULL,
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'ONGOING',
    `game_type`         VARCHAR(20)  DEFAULT NULL          COMMENT 'ENTERTAINMENT/COMPETITIVE',
    `winner_id`         BIGINT       DEFAULT NULL,
    `time_limit`        INT          NOT NULL DEFAULT 60,
    `fault_limit`       TINYINT      NOT NULL DEFAULT 3,
    `total_rounds`      INT          NOT NULL DEFAULT 0,
    `total_players`     TINYINT      NOT NULL DEFAULT 2,
    `start_time`        DATETIME     DEFAULT NULL,
    `end_time`          DATETIME     DEFAULT NULL,
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_battle_id` (`battle_id`),
    INDEX `idx_creator_id` (`creator_id`),
    INDEX `idx_opponent_id` (`opponent_id`),
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. 对战回合表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_battle_round` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
    `battle_id`           VARCHAR(64)   NOT NULL,
    `room_id`             VARCHAR(64)   DEFAULT NULL,
    `user_id`             BIGINT        NOT NULL,
    `round_num`           INT           NOT NULL,
    `poem_content`        VARCHAR(500)  DEFAULT NULL,
    `poetry_id`           BIGINT        DEFAULT NULL,
    `time_used`           INT           DEFAULT NULL COMMENT '用时(秒)',
    `is_correct`          TINYINT       NOT NULL DEFAULT 0,
    `error_reason`        VARCHAR(200)  DEFAULT NULL,
    `fault_count`         TINYINT       DEFAULT NULL,
    `is_eliminated`       TINYINT(1)    NOT NULL DEFAULT 0,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_battle_id` (`battle_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_battle_user` (`battle_id`, `user_id`),
    INDEX `idx_battle_round` (`battle_id`, `round_num`),
    INDEX `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 7. 用户战绩表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_user_record` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`             BIGINT        NOT NULL,
    `battle_id`           VARCHAR(64)   NOT NULL,
    `room_id`             VARCHAR(64)   DEFAULT NULL,
    `battle_type`         VARCHAR(20)   NOT NULL,
    `game_mode`           VARCHAR(50)   NOT NULL,
    `game_mode_name`      VARCHAR(50)   DEFAULT NULL,
    `keyword`             VARCHAR(10)   DEFAULT NULL,
    `opponent_id`         BIGINT        DEFAULT NULL,
    `opponent_name`       VARCHAR(50)   DEFAULT NULL,
    `result`              VARCHAR(20)   DEFAULT NULL COMMENT 'WIN/LOSE/DRAW/TIMEOUT',
    `rank`                SMALLINT      DEFAULT NULL,
    `elimination_round`   INT           DEFAULT NULL,
    `is_eliminated`       TINYINT(1)    NOT NULL DEFAULT 0,
    `score`               SMALLINT      NOT NULL DEFAULT 0,
    `correct_count`       SMALLINT      UNSIGNED DEFAULT 0,
    `wrong_count`         SMALLINT      UNSIGNED DEFAULT 0,
    `total_count`         SMALLINT      UNSIGNED DEFAULT 0,
    `accuracy`            DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    `total_rounds`        SMALLINT      UNSIGNED DEFAULT 0,
    `avg_time_used`       DECIMAL(6,2)  DEFAULT NULL,
    `fastest_time`        SMALLINT      DEFAULT NULL,
    `duration`            INT           UNSIGNED DEFAULT NULL,
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_battle_id` (`battle_id`),
    INDEX `idx_result` (`result`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 8. 多人房间表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_room` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `room_id`             VARCHAR(64)  NOT NULL,
    `room_code`           CHAR(6)      NOT NULL,
    `creator_id`          BIGINT       NOT NULL,
    `game_mode`           VARCHAR(50)  NOT NULL,
    `keyword`             VARCHAR(10)  DEFAULT NULL,
    `keyword2`            VARCHAR(10)  DEFAULT NULL,
    `keyword_position`    INT          DEFAULT NULL,
    `color_keyword`       VARCHAR(10)  DEFAULT NULL,
    `number_keyword`      VARCHAR(10)  DEFAULT NULL,
    `forbidden_word`      VARCHAR(10)  DEFAULT NULL,
    `time_limit`          INT          NOT NULL DEFAULT 60,
    `max_players`         TINYINT      NOT NULL DEFAULT 4,
    `min_players`         TINYINT      NOT NULL DEFAULT 2,
    `fault_limit`         TINYINT      NOT NULL DEFAULT 3,
    `game_type`           VARCHAR(20)  NOT NULL DEFAULT 'ENTERTAINMENT',
    `poetry_scope`        VARCHAR(50)  DEFAULT NULL,
    `status`              VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
    `current_turn_user_id` BIGINT      DEFAULT NULL,
    `battle_id`           VARCHAR(64)  DEFAULT NULL,
    `expire_time`         DATETIME     NOT NULL,
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_id` (`room_id`),
    UNIQUE KEY `uk_room_code` (`room_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 9. 房间玩家表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_room_player` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `room_id`             VARCHAR(64)  NOT NULL,
    `user_id`             BIGINT       NOT NULL,
    `join_order`          TINYINT      NOT NULL DEFAULT 0,
    `is_host`             TINYINT(1)   NOT NULL DEFAULT 0,
    `is_ready`            TINYINT(1)   NOT NULL DEFAULT 0,
    `fault_count`         TINYINT      NOT NULL DEFAULT 3,
    `status`              VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
    `elimination_round`   INT          DEFAULT NULL,
    `join_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 10. 好友关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_friend` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `friend_id`   BIGINT      NOT NULL,
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '0-待确认 1-已好友 2-已拉黑',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 11. 用户收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_user_collection` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `poetry_id`      BIGINT       NOT NULL,
    `title`          VARCHAR(100) DEFAULT NULL,
    `author`         VARCHAR(50)  DEFAULT NULL,
    `dynasty`        VARCHAR(20)  DEFAULT NULL,
    `content`        VARCHAR(500) DEFAULT NULL,
    `full_content`   TEXT         DEFAULT NULL,
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_poetry` (`user_id`, `poetry_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 12. 邮件/通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_mail` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `title`       VARCHAR(200) NOT NULL,
    `content`     TEXT         DEFAULT NULL,
    `sender`      VARCHAR(100) DEFAULT NULL,
    `send_time`   DATETIME     NOT NULL,
    `type`        INT          NOT NULL COMMENT '1-系统通知 2-好友申请 3-对战邀请',
    `is_read`     BOOLEAN      NOT NULL DEFAULT FALSE,
    `related_id`  BIGINT       DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 13. 对战邀请表
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_battle_invite` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `invite_token`    VARCHAR(64)  NOT NULL,
    `inviter_id`      BIGINT       NOT NULL,
    `inviter_name`    VARCHAR(50)  DEFAULT NULL,
    `invitee_id`      BIGINT       NOT NULL,
    `invitee_name`    VARCHAR(50)  DEFAULT NULL,
    `invite_type`     VARCHAR(20)  NOT NULL DEFAULT 'BATTLE',
    `battle_type`     VARCHAR(20)  NOT NULL DEFAULT 'friend',
    `game_mode`       VARCHAR(50)  DEFAULT NULL,
    `keyword`         VARCHAR(10)  DEFAULT NULL,
    `keyword2`        VARCHAR(10)  DEFAULT NULL,
    `time_limit`      SMALLINT     NOT NULL DEFAULT 60,
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    `expire_time`     DATETIME     NOT NULL,
    `accepted_time`   DATETIME     DEFAULT NULL,
    `rejected_time`   DATETIME     DEFAULT NULL,
    `battle_id`       VARCHAR(64)  DEFAULT NULL,
    `room_id`         VARCHAR(64)  DEFAULT NULL,
    `room_code`       VARCHAR(6)   DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_token` (`invite_token`),
    INDEX `idx_inviter_id` (`inviter_id`),
    INDEX `idx_invitee_id` (`invitee_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
