package com.example.feihualinggame.utils;

import android.content.Context;
import android.util.Log;

import com.example.feihualinggame.constant.ApiConstant;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 心跳管理器：维持用户在线状态
 */
public class HeartbeatManager {
    private static final String TAG = "HeartbeatManager";
    private static volatile HeartbeatManager instance;

    /**
     * 心跳间隔（毫秒） - 20秒，确保在Redis TTL过期前刷新
     */
    private static final long HEARTBEAT_INTERVAL = 20 * 1000;

    private ScheduledExecutorService scheduler;
    private Context context;

    private HeartbeatManager() {}

    public static HeartbeatManager getInstance() {
        if (instance == null) {
            synchronized (HeartbeatManager.class) {
                if (instance == null) {
                    instance = new HeartbeatManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 启动心跳定时器（每20秒一次）
     */
    public void start() {
        if (scheduler != null && !scheduler.isShutdown()) {
            Log.d(TAG, "心跳定时器已在运行");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        // 立即执行一次，之后每隔指定间隔执行一次
        scheduler.scheduleAtFixedRate(() -> {
            sendHeartbeat();
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        
        Log.d(TAG, "心跳定时器已启动");
    }

    /**
     * 停止心跳定时器
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            scheduler = null;
            Log.d(TAG, "心跳定时器已停止");
        }
    }

    /**
     * 发送心跳请求
     */
    private void sendHeartbeat() {
        if (context == null) return;

        String token = SharedPrefsUtil.getString(context, "token");
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Token为空，停止心跳");
            stop();
            return;
        }

        OkHttpUtil.postWithAuth(context, ApiConstant.HEARTBEAT, "{}", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "心跳请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "心跳成功");
                } else {
                    Log.e(TAG, "心跳失败，HTTP码: " + response.code());
                    // 如果返回401，说明Token失效，清除本地登录状态
                    if (response.code() == 401) {
                        SharedPrefsUtil.clearUser(context);
                        stop();
                    }
                }
            }
        });
    }
}
