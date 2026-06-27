package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

public class MaxLengthRule implements AtomicRule {

    private final int maxLength;

    public MaxLengthRule(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public String getType() { return "maxLength"; }

    @Override
    public String getDescription() {
        return "诗句长度不能超过 " + maxLength + " 个字";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        String cleaned = ctx.getAnswer();
        if (cleaned == null || cleaned.isEmpty()) {
            return BaseValidationResult.error("答案不能为空");
        }
        if (cleaned.length() > maxLength) {
            return BaseValidationResult.error("诗句过长，不能超过" + maxLength + "个字");
        }
        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        return null;
    }
}
