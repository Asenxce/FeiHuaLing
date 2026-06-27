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
 * 统一邀请表实体类
 * 对应数据库表: t_battle_invite
 * 通过 invite_type 字段区分:
 *   - BATTLE: 对战邀请
 *   - ROOM: 多人房间邀请
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_battle_invite")
public class BattleInvite implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 邀请令牌(UUID)
     */
    @Column(name = "invite_token", nullable = false, unique = true, length = 64)
    private String inviteToken;

    /**
     * 邀请人ID
     */
    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    /**
     * 邀请人名称(冗余)
     */
    @Column(name = "inviter_name", length = 50)
    private String inviterName;

    /**
     * 被邀请人ID
     */
    @Column(name = "invitee_id", nullable = false)
    private Long inviteeId;

    /**
     * 被邀请人名称(冗余)
     */
    @Column(name = "invitee_name", length = 50)
    private String inviteeName;

    /**
     * 邀请类型: BATTLE-对战邀请, ROOM-多人房间邀请
     */
    @Column(name = "invite_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'BATTLE'")
    private String inviteType;

    /**
     * 对战类型
     */
    @Column(name = "battle_type", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'friend'")
    private String battleType;

    /**
     * 预设游戏模式
     */
    @Column(name = "game_mode", length = 50)
    private String gameMode;

    /**
     * 预设关键字
     */
    @Column(length = 10)
    private String keyword;

    /**
     * 预设第二个关键字(双字模式)
     */
    @Column(name = "keyword2", length = 10)
    private String keyword2;

    /**
     * 预设限时(秒)
     */
    @Column(name = "time_limit", columnDefinition = "SMALLINT DEFAULT 60")
    private Short timeLimit;

    /**
     * 状态: PENDING-待处理, ACCEPTED-已接受, REJECTED-已拒绝, EXPIRED-已过期, CANCELLED-已取消
     */
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private String status;

    /**
     * 过期时间
     */
    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;

    /**
     * 接受时间
     */
    @Column(name = "accepted_time")
    private LocalDateTime acceptedTime;

    /**
     * 拒绝时间
     */
    @Column(name = "rejected_time")
    private LocalDateTime rejectedTime;

    /**
     * 生成的对战ID(接受后填充，对战邀请时使用)
     */
    @Column(name = "battle_id", length = 64)
    private String battleId;

    /**
     * 关联房间ID(多人房间邀请时有值)
     */
    @Column(name = "room_id", length = 64)
    private String roomId;

    /**
     * 房间码(多人房间邀请时有值，冗余便于显示)
     */
    @Column(name = "room_code", length = 6)
    private String roomCode;

    /**
     * 邀请发送时间
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

    /**
     * 对战邀请构造器
     */
    public BattleInvite(String inviteToken, Long inviterId, String inviterName,
                       Long inviteeId, String inviteeName, LocalDateTime expireTime) {
        this.inviteToken = inviteToken;
        this.inviterId = inviterId;
        this.inviterName = inviterName;
        this.inviteeId = inviteeId;
        this.inviteeName = inviteeName;
        this.status = "PENDING";
        this.expireTime = expireTime;
        this.battleType = "friend";
        this.inviteType = "BATTLE";
        this.timeLimit = 60;
    }

    /**
     * 房间邀请构造器
     */
    public BattleInvite(String inviteToken, Long inviterId, String inviterName,
                       Long inviteeId, String inviteeName, String roomId,
                       String roomCode, LocalDateTime expireTime) {
        this.inviteToken = inviteToken;
        this.inviterId = inviterId;
        this.inviterName = inviterName;
        this.inviteeId = inviteeId;
        this.inviteeName = inviteeName;
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.status = "PENDING";
        this.expireTime = expireTime;
        this.inviteType = "ROOM";
    }
}
