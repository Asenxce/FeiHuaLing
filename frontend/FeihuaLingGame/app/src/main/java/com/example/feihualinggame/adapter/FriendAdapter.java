package com.example.feihualinggame.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.Friend;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private List<Friend> friendList;

    public FriendAdapter(List<Friend> friendList) {
        this.friendList = friendList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friendList.get(position);
        Context context = holder.itemView.getContext();
        
        // 优先显示昵称，若未设置则显示用户名
        String displayName = friend.getNickname() != null && !friend.getNickname().isEmpty() 
                ? friend.getNickname() : friend.getUsername();
        holder.tvUsername.setText(displayName);
        
        // 加载好友头像
        loadFriendAvatar(context, friend, holder.ivAvatar);
        
        // 设置状态
        if (friend.isOnline()) {
            holder.tvStatus.setText("在线");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.online));
            holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.paper_dark));
            ViewCompat.setBackgroundTintList(holder.vOnlineIndicator, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.online)));
        } else {
            holder.tvStatus.setText("离线");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_hint));
            holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context, R.color.divider));
            ViewCompat.setBackgroundTintList(holder.vOnlineIndicator, ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_hint)));
        }
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }
    
    /**
     * 加载好友头像
     */
    private void loadFriendAvatar(Context context, Friend friend, ImageView ivAvatar) {
        if (friend.getAvatarUrl() == null || friend.getAvatarUrl().isEmpty()) {
            // 无头像，显示默认头像
            ivAvatar.setImageResource(R.drawable.default_avatar);
            return;
        }
        
        // 从本地缓存查找（使用用户名作为键）
        String cacheKey = "friend_avatar_" + friend.getUsername();
        String cachedBase64 = SharedPrefsUtil.getString(context, cacheKey);
        
        if (cachedBase64 != null && !cachedBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(cachedBase64, Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    ivAvatar.setImageBitmap(bitmap);
                    return;
                }
            } catch (Exception e) {
                Log.e("FriendAdapter", "加载缓存头像失败", e);
            }
        }
        
        // 缓存未命中，从后端代理接口下载
        // 注意：avatarUrl 可能是 OSS 完整链接（会 403），必须使用代理接口
        // 注意：OkHttpUtil.getWithAuth 会自动拼接 BASE_URL (以 /api/ 结尾)
        // 所以这里传入的路径不应包含 /api/，避免路径重复导致 403/404
        String proxyUrl = friend.getUserId() != null ? "user/avatar/" + friend.getUserId() : null;
        
        if (proxyUrl == null) {
            Log.w("FriendAdapter", "用户ID为空，无法下载头像: " + friend.getUsername());
            ivAvatar.setImageResource(R.drawable.default_avatar);
            return;
        }
        
        final String finalCacheKey = cacheKey;
        final ImageView finalIvAvatar = ivAvatar;
        final Context finalContext = context;
        
        OkHttpUtil.getWithAuth(context, proxyUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("FriendAdapter", "下载头像失败: " + friend.getUsername(), e);
                ((android.app.Activity) context).runOnUiThread(() -> finalIvAvatar.setImageResource(R.drawable.default_avatar));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] imageBytes = response.body().bytes();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        
                        if (bitmap != null) {
                            // 转换为 Base64 并缓存
                            String base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                            SharedPrefsUtil.saveString(context, finalCacheKey, base64);
                            
                            // 更新 UI
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                finalIvAvatar.setImageBitmap(bitmap);
                            });
                        } else {
                            ((android.app.Activity) context).runOnUiThread(() -> finalIvAvatar.setImageResource(R.drawable.default_avatar));
                        }
                    } catch (Exception e) {
                        Log.e("FriendAdapter", "解析头像失败", e);
                        ((android.app.Activity) context).runOnUiThread(() -> finalIvAvatar.setImageResource(R.drawable.default_avatar));
                    }
                } else {
                    Log.e("FriendAdapter", "下载头像响应码: " + response.code() + ", 用户: " + friend.getUsername());
                    ((android.app.Activity) context).runOnUiThread(() -> finalIvAvatar.setImageResource(R.drawable.default_avatar));
                }
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;
        TextView tvStatus;
        View vOnlineIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_friend_avatar);
            tvUsername = itemView.findViewById(R.id.tv_friend_username);
            tvStatus = itemView.findViewById(R.id.tv_friend_status);
            vOnlineIndicator = itemView.findViewById(R.id.v_online_indicator);
        }
    }
}
