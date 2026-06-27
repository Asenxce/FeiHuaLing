package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.service.PoetryCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 简易模式 / 单关键字策略
 * 规则：答案包含指定关键字即可
 */
@Component
public class SimpleKeywordStrategy implements GameModeStrategy {

    @Autowired
    private PoetryCacheService poetryCacheService;

    private static final int SEARCH_LIMIT = 500;

    @Override
    public String getModeName() {
        return "simple";
    }

    @Override
    public String validate(Battle battle, String normalizedAnswer) {
        String keyword = battle.getKeyword();
        if (!normalizedAnswer.contains(keyword)) {
            return "答案必须包含关键字：" + keyword;
        }
        return null;
    }

    @Override
    public List<Poetry> searchPoetry(GameModeContext ctx) {
        String normalizedAnswer = BattleServiceHelper.normalize(ctx.getAnswer());
        List<Poetry> candidates = poetryCacheService.getCandidatesByKeyword(ctx.getAnswer(), SEARCH_LIMIT);
        return candidates.stream()
            .filter(p -> BattleServiceHelper.normalize(p.getContent()).equals(normalizedAnswer))
            .collect(Collectors.toList());
    }

    @Override
    public List<Poetry> getAIAnswer(GameModeContext ctx, List<String> usedPoems) {
        List<Poetry> candidates = poetryCacheService.getCandidatesByKeyword(ctx.getKeyword(), 200);
        if (usedPoems != null && !usedPoems.isEmpty()) {
            java.util.Set<String> usedSet = new java.util.HashSet<>(usedPoems);
            candidates = candidates.stream()
                .filter(p -> !usedSet.contains(p.getContent()))
                .collect(java.util.stream.Collectors.toList());
        }
        return candidates;
    }
}
