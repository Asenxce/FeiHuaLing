package com.example.feihualinggame.bean;

import java.io.Serializable;

public class Poetry implements Serializable {
    private long id;           // 诗词ID（收藏使用）
    private String content;      // 诗句内容（单句）
    private String fullContent;  // 完整诗词内容
    private String author;       // 作者
    private String title;        // 标题
    private String dynasty;      // 朝代

    public Poetry() {}

    public Poetry(String content, String author, String title) {
        this.content = content;
        this.author = author;
        this.title = title;
    }

    public Poetry(String fullContent, String author, String title, String dynasty) {
        this.fullContent = fullContent;
        this.author = author;
        this.title = title;
        this.dynasty = dynasty;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // Getter & Setter
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
}