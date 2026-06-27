package com.example.feihualinggame.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * 按钮动效工具类
 * 为按钮添加丰富的点击动画效果
 */
public class ButtonAnimationHelper {
    
    /**
     * 为按钮添加按压缩放效果
     * 按下时缩小，松开时恢复并带有弹性效果
     */
    public static void addPressScaleEffect(View button) {
        if (button == null) return;
        
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 按下时缩小
                        animateScale(v, 0.95f, 100);
                        break;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // 松开时恢复，带有弹性效果
                        animateScaleWithBounce(v, 1.0f, 150);
                        break;
                }
                return false; // 不消耗事件，让onClick正常触发
            }
        });
    }
    
    /**
     * 为按钮添加涟漪扩散效果（配合背景使用）
     */
    public static void addRippleEffect(View button) {
        if (button == null) return;
        
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 创建轻微的亮度变化
                    ObjectAnimator alpha = ObjectAnimator.ofFloat(v, "alpha", 1.0f, 0.8f, 1.0f);
                    alpha.setDuration(200);
                    alpha.setInterpolator(new AccelerateDecelerateInterpolator());
                    alpha.start();
                }
                return false;
            }
        });
    }
    
    /**
     * 为按钮添加组合动效（缩放 + 透明度）
     */
    public static void addCombinedEffect(View button) {
        if (button == null) return;
        
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 按下：缩小 + 轻微透明
                        AnimatorSet pressSet = new AnimatorSet();
                        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(v, "scaleX", 0.95f);
                        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(v, "scaleY", 0.95f);
                        ObjectAnimator alphaDown = ObjectAnimator.ofFloat(v, "alpha", 0.85f);
                        pressSet.playTogether(scaleDownX, scaleDownY, alphaDown);
                        pressSet.setDuration(100);
                        pressSet.setInterpolator(new AccelerateDecelerateInterpolator());
                        pressSet.start();
                        break;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // 松开：恢复 + 弹性效果
                        AnimatorSet releaseSet = new AnimatorSet();
                        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(v, "scaleX", 1.0f);
                        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(v, "scaleY", 1.0f);
                        ObjectAnimator alphaUp = ObjectAnimator.ofFloat(v, "alpha", 1.0f);
                        releaseSet.playTogether(scaleUpX, scaleUpY, alphaUp);
                        releaseSet.setDuration(150);
                        releaseSet.setInterpolator(new OvershootInterpolator(1.5f));
                        releaseSet.start();
                        break;
                }
                return false;
            }
        });
    }
    
    /**
     * 为按钮添加入场动画（从下方滑入 + 淡入）
     */
    public static void addEntryAnimation(View button, int delay) {
        if (button == null) return;
        
        button.setTranslationY(50);
        button.setAlpha(0);
        
        AnimatorSet entrySet = new AnimatorSet();
        ObjectAnimator translateY = ObjectAnimator.ofFloat(button, "translationY", 50, 0);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(button, "alpha", 0, 1);
        
        entrySet.playTogether(translateY, fadeIn);
        entrySet.setDuration(400);
        entrySet.setStartDelay(delay);
        entrySet.setInterpolator(new OvershootInterpolator(1.2f));
        entrySet.start();
    }
    
    /**
     * 简单的缩放动画
     */
    private static void animateScale(View view, float targetScale, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", targetScale);
        
        AnimatorSet scaleSet = new AnimatorSet();
        scaleSet.playTogether(scaleX, scaleY);
        scaleSet.setDuration(duration);
        scaleSet.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleSet.start();
    }
    
    /**
     * 带弹性的缩放动画
     */
    private static void animateScaleWithBounce(View view, float targetScale, long duration) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", targetScale);
        
        AnimatorSet scaleSet = new AnimatorSet();
        scaleSet.playTogether(scaleX, scaleY);
        scaleSet.setDuration(duration);
        scaleSet.setInterpolator(new OvershootInterpolator(1.3f));
        scaleSet.start();
    }
}
