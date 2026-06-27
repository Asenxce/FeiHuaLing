package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.PoetryRepository;
import com.example.feihualingbattle.service.PoetryCacheService;
import com.example.feihualingbattle.service.strategy.rule.BattleRuleHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ColorStrategy implements GameModeStrategy {

    @Autowired
    private PoetryRepository poetryRepository;

    @Autowired
    private PoetryCacheService poetryCacheService;

    @Override
    public String getModeName() { return "color"; }

    @Override
    public String validate(Battle battle, String normalizedAnswer) {
        String colorKeyword = battle.getColorKeyword();
        if (colorKeyword == null || colorKeyword.isEmpty()) {
            return "颜色关键字未设置";
        }
        if (!normalizedAnswer.contains(colorKeyword)) {
            return "答案必须包含颜色字：" + colorKeyword;
        }
        return null;
    }

    @Autowired
    private BattleRuleHelper ruleHelper;

    @Override
    public List<Poetry> searchPoetry(GameModeContext ctx) {
        return ruleHelper.searchAndFilter(
            poetryRepository, ctx.getKeyword(), ctx.getAnswer(), 500
        );
    }

    @Override
    public List<Poetry> getAIAnswer(GameModeContext ctx, List<String> usedPoems) {
        String keyword = ctx.getColorKeyword() != null ? ctx.getColorKeyword() : ctx.getKeyword();
        if (keyword != null && !keyword.isEmpty()) {
            List<Poetry> candidates = poetryCacheService.getCandidatesByKeyword(keyword, 200);
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
