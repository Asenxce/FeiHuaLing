package com.example.feihualinggame.bean;

public class UserRecordBean {
    private long id;
    private String battleId;
    private String battleType;
    private String gameModeName;
    private String keyword;
    private String opponentName;
    private String result;
    private int score;
    private int correctCount;
    private int wrongCount;
    private double accuracy;
    private String createTime;
    private int duration;
    private int rank;
    private int totalRounds;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }
    public String getBattleType() { return battleType; }
    public void setBattleType(String battleType) { this.battleType = battleType; }
    public String getGameModeName() { return gameModeName; }
    public void setGameModeName(String gameModeName) { this.gameModeName = gameModeName; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getOpponentName() { return opponentName; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getWrongCount() { return wrongCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
}
