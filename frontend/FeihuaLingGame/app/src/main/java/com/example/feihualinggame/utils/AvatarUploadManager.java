package com.example.feihualinggame.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.feihualinggame.bean.ApiResponse;
import com.example.feihualinggame.bean.UploadTokenRequest;
import com.example.feihualinggame.bean.UploadTokenResponse;
import com.example.feihualinggame.bean.UpdateAvatarRequest;
import com.example.feihualinggame.bean.User;
import com.example.feihualinggame.constant.ApiConstant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 头像上传管理器
 * 负责：图片压缩 → 获取OSS凭证 → 上传OSS → 更新数据库
 */
public class AvatarUploadManager {
    private static final String TAG = "AvatarUpload";
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;
    private static final int COMPRESS_QUALITY = 80;

    private static AvatarUploadManager instance;

    private AvatarUploadManager() {}

    public static AvatarUploadManager getInstance() {
        if (instance == null) {
            synchronized (AvatarUploadManager.class) {
                if (instance == null) {
                    instance = new AvatarUploadManager();
                }
            }
        }
        return instance;
    }

    /**
     * 头像上传回调
     */
    public interface UploadCallback {
        void onSuccess(User userInfo);
        void onError(String error);
        void onProgress(int progress);
    }

    /**
     * 完整上传流程
     * @param context 上下文
     * @param imageFile 图片文件
     * @param callback 回调
     */
    public void uploadAvatar(Context context, File imageFile, UploadCallback callback) {
        if (callback != null) {
            callback.onProgress(5);
        }

        // 1. 压缩图片
        byte[] compressedData = compressImage(imageFile);
        if (compressedData == null) {
            if (callback != null) {
                callback.onError("图片处理失败");
            }
            return;
        }

        if (callback != null) {
            callback.onProgress(10);
        }

        // 2. 获取MIME类型
        String mimeType = "image/jpeg";

        // 3. 获取OSS上传凭证
        getUploadToken(context, mimeType, new TokenCallback() {
            @Override
            public void onSuccess(UploadTokenResponse tokenResponse) {
                if (callback != null) {
                    callback.onProgress(30);
                }

                // 4. 上传到OSS
                uploadToOSS(tokenResponse.getUploadUrl(), compressedData, mimeType, new OssCallback() {
                    @Override
                    public void onSuccess() {
                        if (callback != null) {
                            callback.onProgress(80);
                        }

                        // 5. 更新数据库
                        updateAvatarUrl(context, tokenResponse.getAccessUrl(), new AvatarUpdateCallback() {
                            @Override
                            public void onSuccess(User userInfo) {
                                if (callback != null) {
                                    callback.onProgress(100);
                                    callback.onSuccess(userInfo);
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (callback != null) {
                                    callback.onError("更新头像失败: " + error);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (callback != null) {
                            callback.onError("上传到OSS失败: " + error);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError("获取上传凭证失败: " + error);
                }
            }
        });
    }

    /**
     * 压缩图片
     */
    private byte[] compressImage(File imageFile) {
        try {
            // 1. 解码图片获取尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            // 2. 计算缩放比例
            int inSampleSize = 1;
            if (options.outHeight > MAX_HEIGHT || options.outWidth > MAX_WIDTH) {
                int halfHeight = options.outHeight / 2;
                int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= MAX_HEIGHT &&
                        (halfWidth / inSampleSize) >= MAX_WIDTH) {
                    inSampleSize *= 2;
                }
            }

            // 3. 重新加载压缩后的图片
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            if (bitmap == null) {
                Log.e(TAG, "图片解码失败");
                return null;
            }

            // 4. 压缩为JPEG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outputStream);
            
            byte[] data = outputStream.toByteArray();
            
            // 5. 释放Bitmap
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            outputStream.close();

            Log.d(TAG, "图片压缩完成: 原始=" + imageFile.length() + " bytes, 压缩后=" + data.length + " bytes");
            return data;

        } catch (Exception e) {
            Log.e(TAG, "图片压缩失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取OSS上传凭证
     */
    private void getUploadToken(Context context, String mimeType, TokenCallback callback) {
        UploadTokenRequest request = new UploadTokenRequest(mimeType, "avatar");
        String json = GsonUtil.getGson().toJson(request);

        OkHttpUtil.postWithAuth(context, ApiConstant.OSS_UPLOAD_TOKEN, json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取上传凭证失败: " + e.getMessage());
                callback.onError("网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        ApiResponse<UploadTokenResponse> apiResponse = 
                            GsonUtil.getGson().fromJson(responseBody, 
                            new com.google.gson.reflect.TypeToken<ApiResponse<UploadTokenResponse>>(){}.getType());

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            Log.d(TAG, "获取上传凭证成功: " + apiResponse.getData().getAccessUrl());
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            callback.onError(apiResponse != null ? apiResponse.getMessage() : "未知错误");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析上传凭证失败: " + e.getMessage());
                        callback.onError("解析失败: " + e.getMessage());
                    }
                } else {
                    callback.onError("HTTP错误: " + response.code());
                }
            }
        });
    }

    /**
     * 上传到OSS
     */
    private void uploadToOSS(String uploadUrl, byte[] data, String mimeType, OssCallback callback) {
        new Thread(() -> {
            try {
                Log.d(TAG, "开始上传到OSS: " + uploadUrl);
                
                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    data, 
                    okhttp3.MediaType.parse(mimeType)
                );

                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(uploadUrl)
                    .put(body)
                    .addHeader("Content-Type", mimeType)
                    .build();

                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "上传到OSS成功");
                    callback.onSuccess();
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "无错误信息";
                    Log.e(TAG, "上传到OSS失败: HTTP " + response.code() + ", Body: " + errorBody);
                    callback.onError("HTTP " + response.code());
                }
            } catch (IOException e) {
                Log.e(TAG, "上传到OSS异常: " + e.getMessage());
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * 更新头像URL到数据库
     */
    private void updateAvatarUrl(Context context, String avatarUrl, AvatarUpdateCallback callback) {
        UpdateAvatarRequest request = new UpdateAvatarRequest(avatarUrl);
        String json = GsonUtil.getGson().toJson(request);

        Log.d(TAG, "更新头像URL: " + avatarUrl);

        OkHttpUtil.postWithAuth(context, ApiConstant.USER_AVATAR, json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "更新头像失败: " + e.getMessage());
                callback.onError("网络错误: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        ApiResponse<User> apiResponse = 
                            GsonUtil.getGson().fromJson(responseBody, 
                            new com.google.gson.reflect.TypeToken<ApiResponse<User>>(){}.getType());

                        if (apiResponse != null && apiResponse.isSuccess()) {
                            Log.d(TAG, "更新头像成功");
                            callback.onSuccess(apiResponse.getData());
                        } else {
                            callback.onError(apiResponse != null ? apiResponse.getMessage() : "更新失败");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析更新结果失败: " + e.getMessage());
                        callback.onError("解析失败: " + e.getMessage());
                    }
                } else {
                    callback.onError("HTTP错误: " + response.code());
                }
            }
        });
    }

    // ========== 回调接口 ==========

    private interface TokenCallback {
        void onSuccess(UploadTokenResponse response);
        void onError(String error);
    }

    private interface OssCallback {
        void onSuccess();
        void onError(String error);
    }

    private interface AvatarUpdateCallback {
        void onSuccess(User userInfo);
        void onError(String error);
    }
}
