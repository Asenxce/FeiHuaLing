package com.example.feihualinggame.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.User;
import com.example.feihualinggame.bean.ApiResponse;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.HeartbeatManager;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;  // 注册链接
    private ImageView tvLogo;  // 折扇logo
    private ViewGroup rootLayout;  // 根布局
    private AlertDialog exitDialog;  // 退出确认对话框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 初始化根布局
        rootLayout = findViewById(R.id.root_layout);

        // 初始化控件
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        tvLogo = findViewById(R.id.tvLogo);

        // 为按钮添加动态效果
        ButtonAnimationHelper.addCombinedEffect(btnLogin);
        ButtonAnimationHelper.addCombinedEffect(tvRegisterLink);
        
        // 为logo添加入场动画
        ButtonAnimationHelper.addEntryAnimation(tvLogo, 100);

        // 花朵logo点击事件 - 跳转到home页
        tvLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("skipAuthCheck", true);
                startActivity(intent);
            }
        });

        // 注册链接点击事件
        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // 登录按钮点击事件
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // 前端校验
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "用户名/密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 构造请求参数
                User user = new User();
                user.setUsername(username);
                user.setPassword(password);
                String json = new Gson().toJson(user);

                // 调用登录接口
                OkHttpUtil.post(ApiConstant.LOGIN, json, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // 网络失败（主线程更新UI）
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "网络异常，请检查连接", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            String result = response.body().string();
                            android.util.Log.d("LoginActivity", "========== 登录响应 ==========");
                            android.util.Log.d("LoginActivity", "完整响应: " + result);
                            runOnUiThread(() -> {
                                try {
                                    // 解析后端返回的用户信息
                                    com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(result).getAsJsonObject();
                                    
                                    // 保存用户名
                                    SharedPrefsUtil.saveUsername(LoginActivity.this, username);
                                    android.util.Log.d("LoginActivity", "已保存用户名: " + username);
                                    
                                    // 尝试多种JSON结构获取身份码和Token
                                    String identityCode = null;
                                    String token = null;
                                    
                                    android.util.Log.d("LoginActivity", "开始解析响应数据...");
                                    
                                    // 结构1: {"code": 200, "data": {"identityCode": "xxx", "token": "yyy", "sessionId": "zzz"}}
                                    String sessionId = null;
                                    if (jsonObject.has("data") && jsonObject.get("data").isJsonObject()) {
                                        com.google.gson.JsonObject data = jsonObject.getAsJsonObject("data");
                                        if (data.has("identityCode")) {
                                            identityCode = data.get("identityCode").getAsString();
                                        }
                                        if (data.has("token")) {
                                            token = data.get("token").getAsString();
                                        }
                                        if (data.has("sessionId")) {
                                            sessionId = data.get("sessionId").getAsString();
                                        }
                                    }
                                    // 结构2: {"identityCode": "xxx", "token": "yyy"} (直接返回)
                                    else {
                                        if (jsonObject.has("identityCode")) {
                                            identityCode = jsonObject.get("identityCode").getAsString();
                                        }
                                        if (jsonObject.has("token")) {
                                            token = jsonObject.get("token").getAsString();
                                        }
                                    }
                                    
                                    // 验证后端返回的数据有效性
                                    if (identityCode == null || identityCode.isEmpty()) {
                                        android.util.Log.e("LoginActivity", "后端未返回identityCode，登录失败");
                                        Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    if (token == null || token.isEmpty()) {
                                        android.util.Log.e("LoginActivity", "后端未返回Token，登录失败");
                                        Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    // 保存身份码
                                    SharedPrefsUtil.saveUserId(LoginActivity.this, identityCode);
                                    android.util.Log.d("LoginActivity", "已保存身份码: " + identityCode);
                                    
                                    // 保存Token
                                    SharedPrefsUtil.saveString(LoginActivity.this, "token", token);
                                    
                                    if (sessionId != null && !sessionId.isEmpty()) {
                                        SharedPrefsUtil.saveString(LoginActivity.this, "sessionId", sessionId);
                                    }
                                    android.util.Log.d("LoginActivity", "已保存Token");
                                    
                                    // 启动心跳机制维持在线状态
                                    HeartbeatManager.getInstance().init(LoginActivity.this);
                                    HeartbeatManager.getInstance().start();
                                    
                                    // 验证Token是否保存成功
                                    String savedToken = SharedPrefsUtil.getString(LoginActivity.this, "token");
                                    if (savedToken == null || savedToken.isEmpty()) {
                                        android.util.Log.e("LoginActivity", "Token保存失败！");
                                        Toast.makeText(LoginActivity.this, "登录失败，请重试", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                    
                                    // 登录成功后获取用户信息（包括头像）
                                    fetchUserInfoAfterLogin(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                } catch (Exception e) {
                                    android.util.Log.e("LoginActivity", "解析登录响应失败", e);
                                    e.printStackTrace();
                                    Toast.makeText(LoginActivity.this, "登录失败，服务器响应异常", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "无响应体";
                            android.util.Log.e("LoginActivity", "登录失败，HTTP状态码: " + response.code());
                            android.util.Log.e("LoginActivity", "错误响应: " + errorBody);
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, "登录失败，用户名/密码错误", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }
        });
    }

    /**
     * 登录成功后获取用户信息（包括头像）
     */
    private void fetchUserInfoAfterLogin(Runnable onSuccessRunnable) {
        OkHttpUtil.getWithAuth(this, ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("LoginActivity", "获取用户信息失败", e);
                runOnUiThread(onSuccessRunnable);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                android.util.Log.d("LoginActivity", "用户信息响应: " + body);
                
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            ApiResponse<User> apiResponse = new Gson().fromJson(body, new TypeToken<ApiResponse<User>>(){}.getType());
                            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                                User user = apiResponse.getData();
                                
                                // 始终保存数字 userId（用于 WebSocket 注册）
                                if (user.getId() != null) {
                                    SharedPrefsUtil.saveLong(LoginActivity.this, "numeric_user_id", user.getId());
                                    android.util.Log.d("LoginActivity", "已保存数字userId: " + user.getId());
                                }
                                
                                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                                    SharedPrefsUtil.saveString(LoginActivity.this, "avatarUrl", user.getAvatarUrl());
                                    android.util.Log.d("LoginActivity", "已缓存头像URL: " + user.getAvatarUrl());
                                    
                                    // 通过后端代理接口下载头像（解决 OSS 403 问题）
                                    final Long userId = user.getId();
                                    android.util.Log.d("LoginActivity", "开始通过代理接口下载头像: user/avatar/" + userId);

                                    OkHttpUtil.getWithAuth(LoginActivity.this, "user/avatar/" + userId, new Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {
                                            android.util.Log.e("LoginActivity", "下载头像异常", e);
                                            runOnUiThread(onSuccessRunnable);
                                        }

                                        @Override
                                        public void onResponse(Call call, Response response) throws IOException {
                                            try {
                                                if (response.isSuccessful() && response.body() != null) {
                                                    byte[] imageBytes = response.body().bytes();
                                                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                                                    if (bitmap != null) {
                                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                                                        byte[] pngBytes = baos.toByteArray();
                                                        String base64Avatar = android.util.Base64.encodeToString(pngBytes, android.util.Base64.NO_WRAP);

                                                        android.content.SharedPreferences prefs = getSharedPreferences("feihualing_sp", MODE_PRIVATE);
                                                        prefs.edit().putString("user_avatar_base64", base64Avatar).commit();
                                                    }
                                                }
                                            } catch (Exception e) {
                                                android.util.Log.e("LoginActivity", "下载头像处理异常", e);
                                            } finally {
                                                runOnUiThread(onSuccessRunnable);
                                            }
                                        }
                                    });
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("LoginActivity", "解析用户信息失败", e);
                        }
                    }
                    // 如果没有头像或请求失败，直接跳转
                    runOnUiThread(onSuccessRunnable);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 登录页属于主场景，恢复背景音乐
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }
    
    /**
     * 重写返回键行为：显示退出确认对话框
     */
    @Override
    public void onBackPressed() {
        if (exitDialog != null && exitDialog.isShowing()) {
            exitDialog.dismiss();
        } else {
            exitDialog = new AlertDialog.Builder(this)
                    .setTitle("退出游戏")
                    .setMessage("确定要退出飞花令吗？")
                    .setPositiveButton("退出", (dialog, which) -> finishAffinity())
                    .setNegativeButton("取消", null)
                    .create();
            exitDialog.show();
        }
    }
}