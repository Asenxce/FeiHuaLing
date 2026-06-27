package com.example.feihualingbattle.dto;

import lombok.Data;
import java.util.Map;

@Data
public class EndBattleRequest {
    private Long winnerId;
    private Integer answerCount;
    private Integer wrongCount;
    private Boolean isWin;
    private Map<Long, Integer> playerStats;

    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
    public Integer getAnswerCount() { return answerCount; }
    public void setAnswerCount(Integer answerCount) { this.answerCount = answerCount; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public Boolean getIsWin() { return isWin; }
    public void setIsWin(Boolean isWin) { this.isWin = isWin; }
    public Map<Long, Integer> getPlayerStats() { return playerStats; }
    public void setPlayerStats(Map<Long, Integer> playerStats) { this.playerStats = playerStats; }
}
