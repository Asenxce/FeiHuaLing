package com.example.feihualinggame.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * 高级动效工具类
 * 提供批量入场动画、震动反馈、呼吸灯等高级效果
 */
public class AdvancedAnimationHelper {
    
    /**
     * 批量添加入场动画 -  staggered延迟效果
     * @param views 需要动画的View列表
     * @param animationType 动画类型: 0=从下向上, 1=从左向右, 2=淡入+缩放
     * @param baseDelay 基础延迟(ms)
     * @param staggerDelay 每个View之间的延迟间隔(ms)
     */
    public static void addStaggeredEntryAnimation(List<View> views, int animationType, 
                                                   long baseDelay, long staggerDelay) {
        if (views == null || views.isEmpty()) return;
        
        for (int i = 0; i < views.size(); i++) {
            View view = views.get(i);
            long delay = baseDelay + (i * staggerDelay);
            
            switch (animationType) {
                case 0: // 从下向上滑入
                    animateSlideUp(view, delay);
                    break;
                case 1: // 从左向右滑入
                    animateSlideRight(view, delay);
                    break;
                case 2: // 淡入+缩放
                    animateFadeScale(view, delay);
                    break;
            }
        }
    }
    
    /**
     * 为ViewGroup的所有子View添加批量入场动画
     */
    public static void addStaggeredEntryAnimation(ViewGroup parent, int animationType,
                                                   long baseDelay, long staggerDelay) {
        List<View> views = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            views.add(parent.getChildAt(i));
        }
        addStaggeredEntryAnimation(views, animationType, baseDelay, staggerDelay);
    }
    
    /**
     * 从下向上滑入动画
     */
    private static void animateSlideUp(View view, long delay) {
        view.setTranslationY(80);
        view.setAlpha(0);
        
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateY = ObjectAnimator.ofFloat(view, "translationY", 80, 0);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0, 1);
        
        animatorSet.playTogether(translateY, fadeIn);
        animatorSet.setDuration(500);
        animatorSet.setStartDelay(delay);
        animatorSet.setInterpolator(new OvershootInterpolator(1.1f));
        animatorSet.start();
    }
    
    /**
     * 从左向右滑入动画
     */
    private static void animateSlideRight(View view, long delay) {
        view.setTranslationX(-80);
        view.setAlpha(0);
        
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator translateX = ObjectAnimator.ofFloat(view, "translationX", -80, 0);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0, 1);
        
        animatorSet.playTogether(translateX, fadeIn);
        animatorSet.setDuration(500);
        animatorSet.setStartDelay(delay);
        animatorSet.setInterpolator(new OvershootInterpolator(1.1f));
        animatorSet.start();
    }
    
    /**
     * 淡入+缩放动画
     */
    private static void animateFadeScale(View view, long delay) {
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setAlpha(0);
        
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0, 1);
        
        animatorSet.playTogether(scaleX, scaleY, fadeIn);
        animatorSet.setDuration(450);
        animatorSet.setStartDelay(delay);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }
    
    /**
     * 震动反馈 - API 26+使用VibrationEffect
     * @param context 上下文
     * @param type 震动类型: 0=轻触(50ms), 1=中等(100ms), 2=强烈(200ms), 3=双击
     */
    public static void vibrate(Context context, int type) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        switch (type) {
            case 0: // 轻触
                triggerVibration(vibrator, 50);
                break;
            case 1: // 中等
                triggerVibration(vibrator, 100);
                break;
            case 2: // 强烈
                triggerVibration(vibrator, 200);
                break;
            case 3: // 双击
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[]{0, 50, 30, 50}, -1));
                }
                break;
        }
    }
    
    private static void triggerVibration(Vibrator vibrator, long milliseconds) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, 
                VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(milliseconds);
        }
    }
    
    /**
     * 呼吸灯效果 - 持续循环的透明度动画
     * @param view 目标View
     * @param minAlpha 最小透明度(0.0-1.0)
     * @param maxAlpha 最大透明度(0.0-1.0)
     * @param duration 单次呼吸周期(ms)
     */
    public static void startBreathingEffect(View view, float minAlpha, float maxAlpha, 
                                            long duration) {
        ObjectAnimator breathing = ObjectAnimator.ofFloat(view, "alpha", minAlpha, maxAlpha, minAlpha);
        breathing.setDuration(duration);
        breathing.setRepeatCount(ObjectAnimator.INFINITE);
        breathing.setRepeatMode(ObjectAnimator.RESTART);
        breathing.setInterpolator(new AccelerateDecelerateInterpolator());
        breathing.start();
    }
    
    /**
     * 停止呼吸灯效果
     */
    public static void stopBreathingEffect(View view) {
        view.animate().cancel();
        view.setAlpha(1.0f);
    }
    
    /**
     * 脉冲放大效果 - 一次性强调动画
     */
    public static void playPulseEffect(View view) {
        AnimatorSet pulseSet = new AnimatorSet();
        ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.15f);
        ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.15f);
        ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(view, "scaleX", 1.15f, 1.0f);
        ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(view, "scaleY", 1.15f, 1.0f);
        
        pulseSet.playSequentially(
            createAnimatorPair(scaleX1, scaleY1, 150),
            createAnimatorPair(scaleX2, scaleY2, 150)
        );
        pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseSet.start();
    }
    
    /**
     * 旋转入场动画
     */
    public static void animateRotateEntry(View view, long delay) {
        view.setRotation(-180);
        view.setScaleX(0);
        view.setScaleY(0);
        view.setAlpha(0);
        
        AnimatorSet rotateSet = new AnimatorSet();
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, "rotation", -180, 0);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0, 1);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0, 1);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0, 1);
        
        rotateSet.playTogether(rotation, scaleX, scaleY, fadeIn);
        rotateSet.setDuration(600);
        rotateSet.setStartDelay(delay);
        rotateSet.setInterpolator(new OvershootInterpolator(1.2f));
        rotateSet.start();
    }
    
    /**
     * 抖动效果 - 用于错误提示
     */
    public static void playShakeEffect(View view) {
        ObjectAnimator shake = ObjectAnimator.ofFloat(view, "translationX", 
            0, 10, -10, 8, -8, 6, -6, 4, -4, 2, -2, 0);
        shake.setDuration(500);
        shake.setInterpolator(new AccelerateDecelerateInterpolator());
        shake.start();
    }
    
    /**
     * 创建成对的Animator
     */
    private static AnimatorSet createAnimatorPair(ObjectAnimator anim1, ObjectAnimator anim2, 
                                                  long duration) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(anim1, anim2);
        set.setDuration(duration);
        return set;
    }
    
    /**
     * 弹性弹跳效果
     */
    public static void playBounceEffect(View view) {
        AnimatorSet bounceSet = new AnimatorSet();
        
        ObjectAnimator translateY1 = ObjectAnimator.ofFloat(view, "translationY", 0, -30);
        ObjectAnimator translateY2 = ObjectAnimator.ofFloat(view, "translationY", -30, 0);
        ObjectAnimator translateY3 = ObjectAnimator.ofFloat(view, "translationY", 0, -15);
        ObjectAnimator translateY4 = ObjectAnimator.ofFloat(view, "translationY", -15, 0);
        
        bounceSet.playSequentially(
            translateY1.setDuration(150),
            translateY2.setDuration(150),
            translateY3.setDuration(100),
            translateY4.setDuration(100)
        );
        bounceSet.setInterpolator(new AccelerateDecelerateInterpolator());
        bounceSet.start();
    }
}
