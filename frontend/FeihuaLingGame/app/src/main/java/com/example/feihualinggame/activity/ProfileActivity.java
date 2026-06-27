package com.example.feihualinggame.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.ApiResponse;
import com.example.feihualinggame.bean.User;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.AvatarManager;
import com.example.feihualinggame.utils.AvatarUploadManager;
import com.example.feihualinggame.utils.GsonUtil;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 个人信息展示页面（独立Activity）
 */
public class ProfileActivity extends BaseActivity {
    private static final int REQUEST_CODE_EDIT = 1001;
    private static final int REQUEST_CODE_CROP = 1002;
    private static final int MAX_AVATAR_SIZE = 500;
    private static final int COMPRESS_QUALITY = 80;

    // 裁剪相关
    private static final String CROPPED_AVATAR_PATH = "cropped_avatar";

    private ImageView btnBack;
    private ImageView ivAvatar;
    private ProgressBar progressBarAvatar;
    private TextView tvNickname;
    private TextView tvUsername;
    private TextView tvIdentityCode;
    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvBio;
    private Button btnEditProfile;
    private AvatarManager avatarManager;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SystemUIUtil.setLightStatusBar(this);
        username = SharedPrefsUtil.getUsername(this);

        initLaunchers();
        initViews();
        loadUserProfile();
    }

    /**
     * 初始化图片选择器和权限请求器
     */
    private void initLaunchers() {
        // 权限请求
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "需要相册权限才能选择头像", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 图片选择
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            cropImage(imageUri);
                        }
                    }
                }
        );

        // 裁剪图片
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String croppedFilePath = result.getData().getStringExtra(CircleCropActivity.EXTRA_CROPPED_FILE);
                        if (croppedFilePath != null) {
                            android.util.Log.d("ProfileActivity", "收到裁剪文件: " + croppedFilePath);
                            java.io.File croppedFile = new java.io.File(croppedFilePath);
                            if (croppedFile.exists()) {
                                processCroppedBitmapFile(croppedFile);
                            } else {
                                Toast.makeText(this, "裁剪文件不存在", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivAvatar = findViewById(R.id.ivAvatar);
        progressBarAvatar = findViewById(R.id.progressBarAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvUsername = findViewById(R.id.tvUsername);
        tvIdentityCode = findViewById(R.id.tvIdentityCode);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvBio = findViewById(R.id.tvBio);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        // 初始化头像管理器（用于加载缓存）
        avatarManager = new AvatarManager(this, ivAvatar);

        btnBack.setOnClickListener(v -> finish());
        
        // 点击头像更换
        ivAvatar.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            pickAvatarFromGallery();
        });
        
        btnEditProfile.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, REQUEST_CODE_EDIT);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT && resultCode == RESULT_OK) {
            loadUserProfile();
        }
        // 裁剪结果由 registerForActivityResult 处理，此处不再处理
    }

    /**
     * 打开相册选择头像
     */
    private void pickAvatarFromGallery() {
        boolean hasPermission;
        String permissionName;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionName = Manifest.permission.READ_MEDIA_IMAGES;
            hasPermission = ContextCompat.checkSelfPermission(this, permissionName) == PackageManager.PERMISSION_GRANTED;
        } else {
            permissionName = Manifest.permission.READ_EXTERNAL_STORAGE;
            hasPermission = ContextCompat.checkSelfPermission(this, permissionName) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasPermission) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(permissionName);
        }
    }

    /**
     * 打开相册
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }
    
    /**
     * 打开自定义圆形裁剪页面
     */
    private void cropImage(Uri sourceUri) {
        Intent intent = new Intent(this, CircleCropActivity.class);
        intent.putExtra(CircleCropActivity.EXTRA_IMAGE_URI, sourceUri);
        cropImageLauncher.launch(intent);
    }

    /**
     * 处理裁剪后的文件（圆形）
     */
    private void processCroppedBitmapFile(java.io.File croppedFile) {
        progressBarAvatar.setVisibility(View.VISIBLE);
        ivAvatar.setAlpha(0.5f);
        ivAvatar.setEnabled(false);

        AvatarUploadManager.getInstance().uploadAvatar(this, croppedFile, new AvatarUploadManager.UploadCallback() {
            @Override
            public void onSuccess(User userInfo) {
                runOnUiThread(() -> {
                    progressBarAvatar.setVisibility(View.GONE);
                    ivAvatar.setAlpha(1.0f);
                    ivAvatar.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "头像更新成功", Toast.LENGTH_SHORT).show();
                    
                    // 刷新UI
                    updateUI(userInfo);
                    
                    // 保存头像URL到本地缓存
                    SharedPrefsUtil.saveString(ProfileActivity.this, "avatarUrl", userInfo.getAvatarUrl());
                    
                    // 将裁剪后的图片保存为Base64到本地缓存，用于立即显示
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(croppedFile.getAbsolutePath());
                        if (bitmap != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] imageBytes = baos.toByteArray();
                            String base64Avatar = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                            SharedPrefsUtil.saveString(ProfileActivity.this, "user_avatar_base64", base64Avatar);
                            
                            // 立即更新当前页面的头像显示
                            ivAvatar.setImageBitmap(bitmap);
                            Log.d("ProfileActivity", "头像已保存到本地缓存并更新显示");
                        }
                    } catch (Exception e) {
                        Log.e("ProfileActivity", "保存本地缓存失败", e);
                    }
                    
                    // 通知其他页面刷新
                    setResult(RESULT_OK);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBarAvatar.setVisibility(View.GONE);
                    ivAvatar.setAlpha(1.0f);
                    ivAvatar.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onProgress(int progress) {
                // 可选：如果需要显示进度条，可以更新 progressBarAvatar
                runOnUiThread(() -> {
                    android.util.Log.d("ProfileActivity", "上传进度: " + progress + "%");
                });
            }
        });
    }

    /**
     * 处理裁剪后的Bitmap（圆形） - 保留旧方法以防其他地方调用
     */
    private void processCroppedBitmap(Bitmap croppedBitmap) {
        try {
            android.util.Log.d("ProfileActivity", "开始处理圆形裁剪图片: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
            
            // 压缩为 Base64（PNG保留透明通道）
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean compressSuccess = croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            if (!compressSuccess) {
                android.util.Log.e("ProfileActivity", "PNG压缩失败");
                Toast.makeText(this, "图片压缩失败", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] imageBytes = baos.toByteArray();
            String base64Avatar = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            android.util.Log.d("ProfileActivity", "Base64长度: " + base64Avatar.length());

            // 保存到 SharedPreferences
            SharedPrefsUtil.saveString(this, "user_avatar_base64", base64Avatar);
            android.util.Log.d("ProfileActivity", "头像已保存到SharedPreferences");

            // 更新 UI
            runOnUiThread(() -> {
                if (ivAvatar != null) {
                    ivAvatar.setImageBitmap(croppedBitmap);
                    android.util.Log.d("ProfileActivity", "头像已更新到UI");
                }
            });
            
            Toast.makeText(this, "头像设置成功", Toast.LENGTH_SHORT).show();

            // 设置返回结果，通知 MainActivity 刷新
            setResult(RESULT_OK);
            android.util.Log.d("ProfileActivity", "已设置 setResult(RESULT_OK)");

        } catch (Exception e) {
            android.util.Log.e("ProfileActivity", "处理图片异常: " + e.getMessage(), e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理选中的图片
     */
    private void processImage(Uri imageUri) {
        try {
            android.util.Log.d("ProfileActivity", "开始处理图片: " + imageUri);
            
            // 1. 读取图片尺寸
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                android.util.Log.e("ProfileActivity", "无法打开图片输入流");
                Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show();
                return;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // 2. 计算缩放比例
            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            android.util.Log.d("ProfileActivity", "原始尺寸: " + originalWidth + "x" + originalHeight);
            
            int inSampleSize = 1;
            while (originalWidth / inSampleSize > MAX_AVATAR_SIZE ||
                    originalHeight / inSampleSize > MAX_AVATAR_SIZE) {
                inSampleSize *= 2;
            }
            android.util.Log.d("ProfileActivity", "缩放比例: " + inSampleSize);

            // 3. 解码缩放后的图片
            options = new BitmapFactory.Options();
            options.inSampleSize = inSampleSize;
            inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap == null) {
                android.util.Log.e("ProfileActivity", "Bitmap 解码失败");
                Toast.makeText(this, "解码图片失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            android.util.Log.d("ProfileActivity", "解码成功: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            // 4. 裁剪为圆形头像
            bitmap = getCircleBitmap(bitmap);
            android.util.Log.d("ProfileActivity", "圆形裁剪后: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            // 5. 压缩为 Base64（PNG保留透明通道）
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean compressSuccess = bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            if (!compressSuccess) {
                android.util.Log.e("ProfileActivity", "PNG压缩失败");
                Toast.makeText(this, "图片压缩失败", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] imageBytes = baos.toByteArray();
            String base64Avatar = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            android.util.Log.d("ProfileActivity", "Base64长度: " + base64Avatar.length());

            // 6. 保存到 SharedPreferences
            SharedPrefsUtil.saveString(this, "user_avatar_base64", base64Avatar);
            android.util.Log.d("ProfileActivity", "头像已保存到SharedPreferences");

            // 7. 更新 UI
            final Bitmap finalBitmap = bitmap;
            runOnUiThread(() -> {
                if (ivAvatar != null) {
                    ivAvatar.setImageBitmap(finalBitmap);
                    android.util.Log.d("ProfileActivity", "头像已更新到UI");
                }
            });
            
            Toast.makeText(this, "头像设置成功", Toast.LENGTH_SHORT).show();

            // 8. 设置返回结果，通知 MainActivity 刷新
            setResult(RESULT_OK);
            android.util.Log.d("ProfileActivity", "已设置 setResult(RESULT_OK)");

        } catch (Exception e) {
            android.util.Log.e("ProfileActivity", "处理图片异常: " + e.getMessage(), e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("ProfileActivity", "onResume 被调用 - 准备加载缓存头像");
        // 每次返回页面时重新加载头像
        if (avatarManager != null) {
            avatarManager.loadCachedAvatar();
            android.util.Log.d("ProfileActivity", "已调用 avatarManager.loadCachedAvatar()");
        } else {
            android.util.Log.e("ProfileActivity", "错误: avatarManager 为 null");
        }
    }

    private void loadUserProfile() {
        android.util.Log.d("ProfileActivity", "开始加载用户数据, username=" + username);
        OkHttpUtil.getWithAuth(this, ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("ProfileActivity", "请求失败", e);
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "加载失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                android.util.Log.d("ProfileActivity", "响应码: " + response.code() + ", 响应体: " + body);
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            ApiResponse<User> apiResponse = GsonUtil.getGson().fromJson(body, new com.google.gson.reflect.TypeToken<ApiResponse<User>>(){}.getType());
                            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                                User user = apiResponse.getData();
                                android.util.Log.d("ProfileActivity", "解析User: " + user.toString());
                                // 同步本地昵称
                                if (user.getNickname() != null) {
                                    SharedPrefsUtil.saveString(ProfileActivity.this, "nickname", user.getNickname());
                                }
                                updateUI(user);
                            } else {
                                Toast.makeText(ProfileActivity.this, apiResponse != null ? apiResponse.getMessage() : "获取用户信息失败", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ProfileActivity", "解析数据失败", e);
                            Toast.makeText(ProfileActivity.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void updateUI(User user) {
        tvUsername.setText(user.getUsername());
        tvNickname.setText(user.getNickname() != null && !user.getNickname().isEmpty()
                ? user.getNickname() : "未设置昵称");
        
        // 优先使用后端返回的身份码，若为空则从本地缓存读取
        String identityCode = user.getIdentityCode();
        if (identityCode == null || identityCode.isEmpty()) {
            identityCode = SharedPrefsUtil.getUserId(this);
            android.util.Log.w("ProfileActivity", "后端未返回身份码，使用本地缓存: " + identityCode);
        }
        tvIdentityCode.setText(identityCode != null ? identityCode : "--");
        
        tvEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty()
                ? user.getEmail() : "未填写");
        tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty()
                ? user.getPhone() : "未填写");
        tvBio.setText(user.getBio() != null && !user.getBio().isEmpty()
                ? user.getBio() : "暂无签名");
    }

    /**
     * 将Bitmap裁剪为圆形
     */
    private Bitmap getCircleBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, new Rect(0, 0, size, size), paint);
        return output;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.d("ProfileActivity", "onDestroy 被调用");
    }
}
