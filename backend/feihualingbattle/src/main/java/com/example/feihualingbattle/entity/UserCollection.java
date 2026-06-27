package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏表实体类
 * 对应数据库表: t_user_collection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_user_collection", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "poetry_id"})
})
public class UserCollection implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 诗词ID（关联t_poetry.id）
     */
    @Column(name = "poetry_id", nullable = false)
    private Long poetryId;

    /**
     * 诗题（冗余字段，方便查询）
     */
    @Column(length = 100)
    private String title;

    /**
     * 作者（冗余字段）
     */
    @Column(length = 50)
    private String author;

    /**
     * 朝代（冗余字段）
     */
    @Column(length = 20)
    private String dynasty;

    /**
     * 诗句内容（简体无标点）
     */
    @Column(length = 500)
    private String content;

    /**
     * 完整诗词内容
     */
    @Column(name = "full_content", columnDefinition = "TEXT")
    private String fullContent;

    /**
     * 收藏时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPoetryId() {
        return poetryId;
    }

    public void setPoetryId(Long poetryId) {
        this.poetryId = poetryId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFullContent() {
        return fullContent;
    }

    public void setFullContent(String fullContent) {
        this.fullContent = fullContent;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
