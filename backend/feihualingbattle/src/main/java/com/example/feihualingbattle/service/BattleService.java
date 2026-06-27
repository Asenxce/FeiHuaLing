package com.example.feihualingbattle.service;

import com.example.feihualingbattle.dto.OpponentAnswerResponse;
import com.example.feihualingbattle.entity.*;
import com.example.feihualingbattle.repository.*;
import com.example.feihualingbattle.service.strategy.BattleServiceHelper;
import com.example.feihualingbattle.service.strategy.GameModeContext;
import com.example.feihualingbattle.service.strategy.GameModeStrategy;
import com.example.feihualingbattle.service.strategy.GameModeStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class BattleService {
    private static final Logger log = LoggerFactory.getLogger(BattleService.class);

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleRoundRepository battleRoundRepository;

    @Autowired
    private PoetryRepository poetryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private GameModeStrategyFactory strategyFactory;

    @Autowired
    private BattleServiceHelper helper;

    @Autowired
    private MailService mailService;

    @Autowired
    private BattleDedupService dedupService;

    @Autowired
    private BattleStateService stateService;

    @Autowired
    private BattleCreateService createService;

    @Transactional
    public Battle createAIBattle(Long userId, String gameMode, String keyword, String keyword2,
                                  String colorKeyword, String numberKeyword, String forbiddenWord,
                                  Integer keywordPosition, Integer timeLimit) {
        BattleCreateService.CreateBattleParam param = new BattleCreateService.CreateBattleParam();
        param.setCreatorId(userId);
        param.setBattleType("ai");
        param.setGameMode(gameMode);
        param.setKeyword(keyword);
        param.setKeyword2(keyword2);
        param.setColorKeyword(colorKeyword);
        param.setNumberKeyword(numberKeyword);
        param.setForbiddenWord(forbiddenWord);
        param.setKeywordPosition(keywordPosition);
        param.setTimeLimit(timeLimit);

        return createService.createBattle(param);
    }

    @Transactional
    public Battle createFriendBattle(Long creatorId, Long opponentId, String gameMode,
                                     String keyword, String keyword2, String colorKeyword,
                                     String numberKeyword, String forbiddenWord,
                                     Integer keywordPosition, Integer timeLimit) {
        BattleCreateService.CreateBattleParam param = new BattleCreateService.CreateBattleParam();
        param.setCreatorId(creatorId);
        param.setOpponentId(opponentId);
        param.setBattleType("friend");
        param.setGameMode(gameMode);
        param.setKeyword(keyword);
        param.setKeyword2(keyword2);
        param.setColorKeyword(colorKeyword);
        param.setNumberKeyword(numberKeyword);
        param.setForbiddenWord(forbiddenWord);
        param.setKeywordPosition(keywordPosition);
        param.setTimeLimit(timeLimit);

        return createService.createBattle(param);
    }

    @Transactional
    public Map<String, Object> submitAnswer(String battleId, Long userId, String answer, Integer roundNum) {
        Map<String, Object> result = new HashMap<>();

        Battle battle = battleRepository.findByBattleId(battleId)
                .orElseThrow(() -> new RuntimeException("对战不存在"));

        if (!"ONGOING".equals(battle.getStatus())) {
            throw new RuntimeException("对战已结束");
        }

        String normalizedAnswer = helper.normalizePoetry(answer);

        GameModeStrategy strategy = strategyFactory.getStrategy(battle.getGameMode());
        String errorMsg = strategy.validate(battle, normalizedAnswer);
        if (errorMsg != null) {
            result.put("success", false);
            result.put("message", errorMsg);
            result.put("isCorrect", false);
            return result;
        }

        boolean isDuplicate = checkDuplicate(battleId, normalizedAnswer);
        if (isDuplicate) {
            result.put("success", false);
            result.put("message", "该诗句已被使用，请更换其他诗句");
            result.put("isCorrect", false);
            return result;
        }

        List<Poetry> matched = poetryRepository.findByExactContent(normalizedAnswer);
        if (matched.isEmpty()) {
            result.put("success", false);
            result.put("message", "该诗句不在诗库中，请检查是否正确");
            result.put("isCorrect", false);
            return result;
        }

        BattleRound round = new BattleRound(battleId, userId, normalizedAnswer, roundNum);
        round.setIsCorrect(1);
        battleRoundRepository.save(round);

        markAnswerAsUsed(battleId, normalizedAnswer);

        result.put("success", true);
        result.put("message", "回答正确");
        result.put("isCorrect", true);

        return result;
    }

    @Transactional
    public void endBattle(String battleId, Long winnerId) {
        Battle battle = battleRepository.findByBattleId(battleId)
                .orElseThrow(() -> new RuntimeException("对战不存在"));

        battle.setStatus("COMPLETED");
        battle.setWinnerId(winnerId);
        battle.setEndTime(LocalDateTime.now());
        battleRepository.save(battle);

        stateService.clearBattleState(battleId);
        dedupService.clearDedup(battleId);
    }

    @Transactional
    public Battle surrenderBattle(String battleId, Long userId) {
        Battle battle = battleRepository.findByBattleId(battleId)
                .orElseThrow(() -> new RuntimeException("对战不存在"));

        if (!"ONGOING".equals(battle.getStatus())) {
            throw new RuntimeException("对战已结束");
        }

        Long winnerId;
        if ("ai".equals(battle.getBattleType())) {
            winnerId = null;
        } else {
            winnerId = battle.getCreatorId().equals(userId)
                    ? battle.getOpponentId() : battle.getCreatorId();
        }

        battle.setStatus("COMPLETED");
        battle.setWinnerId(winnerId);
        battle.setEndTime(LocalDateTime.now());
        battleRepository.save(battle);

        stateService.clearBattleState(battleId);
        dedupService.clearDedup(battleId);

        log.info("surrenderBattle: battleId={}, userId={}, winnerId={}, battleType={}",
                battleId, userId, winnerId, battle.getBattleType());

        return battle;
    }

    public Battle getBattleById(String battleId) {
        return battleRepository.findByBattleId(battleId)
                .orElseThrow(() -> new RuntimeException("对战不存在"));
    }

    public List<String> getUsedPoems(String battleId) {
        return dedupService.getUsedPoems(battleId);
    }

    public List<Battle> getUserBattles(Long userId) {
        return battleRepository.findByCreatorIdOrOpponentId(userId, userId);
    }

    private boolean checkDuplicate(String battleId, String answer) {
        return dedupService.isDuplicate(battleId, answer);
    }

    private void markAnswerAsUsed(String battleId, String answer) {
        dedupService.markAsUsed(battleId, answer);
    }



    // ==================== 新接口（策略驱动） ====================

    @Transactional
    public Map<String, Object> validateAndSubmitAnswer(String battleId, Long userId, String gameMode, String keyword,
                                                        String keyword2, String colorKeyword, String numberKeyword,
                                                        String forbiddenWord, Integer keywordPosition,
                                                        String lastChar, String answer) {
        Map<String, Object> result = new HashMap<>();

        String normalizedAnswer = helper.normalizePoetry(answer);

        if (battleId != null && !battleId.isEmpty() && checkDuplicate(battleId, normalizedAnswer)) {
            result.put("success", false);
            result.put("message", "该诗句已经使用过，请换一句");
            result.put("poem", null);
            return result;
        }

        GameModeStrategy strategy = strategyFactory.getStrategy(gameMode);

        String ruleError = strategy.validate(
                buildBattleForValidation(gameMode, keyword, keyword2, colorKeyword, numberKeyword,
                        forbiddenWord, keywordPosition, lastChar),
                normalizedAnswer);
        if (ruleError != null) {
            result.put("success", false);
            result.put("message", ruleError);
            result.put("poem", null);
            return result;
        }

        List<Poetry> matched = poetryRepository.findByExactContent(normalizedAnswer);
        if (matched.isEmpty()) {
            result.put("success", false);
            result.put("message", "该诗句不在诗库中，请检查是否正确");
            result.put("poem", null);
            return result;
        }

        Poetry poetry = matched.get(0);
        poetry.setUsageCount(poetry.getUsageCount() + 1);
        poetryRepository.save(poetry);

        if (battleId != null && !battleId.isEmpty()) {
            markAnswerAsUsed(battleId, normalizedAnswer);
        }

        result.put("success", true);
        result.put("message", "验证通过");
        result.put("poem", poetry.getContent());
        return result;
    }

    private Battle buildBattleForValidation(String gameMode, String keyword, String keyword2,
                                             String colorKeyword, String numberKeyword,
                                             String forbiddenWord, Integer keywordPosition,
                                             String lastChar) {
        Battle battle = new Battle();
        battle.setGameMode(gameMode);
        battle.setKeyword(keyword);
        battle.setKeyword2(keyword2);
        battle.setColorKeyword(colorKeyword);
        battle.setNumberKeyword(numberKeyword);
        battle.setForbiddenWord(forbiddenWord);
        battle.setKeywordPosition(keywordPosition);
        return battle;
    }

    public String getAIAnswerNew(String gameMode, String keyword, String keyword2,
                                  String colorKeyword, String forbiddenWord, Integer keywordPosition,
                                  String lastChar, List<String> usedPoems) {
        // 从 Battle 对象中提取 numberKeyword —— 此方法没有直接传入 numberKeyword，
        // 但 keyword 在 number 模式下也可能作为搜索词使用
        GameModeContext ctx = new GameModeContext.Builder()
                .gameMode(gameMode)
                .keyword(keyword)
                .keyword2(keyword2)
                .colorKeyword(colorKeyword)
                .numberKeyword(keyword) // number 模式下 keyword 即为 numberKeyword
                .forbiddenWord(forbiddenWord)
                .keywordPosition(keywordPosition)
                .lastChar(lastChar)
                .build();

        GameModeStrategy strategy = strategyFactory.getStrategy(gameMode);
        List<Poetry> candidates = strategy.getAIAnswer(ctx, usedPoems);

        if (!candidates.isEmpty()) {
            return candidates.get(new Random().nextInt(candidates.size())).getContent();
        }

        return "";
    }

    public OpponentAnswerResponse getOpponentAnswer(String battleId, Long currentUserId) {
        Battle battle = battleRepository.findByBattleId(battleId)
            .orElseThrow(() -> new RuntimeException("对战不存在"));

        Long opponentId = battle.getCreatorId().equals(currentUserId) ? battle.getOpponentId() : battle.getCreatorId();

        if (opponentId == null) {
            throw new RuntimeException("对手不存在");
        }

        List<BattleRound> opponentDetails = battleRoundRepository.findByBattleIdAndUserId(battleId, opponentId);

        if (opponentDetails.isEmpty()) {
            return new OpponentAnswerResponse("", false);
        }

        BattleRound latestDetail = opponentDetails.stream()
            .max(Comparator.comparing(BattleRound::getRoundNum))
            .orElse(null);

        if (latestDetail != null) {
            return new OpponentAnswerResponse(latestDetail.getPoemContent(), true);
        }

        return new OpponentAnswerResponse("", false);
    }
}
