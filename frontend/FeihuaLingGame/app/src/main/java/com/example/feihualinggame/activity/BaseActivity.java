package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.FeihuaLingGameApplication;
import com.example.feihualinggame.R;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.FeedbackManager;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 基础Activity - 提供顶部和底部栏的通用功能
 * 子类需要调用 setContentViewWithBars() 来设置包含顶部和底部栏的布局
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    protected TextView tvUsername;
    protected TextView tvUserId;
    protected TextView tvTotalScore;
    protected LinearLayout btnPoetryQuery;
    protected LinearLayout btnProfile;
    protected LinearLayout btnFriend;
    protected LinearLayout btnLogout;
    
    // 底部导航栏图标
    protected ImageView ivPoetryQuery;
    protected ImageView ivFriend;
    protected ImageView ivProfile;
    protected ImageView ivLogout;
    
    // 底部导航栏文字标签
    protected TextView tvPoetryQueryLabel;
    protected TextView tvFriendLabel;
    protected TextView tvProfileLabel;
    protected TextView tvLogoutLabel;
    
    private FrameLayout contentContainer;
    private LinearLayout topBar;
    private LinearLayout bottomBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用主题模式
        applyThemeMode();
        
        super.onCreate(savedInstanceState);
        AudioController.getInstance().init(this);
        FeedbackManager.getInstance().init(this);
        // 注意：不在这里设置布局，由子类调用 setContentViewWithBars()
        // SystemUIUtil.hideNavigationBarIndicator() 将在 setContentViewWithBars() 中调用
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
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "light":
            default:
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }
    
    /**
     * 设置带有顶部和底部栏的布局
     * @param contentLayoutId 中间内容的布局资源ID
     */
    protected void setContentViewWithBars(int contentLayoutId) {
        // 先设置基础布局（包含顶部栏、底部栏和内容容器）
        super.setContentView(R.layout.base_activity);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);
        
        // 设置状态栏为浅色背景模式（状态栏图标变为深色，适配白色上边栏）
        SystemUIUtil.setLightStatusBar(this);
        
        // 初始化顶部和底部栏
        initTopAndBottomBars();
        
        // 将子类的内容布局添加到内容容器中
        contentContainer = findViewById(R.id.content_container);
        LayoutInflater.from(this).inflate(contentLayoutId, contentContainer, true);
        
        // 加载用户信息到顶部栏
        loadUserInfoToTopBar();
        
        // 设置底部栏点击事件
        setupBottomBarClicks();
    }
    
    /**
     * 设置不带顶部和底部栏的布局（用于登录页、首页等不需要导航栏的页面）
     * @param layoutResID 布局资源ID
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
    }
    
    /**
     * 初始化顶部和底部栏
     */
    private void initTopAndBottomBars() {
        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);
        
        tvUsername = findViewById(R.id.tv_username);
        tvUserId = findViewById(R.id.tv_user_id);
        tvTotalScore = findViewById(R.id.tv_total_score);
        
        btnPoetryQuery = findViewById(R.id.btn_poetry_query);
        btnProfile = findViewById(R.id.btn_profile);
        btnFriend = findViewById(R.id.btn_friend);
        if (btnFriend != null) btnFriend.setVisibility(View.GONE);
        btnLogout = findViewById(R.id.btn_logout);
        
        ivPoetryQuery = findViewById(R.id.iv_poetry_query);
        ivFriend = findViewById(R.id.iv_friend);
        ivProfile = findViewById(R.id.iv_profile);
        ivLogout = findViewById(R.id.iv_logout);
        
        tvPoetryQueryLabel = findViewById(R.id.tv_poetry_query_label);
        tvFriendLabel = findViewById(R.id.tv_friend_label);
        tvProfileLabel = findViewById(R.id.tv_profile_label);
        tvLogoutLabel = findViewById(R.id.tv_logout_label);
        
        // 默认显示顶部和底部栏
        showTopBar(true);
        showBottomBar(true);
        
        // 初始化底栏高亮
        updateBottomBarSelection();
    }
    
    /**
     * 控制顶部栏的显示/隐藏
     * @param show true显示，false隐藏
     */
    protected void showTopBar(boolean show) {
        if (topBar != null) {
            topBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * 控制底部栏的显示/隐藏
     * @param show true显示，false隐藏
     */
    protected void showBottomBar(boolean show) {
        if (bottomBar != null) {
            bottomBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * 加载用户信息到顶部栏
     */
    private void loadUserInfoToTopBar() {
        String username = SharedPrefsUtil.getUsername(this);
        String userId = SharedPrefsUtil.getUserId(this);
        int totalScore = SharedPrefsUtil.getTotalScore(this);
        
        if (tvUsername != null) {
            tvUsername.setText(username != null && !username.isEmpty() ? username : "未登录");
            
            // 检查Token有效性
            String token = SharedPrefsUtil.getString(this, "token");
            if (token == null || token.isEmpty()) {
                android.util.Log.w("BaseActivity", "Token未设置，跳过加载用户信息");
                return;
            }
        }
        
        if (tvUserId != null) {
            tvUserId.setText(userId != null && !userId.isEmpty() ? "ID: " + userId : "ID: --");
        }
        
        if (tvTotalScore != null) {
            tvTotalScore.setText("积分: " + totalScore);
        }
        
        // 如果身份码为空，尝试从后端获取
        if ((userId == null || userId.isEmpty()) && tvUserId != null) {
            fetchUserInfoFromServer();
        }
    }
    
    /**
     * 从后端获取用户信息
     */
    private void fetchUserInfoFromServer() {
        OkHttpUtil.get(ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (tvUserId != null) {
                        tvUserId.setText("ID: 错误");
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    try {
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(result).getAsJsonObject();
                        String identityCode = null;
                        
                        if (jsonObject.has("data") && jsonObject.get("data").isJsonObject()) {
                            com.google.gson.JsonObject data = jsonObject.getAsJsonObject("data");
                            if (data.has("identityCode")) {
                                identityCode = data.get("identityCode").getAsString();
                            }
                        } else if (jsonObject.has("identityCode")) {
                            identityCode = jsonObject.get("identityCode").getAsString();
                        }
                        
                        final String finalIdentityCode = identityCode;
                        
                        if (finalIdentityCode != null && !finalIdentityCode.isEmpty()) {
                            SharedPrefsUtil.saveUserId(BaseActivity.this, finalIdentityCode);
                            runOnUiThread(() -> {
                                if (tvUserId != null) {
                                    tvUserId.setText("ID: " + finalIdentityCode);
                                }
                            });
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        });
    }
    
    /**
     * 设置底部栏点击事件
     */
    private void setupBottomBarClicks() {
        // 为底部栏项添加点击反馈效果
        setupBottomBarClickEffect(btnPoetryQuery);
        setupBottomBarClickEffect(btnProfile);
        setupBottomBarClickEffect(btnFriend);
        setupBottomBarClickEffect(btnLogout);
        
        // 诗词查询
        if (btnPoetryQuery != null) {
            btnPoetryQuery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioController.getInstance().playSound(AudioController.SOUND_NAV);
                    navigateTo(PoetryQueryActivity.class);
                }
            });
        }
        
        // 个人信息
        if (btnProfile != null) {
            btnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioController.getInstance().playSound(AudioController.SOUND_NAV);
                    navigateTo(ProfileActivity.class);
                }
            });
        }
        
        // 好友列表（已废弃，使用FriendFragment）
        if (btnFriend != null) {
            btnFriend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioController.getInstance().playSound(AudioController.SOUND_NAV);
                    Toast.makeText(BaseActivity.this, "好友功能请在主页使用", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 退出登录
        if (btnLogout != null) {
            btnLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AudioController.getInstance().playSound(AudioController.SOUND_NAV);
                    SharedPrefsUtil.clearUser(BaseActivity.this);
                    Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
    
    /**
     * 为底部栏项添加点击效果
     */
    private void setupBottomBarClickEffect(LinearLayout layout) {
        if (layout == null) return;
        
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.7f);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1.0f);
                        break;
                }
                return false;
            }
        });
    }
    
    /**
     * 导航到指定Activity（避免重复打开当前页面）
     * @param targetClass 目标Activity类
     */
    protected void navigateTo(Class<?> targetClass) {
        if (this.getClass() == targetClass) {
            return; // 如果已经是当前页面，不执行跳转
        }
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
    }
    
    /**
     * 更新底部导航栏选中状态
     */
    protected void updateBottomBarSelection() {
        // 重置所有文字颜色、字体和图标
        int defaultColor = getResources().getColor(R.color.poetry_text_primary);
        int selectedColor = getResources().getColor(R.color.poetry_primary);
        
        // 重置所有图标为空心
        if (ivPoetryQuery != null) ivPoetryQuery.setImageResource(R.drawable.ic_article_outline);
        if (ivFriend != null) ivFriend.setImageResource(R.drawable.ic_people_outline);
        if (ivProfile != null) ivProfile.setImageResource(R.drawable.ic_book_outline);
        if (ivLogout != null) ivLogout.setImageResource(R.drawable.ic_logout);
        
        // 重置所有文字
        if (tvPoetryQueryLabel != null) {
            tvPoetryQueryLabel.setTextColor(defaultColor);
            tvPoetryQueryLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tvFriendLabel != null) {
            tvFriendLabel.setTextColor(defaultColor);
            tvFriendLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tvProfileLabel != null) {
            tvProfileLabel.setTextColor(defaultColor);
            tvProfileLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tvLogoutLabel != null) {
            tvLogoutLabel.setTextColor(getResources().getColor(R.color.loss_red));
            tvLogoutLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        
        // 根据当前Activity高亮对应项
        if (this instanceof PoetryQueryActivity && ivPoetryQuery != null) {
            ivPoetryQuery.setImageResource(R.drawable.ic_article_filled);
            tvPoetryQueryLabel.setTextColor(selectedColor);
            tvPoetryQueryLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            android.util.Log.d("BaseActivity", "高亮: 诗词查询");
        } else if (this instanceof ProfileActivity && ivProfile != null) {
            ivProfile.setImageResource(R.drawable.ic_book_filled);
            tvProfileLabel.setTextColor(selectedColor);
            tvProfileLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            android.util.Log.d("BaseActivity", "高亮: 个人信息");
        }
    }
    
    /**
     * 刷新顶部栏用户信息（子类可在数据更新后调用）
     */
    protected void refreshUserInfo() {
        loadUserInfoToTopBar();
    }
}