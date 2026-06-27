package com.example.feihualinggame.validator.engine;

import com.example.feihualinggame.validator.BaseValidationResult;

import java.util.HashSet;
import java.util.Set;

/**
 * 去重检查步骤
 */
public class DuplicateCheckStep implements ValidationStep {

    private final Set<String> usedPoems = new HashSet<>();

    @Override
    public BaseValidationResult execute(RuleContext ctx) {
        String cleanedAnswer = ctx.getAnswer();
        if (usedPoems.contains(cleanedAnswer)) {
            return BaseValidationResult.error("这句诗已经使用过了，请换一句");
        }
        return BaseValidationResult.success(cleanedAnswer);
    }

    public int getUsedCount() {
        return usedPoems.size();
    }

    public void clear() {
        usedPoems.clear();
    }
}
