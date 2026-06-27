package com.example.feihualinggame.validator.engine;

import com.example.feihualinggame.validator.BaseValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证管道 —— 规则引擎的执行核心
 * 将验证流程拆分为可组合的管道节点：
 *   清洗 → 最小长度 → 关键字匹配 → 去重 → 记录
 * 不同游戏模式通过替换 KeywordMatchStep 实现规则差异化
 */
public class ValidationPipeline {

    private final List<ValidationStep> steps = new ArrayList<>();

    public ValidationPipeline addStep(ValidationStep step) {
        steps.add(step);
        return this;
    }

    /**
     * 执行全部管道步骤，任一失败则短路返回
     */
    public BaseValidationResult execute(RuleContext ctx) {
        for (ValidationStep step : steps) {
            BaseValidationResult result = step.execute(ctx);
            if (!result.isSuccess()) {
                return result;
            }
        }
        return BaseValidationResult.success(ctx.getAnswer());
    }
}
