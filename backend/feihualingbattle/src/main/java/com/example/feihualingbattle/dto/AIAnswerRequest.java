package com.example.feihualingbattle.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIAnswerRequest {
    private String gameMode;
    private String keyword;
    private String keyword2;
    private String colorKeyword;
    private String forbiddenWord;
    private Integer keywordPosition;
    private String lastChar;
    private List<String> usedPoems;

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getKeyword2() { return keyword2; }
    public void setKeyword2(String keyword2) { this.keyword2 = keyword2; }
    public String getColorKeyword() { return colorKeyword; }
    public void setColorKeyword(String colorKeyword) { this.colorKeyword = colorKeyword; }
    public String getForbiddenWord() { return forbiddenWord; }
    public void setForbiddenWord(String forbiddenWord) { this.forbiddenWord = forbiddenWord; }
    public Integer getKeywordPosition() { return keywordPosition; }
    public void setKeywordPosition(Integer keywordPosition) { this.keywordPosition = keywordPosition; }
    public String getLastChar() { return lastChar; }
    public void setLastChar(String lastChar) { this.lastChar = lastChar; }
    public List<String> getUsedPoems() { return usedPoems; }
    public void setUsedPoems(List<String> usedPoems) { this.usedPoems = usedPoems; }
}
