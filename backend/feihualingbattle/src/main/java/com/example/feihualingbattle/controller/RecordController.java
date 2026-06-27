package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.BattleRound;
import com.example.feihualingbattle.entity.UserRecord;
import com.example.feihualingbattle.repository.BattleRoundRepository;
import com.example.feihualingbattle.repository.BattleRepository;
import com.example.feihualingbattle.repository.UserRepository;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 战绩控制器
 * 提供战绩查询、统计等API接口
 */
@RestController
@RequestMapping("/api/record")
public class RecordController {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleRoundRepository battleRoundRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取用户的战绩历史
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRecords(@PathVariable Long userId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 从JWT Token中提取当前用户ID，防止越权访问
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            if (!currentUserId.equals(userId)) {
                response.put("success", false);
                response.put("message", "无权查看其他用户的战绩记录");
                return ResponseEntity.status(403).body(response);
            }

            List<UserRecord> records = settlementService.getUserRecords(userId);

            response.put("success", true);
            response.put("count", records.size());
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户最近的战绩记录
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<Map<String, Object>> getRecentRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 从JWT Token中提取当前用户ID，防止越权访问
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            if (!currentUserId.equals(userId)) {
                response.put("success", false);
                response.put("message", "无权查看其他用户的战绩记录");
                return ResponseEntity.status(403).body(response);
            }

            List<UserRecord> records = settlementService.getRecentRecords(userId, limit);

            response.put("success", true);
            response.put("count", records.size());
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStatistics(@PathVariable Long userId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 从JWT Token中提取当前用户ID，防止越权访问
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            if (!currentUserId.equals(userId)) {
                response.put("success", false);
                response.put("message", "无权查看其他用户的统计数据");
                return ResponseEntity.status(403).body(response);
            }

            Map<String, Object> stats = settlementService.getUserStatistics(userId);

            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前用户的个人战绩（无需传userId）
     */
    @GetMapping("/personal")
    public ResponseEntity<Map<String, Object>> getPersonalRecords(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = jwtUtil.getCurrentUserId(request);

            List<UserRecord> records = settlementService.getUserRecords(userId);
            Map<String, Object> stats = settlementService.getUserStatistics(userId);

            List<Map<String, Object>> recordMaps = new ArrayList<>();
            for (UserRecord r : records) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", r.getId());
                map.put("battleId", r.getBattleId());
                map.put("battleType", r.getBattleType());
                map.put("gameModeName", r.getGameModeName());
                map.put("keyword", r.getKeyword());
                map.put("opponentName", r.getOpponentName());
                map.put("result", r.getResult());
                map.put("score", r.getScore());
                map.put("correctCount", r.getCorrectCount());
                map.put("wrongCount", r.getWrongCount());
                map.put("accuracy", r.getAccuracy());
                map.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
                map.put("duration", r.getDuration());
                map.put("rank", r.getRank() != null ? r.getRank() : 0);
                map.put("totalRounds", r.getTotalRounds() != null ? r.getTotalRounds() : 0);
                recordMaps.add(map);
            }

            response.put("success", true);
            response.put("count", recordMaps.size());
            response.put("records", recordMaps);
            response.put("statistics", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取对战详情（包含诗句详情）
     * GET /api/record/battle/{battleId}
     */
    @GetMapping("/battle/{battleId}")
    public ResponseEntity<Map<String, Object>> getBattleDetail(@PathVariable String battleId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Battle> battleOpt = battleRepository.findByBattleId(battleId);
            if (battleOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "对战不存在");
                return ResponseEntity.badRequest().body(response);
            }
            Battle battle = battleOpt.get();

            List<BattleRound> details = battleRoundRepository.findByBattleIdOrderByRoundNumAsc(battleId);

            Map<String, Object> data = new HashMap<>();
            data.put("battleId", battle.getBattleId());
            data.put("gameModeName", getGameModeName(battle.getGameMode()));
            data.put("keyword", battle.getKeyword());
            data.put("createTime", battle.getCreateTime() != null ? battle.getCreateTime().toString() : "");
            data.put("duration", 0);
            if (battle.getStartTime() != null && battle.getEndTime() != null) {
                data.put("duration", java.time.Duration.between(battle.getStartTime(), battle.getEndTime()).getSeconds());
            }

            List<Map<String, Object>> verses = new ArrayList<>();
            for (BattleRound detail : details) {
                Map<String, Object> verse = new HashMap<>();
                verse.put("content", detail.getPoemContent());
                verse.put("isCorrect", detail.getIsCorrect());
                verse.put("roundNum", detail.getRoundNum());

                com.example.feihualingbattle.entity.User player = userRepository.findById(detail.getUserId()).orElse(null);
                verse.put("playerName", player != null ? (player.getNickname() != null ? player.getNickname() : player.getUsername()) : "未知玩家");

                verses.add(verse);
            }
            data.put("verses", verses);

            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除对战记录（支持批量删除）
     * DELETE /api/record/delete
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteRecords(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);
            @SuppressWarnings("unchecked")
            List<Number> ids = (List<Number>) request.get("ids");
            if (ids == null || ids.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择要删除的记录");
                return ResponseEntity.badRequest().body(response);
            }

            int deleted = 0;
            for (Number id : ids) {
                Long recordId = id.longValue();
                Optional<UserRecord> recordOpt = settlementService.getUserRecordById(recordId);
                if (recordOpt.isPresent() && recordOpt.get().getUserId().equals(userId)) {
                    settlementService.deleteUserRecord(recordId);
                    deleted++;
                }
            }

            response.put("success", true);
            response.put("message", "成功删除 " + deleted + " 条记录");
            response.put("deletedCount", deleted);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String getGameModeName(String gameMode) {
        if (gameMode == null) return "未知模式";
        switch (gameMode.toUpperCase()) {
            case "SIMPLE": case "SINGLE_KEYWORD": return "单关键字飞花令";
            case "POSITION": return "位置飞花令";
            case "DOUBLE_KEYWORD": return "双关键字飞花令";
            case "CHAIN": return "首尾接龙飞花令";
            case "COLOR": return "颜色飞花令";
            case "NUMBER": return "数字飞花令";
            case "FORBIDDEN": return "反飞花令";
            case "CUSTOM": return "自定义飞花令";
            default: return gameMode;
        }
    }
}
