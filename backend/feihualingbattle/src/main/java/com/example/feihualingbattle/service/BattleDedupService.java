package com.example.feihualingbattle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对战答案去重服务 - 统一管理Redis去重逻辑
 */
@Service
public class BattleDedupService {
    private static final Logger log = LoggerFactory.getLogger(BattleDedupService.class);
    
    private static final String DEDUP_PREFIX = "battle:dedup:";
    private static final long DEDUP_EXPIRE_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查答案是否已使用
     * @param battleId 对战ID
     * @param answer 标准化后的答案
     * @return true表示重复，false表示未使用
     */
    public boolean isDuplicate(String battleId, String answer) {
        if (battleId == null || battleId.isEmpty() || answer == null) {
            return false;
        }
        String key = DEDUP_PREFIX + battleId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, answer));
    }

    /**
     * 标记答案为已使用
     * @param battleId 对战ID
     * @param answer 标准化后的答案
     */
    public void markAsUsed(String battleId, String answer) {
        if (battleId == null || battleId.isEmpty() || answer == null) {
            return;
        }
        String key = DEDUP_PREFIX + battleId;
        redisTemplate.opsForSet().add(key, answer);
        redisTemplate.expire(key, DEDUP_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 获取对战中已使用过的所有诗句
     * @param battleId 对战ID
     * @return 已使用诗句列表
     */
    public List<String> getUsedPoems(String battleId) {
        if (battleId == null || battleId.isEmpty()) {
            return List.of();
        }
        String key = DEDUP_PREFIX + battleId;
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    /**
     * 清除对战的所有去重记录
     * @param battleId 对战ID
     */
    public void clearDedup(String battleId) {
        if (battleId == null || battleId.isEmpty()) {
            return;
        }
        String key = DEDUP_PREFIX + battleId;
        redisTemplate.delete(key);
        log.debug("清除去重记录: battleId={}", battleId);
    }
}
