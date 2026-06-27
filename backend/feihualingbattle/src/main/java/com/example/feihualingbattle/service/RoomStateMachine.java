package com.example.feihualingbattle.service;

import com.example.feihualingbattle.enums.RoomPlayerStatus;
import com.example.feihualingbattle.enums.RoomStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 房间状态机 - 集中管理状态转换规则
 */
@Component
public class RoomStateMachine {
    private static final Logger log = LoggerFactory.getLogger(RoomStateMachine.class);

    /**
     * 房间状态转换规则
     * Key: 当前状态, Value: 允许转换到的目标状态集合
     */
    private static final Map<RoomStatus, Set<RoomStatus>> ROOM_TRANSITIONS = new EnumMap<>(RoomStatus.class);

    /**
     * 玩家状态转换规则
     */
    private static final Map<RoomPlayerStatus, Set<RoomPlayerStatus>> PLAYER_TRANSITIONS = new EnumMap<>(RoomPlayerStatus.class);

    static {
        // 房间状态转换规则
        ROOM_TRANSITIONS.put(RoomStatus.WAITING, EnumSet.of(RoomStatus.BATTLE, RoomStatus.DISMISSED));
        ROOM_TRANSITIONS.put(RoomStatus.BATTLE, EnumSet.of(RoomStatus.FINISHED, RoomStatus.DISMISSED));
        ROOM_TRANSITIONS.put(RoomStatus.FINISHED, EnumSet.noneOf(RoomStatus.class)); // 终态
        ROOM_TRANSITIONS.put(RoomStatus.DISMISSED, EnumSet.noneOf(RoomStatus.class)); // 终态

        // 玩家状态转换规则
        PLAYER_TRANSITIONS.put(RoomPlayerStatus.WAITING, EnumSet.of(RoomPlayerStatus.BATTLE, RoomPlayerStatus.QUIT));
        PLAYER_TRANSITIONS.put(RoomPlayerStatus.BATTLE, EnumSet.of(RoomPlayerStatus.ELIMINATED, RoomPlayerStatus.FINISHED, RoomPlayerStatus.QUIT));
        PLAYER_TRANSITIONS.put(RoomPlayerStatus.ELIMINATED, EnumSet.of(RoomPlayerStatus.FINISHED));
        PLAYER_TRANSITIONS.put(RoomPlayerStatus.FINISHED, EnumSet.noneOf(RoomPlayerStatus.class)); // 终态
        PLAYER_TRANSITIONS.put(RoomPlayerStatus.QUIT, EnumSet.noneOf(RoomPlayerStatus.class)); // 终态
    }

    /**
     * 验证房间状态转换是否合法
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许转换
     */
    public boolean canTransition(RoomStatus currentStatus, RoomStatus targetStatus) {
        Set<RoomStatus> allowed = ROOM_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * 验证房间状态转换，如果不合法则抛出异常
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @param operation 操作描述（用于错误信息）
     * @throws IllegalStateException 如果转换不合法
     */
    public void validateRoomTransition(RoomStatus currentStatus, RoomStatus targetStatus, String operation) {
        if (!canTransition(currentStatus, targetStatus)) {
            String msg = String.format("房间状态转换失败: %s -> %s, 操作: %s", 
                    currentStatus.getValue(), targetStatus.getValue(), operation);
            log.warn(msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * 验证玩家状态转换是否合法
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否允许转换
     */
    public boolean canPlayerTransition(RoomPlayerStatus currentStatus, RoomPlayerStatus targetStatus) {
        Set<RoomPlayerStatus> allowed = PLAYER_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * 验证玩家状态转换，如果不合法则抛出异常
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @param operation 操作描述（用于错误信息）
     * @throws IllegalStateException 如果转换不合法
     */
    public void validatePlayerTransition(RoomPlayerStatus currentStatus, RoomPlayerStatus targetStatus, String operation) {
        if (!canPlayerTransition(currentStatus, targetStatus)) {
            String msg = String.format("玩家状态转换失败: %s -> %s, 操作: %s", 
                    currentStatus.getValue(), targetStatus.getValue(), operation);
            log.warn(msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * 检查房间是否处于终态（FINISHED或DISMISSED）
     * @param status 房间状态
     * @return 是否为终态
     */
    public boolean isRoomTerminal(RoomStatus status) {
        return status == RoomStatus.FINISHED || status == RoomStatus.DISMISSED;
    }

    /**
     * 检查玩家是否处于终态（FINISHED、ELIMINATED或QUIT）
     * @param status 玩家状态
     * @return 是否为终态
     */
    public boolean isPlayerTerminal(RoomPlayerStatus status) {
        return status == RoomPlayerStatus.FINISHED || 
               status == RoomPlayerStatus.ELIMINATED || 
               status == RoomPlayerStatus.QUIT;
    }

    /**
     * 获取房间所有可能的目标状态
     * @param currentStatus 当前状态
     * @return 允许转换到的状态集合
     */
    public Set<RoomStatus> getAllowedTransitions(RoomStatus currentStatus) {
        Set<RoomStatus> allowed = ROOM_TRANSITIONS.get(currentStatus);
        return allowed != null ? Collections.unmodifiableSet(allowed) : Collections.emptySet();
    }

    /**
     * 获取玩家所有可能的目标状态
     * @param currentStatus 当前状态
     * @return 允许转换到的状态集合
     */
    public Set<RoomPlayerStatus> getAllowedPlayerTransitions(RoomPlayerStatus currentStatus) {
        Set<RoomPlayerStatus> allowed = PLAYER_TRANSITIONS.get(currentStatus);
        return allowed != null ? Collections.unmodifiableSet(allowed) : Collections.emptySet();
    }
}
