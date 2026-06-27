package com.example.feihualinggame.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.view.CircleCropView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 圆形头像裁剪Activity
 */
public class CircleCropActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_CROPPED_FILE = "extra_cropped_file";

    private CircleCropView cropView;
    private Button btnCancel;
    private Button btnConfirm;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_crop);

        cropView = findViewById(R.id.cropView);
        btnCancel = findViewById(R.id.btnCancel);
        btnConfirm = findViewById(R.id.btnConfirm);

        // 获取传入的图片URI
        imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);
        if (imageUri == null) {
            Toast.makeText(this, "图片路径无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 加载图片
        loadImage();

        // 取消按钮
        btnCancel.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        // 确定按钮
        btnConfirm.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            android.util.Log.d("CircleCropActivity", "确定按钮被点击");
            Bitmap croppedBitmap = cropView.getCroppedBitmap();
            if (croppedBitmap != null) {
                android.util.Log.d("CircleCropActivity", "裁剪成功: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
                
                // 保存到文件
                File croppedFile = new File(getCacheDir(), "cropped_avatar.png");
                try (FileOutputStream fos = new FileOutputStream(croppedFile)) {
                    croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    android.util.Log.d("CircleCropActivity", "已保存到: " + croppedFile.getAbsolutePath() + ", 大小: " + croppedFile.length() + " bytes");
                    
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_CROPPED_FILE, croppedFile.getAbsolutePath());
                    setResult(RESULT_OK, resultIntent);
                    android.util.Log.d("CircleCropActivity", "已设置 RESULT_OK 并返回文件路径");
                } catch (IOException e) {
                    android.util.Log.e("CircleCropActivity", "保存失败: " + e.getMessage());
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                    return;
                } finally {
                    croppedBitmap.recycle();
                }
                finish();
            } else {
                android.util.Log.e("CircleCropActivity", "裁剪失败：getCroppedBitmap 返回 null");
                Toast.makeText(this, "裁剪失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImage() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 先获取图片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // 计算缩放比例
            int maxDimension = 2000; // 最大边长
            int inSampleSize = 1;
            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;

            android.util.Log.d("CircleCropActivity", "原始尺寸: " + originalWidth + "x" + originalHeight);

            while (originalWidth / inSampleSize > maxDimension ||
                    originalHeight / inSampleSize > maxDimension) {
                inSampleSize *= 2;
            }

            android.util.Log.d("CircleCropActivity", "inSampleSize: " + inSampleSize);

            // 解码图片
            options = new BitmapFactory.Options();
            options.inSampleSize = inSampleSize;
            inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap != null) {
                android.util.Log.d("CircleCropActivity", "图片加载成功: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                cropView.setImageBitmap(bitmap);
            } else {
                Toast.makeText(this, "图片解码失败", Toast.LENGTH_SHORT).show();
                finish();
            }

        } catch (IOException e) {
            Toast.makeText(this, "加载图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }
}
