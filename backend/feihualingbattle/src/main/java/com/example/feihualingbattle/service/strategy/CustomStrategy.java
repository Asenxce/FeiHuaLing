package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.service.PoetryCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义模式策略
 * 后端强制校验所有已配置的规则（包括位置校验），不信任前端
 */
@Component
public class CustomStrategy implements GameModeStrategy {

    @Autowired
    private PoetryCacheService poetryCacheService;

    private static final int SEARCH_LIMIT = 500;

    @Override
    public String getModeName() { return "custom"; }

    @Override
    public String validate(Battle battle, String normalizedAnswer) {
        if (battle.getKeyword() != null && !battle.getKeyword().isEmpty()
                && !normalizedAnswer.contains(battle.getKeyword())) {
            return "答案必须包含关键字：" + battle.getKeyword();
        }
        if (battle.getKeyword2() != null && !battle.getKeyword2().isEmpty()
                && !normalizedAnswer.contains(battle.getKeyword2())) {
            return "答案必须包含关键字：" + battle.getKeyword2();
        }
        if (battle.getForbiddenWord() != null && !battle.getForbiddenWord().isEmpty()
                && normalizedAnswer.contains(battle.getForbiddenWord())) {
            return "答案不能包含禁止词：" + battle.getForbiddenWord();
        }
        if (battle.getColorKeyword() != null && !battle.getColorKeyword().isEmpty()
                && !normalizedAnswer.contains(battle.getColorKeyword())) {
            return "答案必须包含颜色字：" + battle.getColorKeyword();
        }
        if (battle.getNumberKeyword() != null && !battle.getNumberKeyword().isEmpty()
                && !normalizedAnswer.contains(battle.getNumberKeyword())) {
            return "答案必须包含数字：" + battle.getNumberKeyword();
        }
        // 位置校验：当设置了关键字和位置时，验证关键字出现在指定位置
        if (battle.getKeyword() != null && !battle.getKeyword().isEmpty()
                && battle.getKeywordPosition() != null && battle.getKeywordPosition() > 0) {
            int pos = battle.getKeywordPosition();
            int index = pos - 1;
            if (index >= normalizedAnswer.length()) {
                return "答案长度不足，无法在第 " + pos + " 位放置关键字";
            }
            String kw = battle.getKeyword();
            if (index + kw.length() > normalizedAnswer.length()) {
                return "答案长度不足，第 " + pos + " 位后无法完整包含关键字「" + kw + "」";
            }
            String actual = normalizedAnswer.substring(index, index + kw.length());
            if (!actual.equals(kw)) {
                return "关键字「" + kw + "」必须出现在第 " + pos + " 位，当前在第 "
                        + (normalizedAnswer.indexOf(kw) + 1) + " 位";
            }
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
