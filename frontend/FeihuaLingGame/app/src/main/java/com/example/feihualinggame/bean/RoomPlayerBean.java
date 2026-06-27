package com.example.feihualinggame.bean;

import com.google.gson.annotations.SerializedName;

public class RoomPlayerBean {
    @SerializedName("userId")
    private long userId;
    @SerializedName("nickname")
    private String nickname;
    @SerializedName("avatarUrl")
    private String avatarUrl;
    @SerializedName("identityCode")
    private String identityCode;
    @SerializedName("joinOrder")
    private int joinOrder;
    @SerializedName("isHost")
    private boolean isHost;
    @SerializedName("isReady")
    private boolean isReady;
    @SerializedName("faultCount")
    private int faultCount;
    @SerializedName("status")
    private String status;
    @SerializedName("eliminationRound")
    private Integer eliminationRound;
    @SerializedName("joinTime")
    private String joinTime;

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getIdentityCode() { return identityCode; }
    public void setIdentityCode(String identityCode) { this.identityCode = identityCode; }
    public int getJoinOrder() { return joinOrder; }
    public void setJoinOrder(int joinOrder) { this.joinOrder = joinOrder; }
    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { isReady = ready; }
    public int getFaultCount() { return faultCount; }
    public void setFaultCount(int faultCount) { this.faultCount = faultCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEliminationRound() { return eliminationRound; }
    public void setEliminationRound(Integer eliminationRound) { this.eliminationRound = eliminationRound; }
    public String getJoinTime() { return joinTime; }
    public void setJoinTime(String joinTime) { this.joinTime = joinTime; }
}
