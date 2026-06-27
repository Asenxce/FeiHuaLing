package com.example.feihualinggame.bean;

/**
 * 游戏模式枚举
 */
public enum GameMode {
    SIMPLE("单关键字飞花令", "single_keyword"),

    CLASSIC_POSITION("位置飞花令", "position"),
    CLASSIC_DOUBLE("双关键字飞花令", "double_keyword"),

    ENTERTAINMENT_CHAIN("首尾接龙飞花令", "chain"),
    ENTERTAINMENT_COLOR("颜色飞花令", "color"),
    ENTERTAINMENT_NUMBER("数字飞花令", "number"),
    ENTERTAINMENT_FORBIDDEN("反飞花令", "forbidden"),

    CUSTOM("自定义飞花令", "custom");
    
    private final String displayName;
    private final String code;
    
    GameMode(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 根据代码获取游戏模式
     */
    public static GameMode fromCode(String code) {
        for (GameMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return SIMPLE; // 默认返回简易模式
    }
}
