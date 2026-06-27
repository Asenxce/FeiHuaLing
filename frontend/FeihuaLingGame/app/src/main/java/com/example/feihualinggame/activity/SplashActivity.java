package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

/**
 * 欢迎页（Splash Screen）
 * 应用启动时显示，提升用户体验
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 欢迎页显示时长 2 秒

    private ImageView ivLogo;
    private TextView tvTitle;
    private TextView tvSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用主题模式
        applyThemeMode();
        
        super.onCreate(savedInstanceState);
        
        // 检查是否已登录，已登录直接跳转
        String username = SharedPrefsUtil.getUsername(this);
        String token = SharedPrefsUtil.getString(this, "token");
        
        if (username != null && !username.isEmpty() && token != null && !token.isEmpty()) {
            // 已登录，跳过欢迎页直接进入主页
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        
        // 未登录，显示欢迎页
        setContentView(R.layout.activity_splash);
        
        // 隐藏底部导航栏指示条
        SystemUIUtil.hideNavigationBarIndicator(this);
        // 设置状态栏为浅色背景
        SystemUIUtil.setLightStatusBar(this);

        // 初始化控件
        ivLogo = findViewById(R.id.iv_logo);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        // 添加入场动画
        startAnimations();
    }

    /**
     * 启动入场动画
     */
    private void startAnimations() {
        // Logo 淡入 + 缩放 + 旋转动画
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_logo_enter);
        logoAnimation.setFillAfter(true);
        ivLogo.startAnimation(logoAnimation);
        
        // Logo 入场后启动呼吸动画
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Animation breathingAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_breathing);
            breathingAnimation.setFillAfter(true);
            ivLogo.startAnimation(breathingAnimation);
        }, 1000);

        // 标题滑入动画（延迟 400ms）
        Animation titleAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_title_slide_up);
        titleAnimation.setFillAfter(true);
        titleAnimation.setStartOffset(400);
        tvTitle.startAnimation(titleAnimation);

        // 副标题滑入动画（延迟 700ms）
        Animation subtitleAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_title_slide_up);
        subtitleAnimation.setFillAfter(true);
        subtitleAnimation.setStartOffset(700);
        tvSubtitle.startAnimation(subtitleAnimation);
        
        // 延迟跳转到主页面
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            navigateToNextActivity();
        }, SPLASH_DURATION);
    }

    /**
     * 跳转到下一个页面
     */
    private void navigateToNextActivity() {
        // 未登录，跳转到 Home 页
        Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
        
        // 无切换动画，避免闪烁
        overridePendingTransition(0, 0);
    }

    /**
     * 应用主题模式
     */
    private void applyThemeMode() {
        String themeMode = SharedPrefsUtil.getString(this, "theme_mode");
        if (themeMode == null || themeMode.isEmpty()) {
            themeMode = "light";
        }
        
        switch (themeMode) {
            case "dark":
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "light":
            default:
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }
}
