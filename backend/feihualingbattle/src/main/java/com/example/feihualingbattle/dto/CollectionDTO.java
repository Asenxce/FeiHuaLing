package com.example.feihualingbattle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 收藏DTO - 包含收藏信息和诗词信息
 */
@Data
@NoArgsConstructor
public class CollectionDTO {
    private Long id;
    private Long userId;
    private Long poetryId;
    private LocalDateTime createTime;
    
    // 诗词信息（从t_poetry表获取）
    private String title;
    private String author;
    private String dynasty;
    private String content;
    private String poetryType;
    private Long masterId;

    /**
     * 完整诗词内容（兼容前端，实际使用content字段）
     */
    @JsonProperty("fullContent")
    public String getFullContent() {
        return content;
    }

    /**
     * JPQL构造函数 - 参数顺序必须与查询语句一致
     */
    public CollectionDTO(Long id, Long userId, Long poetryId, LocalDateTime createTime,
                         String title, String author, String dynasty, String content,
                         String poetryType, Long masterId) {
        this.id = id;
        this.userId = userId;
        this.poetryId = poetryId;
        this.createTime = createTime;
        this.title = title;
        this.author = author;
        this.dynasty = dynasty;
        this.content = content;
        this.poetryType = poetryType;
        this.masterId = masterId;
    }
}
