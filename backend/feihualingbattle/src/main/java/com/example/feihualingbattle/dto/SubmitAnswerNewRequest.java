package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class SubmitAnswerNewRequest {
    private String gameMode;
    private String keyword;
    private String keyword2;
    private String colorKeyword;
    private String numberKeyword;
    private String forbiddenWord;
    private Integer keywordPosition;
    private String lastChar;
    private String answer;
    private String battleId;

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getKeyword2() { return keyword2; }
    public void setKeyword2(String keyword2) { this.keyword2 = keyword2; }
    public String getColorKeyword() { return colorKeyword; }
    public void setColorKeyword(String colorKeyword) { this.colorKeyword = colorKeyword; }
    public String getNumberKeyword() { return numberKeyword; }
    public void setNumberKeyword(String numberKeyword) { this.numberKeyword = numberKeyword; }
    public String getForbiddenWord() { return forbiddenWord; }
    public void setForbiddenWord(String forbiddenWord) { this.forbiddenWord = forbiddenWord; }
    public Integer getKeywordPosition() { return keywordPosition; }
    public String getLastChar() { return lastChar; }
    public void setLastChar(String lastChar) { this.lastChar = lastChar; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }
}
