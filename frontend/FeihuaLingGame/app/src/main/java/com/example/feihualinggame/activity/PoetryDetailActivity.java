package com.example.feihualinggame.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.Poetry;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.PoetryCollectionManager;
import com.example.feihualinggame.utils.SystemUIUtil;

/**
 * 诗词详情页
 */
public class PoetryDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvAuthor, tvDynasty, tvContent;
    private ImageView btnCollect;
    private Poetry currentPoetry;
    private boolean isCollected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poetry_detail);
        SystemUIUtil.setLightStatusBar(this);

        // 获取传递的诗词数据
        currentPoetry = (Poetry) getIntent().getSerializableExtra("poetry");
        if (currentPoetry == null) {
            finish();
            return;
        }

        initViews();
        loadData();
    }

    private void initViews() {
        findViewById(R.id.btn_detail_back).setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });
        
        tvTitle = findViewById(R.id.tv_detail_title);
        tvAuthor = findViewById(R.id.tv_detail_author);
        tvDynasty = findViewById(R.id.tv_detail_dynasty);
        tvContent = findViewById(R.id.tv_detail_content);
        btnCollect = findViewById(R.id.btn_collect);

        btnCollect.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            toggleCollect();
        });
    }

    private void loadData() {
        tvTitle.setText(currentPoetry.getTitle() != null ? currentPoetry.getTitle() : "无题");
        tvAuthor.setText(currentPoetry.getAuthor() != null ? currentPoetry.getAuthor() : "未知");
        tvDynasty.setText(currentPoetry.getDynasty() != null ? currentPoetry.getDynasty() : "");
        
        String content = currentPoetry.getFullContent() != null ? currentPoetry.getFullContent() : currentPoetry.getContent();
        String formattedContent = formatPoetryContent(content);
        tvContent.setText(formattedContent != null ? formattedContent : "暂无内容");

        // 检查收藏状态
        isCollected = PoetryCollectionManager.isCollected(this, currentPoetry.getId());
        updateCollectButton();
    }

    private void toggleCollect() {
        if (isCollected) {
            // 取消收藏
            PoetryCollectionManager.removeFromCollectionAsync(this, currentPoetry.getId(), new PoetryCollectionManager.OnCollectionChangeListener() {
                @Override
                public void onResult(boolean success, String message) {
                    runOnUiThread(() -> {
                        if (success) {
                            isCollected = false;
                            updateCollectButton();
                        }
                        Toast.makeText(PoetryDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            // 添加收藏
            PoetryCollectionManager.addToCollectionAsync(this, currentPoetry, new PoetryCollectionManager.OnCollectionChangeListener() {
                @Override
                public void onResult(boolean success, String message) {
                    runOnUiThread(() -> {
                        if (success) {
                            isCollected = true;
                            updateCollectButton();
                        }
                        Toast.makeText(PoetryDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void updateCollectButton() {
        if (isCollected) {
            btnCollect.setImageResource(R.drawable.ic_star_filled);
            btnCollect.setColorFilter(getResources().getColor(R.color.poetry_primary));
        } else {
            btnCollect.setImageResource(R.drawable.ic_star_outline);
            btnCollect.setColorFilter(getResources().getColor(R.color.poetry_text_secondary));
        }
    }

    /**
     * 格式化诗词内容：在句号、感叹号、问号后换行，优化排版
     */
    private String formatPoetryContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            sb.append(c);
            
            // 检测到句号、感叹号、问号，且后面还有内容时换行
            boolean isEndPunct = (c == '。' || c == '.' || c == '！' || c == '!' || c == '？' || c == '?');
            boolean hasNext = (i < content.length() - 1);
            
            if (isEndPunct && hasNext) {
                sb.append('\n');
            }
        }
        
        return sb.toString().trim();
    }
}
