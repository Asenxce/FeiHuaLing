package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.SystemUIUtil;

public class MultiRoomCreateActivity extends AppCompatActivity {
    private static final String TAG = "创建房间";

    private Spinner spGameMode;
    private SeekBar sbTimeLimit;
    private TextView tvTimeLimit;
    private SeekBar sbMaxPlayers;
    private TextView tvMaxPlayers;
    private SeekBar sbMinPlayers;
    private TextView tvMinPlayers;
    private SeekBar sbFaultLimit;
    private TextView tvFaultLimit;
    private SeekBar sbHelpLimit;
    private TextView tvHelpLimit;
    private RadioGroup rgGameType;
    private Button btnCreate;
    private View btnBack;

    private static final String[] GAME_MODES = {"单关键字飞花令", "位置飞花令", "双关键字飞花令", "首尾接龙飞花令", "颜色飞花令", "数字飞花令", "反飞花令", "自定义飞花令"};
    private static final String[] GAME_MODE_CODES = {"SIMPLE", "POSITION", "DOUBLE_KEYWORD", "CHAIN", "COLOR", "NUMBER", "FORBIDDEN", "CUSTOM"};
    private static final String[] GAME_MODE_UI_CODES = {"single_keyword", "position", "double_keyword", "chain", "color", "number", "forbidden", "custom"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_room_create);

        SystemUIUtil.setupImmersiveStatusBar(this, R.color.poetry_card_background, true);
        SystemUIUtil.hideNavigationBarIndicator(this);
        initViews();
        setupListeners();
        AudioController.getInstance().playBGM(AudioController.SCENE_BATTLE);
    }

    private void initViews() {
        spGameMode = findViewById(R.id.sp_game_mode);
        sbTimeLimit = findViewById(R.id.sb_time_limit);
        tvTimeLimit = findViewById(R.id.tv_time_limit);
        sbMaxPlayers = findViewById(R.id.sb_max_players);
        tvMaxPlayers = findViewById(R.id.tv_max_players);
        sbMinPlayers = findViewById(R.id.sb_min_players);
        tvMinPlayers = findViewById(R.id.tv_min_players);
        sbFaultLimit = findViewById(R.id.sb_fault_limit);
        tvFaultLimit = findViewById(R.id.tv_fault_limit);
        sbHelpLimit = findViewById(R.id.sb_help_limit);
        tvHelpLimit = findViewById(R.id.tv_help_limit);
        rgGameType = findViewById(R.id.rg_game_type);
        btnCreate = findViewById(R.id.btn_create);
        btnBack = findViewById(R.id.btn_back);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, GAME_MODES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGameMode.setAdapter(adapter);

        ButtonAnimationHelper.addCombinedEffect(btnCreate);
        ButtonAnimationHelper.addCombinedEffect(btnBack);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        sbTimeLimit.setOnSeekBarChangeListener(new SeekBarListener(tvTimeLimit, "秒", 30));
        sbFaultLimit.setOnSeekBarChangeListener(new SeekBarListener(tvFaultLimit, "次", 3));
        sbHelpLimit.setOnSeekBarChangeListener(new SeekBarListener(tvHelpLimit, "次", 2));

        // 最少开局人数：联动限制不能超过最多人数
        sbMinPlayers.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int maxVal = sbMaxPlayers.getProgress(); // 最多人数的progress
                if (progress > maxVal) {
                    seekBar.setProgress(maxVal);
                    progress = maxVal;
                }
                tvMinPlayers.setText((progress + 2) + "人");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 最多人数变化时，联动限制最少开局人数
        sbMaxPlayers.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMaxPlayers.setText((progress + 4) + "人");
                if (sbMinPlayers.getProgress() > progress) {
                    sbMinPlayers.setProgress(progress);
                    tvMinPlayers.setText((progress + 2) + "人");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnCreate.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            handleCreateRoom();
        });
    }

    private void handleCreateRoom() {
        int selectedIndex = spGameMode.getSelectedItemPosition();
        String gameMode = GAME_MODE_UI_CODES[selectedIndex];
        String gameModeApi = GAME_MODE_CODES[selectedIndex];
        int timeLimit = sbTimeLimit.getProgress() + 30;
        int maxPlayers = sbMaxPlayers.getProgress() + 4;
        int minPlayers = Math.min(sbMinPlayers.getProgress() + 2, maxPlayers);
        int faultLimit = sbFaultLimit.getProgress() + 3;
        int helpLimit = sbHelpLimit.getProgress() + 2;

        RadioButton selectedType = findViewById(rgGameType.getCheckedRadioButtonId());
        String gameType = selectedType != null && selectedType.getId() == R.id.rb_competitive ? "COMPETITIVE" : "ENTERTAINMENT";

        Intent intent = new Intent(this, KeywordInputActivity.class);
        intent.putExtra("gameMode", gameMode);
        intent.putExtra("gameModeApi", gameModeApi);
        intent.putExtra("battleType", "multi");
        intent.putExtra("timeLimit", timeLimit);
        intent.putExtra("maxPlayers", maxPlayers);
        intent.putExtra("minPlayers", minPlayers);
        intent.putExtra("faultLimit", faultLimit);
        intent.putExtra("helpLimit", helpLimit);
        intent.putExtra("gameType", gameType);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    private static class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        private final TextView textView;
        private final String suffix;
        private final int offset;

        SeekBarListener(TextView tv, String suffix, int offset) {
            this.textView = tv;
            this.suffix = suffix;
            this.offset = offset;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            textView.setText(String.valueOf(progress + offset) + suffix);
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
