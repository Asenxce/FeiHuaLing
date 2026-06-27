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
@Table(name = "t_room_player", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_id", "user_id"})
})
public class RoomPlayer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 64)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "join_order", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Integer joinOrder;

    @Column(name = "is_host", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isHost;

    @Column(name = "is_ready", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isReady;

    @Column(name = "fault_count", nullable = false, columnDefinition = "TINYINT DEFAULT 3")
    private Integer faultCount;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'WAITING'")
    private String status;

    @Column(name = "elimination_round")
    private Integer eliminationRound;

    @CreationTimestamp
    @Column(name = "join_time", updatable = false)
    private LocalDateTime joinTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getJoinOrder() { return joinOrder; }
    public void setJoinOrder(Integer joinOrder) { this.joinOrder = joinOrder; }
    public Boolean getIsHost() { return isHost; }
    public void setIsHost(Boolean isHost) { this.isHost = isHost; }
    public Boolean getIsReady() { return isReady; }
    public void setIsReady(Boolean isReady) { this.isReady = isReady; }
    public Integer getFaultCount() { return faultCount; }
    public void setFaultCount(Integer faultCount) { this.faultCount = faultCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getEliminationRound() { return eliminationRound; }
    public void setEliminationRound(Integer eliminationRound) { this.eliminationRound = eliminationRound; }
    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
