package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class AddCollectionRequest {
    private Long poetryId;
    private String title;
    private String author;
    private String dynasty;
    private String content;
    private String fullContent;

    public Long getPoetryId() { return poetryId; }
    public void setPoetryId(Long poetryId) { this.poetryId = poetryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDynasty() { return dynasty; }
    public void setDynasty(String dynasty) { this.dynasty = dynasty; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
}
