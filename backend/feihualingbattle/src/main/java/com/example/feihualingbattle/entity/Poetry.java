package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 诗词库表实体类
 * 对应数据库表: t_poetry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_poetry", indexes = {
    @Index(name = "idx_author", columnList = "author", unique = false),
    @Index(name = "idx_title", columnList = "title", unique = false),
    @Index(name = "idx_usage_count", columnList = "usage_count", unique = false),
    @Index(name = "idx_master_id", columnList = "master_id", unique = false)
    // FULLTEXT 索引(ft_content, ft_content_author_title) 通过 DDL 脚本管理，JPA 不支持 FULLTEXT 声明
})
public class Poetry implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 诗句内容(简体无标点)
     */
    @Column(nullable = false, length = 500)
    private String content;

    /**
     * 作者
     */
    @Column(length = 50, columnDefinition = "VARCHAR(50) DEFAULT ''")
    private String author;

    /**
     * 诗题/词牌名
     */
    @Column(length = 500, columnDefinition = "VARCHAR(500) DEFAULT ''")
    private String title;

    /**
     * 朝代: 唐/宋/元/明/清
     */
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT ''")
    private String dynasty;

    /**
     * 类型: 唐诗/宋词/其他
     */
    @Column(name = "poetry_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT '唐诗'")
    private String poetryType;

    /**
     * 诗句字符长度
     */
    @Column(name = "content_length", columnDefinition = "SMALLINT UNSIGNED")
    private Short contentLength;

    /**
     * 是否已验证: 0-未验证, 1-已验证
     */
    @Column(name = "is_verified", columnDefinition = "TINYINT UNSIGNED DEFAULT 1")
    private Short isVerified;

    /**
     * 被使用次数(统计热门诗句)
     */
    @Column(name = "usage_count", columnDefinition = "INT UNSIGNED DEFAULT 0")
    private Integer usageCount;

    /**
     * 入库时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 关联t_poetry_master.id(诗词主表ID)
     */
    @Column(name = "master_id")
    private Long masterId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDynasty() {
        return dynasty;
    }

    public void setDynasty(String dynasty) {
        this.dynasty = dynasty;
    }

    public String getPoetryType() {
        return poetryType;
    }

    public void setPoetryType(String poetryType) {
        this.poetryType = poetryType;
    }

    public Short getContentLength() {
        return contentLength;
    }

    public void setContentLength(Short contentLength) {
        this.contentLength = contentLength;
    }

    public Short getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Short isVerified) {
        this.isVerified = isVerified;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getMasterId() {
        return masterId;
    }

    public void setMasterId(Long masterId) {
        this.masterId = masterId;
    }
}