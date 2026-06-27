package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.example.feihualinggame.utils.WebSocketClient;
import com.example.feihualinggame.bean.WebSocketMessageBean;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MultiRoomMenuActivity extends AppCompatActivity {
    private static final String TAG = "多人约战菜单";
    private Gson gson = new Gson();
    private WebSocketClient.MessageCallback wsCallback;

    private CardView cardCreateRoom;
    private CardView cardJoinRoom;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_room_menu);

        SystemUIUtil.setupImmersiveStatusBar(this, R.color.poetry_card_background, true);
        SystemUIUtil.hideNavigationBarIndicator(this);

        initViews();
        setupListeners();
        connectWebSocketForInvite();

        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    private void initViews() {
        cardCreateRoom = findViewById(R.id.card_create_room);
        cardJoinRoom = findViewById(R.id.card_join_room);
        btnBack = findViewById(R.id.btn_back);

        ButtonAnimationHelper.addPressScaleEffect(cardCreateRoom);
        ButtonAnimationHelper.addPressScaleEffect(cardJoinRoom);
        ButtonAnimationHelper.addPressScaleEffect(btnBack);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        cardCreateRoom.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            Intent intent = new Intent(MultiRoomMenuActivity.this, MultiRoomCreateActivity.class);
            startActivity(intent);
        });

        cardJoinRoom.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            showJoinRoomDialog();
        });
    }

    private void showJoinRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_join_room, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        params.width = (int)(metrics.widthPixels * 0.85f);
        dialog.getWindow().setAttributes(params);

        EditText etRoomCode = dialogView.findViewById(R.id.et_room_code);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            String roomCode = etRoomCode.getText().toString().trim().toUpperCase();
            if (roomCode.isEmpty()) {
                Toast.makeText(this, "请输入房间码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (roomCode.length() != 6) {
                Toast.makeText(this, "房间码必须为6位", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            joinRoom(roomCode);
        });

        dialog.show();
    }

    private void joinRoom(String roomCode) {
        Toast.makeText(this, "正在加入房间...", Toast.LENGTH_SHORT).show();

        long userId = Long.parseLong(SharedPrefsUtil.getUserId(this));
        JsonObject body = new JsonObject();
        body.addProperty("userId", userId);
        body.addProperty("roomCode", roomCode);

        OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_JOIN, body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MultiRoomMenuActivity.this, "网络异常: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            JsonObject data = json.getAsJsonObject("data");
                            String roomId = data.get("roomId").getAsString();
                            String code = data.get("roomCode").getAsString();
                            boolean isCreator = data.has("creatorId") && data.get("creatorId").getAsLong() == userId;
                            navigateToRoom(roomId, code, isCreator);
                        } else {
                            String message = json.has("message") ? json.get("message").getAsString() : "加入失败";
                            Toast.makeText(MultiRoomMenuActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MultiRoomMenuActivity.this, "解析响应失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void navigateToRoom(String roomId, String roomCode, boolean isCreator) {
        Intent intent = new Intent(MultiRoomMenuActivity.this, MultiRoomActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("isCreator", isCreator);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wsCallback != null) {
            WebSocketClient.getInstance().removeCallback(wsCallback);
        }
    }

    private void connectWebSocketForInvite() {
        long numericUserId = SharedPrefsUtil.getLong(this, "numeric_user_id", -1);
        if (numericUserId <= 0) {
            fetchUserIdAndConnect();
            return;
        }
        setupWebSocketCallback(numericUserId);
    }

    private void fetchUserIdAndConnect() {
        OkHttpUtil.getWithAuth(this, ApiConstant.USER_INFO, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    JsonObject data = json.has("data") && json.get("data").isJsonObject()
                            ? json.getAsJsonObject("data") : json;
                    if (data.has("id")) {
                        long userId = data.get("id").getAsLong();
                        SharedPrefsUtil.saveLong(MultiRoomMenuActivity.this, "numeric_user_id", userId);
                        runOnUiThread(() -> setupWebSocketCallback(userId));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupWebSocketCallback(long numericUserId) {
        wsCallback = new WebSocketClient.MessageCallback() {
            @Override
            public void onConnected() {
                WebSocketClient.getInstance().register(numericUserId);
            }
            @Override
            public void onDisconnected() {}
            @Override
            public void onMessage(WebSocketMessageBean message) {
                if ("INVITE_RECEIVED".equals(message.getType())) {
                    try {
                        JsonObject data = gson.fromJson(message.getPayload(), JsonObject.class);
                        String inviterName = data.has("inviterName") ? data.get("inviterName").getAsString() : "未知用户";
                        String inviteCode = data.has("roomCode") ? data.get("roomCode").getAsString() : "";
                        String inviteToken = data.has("inviteToken") ? data.get("inviteToken").getAsString() : "";
                        runOnUiThread(() -> {
                            new AlertDialog.Builder(MultiRoomMenuActivity.this)
                                .setTitle("收到房间邀请")
                                .setMessage(inviterName + " 邀请你加入多人对战房间\n房间码: " + inviteCode)
                                .setPositiveButton("接受", (d, w) -> acceptInviteAndJoin(inviteToken))
                                .setNegativeButton("拒绝", null)
                                .setCancelable(true)
                                .show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onError(String error) {}
        };
        WebSocketClient.getInstance().addCallback(wsCallback);
        if (!WebSocketClient.getInstance().isConnected()) {
            WebSocketClient.getInstance().connect(ApiConstant.WS_URL);
        } else {
            WebSocketClient.getInstance().register(numericUserId);
        }
    }

    private void acceptInviteAndJoin(String inviteToken) {
        OkHttpUtil.postWithAuth(this, "room/invite/" + inviteToken + "/accept", "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MultiRoomMenuActivity.this, "加入房间失败", Toast.LENGTH_SHORT).show());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.has("code") && json.get("code").getAsInt() == 200) {
                            JsonObject data = json.has("data") ? json.getAsJsonObject("data") : json;
                            String roomId = data.has("roomId") ? data.get("roomId").getAsString() : "";
                            String roomCode = data.has("roomCode") ? data.get("roomCode").getAsString() : "";
                            navigateToRoom(roomId, roomCode, false);
                        } else {
                            String msg = json.has("message") ? json.get("message").getAsString() : "加入房间失败";
                            Toast.makeText(MultiRoomMenuActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MultiRoomMenuActivity.this, "加入房间失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
