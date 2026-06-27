package com.example.feihualinggame.validator.engine;

import com.example.feihualinggame.validator.BaseValidationResult;

import java.util.HashSet;
import java.util.Set;

/**
 * 记录步骤：验证通过后将答案标记为已使用
 */
public class RecordStep implements ValidationStep {

    private final Set<String> usedPoems = new HashSet<>();

    @Override
    public BaseValidationResult execute(RuleContext ctx) {
        usedPoems.add(ctx.getAnswer());
        return BaseValidationResult.success(ctx.getAnswer());
    }

    public int getUsedCount() {
        return usedPoems.size();
    }

    public void clear() {
        usedPoems.clear();
    }
}
