-- ============================================================
-- 迁移脚本：为 t_poetry 表添加 FULLTEXT 全文索引
-- 执行前请先备份数据
-- ============================================================

USE `feihualingbattle`;

-- 1. 删除旧的 B-tree 前缀索引（与 FULLTEXT 冲突）
-- MySQL 不支持 DROP INDEX IF EXISTS，用存储过程处理
DROP PROCEDURE IF EXISTS `drop_index_if_exists`;
DELIMITER //
CREATE PROCEDURE `drop_index_if_exists`(IN tableName VARCHAR(64), IN indexName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = tableName
          AND index_name = indexName
        LIMIT 1
    ) THEN
        SET @sql = CONCAT('DROP INDEX `', indexName, '` ON `', tableName, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL `drop_index_if_exists`('t_poetry', 'idx_content_prefix');
DROP PROCEDURE IF EXISTS `drop_index_if_exists`;

-- 2. 添加 FULLTEXT 全文索引（content 单字段，ngram 分词）
-- 先检查是否已存在，避免重复添加报错
SET @ft_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_poetry'
      AND index_name = 'ft_content'
);
SET @sql = IF(@ft_exists = 0,
    'ALTER TABLE `t_poetry` ADD FULLTEXT INDEX `ft_content` (`content`) WITH PARSER ngram',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加联合 FULLTEXT 全文索引（content + author + title）
SET @ft2_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 't_poetry'
      AND index_name = 'ft_content_author_title'
);
SET @sql = IF(@ft2_exists = 0,
    'ALTER TABLE `t_poetry` ADD FULLTEXT INDEX `ft_content_author_title` (`content`, `author`, `title`) WITH PARSER ngram',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
