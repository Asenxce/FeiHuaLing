package com.example.feihualingbattle.service;

import com.example.feihualingbattle.dto.*;
import com.example.feihualingbattle.entity.Room;
import com.example.feihualingbattle.entity.RoomPlayer;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.enums.RoomPlayerStatus;
import com.example.feihualingbattle.enums.RoomStatus;
import com.example.feihualingbattle.repository.RoomPlayerRepository;
import com.example.feihualingbattle.repository.RoomRepository;
import com.example.feihualingbattle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class RoomService {
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomPlayerRepository roomPlayerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketSessionManager webSocketSessionManager;

    @Autowired
    private RoomStateMachine stateMachine;

    private static final int ROOM_EXPIRE_MINUTES = 5;
    private static final int ROOM_CODE_LENGTH = 6;

    @Transactional
    public RoomDetailDTO createRoom(CreateRoomRequest request) {
        log.info("createRoom: creatorId={}, gameMode={}", request.getCreatorId(), request.getGameMode());

        User creator = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String roomId = UUID.randomUUID().toString();
        String roomCode = generateUniqueRoomCode();

        Room room = new Room();
        room.setRoomId(roomId);
        room.setRoomCode(roomCode);
        room.setCreatorId(creator.getId());
        room.setGameMode(request.getGameMode());
        room.setKeyword(request.getKeyword());
        room.setKeyword2(request.getKeyword2());
        room.setKeywordPosition(request.getKeywordPosition());
        room.setColorKeyword(request.getColorKeyword());
        room.setNumberKeyword(request.getNumberKeyword());
        room.setForbiddenWord(request.getForbiddenWord());
        room.setTimeLimit(request.getTimeLimit() != null ? request.getTimeLimit() : 60);
        room.setMaxPlayers(request.getMaxPlayers() != null ? request.getMaxPlayers() : 4);
        room.setMinPlayers(request.getMinPlayers() != null ? request.getMinPlayers() : 2);
        room.setFaultLimit(request.getFaultLimit() != null ? request.getFaultLimit() : 3);
        room.setGameType(request.getGameType() != null ? request.getGameType() : "ENTERTAINMENT");
        room.setPoetryScope(request.getPoetryScope());
        room.setStatus(RoomStatus.WAITING.getValue());
        room.setExpireTime(LocalDateTime.now().plusMinutes(ROOM_EXPIRE_MINUTES));
        roomRepository.save(room);

        RoomPlayer roomPlayer = new RoomPlayer();
        roomPlayer.setRoomId(roomId);
        roomPlayer.setUserId(creator.getId());
        roomPlayer.setJoinOrder(1);
        roomPlayer.setIsHost(true);
        roomPlayer.setIsReady(false);
        roomPlayer.setFaultCount(room.getFaultLimit());
        roomPlayer.setStatus(RoomPlayerStatus.WAITING.getValue());
        roomPlayerRepository.save(roomPlayer);

        return buildRoomDetail(room);
    }

    @Transactional
    public RoomDetailDTO joinRoom(JoinRoomRequest request) {
        log.info("joinRoom: roomCode={}, userId={}", request.getRoomCode(), request.getUserId());

        Room room = roomRepository.findByRoomCode(request.getRoomCode().toUpperCase())
                .orElseThrow(() -> new RuntimeException("房间不存在或已失效"));

        RoomStatus currentStatus = RoomStatus.fromValue(room.getStatus());
        if (currentStatus != RoomStatus.WAITING) {
            throw new RuntimeException("房间已开始对局或已解散，无法加入");
        }

        if (room.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("房间已过期");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Optional<RoomPlayer> existingPlayer = roomPlayerRepository.findByRoomIdAndUserId(room.getRoomId(), user.getId());
        if (existingPlayer.isPresent()) {
            RoomPlayerStatus playerStatus = RoomPlayerStatus.fromValue(existingPlayer.get().getStatus());
            if (playerStatus != RoomPlayerStatus.QUIT) {
                throw new RuntimeException("你已在房间中");
            }
            // 如果玩家之前退出过，允许重新加入，更新状态
            roomPlayerRepository.updatePlayerStatus(room.getRoomId(), user.getId(), RoomPlayerStatus.WAITING.getValue());
            return buildRoomDetail(room);
        }

        List<RoomPlayer> players = roomPlayerRepository.findByRoomIdOrderByJoinOrderAsc(room.getRoomId());
        if (players.size() >= room.getMaxPlayers()) {
            throw new RuntimeException("房间已满员");
        }

        int joinOrder = players.size() + 1;
        RoomPlayer roomPlayer = new RoomPlayer();
        roomPlayer.setRoomId(room.getRoomId());
        roomPlayer.setUserId(user.getId());
        roomPlayer.setJoinOrder(joinOrder);
        roomPlayer.setIsHost(false);
        roomPlayer.setIsReady(false);
        roomPlayer.setFaultCount(room.getFaultLimit());
        roomPlayer.setStatus(RoomPlayerStatus.WAITING.getValue());
        roomPlayerRepository.save(roomPlayer);

        String nickname = user.getNickname() != null ? user.getNickname() : user.getUsername();
        webSocketSessionManager.broadcastToRoom(room.getRoomId(),
                "PLAYER_JOINED", "{\"userId\":" + user.getId() + ",\"nickname\":\"" + nickname + "\"}");

        return buildRoomDetail(room);
    }

    @Transactional
    public RoomDetailDTO toggleReady(String roomId, Long userId) {
        log.info("toggleReady: roomId={}, userId={}", roomId, userId);

        Room room = findRoomOrThrow(roomId);
        RoomPlayer roomPlayer = findRoomPlayerOrThrow(roomId, userId);

        // 验证房间状态
        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (stateMachine.isRoomTerminal(roomStatus)) {
            throw new IllegalStateException("房间已结束或已解散，无法切换准备状态");
        }
        if (roomStatus != RoomStatus.WAITING) {
            throw new IllegalStateException("只能在等待阶段切换准备状态");
        }

        // 验证玩家状态
        RoomPlayerStatus playerStatus = RoomPlayerStatus.fromValue(roomPlayer.getStatus());
        if (playerStatus != RoomPlayerStatus.WAITING) {
            throw new IllegalStateException("玩家状态不允许切换准备状态");
        }

        boolean newReady = !roomPlayer.getIsReady();
        roomPlayerRepository.updateReadyStatus(roomId, userId, newReady);
        roomPlayer.setIsReady(newReady);

        webSocketSessionManager.broadcastToRoom(roomId,
                "READY_UPDATE", "{\"userId\":" + userId + ",\"isReady\":" + newReady + "}");

        return buildRoomDetail(room);
    }

    @Transactional
    public RoomDetailDTO kickPlayer(String roomId, Long hostUserId, Long targetUserId) {
        log.info("kickPlayer: roomId={}, hostUserId={}, targetUserId={}", roomId, hostUserId, targetUserId);

        Room room = findRoomOrThrow(roomId);
        
        // 验证房间状态
        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (stateMachine.isRoomTerminal(roomStatus)) {
            throw new IllegalStateException("房间已结束或已解散");
        }
        if (roomStatus != RoomStatus.WAITING) {
            throw new IllegalStateException("只能在等待阶段踢出玩家");
        }

        if (!room.getCreatorId().equals(hostUserId)) {
            throw new RuntimeException("只有房主可以踢出玩家");
        }

        RoomPlayer targetPlayer = findRoomPlayerOrThrow(roomId, targetUserId);
        if (targetPlayer.getIsHost()) {
            throw new RuntimeException("不能踢出房主");
        }

        // 验证目标玩家状态
        RoomPlayerStatus targetStatus = RoomPlayerStatus.fromValue(targetPlayer.getStatus());
        if (targetStatus != RoomPlayerStatus.WAITING) {
            throw new IllegalStateException("只能踢出等待中的玩家");
        }

        roomPlayerRepository.updatePlayerStatus(roomId, targetUserId, RoomPlayerStatus.QUIT.getValue());
        webSocketSessionManager.leaveRoom(targetUserId);

        User targetUser = userRepository.findById(targetUserId).orElse(null);
        String nickname = targetUser != null ? targetUser.getNickname() : "未知玩家";
        webSocketSessionManager.broadcastToRoom(roomId,
                "PLAYER_LEFT", "玩家 " + nickname + " 被房主移出房间");
        webSocketSessionManager.sendToUser(targetUserId, "KICKED", "你被房主移出房间");

        return buildRoomDetail(room);
    }

    @Transactional
    public void leaveRoom(String roomId, Long userId) {
        log.info("leaveRoom: roomId={}, userId={}", roomId, userId);

        Room room = findRoomOrThrow(roomId);
        RoomPlayer roomPlayer = findRoomPlayerOrThrow(roomId, userId);

        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        RoomPlayerStatus playerStatus = RoomPlayerStatus.fromValue(roomPlayer.getStatus());

        // 验证玩家状态是否允许离开
        if (stateMachine.isPlayerTerminal(playerStatus)) {
            log.warn("leaveRoom: 玩家已处于终态, roomId={}, userId={}, status={}", roomId, userId, playerStatus);
            return;
        }

        if (roomPlayer.getIsHost() && roomStatus == RoomStatus.WAITING) {
            transferHostAndLeave(room, userId);
        } else {
            stateMachine.validatePlayerTransition(playerStatus, RoomPlayerStatus.QUIT, "离开房间");
            roomPlayerRepository.updatePlayerStatus(roomId, userId, RoomPlayerStatus.QUIT.getValue());

            User user = userRepository.findById(userId).orElse(null);
            String nickname = user != null ? user.getNickname() : "未知玩家";
            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_LEFT", "{\"userId\":" + userId + ",\"nickname\":\"" + nickname + "\"}");
        }

        webSocketSessionManager.leaveRoom(userId);

        if (roomStatus == RoomStatus.BATTLE) {
            User user = userRepository.findById(userId).orElse(null);
            String nickname = user != null ? user.getNickname() : "未知玩家";
            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_ELIMINATED", "{\"userId\":" + userId + ",\"nickname\":\"" + nickname + "\",\"reason\":\"主动退出\"}");
        }
    }

    private void transferHostAndLeave(Room room, Long userId) {
        List<RoomPlayer> activePlayers = roomPlayerRepository.findByRoomIdAndStatusOrderByJoinOrderAsc(
                room.getRoomId(), RoomPlayerStatus.WAITING.getValue());
        Optional<RoomPlayer> nextHost = activePlayers.stream()
                .filter(p -> !p.getUserId().equals(userId))
                .findFirst();

        if (nextHost.isPresent()) {
            Long newHostUserId = nextHost.get().getUserId();
            room.setCreatorId(newHostUserId);
            roomRepository.save(room);
            roomPlayerRepository.updatePlayerStatus(room.getRoomId(), userId, RoomPlayerStatus.QUIT.getValue());
            roomPlayerRepository.transferHost(room.getRoomId(), newHostUserId);

            User newHost = userRepository.findById(newHostUserId).orElse(null);
            String newHostName = newHost != null ? newHost.getNickname() : "未知玩家";
            webSocketSessionManager.broadcastToRoom(room.getRoomId(),
                    "HOST_TRANSFERRED", "{\"newHostId\":" + newHostUserId + ",\"newHostName\":\"" + newHostName + "\"}");
        } else {
            // 验证状态转换
            stateMachine.validateRoomTransition(RoomStatus.WAITING, RoomStatus.DISMISSED, "房主离开解散房间");
            room.setStatus(RoomStatus.DISMISSED.getValue());
            roomRepository.save(room);
            webSocketSessionManager.broadcastToRoom(room.getRoomId(),
                    "ROOM_DISSOLVED", "房间已解散");
        }
    }

    @Transactional
    public RoomDetailDTO updateRoomConfig(String roomId, UpdateRoomConfigRequest request) {
        log.info("updateRoomConfig: roomId={}, userId={}", roomId, request.getUserId());

        Room room = findRoomOrThrow(roomId);
        
        // 验证房间状态
        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (stateMachine.isRoomTerminal(roomStatus)) {
            throw new IllegalStateException("房间已结束或已解散");
        }
        if (!room.getCreatorId().equals(request.getUserId())) {
            throw new RuntimeException("只有房主可以修改房间配置");
        }
        if (roomStatus != RoomStatus.WAITING) {
            throw new RuntimeException("只能在等待阶段修改配置");
        }

        if (request.getGameMode() != null) room.setGameMode(request.getGameMode());
        if (request.getKeyword() != null) room.setKeyword(request.getKeyword());
        if (request.getKeyword2() != null) room.setKeyword2(request.getKeyword2());
        if (request.getKeywordPosition() != null) room.setKeywordPosition(request.getKeywordPosition());
        if (request.getColorKeyword() != null) room.setColorKeyword(request.getColorKeyword());
        if (request.getNumberKeyword() != null) room.setNumberKeyword(request.getNumberKeyword());
        if (request.getForbiddenWord() != null) room.setForbiddenWord(request.getForbiddenWord());
        if (request.getTimeLimit() != null) room.setTimeLimit(request.getTimeLimit());
        if (request.getMaxPlayers() != null) room.setMaxPlayers(request.getMaxPlayers());
        if (request.getMinPlayers() != null) room.setMinPlayers(request.getMinPlayers());
        if (request.getFaultLimit() != null) room.setFaultLimit(request.getFaultLimit());
        if (request.getGameType() != null) room.setGameType(request.getGameType());
        if (request.getPoetryScope() != null) room.setPoetryScope(request.getPoetryScope());
        roomRepository.save(room);

        webSocketSessionManager.broadcastToRoom(roomId, "ROOM_UPDATE", "房间配置已更新");

        return buildRoomDetail(room);
    }

    public RoomDetailDTO getRoomDetail(String roomId) {
        Room room = findRoomOrThrow(roomId);
        return buildRoomDetail(room);
    }

    public RoomDetailDTO getRoomByCode(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("房间不存在或已失效"));
        return buildRoomDetail(room);
    }

    public boolean canStartBattle(String roomId, Long userId) {
        Room room = findRoomOrThrow(roomId);
        if (!room.getCreatorId().equals(userId)) {
            return false;
        }
        long readyCount = roomPlayerRepository.countActiveReadyPlayers(roomId);
        long playerCount = roomPlayerRepository.countActivePlayers(roomId);
        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        return roomStatus == RoomStatus.WAITING
                && playerCount >= room.getMinPlayers()
                && playerCount <= room.getMaxPlayers()
                && readyCount == playerCount;
    }

    @Transactional
    public void dismissExpiredRooms() {
        List<Room> expiredRooms = roomRepository.findByStatusAndExpireTimeBefore(RoomStatus.WAITING.getValue(), LocalDateTime.now());
        for (Room room : expiredRooms) {
            RoomStatus currentStatus = RoomStatus.fromValue(room.getStatus());
            if (stateMachine.canTransition(currentStatus, RoomStatus.DISMISSED)) {
                room.setStatus(RoomStatus.DISMISSED.getValue());
                roomRepository.save(room);
                webSocketSessionManager.broadcastToRoom(room.getRoomId(),
                        "ROOM_DISSOLVED", "房间因超时未开局已自动解散");
                log.info("dismissExpiredRooms: roomId={}", room.getRoomId());
            }
        }
    }

    private RoomDetailDTO buildRoomDetail(Room room) {
        RoomDetailDTO dto = new RoomDetailDTO();
        dto.setRoomId(room.getRoomId());
        dto.setRoomCode(room.getRoomCode());
        dto.setCreatorId(room.getCreatorId());
        dto.setGameMode(room.getGameMode());
        dto.setKeyword(room.getKeyword());
        dto.setKeyword2(room.getKeyword2());
        dto.setKeywordPosition(room.getKeywordPosition());
        dto.setColorKeyword(room.getColorKeyword());
        dto.setNumberKeyword(room.getNumberKeyword());
        dto.setForbiddenWord(room.getForbiddenWord());
        dto.setTimeLimit(room.getTimeLimit());
        dto.setMaxPlayers(room.getMaxPlayers());
        dto.setMinPlayers(room.getMinPlayers());
        dto.setFaultLimit(room.getFaultLimit());
        dto.setGameType(room.getGameType());
        dto.setPoetryScope(room.getPoetryScope());
        dto.setStatus(room.getStatus());
        dto.setCurrentTurnUserId(room.getCurrentTurnUserId());
        dto.setBattleId(room.getBattleId());
        dto.setExpireTime(room.getExpireTime());
        dto.setCreateTime(room.getCreateTime());

        List<RoomPlayer> allPlayers = roomPlayerRepository.findByRoomIdOrderByJoinOrderAsc(room.getRoomId());
        List<RoomPlayer> activePlayers = allPlayers.stream()
                .filter(p -> RoomPlayerStatus.fromValue(p.getStatus()) != RoomPlayerStatus.QUIT)
                .collect(Collectors.toList());
        dto.setPlayerCount(activePlayers.size());

        long readyCount = activePlayers.stream().filter(RoomPlayer::getIsReady).count();
        dto.setReadyCount((int) readyCount);

        List<RoomPlayerDTO> playerDTOs = allPlayers.stream().map(p -> {
            RoomPlayerDTO pd = new RoomPlayerDTO();
            pd.setUserId(p.getUserId());
            pd.setJoinOrder(p.getJoinOrder());
            pd.setIsHost(p.getIsHost());
            pd.setIsReady(p.getIsReady());
            pd.setFaultCount(p.getFaultCount());
            pd.setStatus(p.getStatus());
            pd.setEliminationRound(p.getEliminationRound());
            pd.setJoinTime(p.getJoinTime());

            User user = userRepository.findById(p.getUserId()).orElse(null);
            if (user != null) {
                pd.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                pd.setAvatarUrl(user.getAvatarUrl());
                pd.setIdentityCode(user.getIdentityCode());
            }
            return pd;
        }).collect(Collectors.toList());
        dto.setPlayers(playerDTOs);

        return dto;
    }

    private String generateUniqueRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            code = sb.toString();
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }

    private Room findRoomOrThrow(String roomId) {
        return roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在: " + roomId));
    }

    private RoomPlayer findRoomPlayerOrThrow(String roomId, Long userId) {
        return roomPlayerRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("玩家不在该房间中"));
    }
}
