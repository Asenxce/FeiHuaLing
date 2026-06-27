package com.example.feihualingbattle.dto;

import com.example.feihualingbattle.entity.PoetryMaster;
import lombok.Data;

@Data
public class PoetryMasterResponse {
    private Long id;
    private String title;
    private String author;
    private String dynasty;
    private String fullContent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDynasty() { return dynasty; }
    public void setDynasty(String dynasty) { this.dynasty = dynasty; }
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }

    public static PoetryMasterResponse fromEntity(PoetryMaster master) {
        PoetryMasterResponse response = new PoetryMasterResponse();
        response.setId(master.getId());
        response.setTitle(master.getTitle());
        response.setAuthor(master.getAuthor());
        response.setDynasty(master.getDynasty());
        response.setFullContent(master.getFullContentSimplified());
        return response;
    }
}
