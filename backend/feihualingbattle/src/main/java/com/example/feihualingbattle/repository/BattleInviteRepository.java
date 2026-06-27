package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.BattleInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BattleInviteRepository extends JpaRepository<BattleInvite, Long> {

    /**
     * 根据邀请令牌查找
     */
    Optional<BattleInvite> findByInviteToken(String inviteToken);

    /**
     * 查找发送给某用户的邀请
     */
    List<BattleInvite> findByInviteeId(Long inviteeId);

    /**
     * 查找某用户发出的邀请
     */
    List<BattleInvite> findByInviterId(Long inviterId);

    /**
     * 查找待处理的邀请
     */
    List<BattleInvite> findByInviteeIdAndStatus(Long inviteeId, String status);

    /**
     * 检查邀请令牌是否存在
     */
    boolean existsByInviteToken(String inviteToken);

    /**
     * 查找某用户发给某用户的待处理邀请
     */
    List<BattleInvite> findByInviterIdAndInviteeIdAndStatus(Long inviterId, Long inviteeId, String status);

    /**
     * 按邀请类型查找发送给某用户的邀请
     */
    List<BattleInvite> findByInviteeIdAndInviteType(Long inviteeId, String inviteType);

    /**
     * 按邀请类型查找某用户发出的邀请
     */
    List<BattleInvite> findByInviterIdAndInviteType(Long inviterId, String inviteType);

    /**
     * 查找某房间的待处理邀请（原 RoomInviteRepository 方法）
     */
    List<BattleInvite> findByRoomIdAndStatus(String roomId, String status);
}
