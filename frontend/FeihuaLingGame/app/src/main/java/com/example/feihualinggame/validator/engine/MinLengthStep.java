package com.example.feihualinggame.validator.engine;

import com.example.feihualinggame.validator.BaseValidationResult;

/**
 * 最小长度检查步骤
 */
public class MinLengthStep implements ValidationStep {

    private final int minLength;

    public MinLengthStep(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public BaseValidationResult execute(RuleContext ctx) {
        String cleanedAnswer = ctx.getAnswer();
        if (cleanedAnswer == null || cleanedAnswer.isEmpty()) {
            return BaseValidationResult.error("答案不能为空");
        }
        if (cleanedAnswer.length() < minLength) {
            return BaseValidationResult.error("请输入完整的诗句（至少" + minLength + "个字）");
        }
        return BaseValidationResult.success(cleanedAnswer);
    }
}
