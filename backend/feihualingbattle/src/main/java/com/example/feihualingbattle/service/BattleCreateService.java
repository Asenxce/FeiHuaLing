package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.repository.BattleRepository;
import com.example.feihualingbattle.service.strategy.BattleServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 对战创建服务 - 统一管理所有对战创建逻辑
 */
@Service
public class BattleCreateService {
    private static final Logger log = LoggerFactory.getLogger(BattleCreateService.class);

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BattleServiceHelper battleServiceHelper;

    @Autowired
    private BattleDedupService dedupService;

    @Autowired
    private BattleStateService stateService;

    @Autowired
    private MailService mailService;

    /**
     * 创建对战的参数对象
     */
    public static class CreateBattleParam {
        private Long creatorId;
        private Long opponentId;
        private String gameMode;
        private String keyword;
        private String keyword2;
        private String colorKeyword;
        private String numberKeyword;
        private String forbiddenWord;
        private Integer keywordPosition;
        private Integer timeLimit;
        private String battleType; // ai, friend
        private String battleSubType; // ONE_VS_ONE, MULTI
        private String roomId; // 多人对战时使用

        // Getters and Setters
        public Long getCreatorId() { return creatorId; }
        public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
        public Long getOpponentId() { return opponentId; }
        public void setOpponentId(Long opponentId) { this.opponentId = opponentId; }
        public String getGameMode() { return gameMode; }
        public void setGameMode(String gameMode) { this.gameMode = gameMode; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public String getKeyword2() { return keyword2; }
        public void setKeyword2(String keyword2) { this.keyword2 = keyword2; }
        public String getColorKeyword() { return colorKeyword; }
        public void setColorKeyword(String colorKeyword) { this.colorKeyword = colorKeyword; }
        public String getNumberKeyword() { return numberKeyword; }
        public void setNumberKeyword(String numberKeyword) { this.numberKeyword = numberKeyword; }
        public String getForbiddenWord() { return forbiddenWord; }
        public void setForbiddenWord(String forbiddenWord) { this.forbiddenWord = forbiddenWord; }
        public Integer getKeywordPosition() { return keywordPosition; }
        public void setKeywordPosition(Integer keywordPosition) { this.keywordPosition = keywordPosition; }
        public Integer getTimeLimit() { return timeLimit; }
        public void setTimeLimit(Integer timeLimit) { this.timeLimit = timeLimit; }
        public String getBattleType() { return battleType; }
        public void setBattleType(String battleType) { this.battleType = battleType; }
        public String getBattleSubType() { return battleSubType; }
        public void setBattleSubType(String battleSubType) { this.battleSubType = battleSubType; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
    }

    /**
     * 统一创建对战
     */
    @Transactional
    public Battle createBattle(CreateBattleParam param) {
        log.info("创建对战: creatorId={}, battleType={}, gameMode={}", 
                param.getCreatorId(), param.getBattleType(), param.getGameMode());

        // 验证参数
        validateParam(param);

        // 取消旧的进行中好友对战
        if ("friend".equals(param.getBattleType()) && param.getOpponentId() != null) {
            cancelOldOngoingBattles(param.getCreatorId(), param.getOpponentId());
        }

        // 创建对战
        String battleId = UUID.randomUUID().toString().replace("-", "");
        
        Battle battle = new Battle();
        battle.setBattleId(battleId);
        battle.setBattleType(param.getBattleType());
        battle.setBattleSubType(param.getBattleSubType());
        battle.setGameMode(param.getGameMode());
        battle.setKeyword(param.getKeyword());
        battle.setKeyword2(param.getKeyword2());
        battle.setColorKeyword(param.getColorKeyword());
        battle.setNumberKeyword(param.getNumberKeyword());
        battle.setForbiddenWord(param.getForbiddenWord());
        battle.setKeywordPosition(param.getKeywordPosition());
        battle.setCreatorId(param.getCreatorId());
        battle.setOpponentId(param.getOpponentId());
        battle.setRoomId(param.getRoomId());
        battle.setStatus("ONGOING");
        battle.setTimeLimit(param.getTimeLimit() != null ? param.getTimeLimit() : 60);
        battle.setTotalRounds(0);
        battle.setStartTime(LocalDateTime.now());

        battle = battleRepository.save(battle);

        // 初始化对战状态
        stateService.initializeBattleState(battleId);

        log.info("对战创建成功: battleId={}", battleId);
        return battle;
    }

    /**
     * 通过身份码创建好友对战
     */
    @Transactional
    public Map<String, Object> createFriendBattleByCode(Long creatorId, String opponentIdentityCode,
                                                         String gameMode, String keyword, String keyword2,
                                                         String colorKeyword, String numberKeyword,
                                                         String forbiddenWord, Integer keywordPosition,
                                                         Integer timeLimit) {
        // 查找对手
        User opponent = userService.findByIdentityCode(opponentIdentityCode);
        if (opponent == null) {
            throw new RuntimeException("找不到身份码为 " + opponentIdentityCode + " 的用户");
        }
        if (opponent.getId().equals(creatorId)) {
            throw new RuntimeException("不能和自己对战");
        }

        // 创建对战
        CreateBattleParam param = new CreateBattleParam();
        param.setCreatorId(creatorId);
        param.setOpponentId(opponent.getId());
        param.setBattleType("friend");
        param.setGameMode(gameMode);
        param.setKeyword(keyword);
        param.setKeyword2(keyword2);
        param.setColorKeyword(colorKeyword);
        param.setNumberKeyword(numberKeyword);
        param.setForbiddenWord(forbiddenWord);
        param.setKeywordPosition(keywordPosition);
        param.setTimeLimit(timeLimit);

        Battle battle = createBattle(param);

        // 发送邮件通知
        User creator = userService.loadUserById(creatorId);
        String creatorName = creator.getNickname() != null && !creator.getNickname().isEmpty()
                ? creator.getNickname() : creator.getUsername();
        try {
            mailService.sendMail(opponent.getId(),
                    "对战邀请",
                    creatorName + " 邀请你进行一场飞花令对战！点击进入对战。",
                    creatorName,
                    3,
                    Long.valueOf(battle.getBattleId().hashCode()));
        } catch (Exception e) {
            log.warn("发送邀请邮件失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("battleId", battle.getBattleId());
        result.put("opponentName", opponent.getNickname() != null ? opponent.getNickname() : opponent.getUsername());
        result.put("opponentId", opponent.getId());
        result.put("gameMode", battle.getGameMode());
        return result;
    }

    /**
     * 加入好友对战
     */
    @Transactional
    public Map<String, Object> joinFriendBattle(Long userId, String inviterIdentityCode) {
        User inviter = userService.findByIdentityCode(inviterIdentityCode);
        if (inviter == null) {
            throw new RuntimeException("找不到身份码为 " + inviterIdentityCode + " 的用户");
        }
        if (inviter.getId().equals(userId)) {
            throw new RuntimeException("不能加入自己创建的对战");
        }

        Battle battle = battleRepository
                .findTopByCreatorIdAndOpponentIdAndStatusAndBattleTypeOrderByCreateTimeDesc(
                        inviter.getId(), userId, "ONGOING", "friend")
                .orElseThrow(() -> new RuntimeException("未找到待加入的对战，请确认邀请方身份码是否正确"));

        Map<String, Object> result = new HashMap<>();
        result.put("battleId", battle.getBattleId());
        result.put("gameMode", battle.getGameMode());
        result.put("keyword", battle.getKeyword());
        result.put("keyword2", battle.getKeyword2());
        result.put("timeLimit", battle.getTimeLimit());
        result.put("opponentName", inviter.getNickname() != null ? inviter.getNickname() : inviter.getUsername());
        return result;
    }

    /**
     * 取消两个用户之间所有旧的进行中好友对战
     */
    public void cancelOldOngoingBattles(Long userId1, Long userId2) {
        try {
            var oldBattles = battleRepository.findOngoingFriendBattlesBetweenUsers(userId1, userId2);
            if (!oldBattles.isEmpty()) {
                log.info("取消 {} 个旧的进行中好友对战 (users: {} & {})", oldBattles.size(), userId1, userId2);
                for (Battle oldBattle : oldBattles) {
                    oldBattle.setStatus("CANCELLED");
                    oldBattle.setEndTime(LocalDateTime.now());
                    battleRepository.save(oldBattle);
                    dedupService.clearDedup(oldBattle.getBattleId());
                }
            }
        } catch (Exception e) {
            log.warn("清理旧对战记录失败 (users: {} & {}): {}", userId1, userId2, e.getMessage());
        }
    }

    /**
     * 验证创建参数
     */
    private void validateParam(CreateBattleParam param) {
        if (param.getCreatorId() == null) {
            throw new RuntimeException("创建者ID不能为空");
        }
        if (param.getGameMode() == null || param.getGameMode().isEmpty()) {
            throw new RuntimeException("游戏模式不能为空");
        }
        if (param.getBattleType() == null || param.getBattleType().isEmpty()) {
            throw new RuntimeException("对战类型不能为空");
        }
        if ("friend".equals(param.getBattleType()) && param.getOpponentId() == null) {
            throw new RuntimeException("好友对战必须指定对手");
        }
    }
}
