package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对战回合统一表实体类
 * 对应数据库表: t_battle_round
 * 记录每回合玩家的答题情况
 * - 人机/好友对战: roomId 为 NULL
 * - 多人对战: roomId 不为 NULL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_battle_round")
public class BattleRound implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "battle_id", nullable = false, length = 64)
    private String battleId;

    /**
     * 房间ID（人机/好友对战时为NULL，多人对战时有值）
     */
    @Column(name = "room_id", length = 64)
    private String roomId;

    @Column(name = "round_num", nullable = false)
    private Integer roundNum;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "poem_content", length = 500)
    private String poemContent;

    @Column(name = "is_correct")
    private Integer isCorrect;

    @Column(name = "time_used")
    private Integer timeUsed;

    /**
     * 答题时间（来自原 t_battle_detail，多人对战场景可为 NULL）
     */
    @Column(name = "answer_time")
    private LocalDateTime answerTime;

    /**
     * 本轮后剩余容错次数（多人对战场景使用，默认0）
     */
    @Column(name = "fault_after", columnDefinition = "TINYINT DEFAULT 0")
    private Integer faultAfter;

    /**
     * 是否超时（多人对战场景使用，默认false）
     */
    @Column(name = "is_timeout", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isTimeout;

    /**
     * 本轮是否被淘汰（多人对战场景使用，默认false）
     */
    @Column(name = "is_eliminated", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isEliminated;

    /**
     * 本轮后存活人数（多人对战场景使用，默认0）
     */
    @Column(name = "survivor_count", columnDefinition = "INT DEFAULT 0")
    private Integer survivorCount;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 便捷构造器（人机/好友对战场景）
     */
    public BattleRound(String battleId, Long userId, String poemContent, Integer roundNum) {
        this.battleId = battleId;
        this.roomId = null;
        this.userId = userId;
        this.poemContent = poemContent;
        this.roundNum = roundNum;
        this.answerTime = LocalDateTime.now();
        this.isCorrect = 1;
        this.faultAfter = 0;
        this.isTimeout = false;
        this.isEliminated = false;
        this.survivorCount = 0;
    }
}
