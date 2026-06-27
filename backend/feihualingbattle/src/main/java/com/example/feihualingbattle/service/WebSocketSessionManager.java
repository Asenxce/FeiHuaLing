package com.example.feihualingbattle.service;

import com.example.feihualingbattle.dto.WebSocketMessage;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketSessionManager {
    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private static final String RECONNECT_KEY_PREFIX = "ws:reconnect:";
    private static final long RECONNECT_TTL_SECONDS = 120; // 断连后保留房间状态2分钟

    @Autowired
    private Gson gson;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final Map<Long, jakarta.websocket.Session> userSessions = new ConcurrentHashMap<>();
    private final Map<String, jakarta.websocket.Session> sessionIdMap = new ConcurrentHashMap<>();
    private final Map<Long, String> userRoomMap = new ConcurrentHashMap<>();
    /** 每个 session 的最后活跃时间（毫秒），用于心跳检测 */
    private final Map<String, Long> lastActivityMap = new ConcurrentHashMap<>();

    public void registerSession(Long userId, jakarta.websocket.Session session) {
        userSessions.put(userId, session);
        sessionIdMap.put(session.getId(), session);
        lastActivityMap.put(session.getId(), System.currentTimeMillis());
        log.info("WebSocket连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    public void registerSession(Long userId, jakarta.websocket.Session session, String roomId) {
        userSessions.put(userId, session);
        sessionIdMap.put(session.getId(), session);
        lastActivityMap.put(session.getId(), System.currentTimeMillis());
        if (roomId != null) {
            userRoomMap.put(userId, roomId);
        }
        log.info("WebSocket连接建立: userId={}, sessionId={}, roomId={}", userId, session.getId(), roomId);
    }

    public Map.Entry<Long, String> removeSession(jakarta.websocket.Session session) {
        String sessionId = session.getId();
        Long userId = null;
        for (Map.Entry<Long, jakarta.websocket.Session> entry : userSessions.entrySet()) {
            if (entry.getValue().getId().equals(sessionId)) {
                userId = entry.getKey();
                break;
            }
        }
        String roomId = null;
        if (userId != null) {
            userSessions.remove(userId);
            roomId = userRoomMap.remove(userId);
        }
        sessionIdMap.remove(sessionId);
        lastActivityMap.remove(sessionId);
        log.info("WebSocket连接断开: sessionId={}, userId={}, roomId={}", sessionId, userId, roomId);
        return new AbstractMap.SimpleEntry<>(userId, roomId);
    }

    public void leaveRoom(Long userId) {
        userRoomMap.remove(userId);
        log.info("用户离开房间: userId={}", userId);
    }

    public void joinRoom(Long userId, String roomId) {
        userRoomMap.put(userId, roomId);
        log.info("用户加入房间: userId={}, roomId={}", userId, roomId);
    }

    public jakarta.websocket.Session getUserSession(Long userId) {
        return userSessions.get(userId);
    }

    public boolean isUserOnline(Long userId) {
        jakarta.websocket.Session session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    public String getUserRoomId(Long userId) {
        return userRoomMap.get(userId);
    }

    public void sendToUser(Long userId, String type, String payload) {
        jakarta.websocket.Session session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                WebSocketMessage msg = new WebSocketMessage();
                msg.setType(type);
                msg.setPayload(payload);
                session.getBasicRemote().sendText(gson.toJson(msg));
            } catch (Exception e) {
                log.error("发送WebSocket消息失败: userId={}, type={}", userId, type, e);
            }
        }
    }

    public void broadcastToRoom(String roomId, String type, String payload) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType(type);
        msg.setRoomId(roomId);
        msg.setPayload(payload);
        String json = gson.toJson(msg);

        int sent = 0;
        int missed = 0;
        for (Map.Entry<Long, String> entry : userRoomMap.entrySet()) {
            if (roomId.equals(entry.getValue())) {
                Long userId = entry.getKey();
                jakarta.websocket.Session session = userSessions.get(userId);
                if (session != null && session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(json);
                        sent++;
                    } catch (Exception e) {
                        log.error("广播消息失败: roomId={}, userId={}", roomId, userId, e);
                    }
                } else {
                    missed++;
                }
            }
        }
        if (sent == 0 && missed > 0) {
            log.warn("broadcastToRoom: roomId={} type={} 所有{}个用户均无活跃session，消息丢失", roomId, type, missed);
        }
    }

    public void broadcastToBattlingRoom(String roomId, String type, String payload) {
        broadcastToRoom(roomId, type, payload);
    }

    public int getOnlineCount() {
        return (int) userSessions.values().stream().filter(jakarta.websocket.Session::isOpen).count();
    }

    /**
     * 更新 session 的最后活跃时间（收到任何消息时调用）
     */
    public void touchSession(jakarta.websocket.Session session) {
        if (session != null) {
            lastActivityMap.put(session.getId(), System.currentTimeMillis());
        }
    }

    /**
     * 定期心跳检测：向所有在线 session 发送 ping，关闭超时无响应的连接
     * 每 30 秒执行一次，超过 90 秒未活跃的 session 被关闭
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeatCheck() {
        long now = System.currentTimeMillis();
        long timeoutMs = 90_000; // 90 秒超时

        for (Map.Entry<Long, jakarta.websocket.Session> entry : userSessions.entrySet()) {
            jakarta.websocket.Session session = entry.getValue();
            Long userId = entry.getKey();

            if (!session.isOpen()) {
                continue;
            }

            Long lastActivity = lastActivityMap.get(session.getId());
            if (lastActivity != null && (now - lastActivity) > timeoutMs) {
                log.warn("WebSocket心跳超时，关闭连接: userId={}, 距上次活跃={}ms", userId, now - lastActivity);
                try {
                    session.close(new jakarta.websocket.CloseReason(
                        jakarta.websocket.CloseReason.CloseCodes.NORMAL_CLOSURE, "心跳超时"));
                } catch (Exception e) {
                    log.error("关闭超时session失败: userId={}", userId, e);
                }
                continue;
            }

            // 发送 PING 消息
            try {
                WebSocketMessage ping = new WebSocketMessage();
                ping.setType("PING");
                ping.setPayload(String.valueOf(now));
                session.getBasicRemote().sendText(gson.toJson(ping));
            } catch (Exception e) {
                log.debug("发送心跳ping失败: userId={}", userId);
            }
        }
    }

    /**
     * 保存用户断连后的重连上下文到 Redis（包含房间和对战信息）
     * 用于在断连后短暂保留状态，允许客户端重连恢复
     */
    public void saveReconnectContext(Long userId, String roomId, String battleId) {
        String key = RECONNECT_KEY_PREFIX + userId;
        Map<String, String> context = new java.util.HashMap<>();
        context.put("roomId", roomId);
        context.put("battleId", battleId != null ? battleId : "");
        redisTemplate.opsForHash().putAll(key, context);
        redisTemplate.expire(key, RECONNECT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 获取并清除用户的重连上下文
     * @return 包含 roomId 和 battleId 的 entry，无上下文返回 null
     */
    public Map.Entry<String, String> consumeReconnectContext(Long userId) {
        String key = RECONNECT_KEY_PREFIX + userId;
        Object roomIdObj = redisTemplate.opsForHash().get(key, "roomId");
        Object battleIdObj = redisTemplate.opsForHash().get(key, "battleId");
        if (roomIdObj != null) {
            redisTemplate.delete(key);
            return new AbstractMap.SimpleEntry<>(roomIdObj.toString(),
                    battleIdObj != null ? battleIdObj.toString() : "");
        }
        return null;
    }
}
