package com.example.feihualinggame.validator;

import com.example.feihualinggame.utils.PoetryCleanUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 反飞花令验证器
 * 规则：答案不能包含指定的禁用关键字
 */
public class AntiFeiHuaLing implements FeiHuaLingValidator {
    private final String forbiddenKeyword;
    private final Set<String> usedPoems;

    public AntiFeiHuaLing(String forbiddenKeyword) {
        this.forbiddenKeyword = forbiddenKeyword;
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

        if (PoetryCleanUtil.containsKeyword(cleanedAnswer, forbiddenKeyword)) {
            return BaseValidationResult.error("答案不能包含禁用字「" + forbiddenKeyword + "」");
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
