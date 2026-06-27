package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户战绩表实体类
 * 对应数据库表: t_user_record
 * 记录每场对战的详细战绩
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_user_record")
public class UserRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 对战ID
     */
    @Column(name = "battle_id", nullable = false, length = 64)
    private String battleId;

    /**
     * 关联多人房间ID(多人对战时有值)
     */
    @Column(name = "room_id", length = 64)
    private String roomId;

    /**
     * 对战类型: ai/friend
     */
    @Column(name = "battle_type", nullable = false, length = 20)
    private String battleType;

    /**
     * 游戏模式
     */
    @Column(name = "game_mode", nullable = false, length = 50)
    private String gameMode;

    /**
     * 游戏模式名称
     */
    @Column(name = "game_mode_name", length = 50)
    private String gameModeName;

    /**
     * 关键字
     */
    @Column(length = 10)
    private String keyword;

    /**
     * 对手ID(ai对战为0)
     */
    @Column(name = "opponent_id")
    private Long opponentId;

    /**
     * 对手名称(冗余字段)
     */
    @Column(name = "opponent_name", length = 50)
    private String opponentName;

    /**
     * 结果: WIN-胜利, LOSE-失败, DRAW-平局, TIMEOUT-超时
     */
    @Column(length = 20)
    private String result;

    /**
     * 本场排名
     */
    @Column(name = "`rank`")
    private Short rank;

    /**
     * 被淘汰的轮次(多人对战)
     */
    @Column(name = "elimination_round")
    private Integer eliminationRound;

    /**
     * 是否中途被淘汰: 0-否, 1-是
     */
    @Column(name = "is_eliminated", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isEliminated;

    /**
     * 本场获得积分(仅好友对战)
     */
    @Column(columnDefinition = "SMALLINT DEFAULT 0")
    private Short score;

    /**
     * 正确答题数
     */
    @Column(name = "correct_count", columnDefinition = "SMALLINT UNSIGNED DEFAULT 0")
    private Short correctCount;

    /**
     * 错误答题数
     */
    @Column(name = "wrong_count", columnDefinition = "SMALLINT UNSIGNED DEFAULT 0")
    private Short wrongCount;

    /**
     * 总答题数
     */
    @Column(name = "total_count", columnDefinition = "SMALLINT UNSIGNED DEFAULT 0")
    private Short totalCount;

    /**
     * 准确率(%)
     */
    @Column(precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0.00")
    private BigDecimal accuracy;

    /**
     * 总回合数
     */
    @Column(name = "total_rounds", columnDefinition = "SMALLINT UNSIGNED DEFAULT 0")
    private Short totalRounds;

    /**
     * 平均答题用时(秒)
     */
    @Column(name = "avg_time_used", precision = 6, scale = 2, columnDefinition = "DECIMAL(6,2) DEFAULT 0.00")
    private BigDecimal avgTimeUsed;

    /**
     * 最快答题用时(秒)
     */
    @Column(name = "fastest_time")
    private Short fastestTime;

    /**
     * 对战时长(秒)
     */
    @Column(columnDefinition = "INT UNSIGNED")
    private Integer duration;

    /**
     * 记录创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBattleId() {
        return battleId;
    }

    public void setBattleId(String battleId) {
        this.battleId = battleId;
    }

    public String getBattleType() {
        return battleType;
    }

    public void setBattleType(String battleType) {
        this.battleType = battleType;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getGameModeName() {
        return gameModeName;
    }

    public void setGameModeName(String gameModeName) {
        this.gameModeName = gameModeName;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(Long opponentId) {
        this.opponentId = opponentId;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Short getRank() {
        return rank;
    }

    public void setRank(Short rank) {
        this.rank = rank;
    }

    public Short getScore() {
        return score;
    }

    public void setScore(Short score) {
        this.score = score;
    }

    public Short getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Short correctCount) {
        this.correctCount = correctCount;
    }

    public Short getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Short wrongCount) {
        this.wrongCount = wrongCount;
    }

    public Short getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Short totalCount) {
        this.totalCount = totalCount;
    }

    public BigDecimal getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(BigDecimal accuracy) {
        this.accuracy = accuracy;
    }

    public Short getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(Short totalRounds) {
        this.totalRounds = totalRounds;
    }

    public BigDecimal getAvgTimeUsed() {
        return avgTimeUsed;
    }

    public void setAvgTimeUsed(BigDecimal avgTimeUsed) {
        this.avgTimeUsed = avgTimeUsed;
    }

    public Short getFastestTime() {
        return fastestTime;
    }

    public void setFastestTime(Short fastestTime) {
        this.fastestTime = fastestTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
