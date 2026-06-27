package com.example.feihualingbattle.enums;

/**
 * 房间玩家状态枚举
 */
public enum RoomPlayerStatus {
    
    /** 等待中 */
    WAITING("WAITING"),
    
    /** 对战中 */
    BATTLE("BATTLE"),
    
    /** 已淘汰 */
    ELIMINATED("ELIMINATED"),
    
    /** 对战完成 */
    FINISHED("FINISHED"),
    
    /** 已退出房间 */
    QUIT("QUIT");

    private final String value;

    RoomPlayerStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 从字符串值解析枚举
     * @param value 状态字符串
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果值无效
     */
    public static RoomPlayerStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("玩家状态不能为null");
        }
        for (RoomPlayerStatus status : RoomPlayerStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的玩家状态: " + value);
    }
}
