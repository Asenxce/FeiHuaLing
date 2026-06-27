package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.FeihuaLingGameApplication;
import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.GameMode;
import com.example.feihualinggame.activity.MainActivity;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends AppCompatActivity {
    private TextView tvHeaderLabel;
    private TextView tvResultTitle;
    private TextView tvTotalScore;
    private TextView tvInfoMode;
    private TextView tvInfoKeyword;
    private TextView tvInfoRounds;
    private TextView tvInfoTime;
    private TextView tvDetailTitle;
    private LinearLayout llQuestionDetails;
    private Button btnRetry;
    private Button btnBackToHome;

    private String gameMode;
    private String battleType;
    private String keyword;
    private String keyword2;
    private int keywordPosition;
    private int timeLimit;
    private boolean isWin;
    private boolean isMulti;
    private long myUserId;
    private String multiBattleResultJson;
    private List<ScoreItem> aiScoreItems;

    private static class ScoreItem {
        int roundNum;
        long timeSpent;
        int baseScore;
        int speedBonus;
        int totalScore;
    }

    private static class PlayerRank {
        long userId;
        String nickname;
        int rank;
        int correctCount;
        int wrongCount;
        int totalCount;
        double accuracy;
        double avgTimeUsed;
        int fastestTime;
        int score;
        boolean isEliminated;
        Integer eliminationRound;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.surface));
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        gameMode = getIntent().getStringExtra("gameMode");
        battleType = getIntent().getStringExtra("battleType");
        keyword = getIntent().getStringExtra("keyword");
        keyword2 = getIntent().getStringExtra("keyword2");
        keywordPosition = getIntent().getIntExtra("keywordPosition", 0);
        timeLimit = getIntent().getIntExtra("timeLimit", 30);
        isWin = getIntent().getBooleanExtra("isWin", false);
        isMulti = getIntent().getBooleanExtra("isMulti", false);
        myUserId = getIntent().getLongExtra("myUserId", 0);
        multiBattleResultJson = getIntent().getStringExtra("multiBattleResult");

        String questionScoresJson = getIntent().getStringExtra("questionScores");
        aiScoreItems = new ArrayList<>();
        if (questionScoresJson != null && !questionScoresJson.isEmpty()) {
            try {
                JsonArray array = new Gson().fromJson(questionScoresJson, JsonArray.class);
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    ScoreItem si = new ScoreItem();
                    si.roundNum = i + 1;
                    si.timeSpent = obj.get("timeSpent").getAsLong();
                    si.baseScore = obj.get("baseScore").getAsInt();
                    si.speedBonus = obj.get("speedBonus").getAsInt();
                    si.totalScore = obj.get("totalScore").getAsInt();
                    aiScoreItems.add(si);
                }
            } catch (Exception ignored) {}
        }

        if (gameMode == null || gameMode.isEmpty()) gameMode = "single_keyword";
        if (battleType == null || battleType.isEmpty()) battleType = "ai";

        initViews();
        setResult();

        btnRetry.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            if (isMulti) {
                goToHome();
            } else {
                Intent intent = new Intent(this, GameModeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnBackToHome.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            goToHome();
        });

        AudioController.getInstance().init(FeihuaLingGameApplication.getInstance());
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
        SystemUIUtil.hideNavigationBarIndicator(this);
    }

    private void initViews() {
        tvHeaderLabel = findViewById(R.id.tv_header_label);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        tvInfoMode = findViewById(R.id.tv_info_mode);
        tvInfoKeyword = findViewById(R.id.tv_info_keyword);
        tvInfoRounds = findViewById(R.id.tv_info_rounds);
        tvInfoTime = findViewById(R.id.tv_info_time);
        tvDetailTitle = findViewById(R.id.tv_detail_title);
        llQuestionDetails = findViewById(R.id.llQuestionDetails);
        btnRetry = findViewById(R.id.btn_retry);
        btnBackToHome = findViewById(R.id.btnBackToHome);

        ButtonAnimationHelper.addCombinedEffect(btnRetry);
        ButtonAnimationHelper.addCombinedEffect(btnBackToHome);
    }

    private void setResult() {
        if (isMulti && multiBattleResultJson != null && !multiBattleResultJson.isEmpty()) {
            setMultiResult();
        } else if ("custom".equals(gameMode)) {
            setCustomResult();
        } else {
            setAiResult();
        }
    }

    // ---------- 人机对战结算 ----------
    private void setAiResult() {
        tvHeaderLabel.setText("训练模式 · 结算");

        tvResultTitle.setText("训练完成");
        tvResultTitle.setTextColor(getColor(R.color.gold));

        int totalScore = calculateAiTotalScore();
        tvTotalScore.setText((totalScore >= 0 ? "+" : "") + totalScore);
        tvTotalScore.setTextColor(totalScore >= 0 ? getColor(R.color.win_green) : getColor(R.color.loss_red));
        animateScore();

        GameMode modeEnum = GameMode.fromCode(gameMode);
        tvInfoMode.setText("模式：" + modeEnum.getDisplayName());
        tvInfoKeyword.setText(buildKeywordDisplay());
        tvInfoRounds.setText("回合：" + aiScoreItems.size() + " 回合");
        tvInfoTime.setText("限时：" + timeLimit + "秒/题");

        tvDetailTitle.setText("每题详情");
        buildAiScoreDetails(totalScore);
    }

    // ---------- 自定义模式结算 ----------
    private void setCustomResult() {
        tvHeaderLabel.setText("自定义模式 · 结算");

        if (isWin) {
            tvResultTitle.setText("通过");
            tvResultTitle.setTextColor(getColor(R.color.gold));
        } else {
            tvResultTitle.setText("未能通过");
            tvResultTitle.setTextColor(getColor(R.color.red_accent));
        }

        int totalScore = calculateAiTotalScore();
        tvTotalScore.setText((totalScore >= 0 ? "+" : "") + totalScore);
        tvTotalScore.setTextColor(totalScore >= 0 ? getColor(R.color.win_green) : getColor(R.color.loss_red));
        animateScore();

        tvInfoMode.setText("模式：自定义飞花令");
        if (keyword != null && !keyword.isEmpty()) {
            tvInfoKeyword.setText("关键字：「" + keyword + "」");
        } else {
            tvInfoKeyword.setVisibility(View.GONE);
        }
        tvInfoRounds.setText("回合：" + aiScoreItems.size() + " 回合");
        tvInfoTime.setText("限时：" + timeLimit + "秒/题");

        tvDetailTitle.setText("每题详情");
        buildAiScoreDetails(totalScore);
    }

    // ---------- 多人对战结算 ----------
    private void setMultiResult() {
        tvHeaderLabel.setText("多人对战 · 结算");

        try {
            JsonObject json = new Gson().fromJson(multiBattleResultJson, JsonObject.class);
            int totalRounds = json.has("totalRounds") ? json.get("totalRounds").getAsInt() : 0;
            long duration = json.has("duration") ? json.get("duration").getAsLong() : 0;

            List<PlayerRank> playerRanks = new ArrayList<>();
            JsonArray players = json.getAsJsonArray("players");
            for (JsonElement elem : players) {
                JsonObject p = elem.getAsJsonObject();
                PlayerRank pr = new PlayerRank();
                pr.userId = p.get("userId").getAsLong();
                pr.nickname = p.has("nickname") ? p.get("nickname").getAsString() : "未知";
                pr.rank = p.get("rank").getAsInt();
                pr.correctCount = p.has("correctCount") ? p.get("correctCount").getAsInt() : 0;
                pr.wrongCount = p.has("wrongCount") ? p.get("wrongCount").getAsInt() : 0;
                pr.totalCount = p.has("totalCount") ? p.get("totalCount").getAsInt() : 0;
                pr.accuracy = p.has("accuracy") ? p.get("accuracy").getAsDouble() : 0;
                pr.avgTimeUsed = p.has("avgTimeUsed") ? p.get("avgTimeUsed").getAsDouble() : 0;
                pr.fastestTime = p.has("fastestTime") ? p.get("fastestTime").getAsInt() : 0;
                pr.score = p.has("score") ? p.get("score").getAsInt() : 0;
                pr.isEliminated = p.has("isEliminated") && p.get("isEliminated").getAsBoolean();
                pr.eliminationRound = p.has("eliminationRound") && !p.get("eliminationRound").isJsonNull()
                        ? p.get("eliminationRound").getAsInt() : null;
                playerRanks.add(pr);
            }

            PlayerRank myRank = null;
            for (PlayerRank pr : playerRanks) {
                if (pr.userId == myUserId) { myRank = pr; break; }
            }
            if (myRank == null) {
                myRank = new PlayerRank();
                myRank.nickname = "你";
                myRank.rank = playerRanks.size() + 1;
                myRank.score = 0;
            }

            if (myRank.rank == 1) {
                tvResultTitle.setText("冠军");
                tvResultTitle.setTextColor(getColor(R.color.gold));
            } else {
                tvResultTitle.setText("第 " + myRank.rank + " 名");
                tvResultTitle.setTextColor(getColor(R.color.poetry_text_primary));
            }

            int score = myRank.score;
            tvTotalScore.setText((score >= 0 ? "+" : "") + score);
            tvTotalScore.setTextColor(score >= 0 ? getColor(R.color.win_green) : getColor(R.color.loss_red));
            animateScore();

            tvInfoMode.setText("排名：" + playerRanks.size() + " 人参与");
            tvInfoKeyword.setText("回合：" + totalRounds + " 回合");
            tvInfoRounds.setText("存活：" + (myRank.isEliminated ? "被淘汰(第" + myRank.eliminationRound + "回合)" : "存活到最后"));
            tvInfoTime.setText("耗时：" + formatDuration(duration));
            tvDetailTitle.setText("玩家排名");

            buildMultiRankContent(playerRanks);
        } catch (Exception e) {
            tvResultTitle.setText("结算异常");
            tvInfoMode.setText("请返回重试");
        }
    }

    // ---------- 积分计算 ----------
    private int calculateAiTotalScore() {
        int sum = 0;
        for (ScoreItem si : aiScoreItems) {
            sum += si.totalScore;
        }
        double multiplier = 1.0;
        switch (gameMode) {
            case "position":
            case "double_keyword":
                multiplier = 1.2;
                break;
            case "chain":
            case "forbidden":
            case "custom":
            case "color":
            case "number":
                multiplier = 1.5;
                break;
        }
        return (int) Math.ceil(sum * multiplier);
    }

    // ---------- 构建显示 ----------
    private String buildKeywordDisplay() {
        if (keyword == null || keyword.isEmpty()) return "关键字：未指定";
        switch (gameMode) {
            case "single_keyword":
                return "关键字：「" + keyword + "」";
            case "position":
                return "关键字：「" + keyword + "」 · 第" + keywordPosition + "位";
            case "double_keyword":
                String k2 = (keyword2 != null && !keyword2.isEmpty()) ? keyword2 : "?";
                return "关键字：「" + keyword + "」+「" + k2 + "」";
            case "chain":
                return "模式：首尾接龙";
            case "forbidden":
                return "禁用字：「" + keyword + "」";
            case "color":
                return "颜色字：「" + keyword + "」";
            case "number":
                return "数字字：「" + keyword + "」";
            default:
                return "关键字：「" + keyword + "」";
        }
    }

    private String getModeDisplayName() {
        GameMode m = GameMode.fromCode(gameMode);
        return m.getDisplayName();
    }

    // ---------- AI 每题详情 ----------
    private void buildAiScoreDetails(int totalScore) {
        llQuestionDetails.removeAllViews();

        for (ScoreItem si : aiScoreItems) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackground(getDrawable(R.drawable.card_white));
            card.setElevation(2f);
            card.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(cp);

            TextView tvRound = new TextView(this);
            tvRound.setText("第" + si.roundNum + "题");
            tvRound.setTextSize(14);
            tvRound.setTextColor(getColor(R.color.gray_text));
            tvRound.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f));

            TextView tvTime = new TextView(this);
            tvTime.setText(si.timeSpent + "秒");
            tvTime.setTextSize(14);
            tvTime.setTextColor(getColor(R.color.poetry_text_secondary));
            tvTime.setGravity(Gravity.CENTER);
            tvTime.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));

            TextView tvScore = new TextView(this);
            tvScore.setText("+" + si.totalScore);
            tvScore.setTextSize(16);
            tvScore.setTextColor(getColor(R.color.gold));
            tvScore.setTypeface(null, android.graphics.Typeface.BOLD);
            tvScore.setGravity(Gravity.END);
            tvScore.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f));

            card.addView(tvRound);
            card.addView(tvTime);
            card.addView(tvScore);
            llQuestionDetails.addView(card);
        }

        LinearLayout modeCard = new LinearLayout(this);
        modeCard.setOrientation(LinearLayout.HORIZONTAL);
        modeCard.setBackground(getDrawable(R.drawable.card_white));
        modeCard.setElevation(2f);
        modeCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams mcp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mcp.setMargins(0, dp(4), 0, 0);
        modeCard.setLayoutParams(mcp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("模式倍率");
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(getColor(R.color.gray_text));
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));

        TextView tvMultiplier = new TextView(this);
        double multiplier = 1.0;
        switch (gameMode) {
            case "position": case "double_keyword": multiplier = 1.2; break;
            case "chain": case "forbidden": case "custom": case "color": case "number": multiplier = 1.5; break;
        }
        tvMultiplier.setText(String.format("x%.1f", multiplier));
        tvMultiplier.setTextSize(14);
        tvMultiplier.setTextColor(getColor(R.color.poetry_text_secondary));
        tvMultiplier.setGravity(Gravity.END);
        tvMultiplier.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.15f));

        TextView tvFinal = new TextView(this);
        tvFinal.setText("=" + totalScore);
        tvFinal.setTextSize(16);
        tvFinal.setTextColor(getColor(R.color.gold));
        tvFinal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvFinal.setGravity(Gravity.END);
        tvFinal.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));

        modeCard.addView(tvLabel);
        modeCard.addView(tvMultiplier);
        modeCard.addView(tvFinal);
        llQuestionDetails.addView(modeCard);
    }

    // ---------- 多人排名 ----------
    private void buildMultiRankContent(List<PlayerRank> playerRanks) {
        llQuestionDetails.removeAllViews();

        for (PlayerRank pr : playerRanks) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(getDrawable(R.drawable.card_white));
            card.setElevation(2f);
            card.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(cp);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvRank = new TextView(this);
            String rankText;
            int rankColor;
            switch (pr.rank) {
                case 1: rankText = "#1"; rankColor = getColor(R.color.gold); break;
                case 2: rankText = "#2"; rankColor = getColor(R.color.poetry_text_secondary); break;
                case 3: rankText = "#3"; rankColor = getColor(R.color.brown_medium); break;
                default: rankText = String.valueOf(pr.rank); rankColor = getColor(R.color.gray_text); break;
            }
            tvRank.setText(rankText);
            tvRank.setTextSize(20);
            tvRank.setTextColor(rankColor);
            tvRank.setTypeface(null, android.graphics.Typeface.BOLD);
            tvRank.setLayoutParams(new LinearLayout.LayoutParams(dp(40), LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvName = new TextView(this);
            tvName.setText(pr.nickname + (pr.userId == myUserId ? " (我)" : ""));
            tvName.setTextSize(15);
            tvName.setTextColor(pr.userId == myUserId ? getColor(R.color.poetry_primary) : getColor(R.color.poetry_text_primary));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            topRow.addView(tvRank);
            topRow.addView(tvName);
            card.addView(topRow);

            String detail = pr.score + "分"
                    + "  |  " + pr.correctCount + "正" + pr.wrongCount + "误"
                    + "  |  准确率 " + ((int) pr.accuracy) + "%";
            if (pr.avgTimeUsed > 0) {
                detail += "  |  平均" + ((int) pr.avgTimeUsed) + "秒";
            }

            TextView tvDetail = new TextView(this);
            tvDetail.setText(detail);
            tvDetail.setTextSize(13);
            tvDetail.setTextColor(getColor(R.color.poetry_text_secondary));
            tvDetail.setPadding(0, dp(4), 0, 0);
            tvDetail.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            card.addView(tvDetail);

            if (pr.isEliminated && pr.eliminationRound != null) {
                TextView tvElim = new TextView(this);
                tvElim.setText("[淘汰] 第 " + pr.eliminationRound + " 回合被淘汰");
                tvElim.setTextSize(12);
                tvElim.setTextColor(getColor(R.color.red_accent));
                tvElim.setPadding(0, dp(2), 0, 0);
                card.addView(tvElim);
            }

            llQuestionDetails.addView(card);
        }
    }

    private void animateScore() {
        tvTotalScore.setScaleX(0.5f);
        tvTotalScore.setScaleY(0.5f);
        tvTotalScore.setAlpha(0f);
        tvTotalScore.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(800).start();
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "秒";
        long m = seconds / 60;
        long s = seconds % 60;
        return m + "分" + s + "秒";
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPrefsUtil.saveLastKeyword(this, null);
    }

    @Override
    public void onBackPressed() {
        AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
        goToHome();
    }

    private void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
