package com.example.feihualinggame.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 头像选择与管理工具类
 */
public class AvatarManager {
    private static final String PREFS_KEY_AVATAR = "user_avatar_base64";
    private static final int MAX_AVATAR_SIZE = 500; // 最大边长像素
    private static final int COMPRESS_QUALITY = 80; // 压缩质量

    private ComponentActivity activity;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ImageView targetImageView;

    public AvatarManager(ComponentActivity activity, ImageView targetImageView) {
        this.activity = activity;
        this.targetImageView = targetImageView;

        // 初始化权限请求启动器
        requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // 权限已授予，打开相册
                        openGallery();
                    } else {
                        Toast.makeText(activity, "需要相册权限才能选择头像", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 初始化图片选择启动器
        pickImageLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            processImage(imageUri);
                        }
                    }
                }
        );
    }

    /**
     * 打开相册选择头像
     */
    public void pickAvatarFromGallery() {
        // 检查权限
        boolean hasPermission;
        String permissionName;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissionName = Manifest.permission.READ_MEDIA_IMAGES;
            hasPermission = ContextCompat.checkSelfPermission(activity, permissionName) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12及以下
            permissionName = Manifest.permission.READ_EXTERNAL_STORAGE;
            hasPermission = ContextCompat.checkSelfPermission(activity, permissionName) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasPermission) {
            openGallery();
        } else {
            // 请求权限
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
    private void processImage(Uri imageUri) {
        try {
            // 1. 读取图片
            InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(activity, "读取图片失败", Toast.LENGTH_SHORT).show();
                return;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // 2. 计算缩放比例
            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            int inSampleSize = 1;

            while (originalWidth / inSampleSize > MAX_AVATAR_SIZE ||
                    originalHeight / inSampleSize > MAX_AVATAR_SIZE) {
                inSampleSize *= 2;
            }

            // 3. 解码缩放后的图片
            options = new BitmapFactory.Options();
            options.inSampleSize = inSampleSize;
            inputStream = activity.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap == null) {
                Toast.makeText(activity, "解码图片失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 4. 压缩为 Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Avatar = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // 5. 保存到 SharedPreferences
            SharedPrefsUtil.saveString(activity, PREFS_KEY_AVATAR, base64Avatar);

            // 6. 更新 UI
            updateAvatarDisplay(bitmap);
            Toast.makeText(activity, "头像设置成功", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(activity, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 更新头像显示
     */
    public void updateAvatarDisplay(Bitmap bitmap) {
        if (targetImageView != null && bitmap != null) {
            targetImageView.setImageBitmap(bitmap);
        }
    }

    /**
     * 从缓存加载头像
     */
    public void loadCachedAvatar() {
        String base64Avatar = SharedPrefsUtil.getString(activity, PREFS_KEY_AVATAR);
        if (base64Avatar != null && !base64Avatar.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(base64Avatar, Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                updateAvatarDisplay(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取 Base64 格式的头像数据
     */
    public static String getAvatarBase64(ComponentActivity activity) {
        return SharedPrefsUtil.getString(activity, PREFS_KEY_AVATAR);
    }

    /**
     * 清除缓存的头像
     */
    public static void clearCachedAvatar(ComponentActivity activity) {
        SharedPrefsUtil.saveString(activity, PREFS_KEY_AVATAR, "");
    }
}
