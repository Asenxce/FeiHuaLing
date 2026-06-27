package com.example.feihualinggame.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.feihualinggame.R;
import com.example.feihualinggame.activity.GameModeActivity;
import com.example.feihualinggame.activity.MultiRoomMenuActivity;
import com.example.feihualinggame.activity.RuleEngineConfigActivity;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.FeedbackManager;
import com.example.feihualinggame.utils.SharedPrefsUtil;

/**
 * 主页Fragment - 显示游戏入口
 */
public class HomeFragment extends Fragment {
    private LinearLayout btnStartGame;      // 人机对战
    private LinearLayout btnCustomMode;     // 自定义模式
    private LinearLayout btnMultiBattle;    // 多人约战
    private TextView tvGreeting;
    private TextView tvUsername;
    private static boolean hasGreetedThisSession = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化控件
        btnStartGame = view.findViewById(R.id.btn_start_game);
        btnCustomMode = view.findViewById(R.id.btn_custom_mode);
        btnMultiBattle = view.findViewById(R.id.btn_multi_battle);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvUsername = view.findViewById(R.id.tv_username);

        // 为按钮添加动效
        ButtonAnimationHelper.addCombinedEffect(btnStartGame);
        ButtonAnimationHelper.addCombinedEffect(btnCustomMode);
        ButtonAnimationHelper.addCombinedEffect(btnMultiBattle);

        // 加载用户信息和统计数据
        loadUserInfo();

        // 开始游戏（人机对战）
        btnStartGame.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            Intent intent = new Intent(getActivity(), GameModeActivity.class);
            intent.putExtra("battleType", "ai");
            startActivity(intent);
        });

        // 自定义模式
        btnCustomMode.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            Intent intent = new Intent(getActivity(), RuleEngineConfigActivity.class);
            startActivity(intent);
        });

        btnMultiBattle.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            Intent intent = new Intent(getActivity(), MultiRoomMenuActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次显示时刷新数据
        loadUserInfo();
    }

    /**
     * 加载用户信息（问候语和昵称）
     */
    private void loadUserInfo() {
        updateGreeting();
        
        String nickname = SharedPrefsUtil.getString(requireContext(), "nickname");
        if (tvUsername != null) {
            tvUsername.setText(nickname != null && !nickname.isEmpty() ? nickname : "飞花令玩家");
        }

        if (!hasGreetedThisSession && getActivity() != null) {
            hasGreetedThisSession = true;
            String name = nickname != null && !nickname.isEmpty() ? nickname : "玩家";
            FeedbackManager.getInstance().speakWelcome(getActivity(), name);
        }
    }

    /**
     * 更新问候语（根据当前时间）
     */
    private void updateGreeting() {
        if (tvGreeting == null) return;
        
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "早上好";
        } else if (hour >= 12 && hour < 14) {
            greeting = "中午好";
        } else if (hour >= 14 && hour < 18) {
            greeting = "下午好";
        } else {
            greeting = "晚上好";
        }
        
        tvGreeting.setText(greeting);
    }
}
