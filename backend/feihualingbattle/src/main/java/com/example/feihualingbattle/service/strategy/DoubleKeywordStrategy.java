package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.service.PoetryCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DoubleKeywordStrategy implements GameModeStrategy {

    @Autowired
    private PoetryCacheService poetryCacheService;

    private static final int SEARCH_LIMIT = 500;

    @Override
    public String getModeName() { return "double_keyword"; }

    @Override
    public String validate(Battle battle, String normalizedAnswer) {
        String keyword = battle.getKeyword();
        String keyword2 = battle.getKeyword2();
        if (!normalizedAnswer.contains(keyword) || !normalizedAnswer.contains(keyword2)) {
            return "答案必须同时包含关键字：" + keyword + " 和 " + keyword2;
        }
        return null;
    }

    @Override
    public List<Poetry> searchPoetry(GameModeContext ctx) {
        if (ctx.getKeyword() != null && ctx.getKeyword2() != null) {
            String normalizedAnswer = BattleServiceHelper.normalize(ctx.getAnswer());
            List<Poetry> candidates = poetryCacheService.getCandidatesByTwoKeywords(ctx.getKeyword(), ctx.getKeyword2(), SEARCH_LIMIT);
            return candidates.stream()
                .filter(p -> BattleServiceHelper.normalize(p.getContent()).equals(normalizedAnswer))
                .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public List<Poetry> getAIAnswer(GameModeContext ctx, List<String> usedPoems) {
        if (ctx.getKeyword() != null && ctx.getKeyword2() != null) {
            List<Poetry> candidates = poetryCacheService.getCandidatesByTwoKeywords(ctx.getKeyword(), ctx.getKeyword2(), 200);
            if (usedPoems != null && !usedPoems.isEmpty()) {
                java.util.Set<String> usedSet = new java.util.HashSet<>(usedPoems);
                candidates = candidates.stream()
                    .filter(p -> !usedSet.contains(p.getContent()))
                    .collect(java.util.stream.Collectors.toList());
            }
            return candidates;
        }
        return List.of();
    }
}
