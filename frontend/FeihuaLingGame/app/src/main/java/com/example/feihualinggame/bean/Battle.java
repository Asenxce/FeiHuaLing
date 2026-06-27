package com.example.feihualinggame.bean;

import java.util.List;

public class Battle {
    private String battleId;         // 对战 ID
    private String opponent;         // 对手用户名
    private String opponentUserId;   // 对手身份码
    private int score;               // 当前得分
    private int opponentScore;       // 对手得分
    private String status;           // 战斗状态（进行中、胜利、失败）
    private String gameMode;         // 游戏模式（SIMPLE/CLASSIC_POSITION/CLASSIC_DOUBLE/ENTERTAINMENT_*）
    private String keyword;          // 关键字（单字）
    private String keyword2;         // 第二个关键字（双字飞花令）
    private int keywordPosition;     // 关键字位置要求（位置飞花令）
    private String colorKeyword;     // 颜色关键字（颜色飞花令）
    private String numberKeyword;    // 数字关键字（数字飞花令）
    private String forbiddenWord;    // 禁止词语（反飞花令）
    private int timeLimit;           // 限时时间（秒）
    private int currentRound;        // 当前回合数
    private int maxRounds;           // 最大回合数
    private List<String> usedPoems;  // 已使用的诗句列表
    private String lastPoem;         // 上一句诗（首尾接龙用）
    private boolean isMyTurn;        // 是否是我的回合
    private long reconnectTimeout;   // 断线重连超时时间

    // Getter & Setter
    public String getBattleId() {
        return battleId;
    }

    public void setBattleId(String battleId) {
        this.battleId = battleId;
    }

    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public String getOpponentUserId() {
        return opponentUserId;
    }

    public void setOpponentUserId(String opponentUserId) {
        this.opponentUserId = opponentUserId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public void setOpponentScore(int opponentScore) {
        this.opponentScore = opponentScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword2() {
        return keyword2;
    }

    public void setKeyword2(String keyword2) {
        this.keyword2 = keyword2;
    }

    public int getKeywordPosition() {
        return keywordPosition;
    }

    public void setKeywordPosition(int keywordPosition) {
        this.keywordPosition = keywordPosition;
    }

    public String getColorKeyword() {
        return colorKeyword;
    }

    public void setColorKeyword(String colorKeyword) {
        this.colorKeyword = colorKeyword;
    }

    public String getNumberKeyword() {
        return numberKeyword;
    }

    public void setNumberKeyword(String numberKeyword) {
        this.numberKeyword = numberKeyword;
    }

    public String getForbiddenWord() {
        return forbiddenWord;
    }

    public void setForbiddenWord(String forbiddenWord) {
        this.forbiddenWord = forbiddenWord;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public List<String> getUsedPoems() {
        return usedPoems;
    }

    public void setUsedPoems(List<String> usedPoems) {
        this.usedPoems = usedPoems;
    }

    public String getLastPoem() {
        return lastPoem;
    }

    public void setLastPoem(String lastPoem) {
        this.lastPoem = lastPoem;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public void setMyTurn(boolean myTurn) {
        isMyTurn = myTurn;
    }

    public long getReconnectTimeout() {
        return reconnectTimeout;
    }

    public void setReconnectTimeout(long reconnectTimeout) {
        this.reconnectTimeout = reconnectTimeout;
    }
}