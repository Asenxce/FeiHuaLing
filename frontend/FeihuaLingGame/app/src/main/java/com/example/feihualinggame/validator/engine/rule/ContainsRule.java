package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

/**
 * 包含关键字规则 —— 答案必须包含指定关键字
 */
public class ContainsRule implements AtomicRule {

    private final String keyword;

    public ContainsRule(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String getType() { return "contains"; }

    @Override
    public String getDescription() {
        return "答案必须包含关键字「" + keyword + "」";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        String cleaned = ctx.getAnswer();
        if (!PoetryCleanUtil.containsKeyword(cleaned, keyword)) {
            return BaseValidationResult.error("答案必须包含关键字「" + keyword + "」");
        }
        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        if (other instanceof NotContainsRule && keyword.equals(other.getTargetKeyword())) {
            return "冲突：同时要求包含和排除关键字「" + keyword + "」";
        }
        return null;
    }

    @Override
    public String getTargetKeyword() { return keyword; }
}
