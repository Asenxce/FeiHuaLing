package com.example.feihualinggame.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.feihualinggame.R;
import com.example.feihualinggame.adapter.FriendAdapter;
import com.example.feihualinggame.bean.Friend;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 好友列表Fragment
 * 优化：添加自动刷新定时器（15秒间隔），实时同步在线状态
 */
public class FriendFragment extends Fragment {
    private EditText etSearchFriend;
    private Button btnSearch;
    private Button btnAddFriend;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvFriends;
    private TextView tvFriendCount;
    private View layoutEmptyFriend;
    
    // 待处理好友申请
    private LinearLayout layoutPendingRequests;
    private TextView tvPendingCount;
    
    private List<Friend> friendList;
    private FriendAdapter adapter;
    
    // 自动刷新定时器
    private Timer autoRefreshTimer;
    private TimerTask autoRefreshTask;
    private static final long AUTO_REFRESH_INTERVAL = 15 * 1000; // 15 秒

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化控件
        etSearchFriend = view.findViewById(R.id.etSearchFriend);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnAddFriend = view.findViewById(R.id.btnAddFriend);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_friends);
        rvFriends = view.findViewById(R.id.rvFriends);
        tvFriendCount = view.findViewById(R.id.tv_friend_count);
        layoutEmptyFriend = view.findViewById(R.id.layout_empty_friend);
        
        // 初始化待处理好友申请入口
        layoutPendingRequests = view.findViewById(R.id.layout_pending_requests);
        tvPendingCount = view.findViewById(R.id.tv_pending_count);
        
        // 点击待处理申请入口，弹出处理列表
        layoutPendingRequests.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showPendingRequestsDialog();
        });

        // 初始化好友列表
        friendList = new ArrayList<>();
        adapter = new FriendAdapter(friendList);
        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriends.setAdapter(adapter);
        
        updateFriendCount();
        loadFriendList();

        // 设置下拉刷新颜色
        swipeRefresh.setColorSchemeResources(R.color.poetry_primary, R.color.poetry_accent, R.color.poetry_purple_dark);
        
        // 设置下拉刷新监听器
        swipeRefresh.setOnRefreshListener(() -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            loadFriendList();
        });

        // 搜索好友
        btnSearch.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            searchFriend();
        });

        // 添加好友
        btnAddFriend.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showAddFriendDialog();
        });
    }

    /**
     * 加载好友列表
     */
    private void loadFriendList() {
        android.util.Log.d("FriendFragment", "开始加载好友列表...");
        
        // 验证登录态
        String token = SharedPrefsUtil.getString(requireContext(), "token");
        if (token == null || token.isEmpty()) {
            android.util.Log.w("FriendFragment", "Token为空");
            requireActivity().runOnUiThread(() -> {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
                addSampleFriends();
            });
            return;
        }
        
        // 获取当前用户身份码
        String identityCode = SharedPrefsUtil.getUserId(requireContext());
        if (identityCode == null || identityCode.isEmpty()) {
            android.util.Log.w("FriendFragment", "身份码为空");
            requireActivity().runOnUiThread(() -> {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "用户信息不完整", Toast.LENGTH_SHORT).show();
                addSampleFriends();
            });
            return;
        }
        
        // 构建带参数的URL - 使用identityCode
        String url = ApiConstant.FRIEND_LIST + "?identityCode=" + identityCode;
        android.util.Log.d("FriendFragment", "请求URL: " + ApiConstant.BASE_URL + url);
        
        OkHttpUtil.getWithAuth(requireContext(), url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("FriendFragment", "加载好友列表失败: " + e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), "网络异常", Toast.LENGTH_SHORT).show();
                    addSampleFriends();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                int responseCode = response.code();
                
                android.util.Log.d("FriendFragment", "好友列表响应码: " + responseCode);
                android.util.Log.d("FriendFragment", "好友列表响应体: " + responseBody);
                
                requireActivity().runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful()) {
                        try {
                            // 解析后端返回的数据
                            parseFriendList(responseBody);
                        } catch (Exception e) {
                            android.util.Log.e("FriendFragment", "解析失败", e);
                            Toast.makeText(requireContext(), "数据解析失败", Toast.LENGTH_SHORT).show();
                            addSampleFriends();
                        }
                    } else {
                        android.util.Log.e("FriendFragment", "请求失败: " + responseCode);
                        Toast.makeText(requireContext(), "加载失败: " + responseCode, Toast.LENGTH_SHORT).show();
                        addSampleFriends();
                    }
                });
            }
        });
    }

    /**
     * 解析好友列表数据
     */
    private void parseFriendList(String json) {
        friendList.clear();
        
        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            
            // 尝试多种JSON结构
            com.google.gson.JsonArray friendsArray = null;
            
            // 结构1: {"code": 200, "data": [...]}
            if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                friendsArray = jsonObject.getAsJsonArray("data");
            }
            // 结构2: 直接是数组 [...]
            else if (jsonObject.isJsonArray()) {
                friendsArray = jsonObject.getAsJsonArray();
            }
            
            if (friendsArray != null && friendsArray.size() > 0) {
                android.util.Log.d("FriendFragment", "解析到 " + friendsArray.size() + " 个好友");
                
                for (int i = 0; i < friendsArray.size(); i++) {
                    com.google.gson.JsonObject friendObj = friendsArray.get(i).getAsJsonObject();
                    
                    // 根据后端返回的字段名解析
                    Long userId = friendObj.has("userId") ? friendObj.get("userId").getAsLong() : null;
                    String username = friendObj.has("username") ? friendObj.get("username").getAsString() : "未知用户";
                    String nickname = friendObj.has("nickname") && !friendObj.get("nickname").isJsonNull() 
                            ? friendObj.get("nickname").getAsString() : null;
                    String avatarUrl = friendObj.has("avatarUrl") && !friendObj.get("avatarUrl").isJsonNull() 
                            ? friendObj.get("avatarUrl").getAsString() : null;
                    int winCount = friendObj.has("winCount") ? friendObj.get("winCount").getAsInt() : 0;
                    int loseCount = friendObj.has("loseCount") ? friendObj.get("loseCount").getAsInt() : 0;
                    boolean online = friendObj.has("online") ? friendObj.get("online").getAsBoolean() : false;
                    
                    Friend friend = new Friend(username, winCount, loseCount, online);
                    friend.setUserId(userId);
                    friend.setNickname(nickname);
                    friend.setAvatarUrl(avatarUrl);
                    friendList.add(friend);
                    android.util.Log.d("FriendFragment", "添加好友: " + username + " (ID:" + userId + ", 昵称:" + nickname + ")");
                }
            } else {
                android.util.Log.w("FriendFragment", "好友列表为空");
            }
        } catch (Exception e) {
            android.util.Log.e("FriendFragment", "解析JSON失败", e);
            throw e;
        }
        
        adapter.notifyDataSetChanged();
        updateFriendCount();
    }

    /**
     * 添加示例好友（演示用）
     */
    private void addSampleFriends() {
        friendList.clear();
        friendList.add(new Friend("李白", 10, 5, true));
        friendList.add(new Friend("杜甫", 8, 7, false));
        friendList.add(new Friend("白居易", 12, 3, true));
        adapter.notifyDataSetChanged();
        updateFriendCount();
    }
    
    /**
     * 更新好友数量
     */
    private void updateFriendCount() {
        if (tvFriendCount != null) {
            tvFriendCount.setText(friendList.size() + "人");
        }
        // 更新空状态提示
        if (layoutEmptyFriend != null) {
            if (friendList.isEmpty()) {
                rvFriends.setVisibility(View.GONE);
                layoutEmptyFriend.setVisibility(View.VISIBLE);
            } else {
                rvFriends.setVisibility(View.VISIBLE);
                layoutEmptyFriend.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 搜索好友（前端本地搜索）
     */
    private void searchFriend() {
        String searchQuery = etSearchFriend.getText().toString().trim();
        
        if (searchQuery.isEmpty()) {
            Toast.makeText(requireContext(), "请输入用户名或身份码", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("FriendFragment", "搜索好友: " + searchQuery);
        
        // 在已加载的好友列表中搜索
        List<Friend> searchResults = new ArrayList<>();
        for (Friend friend : friendList) {
            if (friend.getUsername().contains(searchQuery)) {
                searchResults.add(friend);
            }
        }
        
        if (searchResults.isEmpty()) {
            Toast.makeText(requireContext(), "未找到匹配的好友", Toast.LENGTH_SHORT).show();
        } else {
            // 显示搜索结果
            showSearchResults(searchResults);
        }
    }
    
    /**
     * 显示搜索结果
     */
    private void showSearchResults(List<Friend> results) {
        StringBuilder message = new StringBuilder("找到 " + results.size() + " 个好友：\n\n");
        for (int i = 0; i < results.size(); i++) {
            Friend friend = results.get(i);
            message.append((i + 1)).append(". ").append(friend.getUsername())
                   .append(friend.isOnline() ? " [在线]" : " [离线]")
                   .append("\n");
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("搜索结果")
            .setMessage(message.toString())
            .setPositiveButton("确定", null)
            .show();
    }

    /**
     * 显示添加好友对话框
     */
    private void showAddFriendDialog() {
        // 创建自定义对话框
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_friend, null);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        
        EditText etIdentityCode = dialogView.findViewById(R.id.et_identity_code);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        
        // 添加音效
        btnCancel.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            dialog.dismiss();
        });
        
        btnConfirm.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            String identityCode = etIdentityCode.getText().toString().trim();
            
            if (identityCode.isEmpty()) {
                Toast.makeText(requireContext(), "请输入身份码", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (identityCode.length() != 8) {
                Toast.makeText(requireContext(), "身份码必须是 8 位数字", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 调用添加好友接口
            addFriend(identityCode);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    /**
     * 加载待处理好友申请
     */
    private void loadPendingRequests() {
        String token = SharedPrefsUtil.getString(requireContext(), "token");
        if (token == null || token.isEmpty()) {
            return;
        }
        
        String url = ApiConstant.FRIEND_PENDING_REQUESTS;
        
        OkHttpUtil.getWithAuth(requireContext(), url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 静默失败
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                        com.google.gson.JsonArray dataArray = null;
                        
                        if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                            dataArray = jsonObject.getAsJsonArray("data");
                        }
                        
                        final int pendingCount = dataArray != null ? dataArray.size() : 0;
                        
                        requireActivity().runOnUiThread(() -> {
                            if (pendingCount > 0) {
                                layoutPendingRequests.setVisibility(View.VISIBLE);
                                tvPendingCount.setText(pendingCount + " 个待处理");
                            } else {
                                layoutPendingRequests.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        // 静默解析失败
                    }
                }
            }
        });
    }
    
    /**
     * 显示待处理好友申请列表弹窗
     */
    private void showPendingRequestsDialog() {
        android.util.Log.d("FriendFragment", "显示待处理好友申请列表");
        
        loadPendingRequestsData(new PendingRequestsCallback() {
            @Override
            public void onLoaded(List<PendingRequest> requests) {
                if (requests.isEmpty()) {
                    Toast.makeText(requireContext(), "暂无待处理的好友申请", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 创建卡片容器
                LinearLayout container = new LinearLayout(requireContext());
                container.setOrientation(LinearLayout.VERTICAL);
                container.setBackgroundResource(R.drawable.card_background);
                container.setPadding(0, 0, 0, 0);
                
                // 标题栏
                LinearLayout headerLayout = new LinearLayout(requireContext());
                headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                headerLayout.setPadding(24, 20, 24, 16);
                headerLayout.setGravity(View.TEXT_ALIGNMENT_CENTER);
                headerLayout.setBackgroundResource(R.drawable.card_white);
                
                TextView titleView = new TextView(requireContext());
                titleView.setText("[待处理] 好友申请");
                titleView.setTextSize(16);
                titleView.setTextColor(getResources().getColor(R.color.poetry_text_primary));
                titleView.setTypeface(null, android.graphics.Typeface.BOLD);
                headerLayout.addView(titleView);
                
                TextView countView = new TextView(requireContext());
                countView.setText(requests.size() + " 个");
                countView.setTextSize(13);
                countView.setTextColor(getResources().getColor(R.color.poetry_primary));
                countView.setTypeface(null, android.graphics.Typeface.BOLD);
                headerLayout.addView(countView);
                
                container.addView(headerLayout);
                
                // 分割线
                View divider = new View(requireContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(0x20000000);
                container.addView(divider);
                
                // 每个申请项
                int index = 0;
                for (PendingRequest req : requests) {
                    final LinearLayout itemLayout = new LinearLayout(requireContext());
                    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                    itemLayout.setPadding(20, 14, 20, 14);
                    itemLayout.setGravity(View.TEXT_ALIGNMENT_CENTER);
                    
                    // 序号头像
                    TextView avatarView = new TextView(requireContext());
                    avatarView.setText(String.valueOf(index + 1));
                    avatarView.setTextSize(14);
                    avatarView.setTextColor(getResources().getColor(R.color.white));
                    avatarView.setTypeface(null, android.graphics.Typeface.BOLD);
                    avatarView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                    avatarView.setBackground(getResources().getDrawable(R.drawable.btn_primary_bg));
                    LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(72, 72);
                    avatarParams.setMargins(0, 0, 16, 0);
                    avatarView.setLayoutParams(avatarParams);
                    itemLayout.addView(avatarView);
                    
                    // 用户信息
                    LinearLayout infoLayout = new LinearLayout(requireContext());
                    infoLayout.setOrientation(LinearLayout.VERTICAL);
                    infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    
                    TextView nameView = new TextView(requireContext());
                    nameView.setText(req.getUsername());
                    nameView.setTextSize(15);
                    nameView.setTextColor(getResources().getColor(R.color.poetry_text_primary));
                    nameView.setTypeface(null, android.graphics.Typeface.BOLD);
                    infoLayout.addView(nameView);
                    
                    TextView tipView = new TextView(requireContext());
                    tipView.setText("想添加你为好友");
                    tipView.setTextSize(12);
                    tipView.setTextColor(getResources().getColor(R.color.poetry_text_secondary));
                    infoLayout.addView(tipView);
                    
                    itemLayout.addView(infoLayout);
                    
                    // 接受按钮
                    Button acceptBtn = new Button(requireContext());
                    acceptBtn.setText("接受");
                    acceptBtn.setTextSize(12);
                    acceptBtn.setTypeface(null, android.graphics.Typeface.BOLD);
                    acceptBtn.setTextColor(getResources().getColor(R.color.poetry_text_primary));
                    acceptBtn.setBackground(getResources().getDrawable(R.drawable.button_primary_bg));
                    LinearLayout.LayoutParams acceptParams = new LinearLayout.LayoutParams(0, 96, 1);
                    acceptParams.setMargins(0, 0, 6, 0);
                    acceptBtn.setLayoutParams(acceptParams);
                    acceptBtn.setOnClickListener(v -> {
                        AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                        handlePendingRequest(req.getRequesterId(), true, nameView, itemLayout);
                    });
                    itemLayout.addView(acceptBtn);
                    
                    // 拒绝按钮
                    Button rejectBtn = new Button(requireContext());
                    rejectBtn.setText("拒绝");
                    rejectBtn.setTextSize(12);
                    rejectBtn.setTypeface(null, android.graphics.Typeface.BOLD);
                    rejectBtn.setTextColor(getResources().getColor(R.color.poetry_text_secondary));
                    rejectBtn.setBackground(getResources().getDrawable(R.drawable.btn_secondary_bg));
                    LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(0, 96, 1);
                    rejectBtn.setLayoutParams(rejectParams);
                    rejectBtn.setOnClickListener(v -> {
                        AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                        handlePendingRequest(req.getRequesterId(), false, nameView, itemLayout);
                    });
                    itemLayout.addView(rejectBtn);
                    
                    container.addView(itemLayout);
                    
                    // 分割线（最后一项不显示）
                    if (index < requests.size() - 1) {
                        View itemDivider = new View(requireContext());
                        itemDivider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                        itemDivider.setBackgroundColor(0x10000000);
                        itemDivider.setPadding(112, 0, 0, 0);
                        container.addView(itemDivider);
                    }
                    
                    index++;
                }
                
                // 底部关闭按钮
                LinearLayout footerLayout = new LinearLayout(requireContext());
                footerLayout.setPadding(20, 12, 20, 16);
                footerLayout.setGravity(View.TEXT_ALIGNMENT_CENTER);
                
                View footerDivider = new View(requireContext());
                footerDivider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                footerDivider.setBackgroundColor(0x20000000);
                footerLayout.addView(footerDivider);
                container.addView(footerLayout);
                
                Button closeBtn = new Button(requireContext());
                closeBtn.setText("关闭");
                closeBtn.setTextSize(14);
                closeBtn.setTypeface(null, android.graphics.Typeface.BOLD);
                closeBtn.setTextColor(getResources().getColor(R.color.white));
                closeBtn.setBackgroundResource(R.drawable.btn_primary_bg);
                LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 112);
                closeBtn.setLayoutParams(closeParams);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                final AlertDialog dialog = builder.setView(container).create();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
                
                closeBtn.setOnClickListener(v -> {
                    AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                    dialog.dismiss();
                });
                container.addView(closeBtn);
            }
        });
    }
    
    /**
     * 加载待处理申请数据
     */
    private void loadPendingRequestsData(PendingRequestsCallback callback) {
        String token = SharedPrefsUtil.getString(requireContext(), "token");
        if (token == null || token.isEmpty()) {
            callback.onLoaded(new ArrayList<>());
            return;
        }
        
        String url = ApiConstant.FRIEND_PENDING_REQUESTS;
        
        OkHttpUtil.getWithAuth(requireContext(), url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onLoaded(new ArrayList<>());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<PendingRequest> requests = new ArrayList<>();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                        com.google.gson.JsonArray dataArray = jsonObject.has("data") ? jsonObject.getAsJsonArray("data") : null;
                        
                        if (dataArray != null) {
                            for (int i = 0; i < dataArray.size(); i++) {
                                com.google.gson.JsonObject obj = dataArray.get(i).getAsJsonObject();
                                PendingRequest req = new PendingRequest();
                                req.setRequestId(obj.has("requestId") ? obj.get("requestId").getAsLong() : 0);
                                req.setRequesterId(obj.has("requesterId") ? obj.get("requesterId").getAsLong() : 0);
                                req.setUsername(obj.has("nickname") && !obj.get("nickname").isJsonNull() 
                                        ? obj.get("nickname").getAsString() 
                                        : obj.get("username").getAsString());
                                requests.add(req);
                            }
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }
                
                final List<PendingRequest> finalRequests = requests;
                requireActivity().runOnUiThread(() -> callback.onLoaded(finalRequests));
            }
        });
    }
    
    /**
     * 处理待处理申请（接受/拒绝）
     */
    private void handlePendingRequest(Long requesterId, boolean accept, TextView nameView, LinearLayout itemLayout) {
        String action = accept ? "接受" : "拒绝";
        String url = accept ? ApiConstant.FRIEND_ACCEPT : ApiConstant.FRIEND_REJECT;
        
        String fullUrl = url + "?friendId=" + requesterId;
        
        OkHttpUtil.postWithAuth(requireContext(), fullUrl, "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), action + "失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                        int code = jsonObject.has("code") ? jsonObject.get("code").getAsInt() : -1;
                        String message = jsonObject.has("message") ? jsonObject.get("message").getAsString() : action + "成功";
                        
                        requireActivity().runOnUiThread(() -> {
                            if (code == 200) {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                // 从列表中移除
                                itemLayout.setVisibility(View.GONE);
                                // 刷新待处理列表
                                loadPendingRequests();
                            } else {
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), action + "失败", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), action + "失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 待处理申请回调接口
     */
    interface PendingRequestsCallback {
        void onLoaded(List<PendingRequest> requests);
    }
    
    /**
     * 待处理申请数据类
     */
    static class PendingRequest {
        private long requestId;
        private long requesterId;
        private String username;
        
        public long getRequestId() { return requestId; }
        public void setRequestId(long requestId) { this.requestId = requestId; }
        
        public long getRequesterId() { return requesterId; }
        public void setRequesterId(long requesterId) { this.requesterId = requesterId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    /**
     * 添加好友
     */
    private void addFriend(String identityCode) {
        // 验证登录态
        String token = SharedPrefsUtil.getString(requireContext(), "token");
        if (token == null || token.isEmpty()) {
            android.util.Log.e("FriendFragment", "Token为空，请先登录");
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        // 获取当前用户名
        String currentUsername = SharedPrefsUtil.getUsername(requireContext());
        
        // 检查用户名是否为空
        if (currentUsername == null || currentUsername.isEmpty()) {
            android.util.Log.e("FriendFragment", "当前用户名为空，请先登录");
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "用户信息不完整，请重新登录", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        // 构造请求参数 - 使用身份码而非userId
        com.google.gson.JsonObject requestJson = new com.google.gson.JsonObject();
        requestJson.addProperty("currentUsername", currentUsername);
        requestJson.addProperty("targetIdentityCode", identityCode);
        String json = requestJson.toString();
        
        android.util.Log.d("FriendFragment", "添加好友请求 - 用户名: " + currentUsername + ", 目标身份码: " + identityCode);
        
        // 显示加载提示
        Toast.makeText(requireContext(), "正在添加...", Toast.LENGTH_SHORT).show();
        
        OkHttpUtil.postWithAuth(requireContext(), ApiConstant.FRIEND_ADD, json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("FriendFragment", "网络请求失败", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "网络异常，请检查网络连接", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                int responseCode = response.code();
                
                // 打印响应信息
                android.util.Log.d("FriendFragment", "添加好友响应 - 响应码: " + responseCode + ", 响应体: " + responseBody);
                
                requireActivity().runOnUiThread(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        // 解析响应判断是否真正成功
                        try {
                            com.google.gson.JsonObject responseJson = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                            int code = responseJson.get("code").getAsInt();
                            String message = responseJson.has("message") ? responseJson.get("message").getAsString() : "";
                            
                            if (code == 200) {
                                android.util.Log.d("FriendFragment", "好友申请已发送");
                                Toast.makeText(requireContext(), "好友申请已发送，等待对方确认", Toast.LENGTH_SHORT).show();
                                // 重新加载好友列表
                                loadFriendList();
                            } else {
                                android.util.Log.e("FriendFragment", "添加失败: " + message);
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            // 如果解析失败，默认认为成功
                            android.util.Log.d("FriendFragment", "好友添加成功（默认）");
                            Toast.makeText(requireContext(), "好友添加成功！", Toast.LENGTH_SHORT).show();
                            loadFriendList();
                        }
                    } else if (responseCode == 400) {
                        // 请求参数错误
                        android.util.Log.e("FriendFragment", "400 - 请求参数错误");
                        Toast.makeText(requireContext(), "身份码格式错误", Toast.LENGTH_SHORT).show();
                    } else if (responseCode == 403) {
                        // 认证失败，Token可能已过期
                        android.util.Log.e("FriendFragment", "403 - Token失效，请重新登录");
                        Toast.makeText(requireContext(), "登录已过期，请重新登录", Toast.LENGTH_SHORT).show();
                        // 清除本地凭证
                        SharedPrefsUtil.clearUser(requireContext());
                    } else if (responseCode == 404) {
                        // 用户不存在
                        android.util.Log.e("FriendFragment", "404 - 用户不存在");
                        Toast.makeText(requireContext(), "用户不存在，请检查身份码", Toast.LENGTH_SHORT).show();
                    } else if (responseCode == 409) {
                        // 已经是好友
                        android.util.Log.w("FriendFragment", "409 - 已经是好友");
                        Toast.makeText(requireContext(), "已经是好友了", Toast.LENGTH_SHORT).show();
                    } else if (responseCode == 500) {
                        // 服务器内部错误
                        android.util.Log.e("FriendFragment", "500 - 服务器内部错误");
                        android.util.Log.e("FriendFragment", "响应体: " + responseBody);
                        
                        // 尝试解析错误信息
                        String errorMessage = "服务器内部错误";
                        try {
                            com.google.gson.JsonObject errorJson = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                            if (errorJson.has("message")) {
                                errorMessage = errorJson.get("message").getAsString();
                            }
                        } catch (Exception e) {
                            // 忽略解析错误
                        }
                        
                        Toast.makeText(requireContext(), "服务器错误：" + errorMessage, Toast.LENGTH_LONG).show();
                    } else {
                        // 其他错误
                        android.util.Log.e("FriendFragment", "未知错误: " + responseCode);
                        Toast.makeText(requireContext(), "添加失败：" + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    /**
     * 启动自动刷新定时器（每15秒刷新好友列表）
     */
    private void startAutoRefresh() {
        stopAutoRefresh(); // 先停止旧的
        
        autoRefreshTask = new TimerTask() {
            @Override
            public void run() {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        // 静默刷新，不显示 Toast
                        loadFriendListSilent();
                    });
                }
            }
        };
        
        autoRefreshTimer = new Timer();
        autoRefreshTimer.scheduleAtFixedRate(autoRefreshTask, AUTO_REFRESH_INTERVAL, AUTO_REFRESH_INTERVAL);
        android.util.Log.d("FriendFragment", "自动刷新定时器已启动");
    }
    
    /**
     * 停止自动刷新定时器
     */
    private void stopAutoRefresh() {
        if (autoRefreshTask != null) {
            autoRefreshTask.cancel();
            autoRefreshTask = null;
        }
        if (autoRefreshTimer != null) {
            autoRefreshTimer.cancel();
            autoRefreshTimer = null;
        }
        android.util.Log.d("FriendFragment", "自动刷新定时器已停止");
    }
    
    /**
     * 静默加载好友列表（不显示 Toast 和加载提示）
     */
    private void loadFriendListSilent() {
        String token = SharedPrefsUtil.getString(requireContext(), "token");
        if (token == null || token.isEmpty()) {
            return;
        }
        
        String identityCode = SharedPrefsUtil.getUserId(requireContext());
        if (identityCode == null || identityCode.isEmpty()) {
            return;
        }
        
        String url = ApiConstant.FRIEND_LIST + "?identityCode=" + identityCode;
        
        OkHttpUtil.getWithAuth(requireContext(), url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 静默失败
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        friendList.clear();
                        
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                        com.google.gson.JsonArray friendsArray = null;
                        
                        if (jsonObject.has("data") && jsonObject.get("data").isJsonArray()) {
                            friendsArray = jsonObject.getAsJsonArray("data");
                        } else if (jsonObject.isJsonArray()) {
                            friendsArray = jsonObject.getAsJsonArray();
                        }
                        
                        if (friendsArray != null && friendsArray.size() > 0) {
                            for (int i = 0; i < friendsArray.size(); i++) {
                                com.google.gson.JsonObject friendObj = friendsArray.get(i).getAsJsonObject();
                                
                                Long userId = friendObj.has("userId") ? friendObj.get("userId").getAsLong() : null;
                                String username = friendObj.has("username") ? friendObj.get("username").getAsString() : "未知用户";
                                String nickname = friendObj.has("nickname") && !friendObj.get("nickname").isJsonNull() 
                                        ? friendObj.get("nickname").getAsString() : null;
                                String avatarUrl = friendObj.has("avatarUrl") && !friendObj.get("avatarUrl").isJsonNull() 
                                        ? friendObj.get("avatarUrl").getAsString() : null;
                                int winCount = friendObj.has("winCount") ? friendObj.get("winCount").getAsInt() : 0;
                                int loseCount = friendObj.has("loseCount") ? friendObj.get("loseCount").getAsInt() : 0;
                                boolean online = friendObj.has("online") ? friendObj.get("online").getAsBoolean() : false;
                                
                                Friend friend = new Friend(username, winCount, loseCount, online);
                                friend.setUserId(userId);
                                friend.setNickname(nickname);
                                friend.setAvatarUrl(avatarUrl);
                                friendList.add(friend);
                            }
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            updateFriendCount();
                        });
                    } catch (Exception e) {
                        // 静默解析失败
                    }
                }
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadFriendList();
        loadPendingRequests();
        startAutoRefresh();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopAutoRefresh();
    }
}
