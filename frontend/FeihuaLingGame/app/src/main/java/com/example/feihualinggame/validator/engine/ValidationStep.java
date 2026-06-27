package com.example.feihualinggame.validator.engine;

import com.example.feihualinggame.validator.BaseValidationResult;

/**
 * 验证管道步骤接口
 * 每个步骤接收上下文并返回验证结果
 */
public interface ValidationStep {
    BaseValidationResult execute(RuleContext ctx);
}
