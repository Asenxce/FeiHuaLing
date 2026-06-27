package com.example.feihualinggame.validator;

/**
 * 通用验证结果接口
 */
public interface ValidationResult {
    boolean isSuccess();
    String getMessage();
    String getCleanedAnswer();
}
