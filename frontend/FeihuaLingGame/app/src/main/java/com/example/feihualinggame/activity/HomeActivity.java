package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.NetworkUtil;
import com.example.feihualinggame.utils.RippleEffectHelper;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

/**
 * 游戏首页 - 启动页
 */
public class HomeActivity extends AppCompatActivity {
    private Button btnHomeLogin;
    private Button btnHomeRegister;
    private ImageView tvLogo;  // 花朵logo
    private AlertDialog exitDialog;  // 退出确认对话框

    @Override
    protected void onResume() {
        super.onResume();
        // Home页属于主场景
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // 隐藏底部导航栏指示条（必须在setContentView之后）
        SystemUIUtil.hideNavigationBarIndicator(this);

        // 检查网络状态（仅检查连接，不阻塞请求）
        if (!NetworkUtil.isNetworkConnected(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("网络错误")
                    .setMessage("网络连接异常，请检查网络设置后重试。")
                    .setCancelable(false)
                    .setPositiveButton("退出", (dialog, which) -> finishAffinity())
                    .show();
            return;
        }

        // 检查是否已登录（验证Token），如果已登录则直接进入主页
        // 但如果从登录/注册页的logo点击返回，则跳过此检查
        boolean skipAuthCheck = getIntent().getBooleanExtra("skipAuthCheck", false);
        
        if (!skipAuthCheck) {
            String token = SharedPrefsUtil.getString(this, "token");
            String userId = SharedPrefsUtil.getUserId(this);
            
            // 严格检查：token和用户ID都必须存在
            if (token != null && !token.isEmpty() && userId != null && !userId.isEmpty()) {
                android.util.Log.d("HomeActivity", "Token和用户ID存在，跳转主页");
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            } else {
                // token或userId缺失，清除残留数据
                android.util.Log.w("HomeActivity", "登录信息不完整，清除残留数据");
                SharedPrefsUtil.clearUser(this);
            }
        }

        // 初始化控件
        btnHomeLogin = findViewById(R.id.btn_home_login);
        btnHomeRegister = findViewById(R.id.btn_home_register);
        tvLogo = findViewById(R.id.tvLogo);

        // 为按钮添加动态效果
        ButtonAnimationHelper.addCombinedEffect(btnHomeLogin);
        ButtonAnimationHelper.addCombinedEffect(btnHomeRegister);
        
        // 为logo添加入场动画（延迟稍长，更有层次感）
        ButtonAnimationHelper.addEntryAnimation(tvLogo, 0);
        ButtonAnimationHelper.addEntryAnimation(btnHomeLogin, 200);
        ButtonAnimationHelper.addEntryAnimation(btnHomeRegister, 300);

        // 花朵logo点击事件 - 显示提示或刷新页面
        tvLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                // 在home页点击logo可以显示欢迎信息或播放动画
                android.widget.Toast.makeText(HomeActivity.this, "飞花令 - 诗词对战游戏平台", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // 登录按钮点击事件
        btnHomeLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // 注册按钮点击事件
        btnHomeRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                Intent intent = new Intent(HomeActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
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
}
