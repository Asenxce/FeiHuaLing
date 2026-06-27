package com.example.feihualinggame.utils;

import android.os.CountDownTimer;
import android.widget.TextView;

/**
 * 倒计时工具类
 * 用于游戏限时作答功能
 */
public class TimeCountUtil extends CountDownTimer {
    private TextView textView;  // 显示倒计时的 TextView
    private OnTimeUpListener listener;  // 时间到的回调接口
    private OnTickCallback tickCallback; // 记录剩余时间的回调
    private long currentMillis; // 当前剩余时间（毫秒）

    /**
     * 构造函数
     * @param millisInFuture 倒计时总时间（毫秒）
     * @param countDownInterval 倒计时间隔（毫秒）
     * @param textView 显示倒计时的 TextView
     */
    public TimeCountUtil(long millisInFuture, long countDownInterval, TextView textView) {
        super(millisInFuture, countDownInterval);
        this.textView = textView;
        this.currentMillis = millisInFuture;
    }

    /**
     * 设置时间到的回调监听
     */
    public void setOnTimeUpListener(OnTimeUpListener listener) {
        this.listener = listener;
    }
    
    /**
     * 设置每秒回调，用于保存剩余时间
     */
    public void setOnTickCallback(OnTickCallback callback) {
        this.tickCallback = callback;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        currentMillis = millisUntilFinished;
        // 每秒更新一次显示
        if (textView != null) {
            int seconds = (int) (millisUntilFinished / 1000);
            textView.setText(String.valueOf(seconds));
        }
        // 通知外部剩余时间
        if (tickCallback != null) {
            tickCallback.onTick(millisUntilFinished);
        }
    }

    @Override
    public void onFinish() {
        // 倒计时结束
        if (textView != null) {
            textView.setText("0");
        }
        // 触发回调
        if (listener != null) {
            listener.onTimeUp();
        }
    }

    /**
     * 格式化时间显示（分：秒）
     */
    public static String formatTime(long millis) {
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 获取当前剩余时间
     */
    public long getCurrentMillis() {
        return currentMillis;
    }

    /**
     * 时间到回调接口
     */
    public interface OnTimeUpListener {
        void onTimeUp();
    }
    
    /**
     * 倒计时每秒回调接口
     */
    public interface OnTickCallback {
        void onTick(long millis);
    }
}