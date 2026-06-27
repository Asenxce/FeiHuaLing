package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

/**
 * 关键字位置规则 —— 关键字必须在指定位置出现
 */
public class PositionRule implements AtomicRule {

    private final String keyword;
    private final int position; // 1-based

    public PositionRule(String keyword, int position) {
        this.keyword = keyword;
        this.position = position;
    }

    @Override
    public String getType() { return "position"; }

    @Override
    public String getDescription() {
        return "关键字「" + keyword + "」必须在第 " + position + " 位";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        String cleaned = ctx.getAnswer();

        if (cleaned.length() < position) {
            return BaseValidationResult.error("诗句太短，关键字「" + keyword + "」应在第" + position + "位");
        }

        if (!PoetryCleanUtil.isKeywordAtPosition(cleaned, keyword, position)) {
            char actual = cleaned.charAt(position - 1);
            return BaseValidationResult.error("「" + keyword + "」必须在第" + position + "位（当前是「" + actual + "」）");
        }

        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        if (other instanceof PositionRule) {
            PositionRule o = (PositionRule) other;
            if (keyword.equals(o.keyword) && position != o.position) {
                return "冲突：关键字「" + keyword + "」不能同时在第" + position + "位和第" + o.position + "位";
            }
        }
        if (other instanceof NotContainsRule && keyword.equals(other.getTargetKeyword())) {
            return "冲突：位置规则要求包含「" + keyword + "」，但排除规则禁止";
        }
        return null;
    }

    @Override
    public String getTargetKeyword() { return keyword; }

    public int getKeywordPosition() { return position; }
}
