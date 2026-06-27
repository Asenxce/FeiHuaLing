package com.example.feihualinggame.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;

public class FeedbackManager {

    private static volatile FeedbackManager instance;
    private boolean vibrationEnabled = true;

    private FeedbackManager() {}

    public static FeedbackManager getInstance() {
        if (instance == null) {
            synchronized (FeedbackManager.class) {
                if (instance == null) {
                    instance = new FeedbackManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        vibrationEnabled = SharedPrefsUtil.getBoolean(context, "vibration_enabled", true);
    }

    public void setVibrationEnabled(Context context, boolean enabled) {
        this.vibrationEnabled = enabled;
        SharedPrefsUtil.saveBoolean(context, "vibration_enabled", enabled);
    }

    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    public void speakWelcome(Context context, String username) {
        vibrate(context, 0);
    }

    public void speakCorrect(Activity activity) {
        AudioController.getInstance().playSound(AudioController.SOUND_CORRECT);
        vibrate(activity, 1);
        View contentView = activity.findViewById(android.R.id.content);
        if (contentView != null) {
            View target = contentView.findViewWithTag("feedback_target");
            if (target != null) {
                AdvancedAnimationHelper.playPulseEffect(target);
            }
        }
    }

    public void speakWrong(Activity activity, String errorMessage) {
        AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
        vibrate(activity, 3);
        View contentView = activity.findViewById(android.R.id.content);
        if (contentView != null) {
            View target = contentView.findViewWithTag("feedback_target");
            if (target != null) {
                AdvancedAnimationHelper.playShakeEffect(target);
            }
        }
    }

    public void announceTurn(Activity activity, boolean isMyTurn) {
        if (isMyTurn) {
            vibrate(activity, 0);
        }
    }

    public void announceTimeout(Activity activity) {
        vibrate(activity, 2);
    }

    public void announceEliminated(Activity activity) {
        vibrate(activity, 2);
    }

    public void announceBattleEnd(Activity activity, boolean isWin) {
        vibrate(activity, isWin ? 1 : 2);
    }

    public void vibrate(Context context, int type) {
        if (!vibrationEnabled) return;
        AdvancedAnimationHelper.vibrate(context, type);
    }

    public void release() {
    }
}
