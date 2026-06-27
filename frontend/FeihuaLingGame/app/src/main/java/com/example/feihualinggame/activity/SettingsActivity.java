package com.example.feihualinggame.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.feihualinggame.FeihuaLingGameApplication;
import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.FeedbackManager;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.RippleEffectHelper;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

/**
 * 系统设置页面
 */
public class SettingsActivity extends AppCompatActivity {
    private ImageView btnBack;
    private SeekBar seekbarSoundVolume;
    private SeekBar seekbarBgmVolume;
    private  TextView btnResetSound;
    private Spinner spinnerThemeMode;
    private Switch switchNotification;
    private Switch switchBgm;
    private Switch switchSfx;
    private Switch switchVibration;
    private LinearLayout layoutNotification;
    private LinearLayout layoutCacheManage;
    private boolean isSpinnerInitialized = false;
    private LinearLayout layoutUserAgreement;
    private LinearLayout layoutPrivacyPolicy;
    private ViewGroup rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用主题模式（必须 super.onCreate 之前）
        applyThemeMode();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 隐藏底部导航栏指示条
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 初始化控件
        btnBack = findViewById(R.id.btn_back);
        seekbarSoundVolume = findViewById(R.id.seekbar_sound_volume);
        seekbarBgmVolume = findViewById(R.id.seekbar_bgm_volume);
        btnResetSound = findViewById(R.id.btn_reset_sound);
        spinnerThemeMode = findViewById(R.id.spinner_theme_mode);
        switchNotification = findViewById(R.id.switch_notification);
        switchBgm = findViewById(R.id.switch_bgm);
        switchSfx = findViewById(R.id.switch_sfx);
        layoutNotification = findViewById(R.id.layout_notification);
        layoutCacheManage = findViewById(R.id.layout_cache_manage);
        layoutUserAgreement = findViewById(R.id.layout_user_agreement);
        layoutPrivacyPolicy = findViewById(R.id.layout_privacy_policy);
        rootLayout = findViewById(R.id.root_layout);

        // 主题模式选择 - 必须在loadSettings之前设置adapter
        String[] themeModes = {"浅色模式", "深色模式", "跟随系统"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, themeModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerThemeMode.setAdapter(adapter);
        
        spinnerThemeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true;
                    return;
                }
                String[] modes = {"light", "dark", "system"};
                String selectedMode = modes[position];
                SharedPrefsUtil.saveString(SettingsActivity.this, "theme_mode", selectedMode);
                
                switch (selectedMode) {
                    case "dark":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    case "system":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                }
                SettingsActivity.this.recreate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 加载设置 - 在adapter设置后调用
        loadSettings();

