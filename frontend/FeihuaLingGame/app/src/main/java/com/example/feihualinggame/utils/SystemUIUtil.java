package com.example.feihualinggame.utils;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

/**
 * 系统UI工具类
 * 用于控制系统栏、导航栏等系统UI的显示效果
 */
public class SystemUIUtil {

    /**
     * 统一设置沉浸式状态栏
     * @param activity 目标Activity
     * @param statusBarColor 状态栏颜色资源ID
     * @param lightStatusBar true=浅色背景深色图标，false=深色背景浅色图标
     */
    public static void setupImmersiveStatusBar(Activity activity, int statusBarColor, boolean lightStatusBar) {
        if (activity == null) return;
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(activity, statusBarColor));
        if (lightStatusBar) {
            setLightStatusBar(activity);
        } else {
            setDarkStatusBar(activity);
        }
    }
    
    /**
     * 隐藏底部导航栏的指示条（小黑边）
     * 在Activity的onCreate中调用，必须在setContentView之前调用
     * 
     * @param activity 目标Activity
     */
    public static void hideNavigationBarIndicator(Activity activity) {
        if (activity == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsetsController
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                // 隐藏系统导航栏指示条
                controller.hide(android.view.WindowInsets.Type.navigationBars());
                // 设置为隐藏后不显示指示条
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 使用手势模式提示
            activity.getWindow().setNavigationBarContrastEnforced(false);
        }
        
        // 对于所有版本，设置导航栏透明
        activity.getWindow().setNavigationBarColor(
            activity.getResources().getColor(android.R.color.transparent)
        );
    }
    
    /**
     * 设置状态栏为浅色背景模式（状态栏图标和文字变为深色）
     * 适用于白色或浅色背景的页面
     * 
     * @param activity 目标Activity
     */
    public static void setLightStatusBar(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsetsController
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // 设置状态栏图标为深色
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 - 10 使用 SystemUiVisibility
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flags);
        }
    }
    
    /**
     * 设置状态栏为深色背景模式（状态栏图标和文字变为浅色/白色）
     * 适用于深色背景的页面
     * 
     * @param activity 目标Activity
     */
    public static void setDarkStatusBar(Activity activity) {
        if (activity == null) return;
        
        Window window = activity.getWindow();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsetsController
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                // 清除浅色状态栏标志，恢复为白色图标
                controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 - 10 使用 SystemUiVisibility
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decorView.setSystemUiVisibility(flags);
        }
    }
}
