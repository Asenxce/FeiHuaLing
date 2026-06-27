package com.example.feihualinggame.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.Poetry;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 诗词加载等待界面 - 精致古风沉浸式设计
 * 营造游戏即将开始的紧张感和仪式感
 */
public class PoetryLoadingActivity extends BaseActivity {

    private String keyword;
    private String keyword2;
    private String gameMode;
    private String positionKeyword;
    private String colorKeyword;
    private String numberKeyword;
    private String forbiddenWord;
    private int keywordPosition;
    private String battleType;
    private int timeLimit;
    private boolean isVirtualLoading = false;

    private TextView tvTitle;
    private TextView tvLoadingText;
    private TextView tvPoetryPreview;
    private TextView tvProgress;
    private TextView tvPoetryAuthor;
    private ProgressBar progressBar;
    private ImageView ivLoadingIcon;
    private ImageView ivIconGlow;
    private View vDecoLineLeft;
    private View vDecoLineRight;
    private LinearLayout cardPoetryPreview;

    /** 最短加载时间（ms），确保入场动画和诗词展示有足够时间 */
    private static final long MINIMUM_LOADING_DURATION = 2500;
    private long loadingStartTime;
    private boolean dataReady = false;
    private boolean isLoading = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String[] previewPoetries;
    private String[] previewAuthors;

