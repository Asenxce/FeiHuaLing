package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 诗词关键字索引实体类
 * 对应数据库表: t_poetry_keyword
 * 用于快速检索包含特定关键字的诗词
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_poetry_keyword")
public class PoetryKeyword implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 诗词ID
     */
    @Column(name = "poetry_id", nullable = false)
    private Long poetryId;

    /**
     * 关键字
     */
    @Column(nullable = false, length = 1)
    private String keyword;

    /**
     * 关键字位置(从1开始)
     */
    @Column(nullable = false)
    private Integer position;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public PoetryKeyword(Long poetryId, String keyword, Integer position) {
        this.poetryId = poetryId;
        this.keyword = keyword;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPoetryId() {
        return poetryId;
    }

    public void setPoetryId(Long poetryId) {
        this.poetryId = poetryId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
