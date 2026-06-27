package com.example.feihualinggame.validator.engine.rule;

import android.util.Log;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则组合器 —— 将多条原子规则组合为可执行的验证链
 *
 * 职责：
 * 1. 接收 AtomicRule 列表，生成组合描述
 * 2. 按顺序执行原子规则，任一失败即短路
 * 3. 维护规则列表的不可变视图
 */
public class RuleComposer {

    private static final String TAG = "RuleComposer";

    private final List<AtomicRule> rules;
    private final String composedDescription;

    public RuleComposer(List<AtomicRule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        this.composedDescription = buildDescription();
    }

    /**
     * 按顺序执行所有原子规则
     */
    public BaseValidationResult execute(RuleContext ctx) {
        for (int i = 0; i < rules.size(); i++) {
            AtomicRule rule = rules.get(i);
            try {
                BaseValidationResult result = rule.evaluate(ctx);
                if (!result.isSuccess()) {
                    Log.d(TAG, "规则[" + i + "] " + rule.getType() + " 未通过: " + result.getMessage());
                    return result;
                }
            } catch (Exception e) {
                Log.e(TAG, "规则[" + i + "] " + rule.getType() + " 执行异常", e);
                return BaseValidationResult.error("规则引擎内部错误: " + rule.getType());
            }
        }
        return BaseValidationResult.success(ctx.getAnswer());
    }

    /**
     * 获取规则组合的人类可读描述（用于 UI 提示）
     */
    public String getDescription() {
        return composedDescription;
    }

    /**
     * 获取原子规则数量
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * 获取不可变规则列表
     */
    public List<AtomicRule> getRules() {
        return rules;
    }

    private String buildDescription() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(rules.get(i).getDescription());
        }
        return sb.toString();
    }
}
