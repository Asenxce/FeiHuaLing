package com.example.feihualinggame.validator.engine;

import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.example.feihualinggame.validator.BaseValidationResult;

/**
 * 关键字匹配步骤 —— 规则引擎的核心可替换节点
 * 不同游戏模式在此步骤使用不同的匹配策略
 */
public class KeywordMatchStep implements ValidationStep {

    private final String gameMode;

    public KeywordMatchStep(String gameMode) {
        this.gameMode = gameMode;
    }

    @Override
    public BaseValidationResult execute(RuleContext ctx) {
        String cleanedAnswer = ctx.getAnswer();

        switch (gameMode) {
            case "single_keyword":
            case "color":
            case "number":
                return checkSingleKeyword(cleanedAnswer, ctx.getKeyword());

            case "position":
                return checkPosition(cleanedAnswer, ctx.getKeyword(), ctx.getKeywordPosition());

            case "double_keyword":
                return checkDoubleKeyword(cleanedAnswer, ctx.getKeyword(), ctx.getKeyword2());

            case "chain":
                return checkChain(cleanedAnswer, ctx);

            case "forbidden":
                return checkForbidden(cleanedAnswer, ctx.getKeyword());
        }

        return BaseValidationResult.success(cleanedAnswer);
    }

    private BaseValidationResult checkSingleKeyword(String cleanedAnswer, String keyword) {
        if (!PoetryCleanUtil.containsKeyword(cleanedAnswer, keyword)) {
            return BaseValidationResult.error("答案必须包含关键字「" + keyword + "」");
        }
        return BaseValidationResult.success(cleanedAnswer);
    }

    private BaseValidationResult checkPosition(String cleanedAnswer, String keyword, int position) {
        if (cleanedAnswer.length() < position) {
            return BaseValidationResult.error("诗句太短，关键字「" + keyword + "」应在第" + position + "位");
        }
        if (!PoetryCleanUtil.isKeywordAtPosition(cleanedAnswer, keyword, position)) {
            char actualChar = cleanedAnswer.charAt(position - 1);
            return BaseValidationResult.error("「" + keyword + "」必须在第" + position + "个位置（当前是「" + actualChar + "」）");
        }
        return BaseValidationResult.success(cleanedAnswer);
    }

    private BaseValidationResult checkDoubleKeyword(String cleanedAnswer, String keyword1, String keyword2) {
        boolean hasK1 = PoetryCleanUtil.containsKeyword(cleanedAnswer, keyword1);
        boolean hasK2 = PoetryCleanUtil.containsKeyword(cleanedAnswer, keyword2);
        if (!hasK1 && !hasK2) {
            return BaseValidationResult.error("答案必须同时包含「" + keyword1 + "」和「" + keyword2 + "」");
        } else if (!hasK1) {
            return BaseValidationResult.error("答案缺少关键字「" + keyword1 + "」");
        } else if (!hasK2) {
            return BaseValidationResult.error("答案缺少关键字「" + keyword2 + "」");
        }
        return BaseValidationResult.success(cleanedAnswer);
    }

    private BaseValidationResult checkChain(String cleanedAnswer, RuleContext ctx) {
        Character lastChar = ctx.getLastChar();
        if (lastChar == null) {
            return BaseValidationResult.error("游戏尚未开始，请稍后再试");
        }
        char firstChar = cleanedAnswer.charAt(0);
        if (firstChar != lastChar) {
            return BaseValidationResult.error("首字必须是「" + lastChar + "」（当前是「" + firstChar + "」）");
        }
        ctx.setLastChar(cleanedAnswer.charAt(cleanedAnswer.length() - 1));
        return BaseValidationResult.success(cleanedAnswer);
    }

    private BaseValidationResult checkForbidden(String cleanedAnswer, String forbiddenKeyword) {
        if (PoetryCleanUtil.containsKeyword(cleanedAnswer, forbiddenKeyword)) {
            return BaseValidationResult.error("答案不能包含禁用字「" + forbiddenKeyword + "」");
        }
        return BaseValidationResult.success(cleanedAnswer);
    }
}
