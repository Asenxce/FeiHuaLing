package com.example.feihualinggame.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.ApiResponse;
import com.example.feihualinggame.bean.User;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.GsonUtil;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 个人资料Fragment - 显示用户信息
 */
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private EditText etUsername;
    private EditText etNickname;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etBio;
    private Button btnSaveProfile;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        loadUserInfo();
    }

    private void initViews(View view) {
        etUsername = view.findViewById(R.id.etUsername);
        etNickname = view.findViewById(R.id.etNickname);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etBio = view.findViewById(R.id.etBio);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);

        btnSaveProfile.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            saveProfile();
        });
    }

    private void loadUserInfo() {
        String username = SharedPrefsUtil.getUsername(requireContext());
        if (username == null || username.isEmpty()) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpUtil.getWithAuth(requireContext(), ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "获取用户信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    try {
                        ApiResponse apiResponse = GsonUtil.getGson().fromJson(body, ApiResponse.class);
                        if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                            User user = GsonUtil.getGson().fromJson(
                                    GsonUtil.getGson().toJson(apiResponse.getData()), User.class);
                            if (user != null) {
                                SharedPrefsUtil.saveString(requireContext(), "nickname", user.getNickname());
                                // 保存数字 userId（用于 WebSocket 注册）
                                if (user.getId() != null) {
                                    SharedPrefsUtil.saveLong(requireContext(), "numeric_user_id", user.getId());
                                }
                                updateUI(user);
                            }
                        } else {
                            Toast.makeText(requireContext(),
                                    apiResponse != null ? apiResponse.getMessage() : "获取用户信息失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析用户信息失败", e);
                        Toast.makeText(requireContext(), "数据解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateUI(User user) {
        if (user.getUsername() != null) {
            etUsername.setText(user.getUsername());
        }
        if (user.getNickname() != null) {
            etNickname.setText(user.getNickname());
        }
        if (user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        }
        if (user.getPhone() != null) {
            etPhone.setText(user.getPhone());
        }
        if (user.getBio() != null) {
            etBio.setText(user.getBio());
        }
    }

    private void saveProfile() {
        if (isLoading) return;
        isLoading = true;
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("保存中...");

        String username = etUsername.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        String requestBody = GsonUtil.getGson().toJson(new UserUpdateRequest(username, nickname, email, phone, bio));

        OkHttpUtil.postWithAuth(requireContext(), ApiConstant.USER_UPDATE, requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    isLoading = false;
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("保存资料");
                    Toast.makeText(requireContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    isLoading = false;
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("保存资料");
                    try {
                        ApiResponse apiResponse = GsonUtil.getGson().fromJson(body, ApiResponse.class);
                        if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                            User user = GsonUtil.getGson().fromJson(
                                    GsonUtil.getGson().toJson(apiResponse.getData()), User.class);
                            if (user != null) {
                                SharedPrefsUtil.saveString(requireContext(), "nickname", user.getNickname());
                                SharedPrefsUtil.saveString(requireContext(), "username", user.getUsername());
                            }
                            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    apiResponse != null ? apiResponse.getMessage() : "保存失败",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析保存结果失败", e);
                        Toast.makeText(requireContext(), "数据解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private static class UserUpdateRequest {
        private String username;
        private String nickname;
        private String email;
        private String phone;
        private String bio;

        public UserUpdateRequest(String username, String nickname, String email, String phone, String bio) {
            this.username = username;
            this.nickname = nickname;
            this.email = email;
            this.phone = phone;
            this.bio = bio;
        }
    }
}
