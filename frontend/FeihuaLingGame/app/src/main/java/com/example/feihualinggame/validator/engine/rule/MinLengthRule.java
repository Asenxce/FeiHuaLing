package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

/**
 * 最小长度规则 —— 答案字符数不低于指定值
 */
public class MinLengthRule implements AtomicRule {

    private final int minLength;

    public MinLengthRule(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public String getType() { return "minLength"; }

    @Override
    public String getDescription() {
        return "答案至少 " + minLength + " 个字";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        String cleaned = ctx.getAnswer();
        if (cleaned == null || cleaned.isEmpty()) {
            return BaseValidationResult.error("答案不能为空");
        }
        if (cleaned.length() < minLength) {
            return BaseValidationResult.error("请输入完整的诗句（至少" + minLength + "个字）");
        }
        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        return null; // 最小长度不与其他规则冲突
    }
}
