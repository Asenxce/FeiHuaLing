package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.BattleRound;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.BattleRoundRepository;
import com.example.feihualingbattle.repository.PoetryRepository;
import com.example.feihualingbattle.service.PoetryCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ChainStrategy implements GameModeStrategy {

    @Autowired
    private PoetryRepository poetryRepository;

    @Autowired
    private BattleRoundRepository battleRoundRepository;

    @Autowired
    private PoetryCacheService poetryCacheService;

    private static final int SEARCH_LIMIT = 500;

    @Override
    public String getModeName() { return "chain"; }

    /**
     * 接龙模式验证规则：
     * 1. 第一轮（无上一句）：答案必须包含指定关键字（如有）
     * 2. 非第一轮：答案首字必须与上一句尾字相同（首尾接龙）
     */
    @Override
    public String validate(Battle battle, String normalizedAnswer) {
        if (normalizedAnswer == null || normalizedAnswer.isEmpty()) {
            return "答案不能为空";
        }

        // 查询该对战最近一轮的正确答案
        Optional<BattleRound> lastRoundOpt = battleRoundRepository
                .findTopByBattleIdAndIsCorrectOrderByRoundNumDesc(battleId(battle), 1);

        if (lastRoundOpt.isEmpty()) {
            // 第一轮：按关键字模式校验（如果设置了关键字）
            if (battle.getKeyword() != null && !battle.getKeyword().isEmpty()) {
                if (!normalizedAnswer.contains(battle.getKeyword())) {
                    return "首句答案必须包含关键字：" + battle.getKeyword();
                }
            }
            return null;
        }

        // 非第一轮：执行首尾接龙
        String lastContent = lastRoundOpt.get().getPoemContent();
        if (lastContent == null || lastContent.isEmpty()) {
            return null;
        }

        String lastChar = lastContent.substring(lastContent.length() - 1);
        String firstChar = normalizedAnswer.substring(0, 1);

        if (!lastChar.equals(firstChar)) {
            return "接龙规则：答案首字「" + firstChar + "」必须与上句尾字「" + lastChar + "」相同";
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
        if (ctx.getLastChar() != null && !ctx.getLastChar().isEmpty()) {
            List<Poetry> candidates = poetryRepository.findByContentStartingWith(ctx.getLastChar(), 200);
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

    private String battleId(Battle battle) {
        return battle.getBattleId() != null ? battle.getBattleId() : String.valueOf(battle.getId());
    }
}