        // 返回按钮
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        // 音效音量调节
        seekbarSoundVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    AudioController.getInstance().setSFXVolume(progress / 100.0f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            }
        });

        // 背景音乐音量调节
        seekbarBgmVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    AudioController.getInstance().setBGMVolume(progress / 100.0f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            }
        });

        // 背景音乐开关
        switchBgm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AudioController.getInstance().setBGMEnabled(isChecked);
            seekbarBgmVolume.setEnabled(isChecked);
            Toast.makeText(this, isChecked ? "已开启背景音乐" : "已关闭背景音乐", Toast.LENGTH_SHORT).show();
        });

        // 音效开关
        switchSfx.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AudioController.getInstance().setSFXEnabled(isChecked);
            seekbarSoundVolume.setEnabled(isChecked);
            Toast.makeText(this, isChecked ? "已开启音效" : "已关闭音效", Toast.LENGTH_SHORT).show();
        });

        // 振动开关
        switchVibration = findViewById(R.id.switch_vibration);
        switchVibration.setChecked(FeedbackManager.getInstance().isVibrationEnabled());
        switchVibration.setOnCheckedChangeListener((btn, checked) -> {
            FeedbackManager.getInstance().setVibrationEnabled(this, checked);
        });

        // 消息通知开关
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPrefsUtil.saveBoolean(this, "notification_enabled", isChecked);
            Toast.makeText(this, isChecked ? "已开启消息通知" : "已关闭消息通知", Toast.LENGTH_SHORT).show();
        });

        // 点击消息通知行进入消息中心
        layoutNotification.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MailboxActivity.class);
            startActivity(intent);
        });

        // 缓存管理
        layoutCacheManage.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showCacheManageDialog();
        });

        // 用户协议
        layoutUserAgreement.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showUserAgreementDialog();
        });

        // 隐私政策
        layoutPrivacyPolicy.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showPrivacyPolicyDialog();
        });

        // 恢复默认设置
        btnResetSound.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showResetSoundDialog();
        });
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
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "light":
            default:
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        int soundVolume = SharedPrefsUtil.getSoundVolume(this);
        int bgmVolume = SharedPrefsUtil.getBgmVolume(this);
        String themeMode = SharedPrefsUtil.getString(this, "theme_mode");
        if (themeMode == null || themeMode.isEmpty()) {
            themeMode = "light";
        }
        boolean notificationEnabled = SharedPrefsUtil.getBoolean(this, "notification_enabled", true);

        seekbarSoundVolume.setProgress(soundVolume);
        seekbarBgmVolume.setProgress(bgmVolume);
        
        // 根据SharedPrefs设置Spinner选中项
        int selectedIndex = 0;
        if ("dark".equals(themeMode)) {
            selectedIndex = 1;
        } else if ("system".equals(themeMode)) {
            selectedIndex = 2;
        }
        spinnerThemeMode.setSelection(selectedIndex, false);
        
        switchNotification.setChecked(notificationEnabled);

        boolean bgmEnabled = SharedPrefsUtil.isBgmEnabled(this);
        boolean sfxEnabled = SharedPrefsUtil.isSfxEnabled(this);
        switchBgm.setChecked(bgmEnabled);
        switchSfx.setChecked(sfxEnabled);
        seekbarBgmVolume.setEnabled(bgmEnabled);
        seekbarSoundVolume.setEnabled(sfxEnabled);

        AudioController.getInstance().setSFXVolume(soundVolume / 100.0f);
        AudioController.getInstance().setBGMVolume(bgmVolume / 100.0f);
    }

    /**
     * 显示恢复默认设置对话框
     */
    private void showResetSoundDialog() {
        new AlertDialog.Builder(this)
            .setTitle("恢复默认设置")
            .setMessage("确定要将声音设置恢复为默认值吗？\n\n• 音效音量：80%\n• 背景音乐：80%")
            .setPositiveButton("恢复", (dialog, which) -> {
            resetSoundToDefault();
            FeedbackManager.getInstance().setVibrationEnabled(this, true);
            switchVibration.setChecked(true);
            Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 恢复声音设置为默认值
     */
    private void resetSoundToDefault() {
        int defaultVolume = 80;

        seekbarSoundVolume.setProgress(defaultVolume);
        seekbarBgmVolume.setProgress(defaultVolume);

        SharedPrefsUtil.saveSoundVolume(this, defaultVolume);
        SharedPrefsUtil.saveBgmVolume(this, defaultVolume);

        switchBgm.setChecked(true);
        switchSfx.setChecked(true);
        seekbarBgmVolume.setEnabled(true);
        seekbarSoundVolume.setEnabled(true);

        AudioController.getInstance().setBGMVolume(defaultVolume / 100.0f);
        AudioController.getInstance().setSFXVolume(defaultVolume / 100.0f);
        AudioController.getInstance().setBGMEnabled(true);
        AudioController.getInstance().setSFXEnabled(true);
        AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
    }

    /**
     * 显示缓存管理对话框
     */
    private void showCacheManageDialog() {
        new AlertDialog.Builder(this)
            .setTitle("缓存管理")
            .setMessage("确定要清除应用缓存吗？\n\n这将清除临时文件和数据，但不会影响您的个人信息。")
            .setPositiveButton("清除", (dialog, which) -> {
                // 清除缓存
                clearAppCache();
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 清除应用缓存
     */
    private void clearAppCache() {
        try {
            // 清除代码缓存
            getCacheDir().delete();
            // 清除外存缓存
            if (getExternalCacheDir() != null) {
                getExternalCacheDir().delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示用户协议对话框
     */
    private void showUserAgreementDialog() {
        String agreement = "用户协议\n\n" +
                "欢迎使用飞花令游戏！\n\n" +
                "1. 本应用仅供娱乐和学习使用\n" +
                "2. 请遵守相关法律法规\n" +
                "3. 尊重知识产权，合理使用诗词内容\n" +
                "4. 禁止利用本应用进行任何违法活动\n" +
                "5. 我们有权根据需要修改本协议\n\n" +
                "继续使用即表示您同意以上条款。";
        
        new AlertDialog.Builder(this)
            .setTitle("用户协议")
            .setMessage(agreement)
            .setPositiveButton("我同意", null)
            .show();
    }

    /**
     * 显示隐私政策对话框
     */
    private void showPrivacyPolicyDialog() {
        String privacy = "隐私政策\n\n" +
                "我们非常重视您的隐私保护：\n\n" +
                "1. 信息收集\n" +
                "   - 仅收集必要的用户信息用于账号管理\n" +
                "   - 不会收集您的个人敏感信息\n\n" +
                "2. 信息使用\n" +
                "   - 仅用于提供和改进服务\n" +
                "   - 不会将信息用于其他目的\n\n" +
                "3. 信息安全\n" +
                "   - 采取合理措施保护您的数据安全\n" +
                "   - 不会向第三方泄露您的信息\n\n" +
                "4. 您的权利\n" +
                "   - 您可以随时查看和修改个人信息\n" +
                "   - 您可以选择注销账号";
        
        new AlertDialog.Builder(this)
            .setTitle("隐私政策")
            .setMessage(privacy)
            .setPositiveButton("我知道了", null)
            .show();
    }
}
