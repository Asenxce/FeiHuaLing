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
 * 诗词主表实体类（完整诗词信息）
 * 对应数据库表: t_poetry_master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_poetry_master", indexes = {
    @Index(name = "idx_title", columnList = "title", unique = false),
    @Index(name = "idx_author", columnList = "author", unique = false),
    @Index(name = "idx_dynasty", columnList = "dynasty", unique = false),
    @Index(name = "idx_poetry_type", columnList = "poetry_type", unique = false),
    @Index(name = "idx_usage_count", columnList = "usage_count", unique = false)
    // 注意：full_content_simplified是TEXT类型，不能创建普通索引
    // 如需全文搜索，请使用@javax.persistence.NamedNativeQuery或手动SQL创建FULLTEXT索引
})
public class PoetryMaster implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 诗题/词牌名
     */
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * 作者
     */
    @Column(nullable = false, length = 50)
    private String author;

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
     * 繁体完整内容(原始数据，带标点)
     */
    @Column(name = "full_content_traditional", columnDefinition = "TEXT")
    private String fullContentTraditional;

    /**
     * 简体完整内容(带标点，用于前端展示)
     */
    @Column(name = "full_content_simplified", columnDefinition = "TEXT")
    private String fullContentSimplified;

    /**
     * 诗词结构JSON
     * 格式: [{"line":"春眠不觉晓","order":1},{"line":"处处闻啼鸟","order":2}]
     */
    @Column(name = "poem_structure", columnDefinition = "JSON")
    private String poemStructure;

    /**
     * 总行数/句数
     */
    @Column(name = "line_count", columnDefinition = "INT")
    private Integer lineCount;

    /**
     * 总字符数(不含标点)
     */
    @Column(name = "total_chars", columnDefinition = "INT")
    private Integer totalChars;

    /**
     * 是否已验证: 0-未验证, 1-已验证
     */
    @Column(name = "is_verified", columnDefinition = "TINYINT UNSIGNED DEFAULT 1")
    private Short isVerified;

    /**
     * 被使用次数(统计热门诗词)
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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

    public String getFullContentTraditional() {
        return fullContentTraditional;
    }

    public void setFullContentTraditional(String fullContentTraditional) {
        this.fullContentTraditional = fullContentTraditional;
    }

    public String getFullContentSimplified() {
        return fullContentSimplified;
    }

    public void setFullContentSimplified(String fullContentSimplified) {
        this.fullContentSimplified = fullContentSimplified;
    }

    public String getPoemStructure() {
        return poemStructure;
    }

    public void setPoemStructure(String poemStructure) {
        this.poemStructure = poemStructure;
    }

    public Integer getLineCount() {
        return lineCount;
    }

    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
    }

    public Integer getTotalChars() {
        return totalChars;
    }

    public void setTotalChars(Integer totalChars) {
        this.totalChars = totalChars;
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
}