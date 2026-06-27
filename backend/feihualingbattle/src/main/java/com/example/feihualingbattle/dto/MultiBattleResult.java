package com.example.feihualingbattle.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MultiBattleResult {
    private String battleId;
    private String roomId;
    private int totalRounds;
    private long duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<PlayerResult> players;

    @Data
    public static class PlayerResult {
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private int rank;
        private int correctCount;
        private int wrongCount;
        private int totalCount;
        private Double accuracy;
        private Double avgTimeUsed;
        private Integer fastestTime;
        private int score;
        private boolean isEliminated;
        private Integer eliminationRound;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public int getCorrectCount() { return correctCount; }
        public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
        public int getWrongCount() { return wrongCount; }
        public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public Double getAccuracy() { return accuracy; }
        public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
        public Double getAvgTimeUsed() { return avgTimeUsed; }
        public void setAvgTimeUsed(Double avgTimeUsed) { this.avgTimeUsed = avgTimeUsed; }
        public Integer getFastestTime() { return fastestTime; }
        public void setFastestTime(Integer fastestTime) { this.fastestTime = fastestTime; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public boolean isEliminated() { return isEliminated; }
        public void setEliminated(boolean eliminated) { isEliminated = eliminated; }
        public Integer getEliminationRound() { return eliminationRound; }
        public void setEliminationRound(Integer eliminationRound) { this.eliminationRound = eliminationRound; }
    }

    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<PlayerResult> getPlayers() { return players; }
    public void setPlayers(List<PlayerResult> players) { this.players = players; }
}
