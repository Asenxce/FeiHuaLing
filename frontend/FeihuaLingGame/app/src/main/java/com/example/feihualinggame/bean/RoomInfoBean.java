package com.example.feihualinggame.bean;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoomInfoBean {
    @SerializedName("roomId")
    private String roomId;
    @SerializedName("roomCode")
    private String roomCode;
    @SerializedName("creatorId")
    private long creatorId;
    @SerializedName("gameMode")
    private String gameMode;
    @SerializedName("keyword")
    private String keyword;
    @SerializedName("keyword2")
    private String keyword2;
    @SerializedName("keywordPosition")
    private Integer keywordPosition;
    @SerializedName("colorKeyword")
    private String colorKeyword;
    @SerializedName("numberKeyword")
    private String numberKeyword;
    @SerializedName("forbiddenWord")
    private String forbiddenWord;
    @SerializedName("timeLimit")
    private int timeLimit;
    @SerializedName("maxPlayers")
    private int maxPlayers;
    @SerializedName("minPlayers")
    private int minPlayers;
    @SerializedName("faultLimit")
    private int faultLimit;
    @SerializedName("gameType")
    private String gameType;
    @SerializedName("poetryScope")
    private String poetryScope;
    @SerializedName("status")
    private String status;
    @SerializedName("currentTurnUserId")
    private long currentTurnUserId;
    @SerializedName("battleId")
    private String battleId;
    @SerializedName("expireTime")
    private String expireTime;
    @SerializedName("createTime")
    private String createTime;
    @SerializedName("playerCount")
    private int playerCount;
    @SerializedName("readyCount")
    private int readyCount;
    @SerializedName("players")
    private List<RoomPlayerBean> players;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public long getCreatorId() { return creatorId; }
    public void setCreatorId(long creatorId) { this.creatorId = creatorId; }
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
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    public int getFaultLimit() { return faultLimit; }
    public void setFaultLimit(int faultLimit) { this.faultLimit = faultLimit; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public String getPoetryScope() { return poetryScope; }
    public void setPoetryScope(String poetryScope) { this.poetryScope = poetryScope; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCurrentTurnUserId() { return currentTurnUserId; }
    public void setCurrentTurnUserId(long currentTurnUserId) { this.currentTurnUserId = currentTurnUserId; }
    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }
    public String getExpireTime() { return expireTime; }
    public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public int getReadyCount() { return readyCount; }
    public void setReadyCount(int readyCount) { this.readyCount = readyCount; }
    public List<RoomPlayerBean> getPlayers() { return players; }
    public void setPlayers(List<RoomPlayerBean> players) { this.players = players; }
}
