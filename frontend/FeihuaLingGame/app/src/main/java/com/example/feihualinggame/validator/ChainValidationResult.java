package com.example.feihualinggame.validator;

/**
 * 首尾接龙验证结果
 * 继承BaseValidationResult，额外包含nextChar字段
 */
public class ChainValidationResult extends BaseValidationResult {
    private final Character nextChar;

    private ChainValidationResult(boolean success, String message, String cleanedAnswer, Character nextChar) {
        super(success, message, cleanedAnswer);
        this.nextChar = nextChar;
    }

    public static ChainValidationResult success(String cleanedAnswer, Character nextChar) {
        return new ChainValidationResult(true, "验证通过", cleanedAnswer, nextChar);
    }

    public static ChainValidationResult error(String message) {
        return new ChainValidationResult(false, message, null, null);
    }

    public Character getNextChar() {
        return nextChar;
    }
}
