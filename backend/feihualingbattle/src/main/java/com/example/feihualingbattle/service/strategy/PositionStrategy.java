package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.service.PoetryCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PositionStrategy implements GameModeStrategy {

    @Autowired
    private PoetryCacheService poetryCacheService;

    private static final int SEARCH_LIMIT = 500;

    @Override
    public String getModeName() { return "position"; }

    /**
     * 位置模式验证规则：
     * 1. 答案必须包含关键字
     * 2. 关键字必须出现在指定位置（keywordPosition，1-based）
     */
    @Override
    public String validate(Battle battle, String normalizedAnswer) {
        String keyword = battle.getKeyword();
        if (keyword == null || keyword.isEmpty()) {
            return "关键字未设置";
        }
        if (!normalizedAnswer.contains(keyword)) {
            return "答案必须包含关键字：" + keyword;
        }

        // 校验位置：keywordPosition 从 1 开始计数
        Integer position = battle.getKeywordPosition();
        if (position != null && position > 0) {
            int index = position - 1; // 转为 0-based 索引
            if (index >= normalizedAnswer.length()) {
                return "答案长度不足，无法在第 " + position + " 位放置关键字";
            }
            // 关键字可能为多字符，检查从 position 开始是否匹配
            if (index + keyword.length() > normalizedAnswer.length()) {
                return "答案长度不足，第 " + position + " 位后无法完整包含关键字「" + keyword + "」";
            }
            String actual = normalizedAnswer.substring(index, index + keyword.length());
            if (!actual.equals(keyword)) {
                return "关键字「" + keyword + "」必须出现在第 " + position + " 位，当前在第 "
                        + (normalizedAnswer.indexOf(keyword) + 1) + " 位";
            }
        }

        return null;
    }

    @Override
    public List<Poetry> searchPoetry(GameModeContext ctx) {
        String normalizedAnswer = BattleServiceHelper.normalize(ctx.getAnswer());
        List<Poetry> candidates = poetryCacheService.getCandidatesByKeyword(ctx.getKeyword(), SEARCH_LIMIT);
        return candidates.stream()
            .filter(p -> BattleServiceHelper.normalize(p.getContent()).equals(normalizedAnswer))
            .collect(Collectors.toList());
    }

    @Override
    public List<Poetry> getAIAnswer(GameModeContext ctx, List<String> usedPoems) {
        List<Poetry> candidates;
        if (ctx.getKeywordPosition() != null && ctx.getKeywordPosition() > 0) {
            candidates = poetryCacheService.getCandidatesByKeyword(ctx.getKeyword(), 200).stream()
                .filter(p -> p.getContent() != null && p.getContent().length() >= ctx.getKeywordPosition()
                    && String.valueOf(p.getContent().charAt(ctx.getKeywordPosition() - 1)).equals(ctx.getKeyword()))
                .collect(Collectors.toList());
        } else {
            candidates = poetryCacheService.getCandidatesByKeyword(ctx.getKeyword(), 200);
        }
        if (usedPoems != null && !usedPoems.isEmpty()) {
            java.util.Set<String> usedSet = new java.util.HashSet<>(usedPoems);
            candidates = candidates.stream()
                .filter(p -> !usedSet.contains(p.getContent()))
                .collect(java.util.stream.Collectors.toList());
        }
        return candidates;
    }
}
