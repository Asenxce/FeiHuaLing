package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.GameMode;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

/**
 * 游戏模式选择页面
 */
public class GameModeActivity extends AppCompatActivity {
    private LinearLayout btnSimpleMode;           // 简易模式
    private LinearLayout btnClassicPosition;      // 经典位置飞花令
    private LinearLayout btnClassicDouble;        // 经典双关键字
    private LinearLayout btnEntertainmentChain;   // 娱乐首尾接龙
    private LinearLayout btnEntertainmentColor;   // 娱乐颜色飞花令
    private LinearLayout btnEntertainmentNumber;  // 娱乐数字飞花令
    private LinearLayout btnEntertainmentForbidden; // 娱乐反飞花令
    private LinearLayout btnBack;                 // 返回按钮
    
    private String battleType = "ai";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_mode);

        SystemUIUtil.setupImmersiveStatusBar(this, R.color.cream, true);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 获取传入的对战类型
        if (getIntent().hasExtra("battleType")) {
            battleType = getIntent().getStringExtra("battleType");
        }

        // 初始化控件
        btnSimpleMode = findViewById(R.id.btn_simple_mode);
        btnClassicPosition = findViewById(R.id.btn_classic_position);
        btnClassicDouble = findViewById(R.id.btn_classic_double);
        btnEntertainmentChain = findViewById(R.id.btn_entertainment_chain);
        btnEntertainmentColor = findViewById(R.id.btn_entertainment_color);
        btnEntertainmentNumber = findViewById(R.id.btn_entertainment_number);
        btnEntertainmentForbidden = findViewById(R.id.btn_entertainment_forbidden);
        btnBack = findViewById(R.id.btn_back);

        // 为所有按钮添加动效
        ButtonAnimationHelper.addCombinedEffect(btnSimpleMode);
        ButtonAnimationHelper.addCombinedEffect(btnClassicPosition);
        ButtonAnimationHelper.addCombinedEffect(btnClassicDouble);
        ButtonAnimationHelper.addCombinedEffect(btnEntertainmentChain);
        ButtonAnimationHelper.addCombinedEffect(btnEntertainmentColor);
        ButtonAnimationHelper.addCombinedEffect(btnEntertainmentNumber);
        ButtonAnimationHelper.addCombinedEffect(btnEntertainmentForbidden);
        ButtonAnimationHelper.addCombinedEffect(btnBack);
        
        // 为页面元素添加入场动画（级联效果）
        setupEntryAnimations();

        // 设置点击事件
        btnSimpleMode.setOnClickListener(v -> selectMode(GameMode.SIMPLE));
        btnClassicPosition.setOnClickListener(v -> selectMode(GameMode.CLASSIC_POSITION));
        btnClassicDouble.setOnClickListener(v -> selectMode(GameMode.CLASSIC_DOUBLE));
        btnEntertainmentChain.setOnClickListener(v -> selectMode(GameMode.ENTERTAINMENT_CHAIN));
        btnEntertainmentColor.setOnClickListener(v -> selectMode(GameMode.ENTERTAINMENT_COLOR));
        btnEntertainmentNumber.setOnClickListener(v -> selectMode(GameMode.ENTERTAINMENT_NUMBER));
        btnEntertainmentForbidden.setOnClickListener(v -> selectMode(GameMode.ENTERTAINMENT_FORBIDDEN));
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });
    }

    /**
     * 选择游戏模式
     */
    private void selectMode(GameMode mode) {
        AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
        // 保存选中的模式
        SharedPrefsUtil.saveLastGameMode(this, mode.getCode());
        
        // 人机对战：跳转到自定义关键字输入页面
        if ("ai".equals(battleType)) {
            Intent intent = new Intent(this, KeywordInputActivity.class);
            intent.putExtra("gameMode", mode.getCode());
            intent.putExtra("battleType", battleType);
            startActivity(intent);
        } else {
            // 好友对战：保留原来的关键字选择页面
            Intent intent = new Intent(this, KeywordSelectActivity.class);
            intent.putExtra("gameMode", mode.getCode());
            intent.putExtra("battleType", battleType);
            startActivity(intent);
        }
    }
    
    /**
     * 设置入场动画
     */
    private void setupEntryAnimations() {
        ButtonAnimationHelper.addEntryAnimation(btnSimpleMode, 0);
        ButtonAnimationHelper.addEntryAnimation(btnClassicPosition, 100);
        ButtonAnimationHelper.addEntryAnimation(btnClassicDouble, 150);
        ButtonAnimationHelper.addEntryAnimation(btnEntertainmentChain, 200);
        ButtonAnimationHelper.addEntryAnimation(btnEntertainmentColor, 250);
        ButtonAnimationHelper.addEntryAnimation(btnEntertainmentNumber, 300);
        ButtonAnimationHelper.addEntryAnimation(btnEntertainmentForbidden, 350);
        ButtonAnimationHelper.addEntryAnimation(btnBack, 400);
    }
}