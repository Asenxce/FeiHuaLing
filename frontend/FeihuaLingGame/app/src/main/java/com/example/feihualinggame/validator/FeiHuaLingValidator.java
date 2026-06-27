package com.example.feihualinggame.validator;

/**
 * 飞花令验证器统一接口
 * 所有游戏模式验证器必须实现此接口
 */
public interface FeiHuaLingValidator {
    BaseValidationResult validate(String answer);
    int getUsedCount();
    void clear();
}
