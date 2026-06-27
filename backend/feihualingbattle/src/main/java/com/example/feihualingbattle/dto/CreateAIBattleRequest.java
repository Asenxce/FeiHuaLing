package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class CreateAIBattleRequest {
    private Long userId;
    private String gameMode;
    private String keyword;
    private String keyword2;
    private String colorKeyword;
    private String numberKeyword;
    private String forbiddenWord;
    private Integer keywordPosition;
    private Integer timeLimit;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
}
