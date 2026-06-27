package com.example.feihualinggame.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.SystemUIUtil;

/**
 * 规则详情页面
 */
public class RulesActivity extends AppCompatActivity {

    private LinearLayout btnBack;
    private ImageView btnTopBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        // 设置状态栏颜色与顶栏一致
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.poetry_card_background));

        // 设置状态栏图标为深色（浅色顶栏）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 隐藏底部导航栏指示条
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 初始化控件
        btnBack = findViewById(R.id.btn_back);
        btnTopBack = findViewById(R.id.btn_top_back);

        // 为按钮添加动效
        ButtonAnimationHelper.addCombinedEffect(btnTopBack);
        ButtonAnimationHelper.addCombinedEffect(btnBack);

        // 顶栏返回按钮
        btnTopBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        // 底部返回按钮
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        // 设置入场动画
        setupEntryAnimations();
    }

    /**
     * 设置入场动画
     */
    private void setupEntryAnimations() {
        View scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            ButtonAnimationHelper.addEntryAnimation(scrollView, 0);
        }
        ButtonAnimationHelper.addEntryAnimation(btnBack, 200);
        ButtonAnimationHelper.addEntryAnimation(btnTopBack, 100);
    }
}
