package com.example.feihualinggame.validator.engine.rule;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;

/**
 * 首尾接龙规则 —— 答案首字必须匹配指定字符
 */
public class ChainStartRule implements AtomicRule {

    /** 通过 RuleContext.getLastChar() 动态获取上一句尾字 */
    public ChainStartRule() {}

    @Override
    public String getType() { return "chainStart"; }

    @Override
    public String getDescription() {
        return "答案首字必须与上一句尾字相同（首尾接龙）";
    }

    @Override
    public BaseValidationResult evaluate(RuleContext ctx) {
        Character lastChar = ctx.getLastChar();
        if (lastChar == null) {
            return BaseValidationResult.error("游戏尚未开始，请稍后再试");
        }
        String cleaned = ctx.getAnswer();
        if (cleaned.isEmpty()) {
            return BaseValidationResult.error("答案不能为空");
        }
        char firstChar = cleaned.charAt(0);
        if (firstChar != lastChar) {
            return BaseValidationResult.error("首字必须是「" + lastChar + "」（当前是「" + firstChar + "」）");
        }
        // 更新上下文：下一句的首字 = 本句的尾字
        ctx.setLastChar(cleaned.charAt(cleaned.length() - 1));
        return BaseValidationResult.success(cleaned);
    }

    @Override
    public String conflictsWith(AtomicRule other) {
        if (other instanceof PositionRule && ((PositionRule) other).getKeywordPosition() == 1) {
            return "冲突：接龙规则要求首字由上一句决定，与位置规则(第1位)要求冲突";
        }
        return null;
    }
}
