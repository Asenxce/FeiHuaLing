package com.example.feihualinggame.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.UserRecordBean;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
    private List<UserRecordBean> records;
    private OnRecordClickListener listener;

    public interface OnRecordClickListener {
        void onClick(UserRecordBean record);
    }

    public RecordAdapter(List<UserRecordBean> records, OnRecordClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserRecordBean record = records.get(position);
        holder.tvMode.setText(record.getGameModeName());
        if (record.getKeyword() != null && !record.getKeyword().isEmpty()) {
            holder.tvKeyword.setText("[" + record.getKeyword() + "]");
            holder.tvKeyword.setVisibility(View.VISIBLE);
        } else {
            holder.tvKeyword.setVisibility(View.GONE);
        }

        int rank = record.getRank();
        if (rank > 0) {
            holder.tvRank.setText("第" + rank + "名");
            holder.tvRank.setVisibility(View.VISIBLE);
        } else {
            holder.tvRank.setVisibility(View.GONE);
        }

        String time = record.getCreateTime();
        if (time != null && time.length() > 16) {
            time = time.substring(0, 16);
        }
        holder.tvTime.setText(time != null ? time : "--");

        int seconds = record.getDuration();
        if (seconds > 0) {
            int min = seconds / 60;
            int sec = seconds % 60;
            holder.tvDuration.setText(min > 0 ? min + "分" + sec + "秒" : sec + "秒");
        } else {
            holder.tvDuration.setText("--");
        }

        int totalRounds = record.getTotalRounds();
        holder.tvRounds.setText(totalRounds > 0 ? String.valueOf(totalRounds) : "--");

        double acc = record.getAccuracy();
        if (acc > 1) {
            holder.tvAccuracy.setText(String.format("%.0f%%", acc));
        } else {
            holder.tvAccuracy.setText(String.format("%.0f%%", acc * 100));
        }

        holder.tvCorrect.setText(record.getCorrectCount() + "/" + record.getWrongCount());

        int score = record.getScore();
        holder.tvScore.setText(score > 0 ? "+" + score : String.valueOf(score));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(record);
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMode, tvKeyword, tvRank, tvTime, tvDuration;
        TextView tvRounds, tvAccuracy, tvCorrect, tvScore;

        ViewHolder(View itemView) {
            super(itemView);
            tvMode = itemView.findViewById(R.id.tv_record_mode);
            tvKeyword = itemView.findViewById(R.id.tv_record_keyword);
            tvRank = itemView.findViewById(R.id.tv_record_rank);
            tvTime = itemView.findViewById(R.id.tv_record_time);
            tvDuration = itemView.findViewById(R.id.tv_record_duration);
            tvRounds = itemView.findViewById(R.id.tv_record_rounds);
            tvAccuracy = itemView.findViewById(R.id.tv_record_accuracy);
            tvCorrect = itemView.findViewById(R.id.tv_record_correct);
            tvScore = itemView.findViewById(R.id.tv_record_score);
        }
    }
}
