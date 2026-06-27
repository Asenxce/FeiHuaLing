package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.Battle;
import com.example.feihualinggame.bean.GameMode;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.FeedbackManager;
import com.example.feihualinggame.utils.BattlePoetryCacheManager;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.example.feihualinggame.utils.TimeCountUtil;
import com.example.feihualinggame.validator.*;
import com.example.feihualinggame.validator.engine.RuleContext;
import com.example.feihualinggame.validator.engine.RuleEngine;
import com.example.feihualinggame.validator.engine.rule.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 对战页面（核心对战逻辑）
 * 新逻辑：前端验证格式 + 后端诗词搜索
 */
public class BattleActivity extends AppCompatActivity {
    private static final String TAG = "BattleActivity";
    private TextView tvBattleTitle;         // 标题
    private TextView tvPoetryContent;       // 诗句显示
    private EditText etAnswer;              // 答案输入
    private Button btnSubmit;               // 提交按钮
    private LinearLayout btnVoiceInput;     // 语音输入按钮
    private TextView tvGameState;           // 游戏状态
    private TextView tvTimer;               // 倒计时显示
    private TextView tvRuleHint;            // 规则提示
    private LinearLayout llAiAnswerContainer; // AI 对句容器
    private TextView tvAiAnswer;            // AI 对句内容
    
    private String gameMode;                // 游戏模式
    private String keyword;                 // 关键字
    private String keyword2;                // 第二个关键字（双字模式）
    private String colorKeyword;            // 颜色关键字
    private String numberKeyword;           // 数字关键字
    private String forbiddenWord;           // 禁止词
    private String battleType;              // 对战类型（ai/friend）
    private int timeLimit;                  // 限时时间
    private int keywordPosition;            // 关键字位置
    private int customMaxLength = 12;       // 自定义模式最大字数限制
    
    private Battle battle;                  // 对战信息
    private TimeCountUtil timer;            // 倒计时器
    private long remainingTime = 0;         // 记录暂停时的剩余时间（毫秒）
    private boolean isMyTurn = true;        // 是否我的回合
    private int repeatCount = 0;            // 重复提示次数
    private String battleId;                // 对战ID（用于好友对战）
    private int correctCount = 0;           // 玩家正确答对次数
    private int wrongCount = 0;             // 玩家错误回答次数
    private long battleStartTime = 0;       // 对战开始时间
    private long questionStartTime = 0;     // 当前题开始时间（玩家回合开始时）
    private List<QuestionScore> questionScores = new ArrayList<>(); // 每题得分详情
    
    // 规则引擎
    private RuleEngine ruleEngine;
    private ChainFeiHuaLing chainValidator;
    private RuleContext ruleCtx;
    
    private ActivityResultLauncher<Intent> voiceInputLauncher;
    private AlertDialog exitDialog;
    private final java.util.concurrent.ExecutorService executor = Executors.newCachedThreadPool();
    
    // 好友对战轮询
    private android.os.Handler pollHandler = new android.os.Handler();
    private Runnable pollRunnable;
    private boolean isPolling = false;
    private boolean isGameEnded = false;

    /**
     * 每题得分详情
     */
    private static class QuestionScore {
        long timeSpent;      // 本题用时（秒）
        int baseScore;       // 基础分
        int speedBonus;      // 速度加分
        int totalScore;      // 本题得分（含倍率）
        
        QuestionScore(long timeSpent, int baseScore, int speedBonus, int totalScore) {
            this.timeSpent = timeSpent;
            this.baseScore = baseScore;
            this.speedBonus = speedBonus;
            this.totalScore = totalScore;
        }
    }

    /**
     * 计算本题得分
     */
    private QuestionScore calculateQuestionScore(long timeSpent, String gameMode) {
        int baseScore = 10; // 每题基础分10分
        
        // 速度倍率
        double speedMultiplier;
        if (timeSpent <= 15) {
            speedMultiplier = 2.0;
        } else {
            speedMultiplier = 1.5;
        }
        
        // 模式倍率
        double modeMultiplier = 1.0;
        switch (gameMode) {
            case "position":
            case "double_keyword":
                modeMultiplier = 1.2;
                break;
            case "chain":
            case "forbidden":
            case "color":
            case "number":
                modeMultiplier = 1.5;
                break;
        }
        
        // 单题得分 = 基础分 × 速度倍率
        int questionScore = (int) Math.ceil(baseScore * speedMultiplier);
        
        return new QuestionScore(timeSpent, baseScore, (int)((speedMultiplier - 1.0) * baseScore), questionScore);
    }

    // ==================== 对局内通知系统 ====================
    
    private LinearLayout notificationBanner;
    private TextView tvNotificationIcon;
    private TextView tvNotificationText;
    private android.os.Handler notificationHandler = new android.os.Handler();
    private Runnable notificationDismissRunnable;
    private boolean isNotificationShowing = false;

    /**
     * 通知类型枚举
     */
    private static final int NOTIF_SUCCESS = 0;
    private static final int NOTIF_ERROR = 1;
    private static final int NOTIF_INFO = 2;
    private static final int NOTIF_WARNING = 3;

