package com.example.feihualinggame.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * 全局波纹效果工具类
 * 为任意View添加点击波纹动画效果
 */
public class RippleEffectHelper {
    
    private static final int RIPPLE_COLOR = 0x336A7A8A; // 20%透明度的深灰色（符合古风主题）
    private static final int RIPPLE_DURATION = 500; // 波纹持续时间（毫秒）
    
    /**
     * 为ViewGroup添加全局波纹效果
     * 当点击空白区域时显示波纹
     * 
     * @param rootView 根布局（应该是FrameLayout）
     */
    public static void addGlobalRippleEffect(ViewGroup rootView) {
        if (rootView == null) {
            return;
        }
        
        // 获取ScrollView作为触摸目标
        final View scrollView = rootView.getChildAt(0);
        if (scrollView == null) {
            return;
        }
        
        // 使用dispatchTouchEvent来捕获所有触摸事件
        final ViewGroup finalRootView = rootView;
        final View finalScrollView = scrollView;
        
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float x = event.getX();
                    float y = event.getY();
                    
                    android.util.Log.d("RippleEffect", "Touch at: " + x + ", " + y);
                    
                    // 检查点击的是否是按钮或输入框
                    boolean isClickable = isClickableChild(finalScrollView, x, y);
                    android.util.Log.d("RippleEffect", "Is clickable child: " + isClickable);
                    
                    if (!isClickable) {
                        android.util.Log.d("RippleEffect", "Creating ripple...");
                        createRipple(finalRootView, x, y);
                    }
                }
                return false; // 不消耗事件，让子View正常处理
            }
        });
        
        // 确保rootLayout可以接收触摸事件
        rootView.setClickable(false);
        rootView.setFocusable(false);
    }
    
    /**
     * 检查点击位置是否是可点击的子View（按钮、输入框等）
     */
    private static boolean isClickableChild(View parent, float x, float y) {
        if (!(parent instanceof ViewGroup)) {
            return parent.isClickable();
        }
        
        ViewGroup viewGroup = (ViewGroup) parent;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            
            // 跳过不可见的View
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            
            // 获取子View在父布局中的位置
            int[] location = new int[2];
            child.getLocationOnScreen(location);
            
            // 将触摸坐标转换为屏幕坐标
            int[] parentLocation = new int[2];
            parent.getLocationOnScreen(parentLocation);
            
            float screenX = parentLocation[0] + x;
            float screenY = parentLocation[1] + y;
            
            // 检查点击位置是否在子View范围内
            if (screenX >= location[0] && screenX <= location[0] + child.getWidth() &&
                screenY >= location[1] && screenY <= location[1] + child.getHeight()) {
                
                // 如果是按钮、输入框或其他可点击控件，返回true
                if (child.isClickable() || child instanceof android.widget.Button ||
                    child instanceof android.widget.EditText || child instanceof android.widget.TextView) {
                    return true;
                }
                
                // 递归检查子View
                if (child instanceof ViewGroup) {
                    float childX = x - child.getLeft();
                    float childY = y - child.getTop();
                    if (isClickableChild(child, childX, childY)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 在指定位置创建波纹效果（公开方法，供Activity调用）
     */
    public static void createRippleAt(ViewGroup parent, float x, float y) {
        createRipple(parent, x, y);
    }
    
    /**
     * 检查点击位置是否是可点击的子View（公开方法，供Activity调用）
     */
    public static boolean isPointOnClickableChild(View parent, float x, float y) {
        return isClickableChild(parent, x, y);
    }
    
    /**
     * 在指定位置创建波纹效果
     */
    private static void createRipple(ViewGroup parent, float x, float y) {
        // 创建自定义波纹View
        RippleView rippleView = new RippleView(parent.getContext());
        rippleView.setPosition(x, y);
        
        // 设置LayoutParams，确保不影响其他布局
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        rippleView.setLayoutParams(params);
        
        // 添加到父布局
        parent.addView(rippleView);
        
        // 启动波纹动画
        rippleView.startAnimation();
    }
    
    /**
     * 自定义波纹View
     */
    private static class RippleView extends View {
        private float centerX, centerY;
        private float currentRadius = 0;
        private float maxRadius;
        private Paint paint;
        private ValueAnimator animator;
        
        public RippleView(android.content.Context context) {
            super(context);
            init();
        }
        
        private void init() {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            setWillNotDraw(false);
        }
        
        public void setPosition(float x, float y) {
            this.centerX = x;
            this.centerY = y;
            
            // 计算最大半径（覆盖整个屏幕对角线的一半）
            float width = getResources().getDisplayMetrics().widthPixels;
            float height = getResources().getDisplayMetrics().heightPixels;
            this.maxRadius = (float) Math.sqrt(width * width + height * height) / 2;
        }
        
        public void startAnimation() {
            animator = ValueAnimator.ofFloat(0, maxRadius);
            animator.setDuration(RIPPLE_DURATION);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    currentRadius = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // 动画结束后移除View
                    if (getParent() instanceof ViewGroup) {
                        ((ViewGroup) getParent()).removeView(RippleView.this);
                    }
                }
            });
            animator.start();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            if (currentRadius > 0) {
                // 计算透明度（随半径增大而减小）
                float progress = currentRadius / maxRadius;
                int alpha = (int) (255 * 0.3f * (1 - progress)); // 从30%透明度逐渐到0
                int color = (alpha << 24) | (RIPPLE_COLOR & 0x00FFFFFF);
                
                paint.setColor(color);
                canvas.drawCircle(centerX, centerY, currentRadius, paint);
            }
        }
    }
}
