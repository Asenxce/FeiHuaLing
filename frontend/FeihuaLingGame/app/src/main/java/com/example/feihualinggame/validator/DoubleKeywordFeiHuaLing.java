package com.example.feihualinggame.validator;

import com.example.feihualinggame.utils.PoetryCleanUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 双字飞花令验证器
 * 规则：答案必须同时包含两个关键字，位置不限
 */
public class DoubleKeywordFeiHuaLing implements FeiHuaLingValidator {
    private final String keyword1;
    private final String keyword2;
    private final Set<String> usedPoems;

    public DoubleKeywordFeiHuaLing(String keyword1, String keyword2) {
        this.keyword1 = keyword1;
        this.keyword2 = keyword2;
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

        boolean hasKeyword1 = PoetryCleanUtil.containsKeyword(cleanedAnswer, keyword1);
        boolean hasKeyword2 = PoetryCleanUtil.containsKeyword(cleanedAnswer, keyword2);

        if (!hasKeyword1 && !hasKeyword2) {
            return BaseValidationResult.error("答案必须同时包含「" + keyword1 + "」和「" + keyword2 + "」");
        } else if (!hasKeyword1) {
            return BaseValidationResult.error("答案缺少关键字「" + keyword1 + "」");
        } else if (!hasKeyword2) {
            return BaseValidationResult.error("答案缺少关键字「" + keyword2 + "」");
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
