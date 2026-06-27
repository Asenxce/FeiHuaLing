package com.example.feihualingbattle.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoomDetailDTO {
    private String roomId;
    private String roomCode;
    private Long creatorId;
    private String gameMode;
    private String keyword;
    private String keyword2;
    private Integer keywordPosition;
    private String colorKeyword;
    private String numberKeyword;
    private String forbiddenWord;
    private Integer timeLimit;
    private Integer maxPlayers;
    private Integer minPlayers;
    private Integer faultLimit;
    private String gameType;
    private String poetryScope;
    private String status;
    private Long currentTurnUserId;
    private String battleId;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private int playerCount;
    private int readyCount;
    private List<RoomPlayerDTO> players;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getKeyword2() { return keyword2; }
    public void setKeyword2(String keyword2) { this.keyword2 = keyword2; }
    public Integer getKeywordPosition() { return keywordPosition; }
    public void setKeywordPosition(Integer keywordPosition) { this.keywordPosition = keywordPosition; }
    public String getColorKeyword() { return colorKeyword; }
    public void setColorKeyword(String colorKeyword) { this.colorKeyword = colorKeyword; }
    public String getNumberKeyword() { return numberKeyword; }
    public void setNumberKeyword(String numberKeyword) { this.numberKeyword = numberKeyword; }
    public String getForbiddenWord() { return forbiddenWord; }
    public void setForbiddenWord(String forbiddenWord) { this.forbiddenWord = forbiddenWord; }
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public Integer getMinPlayers() { return minPlayers; }
    public void setMinPlayers(Integer minPlayers) { this.minPlayers = minPlayers; }
    public Integer getFaultLimit() { return faultLimit; }
    public void setFaultLimit(Integer faultLimit) { this.faultLimit = faultLimit; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public String getPoetryScope() { return poetryScope; }
    public void setPoetryScope(String poetryScope) { this.poetryScope = poetryScope; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCurrentTurnUserId() { return currentTurnUserId; }
    public void setCurrentTurnUserId(Long currentTurnUserId) { this.currentTurnUserId = currentTurnUserId; }
    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public int getReadyCount() { return readyCount; }
    public void setReadyCount(int readyCount) { this.readyCount = readyCount; }
    public List<RoomPlayerDTO> getPlayers() { return players; }
    public void setPlayers(List<RoomPlayerDTO> players) { this.players = players; }
}
