package com.example.feihualinggame.validator;

/**
 * 基础验证结果类
 * 所有验证结果类型的公共基类，消除重复代码
 */
public class BaseValidationResult implements ValidationResult {
    protected boolean success;
    protected String message;
    protected String cleanedAnswer;

    protected BaseValidationResult(boolean success, String message, String cleanedAnswer) {
        this.success = success;
        this.message = message;
        this.cleanedAnswer = cleanedAnswer;
    }

    public static BaseValidationResult success(String cleanedAnswer) {
        return new BaseValidationResult(true, "验证通过", cleanedAnswer);
    }

    public static BaseValidationResult error(String message) {
        return new BaseValidationResult(false, message, null);
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getCleanedAnswer() {
        return cleanedAnswer;
    }
}
