package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

public class CharCountMatchRule implements AtomicRule {

    @Override
    public String getType() { return "charCountMatch"; }

    @Override
    public String getDescription() {
        return "诗句字数必须与上一句相同";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        String cleaned = ctx.getAnswer();
        if (cleaned == null || cleaned.isEmpty()) {
            return BaseValidationResult.error("答案不能为空");
        }
        int lastLength = ctx.getLastAnswerLength();
        if (lastLength <= 0) {
            return BaseValidationResult.success(cleaned);
        }
        if (cleaned.length() != lastLength) {
            return BaseValidationResult.error("字数不匹配：上一句为" + lastLength + "字，本句为" + cleaned.length() + "字");
        }
        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        return null;
    }
}
