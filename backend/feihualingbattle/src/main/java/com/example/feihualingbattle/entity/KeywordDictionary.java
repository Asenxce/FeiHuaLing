package com.example.feihualingbattle.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 飞花令关键字字典实体类
 * 用于存储颜色和数字关键字,支持动态管理
 */
@Entity
@Table(name = "t_keyword_dictionary", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"keyword", "type"}, name = "uk_keyword_type"))
public class KeywordDictionary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 1, nullable = false)
    private String keyword;
    
    @Column(length = 20, nullable = false)
    private String type; // color 或 number
    
    @Column(length = 50)
    private String category; // 细分分类
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(length = 200)
    private String description;
    
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    // Constructors
    public KeywordDictionary() {}
    
    public KeywordDictionary(String keyword, String type, String category, String description) {
        this.keyword = keyword;
        this.type = type;
        this.category = category;
        this.description = description;
        this.usageCount = 0;
        this.isActive = true;
        this.sortOrder = 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Integer getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    @Override
    public String toString() {
        return "KeywordDictionary{" +
                "id=" + id +
                ", keyword='" + keyword + '\'' +
                ", type='" + type + '\'' +
                ", category='" + category + '\'' +
                ", usageCount=" + usageCount +
                ", isActive=" + isActive +
                ", sortOrder=" + sortOrder +
                ", description='" + description + '\'' +
                '}';
    }
}
