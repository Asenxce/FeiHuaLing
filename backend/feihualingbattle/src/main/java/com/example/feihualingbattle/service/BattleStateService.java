package com.example.feihualingbattle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 对战状态服务 - 管理Redis中的对战状态
 */
@Service
public class BattleStateService {
    private static final Logger log = LoggerFactory.getLogger(BattleStateService.class);
    
    private static final String BATTLE_STATE_PREFIX = "battle:state:";
    private static final long STATE_EXPIRE_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 初始化对战状态
     */
    public void initializeBattleState(String battleId) {
        String stateKey = BATTLE_STATE_PREFIX + battleId;
        Map<String, Object> state = new HashMap<>();
        state.put("currentRound", 0);
        state.put("status", "ONGOING");
        redisTemplate.opsForHash().putAll(stateKey, state);
        redisTemplate.expire(stateKey, STATE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("初始化对战状态: battleId={}", battleId);
    }

    /**
     * 清除对战状态
     */
    public void clearBattleState(String battleId) {
        String stateKey = BATTLE_STATE_PREFIX + battleId;
        redisTemplate.delete(stateKey);
        log.debug("清除对战状态: battleId={}", battleId);
    }

    /**
     * 获取对战状态
     */
    public Map<Object, Object> getBattleState(String battleId) {
        String stateKey = BATTLE_STATE_PREFIX + battleId;
        return redisTemplate.opsForHash().entries(stateKey);
    }

    /**
     * 更新对战状态
     */
    public void updateBattleState(String battleId, String field, Object value) {
        String stateKey = BATTLE_STATE_PREFIX + battleId;
        redisTemplate.opsForHash().put(stateKey, field, value);
    }

    /**
     * 获取当前回合数
     */
    public int getCurrentRound(String battleId) {
        Map<Object, Object> state = getBattleState(battleId);
        Object round = state.get("currentRound");
        return round != null ? Integer.parseInt(round.toString()) : 0;
    }

    /**
     * 增加回合数
     */
    public void incrementRound(String battleId) {
        int currentRound = getCurrentRound(battleId);
        updateBattleState(battleId, "currentRound", currentRound + 1);
    }
}
