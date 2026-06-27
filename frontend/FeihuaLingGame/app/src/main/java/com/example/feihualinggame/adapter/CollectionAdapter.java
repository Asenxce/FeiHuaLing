package com.example.feihualinggame.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.Poetry;

import java.util.List;

/**
 * 收藏列表适配器
 */
public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {
    private List<Poetry> poetryList;
    private OnPoetryClickListener listener;

    public interface OnPoetryClickListener {
        void onPoetryClick(Poetry poetry);
    }

    public CollectionAdapter(List<Poetry> poetryList, OnPoetryClickListener listener) {
        this.poetryList = poetryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Poetry poetry = poetryList.get(position);
        holder.tvTitle.setText(poetry.getTitle() != null ? poetry.getTitle() : "无题");
        
        String author = poetry.getAuthor() != null ? poetry.getAuthor() : "未知";
        String dynasty = poetry.getDynasty() != null ? poetry.getDynasty() : "";
        holder.tvAuthor.setText(dynasty + " · " + author);
        
        String content = poetry.getFullContent() != null ? poetry.getFullContent() : poetry.getContent();
        holder.tvContent.setText(content != null ? content : "");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPoetryClick(poetry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return poetryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAuthor, tvContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_collection_title);
            tvAuthor = itemView.findViewById(R.id.tv_item_collection_author);
            tvContent = itemView.findViewById(R.id.tv_item_collection_content);
        }
    }
}
