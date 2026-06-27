package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.*;
import com.example.feihualingbattle.repository.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 对战结算服务
 * 负责处理对战结束后的积分计算和战绩记录
 */
@Service
public class SettlementService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserRecordRepository userRecordRepository;
    
    @Autowired
    private BattleRoundRepository battleRoundRepository;
    
    /**
     * 人机对战结算
     */
    @Transactional
    public void settleAIBattle(Battle battle, Long winnerId, int answerCount, int wrongCount, boolean isWin) {
        User user = userRepository.findById(battle.getCreatorId())
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 更新用户胜负次数
        if (isWin) {
            user.setWinCount(user.getWinCount() + 1);
        } else {
            user.setLoseCount(user.getLoseCount() + 1);
        }
        userRepository.save(user);
        
        // 创建战绩记录
        UserRecord record = new UserRecord();
        record.setUserId(user.getId());
        record.setBattleId(battle.getBattleId());
        record.setBattleType("ai");
        record.setGameMode(battle.getGameMode());
        record.setGameModeName(getGameModeName(battle.getGameMode()));
        record.setKeyword(battle.getKeyword());
        record.setOpponentId(0L);
        record.setOpponentName("AI机器人");
        record.setResult(isWin ? "WIN" : "LOSE");
        record.setScore((short) 0);
        
        // 计算统计数据
        List<BattleRound> details = battleRoundRepository.findByBattleIdOrderByRoundNumAsc(battle.getBattleId());
        if (details != null && !details.isEmpty()) {
            calculateRecordStats(record, details, battle);
        } else {
            int totalCount = answerCount + wrongCount;
            record.setCorrectCount((short) answerCount);
            record.setWrongCount((short) wrongCount);
            record.setTotalCount((short) totalCount);
            record.setTotalRounds((short) totalCount);
            record.setAccuracy(totalCount > 0 ? BigDecimal.valueOf((double) answerCount / totalCount * 100).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            if (battle.getStartTime() != null) {
                LocalDateTime endTime = battle.getEndTime() != null ? battle.getEndTime() : LocalDateTime.now();
                long duration = Duration.between(battle.getStartTime(), endTime).getSeconds();
                record.setDuration((int) duration);
            }
        }
        
        userRecordRepository.save(record);
    }
    
    /**
     * 好友对战结算
     */
    @Data
    public static class PlayerStat {
        private Long userId;
        private int answerCount;
        private boolean isWinner;

        public PlayerStat() {
        }

        public PlayerStat(Long userId, int answerCount, boolean isWinner) {
            this.userId = userId;
            this.answerCount = answerCount;
            this.isWinner = isWinner;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public int getAnswerCount() {
            return answerCount;
        }

        public void setAnswerCount(Integer answerCount) {
            this.answerCount = answerCount;
        }

        public boolean isWinner() {
            return isWinner;
        }

        public void setWinner(boolean isWinner) {
            this.isWinner = isWinner;
        }
    }
    
    @Transactional
    public void settleFriendBattle(Battle battle, List<PlayerStat> playerStats) {
        // 按答题数量排序,确定排名
        playerStats.sort((a, b) -> Integer.compare(b.getAnswerCount(), a.getAnswerCount()));
        
        // 分配积分:第一名10分,依次递减1分,最低1分
        for (int i = 0; i < playerStats.size(); i++) {
            PlayerStat stat = playerStats.get(i);
            int scoreChange = Math.max(1, 10 - i);
            
            User user = userRepository.findById(stat.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在: " + stat.getUserId()));
            
            // 更新用户总积分
            user.setTotalScore(user.getTotalScore() + scoreChange);
            
            // 更新胜负次数
            if (stat.isWinner()) {
                user.setWinCount(user.getWinCount() + 1);
            } else {
                user.setLoseCount(user.getLoseCount() + 1);
            }
            userRepository.save(user);
            
            // 创建战绩记录
            UserRecord record = new UserRecord();
            record.setUserId(user.getId());
            record.setBattleId(battle.getBattleId());
            record.setBattleType("friend");
            record.setGameMode(battle.getGameMode());
            record.setGameModeName(getGameModeName(battle.getGameMode()));
            record.setKeyword(battle.getKeyword());
            record.setOpponentId(getOpponentId(stat.getUserId(), playerStats));
            record.setOpponentName(getOpponentName(stat.getUserId(), playerStats, userRepository));
            record.setResult(stat.isWinner() ? "WIN" : "LOSE");
            record.setRank((short) (i + 1));
            record.setScore((short) scoreChange);
            
            // 计算统计数据
            List<BattleRound> details = battleRoundRepository.findByBattleIdAndUserId(battle.getBattleId(), user.getId());
            calculateRecordStats(record, details, battle);
            
            userRecordRepository.save(record);
        }
    }
    
    /**
     * 获取用户的战绩历史
     */
    public List<UserRecord> getUserRecords(Long userId) {
        return userRecordRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 根据ID获取单条战绩记录
     */
    public Optional<UserRecord> getUserRecordById(Long id) {
        return userRecordRepository.findById(id);
    }

    /**
     * 删除战绩记录
     */
    public void deleteUserRecord(Long id) {
        userRecordRepository.deleteById(id);
    }
    
    /**
     * 获取用户最近的战绩记录
     */
    public List<UserRecord> getRecentRecords(Long userId, int limit) {
        return userRecordRepository.findTopByUserIdOrderByCreateTimeDesc(userId, limit);
    }
    
    /**
     * 获取用户统计数据
     */
    public Map<String, Object> getUserStatistics(Long userId) {
        List<UserRecord> records = userRecordRepository.findByUserId(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBattles", records.size());
        
        long winCount = records.stream().filter(r -> "WIN".equals(r.getResult())).count();
        long loseCount = records.stream().filter(r -> "LOSE".equals(r.getResult())).count();
        long aiBattles = records.stream().filter(r -> "ai".equals(r.getBattleType())).count();
        long friendBattles = records.stream().filter(r -> "friend".equals(r.getBattleType())).count();
        
        int totalScore = records.stream().mapToInt(r -> r.getScore() != null ? r.getScore() : 0).sum();
        
        stats.put("winCount", winCount);
        stats.put("loseCount", loseCount);
        stats.put("aiBattles", aiBattles);
        stats.put("friendBattles", friendBattles);
        stats.put("totalScore", totalScore);
        stats.put("winRate", records.size() > 0 ? (double) winCount / records.size() * 100 : 0);
        
        return stats;
    }
    
    /**
     * 计算战绩统计信息
     */
    private void calculateRecordStats(UserRecord record, List<BattleRound> details, Battle battle) {
        if (details == null || details.isEmpty()) {
            record.setCorrectCount((short) 0);
            record.setWrongCount((short) 0);
            record.setTotalCount((short) 0);
            record.setAccuracy(BigDecimal.ZERO);
            record.setTotalRounds((short) 0);
            return;
        }
        
        short correctCount = 0;
        short wrongCount = 0;
        int totalTimeUsed = 0;
        int fastestTime = Integer.MAX_VALUE;
        
        for (BattleRound detail : details) {
            if (detail.getIsCorrect() == null) {
                // 超时/认输等回合的 isCorrect = null，不计入正确也不计入错误
                continue;
            }
            if (detail.getIsCorrect() == 1) {
                correctCount++;
            } else {
                wrongCount++;
            }
            
            if (detail.getTimeUsed() != null) {
                totalTimeUsed += detail.getTimeUsed();
                if (detail.getTimeUsed() < fastestTime) {
                    fastestTime = detail.getTimeUsed();
                }
            }
        }
        
        short totalCount = (short) (correctCount + wrongCount);
        record.setCorrectCount(correctCount);
        record.setWrongCount(wrongCount);
        record.setTotalCount(totalCount);
        record.setAccuracy(totalCount > 0 ? BigDecimal.valueOf((double) correctCount / totalCount * 100).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        record.setTotalRounds((short) details.size());
        record.setAvgTimeUsed(totalCount > 0 ? BigDecimal.valueOf((double) totalTimeUsed / totalCount).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        record.setFastestTime(fastestTime == Integer.MAX_VALUE ? null : (short) fastestTime);
        
        // 计算对战时长
        if (battle.getStartTime() != null && battle.getEndTime() != null) {
            long duration = Duration.between(battle.getStartTime(), battle.getEndTime()).getSeconds();
            record.setDuration((int) duration);
        }
    }
    
    /**
     * 获取游戏模式名称
     */
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
    
    /**
     * 获取对手ID
     */
    private Long getOpponentId(Long currentUserId, List<PlayerStat> playerStats) {
        return playerStats.stream()
            .filter(stat -> !stat.getUserId().equals(currentUserId))
            .findFirst()
            .map(PlayerStat::getUserId)
            .orElse(0L);
    }
    
    /**
     * 获取对手名称
     */
    private String getOpponentName(Long currentUserId, List<PlayerStat> playerStats, UserRepository userRepository) {
        Long opponentId = getOpponentId(currentUserId, playerStats);
        if (opponentId == 0L) {
            return "未知对手";
        }
        return userRepository.findById(opponentId)
            .map(user -> user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername())
            .orElse("未知对手");
    }
}