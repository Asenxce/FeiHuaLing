package com.example.feihualinggame.utils;

import android.content.Context;
import android.util.Log;

import com.example.feihualinggame.bean.Poetry;
import com.example.feihualinggame.constant.ApiConstant;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 诗词收藏管理器
 * 采用本地缓存 + 后端同步的混合策略
 */
public class PoetryCollectionManager {
    private static final String TAG = "PoetryCollection";
    private static final String KEY_COLLECTION_PREFIX = "collection_list_";

    /**
     * 获取当前用户的收藏key
     */
    private static String getUserCollectionKey(Context context) {
        String userId = SharedPrefsUtil.getUserId(context);
        if (userId == null || userId.isEmpty()) {
            return KEY_COLLECTION_PREFIX + "default";
        }
        return KEY_COLLECTION_PREFIX + userId;
    }

    /**
     * 获取收藏列表（优先从后端同步，失败则读本地）
     */
    public static void loadCollectionFromServer(Context context, OnCollectionLoadListener listener) {
        String userId = SharedPrefsUtil.getUserId(context);
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "用户未登录，读取本地缓存");
            if (listener != null) {
                listener.onSuccess(getLocalCollection(context));
            }
            return;
        }

        OkHttpUtil.getWithAuth(context, ApiConstant.COLLECTION_LIST, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "从服务器加载收藏失败: " + e.getMessage());
                // 降级：读取本地缓存
                if (listener != null) {
                    listener.onSuccess(getLocalCollection(context));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        JsonObject jsonObject = new Gson().fromJson(body, JsonObject.class);
                        
                        if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                            JsonArray dataArray = jsonObject.getAsJsonArray("data");
                            List<Poetry> collection = new ArrayList<>();
                            
                            for (int i = 0; i < dataArray.size(); i++) {
                                JsonObject poetryObj = dataArray.get(i).getAsJsonObject();
                                Poetry poetry = new Poetry();
                                poetry.setId(poetryObj.has("id") ? poetryObj.get("id").getAsLong() : 0);
                                poetry.setTitle(poetryObj.has("title") ? poetryObj.get("title").getAsString() : "无题");
                                poetry.setAuthor(poetryObj.has("author") ? poetryObj.get("author").getAsString() : "未知");
                                poetry.setDynasty(poetryObj.has("dynasty") ? poetryObj.get("dynasty").getAsString() : "");
                                poetry.setFullContent(poetryObj.has("fullContent") ? poetryObj.get("fullContent").getAsString() : "");
                                poetry.setContent(poetryObj.has("content") ? poetryObj.get("content").getAsString() : "");
                                collection.add(poetry);
                            }
                            
                            // 同步到本地缓存
                            saveLocalCollection(context, collection);
                            Log.d(TAG, "从服务器加载收藏成功: " + collection.size() + " 首");
                            
                            if (listener != null) {
                                listener.onSuccess(collection);
                            }
                        } else {
                            Log.w(TAG, "响应数据格式异常");
                            if (listener != null) {
                                listener.onSuccess(getLocalCollection(context));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析收藏数据失败", e);
                        if (listener != null) {
                            listener.onSuccess(getLocalCollection(context));
                        }
                    }
                } else {
                    Log.e(TAG, "服务器响应错误: " + response.code());
                    if (listener != null) {
                        listener.onSuccess(getLocalCollection(context));
                    }
                }
            }
        });
    }

    public static void addToCollectionAsync(Context context, Poetry poetry, OnCollectionChangeListener listener) {
        Log.d(TAG, "开始添加收藏 - ID: " + poetry.getId() + ", 标题: " + poetry.getTitle() + ", 作者: " + poetry.getAuthor());
        
        boolean localSuccess = addToCollection(context, poetry);
        if (!localSuccess) {
            Log.d(TAG, "本地检查发现已收藏");
            if (listener != null) {
                listener.onResult(false, "已在收藏中");
            }
            return;
        }

        String userId = SharedPrefsUtil.getUserId(context);
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "用户未登录，仅保存到本地");
            if (listener != null) {
                listener.onResult(true, "已保存到本地");
            }
            return;
        }

        JsonObject params = new JsonObject();
        params.addProperty("poetryId", poetry.getId());
        params.addProperty("title", poetry.getTitle());
        params.addProperty("author", poetry.getAuthor());
        params.addProperty("dynasty", poetry.getDynasty());
        params.addProperty("fullContent", poetry.getFullContent());
        params.addProperty("content", poetry.getContent());

        Log.d(TAG, "发送收藏请求到服务器: " + params.toString());
        
        OkHttpUtil.postWithAuth(context, ApiConstant.COLLECTION_ADD, params.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "同步收藏到服务器失败: " + e.getMessage());
                if (listener != null) {
                    listener.onResult(true, "已保存到本地，网络同步失败");
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "服务器响应: " + response.code() + " " + responseBody);
                
                if (response.isSuccessful()) {
                    if (listener != null) {
                        listener.onResult(true, "收藏成功");
                    }
                } else {
                    try {
                        com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(responseBody, com.google.gson.JsonObject.class);
                        String message = json.has("message") ? json.get("message").getAsString() : "服务器错误";
                        Log.w(TAG, "服务器拒绝收藏: " + message);
                        if (listener != null) {
                            listener.onResult(false, message);
                        }
                    } catch (Exception e) {
                        if (listener != null) {
                            listener.onResult(false, "服务器错误");
                        }
                    }
                }
            }
        });
    }

    /**
     * 取消收藏（同时从本地和后端删除）
     */
    public static void removeFromCollectionAsync(Context context, long poetryId, OnCollectionChangeListener listener) {
        // 1. 先从本地删除（立即生效）
        boolean localRemoved = removeFromCollection(context, poetryId);

        // 2. 异步从后端删除
        String userId = SharedPrefsUtil.getUserId(context);
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "用户未登录，仅从本地删除");
            if (listener != null) {
                listener.onResult(localRemoved, localRemoved ? "已取消收藏" : "未在收藏中");
            }
            return;
        }

        String url = ApiConstant.COLLECTION_REMOVE + "?poetryId=" + poetryId;
        OkHttpUtil.postWithAuth(context, url, "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "同步取消收藏到服务器失败: " + e.getMessage());
                if (listener != null) {
                    listener.onResult(localRemoved, "已从本地删除，网络同步失败");
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "取消收藏同步到服务器成功");
                    if (listener != null) {
                        listener.onResult(true, "已取消收藏");
                    }
                } else {
                    Log.e(TAG, "服务器响应错误: " + response.code());
                    if (listener != null) {
                        listener.onResult(localRemoved, "已从本地删除，服务器同步失败");
                    }
                }
            }
        });
    }

    /**
     * 检查是否已收藏（查本地缓存，优先ID匹配，其次标题+作者匹配）
     */
    public static boolean isCollected(Context context, long poetryId) {
        List<Poetry> collection = getLocalCollection(context);
        for (Poetry p : collection) {
            if (p.getId() == poetryId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否已收藏（按诗词内容匹配，用于ID不稳定的场景如每日好签）
     */
    public static boolean isCollectedByContent(Context context, Poetry poetry) {
        if (poetry == null) return false;
        List<Poetry> collection = getLocalCollection(context);
        for (Poetry p : collection) {
            // 先按ID匹配
            if (p.getId() == poetry.getId() && poetry.getId() > 0) {
                return true;
            }
            // 再按标题+作者匹配（处理ID不稳定情况）
            String title = p.getTitle();
            String author = p.getAuthor();
            if (title != null && author != null && !title.isEmpty() && !author.isEmpty()) {
                if (title.equals(poetry.getTitle()) && author.equals(poetry.getAuthor())) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 本地缓存操作（私有方法） ====================

    /**
     * 获取本地收藏列表
     */
    private static List<Poetry> getLocalCollection(Context context) {
        String key = getUserCollectionKey(context);
        String json = SharedPrefsUtil.getString(context, key);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Gson gson = new Gson();
            return gson.fromJson(json, new TypeToken<List<Poetry>>(){}.getType());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 保存本地收藏列表
     */
    private static void saveLocalCollection(Context context, List<Poetry> collection) {
        String key = getUserCollectionKey(context);
        Gson gson = new Gson();
        String json = gson.toJson(collection);
        SharedPrefsUtil.saveString(context, key, json);
    }

    /**
     * 添加收藏（仅本地）
     */
    private static boolean addToCollection(Context context, Poetry poetry) {
        List<Poetry> collection = getLocalCollection(context);
        
        for (Poetry p : collection) {
            if (p.getId() == poetry.getId() && poetry.getId() > 0) {
                return false;
            }
            String title = p.getTitle();
            String author = p.getAuthor();
            if (title != null && author != null && !title.isEmpty() && !author.isEmpty()) {
                if (title.equals(poetry.getTitle()) && author.equals(poetry.getAuthor())) {
                    return false;
                }
            }
        }
        
        collection.add(poetry);
        saveLocalCollection(context, collection);
        return true;
    }

    /**
     * 取消收藏（仅本地）
     */
    private static boolean removeFromCollection(Context context, long poetryId) {
        List<Poetry> collection = getLocalCollection(context);
        boolean removed = collection.removeIf(p -> p.getId() == poetryId);
        if (removed) {
            saveLocalCollection(context, collection);
        }
        return removed;
    }

    /**
     * 清空收藏（仅本地）
     */
    public static void clearCollection(Context context) {
        String key = getUserCollectionKey(context);
        SharedPrefsUtil.saveString(context, key, "");
    }

    // ==================== 回调接口 ====================

    public interface OnCollectionLoadListener {
        void onSuccess(List<Poetry> collection);
    }

    public interface OnCollectionChangeListener {
        void onResult(boolean success, String message);
    }
}