    private int currentPreviewIndex = 0;
    private Handler previewHandler = new Handler(Looper.getMainLooper());
    private Runnable previewRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏底部导航条，使用沉浸式全屏模式
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );

        setContentView(R.layout.activity_poetry_loading);

        keyword = getIntent().getStringExtra("keyword");
        keyword2 = getIntent().getStringExtra("keyword2");
        gameMode = getIntent().getStringExtra("gameMode");
        positionKeyword = getIntent().getStringExtra("positionKeyword");
        colorKeyword = getIntent().getStringExtra("colorKeyword");
        numberKeyword = getIntent().getStringExtra("numberKeyword");
        forbiddenWord = getIntent().getStringExtra("forbiddenWord");
        keywordPosition = getIntent().getIntExtra("keywordPosition", 3);
        battleType = getIntent().getStringExtra("battleType");
        timeLimit = getIntent().getIntExtra("timeLimit", 30);

        isVirtualLoading = "chain".equals(gameMode) || "forbidden".equals(gameMode)
                || "color".equals(gameMode) || "number".equals(gameMode);

        initViews();
        loadingStartTime = System.currentTimeMillis();
        playEntranceAnimation();
        startLoadingAnimation();
        startPoetryPreview();

        if (isVirtualLoading) {
            startVirtualLoading();
        } else {
            loadPoetryData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioController.getInstance().stopBGM();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvLoadingText = findViewById(R.id.tv_loading_text);
        tvPoetryPreview = findViewById(R.id.tv_poetry_preview);
        tvPoetryAuthor = findViewById(R.id.tv_poetry_author);
        tvProgress = findViewById(R.id.tv_progress);
        progressBar = findViewById(R.id.progress_bar);
        ivLoadingIcon = findViewById(R.id.iv_loading_icon);
        ivIconGlow = findViewById(R.id.iv_icon_glow);
        vDecoLineLeft = findViewById(R.id.v_deco_line_left);
        vDecoLineRight = findViewById(R.id.v_deco_line_right);
        cardPoetryPreview = findViewById(R.id.card_poetry_preview);

        updateTitleAndPreviewByMode();
    }

    /**
     * 入场动画 — 各元素从不同方向淡入滑入
     */
    private void playEntranceAnimation() {
        // 初始状态：隐藏所有元素
        ivIconGlow.setAlpha(0f);
        ivLoadingIcon.setAlpha(0f);
        ivLoadingIcon.setScaleX(0.6f);
        ivLoadingIcon.setScaleY(0.6f);
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(20f);
        vDecoLineLeft.setAlpha(0f);
        vDecoLineLeft.setScaleX(0f);
        vDecoLineRight.setAlpha(0f);
        vDecoLineRight.setScaleX(0f);
        cardPoetryPreview.setAlpha(0f);
        cardPoetryPreview.setTranslationY(30f);
        progressBar.setAlpha(0f);
        progressBar.setTranslationY(10f);

        // 1. 光晕淡入 (0ms)
        ObjectAnimator glowFadeIn = ObjectAnimator.ofFloat(ivIconGlow, "alpha", 0f, 0.7f);
        glowFadeIn.setDuration(600);
        glowFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        // 2. 扇子图标缩放+淡入 (100ms)
        ObjectAnimator iconFadeIn = ObjectAnimator.ofFloat(ivLoadingIcon, "alpha", 0f, 0.9f);
        iconFadeIn.setDuration(500);
        iconFadeIn.setStartDelay(100);
        iconFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(ivLoadingIcon, "scaleX", 0.6f, 1f);
        iconScaleX.setDuration(500);
        iconScaleX.setStartDelay(100);
        iconScaleX.setInterpolator(new OvershootInterpolator(1.2f));

        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(ivLoadingIcon, "scaleY", 0.6f, 1f);
        iconScaleY.setDuration(500);
        iconScaleY.setStartDelay(100);
        iconScaleY.setInterpolator(new OvershootInterpolator(1.2f));

        // 3. 标题淡入+上移 (300ms)
        ObjectAnimator titleFadeIn = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
        titleFadeIn.setDuration(500);
        titleFadeIn.setStartDelay(300);
        titleFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator titleSlideUp = ObjectAnimator.ofFloat(tvTitle, "translationY", 20f, 0f);
        titleSlideUp.setDuration(500);
        titleSlideUp.setStartDelay(300);
        titleSlideUp.setInterpolator(new AccelerateDecelerateInterpolator());

        // 4. 装饰线展开 (400ms)
        ObjectAnimator lineLeftFade = ObjectAnimator.ofFloat(vDecoLineLeft, "alpha", 0f, 0.4f);
        lineLeftFade.setDuration(400);
        lineLeftFade.setStartDelay(400);
        ObjectAnimator lineLeftScale = ObjectAnimator.ofFloat(vDecoLineLeft, "scaleX", 0f, 1f);
        lineLeftScale.setDuration(400);
        lineLeftScale.setStartDelay(400);
        lineLeftScale.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator lineRightFade = ObjectAnimator.ofFloat(vDecoLineRight, "alpha", 0f, 0.4f);
        lineRightFade.setDuration(400);
        lineRightFade.setStartDelay(400);
        ObjectAnimator lineRightScale = ObjectAnimator.ofFloat(vDecoLineRight, "scaleX", 0f, 1f);
        lineRightScale.setDuration(400);
        lineRightScale.setStartDelay(400);
        lineRightScale.setInterpolator(new AccelerateDecelerateInterpolator());

        // 5. 卡片淡入+上移 (500ms)
        ObjectAnimator cardFadeIn = ObjectAnimator.ofFloat(cardPoetryPreview, "alpha", 0f, 1f);
        cardFadeIn.setDuration(500);
        cardFadeIn.setStartDelay(500);
        cardFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator cardSlideUp = ObjectAnimator.ofFloat(cardPoetryPreview, "translationY", 30f, 0f);
        cardSlideUp.setDuration(500);
        cardSlideUp.setStartDelay(500);
        cardSlideUp.setInterpolator(new AccelerateDecelerateInterpolator());

        // 6. 进度条淡入 (700ms)
        ObjectAnimator progressFadeIn = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f);
        progressFadeIn.setDuration(400);
        progressFadeIn.setStartDelay(700);
        ObjectAnimator progressSlideUp = ObjectAnimator.ofFloat(progressBar, "translationY", 10f, 0f);
        progressSlideUp.setDuration(400);
        progressSlideUp.setStartDelay(700);

        // 播放入场动画
        AnimatorSet entranceSet = new AnimatorSet();
        entranceSet.playTogether(
            glowFadeIn, iconFadeIn, iconScaleX, iconScaleY,
            titleFadeIn, titleSlideUp,
            lineLeftFade, lineLeftScale, lineRightFade, lineRightScale,
            cardFadeIn, cardSlideUp,
            progressFadeIn, progressSlideUp
        );
        entranceSet.start();
    }

    /**
     * 根据游戏模式更新标题和诗词预览
     */
    private void updateTitleAndPreviewByMode() {
        String title;

        switch (gameMode) {
            case "double_keyword":
                if (keyword != null && keyword2 != null) {
                    title = "正在筹备「" + keyword + "」和「" + keyword2 + "」字对决";
                } else {
                    title = "正在筹备双字对决";
                }
                previewPoetries = new String[] {
                    "春眠不觉晓，处处闻啼鸟",
                    "春风又绿江南岸，明月何时照我还",
                    "好雨知时节，当春乃发生",
                    "日出江花红胜火，春来江水绿如蓝",
                    "红豆生南国，春来发几枝"
                };
                previewAuthors = new String[] {
                    "唐·孟浩然《春晓》",
                    "宋·王安石《泊船瓜洲》",
                    "唐·杜甫《春夜喜雨》",
                    "唐·白居易《忆江南》",
                    "唐·王维《相思》"
                };
                break;
            case "forbidden":
                if (keyword != null) {
                    title = "正在筹备「" + keyword + "」字禁用对决";
                } else {
                    title = "正在筹备反飞花令对决";
                }
                previewPoetries = new String[] {
                    "山重水复疑无路，柳暗花明又一村",
                    "白日依山尽，黄河入海流",
                    "千山鸟飞绝，万径人踪灭",
                    "独在异乡为异客，每逢佳节倍思亲",
                    "海内存知己，天涯若比邻"
                };
                previewAuthors = new String[] {
                    "宋·陆游《游山西村》",
                    "唐·王之涣《登鹳雀楼》",
                    "唐·柳宗元《江雪》",
                    "唐·王维《九月九日忆山东兄弟》",
                    "唐·王勃《送杜少府之任蜀州》"
                };
                break;
            case "chain":
                title = "正在筹备首尾接龙对决";
                previewPoetries = new String[] {
                    "床前明月光，疑是地上霜",
                    "举头望明月，低头思故乡",
                    "乡书何处达？归雁洛阳边",
                    "边庭流血成海水，武皇开边意未已",
                    "已忍伶俜十年事，心持半偈万缘空"
                };
                previewAuthors = new String[] {
                    "唐·李白《静夜思》",
                    "唐·李白《静夜思》",
                    "唐·王湾《次北固山下》",
                    "唐·杜甫《兵车行》",
                    "唐·杜甫《宿府》"
                };
                break;
            case "color":
                if (keyword != null) {
                    title = "正在筹备「" + keyword + "」色对决";
                } else {
                    title = "正在筹备颜色对决";
                }
                previewPoetries = new String[] {
                    "日出江花红胜火，春来江水绿如蓝",
                    "两个黄鹂鸣翠柳，一行白鹭上青天",
                    "黑云翻墨未遮山，白雨跳珠乱入船",
                    "千里莺啼绿映红，水村山郭酒旗风",
                    "绿树村边合，青山郭外斜"
                };
                previewAuthors = new String[] {
                    "唐·白居易《忆江南》",
                    "唐·杜甫《绝句》",
                    "宋·苏轼《六月二十七日望湖楼醉书》",
                    "唐·杜牧《江南春》",
                    "唐·孟浩然《过故人庄》"
                };
                break;
            case "number":
                if (keyword != null) {
                    title = "正在筹备「" + keyword + "」数对决";
                } else {
                    title = "正在筹备数字对决";
                }
                previewPoetries = new String[] {
                    "一去二三里，烟村四五家",
                    "两个黄鹂鸣翠柳，一行白鹭上青天",
                    "三山半落青天外，二水中分白鹭洲",
                    "四面边声连角起，千嶂里，长烟落日孤城闭",
                    "五岭逶迤腾细浪，乌蒙磅礴走泥丸"
                };
                previewAuthors = new String[] {
                    "宋·邵雍《山村咏怀》",
                    "唐·杜甫《绝句》",
                    "唐·李白《登金陵凤凰台》",
                    "宋·范仲淹《渔家傲·秋思》",
                    "毛泽东《七律·长征》"
                };
                break;
            case "position":
                if (keyword != null) {
                    title = "正在筹备「" + keyword + "」第" + keywordPosition + "位对决";
                } else {
                    title = "正在筹备位置对决";
                }
                previewPoetries = new String[] {
                    "春眠不觉晓，处处闻啼鸟",
                    "好雨知时节，当春乃发生",
                    "国破山河在，城春草木深",
                    "红豆生南国，春来发几枝",
                    "谁言寸草心，报得三春晖"
                };
                previewAuthors = new String[] {
                    "唐·孟浩然《春晓》",
                    "唐·杜甫《春夜喜雨》",
                    "唐·杜甫《春望》",
                    "唐·王维《相思》",
                    "唐·孟郊《游子吟》"
                };
                break;
            default:
                if (keyword != null) {
                    title = "正在筹备「" + keyword + "」字对决";
                } else {
                    title = "正在筹备诗词对决";
                }
                previewPoetries = new String[] {
                    "春眠不觉晓，处处闻啼鸟",
                    "好雨知时节，当春乃发生",
                    "春风又绿江南岸，明月何时照我还",
                    "春色满园关不住，一枝红杏出墙来",
                    "春花秋月何时了，往事知多少"
                };
                previewAuthors = new String[] {
                    "唐·孟浩然《春晓》",
                    "唐·杜甫《春夜喜雨》",
                    "宋·王安石《泊船瓜洲》",
                    "宋·叶绍翁《游园不值》",
                    "南唐·李煜《虞美人》"
                };
                break;
        }

        tvTitle.setText(title);

        // 设置初始诗词预览和作者
        if (previewPoetries.length > 0) {
            tvPoetryPreview.setText(previewPoetries[0]);
            if (previewAuthors != null && previewAuthors.length > 0) {
                tvPoetryAuthor.setText("—— " + previewAuthors[0]);
            }
        }
    }

    /**
     * 启动加载动画（扇子旋转 + 光晕呼吸 + 文字闪烁）
     */
    private void startLoadingAnimation() {
        // 图标旋转动画 — 缓慢优雅的旋转
        ObjectAnimator rotation = ObjectAnimator.ofFloat(ivLoadingIcon, "rotation", 0f, 360f);
        rotation.setDuration(3000);
        rotation.setRepeatCount(ObjectAnimator.INFINITE);
        rotation.setInterpolator(new LinearInterpolator());
        rotation.start();

        // 光晕呼吸动画 — 缩放脉冲
        ObjectAnimator glowScaleX = ObjectAnimator.ofFloat(ivIconGlow, "scaleX", 0.85f, 1.1f, 0.85f);
        glowScaleX.setDuration(2500);
        glowScaleX.setRepeatCount(ObjectAnimator.INFINITE);
        glowScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        glowScaleX.start();

        ObjectAnimator glowScaleY = ObjectAnimator.ofFloat(ivIconGlow, "scaleY", 0.85f, 1.1f, 0.85f);
        glowScaleY.setDuration(2500);
        glowScaleY.setRepeatCount(ObjectAnimator.INFINITE);
        glowScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        glowScaleY.start();

        // 光晕透明度脉冲
        ObjectAnimator glowPulse = ObjectAnimator.ofFloat(ivIconGlow, "alpha", 0.5f, 0.8f, 0.5f);
        glowPulse.setDuration(2500);
        glowPulse.setRepeatCount(ObjectAnimator.INFINITE);
        glowPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulse.start();

        // 加载文字淡入淡出
        ObjectAnimator fadeInOut = ObjectAnimator.ofFloat(tvLoadingText, "alpha", 1f, 0.4f, 1f);
        fadeInOut.setDuration(1800);
        fadeInOut.setRepeatCount(ObjectAnimator.INFINITE);
        fadeInOut.start();
    }

    /**
     * 启动诗词滚动预览
     */
    private void startPoetryPreview() {
        previewRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isLoading) return;

                // 卡片整体淡出
                cardPoetryPreview.animate()
                    .alpha(0f)
                    .translationY(-10f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        // 更新诗词
                        currentPreviewIndex = (currentPreviewIndex + 1) % previewPoetries.length;
                        tvPoetryPreview.setText(previewPoetries[currentPreviewIndex]);
                        if (previewAuthors != null && currentPreviewIndex < previewAuthors.length) {
                            tvPoetryAuthor.setText("—— " + previewAuthors[currentPreviewIndex]);
                        }

                        // 重置位置并淡入
                        cardPoetryPreview.setTranslationY(15f);
                        cardPoetryPreview.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(350)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                    })
                    .start();

                // 每2.5秒切换一次（给更多阅读时间）
                previewHandler.postDelayed(this, 2500);
            }
        };
        previewHandler.post(previewRunnable);
    }

    /**
     * 虚拟加载
     */
    private void startVirtualLoading() {
        updateProgress(10, "游戏即将开始...");

        mainHandler.postDelayed(() -> updateProgress(30, "准备诗词库..."), 400);
        mainHandler.postDelayed(() -> updateProgress(60, "初始化对战环境..."), 800);
        mainHandler.postDelayed(() -> updateProgress(90, "加载完成"), 1200);
        mainHandler.postDelayed(() -> {
            updateProgress(100, "对决开始！");
            mainHandler.postDelayed(() -> enterBattleActivity(), 500);
        }, 1600);
    }

    /**
     * 加载诗词数据
     */
    private void loadPoetryData() {
        isLoading = true;
        updateProgress(10, "正在连接诗词库...");

        String url;
        if (keyword2 != null && !keyword2.isEmpty()) {
            url = ApiConstant.POETRY_SEARCH + "?keyword=" + keyword + "&keyword2=" + keyword2 + "&size=50";
        } else {
            url = ApiConstant.POETRY_SEARCH + "?keyword=" + keyword + "&size=50";
        }

        OkHttpUtil.getWithAuth(this, url, new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                mainHandler.post(() -> {
                    updateProgress(0, "网络连接失败，正在重试...");
                    mainHandler.postDelayed(() -> loadPoetryData(), 2000);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> {
                        updateProgress(0, "服务器响应异常，正在重试...");
                        mainHandler.postDelayed(() -> loadPoetryData(), 2000);
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    mainHandler.post(() -> updateProgress(40, "正在解析诗词数据..."));

                    ArrayList<Poetry> poetryList = new ArrayList<>();

                    try {
                        Gson gson = new Gson();
                        JsonElement element = gson.fromJson(responseBody, JsonElement.class);
                        JsonArray dataArray = null;

                        if (element.isJsonArray()) {
                            dataArray = element.getAsJsonArray();
                        } else if (element.isJsonObject()) {
                            JsonObject obj = element.getAsJsonObject();
                            if (obj.has("data") && obj.get("data").isJsonArray()) {
                                dataArray = obj.getAsJsonArray("data");
                            }
                        }

                        if (dataArray != null && dataArray.size() > 0) {
                            for (int i = 0; i < dataArray.size() && i < 50; i++) {
                                try {
                                    JsonObject poetryObj = dataArray.get(i).getAsJsonObject();
                                    String content = poetryObj.has("content") ? poetryObj.get("content").getAsString() : "";
                                    String fullContent = poetryObj.has("fullContent") ? poetryObj.get("fullContent").getAsString() : "";
                                    String author = poetryObj.has("author") ? poetryObj.get("author").getAsString() : "未知";
                                    String title = poetryObj.has("title") ? poetryObj.get("title").getAsString() : "无题";
                                    String dynasty = poetryObj.has("dynasty") ? poetryObj.get("dynasty").getAsString() : "";

                                    if (fullContent != null && !fullContent.isEmpty()) {
                                        String[] lines = fullContent.split("[。！？\\n]");
                                        for (String line : lines) {
                                            String trimmedLine = line.trim();
                                            if (!trimmedLine.isEmpty()) {
                                                Poetry linePoetry = new Poetry();
                                                linePoetry.setContent(trimmedLine);
                                                linePoetry.setFullContent(fullContent);
                                                linePoetry.setAuthor(author);
                                                linePoetry.setTitle(title);
                                                linePoetry.setDynasty(dynasty);
                                                poetryList.add(linePoetry);
                                            }
                                        }
                                    } else if (content != null && !content.isEmpty()) {
                                        Poetry linePoetry = new Poetry();
                                        linePoetry.setContent(content);
                                        linePoetry.setFullContent(content);
                                        linePoetry.setAuthor(author);
                                        linePoetry.setTitle(title);
                                        linePoetry.setDynasty(dynasty);
                                        poetryList.add(linePoetry);
                                    }
                                } catch (Exception e) {
                                    // 忽略单条解析失败
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (poetryList.isEmpty()) {
                        mainHandler.post(() -> {
                            updateProgress(0, "未找到相关诗词，正在重试...");
                            mainHandler.postDelayed(() -> loadPoetryData(), 2000);
                        });
                        return;
                    }

                    mainHandler.post(() -> updateProgress(60, "正在整理诗词库..."));

                    for (int i = 0; i < Math.min(poetryList.size(), 50); i++) {
                        final int index = i + 1;
                        final int progress = 60 + (int)((index) / (float)Math.min(poetryList.size(), 50) * 30);
                        mainHandler.post(() -> updateProgress(progress, "正在加载第 " + index + " 首诗词..."));
                    }

                    mainHandler.post(() -> updateProgress(100, "诗词库加载完成！"));

                    mainHandler.postDelayed(() -> {
                        enterBattleActivity();
                    }, 800);

                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        updateProgress(0, "数据解析失败，正在重试...");
                        mainHandler.postDelayed(() -> loadPoetryData(), 2000);
                    });
                }
            }
        });
    }

    /**
     * 更新进度（带进度数字动画）
     */
    private void updateProgress(int progress, String text) {
        // 平滑过渡进度条
        int currentProgress = progressBar.getProgress();
        ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", currentProgress, progress);
        progressAnim.setDuration(400);
        progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnim.start();

        tvProgress.setText(progress + "%");
        tvLoadingText.setText(text);
    }

    /**
     * 进入对战界面（需满足最短加载时间）
     */
    private void enterBattleActivity() {
        long elapsed = System.currentTimeMillis() - loadingStartTime;
        if (elapsed < MINIMUM_LOADING_DURATION) {
            // 加载完成过早，等待剩余时间后再跳转
            dataReady = true;
            final long remaining = MINIMUM_LOADING_DURATION - elapsed;
            // 在等待期间进度保持100%
            mainHandler.postDelayed(() -> doEnterBattle(), remaining);
        } else {
            doEnterBattle();
        }
    }

    /**
     * 执行实际跳转
     */
    private void doEnterBattle() {
        isLoading = false;
        previewHandler.removeCallbacks(previewRunnable);

        Intent intent = new Intent(PoetryLoadingActivity.this, BattleActivity.class);
        intent.putExtra("keyword", keyword);
        intent.putExtra("keyword2", keyword2);
        intent.putExtra("gameMode", gameMode);
        intent.putExtra("battleType", battleType);
        intent.putExtra("timeLimit", timeLimit);
        if (colorKeyword != null && !colorKeyword.isEmpty()) {
            intent.putExtra("colorKeyword", colorKeyword);
        }
        if (numberKeyword != null && !numberKeyword.isEmpty()) {
            intent.putExtra("numberKeyword", numberKeyword);
        }
        if (forbiddenWord != null && !forbiddenWord.isEmpty()) {
            intent.putExtra("forbiddenWord", forbiddenWord);
        }
        if (positionKeyword != null && !positionKeyword.isEmpty()) {
            intent.putExtra("positionKeyword", positionKeyword);
        }
        if ("position".equals(gameMode)) {
            intent.putExtra("keywordPosition", keywordPosition);
        }
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isLoading = false;
        previewHandler.removeCallbacks(previewRunnable);
        mainHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        if (isLoading) {
            return;
        }
        super.onBackPressed();
    }
}
