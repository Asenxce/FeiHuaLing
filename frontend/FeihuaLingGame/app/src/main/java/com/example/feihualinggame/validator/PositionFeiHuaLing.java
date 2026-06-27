package com.example.feihualinggame.validator;

import com.example.feihualinggame.utils.PoetryCleanUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 位置飞花令验证器
 * 规则：关键字必须在指定位置（从1开始计数）
 */
public class PositionFeiHuaLing implements FeiHuaLingValidator {
    private final String keyword;
    private final int position;
    private final Set<String> usedPoems;

    public PositionFeiHuaLing(String keyword, int position) {
        this.keyword = keyword;
        this.position = position;
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

        if (cleanedAnswer.length() < position) {
            return BaseValidationResult.error("诗句太短，关键字「" + keyword + "」应在第" + position + "位");
        }

        if (!PoetryCleanUtil.isKeywordAtPosition(cleanedAnswer, keyword, position)) {
            char actualChar = cleanedAnswer.charAt(position - 1);
            return BaseValidationResult.error("「" + keyword + "」必须在第" + position + "个位置（当前是「" + actualChar + "」）");
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
