package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

/**
 * 原子规则 —— 规则引擎的最小执行单元
 *
 * 每种规则类型不可再分，通过组合实现复杂逻辑。
 * 原子规则之间可以检测冲突（如 contains + notContains 同关键字）。
 *
 * 设计原则：
 *   1. 单一职责：每个 AtomicRule 只校验一条规则
 *   2. 可组合：通过 RuleComposer 自由组合
 *   3. 可检测：提供 conflictsWith() 实现冲突消解
 */
public interface AtomicRule {

    /** 规则类型标识，如 "contains", "position", "minLength" */
    String getType();

    /** 人类可读描述，用于 UI 提示和调试 */
    String getDescription();

    /** 执行规则校验，返回校验结果 */
    BaseValidationResult evaluate(RuleContext ctx);

    /**
     * 检测与另一条规则的冲突
     * @return null 表示无冲突，非 null 为冲突描述
     */
    String conflictsWith(AtomicRule other);

    /** 此规则操作的目标关键字（null 表示无关键字） */
    default String getTargetKeyword() { return null; }
}
