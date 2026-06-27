package com.example.feihualinggame;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.HeartbeatManager;
import com.example.feihualinggame.utils.SharedPrefsUtil;

/**
 * 应用全局生命周期管理类
 * 监听App前后台切换，控制背景音乐
 */
public class FeihuaLingGameApplication extends Application {
    
    private static FeihuaLingGameApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // 初始化主题模式
        initThemeMode();
        
        // 注册应用生命周期监听
        registerAppLifecycleObserver();
    }
    
    /**
     * 注册应用生命周期监听器
     */
    private void registerAppLifecycleObserver() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onPause(LifecycleOwner owner) {
                // 应用进入后台（切换app或熄屏）- 使用onPause比onStop更快响应
                android.util.Log.d("AppLifecycle", "应用进入后台，暂停背景音乐和心跳");
                AudioController.getInstance().pauseBGM();
                HeartbeatManager.getInstance().stop();
            }
            
            @Override
            public void onResume(LifecycleOwner owner) {
                // 应用回到前台
                android.util.Log.d("AppLifecycle", "应用回到前台，恢复背景音乐和心跳");
                AudioController.getInstance().resumeBGM();
                
                // 检查是否已登录，如果已登录则重新启动心跳
                String token = SharedPrefsUtil.getString(getApplicationContext(), "token");
                if (token != null && !token.isEmpty()) {
                    HeartbeatManager.getInstance().init(getApplicationContext());
                    HeartbeatManager.getInstance().start();
                }
            }
        });
    }
    
    /**
     * 初始化主题模式
     */
    private void initThemeMode() {
        String themeMode = SharedPrefsUtil.getString(this, "theme_mode");
        if (themeMode == null || themeMode.isEmpty()) {
            themeMode = "light";
            SharedPrefsUtil.saveString(this, "theme_mode", "light");
        }
        
        switch (themeMode) {
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "light":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }
    
    public static FeihuaLingGameApplication getInstance() {
        return instance;
    }
}
