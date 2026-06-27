package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_room")
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, unique = true, length = 64)
    private String roomId;

    @Column(name = "room_code", nullable = false, unique = true, length = 6, columnDefinition = "CHAR(6)")
    private String roomCode;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "game_mode", nullable = false, length = 50)
    private String gameMode;

    @Column(length = 10)
    private String keyword;

    @Column(name = "keyword2", length = 10)
    private String keyword2;

    @Column(name = "keyword_position")
    private Integer keywordPosition;

    @Column(name = "color_keyword", length = 10)
    private String colorKeyword;

    @Column(name = "number_keyword", length = 10)
    private String numberKeyword;

    @Column(name = "forbidden_word", length = 10)
    private String forbiddenWord;

    @Column(name = "time_limit", nullable = false, columnDefinition = "INT DEFAULT 60")
    private Integer timeLimit;

    @Column(name = "max_players", nullable = false, columnDefinition = "TINYINT DEFAULT 4")
    private Integer maxPlayers;

    @Column(name = "min_players", nullable = false, columnDefinition = "TINYINT DEFAULT 2")
    private Integer minPlayers;

    @Column(name = "fault_limit", nullable = false, columnDefinition = "TINYINT DEFAULT 3")
    private Integer faultLimit;

    @Column(name = "game_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ENTERTAINMENT'")
    private String gameType;

    @Column(name = "poetry_scope", length = 50)
    private String poetryScope;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'WAITING'")
    private String status;

    @Column(name = "current_turn_user_id")
    private Long currentTurnUserId;

    @Column(name = "battle_id", length = 64)
    private String battleId;

    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getKeyword2() { return keyword2; }
    public void setKeyword2(String keyword2) { this.keyword2 = keyword2; }
    public Integer getKeywordPosition() { return keywordPosition; }
    public void setKeywordPosition(Integer keywordPosition) { this.keywordPosition = keywordPosition; }
    public String getColorKeyword() { return colorKeyword; }
    public void setColorKeyword(String colorKeyword) { this.colorKeyword = colorKeyword; }
    public String getNumberKeyword() { return numberKeyword; }
    public void setNumberKeyword(String numberKeyword) { this.numberKeyword = numberKeyword; }
    public String getForbiddenWord() { return forbiddenWord; }
    public void setForbiddenWord(String forbiddenWord) { this.forbiddenWord = forbiddenWord; }
    public Integer getTimeLimit() { return timeLimit; }
    public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public Integer getMinPlayers() { return minPlayers; }
    public void setMinPlayers(Integer minPlayers) { this.minPlayers = minPlayers; }
    public Integer getFaultLimit() { return faultLimit; }
    public void setFaultLimit(Integer faultLimit) { this.faultLimit = faultLimit; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public String getPoetryScope() { return poetryScope; }
    public void setPoetryScope(String poetryScope) { this.poetryScope = poetryScope; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCurrentTurnUserId() { return currentTurnUserId; }
    public void setCurrentTurnUserId(Long currentTurnUserId) { this.currentTurnUserId = currentTurnUserId; }
    public String getBattleId() { return battleId; }
    public void setBattleId(String battleId) { this.battleId = battleId; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
