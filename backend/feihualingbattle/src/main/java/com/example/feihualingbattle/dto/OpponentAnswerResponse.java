package com.example.feihualingbattle.dto;

import lombok.Data;

/**
 * 对手答案响应DTO
 */
@Data
public class OpponentAnswerResponse {
    private String poem;
    private boolean isAnswered;

    public String getPoem() { return poem; }
    public void setPoem(String poem) { this.poem = poem; }
    public boolean isAnswered() { return isAnswered; }
    public void setAnswered(boolean isAnswered) { this.isAnswered = isAnswered; }

    public OpponentAnswerResponse() {}
    
    public OpponentAnswerResponse(String poem, boolean isAnswered) {
        this.poem = poem;
        this.isAnswered = isAnswered;
    }
}
