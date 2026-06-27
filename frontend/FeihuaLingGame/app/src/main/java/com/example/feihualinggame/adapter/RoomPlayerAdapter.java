package com.example.feihualinggame.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.RoomPlayerBean;

import java.util.List;

public class RoomPlayerAdapter extends RecyclerView.Adapter<RoomPlayerAdapter.ViewHolder> {

    private List<RoomPlayerBean> players;

    public RoomPlayerAdapter(List<RoomPlayerBean> players) {
        this.players = players;
    }

    public void setPlayers(List<RoomPlayerBean> players) {
        this.players = players;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomPlayerBean player = players.get(position);

        holder.tvNickname.setText(player.getNickname());
        holder.tvOrder.setText(String.valueOf(player.getJoinOrder()));

        String statusText;
        int statusColor;
        switch (player.getStatus()) {
            case "WAITING":
                statusText = player.isReady() ? "已准备" : "等待中";
                statusColor = player.isReady()
                        ? holder.itemView.getContext().getColor(R.color.success)
                        : holder.itemView.getContext().getColor(R.color.text_hint);
                break;
            case "BATTLE":
                statusText = "对战中";
                statusColor = holder.itemView.getContext().getColor(R.color.gold);
                break;
            case "ELIMINATED":
                statusText = "已淘汰";
                statusColor = holder.itemView.getContext().getColor(R.color.error);
                break;
            case "QUIT":
                statusText = "已退出";
                statusColor = holder.itemView.getContext().getColor(R.color.text_hint);
                break;
            default:
                statusText = player.isReady() ? "已准备" : "等待中";
                statusColor = player.isReady()
                        ? holder.itemView.getContext().getColor(R.color.success)
                        : holder.itemView.getContext().getColor(R.color.text_hint);
        }
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setTextColor(statusColor);

        holder.tvHost.setVisibility(player.isHost() ? View.VISIBLE : View.GONE);
        holder.ivReady.setVisibility(player.isReady() ? View.VISIBLE : View.GONE);

        holder.ivAvatar.setImageResource(R.drawable.default_avatar);

        holder.tvFaultCount.setText("容错:" + player.getFaultCount());
    }

    @Override
    public int getItemCount() {
        return players != null ? players.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvNickname;
        TextView tvOrder;
        TextView tvStatus;
        TextView tvHost;
        ImageView ivReady;
        TextView tvFaultCount;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvNickname = itemView.findViewById(R.id.tv_nickname);
            tvOrder = itemView.findViewById(R.id.tv_order);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvHost = itemView.findViewById(R.id.tv_host);
            ivReady = itemView.findViewById(R.id.iv_ready);
            tvFaultCount = itemView.findViewById(R.id.tv_fault_count);
        }
    }
}
