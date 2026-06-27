package com.example.feihualinggame.validator;

import android.util.Log;

import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 首尾接龙飞花令验证器
 * 规则：玩家答案的首字必须是上一句的尾字
 */
public class ChainFeiHuaLing implements FeiHuaLingValidator {
    private static final String TAG = "ChainFeiHuaLing";

    private Character lastChar;
    private final Set<String> usedPoems;
    private final OnInitialPoemListener listener;

    public interface OnInitialPoemListener {
        void onInitialPoemReceived(String poem);
        void onError(String error);
    }

    public ChainFeiHuaLing(OnInitialPoemListener listener) {
        this.lastChar = null;
        this.usedPoems = new HashSet<>();
        this.listener = listener;
    }

    public void startGame() {
        OkHttpUtil.get(ApiConstant.POETRY_RANDOM, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (listener != null) {
                    listener.onError("网络异常，无法获取起始诗句");
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Gson gson = new Gson();
                        JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

                        if (jsonObject.has("data")) {
                            JsonObject data = jsonObject.getAsJsonObject("data");
                            String content = data.get("content").getAsString();
                            String cleanedContent = PoetryCleanUtil.cleanPoetry(content);

                            if (!cleanedContent.isEmpty()) {
                                lastChar = cleanedContent.charAt(cleanedContent.length() - 1);
                                if (listener != null) {
                                    listener.onInitialPoemReceived(cleanedContent);
                                }
                            } else {
                                if (listener != null) {
                                    listener.onError("获取的诗句格式异常");
                                }
                            }
                        } else {
                            if (listener != null) {
                                listener.onError("响应数据格式错误");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析起始诗句失败", e);
                        if (listener != null) {
                            listener.onError("解析数据失败");
                        }
                    }
                } else {
                    if (listener != null) {
                        listener.onError("获取起始诗句失败");
                    }
                }
            }
        });
    }

    public void setStartChar(char startChar) {
        this.lastChar = startChar;
    }

    @Override
    public ChainValidationResult validate(String answer) {
        String cleanedAnswer = PoetryCleanUtil.cleanPoetry(answer);

        if (cleanedAnswer.isEmpty()) {
            return ChainValidationResult.error("答案不能为空");
        }

        if (cleanedAnswer.length() < 2) {
            return ChainValidationResult.error("请输入完整的诗句（至少2个字）");
        }

        if (lastChar == null) {
            return ChainValidationResult.error("游戏尚未开始，请稍后再试");
        }

        char firstChar = cleanedAnswer.charAt(0);
        if (firstChar != lastChar) {
            return ChainValidationResult.error("首字必须是「" + lastChar + "」（当前是「" + firstChar + "」）");
        }

        if (usedPoems.contains(cleanedAnswer)) {
            return ChainValidationResult.error("这句诗已经使用过了，请换一句");
        }

        lastChar = cleanedAnswer.charAt(cleanedAnswer.length() - 1);
        usedPoems.add(cleanedAnswer);

        return ChainValidationResult.success(cleanedAnswer, lastChar);
    }

    public Character getLastChar() {
        return lastChar;
    }

    @Override
    public int getUsedCount() {
        return usedPoems.size();
    }

    @Override
    public void clear() {
        usedPoems.clear();
        lastChar = null;
    }
}
