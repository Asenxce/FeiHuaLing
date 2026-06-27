package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.Room;
import com.example.feihualingbattle.entity.BattleInvite;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.dto.JoinRoomRequest;
import com.example.feihualingbattle.dto.RoomDetailDTO;
import com.example.feihualingbattle.enums.RoomStatus;
import com.example.feihualingbattle.repository.BattleInviteRepository;
import com.example.feihualingbattle.repository.RoomRepository;
import com.example.feihualingbattle.repository.UserRepository;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomInviteService {
    private static final Logger log = LoggerFactory.getLogger(RoomInviteService.class);
    private static final Gson gson = new Gson();

    @Autowired
    private BattleInviteRepository battleInviteRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MailService mailService;

    @Autowired
    private WebSocketSessionManager wsSessionManager;

    /**
     * 创建房间邀请
     */
    @Transactional
    public Map<String, Object> createRoomInvite(Long inviterId, Long inviteeId, String roomId) {
        log.info("创建房间邀请: inviterId={}, inviteeId={}, roomId={}", inviterId, inviteeId, roomId);

        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (roomStatus != RoomStatus.WAITING) {
            throw new RuntimeException("房间已开始对局或已解散，无法邀请");
        }

        if (room.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("房间已过期");
        }

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (inviterId.equals(inviteeId)) {
            throw new RuntimeException("不能邀请自己");
        }

        List<BattleInvite> existingInvites = battleInviteRepository
                .findByInviterIdAndInviteeIdAndStatus(inviterId, inviteeId, "PENDING");
        if (!existingInvites.isEmpty()) {
            // 自动撤销同一对用户之间的旧待处理邀请（允许在不同房间重新邀请）
            for (BattleInvite oldInvite : existingInvites) {
                oldInvite.setStatus("CANCELLED");
                battleInviteRepository.save(oldInvite);
                log.info("自动撤销旧邀请: inviteId={}, oldRoomId={}", oldInvite.getId(), oldInvite.getRoomId());
            }
        }

        String inviteToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        BattleInvite invite = new BattleInvite(
                inviteToken,
                inviterId,
                inviter.getNickname() != null ? inviter.getNickname() : inviter.getUsername(),
                inviteeId,
                invitee.getNickname() != null ? invitee.getNickname() : invitee.getUsername(),
                roomId,
                room.getRoomCode(),
                expireTime
        );

        battleInviteRepository.save(invite);

        String inviterName = inviter.getNickname() != null ? inviter.getNickname() : inviter.getUsername();
        try {
            mailService.sendMail(
                    inviteeId,
                    "房间邀请",
                    inviterName + " 邀请你加入飞花令多人对战房间！房间码: " + room.getRoomCode() + "，请打开游戏查看邀请。",
                    inviterName,
                    6,
                    invite.getId()
            );
            log.info("房间邀请邮件发送成功: inviteId={}, inviteeId={}", invite.getId(), inviteeId);
        } catch (Exception e) {
            log.warn("房间邀请邮件发送失败: {}", e.getMessage());
        }

        try {
            Map<String, Object> wsPayload = new HashMap<>();
            wsPayload.put("inviteToken", inviteToken);
            wsPayload.put("inviterId", inviterId);
            wsPayload.put("inviterName", inviterName);
            wsPayload.put("roomId", roomId);
            wsPayload.put("roomCode", room.getRoomCode());
            wsPayload.put("gameMode", room.getGameMode());
            wsPayload.put("timeLimit", room.getTimeLimit());
            wsPayload.put("maxPlayers", room.getMaxPlayers());
            wsSessionManager.sendToUser(inviteeId, "INVITE_RECEIVED", gson.toJson(wsPayload));
            log.info("房间邀请WebSocket通知发送成功: inviteeId={}", inviteeId);
        } catch (Exception e) {
            log.warn("房间邀请WebSocket通知发送失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inviteToken", inviteToken);
        result.put("inviteeName", invite.getInviteeName());
        result.put("roomCode", room.getRoomCode());
        result.put("roomId", roomId);
        result.put("expireTime", invite.getExpireTime());
        return result;
    }

    /**
     * 接受房间邀请并自动加入房间
     */
    @Transactional
    public Map<String, Object> acceptRoomInvite(String inviteToken, Long userId) {
        log.info("接受房间邀请: inviteToken={}, userId={}", inviteToken, userId);

        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        if (!invite.getInviteeId().equals(userId)) {
            throw new RuntimeException("您不是该邀请的被邀请人");
        }

        if (!"PENDING".equals(invite.getStatus())) {
            throw new RuntimeException("该邀请已处理或已过期");
        }

        if (LocalDateTime.now().isAfter(invite.getExpireTime())) {
            invite.setStatus("EXPIRED");
            battleInviteRepository.save(invite);
            throw new RuntimeException("邀请已过期");
        }

        Room room = roomRepository.findByRoomId(invite.getRoomId())
                .orElseThrow(() -> new RuntimeException("房间不存在或已失效"));

        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (roomStatus != RoomStatus.WAITING) {
            throw new RuntimeException("房间已开始对局或已解散，无法加入");
        }

        JoinRoomRequest joinRequest = new JoinRoomRequest();
        joinRequest.setUserId(userId);
        joinRequest.setRoomCode(invite.getRoomCode());

        RoomDetailDTO roomDetail = roomService.joinRoom(joinRequest);

        invite.setStatus("ACCEPTED");
        invite.setAcceptedTime(LocalDateTime.now());
        battleInviteRepository.save(invite);

        User invitee = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String inviteeName = invitee.getNickname() != null ? invitee.getNickname() : invitee.getUsername();

        try {
            mailService.sendMail(
                    invite.getInviterId(),
                    "邀请已接受",
                    inviteeName + " 已接受你的房间邀请并加入了房间！",
                    inviteeName,
                    7,
                    invite.getId()
            );
        } catch (Exception e) {
            log.warn("接受通知邮件发送失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", roomDetail.getRoomId());
        result.put("roomCode", roomDetail.getRoomCode());
        result.put("gameMode", roomDetail.getGameMode());
        result.put("timeLimit", roomDetail.getTimeLimit());
        result.put("maxPlayers", roomDetail.getMaxPlayers());
        result.put("playerCount", roomDetail.getPlayerCount());
        result.put("inviteId", invite.getId());
        return result;
    }

    /**
     * 取消房间邀请
     */
    @Transactional
    public void cancelRoomInvite(String inviteToken, Long userId) {
        log.info("取消房间邀请: inviteToken={}, userId={}", inviteToken, userId);

        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        if (!invite.getInviterId().equals(userId)) {
            throw new RuntimeException("您不是该邀请的发起人");
        }

        if (!"PENDING".equals(invite.getStatus())) {
            throw new RuntimeException("该邀请已处理");
        }

        invite.setStatus("CANCELLED");
        battleInviteRepository.save(invite);
    }

    /**
     * 获取用户的待处理房间邀请列表
     */
    public List<Map<String, Object>> getPendingRoomInvites(Long userId) {
        log.info("获取待处理房间邀请: userId={}", userId);

        List<BattleInvite> allPending = battleInviteRepository.findByInviteeIdAndStatus(userId, "PENDING");
        List<BattleInvite> invites = allPending.stream()
                .filter(i -> "ROOM".equals(i.getInviteType()))
                .collect(Collectors.toList());

        return invites.stream().map(invite -> {
            Map<String, Object> map = new HashMap<>();
            map.put("inviteToken", invite.getInviteToken());
            map.put("inviterId", invite.getInviterId());
            map.put("inviterName", invite.getInviterName());
            map.put("roomCode", invite.getRoomCode());
            map.put("roomId", invite.getRoomId());
            map.put("expireTime", invite.getExpireTime());
            map.put("createTime", invite.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取邀请详情
     */
    public Map<String, Object> getInviteDetail(String inviteToken, Long userId) {
        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        if (!invite.getInviterId().equals(userId) && !invite.getInviteeId().equals(userId)) {
            throw new RuntimeException("无权查看该邀请");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("inviteToken", invite.getInviteToken());
        detail.put("inviterId", invite.getInviterId());
        detail.put("inviterName", invite.getInviterName());
        detail.put("inviteeId", invite.getInviteeId());
        detail.put("inviteeName", invite.getInviteeName());
        detail.put("roomId", invite.getRoomId());
        detail.put("roomCode", invite.getRoomCode());
        detail.put("status", invite.getStatus());
        detail.put("expireTime", invite.getExpireTime());
        detail.put("createTime", invite.getCreateTime());
        if (invite.getAcceptedTime() != null) {
            detail.put("acceptedTime", invite.getAcceptedTime());
        }
        return detail;
    }
}
