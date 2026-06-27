package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.dto.*;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.enums.RoomStatus;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.MultiplayerBattleService;
import com.example.feihualingbattle.service.PoetryCacheService;
import com.example.feihualingbattle.service.RoomService;
import com.example.feihualingbattle.service.strategy.BattleServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/room")
public class RoomController {
    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    @Autowired
    private RoomService roomService;

    @Autowired
    private MultiplayerBattleService multiplayerBattleService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PoetryCacheService poetryCacheService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody CreateRoomRequest request,
                                                          HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        request.setCreatorId(userId);
        log.info("createRoom: userId={}, gameMode={}", userId, request.getGameMode());
        RoomDetailDTO room = roomService.createRoom(request);
        return ResponseEntity.ok(Map.of("code", 200, "message", "房间创建成功", "data", room));
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinRoom(@RequestBody JoinRoomRequest request,
                                                        HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        request.setUserId(userId);
        log.info("joinRoom: roomCode={}, userId={}", request.getRoomCode(), userId);
        RoomDetailDTO room = roomService.joinRoom(request);
        return ResponseEntity.ok(Map.of("code", 200, "message", "加入房间成功", "data", room));
    }

    @PostMapping("/{roomId}/ready")
    public ResponseEntity<Map<String, Object>> toggleReady(@PathVariable String roomId,
                                                           HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        log.info("toggleReady: roomId={}, userId={}", roomId, userId);
        RoomDetailDTO room = roomService.toggleReady(roomId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "准备状态切换成功", "data", room));
    }

    @PostMapping("/{roomId}/kick")
    public ResponseEntity<Map<String, Object>> kickPlayer(@PathVariable String roomId,
                                                          @RequestBody Map<String, Long> body,
                                                          HttpServletRequest httpRequest) {
        Long hostUserId = jwtUtil.getCurrentUserId(httpRequest);
        Long targetUserId = body.get("targetUserId");
        log.info("kickPlayer: roomId={}, host={}, target={}", roomId, hostUserId, targetUserId);
        RoomDetailDTO room = roomService.kickPlayer(roomId, hostUserId, targetUserId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已移出玩家", "data", room));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Map<String, Object>> leaveRoom(@PathVariable String roomId,
                                                         HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        log.info("leaveRoom: roomId={}, userId={}", roomId, userId);

        RoomDetailDTO roomDetail = roomService.getRoomDetail(roomId);
        RoomStatus roomStatus = RoomStatus.fromValue(roomDetail.getStatus());
        if (roomStatus == RoomStatus.BATTLE && roomDetail.getBattleId() != null) {
            Map<String, Object> surrenderResult = multiplayerBattleService.handleSurrender(
                    roomDetail.getBattleId(), roomId, userId);
            return ResponseEntity.ok(Map.of("code", 200, "message", "已认输出局，对局继续", "data", surrenderResult));
        }

        roomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "已退出房间"));
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<Map<String, Object>> startBattle(@PathVariable String roomId,
                                                           HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        log.info("startBattle: roomId={}, userId={}", roomId, userId);
        try {
            Map<String, Object> result = multiplayerBattleService.startBattle(roomId, userId);
            return ResponseEntity.ok(Map.of("code", 200, "message", "对局开始", "data", result));
        } catch (Exception e) {
            log.error("startBattle失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomDetail(@PathVariable String roomId) {
        log.info("getRoomDetail: roomId={}", roomId);
        RoomDetailDTO room = roomService.getRoomDetail(roomId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "查询成功", "data", room));
    }

    @GetMapping("/code/{roomCode}")
    public ResponseEntity<Map<String, Object>> getRoomByCode(@PathVariable String roomCode) {
        log.info("getRoomByCode: roomCode={}", roomCode);
        RoomDetailDTO room = roomService.getRoomByCode(roomCode);
        return ResponseEntity.ok(Map.of("code", 200, "message", "查询成功", "data", room));
    }

    @PutMapping("/{roomId}/config")
    public ResponseEntity<Map<String, Object>> updateRoomConfig(@PathVariable String roomId,
                                                                @RequestBody UpdateRoomConfigRequest request,
                                                                HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        request.setUserId(userId);
        log.info("updateRoomConfig: roomId={}, userId={}", roomId, userId);
        RoomDetailDTO room = roomService.updateRoomConfig(roomId, request);
        return ResponseEntity.ok(Map.of("code", 200, "message", "配置更新成功", "data", room));
    }

    @PostMapping("/{roomId}/submit")
    public ResponseEntity<Map<String, Object>> submitAnswer(@PathVariable String roomId,
                                                            @RequestBody MultiBattleSubmitRequest request,
                                                            HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        request.setUserId(userId);
        request.setRoomId(roomId);
        log.info("submitAnswer: roomId={}, battleId={}, userId={}", roomId, request.getBattleId(), userId);
        Map<String, Object> result = multiplayerBattleService.submitAnswer(request);
        return ResponseEntity.ok(Map.of("code", 200, "message", "提交成功", "data", result));
    }

    @PostMapping("/{roomId}/timeout")
    public ResponseEntity<Map<String, Object>> handleTimeout(@PathVariable String roomId,
                                                             @RequestBody Map<String, Object> body,
                                                             HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        String battleId = (String) body.get("battleId");
        log.info("handleTimeout: roomId={}, battleId={}, userId={}", roomId, battleId, userId);
        multiplayerBattleService.handleTimeout(battleId, roomId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "超时已处理"));
    }

    @PostMapping("/{roomId}/surrender")
    public ResponseEntity<Map<String, Object>> surrender(@PathVariable String roomId,
                                                         @RequestBody Map<String, String> body,
                                                         HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        String battleId = body.get("battleId");
        log.info("surrender: roomId={}, battleId={}, userId={}", roomId, battleId, userId);
        try {
            Map<String, Object> result = multiplayerBattleService.handleSurrender(battleId, roomId, userId);
            return ResponseEntity.ok(Map.of("code", 200, "message", "已认输出局", "data", result));
        } catch (Exception e) {
            log.error("surrender失败: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp(@RequestParam String gameMode,
                                                        @RequestParam String keyword,
                                                        HttpServletRequest httpRequest) {
        Long userId = jwtUtil.getCurrentUserId(httpRequest);
        log.info("getHelp: userId={}, gameMode={}, keyword={}", userId, gameMode, keyword);

        List<Poetry> candidates = poetryCacheService.getCandidatesByKeyword(keyword, 200);
        if (candidates.isEmpty()) {
            return ResponseEntity.ok(Map.of("code", 200, "message", "无可用水诗词", "content", "", "author", "", "title", ""));
        }

        Poetry selected = candidates.get(new Random().nextInt(candidates.size()));
        String normalizedAnswer = BattleServiceHelper.normalize(selected.getContent());

        return ResponseEntity.ok(Map.of(
            "code", 200,
            "message", "求助成功",
            "content", normalizedAnswer,
            "author", selected.getAuthor() != null ? selected.getAuthor() : "",
            "title", selected.getTitle() != null ? selected.getTitle() : ""
        ));
    }
}
