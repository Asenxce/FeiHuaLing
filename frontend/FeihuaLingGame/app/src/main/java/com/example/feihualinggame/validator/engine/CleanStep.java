package com.example.feihualinggame.validator.engine;

import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.example.feihualinggame.validator.BaseValidationResult;

/**
 * 清洗步骤：去除标点 + 繁简转换
 */
public class CleanStep implements ValidationStep {

    public static final CleanStep INSTANCE = new CleanStep();

    private CleanStep() {}

    @Override
    public BaseValidationResult execute(RuleContext ctx) {
        String cleanedAnswer = PoetryCleanUtil.cleanPoetry(ctx.getAnswer());
        ctx.setAnswer(cleanedAnswer);
        return BaseValidationResult.success(cleanedAnswer);
    }
}
