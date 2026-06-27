package com.example.feihualinggame.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;

import com.example.feihualinggame.R;

/**
 * 统一音频控制器
 * 管理所有背景音乐和音效的播放、音量控制及场景切换
 */
public class AudioController {
    private static AudioController instance;

    public static final int SCENE_MAIN = 0;
    public static final int SCENE_BATTLE = 1;

    public static final int SOUND_BUTTON = 0;
    public static final int SOUND_NAV = 1;
    public static final int SOUND_CORRECT = 2;
    public static final int SOUND_WRONG = 3;

    private MediaPlayer bgmPlayer;
    private int currentScene = SCENE_MAIN;
    private float bgmVolume = 0.8f;

    private SoundPool soundPool;
    private int[] soundIds = new int[4];
    private boolean[] soundLoaded = new boolean[4];
    private float sfxVolume = 0.8f;

    private Context appContext;
    private boolean isInitialized = false;

    private AudioController() {}

    public static synchronized AudioController getInstance() {
        if (instance == null) {
            instance = new AudioController();
        }
        return instance;
    }

    public void init(Context context) {
        if (isInitialized) return;

        appContext = context.getApplicationContext();

        initBGM(context);
        initSFX(context);
        loadUserSettings(context);

        isInitialized = true;
    }

    private void initBGM(Context context) {
        bgmPlayer = MediaPlayer.create(context, R.raw.bgm_main);
        if (bgmPlayer != null) {
            bgmPlayer.setLooping(true);
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
        }
    }

    private void initSFX(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(attributes)
                    .build();
        } else {
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            for (int i = 0; i < soundIds.length; i++) {
                if (soundIds[i] == sampleId) {
                    soundLoaded[i] = (status == 0);
                    break;
                }
            }
        });

        soundIds[SOUND_BUTTON] = soundPool.load(context, R.raw.click_button, 1);
        soundIds[SOUND_NAV] = soundPool.load(context, R.raw.click_nav, 1);
        soundIds[SOUND_CORRECT] = soundPool.load(context, R.raw.correct, 1);
        soundIds[SOUND_WRONG] = soundPool.load(context, R.raw.wrong, 1);
    }

    private void loadUserSettings(Context context) {
        int bgmVol = SharedPrefsUtil.getBgmVolume(context);
        int sfxVol = SharedPrefsUtil.getSoundVolume(context);

        bgmVolume = Math.max(0.0f, Math.min(1.0f, bgmVol / 100.0f));
        sfxVolume = Math.max(0.0f, Math.min(1.0f, sfxVol / 100.0f));

        if (bgmPlayer != null) {
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
        }
    }

    public int getCurrentScene() {
        return currentScene;
    }

    public void playBGM(int scene) {
        if (!isBgmEnabled() || bgmPlayer == null) return;

        if (currentScene == scene && bgmPlayer.isPlaying()) {
            return;
        }

        if (currentScene == scene && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
            return;
        }

        currentScene = scene;

        try {
            int resId = (scene == SCENE_BATTLE) ? R.raw.bgm_battle : R.raw.bgm_main;

            if (bgmPlayer != null) {
                bgmPlayer.stop();
                bgmPlayer.release();
            }

            bgmPlayer = MediaPlayer.create(appContext, resId);
            if (bgmPlayer != null) {
                bgmPlayer.setLooping(true);
                bgmPlayer.setVolume(bgmVolume, bgmVolume);
                bgmPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止背景音乐并重置播放位置（如加载页等静音场景）
     */
    public void stopBGM() {
        if (bgmPlayer != null) {
            if (bgmPlayer.isPlaying()) {
                bgmPlayer.pause();
            }
            bgmPlayer.seekTo(0);
        }
    }

    /**
     * 暂停背景音乐（保留播放位置，用于前后台切换）
     */
    public void pauseBGM() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    /**
     * 恢复背景音乐
     */
    public void resumeBGM() {
        if (bgmPlayer != null && isBgmEnabled() && !bgmPlayer.isPlaying()) {
            // seekTo(0) 被 stopBGM() 调用后需要重建
            try {
                bgmPlayer.start();
            } catch (IllegalStateException e) {
                playBGM(currentScene);
            }
        }
    }

    /**
     * 播放音效
     */
    public void playSound(int soundType) {
        if (!isSfxEnabled() || soundPool == null) return;

        if (soundType >= 0 && soundType < soundIds.length && soundIds[soundType] != 0) {
            soundPool.play(soundIds[soundType], sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    public void setBGMVolume(float volume) {
        bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (bgmPlayer != null) {
            bgmPlayer.setVolume(bgmVolume, bgmVolume);
        }
        // 静音联动：音量拖到0自动关闭音乐，>0自动开启
        if (bgmVolume == 0f) {
            SharedPrefsUtil.setBgmEnabled(appContext, false);
        }
        SharedPrefsUtil.saveBgmVolume(appContext, (int)(bgmVolume * 100));
    }

    public void setSFXVolume(float volume) {
        sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
        SharedPrefsUtil.saveSoundVolume(appContext, (int)(sfxVolume * 100));
    }

    public void setBGMEnabled(boolean enabled) {
        SharedPrefsUtil.setBgmEnabled(appContext, enabled);
        if (!enabled && bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        } else if (enabled && bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    public boolean isBgmEnabled() {
        return appContext != null && SharedPrefsUtil.isBgmEnabled(appContext);
    }

    public void setSFXEnabled(boolean enabled) {
        SharedPrefsUtil.setSfxEnabled(appContext, enabled);
    }

    public boolean isSfxEnabled() {
        return appContext != null && SharedPrefsUtil.isSfxEnabled(appContext);
    }

    public float getBGMVolume() {
        return bgmVolume;
    }

    public float getSFXVolume() {
        return sfxVolume;
    }

    public void release() {
        if (bgmPlayer != null) {
            bgmPlayer.release();
            bgmPlayer = null;
        }

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        isInitialized = false;
    }
}
