package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

/**
 * 排除关键字规则 —— 答案不能包含指定关键字
 * 用于反飞花令模式
 */
public class NotContainsRule implements AtomicRule {

    private final String keyword;

    public NotContainsRule(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String getType() { return "notContains"; }

    @Override
    public String getDescription() {
        return "答案不能包含禁用字「" + keyword + "」";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        String cleaned = ctx.getAnswer();
        if (PoetryCleanUtil.containsKeyword(cleaned, keyword)) {
            return BaseValidationResult.error("答案不能包含禁用字「" + keyword + "」");
        }
        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        if (other instanceof ContainsRule && keyword.equals(other.getTargetKeyword())) {
            return "冲突：同时要求包含和排除关键字「" + keyword + "」";
        }
        return null;
    }

    @Override
    public String getTargetKeyword() { return keyword; }
}
