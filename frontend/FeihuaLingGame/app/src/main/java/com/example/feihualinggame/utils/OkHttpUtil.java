package com.example.feihualinggame.utils;

import android.content.Context;
import android.content.Intent;

import com.example.feihualinggame.activity.LoginActivity;
import com.example.feihualinggame.constant.ApiConstant;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {
    // 使用ApiConstant中定义的基础URL（trim防护空格）
    private static final String BASE_URL = ApiConstant.BASE_URL.trim();

    private static String buildUrl(String path) {
        return BASE_URL + (path != null ? path.trim() : "");
    }
    
    // 优化OkHttpClient配置：连接池、超时时间
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES)) // 连接池：最多10个空闲连接，保持5分钟
            .connectTimeout(5, TimeUnit.SECONDS)  // 连接超时5秒
            .readTimeout(10, TimeUnit.SECONDS)    // 读取超时10秒
            .writeTimeout(10, TimeUnit.SECONDS)   // 写入超时10秒
            .retryOnConnectionFailure(true)       // 连接失败自动重试
            .build();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 检查响应码并处理401/403错误
     * @return true表示需要处理错误（已跳转登录），false表示继续处理
     */
    private static boolean handleAuthError(Context context, Response response) {
        int code = response.code();
        if (code == 401 || code == 403) {
            android.util.Log.w("OkHttpUtil", "收到" + code + "错误，Token失效，清除本地凭证");

            String message = "登录已失效，请重新登录";
            try {
                String body = response.peekBody(512).string();
                com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(body, com.google.gson.JsonObject.class);
                if (json.has("message")) {
                    message = json.get("message").getAsString();
                }
            } catch (Exception ignored) {}

            final String finalMessage = message;
            SharedPrefsUtil.clearUser(context);

            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(() -> {
                        android.widget.Toast.makeText(context, finalMessage, android.widget.Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(context, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                        activity.finish();
                    });
                }
            }
            return true;
        }
        return false;
    }

    // GET请求（基础飞花令查询用）
    public static void get(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(buildUrl(url))
                .build();
        client.newCall(request).enqueue(callback);
    }

    // POST请求（登录用）
    public static void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(buildUrl(url))
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    // POST请求（带Token认证）
    public static void postWithAuth(Context context, String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(JSON, json);
        String token = SharedPrefsUtil.getString(context, "token");

        Request.Builder requestBuilder = new Request.Builder()
                .url(buildUrl(url))
                .post(body);

        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new AuthCallbackWrapper(context, callback));
    }

    // GET请求（带Token认证）
    public static void getWithAuth(Context context, String url, Callback callback) {
        String token = SharedPrefsUtil.getString(context, "token");

        Request.Builder requestBuilder = new Request.Builder()
                .url(buildUrl(url));

        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new AuthCallbackWrapper(context, callback));
    }

    // DELETE请求（带Token认证，用于删除记录等）
    public static void deleteWithAuth(Context context, String url, RequestBody body, Callback callback) {
        String token = SharedPrefsUtil.getString(context, "token");

        Request.Builder requestBuilder = new Request.Builder()
                .url(buildUrl(url))
                .delete(body);

        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new AuthCallbackWrapper(context, callback));
    }

    /**
     * 认证回调包装器：拦截401/403响应，自动处理Token失效
     */
    private static class AuthCallbackWrapper implements Callback {
        private final Context context;
        private final Callback delegate;

        AuthCallbackWrapper(Context context, Callback delegate) {
            this.context = context;
            this.delegate = delegate;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            delegate.onFailure(call, e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (handleAuthError(context, response)) {
                response.close();
                return;
            }
            delegate.onResponse(call, response);
        }
    }

    // PUT请求（用于OSS上传）
    public static void put(String url, byte[] data, String mimeType, Callback callback) {
        RequestBody body = RequestBody.create(data, MediaType.parse(mimeType));
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        client.newCall(request).enqueue(callback);
    }
}