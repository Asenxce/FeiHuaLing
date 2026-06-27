package com.example.feihualinggame.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.Mail;

import java.util.List;

/**
 * 邮件列表适配器
 */
public class MailAdapter extends RecyclerView.Adapter<MailAdapter.MailViewHolder> {
    private List<Mail> mailList;
    private OnMailClickListener listener;

    public interface OnMailClickListener {
        void onMailClick(Mail mail, int position);
    }

    public MailAdapter(List<Mail> mailList, OnMailClickListener listener) {
        this.mailList = mailList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mail, parent, false);
        return new MailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MailViewHolder holder, int position) {
        Mail mail = mailList.get(position);
        
        // 设置标题
        holder.tvTitle.setText(mail.getTitle());
        
        // 设置发件人
        holder.tvSender.setText(mail.getSender());
        
        // 设置时间
        holder.tvTime.setText(mail.getSendTime());
        
        // 设置内容预览
        String preview = mail.getContent();
        if (preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }
        holder.tvPreview.setText(preview);
        
        // 设置未读红点
        if (!mail.isRead()) {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.poetry_primary));
        } else {
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.poetry_text_primary));
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMailClick(mail, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mailList.size();
    }

    static class MailViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTypeIcon;
        TextView tvTitle;
        TextView tvSender;
        TextView tvTime;
        TextView tvPreview;
        View viewUnreadDot;

        public MailViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTypeIcon = itemView.findViewById(R.id.tv_mail_type_icon);
            tvTitle = itemView.findViewById(R.id.tv_mail_title);
            tvSender = itemView.findViewById(R.id.tv_mail_sender);
            tvTime = itemView.findViewById(R.id.tv_mail_time);
            tvPreview = itemView.findViewById(R.id.tv_mail_preview);
            viewUnreadDot = itemView.findViewById(R.id.view_unread_dot);
        }
    }
}
