package com.example.feihualinggame.bean;

public class Friend {
    private Long userId;         // 好友用户ID
    private String username;     // 好友用户名
    private String nickname;     // 好友昵称
    private String avatarUrl;    // 好友头像URL
    private int winCount;       // 胜利次数
    private int loseCount;      // 失败次数
    private boolean online;     // 是否在线

    // 无参构造函数
    public Friend() {
    }
    
    // 带参数的构造函数
    public Friend(String username, int winCount, int loseCount, boolean online) {
        this.username = username;
        this.winCount = winCount;
        this.loseCount = loseCount;
        this.online = online;
    }

    // Getter & Setter
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getLoseCount() {
        return loseCount;
    }

    public void setLoseCount(int loseCount) {
        this.loseCount = loseCount;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}