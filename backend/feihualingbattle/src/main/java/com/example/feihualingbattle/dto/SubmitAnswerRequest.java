package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private Long userId;
    private String answer;
    private Integer roundNum;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public Integer getRoundNum() { return roundNum; }
    public void setRoundNum(Integer roundNum) { this.roundNum = roundNum; }
}
