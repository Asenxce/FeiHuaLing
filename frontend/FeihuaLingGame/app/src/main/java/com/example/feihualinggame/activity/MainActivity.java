package com.example.feihualinggame.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.FeihuaLingGameApplication;
import com.example.feihualinggame.R;
import com.example.feihualinggame.adapter.MailAdapter;
import com.example.feihualinggame.bean.Mail;
import com.example.feihualinggame.bean.Poetry;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.fragment.CollectionFragment;
import com.example.feihualinggame.fragment.FriendFragment;
import com.example.feihualinggame.fragment.HomeFragment;
import com.example.feihualinggame.fragment.PoetryQueryFragment;
import com.example.feihualinggame.fragment.ProfileFragment;
import com.example.feihualinggame.utils.AvatarManager;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.HeartbeatManager;
import com.example.feihualinggame.utils.NetworkUtil;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.PoetryCollectionManager;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.WebSocketClient;
import com.example.feihualinggame.bean.WebSocketMessageBean;

import com.example.feihualinggame.utils.SystemUIUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 主页面 - 游戏入口（使用Fragment实现底部导航）
 */
public class MainActivity extends AppCompatActivity {
    private ImageView btnUserAvatar;  // 用户头像（点击跳转个人信息详细页）
    private ImageView btnSettings;     // 菜单按钮
    private LinearLayout dropdownMenu; // 下拉菜单
    private LinearLayout menuMailbox;  // 邮箱菜单项
    private LinearLayout menuDailySign; // 每日好签菜单项
    private LinearLayout menuSettings;  // 系统设置菜单项
    private LinearLayout menuLogout;   // 退出登录菜单项
    private TextView badgeUnread;      // 未读消息角标
    private LinearLayout btnBattleHall;   // 对战大厅
    private LinearLayout btnPoetryQuery;  // 诗词查询
    private LinearLayout btnFriend;       // 好友列表
    private LinearLayout btnProfile;      // 个人信息
    private com.google.gson.Gson gson = new com.google.gson.Gson();
    private WebSocketClient.MessageCallback wsCallback;
    private LinearLayout topBar;          // 顶部栏
    private RelativeLayout mainContainer; // 主容器
    
    // 底部导航栏图标
    private ImageView ivBattleHall;
    private ImageView ivPoetryQuery;
    private ImageView ivFriend;
    private ImageView ivProfile;
    
    // 底部导航栏文字标签
    private TextView tvBattleHallLabel;
    private TextView tvPoetryQueryLabel;
    private TextView tvFriendLabel;
    private TextView tvProfileLabel;
    
    // 邮箱相关
    private View mailboxOverlay;           // 邮箱遮罩层
    private LinearLayout mailboxPanel;     // 邮箱面板
    private RecyclerView rvMailList;       // 邮件列表
    private LinearLayout tvMailEmpty;      // 空状态提示
    private Button btnMailboxBack;         // 邮箱返回按钮
    private TextView btnClearRead;         // 清空已读按钮
    
    // 邮件详情相关
    private View mailDetailOverlay;        // 邮件详情遮罩层
    private ScrollView mailDetailPanel;    // 邮件详情面板
    private TextView tvMailTitle;          // 邮件标题
    private TextView tvMailSender;         // 发件人
    private TextView tvMailTime;           // 发送时间
    private TextView tvMailContent;        // 邮件内容
    private Button btnDeleteMail;          // 删除按钮
    private Button btnMailDetailBack;      // 详情返回按钮
    
    // 数据
    private List<Mail> mailList;           // 邮件列表
    private MailAdapter mailAdapter;       // 邮件适配器
    private Mail currentMail;              // 当前查看的邮件
    private int currentMailPosition = -1;  // 当前邮件位置
    
    // Fragment管理
    private FragmentManager fragmentManager;
    private HomeFragment homeFragment;
    private PoetryQueryFragment poetryQueryFragment;
    private FriendFragment friendFragment;
    private ProfileFragment profileFragment;
    private CollectionFragment collectionFragment;
    private Fragment currentFragment;  // 当前显示的Fragment
    private AlertDialog exitDialog;  // 退出确认对话框
    
    // 抽屉相关
    private androidx.drawerlayout.widget.DrawerLayout drawerLayout;  // DrawerLayout容器
    private LinearLayout navView;  // 侧滑抽屉视图
    private ImageView drawerAvatar;  // 抽屉头像
    private static final int REQUEST_CODE_PROFILE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. 优先检查登录态（验证Token和用户ID）
        String token = SharedPrefsUtil.getString(this, "token");
        String userId = SharedPrefsUtil.getUserId(this);
        
        if (token == null || token.isEmpty() || userId == null || userId.isEmpty()) {
            android.util.Log.w("MainActivity", "登录信息不完整，跳转登录页");
            // 清除残留数据
            SharedPrefsUtil.clearUser(this);
            
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // 2. 登录态有效，初始化统一音频控制器
        AudioController.getInstance().init(this);
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
        
        // 3. 设置布局
        setContentView(R.layout.activity_main_frame);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);
        
        // 设置沉浸式状态栏（状态栏图标变深色，背景色与顶栏一致）
        SystemUIUtil.setupImmersiveStatusBar(this, R.color.poetry_card_background, true);

        // 初始化控件
        btnUserAvatar = findViewById(R.id.btn_user_avatar);
        btnSettings = findViewById(R.id.btn_settings);
        dropdownMenu = findViewById(R.id.dropdown_menu);
        menuMailbox = findViewById(R.id.menu_mailbox);
        menuDailySign = findViewById(R.id.menu_daily_sign);
        menuSettings = findViewById(R.id.menu_settings);
        menuLogout = findViewById(R.id.menu_logout);
        badgeUnread = findViewById(R.id.badge_unread);
        btnBattleHall = findViewById(R.id.btn_battle_hall);
        btnPoetryQuery = findViewById(R.id.btn_poetry_query);
        btnFriend = findViewById(R.id.btn_friend);
        btnProfile = findViewById(R.id.btn_profile);
        
