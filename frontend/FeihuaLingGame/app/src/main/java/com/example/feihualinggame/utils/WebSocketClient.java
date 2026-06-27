package com.example.feihualinggame.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.feihualinggame.bean.WebSocketMessageBean;
import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import okhttp3.*;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";
    private static final Gson gson = new Gson();
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int MAX_RECONNECT_ATTEMPTS_BATTLE = 20;
    private static final long RECONNECT_BASE_DELAY_MS = 2000;
    private static final long BATTLE_RECONNECT_DELAYS_MS[] = {300, 500, 800, 1200, 2000, 3000, 4000, 5000};
    private static WebSocketClient instance;

    private WebSocket webSocket;
    private String serverUrl;
    private volatile boolean connected;
    private volatile boolean connecting = false;
    private boolean intentionalClose = false;
    private String sessionId;
    private Long registeredUserId;
    private int reconnectAttempt = 0;
    private int generation = 0;
    private boolean battleMode = false;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());

    public interface MessageCallback {
        void onConnected();
        void onDisconnected();
        void onMessage(WebSocketMessageBean message);
        void onError(String error);
        default void onReconnectAttempt(int attempt, int maxAttempts) {}
    }

    private final List<MessageCallback> callbacks = new CopyOnWriteArrayList<>();

    public static synchronized WebSocketClient getInstance() {
        if (instance == null) {
            instance = new WebSocketClient();
        }
        return instance;
    }

    public void addCallback(MessageCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public void removeCallback(MessageCallback callback) {
        callbacks.remove(callback);
    }

    public synchronized void connect(String url) {
        if (connected || connecting) {
            Log.d(TAG, "已连接或正在连接，跳过: connected=" + connected + " connecting=" + connecting);
            return;
        }
        if (webSocket != null) {
            generation++;
            Log.d(TAG, "关闭旧连接 gen=" + generation);
            try {
                webSocket.close(1000, "重连");
            } catch (Exception e) {
                Log.w(TAG, "关闭旧连接异常: " + e.getMessage());
            }
            webSocket = null;
        }
        this.serverUrl = url;
        this.connected = false;
        this.connecting = true;
        this.intentionalClose = false;

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        final int myGen = generation;
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                if (generation != myGen) {
                    Log.d(TAG, "onOpen: 旧连接事件 gen=" + myGen + " 当前gen=" + generation);
                    return;
                }
                Log.i(TAG, "WebSocket连接成功: " + url);
                connected = true;
                connecting = false;
                reconnectAttempt = 0;
                for (MessageCallback cb : callbacks) {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onConnected());
                }
                if (registeredUserId != null) {
                    sendRegisterMessage(registeredUserId);
                }
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                if (generation != myGen) return;
                Log.d(TAG, "收到消息: " + text);
                try {
                    WebSocketMessageBean msg = gson.fromJson(text, WebSocketMessageBean.class);
                    for (MessageCallback cb : callbacks) {
                        new Handler(Looper.getMainLooper()).post(() -> cb.onMessage(msg));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "消息解析失败: " + e.getMessage());
                }
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                if (generation != myGen) return;
                Log.i(TAG, "WebSocket关闭中: code=" + code + ", reason=" + reason);
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                if (generation != myGen) {
                    Log.d(TAG, "onClosed: 旧连接事件 gen=" + myGen + " 当前gen=" + generation);
                    return;
                }
                Log.i(TAG, "WebSocket已关闭: code=" + code + ", reason=" + reason);
                connected = false;
                connecting = false;
                for (MessageCallback cb : callbacks) {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onDisconnected());
                }
                if (!intentionalClose) {
                    attemptReconnect();
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                if (generation != myGen) {
                    Log.d(TAG, "onFailure: 旧连接事件 gen=" + myGen + " 当前gen=" + generation);
                    return;
                }
                Log.e(TAG, "WebSocket连接失败: " + t.getMessage());
                connected = false;
                connecting = false;
                for (MessageCallback cb : callbacks) {
                    new Handler(Looper.getMainLooper()).post(() -> cb.onError(t.getMessage()));
                }
                if (!intentionalClose) {
                    attemptReconnect();
                }
            }
        });
    }

    public void setBattleMode(boolean battleMode) {
        this.battleMode = battleMode;
    }

    public void forceReconnect() {
        reconnectHandler.removeCallbacksAndMessages(null);
        reconnectAttempt = 0;
        intentionalClose = false;
        connecting = false;
        if (serverUrl != null && !connected) {
            connect(serverUrl);
        }
    }

    public void cancelReconnect() {
        reconnectHandler.removeCallbacksAndMessages(null);
        intentionalClose = true;
    }

    public int getReconnectAttempt() {
        return reconnectAttempt;
    }

    private void attemptReconnect() {
        int maxAttempts = battleMode ? MAX_RECONNECT_ATTEMPTS_BATTLE : MAX_RECONNECT_ATTEMPTS;
        if (reconnectAttempt >= maxAttempts) {
            Log.w(TAG, "重连次数已达上限(" + maxAttempts + ")，放弃重连");
            return;
        }
        long delay;
        if (battleMode && reconnectAttempt < BATTLE_RECONNECT_DELAYS_MS.length) {
            delay = BATTLE_RECONNECT_DELAYS_MS[reconnectAttempt];
        } else if (battleMode) {
            delay = 5000;
        } else {
            delay = Math.min(RECONNECT_BASE_DELAY_MS * (1L << Math.min(reconnectAttempt, 4)), 30000);
        }
        reconnectAttempt++;
        Log.i(TAG, "尝试第" + reconnectAttempt + "/" + maxAttempts + "次重连，延迟" + delay + "ms");
        for (MessageCallback cb : callbacks) {
            new Handler(Looper.getMainLooper()).post(() -> cb.onReconnectAttempt(reconnectAttempt, maxAttempts));
        }
        reconnectHandler.postDelayed(() -> {
            if (!connected && !intentionalClose && !connecting) {
                connect(serverUrl);
            }
        }, delay);
    }

    public void register(Long userId) {
        this.registeredUserId = userId;
        if (webSocket != null && connected) {
            sendRegisterMessage(userId);
        }
    }

    private void sendRegisterMessage(Long userId) {
        String msg;
        if (sessionId != null && !sessionId.isEmpty()) {
            msg = "{\"type\":\"REGISTER\",\"userId\":" + userId + ",\"sessionId\":\"" + sessionId + "\"}";
        } else {
            msg = "{\"type\":\"REGISTER\",\"userId\":" + userId + "}";
        }
        webSocket.send(msg);
        Log.d(TAG, "发送注册消息: userId=" + userId);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void sendMessage(String message) {
        if (webSocket != null && connected) {
            webSocket.send(message);
            Log.d(TAG, "发送消息: " + message);
        } else {
            Log.w(TAG, "WebSocket未连接，无法发送消息");
        }
    }

    public void disconnect() {
        intentionalClose = true;
        reconnectHandler.removeCallbacksAndMessages(null);
        if (webSocket != null) {
            webSocket.close(1000, "客户端主动断开");
            connected = false;
            connecting = false;
        }
        registeredUserId = null;
    }

    public void disconnectWithoutIntentionalFlag() {
        reconnectHandler.removeCallbacksAndMessages(null);
        connecting = false;
        if (webSocket != null) {
            try {
                webSocket.close(1000, "重连中断");
            } catch (Exception e) {
                Log.w(TAG, "关闭连接异常: " + e.getMessage());
            }
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected && webSocket != null;
    }
}
