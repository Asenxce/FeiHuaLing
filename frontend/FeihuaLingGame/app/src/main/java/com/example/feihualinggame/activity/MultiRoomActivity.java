package com.example.feihualinggame.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.adapter.RoomPlayerAdapter;
import com.example.feihualinggame.bean.RoomInfoBean;
import com.example.feihualinggame.bean.RoomPlayerBean;
import com.example.feihualinggame.bean.WebSocketMessageBean;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.example.feihualinggame.utils.WebSocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MultiRoomActivity extends AppCompatActivity {
    private static final String TAG = "多人房间";
    private Gson gson = new Gson();
    private Handler handler = new Handler(Looper.getMainLooper());

    private String roomId;
    private String roomCode;
    private boolean isCreator;
    private long myUserId;
    private String battleId;
    private boolean battleStarted = false;

    private TextView tvRoomCode;
    private TextView tvGameInfo;
    private TextView tvPlayerCount;
    private RecyclerView rvPlayers;
    private Button btnReady;
    private Button btnStart;
    private Button btnLeave;
    private Button btnInvite;
    private View btnBack;

    private RoomPlayerAdapter adapter;
    private List<RoomPlayerBean> playerList = new ArrayList<>();
    private boolean myReady = false;
    private RoomInfoBean currentRoom;
    private Runnable pollingRunnable;
    private WebSocketClient.MessageCallback wsCallback;
    private boolean startRequesting = false;
    private boolean wsSubscribed = false;

    private void showInviteFriendDialog() {
        String identityCode = SharedPrefsUtil.getUserId(this);
        if (identityCode == null || identityCode.isEmpty()) {
            Toast.makeText(this, "用户信息不完整", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = ApiConstant.FRIEND_LIST + "?identityCode=" + identityCode;
        OkHttpUtil.getWithAuth(this, url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> Toast.makeText(MultiRoomActivity.this, "加载好友列表失败", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                handler.post(() -> {
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        List<FriendItem> friends = new ArrayList<>();
                        // 兼容 {"code":200,"data":[...]} 和直接返回数组两种结构
                        JsonArray dataArray = null;
                        if (json.has("data") && json.get("data").isJsonArray()) {
                            dataArray = json.getAsJsonArray("data");
                        } else if (json.isJsonArray()) {
                            dataArray = json.getAsJsonArray();
                        }
                        if (dataArray != null) {
                            for (JsonElement element : dataArray) {
                                JsonObject friendObj = element.getAsJsonObject();
                                FriendItem item = new FriendItem();
                                item.userId = friendObj.has("userId") ? friendObj.get("userId").getAsLong() : 0;
                                item.nickname = friendObj.has("nickname") && !friendObj.get("nickname").isJsonNull()
                                        ? friendObj.get("nickname").getAsString() : "";
                                if (item.nickname.isEmpty()) {
                                    item.nickname = friendObj.has("username") ? friendObj.get("username").getAsString() : "未知用户";
                                }
                                item.online = friendObj.has("online") && friendObj.get("online").getAsBoolean();
                                if (item.userId > 0) {
                                    friends.add(item);
                                }
                            }
                        }
                        showFriendSelectionDialog(friends);
                    } catch (Exception e) {
                        Log.e(TAG, "解析好友列表失败", e);
                        Toast.makeText(MultiRoomActivity.this, "加载好友列表失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showFriendSelectionDialog(List<FriendItem> friends) {
        List<FriendItem> onlineFriends = new ArrayList<>();
        for (FriendItem f : friends) {
            if (f.online) onlineFriends.add(f);
        }
        if (onlineFriends.isEmpty()) {
            Toast.makeText(this, "没有在线好友", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[onlineFriends.size()];
        for (int i = 0; i < onlineFriends.size(); i++) {
            names[i] = onlineFriends.get(i).nickname;
        }

        new AlertDialog.Builder(this)
            .setTitle("邀请在线好友")
            .setItems(names, (dialog, which) -> {
                FriendItem selected = onlineFriends.get(which);
                sendRoomInvite(selected.userId);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void sendRoomInvite(long inviteeId) {
        JsonObject body = new JsonObject();
        body.addProperty("inviteeId", inviteeId);
        body.addProperty("roomId", roomId);
        OkHttpUtil.postWithAuth(this, "room/invite/create", body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> Toast.makeText(MultiRoomActivity.this, "邀请发送失败", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                handler.post(() -> {
                    try {
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            Toast.makeText(MultiRoomActivity.this, "邀请已发送", Toast.LENGTH_SHORT).show();
                        } else {
                            String msg = json.has("message") ? json.get("message").getAsString() : "邀请失败";
                            Toast.makeText(MultiRoomActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MultiRoomActivity.this, "邀请失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showInviteReceivedDialog(JsonObject inviteData) {
        String inviterName = inviteData.has("inviterName") ? inviteData.get("inviterName").getAsString() : "未知用户";
        String inviteCode = inviteData.has("roomCode") ? inviteData.get("roomCode").getAsString() : "";
        String inviteToken = inviteData.has("inviteToken") ? inviteData.get("inviteToken").getAsString() : "";

        new AlertDialog.Builder(this)
            .setTitle("收到房间邀请")
            .setMessage(inviterName + " 邀请你加入多人对战房间\n房间码: " + inviteCode)
            .setPositiveButton("接受", (dialog, which) -> acceptRoomInvite(inviteToken))
            .setNegativeButton("拒绝", (dialog, which) -> rejectRoomInvite(inviteToken))
            .setCancelable(false)
            .show();
    }

    private void acceptRoomInvite(String inviteToken) {
        OkHttpUtil.postWithAuth(this, "room/invite/" + inviteToken + "/accept", "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> Toast.makeText(MultiRoomActivity.this, "接受邀请失败", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                handler.post(() -> {
                    try {
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            JsonObject data = json.getAsJsonObject("data");
                            String newRoomId = data.get("roomId").getAsString();
                            String newRoomCode = data.get("roomCode").getAsString();
                            Toast.makeText(MultiRoomActivity.this, "已加入房间", Toast.LENGTH_SHORT).show();
                            roomId = newRoomId;
                            roomCode = newRoomCode;
                            isCreator = false;
                            btnStart.setVisibility(View.GONE);
                            tvRoomCode.setText("房间码: " + newRoomCode);
                            loadRoomDetailAndInitWs();
                        } else {
                            String msg = json.has("message") ? json.get("message").getAsString() : "接受失败";
                            Toast.makeText(MultiRoomActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MultiRoomActivity.this, "接受邀请失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void rejectRoomInvite(String inviteToken) {
        OkHttpUtil.postWithAuth(this, "room/invite/" + inviteToken + "/cancel", "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {}
        });
    }

    private static class FriendItem {
        long userId;
        String nickname;
        String identityCode;
        boolean online;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_room);

        SystemUIUtil.setupImmersiveStatusBar(this, R.color.poetry_card_background, true);
        SystemUIUtil.hideNavigationBarIndicator(this);

        roomId = getIntent().getStringExtra("roomId");
        roomCode = getIntent().getStringExtra("roomCode");
        isCreator = getIntent().getBooleanExtra("isCreator", false);
        myUserId = 0;

        initViews();
        setupListeners();
        // WebSocket连接成功后会自动触发loadRoomDetailAndInitWs()
        connectWebSocket();

        AudioController.getInstance().playBGM(AudioController.SCENE_BATTLE);
    }

    private void initViews() {
        tvRoomCode = findViewById(R.id.tv_room_code);
        tvGameInfo = findViewById(R.id.tv_game_info);
        tvPlayerCount = findViewById(R.id.tv_player_count);
        rvPlayers = findViewById(R.id.rv_players);
        btnReady = findViewById(R.id.btn_ready);
        btnStart = findViewById(R.id.btn_start);
        btnLeave = findViewById(R.id.btn_leave);
        btnInvite = findViewById(R.id.btn_invite);
        btnBack = findViewById(R.id.btn_back);

        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomPlayerAdapter(playerList);
        rvPlayers.setAdapter(adapter);

        if (isCreator) {
            btnStart.setVisibility(View.VISIBLE);
            btnStart.setEnabled(false);
        } else {
            btnStart.setVisibility(View.GONE);
        }

        ButtonAnimationHelper.addCombinedEffect(btnReady);
        ButtonAnimationHelper.addCombinedEffect(btnStart);
        ButtonAnimationHelper.addCombinedEffect(btnLeave);
        ButtonAnimationHelper.addCombinedEffect(btnInvite);
        ButtonAnimationHelper.addCombinedEffect(btnBack);

        tvRoomCode.setText("房间码: " + roomCode);
        tvRoomCode.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("roomCode", roomCode));
            Toast.makeText(this, "房间码已复制", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            leaveRoom();
        });

        btnLeave.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            leaveRoom();
        });

        btnReady.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            toggleReady();
        });

        btnStart.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            if (isCreator) {
                startBattle();
            }
        });

        btnInvite.setOnClickListener(v -> {
            showInviteFriendDialog();
        });
    }

    /**
     * 初始化 WebSocket 连接，连接成功后自动加载房间信息并注册订阅
     */
    private void connectWebSocket() {
        String sessionId = SharedPrefsUtil.getString(this, "sessionId");
        WebSocketClient.getInstance().setSessionId(sessionId);
        wsCallback = new WebSocketClient.MessageCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "WebSocket已连接");
                handler.post(() -> {
                    if (myUserId > 0) {
                        WebSocketClient.getInstance().register(myUserId);
                        subscribeRoomOnWs();
                    } else {
                        loadRoomDetailAndInitWs();
                    }
                });
            }
            @Override
            public void onDisconnected() {
                Log.w(TAG, "WebSocket断开连接");
                wsSubscribed = false;
                if (!isFinishing() && !battleStarted) {
                    handler.postDelayed(() -> {
                        if (!isFinishing() && !battleStarted && !WebSocketClient.getInstance().isConnected()) {
                            WebSocketClient.getInstance().connect(ApiConstant.WS_URL);
                        }
                    }, 3000);
                }
            }
            @Override
            public void onMessage(WebSocketMessageBean message) {
                handleWebSocketMessage(message);
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "WS错误: " + error);
            }
        };
        WebSocketClient.getInstance().addCallback(wsCallback);

        if (WebSocketClient.getInstance().isConnected()) {
            Log.d(TAG, "WebSocket已连接，直接初始化");
            handler.post(() -> {
                if (myUserId > 0) {
                    WebSocketClient.getInstance().register(myUserId);
                    subscribeRoomOnWs();
                } else {
                    loadRoomDetailAndInitWs();
                }
            });
        } else {
            WebSocketClient.getInstance().connect(ApiConstant.WS_URL);
            handler.postDelayed(() -> {
                if (!isFinishing() && !battleStarted && myUserId == 0) {
                    Log.w(TAG, "WebSocket连接超时，通过HTTP加载房间信息");
                    loadRoomDetailAndInitWs();
                }
            }, 5000);
        }
    }

    private void loadRoomDetailAndInitWs() {
        OkHttpUtil.getWithAuth(this, ApiConstant.ROOM_DETAIL + roomId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> {
                    Log.e(TAG, "加载房间详情失败: " + e.getMessage());
                    handler.postDelayed(() -> {
                        if (!isFinishing() && !battleStarted) {
                            loadRoomDetailAndInitWs();
                        }
                    }, 2000);
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                handler.post(() -> {
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            RoomInfoBean room = gson.fromJson(json.get("data"), RoomInfoBean.class);
                            findMyUserIdFromRoom(room);
                            if (myUserId > 0) {
                                registerAndSubscribeRoom();
                            }
                            updateUI(room);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析房间信息失败", e);
                    }
                    startPolling();
                });
            }
        });
    }

    private void findMyUserIdFromRoom(RoomInfoBean room) {
        if (myUserId > 0) return;
        if (room.getPlayers() != null) {
            String storedUserIdStr = SharedPrefsUtil.getUserId(this);
            if (storedUserIdStr != null && !storedUserIdStr.isEmpty()) {
                try {
                    long storedUserId = Long.parseLong(storedUserIdStr);
                    for (RoomPlayerBean p : room.getPlayers()) {
                        if (p.getUserId() == storedUserId) {
                            myUserId = p.getUserId();
                            Log.d(TAG, "通过userId找到 myUserId: " + myUserId);
                            return;
                        }
                    }
                } catch (NumberFormatException ignored) {}

                for (RoomPlayerBean p : room.getPlayers()) {
                    if (storedUserIdStr.equals(p.getIdentityCode())) {
                        myUserId = p.getUserId();
                        Log.d(TAG, "通过identityCode找到 myUserId: " + myUserId);
                        return;
                    }
                }
            }
        }
        if (myUserId == 0 && isCreator && room.getCreatorId() > 0) {
            myUserId = room.getCreatorId();
            Log.d(TAG, "通过房主ID找到 myUserId: " + myUserId);
        }
    }

    private void registerAndSubscribeRoom() {
        if (myUserId <= 0) {
            Log.w(TAG, "myUserId 未知，跳过注册和订阅");
            return;
        }
        WebSocketClient.getInstance().register(myUserId);
        subscribeRoomOnWs();
    }

    private void subscribeRoomOnWs() {
        if (wsSubscribed) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "SUBSCRIBE_ROOM");
        msg.addProperty("roomId", roomId);
        msg.addProperty("userId", myUserId);
        WebSocketClient.getInstance().sendMessage(msg.toString());
        wsSubscribed = true;
        Log.d(TAG, "发送房间订阅: roomId=" + roomId + ", userId=" + myUserId);
    }

    private void startPolling() {
        stopPolling();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || battleStarted) return;
                Log.d(TAG, "轮询刷新房间信息 (wsSubscribed=" + wsSubscribed + ")");
                loadRoomDetail();
                handler.postDelayed(this, 3000);
            }
        };
        handler.postDelayed(pollingRunnable, 3000);
    }

    private void stopPolling() {
        if (pollingRunnable != null) {
            handler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    private void handleWebSocketMessage(WebSocketMessageBean msg) {
        String type = msg.getType();
        Log.d(TAG, "收到WS消息: type=" + type + ", roomId=" + msg.getRoomId());

        // INVITE_RECEIVED 不受 roomId 过滤，因为邀请可能来自任意房间
        if ("INVITE_RECEIVED".equals(type)) {
            try {
                JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                handler.post(() -> showInviteReceivedDialog(jo));
            } catch (Exception e) {
                Log.e(TAG, "解析INVITE_RECEIVED失败", e);
            }
            return;
        }

        if (msg.getRoomId() != null && !msg.getRoomId().isEmpty() && !msg.getRoomId().equals(roomId)) {
            return;
        }

        switch (type) {
            case "PLAYER_JOINED":
            case "PLAYER_LEFT":
            case "READY_UPDATE":
                handler.post(() -> {
                    loadRoomDetail();
                    restartPollingIfNeeded();
                });
                break;
            case "ROOM_UPDATE":
                handler.post(() -> {
                    loadRoomDetail();
                    restartPollingIfNeeded();
                });
                break;
            case "HOST_TRANSFERRED":
                try {
                    JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                    long newHostId = jo.get("newHostId").getAsLong();
                    isCreator = (newHostId == myUserId);
                    handler.post(() -> {
                        btnStart.setVisibility(isCreator ? View.VISIBLE : View.GONE);
                        btnStart.setEnabled(false);
                        Toast.makeText(MultiRoomActivity.this,
                                isCreator ? "你已成为房主" : "房主已更换", Toast.LENGTH_SHORT).show();
                        loadRoomDetail();
                        restartPollingIfNeeded();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "解析HOST_TRANSFERRED失败", e);
                }
                break;
            case "BATTLE_START":
                Log.d(TAG, "收到BATTLE_START: " + msg.getPayload());
                try {
                    JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                    battleId = jo.get("battleId").getAsString();
                    int timeLimit = jo.get("timeLimit").getAsInt();
                    String keyword = jo.get("keyword").getAsString();
                    long firstTurnUserId = jo.has("firstTurnUserId") ? jo.get("firstTurnUserId").getAsLong() : 0;
                    String firstTurnNickname = jo.has("firstTurnNickname") ? jo.get("firstTurnNickname").getAsString() : "";
                    String gameType = jo.has("gameType") ? jo.get("gameType").getAsString() : "ENTERTAINMENT";
                    int helpLimit = jo.has("helpLimit") ? jo.get("helpLimit").getAsInt() : 3;
                    battleStarted = true;
                    stopPolling();
                    final long ftu = firstTurnUserId;
                    final String ftn = firstTurnNickname;
                    final String gt = gameType;
                    final int hl = helpLimit;
                    handler.post(() -> navigateToBattle(timeLimit, keyword, ftu, ftn, gt, hl));
                } catch (Exception e) {
                    Log.e(TAG, "解析BATTLE_START失败: " + msg.getPayload(), e);
                }
                break;
            case "ROOM_DISSOLVED":
                handler.post(() -> {
                    Toast.makeText(this, "房间已解散", Toast.LENGTH_SHORT).show();
                    finish();
                });
                break;
            case "KICKED":
                handler.post(() -> {
                    Toast.makeText(this, "你被房主移出房间", Toast.LENGTH_SHORT).show();
                    finish();
                });
                break;
        }
    }

    private void restartPollingIfNeeded() {
        if (!isFinishing() && !battleStarted) {
            startPolling();
        }
    }

    private void loadRoomDetail() {
        OkHttpUtil.getWithAuth(this, ApiConstant.ROOM_DETAIL + roomId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> Toast.makeText(MultiRoomActivity.this, "加载失败", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                handler.post(() -> {
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            RoomInfoBean room = gson.fromJson(json.get("data"), RoomInfoBean.class);
                            updateUI(room);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析房间信息失败", e);
                    }
                });
            }
        });
    }

    private void updateUI(RoomInfoBean room) {
        currentRoom = room;
        if (myUserId == 0) {
            findMyUserIdFromRoom(room);
            if (myUserId > 0) {
                registerAndSubscribeRoom();
            }
        }

        if (room.getStatus().equals("BATTLE") && !battleStarted
                && room.getBattleId() != null && !room.getBattleId().isEmpty()) {
            battleId = room.getBattleId();
            battleStarted = true;
            String gt = room.getGameType() != null ? room.getGameType() : "ENTERTAINMENT";
            navigateToBattle(room.getTimeLimit(), buildKeywordInfo(room), room.getCurrentTurnUserId(), "", gt, 3);
            return;
        }

        playerList.clear();
        if (room.getPlayers() != null) {
            playerList.addAll(room.getPlayers());
        }
        adapter.notifyDataSetChanged();

        tvPlayerCount.setText(room.getPlayerCount() + "/" + room.getMaxPlayers() + "人  已准备:" + room.getReadyCount());

        StringBuilder info = new StringBuilder();
        info.append("模式: ").append(getModeName(room.getGameMode()));
        String keywordInfo = buildKeywordInfo(room);
        if (keywordInfo != null && !keywordInfo.isEmpty() && !"未知".equals(keywordInfo)) {
            info.append("\n关键字: ").append(keywordInfo);
        }
        info.append("\n限时: ").append(room.getTimeLimit()).append("秒 | ");
        info.append("容错: ").append(room.getFaultLimit()).append("次 | ");
        info.append(room.getGameType().equals("COMPETITIVE") ? "竞技" : "娱乐");
        tvGameInfo.setText(info.toString());

        for (RoomPlayerBean p : playerList) {
            if (p.getUserId() == myUserId) {
                myReady = p.isReady();
                btnReady.setText(myReady ? "取消准备" : "准备");
                break;
            }
        }

        if (isCreator) {
            boolean canStart = room.getStatus().equals("WAITING")
                    && room.getPlayerCount() >= room.getMinPlayers()
                    && room.getReadyCount() == room.getPlayerCount();
            btnStart.setEnabled(canStart);
        }
    }

    private void toggleReady() {
        if (myUserId <= 0) {
            Toast.makeText(this, "用户信息未加载，请稍后", Toast.LENGTH_SHORT).show();
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("userId", myUserId);
        OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_READY + roomId + "/ready", body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handler.post(() -> loadRoomDetail());
            }
        });
    }

    private void startBattle() {
        if (startRequesting) {
            return;
        }
        if (myUserId <= 0) {
            Toast.makeText(this, "用户信息未加载", Toast.LENGTH_SHORT).show();
            return;
        }
        startRequesting = true;
        btnStart.setEnabled(false);
        Log.w(TAG, "=== startBattle 请求: roomId=" + roomId + ", userId=" + myUserId);

        JsonObject body = new JsonObject();
        body.addProperty("userId", myUserId);
        String fullUrl = ApiConstant.ROOM_START + roomId + "/start";
        OkHttpUtil.postWithAuth(this, fullUrl, body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "startBattle 网络失败: " + e.getMessage());
                handler.post(() -> {
                    startRequesting = false;
                    btnStart.setEnabled(true);
                    Toast.makeText(MultiRoomActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = null;
                try {
                    okhttp3.ResponseBody rb = response.body();
                    if (rb == null) {
                        Log.e(TAG, "startBattle 响应body为null, code=" + response.code());
                        handler.post(() -> {
                            startRequesting = false;
                            btnStart.setEnabled(true);
                            Toast.makeText(MultiRoomActivity.this, "服务器返回空", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    respBody = rb.string();
                } catch (IOException e) {
                    Log.e(TAG, "startBattle 读取body异常", e);
                    handler.post(() -> {
                        startRequesting = false;
                        btnStart.setEnabled(true);
                        Toast.makeText(MultiRoomActivity.this, "读取响应失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Log.w(TAG, "=== startBattle 响应(" + response.code() + "): [" + respBody + "]");

                if (respBody == null || respBody.isEmpty()) {
                    handler.post(() -> {
                        startRequesting = false;
                        btnStart.setEnabled(true);
                        Toast.makeText(MultiRoomActivity.this, "响应为空", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                final String finalBody = respBody;
                handler.post(() -> {
                    startRequesting = false;
                    try {
                        JsonObject json = gson.fromJson(finalBody, JsonObject.class);
                        if (!json.has("code")) {
                            btnStart.setEnabled(true);
                            Toast.makeText(MultiRoomActivity.this, "响应格式异常", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int code = json.get("code").getAsInt();
                        if (code != 200) {
                            String msg = json.has("message") ? json.get("message").getAsString() : "开局失败";
                            btnStart.setEnabled(true);
                            Toast.makeText(MultiRoomActivity.this, msg, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (battleStarted) {
                            return;
                        }
                        JsonObject data = json.getAsJsonObject("data");
                        battleId = data.get("battleId").getAsString();
                        battleStarted = true;
                        stopPolling();
                        String kw = data.has("keyword") && !data.get("keyword").isJsonNull() ? data.get("keyword").getAsString() : "";
                        int tl = 60;
                        if (data.has("timeLimit")) tl = data.get("timeLimit").getAsInt();
                        else if (currentRoom != null) tl = currentRoom.getTimeLimit();
                        long ftu = data.has("firstPlayerId") ? data.get("firstPlayerId").getAsLong() : 0;
                        String ftn = data.has("firstPlayerNickname") ? data.get("firstPlayerNickname").getAsString() : "";
                        String gt = currentRoom != null && currentRoom.getGameType() != null ? currentRoom.getGameType() : "ENTERTAINMENT";
                        navigateToBattle(tl, kw, ftu, ftn, gt, 3);
                    } catch (Exception e) {
                        Log.e(TAG, "startBattle JSON解析失败, body=[" + finalBody + "]", e);
                        btnStart.setEnabled(true);
                        Toast.makeText(MultiRoomActivity.this, "响应解析异常，请重试", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void leaveRoom() {
        if (battleStarted) {
            finish();
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("userId", myUserId);
        OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_LEAVE + roomId + "/leave", body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handler.post(() -> finish());
            }
        });
    }

    private void navigateToBattle(int timeLimit, String keyword, long firstTurnUserId, String firstTurnNickname) {
        navigateToBattle(timeLimit, keyword, firstTurnUserId, firstTurnNickname, "ENTERTAINMENT", 3);
    }

    private void navigateToBattle(int timeLimit, String keyword, long firstTurnUserId, String firstTurnNickname,
                                  String gameType, int helpLimit) {
        Intent intent = new Intent(MultiRoomActivity.this, MultiBattleActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("battleId", battleId);
        intent.putExtra("timeLimit", timeLimit);
        intent.putExtra("keyword", keyword);
        intent.putExtra("userId", myUserId);
        intent.putExtra("firstTurnUserId", firstTurnUserId);
        intent.putExtra("firstTurnNickname", firstTurnNickname);
        intent.putExtra("gameType", gameType != null ? gameType : "ENTERTAINMENT");
        intent.putExtra("helpLimit", helpLimit);
        startActivity(intent);
        handler.postDelayed(() -> {
            if (wsCallback != null) {
                WebSocketClient.getInstance().removeCallback(wsCallback);
            }
        }, 1000);
    }

    private String getModeName(String mode) {
        switch (mode) {
            case "SIMPLE": return "单关键字飞花令";
            case "POSITION": return "位置飞花令";
            case "DOUBLE_KEYWORD": return "双关键字飞花令";
            case "CHAIN": return "首尾接龙飞花令";
            case "COLOR": return "颜色飞花令";
            case "NUMBER": return "数字飞花令";
            case "FORBIDDEN": return "反飞花令";
            case "CUSTOM": return "自定义飞花令";
            default: return mode;
        }
    }

    private String buildKeywordInfo(RoomInfoBean room) {
        switch (room.getGameMode()) {
            case "SIMPLE": return room.getKeyword();
            case "POSITION": {
                String kw = room.getKeyword();
                if (kw != null && room.getKeywordPosition() != null) {
                    return kw + "(第" + room.getKeywordPosition() + "字)";
                }
                return kw;
            }
            case "DOUBLE_KEYWORD": {
                String kw1 = room.getKeyword();
                String kw2 = room.getKeyword2();
                if (kw1 != null && kw2 != null) return kw1 + " + " + kw2;
                return kw1;
            }
            case "CHAIN": return "首尾接龙" + (room.getKeyword() != null ? "(" + room.getKeyword() + ")" : "");
            case "COLOR": return room.getColorKeyword() != null ? "颜色-" + room.getColorKeyword() : "颜色飞花令";
            case "NUMBER": return room.getNumberKeyword() != null ? "数字-" + room.getNumberKeyword() : "数字飞花令";
            case "FORBIDDEN": return room.getForbiddenWord() != null ? "禁" + room.getForbiddenWord() : "反飞花令";
            case "CUSTOM": return room.getKeyword() != null ? room.getKeyword() : "自定义";
            default: return room.getKeyword() != null ? room.getKeyword() : "未知";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (wsCallback != null) {
            WebSocketClient.getInstance().removeCallback(wsCallback);
        }
        if (!battleStarted) {
            if (myUserId > 0) {
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "UNSUBSCRIBE_ROOM");
                msg.addProperty("roomId", roomId);
                msg.addProperty("userId", myUserId);
                WebSocketClient.getInstance().sendMessage(msg.toString());
            }
            WebSocketClient.getInstance().disconnect();
        }
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }
}
