package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.utils.BattlePoetryCacheManager;
import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

import java.util.Set;

/**
 * 不重复规则 —— 已使用过的诗句不能再次提交
 * 共享 BattlePoetryCacheManager 的去重集合
 */
public class NoRepeatRule implements AtomicRule {

    @Override
    public String getType() { return "noRepeat"; }

    @Override
    public String getDescription() {
        return "不能重复使用已提交过的诗句";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        String cleaned = ctx.getAnswer();
        Set<String> usedPoems = BattlePoetryCacheManager.getUsedPoemsSet();
        if (usedPoems.contains(cleaned)) {
            return BaseValidationResult.error("这句诗已经使用过了，请换一句");
        }
        BattlePoetryCacheManager.markPoemAsUsed(cleaned);
        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        return null;
    }
}
