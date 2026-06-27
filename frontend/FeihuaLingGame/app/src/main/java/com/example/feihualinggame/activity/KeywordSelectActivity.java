package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.google.android.material.card.MaterialCardView;

/**
 * 关键字选择页面
 */
public class KeywordSelectActivity extends AppCompatActivity {
    private EditText etKeywordSearch;
    private Button btnConfirmKeyword;
    
    private String gameMode;
    private String battleType;
    private String opponentCode;
    private String selectedKeyword;
    private String selectedKeyword2;
    private int keywordSelectionStep = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyword_select);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 获取传入的参数
        gameMode = getIntent().getStringExtra("gameMode");
        battleType = getIntent().getStringExtra("battleType");
        opponentCode = getIntent().getStringExtra("opponentCode");

        // 初始化控件
        etKeywordSearch = findViewById(R.id.etKeywordSearch);
        btnConfirmKeyword = findViewById(R.id.btnConfirmKeyword);
        Button btnStartBattle = findViewById(R.id.btnStartBattle);

        // 顶栏返回按钮
        LinearLayout btnTopBack = findViewById(R.id.btnTopBack);
        btnTopBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        // 确认按钮
        btnConfirmKeyword.setOnClickListener(v -> {
            String customKeyword = etKeywordSearch.getText().toString().trim();
            if (!customKeyword.isEmpty()) {
                selectedKeyword = customKeyword;
                Toast.makeText(this, "已选择：" + selectedKeyword, Toast.LENGTH_SHORT).show();
            }
        });
        
        // 开始对战按钮
        btnStartBattle.setOnClickListener(v -> confirmKeyword());
        
        // 底部返回上一步按钮
        MaterialCardView btnBackToGameMode = findViewById(R.id.btnBackToGameMode);
        btnBackToGameMode.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });
    }



    /**
     * 确认选择关键字
     */
    private void confirmKeyword() {
        if (selectedKeyword == null || selectedKeyword.isEmpty()) {
            Toast.makeText(this, "请先选择一个关键字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 双字模式：需要选择两个关键字
        if ("double_keyword".equals(gameMode)) {
            if (keywordSelectionStep == 1) {
                selectedKeyword2 = selectedKeyword;
                selectedKeyword = null;
                keywordSelectionStep = 2;
                Toast.makeText(this, "已选择第一个字「" + selectedKeyword2 + "」，请选择第二个字", Toast.LENGTH_SHORT).show();
                etKeywordSearch.setText("");
                return;
            }
            String kw1 = selectedKeyword2;
            String kw2 = selectedKeyword;
            SharedPrefsUtil.saveLastKeyword(this, kw1);
            SharedPrefsUtil.saveString(this, "keyword2", kw2);
            launchBattle(kw1, kw2);
            return;
        }

        // 其他模式：直接跳转
        SharedPrefsUtil.saveLastKeyword(this, selectedKeyword);

        if (!validateKeyword()) {
            return;
        }

        launchBattle(selectedKeyword, null);
    }

    private void launchBattle(String keyword1, String keyword2) {
        jumpToBattle(keyword1, keyword2);
    }

    private void jumpToBattle(String keyword1, String keyword2) {
        Intent intent = new Intent(this, BattleActivity.class);
        intent.putExtra("gameMode", gameMode);
        intent.putExtra("keyword", keyword1);
        if (keyword2 != null && !keyword2.isEmpty()) {
            intent.putExtra("keyword2", keyword2);
        }
        intent.putExtra("battleType", battleType);
        startActivity(intent);
        finish();
    }

    /**
     * 验证关键字是否符合游戏模式要求
     */
    private boolean validateKeyword() {
        // 简易模式：单个汉字
        if ("single_keyword".equals(gameMode)) {
            if (selectedKeyword.length() != 1) {
                Toast.makeText(this, "单关键字飞花令请选择单个字", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        // 经典位置飞花令：单个汉字
        if ("position".equals(gameMode)) {
            if (selectedKeyword.length() != 1) {
                Toast.makeText(this, "位置飞花令请选择单个字", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        // 经典双字飞花令：由玩家选择两个字
        if ("double_keyword".equals(gameMode)) {
            if (selectedKeyword == null || selectedKeyword.length() != 1) {
                Toast.makeText(this, "双关键字飞花令需要选择两个字", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        // 首尾接龙：不需要选择关键字
        if ("chain".equals(gameMode)) {
            // 接龙模式不需要关键字，直接使用
            return true;
        }
        
        // 反飞花令：单个汉字（禁用字）
        if ("forbidden".equals(gameMode)) {
            if (selectedKeyword.length() != 1) {
                Toast.makeText(this, "反飞花令请选择单个禁用字", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        // 混合飞花令：单个汉字
        if ("mixed".equals(gameMode)) {
            if (selectedKeyword.length() != 1) {
                Toast.makeText(this, "混合飞花令请选择单个字", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        return true;
    }
}