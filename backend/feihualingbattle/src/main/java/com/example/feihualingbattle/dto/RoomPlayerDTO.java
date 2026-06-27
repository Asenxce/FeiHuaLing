package com.example.feihualingbattle.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoomPlayerDTO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String identityCode;
    private Integer joinOrder;
    private Boolean isHost;
    private Boolean isReady;
    private Integer faultCount;
    private String status;
    private Integer eliminationRound;
    private LocalDateTime joinTime;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getIdentityCode() { return identityCode; }
    public void setIdentityCode(String identityCode) { this.identityCode = identityCode; }
    public Integer getJoinOrder() { return joinOrder; }
    public void setJoinOrder(Integer joinOrder) { this.joinOrder = joinOrder; }
    public Boolean getIsHost() { return isHost; }
    public void setIsHost(Boolean isHost) { this.isHost = isHost; }
    public Boolean getIsReady() { return isReady; }
    public void setIsReady(Boolean isReady) { this.isReady = isReady; }
    public Integer getFaultCount() { return faultCount; }
    public void setFaultCount(Integer faultCount) { this.faultCount = faultCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEliminationRound() { return eliminationRound; }
    public void setEliminationRound(Integer eliminationRound) { this.eliminationRound = eliminationRound; }
    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }
}
