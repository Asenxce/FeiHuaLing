package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.GameMode;
import com.example.feihualinggame.bean.RoomInfoBean;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.example.feihualinggame.validator.KeywordValidator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class KeywordInputActivity extends AppCompatActivity {
    private TextView tvKeywordInputTitle;
    private TextView tvModeHint;
    private EditText etKeyword1;
    private LinearLayout llKeyword2Container;
    private EditText etKeyword2;
    private LinearLayout llPositionSelector;
    private TextView tvChar1, tvChar2, tvChar3, tvChar4, tvChar5, tvChar6, tvChar7;
    private TextView tvPos1, tvPos2, tvPos3, tvPos4, tvPos5, tvPos6, tvPos7;
    private ImageView btnRandomKeyword;
    private ImageView btnRandomKeyword2;
    private Button btnTime15, btnTime30, btnTime60;
    private MaterialButton btnStartBattle;
    private MaterialCardView btnBack;
    private LinearLayout btnTopBack;
    private TextView tvTimeLabel;
    private View cardTimeSection;

    private String gameMode;
    private String gameModeApi;
    private String battleType;
    private int selectedPosition = 3;
    private int selectedTime = 30;

    private int roomTimeLimit;
    private int roomMaxPlayers;
    private int roomMinPlayers;
    private int roomFaultLimit;
    private String roomGameType;

    @Override
    protected void onResume() {
        super.onResume();
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyword_input_new);

        SystemUIUtil.setupImmersiveStatusBar(this, R.color.cream, true);
        SystemUIUtil.hideNavigationBarIndicator(this);

        gameMode = getIntent().getStringExtra("gameMode");
        battleType = getIntent().getStringExtra("battleType");

        if (gameMode == null || gameMode.isEmpty()) {
            gameMode = "single_keyword";
        }
        if (battleType == null || battleType.isEmpty()) {
            battleType = "ai";
        }

        if ("multi".equals(battleType)) {
            roomTimeLimit = getIntent().getIntExtra("timeLimit", 60);
            roomMaxPlayers = getIntent().getIntExtra("maxPlayers", 4);
            roomMinPlayers = getIntent().getIntExtra("minPlayers", 2);
            roomFaultLimit = getIntent().getIntExtra("faultLimit", 3);
            roomGameType = getIntent().getStringExtra("gameType");
            if (roomGameType == null) roomGameType = "ENTERTAINMENT";
            gameModeApi = getIntent().getStringExtra("gameModeApi");
            if (gameModeApi == null) gameModeApi = gameMode.toUpperCase();
            selectedTime = roomTimeLimit;
        }

        tvKeywordInputTitle = findViewById(R.id.tvKeywordInputTitle);
        tvModeHint = findViewById(R.id.tvModeHint);
        etKeyword1 = findViewById(R.id.etKeyword1);
        llKeyword2Container = findViewById(R.id.llKeyword2Container);
        etKeyword2 = findViewById(R.id.etKeyword2);
        llPositionSelector = findViewById(R.id.llPositionSelector);
        tvChar1 = findViewById(R.id.tvChar1);
        tvChar2 = findViewById(R.id.tvChar2);
        tvChar3 = findViewById(R.id.tvChar3);
        tvChar4 = findViewById(R.id.tvChar4);
        tvChar5 = findViewById(R.id.tvChar5);
        tvChar6 = findViewById(R.id.tvChar6);
        tvChar7 = findViewById(R.id.tvChar7);
        tvPos1 = findViewById(R.id.tvPos1);
        tvPos2 = findViewById(R.id.tvPos2);
        tvPos3 = findViewById(R.id.tvPos3);
        tvPos4 = findViewById(R.id.tvPos4);
        tvPos5 = findViewById(R.id.tvPos5);
        tvPos6 = findViewById(R.id.tvPos6);
        tvPos7 = findViewById(R.id.tvPos7);
        btnRandomKeyword = findViewById(R.id.btnRandomKeyword);
        btnRandomKeyword2 = findViewById(R.id.btnRandomKeyword2);
        btnTime15 = findViewById(R.id.btnTime15);
        btnTime30 = findViewById(R.id.btnTime30);
        btnTime60 = findViewById(R.id.btnTime60);
        btnStartBattle = findViewById(R.id.btnStartBattle);
        btnBack = findViewById(R.id.btnBack);
        btnTopBack = findViewById(R.id.btnTopBack);
        tvTimeLabel = findViewById(R.id.tv_time_label);
        cardTimeSection = findViewById(R.id.card_time_section);

        setupButtonDisabledColor();

        setupUIForGameMode();

        ButtonAnimationHelper.addCombinedEffect(btnStartBattle);
        ButtonAnimationHelper.addCombinedEffect(btnBack);

        initTimeButtons();
        initPositionSelector();
        initRandomButtons();
    }

    private void setupButtonDisabledColor() {
        int goldColor = ContextCompat.getColor(this, R.color.poetry_primary);
        int disabledAlpha = 128;
        int disabledColor = (disabledAlpha << 24) | (goldColor & 0x00FFFFFF);
        
        android.content.res.ColorStateList colorStateList = new android.content.res.ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_enabled},
                        new int[]{-android.R.attr.state_enabled}
                },
                new int[]{
                        goldColor,
                        disabledColor
                }
        );
        btnStartBattle.setBackgroundTintList(colorStateList);
    }

    private void initTimeButtons() {
        btnTime15.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            selectTime(15);
        });
        btnTime30.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            selectTime(30);
        });
        btnTime60.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            selectTime(60);
        });
        selectTime(selectedTime);

        if ("multi".equals(battleType)) {
            btnTime15.setVisibility(View.GONE);
            btnTime30.setVisibility(View.GONE);
            btnTime60.setVisibility(View.GONE);
            tvTimeLabel.setVisibility(View.GONE);
            cardTimeSection.setVisibility(View.GONE);
            btnStartBattle.setText("创建房间");
            btnStartBattle.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                createMultiRoom();
            });
        } else {
            btnStartBattle.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                startBattle();
            });
        }

        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        btnTopBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });
    }

    private void selectTime(int time) {
        selectedTime = time;
        btnTime15.setSelected(time == 15);
        btnTime30.setSelected(time == 30);
        btnTime60.setSelected(time == 60);
        btnTime15.setTextColor(time == 15 ? getColor(R.color.poetry_primary) : getColor(R.color.poetry_text_secondary));
        btnTime30.setTextColor(time == 30 ? getColor(R.color.poetry_primary) : getColor(R.color.poetry_text_secondary));
        btnTime60.setTextColor(time == 60 ? getColor(R.color.poetry_primary) : getColor(R.color.poetry_text_secondary));
    }

    private void setupUIForGameMode() {
        GameMode mode = GameMode.fromCode(gameMode);
        if ("multi".equals(battleType)) {
            tvKeywordInputTitle.setText(mode.getDisplayName() + " - 创建房间");
        } else {
            tvKeywordInputTitle.setText(mode.getDisplayName() + " - 准备界面");
        }

        InputFilter[] filterArray = new InputFilter[]{new InputFilter.LengthFilter(1)};

        switch (gameMode) {
            case "single_keyword":
                tvModeHint.setText("单关键字飞花令：输入一个汉字作为关键字\n例如：春、花、月、山");
                etKeyword1.setHint("春");
                etKeyword1.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.GONE);
                llPositionSelector.setVisibility(View.GONE);
                break;
            case "position":
                tvModeHint.setText("位置飞花令：输入一个汉字\n该字必须出现在指定位置");
                etKeyword1.setHint("春");
                etKeyword1.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.GONE);
                llPositionSelector.setVisibility(View.VISIBLE);
                break;
            case "double_keyword":
                tvModeHint.setText("双关键字飞花令：输入两个不同的汉字\n诗句中必须同时包含这两个字");
                etKeyword1.setHint("春");
                etKeyword1.setFilters(filterArray);
                etKeyword2.setHint("月");
                etKeyword2.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.VISIBLE);
                llPositionSelector.setVisibility(View.GONE);
                break;
            case "chain":
                tvModeHint.setText("首尾接龙飞花令\n你必须以指定字开头，随后尾字=新首字");
                etKeyword1.setHint("春");
                etKeyword1.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.GONE);
                llPositionSelector.setVisibility(View.GONE);
                btnStartBattle.setText("开始接龙");
                break;
            case "forbidden":
                tvModeHint.setText("反飞花令\n诗句中【不能包含】该字");
                etKeyword1.setHint("禁用字");
                etKeyword1.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.GONE);
                llPositionSelector.setVisibility(View.GONE);
                break;
            case "color":
                tvModeHint.setText("颜色飞花令\n输入一个颜色字");
                etKeyword1.setHint("红");
                etKeyword1.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.GONE);
                llPositionSelector.setVisibility(View.GONE);
                break;
            case "number":
                tvModeHint.setText("数字飞花令\n输入一个数字");
                etKeyword1.setHint("一");
                etKeyword1.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.GONE);
                llPositionSelector.setVisibility(View.GONE);
                break;
            default:
                tvModeHint.setText("输入关键字开始游戏");
                etKeyword1.setHint("关键字");
                etKeyword1.setFilters(filterArray);
                llKeyword2Container.setVisibility(View.GONE);
                llPositionSelector.setVisibility(View.GONE);
                break;
        }

        if ("multi".equals(battleType)) {
            tvTimeLabel.setVisibility(View.GONE);
            cardTimeSection.setVisibility(View.GONE);
        }
    }

    private void initPositionSelector() {
        TextView[] positionViews = {tvPos1, tvPos2, tvPos3, tvPos4, tvPos5, tvPos6, tvPos7};
        for (int i = 0; i < positionViews.length; i++) {
            final int position = i + 1;
            positionViews[i].setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                selectPosition(position);
            });
        }
        selectPosition(selectedPosition);
    }

    private void initRandomButtons() {
        btnRandomKeyword.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            String randomKeyword = generateRandomKeywordForMode(gameMode, 1);
            etKeyword1.setText(randomKeyword);
            etKeyword1.setSelection(randomKeyword.length());
        });

        if (btnRandomKeyword2 != null) {
            btnRandomKeyword2.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                String keyword1 = etKeyword1.getText().toString().trim();
                String randomKeyword;
                do {
                    randomKeyword = generateRandomKeywordForMode(gameMode, 2);
                } while (randomKeyword.equals(keyword1));
                etKeyword2.setText(randomKeyword);
                etKeyword2.setSelection(randomKeyword.length());
            });
        }
    }

    private String generateRandomKeywordForMode(String mode, int keywordIndex) {
        switch (mode) {
            case "color":
                return getRandomFromPool(COLOR_KEYWORDS);
            case "number":
                return getRandomFromPool(NUMBER_KEYWORDS);
            case "double_keyword":
                return getRandomFromPool(COMMON_KEYWORDS);
            default:
                return getRandomFromPool(COMMON_KEYWORDS);
        }
    }

    private String getRandomFromPool(String[] pool) {
        return pool[(int) (Math.random() * pool.length)];
    }

    private static final String[] COMMON_KEYWORDS = {
        "春", "夏", "秋", "冬",
        "花", "月", "风", "雪",
        "山", "水", "云", "天",
        "酒", "诗", "琴", "画",
        "红", "绿", "青", "白",
        "一", "千", "万", "年"
    };

    private static final String[] COLOR_KEYWORDS = {
        "红", "绿", "蓝", "紫",
        "黄", "橙", "青", "白",
        "黑", "粉", "金", "银",
        "彩", "碧", "翠", "丹"
    };

    private static final String[] NUMBER_KEYWORDS = {
        "一", "二", "三", "四",
        "五", "六", "七", "八",
        "九", "十", "百", "千",
        "万", "半", "双", "孤"
    };

    private void selectPosition(int position) {
        selectedPosition = position;
        TextView[] positionViews = {tvPos1, tvPos2, tvPos3, tvPos4, tvPos5, tvPos6, tvPos7};
        for (int i = 0; i < positionViews.length; i++) {
            boolean isSelected = (i + 1 == position);
            positionViews[i].setSelected(isSelected);
            positionViews[i].setTextColor(isSelected ? getColor(R.color.text_on_primary) : getColor(R.color.poetry_text_secondary));
        }
        TextView[] charViews = {tvChar1, tvChar2, tvChar3, tvChar4, tvChar5, tvChar6, tvChar7};
        for (int i = 0; i < charViews.length; i++) {
            boolean isHighlight = (i + 1 == position);
            if (isHighlight) {
                charViews[i].setTextColor(getColor(R.color.poetry_primary));
                charViews[i].setTextSize(22);
            } else {
                charViews[i].setTextColor(getColor(R.color.poetry_text_primary));
                charViews[i].setTextSize(18);
            }
        }
    }

    private void startBattle() {
        String keyword1 = etKeyword1.getText().toString().trim();
        String keyword2 = "";

        if ("chain".equals(gameMode)) {
            if (keyword1.isEmpty()) {
                Toast.makeText(this, "请输入起始字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword1.length() != 1) {
                Toast.makeText(this, "起始字必须是单个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (keyword1.isEmpty()) {
                Toast.makeText(this, "请输入关键字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword1.length() != 1) {
                Toast.makeText(this, "关键字必须是单个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if ("double_keyword".equals(gameMode)) {
            keyword2 = etKeyword2.getText().toString().trim();
            if (keyword2.isEmpty()) {
                Toast.makeText(this, "请输入第二个关键字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword2.length() != 1) {
                Toast.makeText(this, "第二个关键字必须是单个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword1.equals(keyword2)) {
                Toast.makeText(this, "两个关键字不能相同", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        SharedPrefsUtil.saveTempKeyword(this, keyword1);
        if ("double_keyword".equals(gameMode)) {
            SharedPrefsUtil.saveTempKeyword2(this, keyword2);
        }
        SharedPrefsUtil.saveTempGameMode(this, gameMode);

        if ("position".equals(gameMode)) {
            SharedPrefsUtil.saveInt(this, "temp_position", selectedPosition);
        }

        if ("color".equals(gameMode) || "number".equals(gameMode)) {
            String type = "color".equals(gameMode) ? "color" : "number";
            final String kw1 = keyword1;
            final String kw2 = keyword2;
            validateColorNumberKeyword(kw1, kw2, type, "开始对战", () -> jumpToLoadingScreen(kw1, kw2));
            return;
        }

        if ("chain".equals(gameMode) || "forbidden".equals(gameMode)) {
            jumpToLoadingScreen(keyword1, keyword2);
            return;
        }

        jumpToLoadingScreen(keyword1, keyword2);
    }

    private void createMultiRoom() {
        String keyword1 = etKeyword1.getText().toString().trim();
        String keyword2 = "";

        if ("chain".equals(gameMode)) {
            if (keyword1.isEmpty()) {
                Toast.makeText(this, "请输入起始字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword1.length() != 1) {
                Toast.makeText(this, "起始字必须是单个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if ("forbidden".equals(gameMode)) {
            if (keyword1.isEmpty()) {
                Toast.makeText(this, "请输入禁用字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword1.length() != 1) {
                Toast.makeText(this, "禁用字必须是单个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (keyword1.isEmpty()) {
                Toast.makeText(this, "请输入关键字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword1.length() != 1) {
                Toast.makeText(this, "关键字必须是单个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if ("double_keyword".equals(gameMode)) {
            keyword2 = etKeyword2.getText().toString().trim();
            if (keyword2.isEmpty()) {
                Toast.makeText(this, "请输入第二个关键字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword2.length() != 1) {
                Toast.makeText(this, "第二个关键字必须是单个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (keyword1.equals(keyword2)) {
                Toast.makeText(this, "两个关键字不能相同", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if ("color".equals(gameMode) || "number".equals(gameMode)) {
            String type = "color".equals(gameMode) ? "color" : "number";
            final String kw1 = keyword1;
            final String kw2 = keyword2;
            validateColorNumberKeyword(kw1, kw2, type, "创建房间", () -> doCreateRoom(kw1, kw2));
            return;
        }

        doCreateRoom(keyword1, keyword2);
    }

    private void validateColorNumberKeyword(String keyword1, String keyword2, String type,
                                            String restoreText, Runnable onSuccess) {
        btnStartBattle.setEnabled(false);
        btnStartBattle.setText("验证中...");
        KeywordValidator.checkKeywordExists(this, keyword1, type, new KeywordValidator.OnKeywordCheckListener() {
            @Override
            public void onCheckResult(boolean exists, String message) {
                runOnUiThread(() -> {
                    btnStartBattle.setEnabled(true);
                    btnStartBattle.setText(restoreText);
                    if (!exists) {
                        Toast.makeText(KeywordInputActivity.this, message, Toast.LENGTH_LONG).show();
                        return;
                    }
                    onSuccess.run();
                });
            }
        });
    }

    private void doCreateRoom(String keyword, String keyword2) {
        long userId = Long.parseLong(SharedPrefsUtil.getUserId(this));
        String mode = (gameModeApi != null) ? gameModeApi : gameMode.toUpperCase();
        JsonObject body = new JsonObject();
        body.addProperty("creatorId", userId);
        body.addProperty("gameMode", mode);
        body.addProperty("timeLimit", roomTimeLimit);
        body.addProperty("maxPlayers", roomMaxPlayers);
        body.addProperty("minPlayers", roomMinPlayers);
        body.addProperty("faultLimit", roomFaultLimit);
        body.addProperty("gameType", roomGameType);

        switch (mode) {
            case "SIMPLE":
            case "POSITION":
            case "CHAIN":
            case "CUSTOM":
                body.addProperty("keyword", keyword);
                break;
            case "DOUBLE_KEYWORD":
                body.addProperty("keyword", keyword);
                body.addProperty("keyword2", keyword2);
                break;
            case "COLOR":
                body.addProperty("colorKeyword", keyword);
                break;
            case "NUMBER":
                body.addProperty("numberKeyword", keyword);
                break;
            case "FORBIDDEN":
                body.addProperty("forbiddenWord", keyword);
                break;
        }

        if ("POSITION".equals(mode)) {
            body.addProperty("keywordPosition", selectedPosition);
        }

        btnStartBattle.setEnabled(false);
        btnStartBattle.setText("创建中...");

        OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_CREATE, body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnStartBattle.setEnabled(true);
                    btnStartBattle.setText("创建房间");
                    Toast.makeText(KeywordInputActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                runOnUiThread(() -> {
                    btnStartBattle.setEnabled(true);
                    btnStartBattle.setText("创建房间");
                    try {
                        JsonObject json = new Gson().fromJson(respBody, JsonObject.class);
                        int code = json.get("code").getAsInt();
                        if (code == 200) {
                            RoomInfoBean room = new Gson().fromJson(json.get("data"), RoomInfoBean.class);
                            Intent intent = new Intent(KeywordInputActivity.this, MultiRoomActivity.class);
                            intent.putExtra("roomId", room.getRoomId());
                            intent.putExtra("roomCode", room.getRoomCode());
                            intent.putExtra("isCreator", true);
                            startActivity(intent);
                            finish();
                        } else {
                            String msg = json.has("message") ? json.get("message").getAsString() : "创建失败";
                            Toast.makeText(KeywordInputActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(KeywordInputActivity.this, "创建失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void jumpToLoadingScreen(String keyword1, String keyword2) {
        Intent intent = new Intent(this, PoetryLoadingActivity.class);
        intent.putExtra("keyword", keyword1);
        intent.putExtra("keyword2", keyword2);
        intent.putExtra("gameMode", gameMode);
        intent.putExtra("battleType", battleType);
        intent.putExtra("timeLimit", selectedTime);
        if ("color".equals(gameMode)) {
            intent.putExtra("colorKeyword", keyword1);
        } else if ("number".equals(gameMode)) {
            intent.putExtra("numberKeyword", keyword1);
        } else if ("forbidden".equals(gameMode)) {
            intent.putExtra("forbiddenWord", keyword1);
        }
        if ("position".equals(gameMode)) {
            intent.putExtra("positionKeyword", keyword1);
            intent.putExtra("keywordPosition", selectedPosition);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
