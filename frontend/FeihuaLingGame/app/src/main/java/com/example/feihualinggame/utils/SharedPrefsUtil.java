package com.example.feihualinggame.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences 工具类
 * 用于本地数据存储
 */
public class SharedPrefsUtil {
    private static final String SP_NAME = "feihualing_sp";
    
    // 用户相关 key
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_TOTAL_SCORE = "total_score";
    private static final String KEY_TOTAL_GAMES = "total_games";
    private static final String KEY_WIN_COUNT = "win_count";
    private static final String KEY_LOSE_COUNT = "lose_count";
    
    // 游戏相关 key
    private static final String KEY_LAST_GAME_MODE = "last_game_mode";
    private static final String KEY_LAST_KEYWORD = "last_keyword";
    
    // 临时对战缓存 key（对战结束后清除）
    private static final String KEY_TEMP_KEYWORD = "temp_keyword";
    private static final String KEY_TEMP_KEYWORD2 = "temp_keyword2";
    private static final String KEY_TEMP_GAME_MODE = "temp_game_mode";
    private static final String KEY_TEMP_BATTLE_TYPE = "temp_battle_type";
    private static final String KEY_TEMP_POSITION = "temp_position";
    
    // 设置相关 key
    private static final String KEY_TIME_LIMIT = "time_limit";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_BGM_ENABLED = "bgm_enabled";
    private static final String KEY_SFX_ENABLED = "sfx_enabled";
    private static final String KEY_SOUND_VOLUME = "sound_volume";
    private static final String KEY_BGM_VOLUME = "bgm_volume";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    
    /**
     * 保存登录用户名
     */
    public static void saveUsername(Context context, String username) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_USERNAME, username).apply();
    }
    
    /**
     * 获取登录用户名（判断是否登录）
     */
    public static String getUsername(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USERNAME, null);
    }
    
    /**
     * 保存用户 ID
     */
    public static void saveUserId(Context context, String userId) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_USER_ID, userId).apply();
    }
    
    /**
     * 获取用户 ID
     */
    public static String getUserId(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_USER_ID, "");
    }

    /**
     * 保存 long 类型值
     */
    public static void saveLong(Context context, String key, long value) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putLong(key, value).apply();
    }

    /**
     * 获取 long 类型值
     */
    public static long getLong(Context context, String key, long defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getLong(key, defaultValue);
    }
    
    /**
     * 保存总积分
     */
    public static void saveTotalScore(Context context, int score) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_TOTAL_SCORE, score).apply();
    }
    
    /**
     * 获取总积分
     */
    public static int getTotalScore(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_TOTAL_SCORE, 0);
    }

    /**
     * 保存总游戏场次
     */
    public static void saveTotalGames(Context context, int count) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_TOTAL_GAMES, count).apply();
    }

    /**
     * 获取总游戏场次
     */
    public static int getTotalGames(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_TOTAL_GAMES, 0);
    }
    
    /**
     * 保存胜利次数
     */
    public static void saveWinCount(Context context, int count) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_WIN_COUNT, count).apply();
    }
    
    /**
     * 获取胜利次数
     */
    public static int getWinCount(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_WIN_COUNT, 0);
    }
    
    /**
     * 保存失败次数
     */
    public static void saveLoseCount(Context context, int count) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_LOSE_COUNT, count).apply();
    }
    
    /**
     * 获取失败次数
     */
    public static int getLoseCount(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_LOSE_COUNT, 0);
    }
    
    /**
     * 保存上次游戏模式
     */
    public static void saveLastGameMode(Context context, String gameMode) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_LAST_GAME_MODE, gameMode).apply();
    }
    
    /**
     * 获取上次游戏模式
     */
    public static String getLastGameMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_LAST_GAME_MODE, null);
    }
    
    /**
     * 保存上次关键字
     */
    public static void saveLastKeyword(Context context, String keyword) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_LAST_KEYWORD, keyword).apply();
    }
    
    /**
     * 获取上次关键字
     */
    public static String getLastKeyword(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_LAST_KEYWORD, null);
    }
    
    /**
     * 保存限时时间设置
     */
    public static void saveTimeLimit(Context context, int seconds) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_TIME_LIMIT, seconds).apply();
    }
    
    /**
     * 获取限时时间设置
     */
    public static int getTimeLimit(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_TIME_LIMIT, 30); // 默认 30 秒
    }
    
    /**
     * 保存音效开关设置
     */
    public static void saveSoundEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
    
    /**
     * 获取音效开关设置
     */
    public static boolean isSoundEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void setBgmEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_BGM_ENABLED, enabled).apply();
    }

    public static boolean isBgmEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_BGM_ENABLED, true);
    }

    public static void setSfxEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_SFX_ENABLED, enabled).apply();
    }

    public static boolean isSfxEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_SFX_ENABLED, true);
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }

    public static boolean isVibrationEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    /**
     * 保存音效音量设置 (0-100)，按用户独立存储
     */
    public static void saveSoundVolume(Context context, int volume) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String userId = sp.getString(KEY_USER_ID, "default");
        sp.edit().putInt(KEY_SOUND_VOLUME + "_" + userId, volume).apply();
    }
    
    /**
     * 获取音效音量设置 (0-100)，默认80
     */
    public static int getSoundVolume(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String userId = sp.getString(KEY_USER_ID, "default");
        return sp.getInt(KEY_SOUND_VOLUME + "_" + userId, 80);
    }
    
    /**
     * 保存背景音乐音量设置 (0-100)，按用户独立存储
     */
    public static void saveBgmVolume(Context context, int volume) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String userId = sp.getString(KEY_USER_ID, "default");
        sp.edit().putInt(KEY_BGM_VOLUME + "_" + userId, volume).apply();
    }
    
    /**
     * 获取背景音乐音量设置 (0-100)，默认80
     */
    public static int getBgmVolume(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String userId = sp.getString(KEY_USER_ID, "default");
        return sp.getInt(KEY_BGM_VOLUME + "_" + userId, 80);
    }
    
    /**
     * 退出登录（清除用户数据和Token）
     */
    public static void clearUser(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        
        // 先获取当前用户ID，用于清除对应的收藏数据
        String userId = sp.getString(KEY_USER_ID, null);
        
        SharedPreferences.Editor editor = sp.edit();
        // 清除认证相关数据
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_TOTAL_SCORE);
        editor.remove(KEY_WIN_COUNT);
        editor.remove(KEY_LOSE_COUNT);
        editor.remove("token");
        editor.remove("sessionId");
        // 清除用户个性化数据
        editor.remove("nickname");
        editor.remove("email");
        editor.remove("phone");
        editor.remove("bio");
        editor.remove("user_avatar_base64");
        editor.remove("avatarUrl");
        // 清除当前用户的收藏数据
        if (userId != null && !userId.isEmpty()) {
            editor.remove("collection_list_" + userId);
            // 清除当前用户的音量设置
            editor.remove(KEY_SOUND_VOLUME + "_" + userId);
            editor.remove(KEY_BGM_VOLUME + "_" + userId);
        }
        editor.remove("collection_list_default");
        // 保留：应用设置（音量、通知等）
        editor.apply();
    }
    
    /**
     * 清除所有数据
     */
    public static void clearAll(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }
    
    /**
     * 保存自定义字符串
     */
    public static void saveString(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(key, value).apply();
    }
    
    /**
     * 获取自定义字符串
     */
    public static String getString(Context context, String key) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(key, null);
    }
    
    /**
     * 保存自定义布尔值
     */
    public static void saveBoolean(Context context, String key, boolean value) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key, value).apply();
    }
    
    /**
     * 获取自定义布尔值
     */
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defaultValue);
    }
    
    /**
     * 保存自定义整数值
     */
    public static void saveInt(Context context, String key, int value) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(key, value).apply();
    }
    
    /**
     * 获取自定义整数值
     */
    public static int getInt(Context context, String key, int defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getInt(key, defaultValue);
    }
    
    // ==================== 临时对战缓存方法 ====================
    
    /**
     * 保存临时关键字（人机对战用）
     */
    public static void saveTempKeyword(Context context, String keyword) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TEMP_KEYWORD, keyword).apply();
    }
    
    /**
     * 获取临时关键字
     */
    public static String getTempKeyword(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TEMP_KEYWORD, null);
    }
    
    /**
     * 保存临时关键字2（双字飞花令用）
     */
    public static void saveTempKeyword2(Context context, String keyword2) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TEMP_KEYWORD2, keyword2).apply();
    }
    
    /**
     * 获取临时关键字2
     */
    public static String getTempKeyword2(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TEMP_KEYWORD2, null);
    }
    
    /**
     * 保存临时游戏模式
     */
    public static void saveTempGameMode(Context context, String gameMode) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_TEMP_GAME_MODE, gameMode).apply();
    }
    
    /**
     * 获取临时游戏模式
     */
    public static String getTempGameMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_TEMP_GAME_MODE, null);
    }
    
    /**
     * 清除临时对战缓存（对战结束后调用）
     */
    public static void clearTempBattleCache(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(KEY_TEMP_KEYWORD);
        editor.remove(KEY_TEMP_KEYWORD2);
        editor.remove(KEY_TEMP_GAME_MODE);
        editor.remove(KEY_TEMP_BATTLE_TYPE);
        editor.remove(KEY_TEMP_POSITION);
        editor.apply();
    }
}
