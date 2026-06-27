package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对战主表实体类
 * 对应数据库表: t_battle
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_battle")
public class Battle implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 对战唯一ID(UUID)
     */
    @Column(name = "battle_id", nullable = false, unique = true, length = 64)
    private String battleId;

    /**
     * 对战类型: ai/friend
     */
    @Column(name = "battle_type", nullable = false, length = 20)
    private String battleType;

    /**
     * 对战子类型: ONE_VS_ONE-1v1, MULTI-多人
     */
    @Column(name = "battle_sub_type", length = 20)
    private String battleSubType;

    /**
     * 游戏模式: SIMPLE/POSITION/DOUBLE_KEYWORD等
     */
    @Column(name = "game_mode", nullable = false, length = 50)
    private String gameMode;

    /**
     * 关键字
     */
    @Column(length = 10)
    private String keyword;

    /**
     * 第二关键字(双字模式)
     * @deprecated 使用 gameModeParams JSON字段
     */
    @Deprecated
    @Column(name = "keyword2", length = 10)
    private String keyword2;

    /**
     * 关键字位置要求(位置模式)
     * @deprecated 使用 gameModeParams JSON字段
     */
    @Deprecated
    @Column(name = "keyword_position")
    private Integer keywordPosition;

    /**
     * 颜色关键字(颜色模式)
     * @deprecated 使用 gameModeParams JSON字段
     */
    @Deprecated
    @Column(name = "color_keyword", length = 10)
    private String colorKeyword;

    /**
     * 数字关键字(数字模式)
     * @deprecated 使用 gameModeParams JSON字段
     */
    @Deprecated
    @Column(name = "number_keyword", length = 10)
    private String numberKeyword;

    /**
     * 禁止词(反飞花令)
     * @deprecated 使用 gameModeParams JSON字段
     */
    @Deprecated
    @Column(name = "forbidden_word", length = 10)
    private String forbiddenWord;

    /**
     * 游戏模式参数(JSON格式)
     * 存储模式特定的参数，如keyword2、keywordPosition、colorKeyword等
     */
    @Column(name = "game_mode_params", columnDefinition = "JSON")
    private String gameModeParams;

    /**
     * 创建者ID
     */
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    /**
     * 对手ID(ai对战为NULL)
     */
    @Column(name = "opponent_id")
    private Long opponentId;

    /**
     * 关联多人房间ID(多人为NOT NULL, 1v1为NULL)
     */
    @Column(name = "room_id", length = 64)
    private String roomId;

    /**
     * 状态: ONGOING/COMPLETED/TIMEOUT/CANCELLED
     */
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ONGOING'")
    private String status;

    /**
     * 对局类型: ENTERTAINMENT-娱乐, COMPETITIVE-竞技(多人和1v1共用)
     */
    @Column(name = "game_type", length = 20)
    private String gameType;

    /**
     * 获胜者ID
     */
    @Column(name = "winner_id")
    private Long winnerId;

    /**
     * 限时时间(秒)
     */
    @Column(name = "time_limit", columnDefinition = "INT DEFAULT 60")
    private Integer timeLimit;

    /**
     * 每人容错次数
     */
    @Column(name = "fault_limit", columnDefinition = "TINYINT DEFAULT 3")
    private Integer faultLimit;

    /**
     * 总回合数
     */
    @Column(name = "total_rounds", columnDefinition = "INT DEFAULT 0")
    private Integer totalRounds;

    /**
     * 总参与人数
     */
    @Column(name = "total_players", columnDefinition = "TINYINT DEFAULT 2")
    private Integer totalPlayers;

    /**
     * 开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Battle(String battleId, String battleType, String gameMode, Long creatorId, Long opponentId) {
        this.battleId = battleId;
        this.battleType = battleType;
        this.gameMode = gameMode;
        this.creatorId = creatorId;
        this.opponentId = opponentId;
        this.status = "ONGOING";
        this.timeLimit = 60;
        this.totalRounds = 0;
        this.startTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getKeywordPosition() {
        return keywordPosition;
    }

    public void setKeywordPosition(Integer keywordPosition) {
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

    public String getGameModeParams() {
        return gameModeParams;
    }

    public void setGameModeParams(String gameModeParams) {
        this.gameModeParams = gameModeParams;
    }

    /**
     * 获取游戏模式参数对象
     * 优先从gameModeParams JSON字段解析，如果为空则从旧字段构建
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public com.example.feihualingbattle.dto.GameModeParams getGameModeParamsObj() {
        if (gameModeParams != null && !gameModeParams.isEmpty()) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    gameModeParams, com.example.feihualingbattle.dto.GameModeParams.class);
            } catch (Exception e) {
                // 解析失败，返回null
            }
        }
        // 从旧字段构建
        if (keyword2 != null || keywordPosition != null || colorKeyword != null || 
            numberKeyword != null || forbiddenWord != null) {
            return com.example.feihualingbattle.dto.GameModeParams.create(
                keyword2, keywordPosition, colorKeyword, numberKeyword, forbiddenWord);
        }
        return null;
    }

    /**
     * 设置游戏模式参数对象
     */
    public void setGameModeParamsObj(com.example.feihualingbattle.dto.GameModeParams params) {
        if (params != null && params.hasAnyParam()) {
            try {
                this.gameModeParams = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params);
            } catch (Exception e) {
                // 序列化失败
            }
            // 同时更新旧字段以保持兼容性
            this.keyword2 = params.getKeyword2();
            this.keywordPosition = params.getKeywordPosition();
            this.colorKeyword = params.getColorKeyword();
            this.numberKeyword = params.getNumberKeyword();
            this.forbiddenWord = params.getForbiddenWord();
        }
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getOpponentId() {
        return opponentId;
    }

    public void setOpponentId(Long opponentId) {
        this.opponentId = opponentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
