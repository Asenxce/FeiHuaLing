package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.dto.*;
import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.repository.BattleRoundRepository;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.BattleCreateService;
import com.example.feihualingbattle.service.BattleService;
import com.example.feihualingbattle.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/battle")
public class BattleController {
    private static final Logger log = LoggerFactory.getLogger(BattleController.class);

    @Autowired
    private BattleService battleService;

    @Autowired
    private BattleCreateService battleCreateService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private BattleRoundRepository battleRoundRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/ai/create")
    public ApiResponse<Battle> createAIBattle(HttpServletRequest httpRequest, @RequestBody CreateAIBattleRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            Battle battle = battleService.createAIBattle(
                userId,
                request.getGameMode(),
                request.getKeyword(),
                request.getKeyword2(),
                request.getColorKeyword(),
                request.getNumberKeyword(),
                request.getForbiddenWord(),
                request.getKeywordPosition(),
                request.getTimeLimit()
            );

            return ApiResponse.success("人机对战创建成功", battle);
        } catch (Exception e) {
            log.error("创建人机对战失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/friend/create")
    public ApiResponse<Battle> createFriendBattle(HttpServletRequest httpRequest, @RequestBody CreateFriendBattleRequest request) {
        try {
            Long creatorId = jwtUtil.getCurrentUserId(httpRequest);

            Battle battle = battleService.createFriendBattle(
                creatorId,
                request.getOpponentId(),
                request.getGameMode(),
                request.getKeyword(),
                request.getKeyword2(),
                request.getColorKeyword(),
                request.getNumberKeyword(),
                request.getForbiddenWord(),
                request.getKeywordPosition(),
                request.getTimeLimit()
            );

            return ApiResponse.success("好友对战创建成功", battle);
        } catch (Exception e) {
            log.error("创建好友对战失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{battleId}/submit")
    public ApiResponse<Map<String, Object>> submitAnswer(
            HttpServletRequest httpRequest,
            @PathVariable String battleId,
            @RequestBody SubmitAnswerRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            Map<String, Object> result = battleService.submitAnswer(
                battleId,
                userId,
                request.getAnswer(),
                request.getRoundNum()
            );

            if ((Boolean) result.get("success")) {
                return ApiResponse.success(result.get("message").toString(), result);
            } else {
                return ApiResponse.error(result.get("message").toString());
            }
        } catch (Exception e) {
            log.error("提交答案失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{battleId}/ai-answer")
    public ApiResponse<Map<String, Object>> getAIAnswer(
            @PathVariable String battleId,
            @RequestParam Integer roundNum) {
        try {
            Battle battle = battleService.getBattleById(battleId);

            // 计算位置（与旧逻辑一致）
            Integer keywordPosition = null;
            if ("position".equalsIgnoreCase(battle.getGameMode())) {
                keywordPosition = ((roundNum - 1) % 7) + 1;
            }

            // 获取已使用诗句
            java.util.List<String> usedPoems = battleService.getUsedPoems(battle.getBattleId());

            String aiAnswer = battleService.getAIAnswerNew(
                battle.getGameMode(),
                battle.getKeyword(),
                battle.getKeyword2(),
                battle.getColorKeyword(),
                battle.getForbiddenWord(),
                keywordPosition,
                null, // lastChar
                usedPoems
            );

            if (aiAnswer != null && !aiAnswer.isEmpty()) {
                return ApiResponse.success(Map.of("answer", aiAnswer));
            } else {
                return ApiResponse.success("AI无法找到合适的答案，游戏结束", null);
            }
        } catch (Exception e) {
            log.error("AI应答失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{battleId}/end")
    public ApiResponse<Void> endBattle(
            @PathVariable String battleId,
            @RequestBody EndBattleRequest request) {
        try {
            Battle battle = battleService.getBattleById(battleId);

            if ("ai".equals(battle.getBattleType())) {
                settlementService.settleAIBattle(
                    battle,
                    request.getWinnerId(),
                    request.getAnswerCount(),
                    request.getWrongCount() != null ? request.getWrongCount() : 0,
                    request.getIsWin()
                );
            } else {
                List<SettlementService.PlayerStat> playerStatsList = request.getPlayerStats().entrySet().stream()
                    .map(entry -> {
                        SettlementService.PlayerStat stat = new SettlementService.PlayerStat();
                        stat.setUserId(entry.getKey());
                        stat.setAnswerCount(entry.getValue());
                        stat.setWinner(entry.getKey().equals(request.getWinnerId()));
                        return stat;
                    })
                    .collect(java.util.stream.Collectors.toList());
                settlementService.settleFriendBattle(battle, playerStatsList);
            }

            battleService.endBattle(battleId, request.getWinnerId());

            return ApiResponse.success("对战已结束，结算完成", null);
        } catch (Exception e) {
            log.error("结束对战失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/{battleId}/surrender")
    public ApiResponse<Map<String, Object>> surrenderBattle(
            HttpServletRequest httpRequest,
            @PathVariable String battleId) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);
            Battle battle = battleService.surrenderBattle(battleId, userId);

            if ("ai".equals(battle.getBattleType())) {
                settlementService.settleAIBattle(battle, null, 0, 0, false);
            } else {
                Long opponentId = battle.getCreatorId().equals(userId)
                        ? battle.getOpponentId() : battle.getCreatorId();

                int surrenderAnswerCount = (int) battleRoundRepository.countTotalAnswers(battleId, userId);
                int opponentAnswerCount = (int) battleRoundRepository.countTotalAnswers(battleId, opponentId);

                List<SettlementService.PlayerStat> playerStats = List.of(
                        new SettlementService.PlayerStat(userId, surrenderAnswerCount, false),
                        new SettlementService.PlayerStat(opponentId, opponentAnswerCount, true)
                );
                settlementService.settleFriendBattle(battle, playerStats);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("surrendered", true);
            result.put("battleId", battleId);
            result.put("battleType", battle.getBattleType());
            return ApiResponse.success("已认输出局，对局结束", result);
        } catch (Exception e) {
            log.error("认输失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{battleId}")
    public ApiResponse<Battle> getBattleDetail(@PathVariable String battleId) {
        try {
            Battle battle = battleService.getBattleById(battleId);
            return ApiResponse.success(battle);
        } catch (Exception e) {
            log.error("获取对战详情失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<Battle>> getUserBattles(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            if (!currentUserId.equals(userId)) {
                return ApiResponse.error(403, "无权查看其他用户的对战记录");
            }

            List<Battle> battles = battleService.getUserBattles(userId);
            return ApiResponse.success(battles);
        } catch (Exception e) {
            log.error("获取对战列表失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submitAnswerNew(HttpServletRequest httpRequest, @RequestBody SubmitAnswerNewRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            Map<String, Object> result = battleService.validateAndSubmitAnswer(
                request.getBattleId(),
                userId,
                request.getGameMode(),
                request.getKeyword(),
                request.getKeyword2(),
                request.getColorKeyword(),
                request.getNumberKeyword(),
                request.getForbiddenWord(),
                request.getKeywordPosition(),
                request.getLastChar(),
                request.getAnswer()
            );

            if (result.get("success").equals(true)) {
                return ApiResponse.success("验证通过", Map.of("valid", true, "poem", result.get("poem")));
            } else {
                return ApiResponse.error(result.get("message").toString());
            }
        } catch (Exception e) {
            log.error("提交答案验证失败: {}", e.getMessage());
            return ApiResponse.error(500, "服务器内部错误");
        }
    }

    @PostMapping("/ai/answer")
    public ApiResponse<Map<String, Object>> getAIAnswerNew(@RequestBody AIAnswerRequest request) {
        try {
            String aiPoem = battleService.getAIAnswerNew(
                request.getGameMode(),
                request.getKeyword(),
                request.getKeyword2(),
                request.getColorKeyword(),
                request.getForbiddenWord(),
                request.getKeywordPosition(),
                request.getLastChar(),
                request.getUsedPoems()
            );

            return ApiResponse.success(Map.of("poem", aiPoem != null ? aiPoem : ""));
        } catch (Exception e) {
            log.error("AI应答失败: {}", e.getMessage());
            return ApiResponse.error(500, "服务器内部错误");
        }
    }

    @GetMapping("/opponent/answer")
    public ApiResponse<OpponentAnswerResponse> getOpponentAnswer(@RequestParam String battleId, HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);
            OpponentAnswerResponse opponentAnswer = battleService.getOpponentAnswer(battleId, userId);
            return ApiResponse.success(opponentAnswer);
        } catch (Exception e) {
            log.error("获取对手答案失败: {}", e.getMessage());
            return ApiResponse.error(500, "服务器内部错误");
        }
    }

    // ==================== 好友对战简化接口 ====================

    @PostMapping("/friend/create-by-code")
    public ApiResponse<Map<String, Object>> createFriendBattleByCode(HttpServletRequest httpRequest,
                                                                     @RequestBody Map<String, Object> request) {
        try {
            Long creatorId = jwtUtil.getCurrentUserId(httpRequest);
            String opponentIdentityCode = (String) request.get("opponentIdentityCode");
            String gameMode = (String) request.get("gameMode");
            String keyword = (String) request.get("keyword");
            String keyword2 = (String) request.get("keyword2");
            String colorKeyword = (String) request.get("colorKeyword");
            String numberKeyword = (String) request.get("numberKeyword");
            String forbiddenWord = (String) request.get("forbiddenWord");
            Integer keywordPosition = request.containsKey("keywordPosition") ? ((Number) request.get("keywordPosition")).intValue() : null;
            Integer timeLimit = request.containsKey("timeLimit") ? ((Number) request.get("timeLimit")).intValue() : 60;

            if (opponentIdentityCode == null || opponentIdentityCode.isEmpty()) {
                return ApiResponse.error("对手身份码不能为空");
            }
            if (gameMode == null || gameMode.isEmpty()) {
                return ApiResponse.error("游戏模式不能为空");
            }

            Map<String, Object> result = battleCreateService.createFriendBattleByCode(
                    creatorId, opponentIdentityCode, gameMode, keyword, keyword2,
                    colorKeyword, numberKeyword, forbiddenWord, keywordPosition, timeLimit);
            return ApiResponse.success("好友对战创建成功", result);
        } catch (Exception e) {
            log.error("创建好友对战失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/friend/join")
    public ApiResponse<Map<String, Object>> joinFriendBattle(HttpServletRequest httpRequest,
                                                              @RequestBody Map<String, Object> request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);
            String inviterIdentityCode = (String) request.get("inviterIdentityCode");

            if (inviterIdentityCode == null || inviterIdentityCode.isEmpty()) {
                return ApiResponse.error("邀请方身份码不能为空");
            }

            Map<String, Object> result = battleCreateService.joinFriendBattle(userId, inviterIdentityCode);
            return ApiResponse.success("加入对战成功", result);
        } catch (Exception e) {
            log.error("加入好友对战失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
}
