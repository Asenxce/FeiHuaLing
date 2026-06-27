package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class CreateFriendBattleRequest {
    private Long creatorId;
    private Long opponentId;
    private String gameMode;
    private String keyword;
    private String keyword2;
    private String colorKeyword;
    private String numberKeyword;
    private String forbiddenWord;
    private Integer keywordPosition;
    private Integer timeLimit;

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getOpponentId() { return opponentId; }
    public void setOpponentId(Long opponentId) { this.opponentId = opponentId; }
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getKeyword2() { return keyword2; }
    public void setKeyword2(String keyword2) { this.keyword2 = keyword2; }
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
}
