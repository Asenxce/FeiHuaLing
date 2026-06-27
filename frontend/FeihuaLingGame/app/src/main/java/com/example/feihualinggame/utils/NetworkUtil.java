package com.example.feihualinggame.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.example.feihualinggame.constant.ApiConstant;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 网络可用性检测工具
 * 不仅检查网络连接状态，还通过实际访问后端API验证网络是否真正可用
 */
public class NetworkUtil {
    
    private static final int NETWORK_TIMEOUT = 5000; // 超时时间5秒
    
    /**
     * 检查网络是否真正可用（同步方法）
     * @param context 上下文
     * @return true表示网络可用，false表示不可用
     */
    public static boolean isNetworkReallyAvailable(Context context) {
        // 第一步：检查是否有网络连接
        if (!isNetworkConnected(context)) {
            return false;
        }
        
        // 第二步：尝试访问后端API验证网络真正可用
        return canReachBackend();
    }
    
    /**
     * 检查是否有网络连接（公开方法）
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        
        NetworkCapabilities capabilities = 
            connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }
    
    /**
     * 尝试访问后端API验证网络可达性
     * 使用同步方式等待结果
     */
    private static boolean canReachBackend() {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(NETWORK_TIMEOUT, TimeUnit.MILLISECONDS)
            .build();
        
        // 尝试访问一个轻量级的接口（如随机诗句接口）
        String testUrl = ApiConstant.BASE_URL + ApiConstant.POETRY_RANDOM;
        
        Request request = new Request.Builder()
            .url(testUrl)
            .get()
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 网络请求失败，说明网络不可用
                result[0] = false;
                latch.countDown();
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                // 只要能收到响应（即使是错误码），说明网络可达
                result[0] = true;
                response.close(); // 关闭响应释放资源
                latch.countDown();
            }
        });
        
        try {
            // 等待最多NETWORK_TIMEOUT毫秒
            latch.await(NETWORK_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        return result[0];
    }
}