        // 底部导航栏图标
        ivBattleHall = findViewById(R.id.iv_battle_hall);
        ivPoetryQuery = findViewById(R.id.iv_poetry_query);
        ivFriend = findViewById(R.id.iv_friend);
        ivProfile = findViewById(R.id.iv_profile);
        
        // 底部导航栏文字标签
        tvBattleHallLabel = findViewById(R.id.tv_battle_hall_label);
        tvPoetryQueryLabel = findViewById(R.id.tv_poetry_query_label);
        tvFriendLabel = findViewById(R.id.tv_friend_label);
        tvProfileLabel = findViewById(R.id.tv_profile_label);
        
        // 顶部栏
        topBar = findViewById(R.id.top_bar);
        
        // 主容器（点击关闭下拉菜单）
        mainContainer = findViewById(R.id.main);
        if (mainContainer != null) {
            mainContainer.setOnClickListener(v -> {
                if (dropdownMenu != null && dropdownMenu.getVisibility() == View.VISIBLE) {
                    hideDropdownMenu();
                }
            });
        }
        
        // 设置头像点击事件（唤出侧滑抽屉）
        if (btnUserAvatar != null) {
            btnUserAvatar.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                openProfileDrawer();
            });
        }
        
        // 显示用户信息（目前暂不需要，后续开发头像功能时使用）
        // loadUserInfo();
        
        // 初始化Fragment管理
        fragmentManager = getSupportFragmentManager();
        initFragments();
        
        // 登录成功后弹出每日诗签（自动模式，受"今日不再显示"限制）
        showDailySign(false);
        
        // 初始化底栏高亮（默认选中对战大厅，索引0）
        updateBottomBarSelection(0);
        
        // 设置底部导航栏点击事件
        setupBottomNavigation();
        
        // 为底部导航栏添加交互反馈
        setupBottomNavigationEffect();
        
        // 设置下拉菜单
        setupDropdownMenu();
        
        // 每日好签点击事件
        if (menuDailySign != null) {
            menuDailySign.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                showDailySign(true); // 强制显示，忽略"今日不再显示"
                hideDropdownMenu();
            });
        }
        
        // 系统设置点击事件
        if (menuSettings != null) {
            menuSettings.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                hideDropdownMenu();
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
        
        // 初始化个人资料抽屉
        initProfileDrawer();
        
        // 加载抽屉头像
        loadDrawerAvatar();
    }
    
    /**
     * 初始化抽屉
     */
    private void initProfileDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        
        if (drawerLayout == null || navView == null) return;
        
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                float scale = 1f - 0.15f * slideOffset;
                mainContainer.setScaleX(scale);
                mainContainer.setScaleY(scale);
                mainContainer.setPivotX(0f);
                mainContainer.setPivotY(mainContainer.getHeight() / 2f);
                mainContainer.setTranslationX(300f * slideOffset * 0.3f);
            }

            @Override
            public void onDrawerOpened(View drawerView) {}

            @Override
            public void onDrawerClosed(View drawerView) {
                mainContainer.setScaleX(1f);
                mainContainer.setScaleY(1f);
                mainContainer.setTranslationX(0f);
            }

            @Override
            public void onDrawerStateChanged(int newState) {}
        });
        
        // 读取并显示用户身份码
        TextView tvDrawerUsername = findViewById(R.id.drawer_username);
        TextView tvDrawerIdentity = findViewById(R.id.drawer_identity);
        drawerAvatar = findViewById(R.id.drawer_avatar);
        if (tvDrawerUsername != null && tvDrawerIdentity != null) {
            String userId = SharedPrefsUtil.getUserId(this);
            String nickname = SharedPrefsUtil.getString(this, "nickname");
            tvDrawerUsername.setText(nickname != null && !nickname.isEmpty() ? nickname : "飞花令玩家");
            tvDrawerIdentity.setText("身份码：" + (userId != null ? userId : "---"));
        }
        
        // 抽屉菜单项点击事件
        LinearLayout menuProfile = findViewById(R.id.drawer_menu_profile);
        LinearLayout menuSettings = findViewById(R.id.drawer_menu_settings);
        LinearLayout menuRules = findViewById(R.id.drawer_menu_rules);
        LinearLayout menuMailbox = findViewById(R.id.drawer_menu_mailbox);
        LinearLayout menuLogout = findViewById(R.id.drawer_menu_logout);
        LinearLayout menuRecords = findViewById(R.id.drawer_menu_records);
        
        if (menuProfile != null) {
            menuProfile.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                drawerLayout.closeDrawers();
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivityForResult(intent, REQUEST_CODE_PROFILE);
            });
        }
        
        if (menuSettings != null) {
            menuSettings.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                drawerLayout.closeDrawers();
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
        
        if (menuRules != null) {
            menuRules.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                drawerLayout.closeDrawers();
                Intent intent = new Intent(MainActivity.this, RulesActivity.class);
                startActivity(intent);
            });
        }

        if (menuMailbox != null) {
            menuMailbox.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                drawerLayout.closeDrawers();
                Intent intent = new Intent(MainActivity.this, MailboxActivity.class);
                startActivity(intent);
            });
        }

        if (menuRecords != null) {
            menuRecords.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                drawerLayout.closeDrawers();
                Intent intent = new Intent(MainActivity.this, RecordActivity.class);
                startActivity(intent);
            });
        }
        
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                drawerLayout.closeDrawers();
                showLogoutDialog();
            });
        }

        // 建立 WebSocket 连接以接收房间邀请
        connectWebSocketForInvite();
    }

    /**
     * 建立 WebSocket 连接，监听房间邀请
     */
    private void connectWebSocketForInvite() {
        long numericUserId = SharedPrefsUtil.getLong(this, "numeric_user_id", -1);
        if (numericUserId <= 0) {
            // 首次登录可能还未保存，尝试从服务器获取
            fetchUserIdAndConnect();
            return;
        }
        setupWebSocketCallback(numericUserId);
    }

    /**
     * 从服务器获取数字 userId 后建立 WebSocket
     */
    private void fetchUserIdAndConnect() {
        OkHttpUtil.getWithAuth(this, ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    JsonObject data = json.has("data") && json.get("data").isJsonObject()
                            ? json.getAsJsonObject("data") : json;
                    if (data.has("id")) {
                        long userId = data.get("id").getAsLong();
                        SharedPrefsUtil.saveLong(MainActivity.this, "numeric_user_id", userId);
                        runOnUiThread(() -> setupWebSocketCallback(userId));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupWebSocketCallback(long numericUserId) {
        if (wsCallback != null) {
            WebSocketClient.getInstance().removeCallback(wsCallback);
        }
        wsCallback = new WebSocketClient.MessageCallback() {
            @Override
            public void onConnected() {
                // 连接成功后注册用户
                WebSocketClient.getInstance().register(numericUserId);
            }
            @Override
            public void onDisconnected() {}
            @Override
            public void onMessage(WebSocketMessageBean message) {
                if ("INVITE_RECEIVED".equals(message.getType())) {
                    handleInviteReceived(message.getPayload());
                }
            }
            @Override
            public void onError(String error) {}
        };
        WebSocketClient.getInstance().addCallback(wsCallback);

        // 如果尚未连接，发起连接；已连接则直接注册
        if (!WebSocketClient.getInstance().isConnected()) {
            WebSocketClient.getInstance().connect(ApiConstant.WS_URL);
        } else {
            WebSocketClient.getInstance().register(numericUserId);
        }
    }

    /**
     * 处理收到的房间邀请
     */
    private void handleInviteReceived(String payload) {
        try {
            JsonObject inviteData = gson.fromJson(payload, JsonObject.class);
            String inviterName = inviteData.has("inviterName") ? inviteData.get("inviterName").getAsString() : "未知用户";
            String inviteCode = inviteData.has("roomCode") ? inviteData.get("roomCode").getAsString() : "";
            String inviteToken = inviteData.has("inviteToken") ? inviteData.get("inviteToken").getAsString() : "";

            runOnUiThread(() -> showInviteDialog(inviterName, inviteCode, inviteToken));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示房间邀请弹窗
     */
    private void showInviteDialog(String inviterName, String roomCode, String inviteToken) {
        new AlertDialog.Builder(this)
            .setTitle("收到房间邀请")
            .setMessage(inviterName + " 邀请你加入多人对战房间\n房间码: " + roomCode)
            .setPositiveButton("接受", (dialog, which) -> acceptInviteAndJoin(inviteToken))
            .setNegativeButton("拒绝", (dialog, which) -> {
                // 静默拒绝
            })
            .setCancelable(true)
            .show();
    }

    /**
     * 接受邀请并跳转到房间
     */
    private void acceptInviteAndJoin(String inviteToken) {
        OkHttpUtil.postWithAuth(this, "room/invite/" + inviteToken + "/accept", "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "加入房间失败", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.has("code") && json.get("code").getAsInt() == 200) {
                            JsonObject data = json.has("data") ? json.getAsJsonObject("data") : json;
                            String roomId = data.has("roomId") ? data.get("roomId").getAsString() : "";
                            String roomCode = data.has("roomCode") ? data.get("roomCode").getAsString() : "";

                            Intent intent = new Intent(MainActivity.this, MultiRoomActivity.class);
                            intent.putExtra("roomId", roomId);
                            intent.putExtra("roomCode", roomCode);
                            intent.putExtra("isCreator", false);
                            startActivity(intent);
                        } else {
                            String msg = json.has("message") ? json.get("message").getAsString() : "加入房间失败";
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "加入房间失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wsCallback != null) {
            WebSocketClient.getInstance().removeCallback(wsCallback);
        }
    }

    /**
     * 打开抽屉
     */
    private void openProfileDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(navView);
        }
    }
    
    /**
     * 关闭抽屉
     */
    private void closeProfileDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }
    
    /**
     * 刷新抽屉数据
     */
    private void refreshDrawerData() {
        TextView tvDrawerUsername = findViewById(R.id.drawer_username);
        TextView tvDrawerIdentity = findViewById(R.id.drawer_identity);
        if (tvDrawerUsername != null) {
            String nickname = SharedPrefsUtil.getString(this, "nickname");
            tvDrawerUsername.setText(nickname != null && !nickname.isEmpty() ? nickname : "飞花令玩家");
        }
        if (tvDrawerIdentity != null) {
            String userId = SharedPrefsUtil.getUserId(this);
            tvDrawerIdentity.setText("身份码：" + (userId != null ? userId : "---"));
        }
        // 刷新头像
        if (drawerAvatar != null) {
            String avatarBase64 = SharedPrefsUtil.getString(this, "user_avatar_base64");
            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                try {
                    byte[] bytes = android.util.Base64.decode(avatarBase64, android.util.Base64.NO_WRAP);
                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bmp != null) drawerAvatar.setImageBitmap(bmp);
                } catch (Exception e) { e.printStackTrace(); }
            } else {
                drawerAvatar.setImageResource(R.drawable.default_avatar);
            }
        }
    }

    /**
     * 从后端加载用户信息并刷新UI
     */
    private void loadUserInfoFromServer() {
        String token = SharedPrefsUtil.getString(this, "token");
        if (token == null || token.isEmpty()) {
            return; // 未登录，不加载
        }

        OkHttpUtil.getWithAuth(this, ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("MainActivity", "加载用户信息失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().string();
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        
                        if (jsonObject.has("data") && jsonObject.get("data").isJsonObject()) {
                            com.google.gson.JsonObject data = jsonObject.getAsJsonObject("data");
                            
                            // 提取昵称
                            if (data.has("nickname")) {
                                final String nickname = data.get("nickname").getAsString();
                                
                                // 保存到本地
                                if (nickname != null && !nickname.isEmpty()) {
                                    SharedPrefsUtil.saveString(MainActivity.this, "nickname", nickname);
                                    
                                    // 更新UI
                                    runOnUiThread(() -> {
                                        TextView tvDrawerUsername = findViewById(R.id.drawer_username);
                                        if (tvDrawerUsername != null) {
                                            tvDrawerUsername.setText(nickname);
                                        }
                                        android.util.Log.d("MainActivity", "昵称已更新: " + nickname);
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "解析用户信息失败", e);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PROFILE && resultCode == RESULT_OK) {
            refreshDrawerData();
        }
    }
    
    /**
     * 重写返回键行为：显示退出确认对话框
     */
    @Override
    public void onBackPressed() {
        if (exitDialog != null && exitDialog.isShowing()) {
            // 对话框已显示，点击返回键关闭对话框
            exitDialog.dismiss();
        } else {
            // 显示退出确认对话框
            exitDialog = new AlertDialog.Builder(this)
                    .setTitle("退出游戏")
                    .setMessage("确定要退出飞花令吗？")
                    .setPositiveButton("退出", (dialog, which) -> {
                        finishAffinity();
                    })
                    .setNegativeButton("取消", null)
                    .create();
            exitDialog.show();
        }
    }
    
    /**
     * 显示每日诗签（登录时自动弹出或手动打开）
     * @param forceShow true=强制显示（菜单调用），false=受“今日不再显示”限制（登录自动）
     */
    public void showDailySign(boolean forceShow) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long today = java.time.LocalDate.now().toEpochDay();
            
        // 检查今日是否已勾选"不再显示"（仅在非强制模式下检查）
        if (!forceShow) {
            long noShowDate = prefs.getLong("daily_sign_no_show_date", 0);
                
            if (noShowDate == today) {
                return; // 今日已勾选不再显示
            }
        }
            
        // 检查今日缓存（无论是否强制显示都检查，确保每天只显示同一首诗）
        long todayPoetryId = prefs.getLong("today_poetry_id", -1);
        String todayPoetryJson = prefs.getString("today_poetry_json", null);
        long savedDate = prefs.getLong("today_poetry_date", 0);
        
        android.util.Log.d("MainActivity", "缓存检查 - today: " + today + ", savedDate: " + savedDate + ", hasJson: " + (todayPoetryJson != null));
            
        if (savedDate == today && todayPoetryJson != null && !todayPoetryJson.isEmpty()) {
            try {
                JsonObject poetryJson = JsonParser.parseString(todayPoetryJson).getAsJsonObject();
                final Poetry poetry = new Poetry();
                if (poetryJson.has("id")) {
                    poetry.setId(poetryJson.get("id").getAsLong());
                } else {
                    poetry.setId(todayPoetryId);
                }
                if (poetryJson.has("title")) poetry.setTitle(poetryJson.get("title").getAsString());
                if (poetryJson.has("author")) poetry.setAuthor(poetryJson.get("author").getAsString());
                if (poetryJson.has("dynasty")) poetry.setDynasty(poetryJson.get("dynasty").getAsString());
                if (poetryJson.has("fullContent")) {
                    String fullContent = poetryJson.get("fullContent").getAsString();
                    poetry.setFullContent(fullContent);
                    poetry.setContent(fullContent);
                } else if (poetryJson.has("content")) {
                    String content = poetryJson.get("content").getAsString();
                    poetry.setContent(content);
                    poetry.setFullContent(content);
                }
                    
                runOnUiThread(() -> showDailySignDialog(prefs, today, poetry));
                return;
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "解析缓存诗词失败: " + e.getMessage());
            }
        }
            
        prefs.edit().remove("today_poetry_id").remove("today_poetry_json").remove("today_poetry_date").apply();

        // 从后端获取随机诗词
        fetchRandomPoetry(prefs, today);
    }
        
    /**
     * 从后端随机获取一首诗词
     */
    private void fetchRandomPoetry(SharedPreferences prefs, long today) {
        OkHttpUtil.get(ApiConstant.POETRY_RANDOM, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("DailySign", "获取每日好签失败", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "加载失败", Toast.LENGTH_SHORT).show());
            }
    
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    android.util.Log.e("DailySign", "获取每日好签失败: HTTP " + response.code());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "加载失败", Toast.LENGTH_SHORT).show());
                    return;
                }
                    
                String responseBody = response.body().string();
                android.util.Log.d("MainActivity", "每日好签响应: " + responseBody);
                try {
                    JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonObject poetryJson = root.has("data") ? root.getAsJsonObject("data") : root;
                    
                    final Poetry poetry = new Poetry();
                    if (poetryJson.has("id")) {
                        poetry.setId(poetryJson.get("id").getAsLong());
                    } else {
                        poetry.setId(System.currentTimeMillis());
                    }
                    if (poetryJson.has("title")) poetry.setTitle(poetryJson.get("title").getAsString());
                    if (poetryJson.has("author")) poetry.setAuthor(poetryJson.get("author").getAsString());
                    if (poetryJson.has("dynasty")) poetry.setDynasty(poetryJson.get("dynasty").getAsString());
                    if (poetryJson.has("fullContent")) {
                        String fullContent = poetryJson.get("fullContent").getAsString();
                        poetry.setFullContent(fullContent);
                        poetry.setContent(fullContent);
                    } else if (poetryJson.has("content")) {
                        String content = poetryJson.get("content").getAsString();
                        poetry.setContent(content);
                        poetry.setFullContent(content);
                    }
                                            
                    prefs.edit()
                        .putLong("today_poetry_id", poetry.getId())
                        .putString("today_poetry_json", poetryJson.toString())
                        .putLong("today_poetry_date", today)
                        .apply();
                        
                    runOnUiThread(() -> showDailySignDialog(prefs, today, poetry));
                        
                } catch (Exception e) {
                    android.util.Log.e("DailySign", "解析失败", e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "解析失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
        
    private void refreshDailySignPoetry(SharedPreferences prefs, long today,
            TextView tvTitle, TextView tvAuthor, TextView tvDynasty,
            TextView tvContent, ScrollView scrollContent, ImageView ivFavorite) {
        android.util.Log.d("DailySign", "开始刷新诗签，API地址: " + ApiConstant.POETRY_RANDOM);
        String url = ApiConstant.POETRY_RANDOM + "?_t=" + System.currentTimeMillis();
        OkHttpUtil.get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("DailySign", "刷新诗签失败: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(MainActivity.this, "刷新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
    
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                android.util.Log.d("DailySign", "刷新诗签响应: " + response.code() + " " + responseBody);
                
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(MainActivity.this, "刷新失败: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                    
                try {
                    JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonObject poetryJson = root.has("data") ? root.getAsJsonObject("data") : root;
                    
                    final Poetry poetry = new Poetry();
                    if (poetryJson.has("id")) {
                        poetry.setId(poetryJson.get("id").getAsLong());
                    } else {
                        poetry.setId(System.currentTimeMillis());
                    }
                    if (poetryJson.has("title")) poetry.setTitle(poetryJson.get("title").getAsString());
                    if (poetryJson.has("author")) poetry.setAuthor(poetryJson.get("author").getAsString());
                    if (poetryJson.has("dynasty")) poetry.setDynasty(poetryJson.get("dynasty").getAsString());
                    if (poetryJson.has("fullContent")) {
                        String fullContent = poetryJson.get("fullContent").getAsString();
                        poetry.setFullContent(fullContent);
                        poetry.setContent(fullContent);
                    } else if (poetryJson.has("content")) {
                        String content = poetryJson.get("content").getAsString();
                        poetry.setContent(content);
                        poetry.setFullContent(content);
                    }

                    prefs.edit()
                        .putLong("today_poetry_id", poetry.getId())
                        .putString("today_poetry_json", poetryJson.toString())
                        .putLong("today_poetry_date", today)
                        .apply();
                        
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        
                        tvTitle.setText("《" + poetry.getTitle() + "》");
                        tvAuthor.setText(poetry.getAuthor());
                        tvDynasty.setText(poetry.getDynasty());

                        String displayContent = poetry.getFullContent() != null ? poetry.getFullContent() : poetry.getContent();
                        String formattedContent = formatSignPoetryContent(displayContent);
                        tvContent.setText(formattedContent);

                        int contentLength = formattedContent.length();
                        float fontSize;
                        if (contentLength <= 30) {
                            fontSize = 22;
                        } else if (contentLength <= 60) {
                            fontSize = 17;
                        } else {
                            fontSize = 14;
                        }
                        tvContent.setTextSize(fontSize);

                        if (scrollContent != null) {
                            tvContent.measure(0, 0);
                            int lineCount = tvContent.getLineCount();
                            if (lineCount <= 0) lineCount = 1;
                            float lineHeightPx = fontSize * getResources().getDisplayMetrics().scaledDensity;
                            int contentHeight = (int) (lineCount * lineHeightPx + 30 * getResources().getDisplayMetrics().density);
                            int maxHeight = (int) (160 * getResources().getDisplayMetrics().density);
                            int finalHeight = Math.min(contentHeight, maxHeight);
                            int minHeight = (int) (80 * getResources().getDisplayMetrics().density);
                            finalHeight = Math.max(finalHeight, minHeight);
                            scrollContent.getLayoutParams().height = finalHeight;
                            scrollContent.requestLayout();
                        }

                        final boolean[] isCollected = {PoetryCollectionManager.isCollectedByContent(MainActivity.this, poetry)};
                        updateFavoriteIcon(ivFavorite, isCollected[0]);
                        ivFavorite.setOnClickListener(v -> {
                            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                            if (isCollected[0]) {
                                PoetryCollectionManager.removeFromCollectionAsync(MainActivity.this, poetry.getId(), new PoetryCollectionManager.OnCollectionChangeListener() {
                                    @Override
                                    public void onResult(boolean success, String message) {
                                        runOnUiThread(() -> {
                                            if (success) {
                                                isCollected[0] = false;
                                                updateFavoriteIcon(ivFavorite, false);
                                            }
                                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            } else {
                                PoetryCollectionManager.addToCollectionAsync(MainActivity.this, poetry, new PoetryCollectionManager.OnCollectionChangeListener() {
                                    @Override
                                    public void onResult(boolean success, String message) {
                                        runOnUiThread(() -> {
                                            if (success) {
                                                isCollected[0] = true;
                                                updateFavoriteIcon(ivFavorite, true);
                                            }
                                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            }
                        });
                        
                        Toast.makeText(MainActivity.this, "已刷新", Toast.LENGTH_SHORT).show();
                    });
                        
                } catch (Exception e) {
                    android.util.Log.e("DailySign", "刷新解析失败", e);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(MainActivity.this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
        
    /**
     * 显示每日诗签对话框
     */
    private void showDailySignDialog(SharedPreferences prefs, long today, Poetry poetry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_daily_sign, null);
        builder.setView(dialogView);
            
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                
        // 设置弹窗宽度为屏幕85%，高度自适应（wrap_content）
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        params.width = (int)(metrics.widthPixels * 0.85f);
        params.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);
            
        TextView tvTitle = dialogView.findViewById(R.id.tv_sign_title);
        TextView tvAuthor = dialogView.findViewById(R.id.tv_sign_author);
        TextView tvDynasty = dialogView.findViewById(R.id.tv_sign_dynasty);
        TextView tvContent = dialogView.findViewById(R.id.tv_sign_content);
        ScrollView scrollContent = dialogView.findViewById(R.id.scroll_content);
        ImageView ivFavorite = dialogView.findViewById(R.id.iv_sign_favorite);
        ImageView ivRefresh = dialogView.findViewById(R.id.iv_sign_refresh);
        CheckBox cbNoShow = dialogView.findViewById(R.id.cb_no_show_today);
        Button btnClose = dialogView.findViewById(R.id.btn_sign_close);
        
        // 动态限制 ScrollView 最大高度为 160dp
        if (scrollContent != null) {
            android.util.DisplayMetrics scrollMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(scrollMetrics);
            int maxHeight = (int) (160 * scrollMetrics.density);
            scrollContent.getLayoutParams().height = maxHeight;
            scrollContent.requestLayout();
        }
            
        tvTitle.setText("《" + poetry.getTitle() + "》");
        tvAuthor.setText(poetry.getAuthor());
        tvDynasty.setText(poetry.getDynasty());
            
        String displayContent = poetry.getFullContent() != null ? poetry.getFullContent() : poetry.getContent();
        String formattedContent = formatSignPoetryContent(displayContent);
        tvContent.setText(formattedContent);
        
        // 根据诗词内容长度动态调整字体大小
        int contentLength = formattedContent.length();
        float fontSize;
        if (contentLength <= 30) {
            fontSize = 22; // 短诗词放大
        } else if (contentLength <= 60) {
            fontSize = 17; // 中等诗词默认
        } else {
            fontSize = 14; // 长诗词缩小
        }
        tvContent.setTextSize(fontSize);
        
        // 重新调整 ScrollView 高度，适配字体变化
        if (scrollContent != null) {
            tvContent.measure(0, 0);
            int lineCount = tvContent.getLineCount();
            if (lineCount <= 0) lineCount = 1;
            
            // 估算行高 (sp 转 px)
            float lineHeightPx = fontSize * getResources().getDisplayMetrics().scaledDensity;
            // 计算内容高度 + padding (约 30dp)
            int contentHeight = (int) (lineCount * lineHeightPx + 30 * getResources().getDisplayMetrics().density);
            
            // 限制最大高度 160dp
            int maxHeight = (int) (160 * getResources().getDisplayMetrics().density);
            int finalHeight = Math.min(contentHeight, maxHeight);
            
            // 限制最小高度 80dp
            int minHeight = (int) (80 * getResources().getDisplayMetrics().density);
            finalHeight = Math.max(finalHeight, minHeight);
            
            scrollContent.getLayoutParams().height = finalHeight;
            scrollContent.requestLayout();
        }
            
        // 检查是否已收藏（按标题+作者匹配，避免ID不稳定）
        final boolean[] isCollected = {PoetryCollectionManager.isCollectedByContent(MainActivity.this, poetry)};
        android.util.Log.d("DailySign", "诗词ID: " + poetry.getId() + ", 标题: " + poetry.getTitle() + ", 作者: " + poetry.getAuthor() + ", 已收藏: " + isCollected[0]);
        updateFavoriteIcon(ivFavorite, isCollected[0]);
            
        // 收藏按钮点击（仿照 PoetryDetailActivity 逻辑）
        ivFavorite.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            if (isCollected[0]) {
                // 取消收藏
                PoetryCollectionManager.removeFromCollectionAsync(MainActivity.this, poetry.getId(), new PoetryCollectionManager.OnCollectionChangeListener() {
                    @Override
                    public void onResult(boolean success, String message) {
                        runOnUiThread(() -> {
                            if (success) {
                                isCollected[0] = false;
                                updateFavoriteIcon(ivFavorite, false);
                            }
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                // 添加收藏
                PoetryCollectionManager.addToCollectionAsync(MainActivity.this, poetry, new PoetryCollectionManager.OnCollectionChangeListener() {
                    @Override
                    public void onResult(boolean success, String message) {
                        runOnUiThread(() -> {
                            if (success) {
                                isCollected[0] = true;
                                updateFavoriteIcon(ivFavorite, true);
                            }
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
            
        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> {
                android.util.Log.d("DailySign", "刷新按钮被点击");
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                Toast.makeText(MainActivity.this, "正在换一首...", Toast.LENGTH_SHORT).show();
                refreshDailySignPoetry(prefs, today, tvTitle, tvAuthor, tvDynasty, tvContent, scrollContent, ivFavorite);
            });
        }

        btnClose.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                
            // 处理"今日不再显示"逻辑
            if (cbNoShow.isChecked()) {
                prefs.edit().putLong("daily_sign_no_show_date", today).apply();
            }
                
            dialog.dismiss();
        });
            
        dialog.show();
    }
    
    /**
     * 格式化诗签诗词内容：每两个半句合并为一整句后换行
     */
    private String formatSignPoetryContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        StringBuilder sb = new StringBuilder();
        int halfLineCount = 0; // 半句计数
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            sb.append(c);
            
            // 检测到逗号或句号/叹号/问号，计为一个半句
            boolean isPunct = (c == '，' || c == ',' || c == '。' || c == '.' || c == '！' || c == '!' || c == '？' || c == '?');
            if (isPunct) {
                halfLineCount++;
                
                // 每两个半句换行一次，且后面还有内容时换行
                boolean hasNext = (i < content.length() - 1);
                if (halfLineCount % 2 == 0 && hasNext) {
                    sb.append('\n');
                }
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 更新收藏图标状态（仿照 PoetryDetailActivity 逻辑）
     */
    private void updateFavoriteIcon(ImageView ivFavorite, boolean isCollected) {
        if (isCollected) {
            ivFavorite.setImageResource(R.drawable.ic_star_filled);
            ivFavorite.setColorFilter(getResources().getColor(R.color.poetry_primary));
        } else {
            ivFavorite.setImageResource(R.drawable.ic_star_outline);
            ivFavorite.setColorFilter(getResources().getColor(R.color.poetry_text_secondary));
        }
    }
    
    /**
     * 初始化Fragment
     */
    private void initFragments() {
        homeFragment = new HomeFragment();
        poetryQueryFragment = new PoetryQueryFragment();
        friendFragment = new FriendFragment();
        profileFragment = new ProfileFragment();
        collectionFragment = new CollectionFragment();
        
        // 默认显示主页
        showFragment(homeFragment);
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("MainActivity", "onResume 被调用");
        
        // 异步检查网络可用性，避免阻塞主线程
        checkNetworkAsync();
        
        // 刷新未读邮件角标
        updateUnreadBadge();
        // 刷新抽屉头像
        android.util.Log.d("MainActivity", "准备调用 loadDrawerAvatar()");
        loadDrawerAvatar();
        // 刷新顶栏头像
        loadTopBarAvatar();
        // 从后端加载用户信息并刷新昵称
        loadUserInfoFromServer();
    }
    
    /**
     * 异步检查网络可用性
     */
    private void checkNetworkAsync() {
        new Thread(() -> {
            boolean isAvailable = NetworkUtil.isNetworkReallyAvailable(this);
            runOnUiThread(() -> {
                if (!isAvailable) {
                    new AlertDialog.Builder(this)
                            .setTitle("网络错误")
                            .setMessage("网络连接异常，请检查网络设置后重试。")
                            .setCancelable(false)
                            .setPositiveButton("退出", (dialog, which) -> finishAffinity())
                            .show();
                }
            });
        }).start();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 注释掉此处暂停逻辑，实现跨页面背景音乐连续播放
        // BackgroundMusicManager.getInstance().pause();
    }
    
    /**
     * 显示指定的Fragment
     */
    private void showFragment(Fragment fragment) {
        // 如果当前Fragment就是要显示的，直接返回
        if (currentFragment == fragment) {
            return;
        }
        
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // 隐藏当前Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }
        
        // 添加或显示目标Fragment
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment);
        } else {
            transaction.show(fragment);
        }
        
        transaction.commit();
        currentFragment = fragment;
    }
    
    /**
     * 设置底部导航栏点击事件
     */
    private void setupBottomNavigation() {
        // 对战大厅（默认显示）
        btnBattleHall.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_NAV);
            showFragment(homeFragment);
            updateBottomBarSelection(0);
        });
        
        // 诗词查询
        btnPoetryQuery.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_NAV);
            showFragment(poetryQueryFragment);
            updateBottomBarSelection(1);
        });
        
        // 好友列表
        btnFriend.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_NAV);
            showFragment(friendFragment);
            updateBottomBarSelection(2);
        });
        
        // 经典收藏
        btnProfile.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_NAV);
            showFragment(collectionFragment);
            updateBottomBarSelection(3);
        });
    }
    
    /**
     * 更新底部导航栏选中状态
     * @param selectedIndex 选中的索引：0=对战大厅, 1=诗词查询, 2=收藏, 3=设置
     */
    private void updateBottomBarSelection(int selectedIndex) {
        // 重置所有文字颜色和字体，以及图标为空心
        int defaultColor = getResources().getColor(R.color.poetry_text_primary);
        int selectedColor = getResources().getColor(R.color.poetry_primary);
        
        // 重置所有图标为空心
        if (ivBattleHall != null) ivBattleHall.setImageResource(R.drawable.ic_battle_outline);
        if (ivPoetryQuery != null) ivPoetryQuery.setImageResource(R.drawable.ic_article_outline);
        if (ivFriend != null) ivFriend.setImageResource(R.drawable.ic_people_outline);
        if (ivProfile != null) ivProfile.setImageResource(R.drawable.ic_book_outline);
        
        // 重置所有文字
        if (tvBattleHallLabel != null) {
            tvBattleHallLabel.setTextColor(defaultColor);
            tvBattleHallLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
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
        
        // 根据选中索引高亮对应项（图标变实心 + 文字变紫+加粗）
        switch (selectedIndex) {
            case 0: // 对战大厅
                if (ivBattleHall != null) ivBattleHall.setImageResource(R.drawable.ic_battle_filled);
                if (tvBattleHallLabel != null) {
                    tvBattleHallLabel.setTextColor(selectedColor);
                    tvBattleHallLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case 1: // 诗词查询
                if (ivPoetryQuery != null) ivPoetryQuery.setImageResource(R.drawable.ic_article_filled);
                if (tvPoetryQueryLabel != null) {
                    tvPoetryQueryLabel.setTextColor(selectedColor);
                    tvPoetryQueryLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case 2: // 好友列表
                if (ivFriend != null) ivFriend.setImageResource(R.drawable.ic_people_filled);
                if (tvFriendLabel != null) {
                    tvFriendLabel.setTextColor(selectedColor);
                    tvFriendLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case 3: // 经典收藏
                if (ivProfile != null) ivProfile.setImageResource(R.drawable.ic_book_filled);
                if (tvProfileLabel != null) {
                    tvProfileLabel.setTextColor(selectedColor);
                    tvProfileLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
        }
        
        android.util.Log.d("MainActivity", "底栏选中: " + selectedIndex);
    }
    
    /**
     * 设置底部导航栏交互反馈
     */
    private void setupBottomNavigationEffect() {
        // 为每个底部导航项添加点击缩放效果
        View[] bottomItems = {btnBattleHall, btnPoetryQuery, btnFriend, btnProfile};
        for (View item : bottomItems) {
            com.example.feihualinggame.utils.ButtonAnimationHelper.addPressScaleEffect(item);
        }
    }
    
    /**
     * 设置下拉菜单
     */
    private void setupDropdownMenu() {
        // 设置按钮点击事件
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                showDropdownMenu();
            });
        }
        
        // 邮箱菜单项
        if (menuMailbox != null) {
            menuMailbox.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                hideDropdownMenu();
                Intent intent = new Intent(MainActivity.this, MailboxActivity.class);
                startActivity(intent);
            });
        }
        
        // 退出登录菜单项（下拉菜单）
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                hideDropdownMenu();
                showLogoutDialog();
            });
        }
        
        // 初始化未读邮件角标
        updateUnreadBadge();
    }
    
    /**
     * 更新未读邮件角标
     */
    private void updateUnreadBadge() {
        if (badgeUnread == null) return;
        
        boolean notificationEnabled = SharedPrefsUtil.getBoolean(this, "notification_enabled", true);
        if (!notificationEnabled) {
            badgeUnread.setVisibility(View.GONE);
            return;
        }
        
        String identityCode = SharedPrefsUtil.getUserId(this);
        if (identityCode == null || identityCode.isEmpty()) {
            badgeUnread.setVisibility(View.GONE);
            return;
        }
        
        String url = ApiConstant.MAIL_UNREAD_COUNT + "?identityCode=" + identityCode;
        OkHttpUtil.getWithAuth(this, url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> badgeUnread.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                        int unreadCount = 0;
                        if (jsonObject.get("code").getAsInt() == 200) {
                            unreadCount = jsonObject.get("data").getAsInt();
                        }
                        final int count = unreadCount;
                        runOnUiThread(() -> {
                            if (count > 0) {
                                badgeUnread.setVisibility(View.VISIBLE);
                                badgeUnread.setText(String.valueOf(count));
                            } else {
                                badgeUnread.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> badgeUnread.setVisibility(View.GONE));
                    }
                } else {
                    runOnUiThread(() -> badgeUnread.setVisibility(View.GONE));
                }
            }
        });
    }
    
    /**
     * 显示下拉菜单
     */
    private void showDropdownMenu() {
        if (dropdownMenu != null) {
            dropdownMenu.setVisibility(View.VISIBLE);
            // 添加出现动画
            dropdownMenu.setAlpha(0f);
            dropdownMenu.setTranslationY(-20f);
            dropdownMenu.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start();
        }
    }
    
    /**
     * 隐藏下拉菜单
     */
    private void hideDropdownMenu() {
        if (dropdownMenu != null) {
            dropdownMenu.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(150)
                .withEndAction(() -> {
                    dropdownMenu.setVisibility(View.GONE);
                })
                .start();
        }
    }
    
    /**
     * 显示退出登录确认对话框
     */
    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                // 调用后端退出登录接口，清除在线状态
                OkHttpUtil.postWithAuth(this, ApiConstant.LOGOUT, "{}", new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        android.util.Log.e("MainActivity", "退出登录请求失败", e);
                        // 即使请求失败也清除本地状态
                        performLogout();
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                        android.util.Log.d("MainActivity", "退出登录响应码: " + response.code());
                        // 无论成功失败都清除本地状态
                        performLogout();
                    }
                });
            })
            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
            .show();
    }
    
    /**
     * 执行退出登录逻辑
     */
    private void performLogout() {
        runOnUiThread(() -> {
            // 停止心跳
            HeartbeatManager.getInstance().stop();
            
            // 释放音频资源
            AudioController.getInstance().release();
            
            // 清除用户数据
            SharedPrefsUtil.clearUser(this);
            
            // 跳转到登录页面
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    /**
     * 加载抽屉头像
     */
    private void loadDrawerAvatar() {
        android.util.Log.d("MainActivity", "loadDrawerAvatar() 开始执行");
        
        if (drawerAvatar == null) {
            android.util.Log.e("MainActivity", "错误: drawerAvatar 为 null");
            return;
        }
        android.util.Log.d("MainActivity", "drawerAvatar 控件已找到");
        
        String base64Avatar = AvatarManager.getAvatarBase64(this);
        android.util.Log.d("MainActivity", "从 SharedPreferences 读取头像 - Base64长度: " + 
            (base64Avatar != null ? base64Avatar.length() : 0));
        
        if (base64Avatar != null && !base64Avatar.isEmpty()) {
            try {
                byte[] imageBytes = android.util.Base64.decode(base64Avatar, android.util.Base64.NO_WRAP);
                android.util.Log.d("MainActivity", "Base64 解码成功 - 字节数组长度: " + imageBytes.length);
                
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    android.util.Log.d("MainActivity", "Bitmap 解码成功 - 尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                    // 裁剪为圆形
                    android.graphics.Bitmap circleBitmap = getCircleBitmap(bitmap);
                    drawerAvatar.setImageBitmap(circleBitmap);
                    android.util.Log.d("MainActivity", "头像已设置到 ImageView");
                } else {
                    android.util.Log.e("MainActivity", "错误: Bitmap 解码失败");
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "加载头像异常: " + e.getMessage(), e);
                e.printStackTrace();
            }
        } else {
            android.util.Log.w("MainActivity", "警告: SharedPreferences 中没有头像数据");
        }
    }
    
    /**
     * 加载顶栏头像
     */
    private void loadTopBarAvatar() {
        if (btnUserAvatar == null) {
            android.util.Log.e("MainActivity", "错误: btnUserAvatar 为 null");
            return;
        }
        
        String base64Avatar = AvatarManager.getAvatarBase64(this);
        if (base64Avatar != null && !base64Avatar.isEmpty()) {
            try {
                byte[] imageBytes = android.util.Base64.decode(base64Avatar, android.util.Base64.NO_WRAP);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    // 裁剪为圆形
                    android.graphics.Bitmap circleBitmap = getCircleBitmap(bitmap);
                    btnUserAvatar.setImageBitmap(circleBitmap);
                    android.util.Log.d("MainActivity", "顶栏头像已更新");
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "加载顶栏头像异常: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 将 Bitmap 裁剪为圆形
     */
    private android.graphics.Bitmap getCircleBitmap(android.graphics.Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        android.graphics.Bitmap output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);
        
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        
        // 绘制圆形裁剪路径
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        
        // 绘制图片
        android.graphics.Rect src = new android.graphics.Rect(0, 0, size, size);
        android.graphics.Rect dst = new android.graphics.Rect(0, 0, size, size);
        canvas.drawBitmap(bitmap, src, dst, paint);
        
        return output;
    }
}
