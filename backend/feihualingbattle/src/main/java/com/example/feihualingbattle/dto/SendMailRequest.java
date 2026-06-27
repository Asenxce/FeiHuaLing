package com.example.feihualingbattle.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SendMailRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String identityCode;

    @Deprecated
    private Long userId;

    private String title;
    private String content;
    private String sender;
    private Integer type;
    private Long relatedId;

    public String getIdentityCode() { return identityCode; }
    public void setIdentityCode(String identityCode) { this.identityCode = identityCode; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
}
