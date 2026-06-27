package com.example.feihualingbattle.enums;

/**
 * 房间状态枚举
 */
public enum RoomStatus {
    
    /** 等待玩家加入 */
    WAITING("WAITING"),
    
    /** 对战进行中 */
    BATTLE("BATTLE"),
    
    /** 对战结束 */
    FINISHED("FINISHED"),
    
    /** 房间已解散 */
    DISMISSED("DISMISSED");

    private final String value;

    RoomStatus(String value) {
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
    public static RoomStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("房间状态不能为null");
        }
        for (RoomStatus status : RoomStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的房间状态: " + value);
    }
}
