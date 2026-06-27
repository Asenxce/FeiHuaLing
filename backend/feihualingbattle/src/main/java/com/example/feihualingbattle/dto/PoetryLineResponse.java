package com.example.feihualingbattle.dto;

import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.entity.PoetryMaster;
import lombok.Data;

@Data
public class PoetryLineResponse {
    private String content;
    private String fullContent;
    private String author;
    private String title;
    private String dynasty;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDynasty() { return dynasty; }
    public void setDynasty(String dynasty) { this.dynasty = dynasty; }

    public static PoetryLineResponse fromEntity(Poetry poetry) {
        PoetryLineResponse response = new PoetryLineResponse();
        response.setContent(poetry.getContent());
        response.setAuthor(poetry.getAuthor());
        response.setTitle(poetry.getTitle());
        return response;
    }

    public static PoetryLineResponse fromEntities(Poetry poetry, PoetryMaster master) {
        PoetryLineResponse response = new PoetryLineResponse();
        response.setContent(poetry.getContent());
        response.setAuthor(poetry.getAuthor());
        response.setTitle(poetry.getTitle());
        if (master != null) {
            response.setFullContent(master.getFullContentSimplified());
            response.setDynasty(master.getDynasty());
        }
        return response;
    }
}
