package com.example.feihualinggame.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

public class SwipeDrawerLayout extends DrawerLayout {
    private static final String TAG = "SwipeDrawerLayout";
    private static final float EDGE_WIDTH_RATIO = 0.2f;
    private int edgeSize;
    private int touchSlop;
    private float downX;
    private float downY;
    private boolean isEdgeTouched;
    private boolean isDragging;

    public SwipeDrawerLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SwipeDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipeDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = wm.getDefaultDisplay().getWidth();
        edgeSize = (int) (screenWidth * EDGE_WIDTH_RATIO);
        int systemTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        touchSlop = (int) (systemTouchSlop * 0.4f);
        Log.i(TAG, "初始化完成，热区大小: " + edgeSize + "px, 触摸容差: " + touchSlop + "px");
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isDrawerOpen(Gravity.START)) {
            return super.onInterceptTouchEvent(ev);
        }

        handleEdgeTouch(ev);
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isDrawerOpen(Gravity.START)) {
            return super.onTouchEvent(ev);
        }

        handleEdgeTouch(ev);
        return super.onTouchEvent(ev);
    }

    private void handleEdgeTouch(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                isEdgeTouched = downX <= edgeSize;
                isDragging = false;
                Log.d(TAG, "按下事件，X坐标: " + downX + ", 热区范围: " + edgeSize + ", 是否在热区内: " + isEdgeTouched);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isEdgeTouched && !isDrawerOpen(Gravity.START)) {
                    float dx = ev.getRawX() - downX;
                    float dy = Math.abs(ev.getRawY() - downY);
                    
                    if (dx > touchSlop && dx > dy) {
                        isDragging = true;
                        if (!isDrawerOpen(Gravity.START)) {
                            Log.i(TAG, "检测到边缘滑动，打开抽屉，滑动距离: " + dx);
                            openDrawer(Gravity.START);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isEdgeTouched = false;
                isDragging = false;
                break;
        }
    }
}
