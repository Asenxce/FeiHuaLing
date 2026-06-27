package com.example.feihualinggame.validator;

import android.util.Log;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 关键字表验证器（颜色飞花令、数字飞花令）
 */
public class KeywordValidator {
    private static final String TAG = "KeywordValidator";
    
    /**
     * 异步检查关键字是否存在于数据库关键字表中
     * @param context 上下文（用于获取Token）
     * @param keyword 关键字
     * @param type 类型（color/number）
     * @param listener 回调
     */
    public static void checkKeywordExists(android.content.Context context, String keyword, String type, OnKeywordCheckListener listener) {
        String url = ApiConstant.KEYWORD_CHECK + "?keyword=" + keyword + "&type=" + type;
        
        Log.d(TAG, "检查关键字: " + keyword + ", 类型: " + type);
        
        OkHttpUtil.getWithAuth(context, url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "检查关键字失败: " + e.getMessage());
                if (listener != null) {
                    listener.onCheckResult(false, "网络异常");
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        int code = jsonObject.has("code") ? jsonObject.get("code").getAsInt() : -1;
                        
                        boolean exists = false;
                        if (code == 200 && jsonObject.has("data")) {
                            JsonObject data = jsonObject.getAsJsonObject("data");
                            exists = data.has("exists") && data.get("exists").getAsBoolean();
                        }
                        
                        Log.d(TAG, "关键字 " + keyword + " 存在性: " + exists);
                        
                        if (listener != null) {
                            listener.onCheckResult(exists, exists ? "关键字有效" : "关键字不在题库中");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析响应失败", e);
                        if (listener != null) {
                            listener.onCheckResult(false, "数据解析失败");
                        }
                    }
                } else {
                    Log.e(TAG, "服务器响应错误: " + response.code());
                    if (listener != null) {
                        listener.onCheckResult(false, "服务器错误");
                    }
                }
            }
        });
    }
    
    public interface OnKeywordCheckListener {
        void onCheckResult(boolean exists, String message);
    }
}
