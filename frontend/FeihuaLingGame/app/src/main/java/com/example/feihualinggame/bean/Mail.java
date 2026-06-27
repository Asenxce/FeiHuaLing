package com.example.feihualinggame.bean;

/**
 * 邮件数据模型
 */
public class Mail {
    private Long id;              // 邮件ID
    private String title;           // 标题
    private String content;         // 内容
    private String sender;          // 发件人（系统/好友名）
    private String sendTime;        // 发送时间
    private int type;               // 类型：1-系统通知，2-好友申请，3-对战申请
    private boolean isRead;         // 是否已读
    private Long relatedId;       // 关联ID（好友ID或对战ID）

    public Mail() {
    }

    public Mail(Long id, String title, String content, String sender, String sendTime, int type, boolean isRead) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.sender = sender;
        this.sendTime = sendTime;
        this.type = type;
        this.isRead = isRead;
    }

    // Getters and Setters
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    /**
     * 获取类型图标
     */
    public String getTypeIcon() {
        switch (type) {
            case 1:
                return "[系统]";
            case 2:
                return "[好友]";  // 好友申请
            case 3:
                return "[对战]";  // 对战申请
            default:
                return "[邮件]";
        }
    }
}
