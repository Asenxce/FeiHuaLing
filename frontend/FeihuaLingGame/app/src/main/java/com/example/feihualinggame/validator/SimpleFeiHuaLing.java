package com.example.feihualinggame.validator;

import com.example.feihualinggame.utils.PoetryCleanUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 简易模式验证器
 * 规则：答案包含关键字即可
 */
public class SimpleFeiHuaLing implements FeiHuaLingValidator {
    private final String keyword;
    private final Set<String> usedPoems;

    public SimpleFeiHuaLing(String keyword) {
        this.keyword = keyword;
        this.usedPoems = new HashSet<>();
    }

    @Override
    public BaseValidationResult validate(String answer) {
        String cleanedAnswer = PoetryCleanUtil.cleanPoetry(answer);

        if (cleanedAnswer.isEmpty()) {
            return BaseValidationResult.error("答案不能为空");
        }

        if (cleanedAnswer.length() < 2) {
            return BaseValidationResult.error("请输入完整的诗句（至少2个字）");
        }

        if (!PoetryCleanUtil.containsKeyword(cleanedAnswer, keyword)) {
            return BaseValidationResult.error("答案必须包含关键字「" + keyword + "」");
        }

        if (usedPoems.contains(cleanedAnswer)) {
            return BaseValidationResult.error("这句诗已经使用过了，请换一句");
        }

        usedPoems.add(cleanedAnswer);
        return BaseValidationResult.success(cleanedAnswer);
    }

    @Override
    public int getUsedCount() {
        return usedPoems.size();
    }

    @Override
    public void clear() {
        usedPoems.clear();
    }
}