    /**
     * 显示对局内通知（替代Toast）
     * @param type 通知类型: NOTIF_SUCCESS / NOTIF_ERROR / NOTIF_INFO / NOTIF_WARNING
     * @param message 通知内容
     * @param duration 持续时间（毫秒），0表示不自动消失
     */
    private void showNotification(int type, String message, long duration) {
        if (notificationBanner == null) return;
        
        runOnUiThread(() -> {
            // 取消之前的自动消失
            if (notificationDismissRunnable != null) {
                notificationHandler.removeCallbacks(notificationDismissRunnable);
            }

            // 设置图标和背景
            String icon;
            int bgRes;
            switch (type) {
                case NOTIF_SUCCESS:
                    icon = "✓";
                    bgRes = R.drawable.battle_notification_success;
                    tvNotificationIcon.setTextColor(getResources().getColor(R.color.success));
                    break;
                case NOTIF_ERROR:
                    icon = "✗";
                    bgRes = R.drawable.battle_notification_error;
                    tvNotificationIcon.setTextColor(getResources().getColor(R.color.error));
                    break;
                case NOTIF_WARNING:
                    icon = "!";
                    bgRes = R.drawable.battle_notification_warning;
                    tvNotificationIcon.setTextColor(getResources().getColor(R.color.warning));
                    break;
                case NOTIF_INFO:
                default:
                    icon = "i";
                    bgRes = R.drawable.battle_notification_info;
                    tvNotificationIcon.setTextColor(getResources().getColor(R.color.gold));
                    break;
            }

            tvNotificationIcon.setText(icon);
            tvNotificationText.setText(message);
            notificationBanner.setBackgroundResource(bgRes);

            // 显示动画
            if (notificationBanner.getVisibility() != View.VISIBLE) {
                notificationBanner.setVisibility(View.VISIBLE);
                notificationBanner.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_notification));
                isNotificationShowing = true;
            }

            // 自动消失
            if (duration > 0) {
                notificationDismissRunnable = () -> hideNotification();
                notificationHandler.postDelayed(notificationDismissRunnable, duration);
            }
        });
    }

    /**
     * 隐藏通知
     */
    private void hideNotification() {
        if (notificationBanner == null) return;
        
        runOnUiThread(() -> {
            if (notificationBanner.getVisibility() == View.VISIBLE) {
                android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_out_notification);
                anim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(android.view.animation.Animation animation) {}
                    @Override
                    public void onAnimationRepeat(android.view.animation.Animation animation) {}
                    @Override
                    public void onAnimationEnd(android.view.animation.Animation animation) {
                        notificationBanner.setVisibility(View.GONE);
                        isNotificationShowing = false;
                    }
                });
                notificationBanner.startAnimation(anim);
            }
        });
    }

    /**
     * 显示本题得分提示（简短通知，不遮挡屏幕）
     */
    private void showQuestionScoreDialog(QuestionScore score) {
        String message = String.format("+%d分（用时%d秒）", score.totalScore, score.timeSpent);
        showNotification(NOTIF_SUCCESS, message, 2500);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        SystemUIUtil.setupImmersiveStatusBar(this, R.color.poetry_primary, false);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 获取传入的参数
        gameMode = getIntent().getStringExtra("gameMode");
        keyword = getIntent().getStringExtra("keyword");
        keyword2 = getIntent().getStringExtra("keyword2");
        colorKeyword = getIntent().getStringExtra("colorKeyword");
        numberKeyword = getIntent().getStringExtra("numberKeyword");
        forbiddenWord = getIntent().getStringExtra("forbiddenWord");
        battleType = getIntent().getStringExtra("battleType");
        battleId = getIntent().getStringExtra("battleId");
        timeLimit = getIntent().getIntExtra("timeLimit", 30);  // 从 Intent 获取时间限制，默认30秒
        
        // 如果是位置飞花令，获取用户选择的位置
        if ("position".equals(gameMode)) {
            keywordPosition = getIntent().getIntExtra("keywordPosition", 3);  // 默认第3位
        }
        
        // 如果是自定义模式，获取用户设置的最大字数
        if ("custom".equals(gameMode)) {
            customMaxLength = getIntent().getIntExtra("maxLength", 12);
        }
        
        BattlePoetryCacheManager.clearUsedPoems();

        // 参数验证和默认值处理
        if (gameMode == null || gameMode.isEmpty()) {
            gameMode = "single_keyword"; // 默认单关键字模式
        }
        if (keyword == null) {
            keyword = "";
        }
        if (battleType == null || battleType.isEmpty()) {
            battleType = "ai"; // 默认为人机对战
        }

        // 初始化控件
        tvBattleTitle = findViewById(R.id.tv_battle_title);
        tvPoetryContent = findViewById(R.id.tv_poetry_content);
        etAnswer = findViewById(R.id.et_answer);
        btnSubmit = findViewById(R.id.btn_submit);
        btnVoiceInput = findViewById(R.id.btn_voice_input);
        tvGameState = findViewById(R.id.tv_game_state);
        tvTimer = findViewById(R.id.tv_timer);
        tvRuleHint = findViewById(R.id.tv_rule_hint);
        llAiAnswerContainer = findViewById(R.id.ll_ai_answer_container);
        tvAiAnswer = findViewById(R.id.tv_ai_answer);
        TextView tvKeywordTag = findViewById(R.id.tv_keyword_tag);
        ImageView btnMusicVolume = findViewById(R.id.btn_music_volume);
        
        // 初始化通知栏
        notificationBanner = findViewById(R.id.notification_banner);
        tvNotificationIcon = findViewById(R.id.tv_notification_icon);
        tvNotificationText = findViewById(R.id.tv_notification_text);

        // 记录对战开始时间
        battleStartTime = System.currentTimeMillis();
        questionStartTime = battleStartTime; // 第一题开始时间

        // 如果没有battleId，先在后端创建对战记录（用于结算和战绩）
        if (battleId == null || battleId.isEmpty()) {
            createBattleOnServer();
        }

        // 初始化对战信息
        battle = new Battle();
        battle.setGameMode(gameMode);
        battle.setKeyword(keyword);
        battle.setKeyword2(keyword2);

        // 设置标题
        GameMode mode = GameMode.fromCode(gameMode);
        tvBattleTitle.setText(battleType != null && battleType.equals("ai") ? "人机对战 - " + mode.getDisplayName() : "好友对战 - " + mode.getDisplayName());
        
        // 设置右上角关键字标签
        updateKeywordTag(tvKeywordTag);

        // 根据游戏模式初始化验证器
        initValidator();

        // 初始化游戏
        initGame();

        // 提交按钮点击事件
        btnSubmit.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            submitAnswer();
        });

        // 语音输入按钮
        btnVoiceInput.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            startVoiceInput();
        });
        
        // 为按钮添加动效
        ButtonAnimationHelper.addCombinedEffect(btnSubmit);
        ButtonAnimationHelper.addCombinedEffect(btnVoiceInput);
        
        // 音乐音量按钮点击事件
        btnMusicVolume.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showVolumeDialog();
        });
        
        // 注册语音输入 launcher
        voiceInputLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && results.size() > 0) {
                        etAnswer.setText(results.get(0));
                    }
                }
            }
        );
        
        // 为页面元素添加入场动画（级联效果）
        setupEntryAnimations();
        
        // 切换到对战背景音乐
        AudioController.getInstance().playBGM(AudioController.SCENE_BATTLE);
    }

    /**
     * 根据游戏模式初始化验证器
     */
    private void initValidator() {
        if (keyword == null) {
            keyword = "";
        }

        ruleCtx = new RuleContext(gameMode, keyword)
                .setKeyword2(keyword2)
                .setKeywordPosition(keywordPosition);

        switch (gameMode) {
            case "single_keyword":
                tvRuleHint.setText("[规则] 说出包含「" + keyword + "」字的诗句\n[注意] 不能重复使用已说过的诗句");
                break;
            case "position":
                if (keywordPosition <= 0) {
                    keywordPosition = (int) (Math.random() * 7) + 1;
                }
                battle.setKeywordPosition(keywordPosition);
                ruleCtx.setKeywordPosition(keywordPosition);
                tvRuleHint.setText("[规则] 说出「" + keyword + "」字在第" + keywordPosition + "位的诗句\n[注意] 关键字位置必须准确");
                break;
            case "double_keyword":
                if (keyword2 == null || keyword2.isEmpty()) {
                    showNotification(NOTIF_ERROR, "双关键字飞花令需要两个关键字", 3000);
                    finish();
                    return;
                }
                tvRuleHint.setText("[规则] 说出同时包含「" + keyword + "」和「" + keyword2 + "」的诗句\n[注意] 两个字都要有");
                break;
            case "chain":
                chainValidator = new ChainFeiHuaLing(new ChainFeiHuaLing.OnInitialPoemListener() {
                    @Override
                    public void onInitialPoemReceived(String poem) {
                        runOnUiThread(() -> {
                            tvPoetryContent.setText("起始句：" + poem);
                            Character nextChar = PoetryCleanUtil.extractLastChar(poem);
                            if (nextChar != null) {
                                keyword = String.valueOf(nextChar);
                                ruleCtx.setKeyword(keyword);
                                ruleCtx.setLastChar(nextChar);
                                updateKeywordTag(findViewById(R.id.tv_keyword_tag));
                                tvRuleHint.setText("[规则] 首尾接龙飞花令\n[目标] 你的首字必须是：「" + nextChar + "」\n[注意] 不能重复使用已说过的诗句");
                            }
                            tvGameState.setText("轮到你了");
                            startTimer();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showNotification(NOTIF_ERROR, error, 3500);
                            endGame(false);
                        });
                    }
                });

                if (keyword != null && !keyword.isEmpty()) {
                    char startChar = keyword.charAt(0);
                    chainValidator.setStartChar(startChar);
                    ruleCtx.setLastChar(startChar);
                    tvRuleHint.setText("[规则] 首尾接龙飞花令\n[目标] 你的首字必须是：「" + startChar + "」\n[注意] 不能重复使用已说过的诗句");
                    tvPoetryContent.setText(getPromptText());
                    tvGameState.setText("轮到你了");
                } else {
                    tvRuleHint.setText("[规则] 首尾接龙飞花令\n[等待] 等待系统给出起始句...");
                }
                return;
            case "forbidden":
                tvRuleHint.setText("[规则] 说出【不包含】「" + keyword + "」字的诗句\n[注意] 禁用字绝对不能出现");
                break;
            case "color":
                tvRuleHint.setText("[规则] 说出包含「" + keyword + "」字的诗句\n[颜色] 关键字来自颜色字典");
                break;
            case "number":
                tvRuleHint.setText("[规则] 说出包含「" + keyword + "」字的诗句\n[数字] 关键字来自数字字典");
                break;
            case "custom":
                tvRuleHint.setText("[自定义] 飞花令\n[注意] 不能重复使用已说过的诗句");
                buildCustomEngine(ruleCtx);
                return;
            default:
                tvRuleHint.setText("[规则] 说出包含「" + keyword + "」字的诗句\n[注意] 不能重复使用已说过的诗句");
                break;
        }

        ruleEngine = new RuleEngine(this);
        ruleEngine.registerFromConfig(getApplicationContext(), ruleCtx);
    }

    private void buildCustomEngine(RuleContext ctx) {
        String customRules = getIntent().getStringExtra("customRules");
        if (customRules == null || customRules.isEmpty()) {
            ruleEngine = new RuleEngine(this);
            ruleEngine.registerFromConfig(getApplicationContext(), ctx);
            return;
        }

        String[] ruleKeys = customRules.split(",");
        List<AtomicRule> rules = new ArrayList<>();
        rules.add(new MinLengthRule(4));

        StringBuilder hint = new StringBuilder("[自定义] 飞花令规则：");
        for (String key : ruleKeys) {
            key = key.trim();
            switch (key) {
                case "contains":
                    if (!keyword.isEmpty()) rules.add(new ContainsRule(keyword));
                    if (keyword2 != null && !keyword2.isEmpty()) rules.add(new ContainsRule(keyword2));
                    hint.append("\n• 必须包含关键字「").append(keyword).append("」");
                    if (keyword2 != null && !keyword2.isEmpty()) {
                        hint.append("和「").append(keyword2).append("」");
                    }
                    break;
                case "notContains":
                    if (!keyword.isEmpty()) rules.add(new NotContainsRule(keyword));
                    hint.append("\n• 不能包含关键字「").append(keyword).append("」");
                    break;
                case "position":
                    if (!keyword.isEmpty()) rules.add(new PositionRule(keyword, keywordPosition > 0 ? keywordPosition : 1));
                    hint.append("\n• 关键字「").append(keyword).append("」固定在第").append(keywordPosition).append("位");
                    break;
                case "chainStart":
                    rules.add(new ChainStartRule());
                    if (!keyword.isEmpty()) ctx.setLastChar(keyword.charAt(0));
                    hint.append("\n• 首尾接龙（首字接上一句）");
                    break;
                case "noRepeat":
                    rules.add(new NoRepeatRule());
                    hint.append("\n• 不能重复使用诗句");
                    break;
                case "maxLength":
                    rules.add(new MaxLengthRule(customMaxLength));
                    hint.append("\n• 诗句长度不超过").append(customMaxLength).append("字");
                    break;
                case "charCountMatch":
                    rules.add(new CharCountMatchRule());
                    hint.append("\n• 字数必须与上一句相同");
                    break;
            }
        }

        tvRuleHint.setText(hint.toString());

        ruleEngine = new RuleEngine(this);
        ruleEngine.registerFromConfig(getApplicationContext(), ctx);
    }

    /**
     * 初始化游戏
     */
    private void initGame() {
        if ("ai".equals(battleType)) {
            // 人机对战：系统先出题
            tvGameState.setText("系统出题中...");
            tvPoetryContent.setText("等待系统出题...");
            
            if ("chain".equals(gameMode)) {
                // 首尾接龙模式：检查是否有关键字（玩家指定的起始字）
                if (keyword != null && !keyword.isEmpty()) {
                    // 玩家已指定起始字，直接开始
                    char startChar = keyword.charAt(0);
                    chainValidator.setStartChar(startChar);
                    tvRuleHint.setText("[规则] 首尾接龙\n[目标] 你的首字必须是：「" + startChar + "」\n[注意] 不能重复使用已说过的诗句");
                    tvPoetryContent.setText(getPromptText());
                    tvGameState.setText("轮到你了");
                    startTimer();
                } else {
                    // 没有指定起始字，从后端获取随机诗句
                    if (chainValidator != null) {
                        chainValidator.startGame();
                    }
                }
            } else {
                // 其他模式：直接轮到玩家
                tvPoetryContent.setText(getPromptText());
                tvGameState.setText("轮到你了");
                startTimer();
            }
        } else {
            // 好友对战
            boolean isCreator = getIntent().getBooleanExtra("isCreator", false);
            if (isCreator) {
                // 创建者先手
                tvPoetryContent.setText(getPromptText());
                tvGameState.setText("轮到你了");
                startTimer();
            } else {
                // 被邀请方等待对手
                tvGameState.setText("等待对手出题...");
                tvPoetryContent.setText("对手思考中...");
                
                if ("chain".equals(gameMode)) {
                    if (keyword != null && !keyword.isEmpty()) {
                        char startChar = keyword.charAt(0);
                        chainValidator.setStartChar(startChar);
                        tvRuleHint.setText("[规则] 首尾接龙\n[目标] 你的首字必须是：「" + startChar + "」\n[注意] 不能重复使用已说过的诗句");
                        tvPoetryContent.setText(getPromptText());
                        tvGameState.setText("轮到你了");
                        startTimer();
                    } else {
                        if (chainValidator != null) {
                            chainValidator.startGame();
                        }
                    }
                } else {
                    startOpponentPolling();
                }
            }
        }
    }
    
    /**
     * 获取提示文本
     */
    private String getPromptText() {
        if (keyword == null || keyword.isEmpty()) {
            return "请说出诗句";
        }
        
        switch (gameMode) {
            case "position":
                return "请说出「" + keyword + "」字在第" + keywordPosition + "位的诗句";
            case "double_keyword":
                return "请说出同时包含「" + keyword + "」和「" + (keyword2 != null ? keyword2 : "?") + "」的诗句";
            case "forbidden":
                return "请说出不包含「" + keyword + "」字的诗句";
            case "chain":
                // 接龙模式：显示当前需要的首字
                if (chainValidator != null && chainValidator.getLastChar() != null) {
                    return "[目标] 你的首字必须是：「" + chainValidator.getLastChar() + "」";
                }
                return "请说出诗句（首尾接龙飞花令）";
            case "color":
                return "[颜色] 请说出包含「" + keyword + "」字的诗句";
            case "number":
                return "[数字] 请说出包含「" + keyword + "」字的诗句";
            default:
                return "请说出包含「" + keyword + "」字的诗句";
        }
    }
    
    /**
     * 更新右上角关键字标签显示
     */
    private void updateKeywordTag(TextView tvKeywordTag) {
        if (tvKeywordTag == null) return;
        
        switch (gameMode) {
            case "double_keyword":
                // 双字模式：显示两个关键字
                String tagText = keyword + "," + (keyword2 != null ? keyword2 : "?");
                tvKeywordTag.setText(tagText);
                break;
            case "forbidden":
                // 反飞花令：显示禁用字，加特殊标识
                tvKeywordTag.setText("禁:" + keyword);
                break;
            case "chain":
                // 接龙模式：显示当前需要的首字
                if (chainValidator != null && chainValidator.getLastChar() != null) {
                    tvKeywordTag.setText("接:" + chainValidator.getLastChar());
                } else {
                    tvKeywordTag.setText("接龙");
                }
                break;
            case "color":
                tvKeywordTag.setText("色:" + keyword);
                break;
            case "number":
                tvKeywordTag.setText("数:" + keyword);
                break;
            case "position":
                tvKeywordTag.setText(keyword + "@" + keywordPosition);
                break;
            default:
                tvKeywordTag.setText(keyword != null && !keyword.isEmpty() ? keyword : "无");
                break;
        }
    }
    


    /**
     * 开始轮询对手答案（好友对战）
     */
    private void startOpponentPolling() {
        if (isPolling) return;
        isPolling = true;
        
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling) return;
                
                // 调用后端获取对手答案
                fetchOpponentAnswer();
                
                // 每3秒轮询一次
                pollHandler.postDelayed(this, 3000);
            }
        };
        pollHandler.post(pollRunnable);
    }
    
    /**
     * 停止轮询
     */
    private void stopOpponentPolling() {
        isPolling = false;
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
        }
    }
    
    /**
     * 获取对手答案
     */
    private void fetchOpponentAnswer() {
        String url = ApiConstant.BATTLE_OPPONENT_ANSWER + "?battleId=" + battleId;
        
        OkHttpUtil.getWithAuth(this, url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 网络异常，继续轮询
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                            JsonObject data = jsonObject.getAsJsonObject("data");
                            String opponentPoem = data.has("poem") ? data.get("poem").getAsString() : "";
                            
                            if (!opponentPoem.isEmpty()) {
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    stopOpponentPolling();
                                    tvPoetryContent.setText("对手：" + opponentPoem);
                                    
                                    // 延迟后轮到玩家
                                    tvPoetryContent.postDelayed(() -> {
                                        if (isGameEnded || isFinishing() || isDestroyed()) return;
                                        tvGameState.setText("轮到你了");
                                        tvPoetryContent.setText(getPromptText());
                                        startTimer();
                                    }, 2000);
                                });
                            }
                        }
                    } catch (Exception e) {
                        // 解析失败，继续轮询
                    }
                }
            }
        });
    }

    /**
     * 开始倒计时（从记录的剩余时间开始）
     * 注意：只在玩家回合开始时调用，记录本题开始时间
     */
    private void startTimer() {
        if (isGameEnded || isFinishing() || isDestroyed()) return;
        if (timer != null) {
            timer.cancel();
        }
        
        // 只在玩家回合开始时记录本题开始时间
        if (isMyTurn) {
            questionStartTime = System.currentTimeMillis();
        }
        
        long startTime = remainingTime > 0 ? remainingTime : timeLimit * 1000;
        timer = new TimeCountUtil(startTime, 1000, tvTimer);
        timer.setOnTimeUpListener(() -> {
            runOnUiThread(() -> {
                if (isGameEnded || isFinishing() || isDestroyed()) return;
                tvTimer.setText("0");
                tvGameState.setText("时间到！");
                showNotification(NOTIF_WARNING, "时间到！本轮训练结束", 0);
                FeedbackManager.getInstance().speakWrong(BattleActivity.this, "时间到");
                endGame(false);
            });
        });
        timer.start();
    }
    
    /**
     * 提交答案
     */
    private void submitAnswer() {
        // 提交时立刻暂停计时，保存剩余时间
        if (timer != null) {
            timer.cancel();
            remainingTime = timer.getCurrentMillis();
        }
        
        // 计算本题耗时（秒）- 必须在提交时立即计算（向上取整）
        long questionTime = (long) Math.ceil((System.currentTimeMillis() - questionStartTime) / 1000.0);
        
        String answer = etAnswer.getText().toString().trim();
        
        if (answer.isEmpty()) {
            showNotification(NOTIF_WARNING, "请输入答案", 2500);
            startTimer(); // 输入为空，恢复倒计时
            return;
        }

        // 关闭软键盘
        hideKeyboard();

        // 显示加载状态
        btnSubmit.setEnabled(false);
        tvGameState.setText("验证中...");

        // 异步验证，避免阻塞主线程
        executor.execute(() -> {
            if (isFinishing() || isDestroyed()) return;
            String cleanedAnswer = PoetryCleanUtil.cleanPoetry(answer);
            
            // 检查是否为单字
            if (cleanedAnswer.length() < 2) {
                runOnUiThread(() -> {
                    showNotification(NOTIF_ERROR, "不能提交单个汉字，请输入完整诗句", 3500);
                    AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
                    startTimer(); // 恢复倒计时
                    btnSubmit.setEnabled(true);
                    tvGameState.setText("轮到你了");
                });
                return;
            }
            
            // 检查是否包含违规内容（数字、特殊符号等）
            if (!isValidPoetryContent(cleanedAnswer)) {
                runOnUiThread(() -> {
                    showNotification(NOTIF_ERROR, "答案只能包含中文汉字，不能包含数字、字母或特殊符号", 3500);
                    AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
                    startTimer(); // 恢复倒计时
                    btnSubmit.setEnabled(true);
                    tvGameState.setText("轮到你了");
                });
                return;
            }
            
            // 检查是否重复使用
            if (BattlePoetryCacheManager.isPoemUsed(cleanedAnswer)) {
                runOnUiThread(() -> {
                    showNotification(NOTIF_WARNING, "这句诗已经用过了，请换一句", 3000);
                    AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
                    startTimer();
                    btnSubmit.setEnabled(true);
                    tvGameState.setText("轮到你了");
                });
                return;
            }
            
            // 先进行前端规则验证
            runOnUiThread(() -> {
                boolean frontValid = validateAnswer(cleanedAnswer);
                if (!frontValid) {
                    // 验证失败，恢复倒计时
                    startTimer();
                    btnSubmit.setEnabled(true);
                    tvGameState.setText("轮到你了");
                    return;
                }
                
                // 规则验证通过后，提交到后端验证诗句是否存在于题库
                submitToServer(cleanedAnswer, questionTime);
                // 请求发出后保持禁用，直到收到响应
            });
        });
    }
    
    /**
     * 隐藏软键盘
     */
    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && etAnswer != null) {
                imm.hideSoftInputFromWindow(etAnswer.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证答案内容是否合法（只允许中文汉字）
     * @param answer 清洗后的答案
     * @return true表示合法，false表示包含违规内容
     */
    private boolean isValidPoetryContent(String answer) {
        if (answer == null || answer.isEmpty()) {
            return false;
        }
        
        // 检查每个字符是否为中文汉字（Unicode范围：\u4e00-\u9fff）
        for (int i = 0; i < answer.length(); i++) {
            char c = answer.charAt(i);
            if (c < '\u4e00' || c > '\u9fff') {
                // 发现非中文字符
                android.util.Log.d("BattleDebug", "发现非法字符: '" + c + "' (Unicode: " + (int)c + ")");
                return false;
            }
        }
        
        return true;
    }

    /**
     * 验证答案是否符合规则
     */
    private boolean validateAnswer(String answer) {
        return validateAnswer(answer, true);
    }
    
    /**
     * 验证答案是否符合规则（可控制是否显示Toast）
     * @param showToast 是否显示错误提示
     */
    private boolean validateAnswer(String answer, boolean showToast) {
        BaseValidationResult result = null;

        if ("chain".equals(gameMode)) {
            if (chainValidator != null) {
                ChainValidationResult chainResult = chainValidator.validate(answer);
                if (chainResult.isSuccess()) {
                    battle.setLastPoem(chainResult.getCleanedAnswer());
                    Character nextChar = chainResult.getNextChar();
                    if (nextChar != null) {
                        if (ruleEngine != null) {
                            ruleEngine.getContext().setLastChar(nextChar);
                        } else {
                            ruleCtx.setLastChar(nextChar);
                        }
                        runOnUiThread(() -> {
                            tvRuleHint.setText("[规则] 首尾接龙\n[目标] 下一句首字必须是：「" + nextChar + "」\n[注意] 不能重复使用已说过的诗句");
                        });
                    }
                }
                result = chainResult;
            }
        } else if (ruleEngine != null) {
            result = ruleEngine.validate(answer);
        }

        if (result == null) {
            if (showToast) {
                showNotification(NOTIF_ERROR, "规则引擎未初始化", 3000);
            }
            return false;
        }

        if (!result.isSuccess()) {
            if (showToast) {
                showNotification(NOTIF_ERROR, result.getMessage(), 3500);
            } else {
                android.util.Log.w("BattleDebug", "验证失败: " + result.getMessage());
            }
            return false;
        }

        return true;
    }

    private String preCheckAnswer(String answer) {
        if (answer == null || answer.isEmpty()) return "答案不能为空";

        switch (gameMode) {
            case "single_keyword":
            case "color":
            case "number":
                if (!PoetryCleanUtil.containsKeyword(answer, keyword)) {
                    return "答案必须包含关键字「" + keyword + "」";
                }
                break;
            case "double_keyword":
                if (!PoetryCleanUtil.containsKeyword(answer, keyword)) {
                    return "答案必须包含关键字「" + keyword + "」";
                }
                if (keyword2 != null && !PoetryCleanUtil.containsKeyword(answer, keyword2)) {
                    return "答案必须包含关键字「" + keyword2 + "」";
                }
                break;
            case "position":
                if (!PoetryCleanUtil.containsKeyword(answer, keyword)) {
                    return "答案必须包含关键字「" + keyword + "」";
                }
                if (keywordPosition > 0) {
                    int pos = keywordPosition - 1;
                    if (answer.length() <= pos || answer.charAt(pos) != keyword.charAt(0)) {
                        return "关键字「" + keyword + "」应在第" + keywordPosition + "位";
                    }
                }
                break;
            case "forbidden":
                if (PoetryCleanUtil.containsKeyword(answer, keyword)) {
                    return "答案不能包含禁用字「" + keyword + "」";
                }
                break;
            case "chain":
                // 接龙模式：由 validateAnswer 中的 chainValidator 统一校验，此处跳过
                break;
        }
        return null;
    }

    /**
     * 提交答案到服务器（后端只负责搜索验证诗句是否存在）
     * @param answer 玩家回答的诗句
     * @param questionTime 本题耗时（秒）
     */
    private void submitToServer(String answer, long questionTime) {
        if ("friend".equals(battleType)) {
            submitToFriendBattle(answer, questionTime);
        } else {
            submitToAIBattle(answer, questionTime);
        }
    }

    private void submitToAIBattle(String answer, long questionTime) {
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("gameMode", gameMode);
        requestJson.addProperty("keyword", keyword);
        requestJson.addProperty("answer", answer);
        
        if ("double_keyword".equals(gameMode) && keyword2 != null) {
            requestJson.addProperty("keyword2", keyword2);
        }
        if ("color".equals(gameMode) && colorKeyword != null) {
            requestJson.addProperty("colorKeyword", colorKeyword);
        }
        if ("number".equals(gameMode) && numberKeyword != null) {
            requestJson.addProperty("numberKeyword", numberKeyword);
        }
        if ("forbidden".equals(gameMode) && forbiddenWord != null) {
            requestJson.addProperty("forbiddenWord", forbiddenWord);
        }
        if ("position".equals(gameMode)) {
            requestJson.addProperty("keywordPosition", keywordPosition);
        }
        if ("chain".equals(gameMode) && chainValidator != null) {
            Character lastChar = chainValidator.getLastChar();
            if (lastChar != null) {
                requestJson.addProperty("lastChar", lastChar.toString());
            }
        }
        if (battleId != null) {
            requestJson.addProperty("battleId", battleId);
        }
        
        String url = ApiConstant.BATTLE_SUBMIT;
        
        android.util.Log.d("BattleDebug", "========== AI提交答案 ==========");
        android.util.Log.d("BattleDebug", "游戏模式: " + gameMode);
        android.util.Log.d("BattleDebug", "关键字: " + keyword);
        android.util.Log.d("BattleDebug", "玩家答案: " + answer);
        android.util.Log.d("BattleDebug", "请求URL: " + url);

        String preCheckError = preCheckAnswer(answer);
        if (preCheckError != null) {
            wrongCount++;
            showNotification(NOTIF_ERROR, preCheckError, 3500);
            AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
            etAnswer.setText("");
            BattlePoetryCacheManager.markPoemAsUsed(answer);
            fetchAIPoetryFromServer();
            return;
        }
        
        OkHttpUtil.postWithAuth(this, url, requestJson.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showNotification(NOTIF_ERROR, "网络异常: " + e.getMessage(), 3500);
                    fetchAIPoetryFromServer();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful()) {
                        try {
                            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                            int code = jsonObject.has("code") ? jsonObject.get("code").getAsInt() : -1;
                            boolean isValid = false;
                            if (code == 200 && jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                                JsonObject data = jsonObject.getAsJsonObject("data");
                                if (data.has("valid")) {
                                    isValid = data.get("valid").getAsBoolean();
                                }
                            }
                            
                            if (isValid) {
                                handleAnswerCorrect(answer, questionTime, true);
                            } else {
                                wrongCount++;
                                String displayMessage = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "答案未在题库中找到";
                                showNotification(NOTIF_ERROR, displayMessage, 3500);
                                AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
                                etAnswer.setText("");
                                BattlePoetryCacheManager.markPoemAsUsed(answer);
                                fetchAIPoetryFromServer();
                            }
                        } catch (Exception e) {
                            showNotification(NOTIF_ERROR, "解析响应失败", 3000);
                            fetchAIPoetryFromServer();
                        }
                    } else {
                        showNotification(NOTIF_ERROR, "答案错误或服务器异常", 3000);
                        AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
                        etAnswer.setText("");
                        fetchAIPoetryFromServer();
                    }
                });
            }
        });
    }

    private void submitToFriendBattle(String answer, long questionTime) {
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("answer", answer);
        requestJson.addProperty("roundNum", correctCount + 1);
        
        String url = ApiConstant.BATTLE_SUBMIT_FRIEND + battleId + "/submit";
        
        android.util.Log.d("BattleDebug", "========== 好友对战提交答案 ==========");
        android.util.Log.d("BattleDebug", "battleId: " + battleId);
        android.util.Log.d("BattleDebug", "玩家答案: " + answer);
        android.util.Log.d("BattleDebug", "请求URL: " + url);
        
        OkHttpUtil.postWithAuth(this, url, requestJson.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showNotification(NOTIF_ERROR, "网络异常: " + e.getMessage(), 3500);
                    btnSubmit.setEnabled(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (response.isSuccessful()) {
                        try {
                            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                            int code = jsonObject.has("code") ? jsonObject.get("code").getAsInt() : -1;
                            
                            boolean isValid = false;
                            if (code == 200 && jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                                JsonObject data = jsonObject.getAsJsonObject("data");
                                if (data.has("isCorrect")) {
                                    isValid = data.get("isCorrect").getAsBoolean();
                                } else if (data.has("success")) {
                                    isValid = data.get("success").getAsBoolean();
                                }
                            }
                            
                            if (isValid) {
                                handleAnswerCorrect(answer, questionTime, false);
                            } else {
                                wrongCount++;
                                String msg = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "答案验证失败";
                                showNotification(NOTIF_INFO, msg, 3500);
                                AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
                                startTimer();
                                btnSubmit.setEnabled(true);
                            }
                        } catch (Exception e) {
                            showNotification(NOTIF_ERROR, "解析响应失败", 3000);
                            startTimer();
                            btnSubmit.setEnabled(true);
                        }
                    } else {
                        showNotification(NOTIF_ERROR, "提交失败，请重试", 3000);
                        startTimer();
                        btnSubmit.setEnabled(true);
                    }
                });
            }
        });
    }

    private void handleAnswerCorrect(String answer, long questionTime, boolean isAI) {
        QuestionScore score = calculateQuestionScore(questionTime, gameMode);
        questionScores.add(score);
        correctCount++;
        
        showQuestionScoreDialog(score);
        
        showNotification(NOTIF_SUCCESS, "回答正确！", 2500);
        FeedbackManager.getInstance().speakCorrect(BattleActivity.this);
        etAnswer.setText("");
        
        BattlePoetryCacheManager.markPoemAsUsed(answer);
        
        if (ruleCtx != null) {
            ruleCtx.setLastAnswerLength(answer.length());
        }
                                        
        remainingTime = timeLimit * 1000;
        
        hideAiAnswer();
                                        
        if (isAI) {
            fetchAIPoetryFromServer();
        } else {
            tvGameState.setText("等待对手作答...");
            tvPoetryContent.setText("对手思考中...");
            btnSubmit.setEnabled(true);
            startOpponentPolling();
        }
    }
    
    /**
     * 从后端获取AI的诗句答案
     */
    private void fetchAIPoetryFromServer() {
        android.util.Log.d("BattleDebug", "========== AI开始从后端获取答案 ==========");
        android.util.Log.d("BattleDebug", "游戏模式: " + gameMode);
        android.util.Log.d("BattleDebug", "关键字: " + keyword);
        android.util.Log.d("BattleDebug", "关键字位置: " + keywordPosition);
        
        btnSubmit.setEnabled(false); // AI思考期间禁用按钮
        tvGameState.setText("AI 思考中...");
        tvPoetryContent.setText("等待 AI 作答...");
        
        // 构造请求参数
        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("gameMode", gameMode);
        requestJson.addProperty("keyword", keyword);
        
        if ("double_keyword".equals(gameMode) && keyword2 != null) {
            requestJson.addProperty("keyword2", keyword2);
        }
        if ("color".equals(gameMode) && colorKeyword != null) {
            requestJson.addProperty("colorKeyword", colorKeyword);
        }
        if ("position".equals(gameMode)) {
            requestJson.addProperty("keywordPosition", keywordPosition);
        }
        
        // 如果是接龙模式，需要传递上一句的尾字
        if ("chain".equals(gameMode) && chainValidator != null) {
            Character lastChar = chainValidator.getLastChar();
            if (lastChar != null) {
                requestJson.addProperty("lastChar", lastChar.toString());
            }
        }
        
        // 传递已使用的诗句列表，避免 AI 重复
        requestJson.add("usedPoems", new Gson().toJsonTree(BattlePoetryCacheManager.getUsedPoems()));
        
        android.util.Log.d("BattleDebug", "AI请求JSON: " + requestJson.toString());
        android.util.Log.d("BattleDebug", "AI请求URL: " + ApiConstant.BATTLE_AI_ANSWER);
        
        OkHttpUtil.postWithAuth(this, ApiConstant.BATTLE_AI_ANSWER, requestJson.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("BattleDebug", "AI 请求网络异常: " + e.getMessage());
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    android.util.Log.w("BattleDebug", "后端 AI 接口不可用，跳过 AI 回合");
                    skipAITurn();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                
                android.util.Log.d("BattleDebug", "========== AI后端响应 ==========");
                android.util.Log.d("BattleDebug", "HTTP状态码: " + response.code());
                android.util.Log.d("BattleDebug", "响应内容: " + responseBody);
                
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                        int code = jsonObject.has("code") ? jsonObject.get("code").getAsInt() : -1;
                        
                        if (code == 200 && jsonObject.has("data")) {
                            JsonObject data = jsonObject.getAsJsonObject("data");
                            String aiPoem = data.has("poem") ? data.get("poem").getAsString() : "";
                            
                            android.util.Log.d("BattleDebug", "AI返回的诗句: " + aiPoem);
                            
                            if (!aiPoem.isEmpty()) {
                                // 验证 AI 答案是否符合规则（不显示Toast）
                                String cleanedAiPoem = PoetryCleanUtil.cleanPoetry(aiPoem);
                                boolean aiValid = validateAnswer(cleanedAiPoem, false);
                                
                                if (aiValid) {
                                    // AI 成功作答且符合规则
                                    // 更新上下文中的上一句字数（用于字数匹配规则）
                                    if (ruleCtx != null) {
                                        ruleCtx.setLastAnswerLength(cleanedAiPoem.length());
                                    }
                                    showAiAnswer(aiPoem);
                                    AudioController.getInstance().playSound(AudioController.SOUND_CORRECT);
                                    FeedbackManager.getInstance().speakCorrect(BattleActivity.this);
                                    
                                    // 延迟后轮到玩家（延长显示时间到3秒）
                                    tvPoetryContent.postDelayed(() -> {
                                        if (isGameEnded || isFinishing() || isDestroyed()) return;
                                        tvGameState.setText("轮到你了");
                                        tvPoetryContent.setText(getPromptText());
                                        btnSubmit.setEnabled(true);
                                        
                                        updateRuleHintForNextTurn(aiPoem);
                                        startTimer();
                                    }, 3000);
                                } else {
                                    // AI 答案不符合规则，跳过 AI 回合
                                    android.util.Log.w("BattleDebug", "AI 答案不符合规则，跳过 AI 回合");
                                    skipAITurn();
                                }
                            } else {
                                // AI 无法从后端获取答案，跳过 AI 回合
                                android.util.Log.w("BattleDebug", "后端返回空诗句，跳过 AI 回合");
                                skipAITurn();
                            }
                        } else {
                            // 后端返回错误码，记录详细信息
                            String errorMsg = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "未知错误";
                            android.util.Log.e("BattleDebug", "后端 AI 接口返回错误 - code: " + code + ", message: " + errorMsg);
                            android.util.Log.w("BattleDebug", "跳过 AI 回合");
                            skipAITurn();
                        }
                    } catch (Exception e) {
                        showNotification(NOTIF_ERROR, "解析 AI 答案失败", 3000);
                        btnSubmit.setEnabled(true);
                        // AI作答异常，恢复倒计时
                        startTimer();
                        tvPoetryContent.setText(getPromptText());
                        tvGameState.setText("轮到你了");
                    }
                });
            }
        });
    }
    
    /**
     * 显示 AI 对句
     */
    private void showAiAnswer(String aiPoem) {
        if (llAiAnswerContainer != null && tvAiAnswer != null) {
            tvAiAnswer.setText(aiPoem);
            llAiAnswerContainer.setVisibility(View.VISIBLE);
            
            // 添加淡入动画
            llAiAnswerContainer.setAlpha(0f);
            llAiAnswerContainer.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();
        }
    }
    
    /**
     * 隐藏 AI 对句
     */
    private void hideAiAnswer() {
        if (llAiAnswerContainer != null) {
            llAiAnswerContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * 更新下一轮的规则提示（接龙模式）
     */
    private void updateRuleHintForNextTurn(String aiPoem) {
        String cleanedPoem = PoetryCleanUtil.cleanPoetry(aiPoem);
        if (cleanedPoem.isEmpty()) return;
        
        char nextChar = cleanedPoem.charAt(cleanedPoem.length() - 1);
        
        if ("chain".equals(gameMode)) {
            keyword = String.valueOf(nextChar);
            updateKeywordTag(findViewById(R.id.tv_keyword_tag));
            tvRuleHint.setText("[规则] 首尾接龙\n[目标] 你的首字必须是：「" + nextChar + "」\n[注意] 不能重复使用已说过的诗句");

            if (ruleCtx != null) {
                ruleCtx.setKeyword(keyword);
                ruleCtx.setLastChar(nextChar);
            }
        }
    }
    
    /**
     * 跳过 AI 回合，直接轮到玩家
     */
    private void skipAITurn() {
        showNotification(NOTIF_INFO, "AI 暂时无法作答，请继续", 2500);
        btnSubmit.setEnabled(true);
        tvPoetryContent.setText(getPromptText());
        tvGameState.setText("轮到你了");
        startTimer();
    }

    /**
     * 启动语音输入
     */
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出诗句");

        try {
            voiceInputLauncher.launch(intent);
        } catch (Exception e) {
            showNotification(NOTIF_WARNING, "语音输入不可用", 2500);
        }
    }

    /**
     * 在后端创建对战记录（用于结算和战绩统计）
     * 异步调用，不阻塞游戏开始
     */
    private void createBattleOnServer() {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("gameMode", gameMode);
        requestBody.addProperty("keyword", keyword);
        requestBody.addProperty("keyword2", keyword2 != null ? keyword2 : "");
        requestBody.addProperty("colorKeyword", colorKeyword != null ? colorKeyword : "");
        requestBody.addProperty("numberKeyword", numberKeyword != null ? numberKeyword : "");
        requestBody.addProperty("forbiddenWord", forbiddenWord != null ? forbiddenWord : "");
        requestBody.addProperty("keywordPosition", keywordPosition);
        requestBody.addProperty("timeLimit", timeLimit);

        String url = ApiConstant.BATTLE_CREATE_AI;
        Log.i(TAG, "创建对战请求: url=" + url + " body=" + requestBody);
        OkHttpUtil.postWithAuth(this, url, requestBody.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "创建对战失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.i(TAG, "创建对战响应: code=" + response.code() + " body=" + body);
                try {
                    JsonObject json = new Gson().fromJson(body, JsonObject.class);
                    if (json != null && json.has("code") && json.get("code").getAsInt() == 200) {
                        JsonObject data = json.getAsJsonObject("data");
                        if (data != null && data.has("battleId")) {
                            battleId = data.get("battleId").getAsString();
                            Log.i(TAG, "对战创建成功, battleId=" + battleId);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析创建对战响应失败", e);
                }
            }
        });
    }

    /**
     * 结束游戏
     */
    private void endGame(boolean isWin) {
        if (isGameEnded) return;
        isGameEnded = true;
        stopOpponentPolling();
        
        if (timer != null) {
            timer.cancel();
        }
        
        // 通知后端结束对战
        if (battleId != null) {
            JsonObject endRequest = new JsonObject();
            if ("friend".equals(battleType)) {
                endRequest.addProperty("winnerId", SharedPrefsUtil.getUserId(this));
            } else {
                endRequest.addProperty("isWin", isWin);
                endRequest.addProperty("answerCount", correctCount);
                endRequest.addProperty("wrongCount", wrongCount);
            }
            String url = ApiConstant.BATTLE_END + battleId + "/end";
            Log.i(TAG, "结束对战请求: url=" + url + " body=" + endRequest);
            OkHttpUtil.postWithAuth(this, url, endRequest.toString(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "结束对战请求失败: " + e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.i(TAG, "结束对战响应: code=" + response.code() + " body=" + body);
                }
            });
        }
        
        // 人机对战：清除临时缓存
        if ("ai".equals(battleType)) {
            SharedPrefsUtil.clearTempBattleCache(this);
            BattlePoetryCacheManager.clearUsedPoems();
        }
        
        // 计算总耗时（秒）
        long totalTimeSpent = 0;
        for (QuestionScore qs : questionScores) {
            totalTimeSpent += qs.timeSpent;
        }
        
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("correctCount", correctCount);
        intent.putExtra("totalTimeSpent", totalTimeSpent);
        intent.putExtra("questionScores", new Gson().toJson(questionScores));
        intent.putExtra("gameMode", gameMode);
        intent.putExtra("battleType", battleType);
        intent.putExtra("isWin", isWin);
        intent.putExtra("keyword", keyword);
        intent.putExtra("keyword2", keyword2 != null ? keyword2 : "");
        intent.putExtra("keywordPosition", keywordPosition);
        intent.putExtra("timeLimit", timeLimit);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopOpponentPolling();

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (exitDialog != null && exitDialog.isShowing()) {
            exitDialog.dismiss();
        }
        exitDialog = null;

        pollHandler.removeCallbacksAndMessages(null);

        executor.shutdownNow();

        if (tvPoetryContent != null) {
            tvPoetryContent.removeCallbacks(null);
        }

        boolean endedNormally = isGameEnded;
        isGameEnded = true;

        if (!endedNormally && "ai".equals(battleType) && battleId != null) {
            JsonObject endRequest = new JsonObject();
            endRequest.addProperty("isWin", false);
            endRequest.addProperty("answerCount", correctCount);
            endRequest.addProperty("wrongCount", wrongCount);
            String url = ApiConstant.BATTLE_END + battleId + "/end";
            Log.w(TAG, "onDestroy保护调用: url=" + url);
            OkHttpUtil.postWithAuth(this, url, endRequest.toString(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "onDestroy结束对战失败: " + e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    Log.i(TAG, "onDestroy结束对战响应: code=" + response.code() + " body=" + body);
                }
            });
            SharedPrefsUtil.clearTempBattleCache(this);
            BattlePoetryCacheManager.clearUsedPoems();
        }

        AudioController controller = AudioController.getInstance();
        if (controller.getCurrentScene() == AudioController.SCENE_BATTLE) {
            controller.playBGM(AudioController.SCENE_MAIN);
        }
    }
    
    /**
     * 拦截返回键，提示认输
     */
    @Override
    public void onBackPressed() {
        if (exitDialog != null && exitDialog.isShowing()) {
            return;
        }
        
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("是否要认输？")
                .setPositiveButton("确定", (dialog, which) -> {
                    AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                    endGame(false);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        AudioController.getInstance().pauseBGM();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (!isGameEnded) {
            AudioController.getInstance().resumeBGM();
        }
    }
    
    /**
     * 设置入场动画
     */
    private void setupEntryAnimations() {
        ButtonAnimationHelper.addEntryAnimation(tvBattleTitle, 0);
        ButtonAnimationHelper.addEntryAnimation(tvTimer, 100);
        ButtonAnimationHelper.addEntryAnimation(tvRuleHint, 150);
        ButtonAnimationHelper.addEntryAnimation(tvPoetryContent, 200);
        ButtonAnimationHelper.addEntryAnimation(tvGameState, 250);
        ButtonAnimationHelper.addEntryAnimation(etAnswer, 300);
        ButtonAnimationHelper.addEntryAnimation(btnSubmit, 350);
        ButtonAnimationHelper.addEntryAnimation(btnVoiceInput, 400);
    }
    
    /**
     * 显示音量调节对话框
     */
    private void showVolumeDialog() {
        // 创建 SeekBar
        android.widget.SeekBar seekBar = new android.widget.SeekBar(this);
        int currentVolume = (int) (AudioController.getInstance().getBGMVolume() * 100);
        seekBar.setProgress(currentVolume);
        seekBar.setMax(100);
        
        // 设置布局参数
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(40, 20, 40, 20);
        seekBar.setLayoutParams(params);
        
        // 创建对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("对战音乐音量")
            .setView(seekBar)
            .setPositiveButton("确定", (dialog, which) -> {
                dialog.dismiss();
            })
            .setNegativeButton("静音", (dialog, which) -> {
                AudioController.getInstance().setBGMVolume(0f);
                dialog.dismiss();
            })
            .show();
        
        // 监听滑块变化，实时调整音量
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float volume = progress / 100.0f;
                    AudioController.getInstance().setBGMVolume(volume);
                }
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
            }
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
            }
        });
    }
}
