package com.example.feihualinggame.adapter;

import android.content.Context;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.Poetry;
import java.util.List;

public class PoetryAdapter extends RecyclerView.Adapter<PoetryAdapter.ViewHolder> {
    private final Context context;
    private List<Poetry> poetryList;
    private String highlightKeyword;
    private OnPoetryClickListener listener;

    public interface OnPoetryClickListener {
        void onPoetryClick(Poetry poetry);
    }

    public void setOnPoetryClickListener(OnPoetryClickListener listener) {
        this.listener = listener;
    }

    public PoetryAdapter(Context context, List<Poetry> poetryList) {
        this.context = context;
        this.poetryList = poetryList;
    }

    public void setPoetryList(List<Poetry> poetryList) {
        this.poetryList = poetryList;
    }
    public void setHighlightKeyword(String keyword) {
        this.highlightKeyword = keyword;
        notifyDataSetChanged();
    }
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poetry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Poetry poetry = poetryList.get(position);
        
        // 获取真实标题用于排版判断
        String rawTitle = poetry.getTitle();
        
        // 设置标题（带高亮）
        String title = rawTitle != null ? rawTitle : "无题";
        holder.tvTitle.setText(highlightKeyword(title));
        
        // 设置作者和朝代（带高亮）
        String authorInfo = poetry.getAuthor();
        if (poetry.getDynasty() != null && !poetry.getDynasty().isEmpty()) {
            authorInfo = "【" + poetry.getDynasty() + "】" + poetry.getAuthor();
        }
        holder.tvAuthor.setText(highlightKeyword(authorInfo));
        
        // 设置内容（整首诗词，带高亮）
        String content = poetry.getFullContent() != null ? poetry.getFullContent() : poetry.getContent();
        if (content == null || content.isEmpty()) {
            content = "暂无内容";
        } else {
            // 根据标题判断是诗还是词，应用不同的排版逻辑
            if (isCi(rawTitle)) {
                // 词：保持原始排版格式，不做额外处理
            } else {
                // 诗：按照句号/叹号/问号换行
                content = formatPoetryContent(content);
            }
        }
        holder.tvContent.setText(highlightKeyword(content));

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPoetryClick(poetry);
            }
        });
    }

    /**
     * 判断是否为词（通过标题常见词牌名或标题特征判断）
     */
    private boolean isCi(String title) {
        if (title == null) return false;
        // 常见词牌名列表（可扩展）
        String[] ciPatterns = {"令", "引", "近", "慢", "调", "歌头", "子", "娘", "儿", "仙", "破阵子", "水调歌头", "西江月", "如梦令", "蝶恋花", "浣溪沙", "菩萨蛮", "卜算子", "采桑子", "忆江南", "长相思", "相见欢", "虞美人", "临江仙", "满江红", "沁园春", "水龙吟", "雨霖铃", "声声慢", "念奴娇", "贺新郎", "永遇乐", "青玉案", "渔家傲", "苏幕遮", "醉花阴", "一剪梅", "钗头凤", "浪淘沙", "南乡子", "定风波", "江城子", "八声甘州", "扬州慢", "桂枝香", "望海潮"};
        for (String pattern : ciPatterns) {
            if (title.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 格式化诗词内容：忠实于数据库原始数据，仅添加换行符优化排版
     * 规则：仅在句号、感叹号、问号后换行，直到结束（最后一个标点除外）
     */
    private String formatPoetryContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            sb.append(c); // 100% 保留原始字符和标点
            
            // 检测到句号、感叹号、问号，且后面还有内容时换行
            boolean isEndPunct = (c == '。' || c == '.' || c == '！' || c == '!' || c == '？' || c == '?');
            boolean hasNext = (i < content.length() - 1);
            
            if (isEndPunct && hasNext) {
                sb.append('\n');
            }
        }
        
        return sb.toString().trim();
    }

    /**
     * 高亮关键字（使用SpannableString）
     */
    private SpannableString highlightKeyword(String text) {
        SpannableString spannableString = new SpannableString(text);
        
        if (highlightKeyword != null && !highlightKeyword.isEmpty() && text.contains(highlightKeyword)) {
            int startIndex = 0;
            while ((startIndex = text.indexOf(highlightKeyword, startIndex)) != -1) {
                int endIndex = startIndex + highlightKeyword.length();
                // 设置黄色背景
                spannableString.setSpan(
                    new BackgroundColorSpan(ContextCompat.getColor(context, R.color.gold_light)),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannableString.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_on_primary)),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                startIndex = endIndex;
            }
        }
        
        return spannableString;
    }

    @Override
    public int getItemCount() {
        return poetryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvContent;
        TextView tvAuthor;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_poetry_title);
            tvContent = itemView.findViewById(R.id.tv_poetry_content);
            tvAuthor = itemView.findViewById(R.id.tv_poetry_author);
        }
    }
}