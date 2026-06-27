package com.example.feihualingbattle.config;

import com.example.feihualingbattle.entity.Room;
import com.example.feihualingbattle.repository.RoomRepository;
import com.example.feihualingbattle.service.MultiplayerBattleService;
import com.example.feihualingbattle.service.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Map;
import java.util.concurrent.*;

@Component
@ServerEndpoint("/ws/room")
public class WebSocketServer {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /** 断连后延迟 15 秒再执行认输，允许客户端在此期间重连 */
    private static final long SURRENDER_DELAY_SECONDS = 15;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private static WebSocketSessionManager sessionManager;
    private static StringRedisTemplate redisTemplate;
    private static MultiplayerBattleService multiplayerBattleService;
    private static RoomRepository roomRepository;

    @Autowired
    public void setSessionManager(WebSocketSessionManager sessionManager) {
        WebSocketServer.sessionManager = sessionManager;
    }

    @Autowired
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        WebSocketServer.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setMultiplayerBattleService(MultiplayerBattleService multiplayerBattleService) {
        WebSocketServer.multiplayerBattleService = multiplayerBattleService;
    }

    @Autowired
    public void setRoomRepository(RoomRepository roomRepository) {
        WebSocketServer.roomRepository = roomRepository;
    }

    @OnOpen
    public void onOpen(Session session) {
        log.debug("WebSocket连接打开: sessionId={}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        if (sessionManager != null) {
            Map.Entry<Long, String> disconnectInfo = sessionManager.removeSession(session);
            handleDisconnect(disconnectInfo);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        // Connection reset 是客户端断开连接时的正常网络行为
        // ClosedChannelException 是应用重启/关闭时的正常行为
        if (error instanceof java.net.SocketException
                || error instanceof java.nio.channels.ClosedChannelException
                || (error.getCause() instanceof java.nio.channels.ClosedChannelException)) {
            log.debug("WebSocket连接关闭: sessionId={}, reason={}", session.getId(), error.getMessage());
        } else {
            log.error("WebSocket错误: sessionId={}", session.getId(), error);
        }
        if (sessionManager != null) {
            Map.Entry<Long, String> disconnectInfo = sessionManager.removeSession(session);
            handleDisconnect(disconnectInfo);
        }
    }

    /**
     * 断连处理：保存重连上下文到 Redis，并延迟 15 秒执行认输
     * 如果用户在此期间重连，认输任务会被取消
     */
    private void handleDisconnect(Map.Entry<Long, String> disconnectInfo) {
        if (disconnectInfo == null || disconnectInfo.getKey() == null || disconnectInfo.getValue() == null) {
            return;
        }
        Long userId = disconnectInfo.getKey();
        String roomId = disconnectInfo.getValue();

        try {
            if (roomRepository != null) {
                Room room = roomRepository.findByRoomId(roomId).orElse(null);
                if (room != null && "BATTLE".equals(room.getStatus()) && room.getBattleId() != null) {
                    // 保存重连上下文到 Redis
                    sessionManager.saveReconnectContext(userId, roomId, room.getBattleId());

                    // 延迟执行认输，允许客户端重连恢复
                    String surrenderTaskKey = "surrender:task:" + userId;
                    ScheduledFuture<?> future = scheduler.schedule(() -> {
                        try {
                            // 检查用户是否已重连（重连后重连上下文会被清除）
                            Map.Entry<String, String> ctx = sessionManager.consumeReconnectContext(userId);
                            if (ctx == null) {
                                log.info("用户已重连，取消认输: userId={}", userId);
                                return;
                            }

                            // 用户未重连，执行认输
                            Room currentRoom = roomRepository.findByRoomId(roomId).orElse(null);
                            if (currentRoom != null && "BATTLE".equals(currentRoom.getStatus())) {
                                log.info("断连超时触发认输: userId={}, roomId={}, battleId={}", userId, roomId, currentRoom.getBattleId());
                                multiplayerBattleService.handleSurrender(currentRoom.getBattleId(), roomId, userId);
                            }
                        } catch (Exception e) {
                            log.error("延迟认输处理失败: userId={}, roomId={}", userId, roomId, e);
                        }
                    }, SURRENDER_DELAY_SECONDS, TimeUnit.SECONDS);

                    // 将 future 存入 Redis 以便重连时取消
                    if (redisTemplate != null) {
                        redisTemplate.opsForValue().set(surrenderTaskKey, "pending", SURRENDER_DELAY_SECONDS + 5, TimeUnit.SECONDS);
                    }

                    log.info("断连已保存重连上下文，延迟{}秒认输: userId={}, roomId={}", SURRENDER_DELAY_SECONDS, userId, roomId);
                }
            }
        } catch (Exception e) {
            log.error("WebSocket断开处理失败: userId={}, roomId={}", userId, roomId, e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 更新 session 活跃时间（心跳检测）
        if (sessionManager != null) {
            sessionManager.touchSession(session);
        }

        try {
            if (message.contains("\"type\":\"REGISTER\"")) {
                String userIdStr = extractValue(message, "userId");
                String sessionId = extractStringValue(message, "sessionId");
                if (userIdStr != null) {
                    Long userId = Long.parseLong(userIdStr);

                    if (sessionId != null && redisTemplate != null) {
                        String redisSessionId = redisTemplate.opsForValue().get("user:session:" + userId);
                        if (redisSessionId != null && !redisSessionId.equals(sessionId)) {
                            log.warn("WebSocket顶号: userId={} 被新登录踢下线", userId);
                            session.getBasicRemote().sendText(
                                "{\"type\":\"KICKED\",\"payload\":\"您的账号已在其他设备登录\"}");
                            session.close();
                            return;
                        }
                    }

                    sessionManager.registerSession(userId, session);
                    log.info("WebSocket注册成功: userId={}", userId);

                    // 检查是否有待恢复的重连上下文
                    Map.Entry<String, String> reconnectCtx = sessionManager.consumeReconnectContext(userId);
                    if (reconnectCtx != null && reconnectCtx.getKey() != null && !reconnectCtx.getKey().isEmpty()) {
                        String roomId = reconnectCtx.getKey();
                        // 自动重新订阅房间
                        sessionManager.joinRoom(userId, roomId);
                        // 通知客户端重连成功
                        session.getBasicRemote().sendText(
                            "{\"type\":\"RECONNECTED\",\"roomId\":\"" + roomId
                                + "\",\"battleId\":\"" + reconnectCtx.getValue() + "\"}");
                        log.info("WebSocket重连恢复: userId={}, roomId={}", userId, roomId);
                    }
                }
            } else if (message.contains("\"type\":\"PONG\"")) {
                // 客户端心跳响应，已在 touchSession 中更新
            } else if (message.contains("\"type\":\"SUBSCRIBE_ROOM\"")) {
                String userIdStr = extractValue(message, "userId");
                String roomId = extractStringValue(message, "roomId");
                if (userIdStr != null && roomId != null) {
                    Long userId = Long.parseLong(userIdStr);
                    sessionManager.joinRoom(userId, roomId);
                    log.info("用户订阅房间: userId={}, roomId={}", userId, roomId);
                }
            } else if (message.contains("\"type\":\"UNSUBSCRIBE_ROOM\"")) {
                String userIdStr = extractValue(message, "userId");
                if (userIdStr != null) {
                    Long userId = Long.parseLong(userIdStr);
                    sessionManager.leaveRoom(userId);
                    log.info("用户取消订阅房间: userId={}", userId);
                }
            }
        } catch (Exception e) {
            log.error("WebSocket消息处理失败: {}", e.getMessage());
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private String extractStringValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
