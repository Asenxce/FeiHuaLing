package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class JoinRoomRequest {
    private Long userId;
    private String roomCode;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
}
