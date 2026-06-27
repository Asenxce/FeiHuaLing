package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class MultiBattleSubmitRequest {
    private String battleId;
    private String roomId;
    private Long userId;
    private String poemContent;

    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPoemContent() { return poemContent; }
    public void setPoemContent(String poemContent) { this.poemContent = poemContent; }
}
