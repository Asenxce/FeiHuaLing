package com.example.feihualinggame.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.ApiResponse;
import com.example.feihualinggame.bean.User;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.GsonUtil;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 编辑个人资料页面
 */
public class EditProfileActivity extends BaseActivity {
    private ImageView btnBack;
    private EditText etUsername;
    private EditText etNickname;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etBio;
    private Button btnSaveProfile;

    private String username;

    @Override
    protected void onResume() {
        super.onResume();
        // 编辑资料页属于主场景
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        SystemUIUtil.setLightStatusBar(this);
        username = SharedPrefsUtil.getUsername(this);

        btnBack = findViewById(R.id.btn_back);
        etUsername = findViewById(R.id.etUsername);
        etNickname = findViewById(R.id.etNickname);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etBio = findViewById(R.id.etBio);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });
        loadUserProfile();
        btnSaveProfile.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            saveProfile();
        });
    }

    private void loadUserProfile() {
        etUsername.setText(username);
        etUsername.setEnabled(false);

        OkHttpUtil.getWithAuth(this, ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                android.util.Log.d("EditProfile", "Response: " + body);
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            ApiResponse<User> apiResponse = GsonUtil.getGson().fromJson(body, new com.google.gson.reflect.TypeToken<ApiResponse<User>>(){}.getType());
                            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                                User user = apiResponse.getData();
                                etNickname.setText(user.getNickname() != null ? user.getNickname() : "");
                                etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                                etPhone.setText(user.getPhone() != null ? user.getPhone() : "");
                                etBio.setText(user.getBio() != null ? user.getBio() : "");
                            } else {
                                Toast.makeText(EditProfileActivity.this, apiResponse != null ? apiResponse.getMessage() : "数据为空", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(EditProfileActivity.this, "解析数据失败", Toast.LENGTH_SHORT).show();
                            android.util.Log.e("EditProfile", "Parse error: " + e.getMessage());
                        }
                    } else {
                        Toast.makeText(EditProfileActivity.this, "请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        // 使用 Gson 构建 JSON，避免手动拼接导致特殊字符转义问题
        User updateData = new User();
        updateData.setUsername(username);
        updateData.setNickname(nickname);
        updateData.setEmail(email);
        updateData.setPhone(phone);
        updateData.setBio(bio);
        String json = GsonUtil.getGson().toJson(updateData);

        OkHttpUtil.postWithAuth(this, ApiConstant.USER_UPDATE, json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "网络异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                android.util.Log.d("EditProfile", "Save Response: " + body);
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            ApiResponse<?> apiResponse = GsonUtil.getGson().fromJson(body, new com.google.gson.reflect.TypeToken<ApiResponse<?>>(){}.getType());
                            if (apiResponse != null && apiResponse.isSuccess()) {
                                // 同步本地昵称
                                SharedPrefsUtil.saveString(EditProfileActivity.this, "nickname", nickname);
                                Toast.makeText(EditProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(EditProfileActivity.this, apiResponse != null ? apiResponse.getMessage() : "保存失败", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            // 如果解析失败但请求成功，默认保存成功
                            SharedPrefsUtil.saveString(EditProfileActivity.this, "nickname", nickname);
                            Toast.makeText(EditProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        }
                    } else {
                        Toast.makeText(EditProfileActivity.this, "保存失败: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
