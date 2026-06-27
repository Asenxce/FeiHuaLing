package com.example.feihualinggame.activity;

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
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.RippleEffectHelper;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 用户注册页面
 */
public class RegisterActivity extends AppCompatActivity {
    private EditText etRegisterUsername;
    private EditText etRegisterPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ImageView tvLogo;  // 折扇logo
    private ViewGroup rootLayout;  // 根布局
    private AlertDialog exitDialog;  // 退出确认对话框

    @Override
    protected void onResume() {
        super.onResume();
        // 注册页属于主场景
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 初始化根布局
        rootLayout = findViewById(R.id.root_layout);

        // 初始化控件
        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        tvLogo = findViewById(R.id.tvLogo);

        // 为按钮添加动态效果
        ButtonAnimationHelper.addCombinedEffect(btnRegister);
        ButtonAnimationHelper.addCombinedEffect(tvLoginLink);
        
        // 为logo添加入场动画
        ButtonAnimationHelper.addEntryAnimation(tvLogo, 100);

        // 花朵logo点击事件 - 跳转到home页
        tvLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.putExtra("skipAuthCheck", true);
                startActivity(intent);
            }
        });

        // 注册按钮点击事件
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                register();
            }
        });

        // 跳转到登录页面
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
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

    /**
     * 执行注册逻辑
     */
    private void register() {
        String username = etRegisterUsername.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 前端校验
        if (username.isEmpty()) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            Toast.makeText(this, "用户名长度应在 3-20 位之间", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6 || password.length() > 20) {
            Toast.makeText(this, "密码长度应在 6-20 位之间", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构造用户对象（只发送username和password，identityCode由后端生成）
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        String json = new Gson().toJson(user);

        // 调用注册接口
        OkHttpUtil.post(ApiConstant.REGISTER, json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "网络异常，请检查连接", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                        int code = jsonObject.has("code") ? jsonObject.get("code").getAsInt() : 200;
                        String message = jsonObject.has("message") ? jsonObject.get("message").getAsString() : "注册失败";
                        
                        if (code == 200) {
                            Toast.makeText(RegisterActivity.this, "注册成功！即将跳转到登录页面", Toast.LENGTH_LONG).show();
                            btnRegister.postDelayed(() -> {
                                if (isFinishing() || isDestroyed()) return;
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }, 1500);
                        } else {
                            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "注册失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}