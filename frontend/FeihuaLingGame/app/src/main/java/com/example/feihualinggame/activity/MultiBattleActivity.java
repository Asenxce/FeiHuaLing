package com.example.feihualinggame.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.WebSocketMessageBean;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.FeedbackManager;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.NetworkUtil;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.PoetryCleanUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.example.feihualinggame.utils.WebSocketClient;
import com.example.feihualinggame.validator.engine.RuleEngine;
import com.example.feihualinggame.validator.engine.RuleContext;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MultiBattleActivity extends AppCompatActivity {
    private static final String TAG = "多人对战";
    private Gson gson = new Gson();
    private Handler handler = new Handler(Looper.getMainLooper());

    private String roomId;
    private String battleId;
    private long myUserId;
    private long firstTurnUserId;
    private String firstTurnNickname;
    private int timeLimit;
    private String keyword;
    private String gameType;
    private int helpLimit = 3;
    private int helpUsed = 0;
    private boolean isMyTurn = false;
    private boolean isEliminated = false;
    private boolean battleEnded = false;
    private boolean timeoutNotified = false;
    private boolean battleUIInitialized = false;
    private long lastSubmittedUserId = 0;
    private boolean isDisconnected = false;
    private long disconnectStartTime = 0;
    private int remainingSecondsOnDisconnect = 0;
    private boolean userInitiatedExit = false;
    private final AtomicBoolean stateSynced = new AtomicBoolean(false);
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private ConnectivityManager.NetworkCallback networkCallback;
    private Handler reconnectTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable reconnectTimeoutRunnable;
    private Handler healthCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable healthCheckRunnable;

    private TextView tvKeyword;
    private TextView tvRoundInfo;
    private TextView tvTimer;
    private EditText etAnswer;
    private Button btnSubmit;
    private Button btnHelp;
    private LinearLayout btnVoiceInput;
    private LinearLayout llPlayerStatus;
    private View btnBack;
    private TextView tvBroadcast;
    private ScrollView svBroadcast;
    private SeekBar sbVolume;

    private CountDownTimer countDownTimer;
    private WebSocketClient.MessageCallback wsCallback;
    private ActivityResultLauncher<Intent> voiceInputLauncher;
    private Map<Long, View> playerStatusViews = new HashMap<>();
    private Map<Long, Boolean> eliminatedPlayers = new HashMap<>();

    private FrameLayout overlayReconnecting;
    private ProgressBar pbReconnecting;
    private TextView tvReconnectHint;
    private TextView tvReconnectTimeout;
    private Button btnReconnectCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_battle);

        SystemUIUtil.hideNavigationBarIndicator(this);
        SystemUIUtil.setupImmersiveStatusBar(this, R.color.poetry_card_background, true);

        roomId = getIntent().getStringExtra("roomId");
        battleId = getIntent().getStringExtra("battleId");
        timeLimit = getIntent().getIntExtra("timeLimit", 60);
        keyword = getIntent().getStringExtra("keyword");
        myUserId = getIntent().getLongExtra("userId", 0);
        firstTurnUserId = getIntent().getLongExtra("firstTurnUserId", 0);
        firstTurnNickname = getIntent().getStringExtra("firstTurnNickname");
        gameType = getIntent().getStringExtra("gameType");
        helpLimit = getIntent().getIntExtra("helpLimit", 3);

        initViews();
        setupListeners();
        setupReconnectOverlay();
        loadRoomDetail();
        connectWebSocket();
        setupNetworkMonitoring();

        AudioController.getInstance().playBGM(AudioController.SCENE_BATTLE);
    }

    private void initViews() {
        tvKeyword = findViewById(R.id.tv_keyword);
        tvRoundInfo = findViewById(R.id.tv_round_info);
        tvTimer = findViewById(R.id.tv_timer);
        etAnswer = findViewById(R.id.et_answer);
        btnSubmit = findViewById(R.id.btn_submit);
        btnVoiceInput = findViewById(R.id.btn_voice_input);
        llPlayerStatus = findViewById(R.id.ll_player_status);
        btnBack = findViewById(R.id.btn_back);
        tvBroadcast = findViewById(R.id.tv_broadcast);
        svBroadcast = findViewById(R.id.sv_broadcast);
        sbVolume = findViewById(R.id.sb_volume);
        btnHelp = findViewById(R.id.btn_help);

        tvKeyword.setText("关键字: " + keyword);
        tvTimer.setText(String.valueOf(timeLimit));
        updateHelpButtonText();

        sbVolume.setProgress((int) (AudioController.getInstance().getBGMVolume() * 100));
        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    AudioController.getInstance().setBGMVolume(progress / 100f);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnHelp.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            handleHelp();
        });

        ButtonAnimationHelper.addCombinedEffect(btnSubmit);
        ButtonAnimationHelper.addCombinedEffect(btnBack);
        btnSubmit.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            submitAnswer();
        });
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            confirmSurrender();
        });
    }

    private void setupListeners() {
        voiceInputLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    java.util.ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && results.size() > 0) {
                        etAnswer.setText(results.get(0));
                    }
                }
            }
        );

        btnVoiceInput.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            startVoiceInput();
        });
        ButtonAnimationHelper.addCombinedEffect(btnVoiceInput);
    }

    private void setupReconnectOverlay() {
        View overlay = getLayoutInflater().inflate(R.layout.overlay_reconnecting, null);
        overlayReconnecting = overlay.findViewById(R.id.overlay_reconnecting);
        pbReconnecting = overlay.findViewById(R.id.pb_reconnecting);
        tvReconnectHint = overlay.findViewById(R.id.tv_reconnect_hint);
        tvReconnectTimeout = overlay.findViewById(R.id.tv_reconnect_timeout);
        btnReconnectCancel = overlay.findViewById(R.id.btn_reconnect_cancel);

        btnReconnectCancel.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            userInitiatedExit = true;
            confirmSurrender();
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ((ViewGroup) findViewById(android.R.id.content)).addView(overlay, lp);
    }

    private void setupNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "网络恢复可用");
                handler.post(() -> {
                    if (isDisconnected && !battleEnded && !isFinishing()) {
                        tvReconnectHint.setText("网络已恢复，正在重连...");
                        WebSocketClient.getInstance().forceReconnect();
                        syncBattleState();
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                Log.w(TAG, "网络连接丢失");
                handler.post(() -> {
                    if (!battleEnded && !isFinishing()) {
                        handleDisconnection();
                    }
                });
            }
        };

        cm.registerNetworkCallback(request, networkCallback);
    }

    private void connectWebSocket() {
        String sessionId = SharedPrefsUtil.getString(this, "sessionId");
        WebSocketClient.getInstance().setSessionId(sessionId);
        WebSocketClient.getInstance().setBattleMode(true);
        stateSynced.set(false);
        wsCallback = new WebSocketClient.MessageCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "MultiBattle WebSocket已连接");
                registerAndSubscribe();
                if (!battleUIInitialized) {
                    battleUIInitialized = true;
                    initBattleUI();
                }
                if (isDisconnected) {
                    handler.postDelayed(() -> syncBattleState(), 200);
                }
            }
            @Override
            public void onDisconnected() {
                Log.w(TAG, "MultiBattle WebSocket断开");
                if (!isFinishing() && !battleEnded && !userInitiatedExit) {
                    handler.post(() -> handleDisconnection());
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
            @Override
            public void onReconnectAttempt(int attempt, int maxAttempts) {
                handler.post(() -> {
                    if (isDisconnected) {
                        tvReconnectHint.setText("正在尝试重新连接... (" + attempt + "/" + maxAttempts + ")");
                    }
                });
            }
        };
        WebSocketClient.getInstance().addCallback(wsCallback);

        if (WebSocketClient.getInstance().isConnected()) {
            registerAndSubscribe();
            if (!battleUIInitialized) {
                battleUIInitialized = true;
                initBattleUI();
            }
        } else {
            battleUIInitialized = true;
            WebSocketClient.getInstance().connect(ApiConstant.WS_URL);
        }
    }

    private void handleDisconnection() {
        if (isDisconnected || battleEnded || userInitiatedExit) return;
        isDisconnected = true;
        disconnectStartTime = System.currentTimeMillis();

        if (isMyTurn && countDownTimer != null) {
            String timerText = tvTimer.getText().toString();
            try {
                remainingSecondsOnDisconnect = Integer.parseInt(timerText);
            } catch (NumberFormatException e) {
                remainingSecondsOnDisconnect = timeLimit;
            }
        }

        stopTimer();
        etAnswer.setEnabled(false);
        btnSubmit.setEnabled(false);
        btnVoiceInput.setEnabled(false);
        btnHelp.setEnabled(false);

        showReconnectingOverlay();
        startHealthCheck();
        broadcast("[系统] 网络连接中断，正在尝试重连...");
    }

    private void syncBattleState() {
        if (battleEnded || isFinishing() || !isDisconnected) return;
        if (!syncInProgress.compareAndSet(false, true)) return;

        OkHttpUtil.getWithAuth(this, ApiConstant.ROOM_DETAIL + roomId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "同步对战状态失败: " + e.getMessage());
                syncInProgress.set(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                syncInProgress.set(false);
                handler.post(() -> {
                    if (battleEnded || isFinishing()) return;
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            JsonObject data = json.getAsJsonObject("data");
                            if (data.has("battleStatus")) {
                                String battleStatus = data.get("battleStatus").getAsString();
                                if ("FINISHED".equals(battleStatus)) {
                                    Log.i(TAG, "对局已结束");
                                    hideReconnectingOverlay();
                                    isDisconnected = false;
                                    stopHealthCheck();
                                    if (data.has("battleResult")) {
                                        navigateToResult(data.getAsJsonObject("battleResult").toString());
                                    }
                                    return;
                                }
                            }
                            if (data.has("players")) {
                                JsonArray players = data.getAsJsonArray("players");
                                rebuildPlayerStatusFromSync(players);
                            }
                            if (data.has("currentTurnUserId")) {
                                long currentTurnUserId = data.get("currentTurnUserId").getAsLong();
                                int serverRemainingTime = data.has("serverRemainingSeconds")
                                        ? data.get("serverRemainingSeconds").getAsInt() : timeLimit;
                                boolean myTurn = currentTurnUserId == myUserId;
                                hideReconnectingOverlay();
                                isDisconnected = false;
                                cancelReconnectTimeout();
                                stopHealthCheck();
                                stateSynced.set(true);

                                if (myTurn && serverRemainingTime > 0) {
                                    broadcast("[系统] 重连成功，轮到你作答！");
                                    setMyTurnWithRemainingTime(serverRemainingTime);
                                } else if (!myTurn) {
                                    broadcast("[系统] 重连成功！");
                                    String nickname = data.has("currentTurnNickname")
                                            ? data.get("currentTurnNickname").getAsString() : "对方";
                                    tvRoundInfo.setText("等待 " + nickname + " 作答...");
                                } else {
                                    broadcast("[系统] 重连成功，但已超时");
                                    setMyTurn(false);
                                }
                            } else {
                                hideReconnectingOverlay();
                                isDisconnected = false;
                                stopHealthCheck();
                                broadcast("[系统] 重连成功！");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "同步对战状态解析失败", e);
                    }
                });
            }
        });
    }

    private void setMyTurnWithRemainingTime(int remainingSeconds) {
        isMyTurn = true;
        timeoutNotified = false;
        etAnswer.setEnabled(!isEliminated);
        btnSubmit.setEnabled(!isEliminated);
        btnVoiceInput.setEnabled(!isEliminated);
        btnHelp.setEnabled(!isEliminated && helpUsed < helpLimit);
        if (!isEliminated) {
            etAnswer.setText("");
            etAnswer.requestFocus();
            FeedbackManager.getInstance().announceTurn(this, true);
            startTimerWithRemaining(remainingSeconds);
        }
    }

    private void startTimerWithRemaining(int remainingSeconds) {
        stopTimer();
        timeoutNotified = false;
        tvTimer.setText(String.valueOf(remainingSeconds));
        countDownTimer = new CountDownTimer(remainingSeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(String.valueOf(millisUntilFinished / 1000));
            }
            @Override
            public void onFinish() {
                tvTimer.setText("0");
                setMyTurn(false);
                notifyTimeout();
            }
        };
        countDownTimer.start();
    }

    private void rebuildPlayerStatusFromSync(JsonArray players) {
        for (int i = 0; i < players.size(); i++) {
            JsonObject p = players.get(i).getAsJsonObject();
            long uid = p.get("userId").getAsLong();
            String status = p.get("status").getAsString();
            int faultCount = p.get("faultCount").getAsInt();

            if ("ELIMINATED".equals(status) || "QUIT".equals(status)) {
                markPlayerEliminated(uid);
            }
            updatePlayerFaultCount(uid, faultCount);

            View card = playerStatusViews.get(uid);
            if (card != null) {
                TextView tvName = card.findViewById(R.id.tv_player_name);
                String name = tvName.getText().toString();
                name = name.replace(" [✓]", "").replace(" [✗]", "").replace(" [轮]", "")
                        .replace(" [断线]", "").replace(" [重连中]", "");
                if ("DISCONNECTED".equals(status)) {
                    tvName.setText(name + " [断线]");
                    tvName.setTextColor(Color.parseColor("#F59E0B"));
                } else if (!"ELIMINATED".equals(status) && !"QUIT".equals(status)) {
                    tvName.setText(name);
                    tvName.setTextColor(Color.parseColor("#374151"));
                }
            }
        }
    }

    private void showReconnectingOverlay() {
        if (overlayReconnecting == null) return;
        overlayReconnecting.setVisibility(View.VISIBLE);
        tvReconnectHint.setText("正在尝试重新连接...");
        tvReconnectTimeout.setVisibility(View.VISIBLE);

        cancelReconnectTimeout();
        reconnectTimeoutRunnable = () -> {
            if (isDisconnected && !battleEnded) {
                int timeoutSeconds = isMyTurn ? remainingSecondsOnDisconnect : timeLimit;
                broadcast("[系统] 重连超时，已被判定为超时判负");
                isDisconnected = false;
                hideReconnectingOverlay();
                setMyTurn(false);
                notifyTimeout();
            }
        };
        int timeoutMs = isMyTurn ? remainingSecondsOnDisconnect * 1000 : timeLimit * 1000;
        tvReconnectTimeout.setText("剩余重连时间: " + (timeoutMs / 1000) + "s");

        CountDownTimer timeoutCountdown = new CountDownTimer(timeoutMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs = millisUntilFinished / 1000;
                tvReconnectTimeout.setText("剩余重连时间: " + secs + "s");
                if (secs <= 10) {
                    tvReconnectTimeout.setTextColor(Color.parseColor("#B84B3A"));
                }
            }
            @Override
            public void onFinish() {
                tvReconnectTimeout.setText("剩余重连时间: 0s");
            }
        };
        timeoutCountdown.start();
        reconnectTimeoutHandler.postDelayed(reconnectTimeoutRunnable, timeoutMs);
    }

    private void hideReconnectingOverlay() {
        if (overlayReconnecting != null) {
            overlayReconnecting.setVisibility(View.GONE);
        }
        cancelReconnectTimeout();
    }

    private void cancelReconnectTimeout() {
        if (reconnectTimeoutRunnable != null) {
            reconnectTimeoutHandler.removeCallbacks(reconnectTimeoutRunnable);
            reconnectTimeoutRunnable = null;
        }
    }

    private void startHealthCheck() {
        stopHealthCheck();
        healthCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (battleEnded || !isDisconnected || isFinishing()) return;
                Log.d(TAG, "健康检查: WS连接=" + WebSocketClient.getInstance().isConnected());
                if (!WebSocketClient.getInstance().isConnected()) {
                    WebSocketClient.getInstance().forceReconnect();
                }
                syncBattleState();
                healthCheckHandler.postDelayed(this, 5000);
            }
        };
        healthCheckHandler.postDelayed(healthCheckRunnable, 5000);
    }

    private void stopHealthCheck() {
        if (healthCheckRunnable != null) {
            healthCheckHandler.removeCallbacks(healthCheckRunnable);
            healthCheckRunnable = null;
        }
    }

    private void registerAndSubscribe() {
        WebSocketClient.getInstance().register(myUserId);
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "SUBSCRIBE_ROOM");
        msg.addProperty("roomId", roomId);
        msg.addProperty("userId", myUserId);
        WebSocketClient.getInstance().sendMessage(msg.toString());
        Log.d(TAG, "MultiBattle WebSocket已注册并订阅房间");
    }

    private void initBattleUI() {
        broadcast("--- 对局开始 ---");

        if (myUserId == firstTurnUserId && firstTurnUserId > 0) {
            broadcast("[你] 首轮作答");
            handler.postDelayed(() -> {
                if (!battleEnded && !isEliminated) {
                    setMyTurn(true);
                }
            }, 500);
        } else if (firstTurnNickname != null && !firstTurnNickname.isEmpty()) {
            broadcast("[" + firstTurnNickname + "] 首轮作答");
            tvRoundInfo.setText("等待 " + firstTurnNickname + " 作答...");
        }
    }

    private void handleWebSocketMessage(WebSocketMessageBean msg) {
        switch (msg.getType()) {
            case "TURN_NOTIFY":
                handler.post(() -> {
                    if (!isMyTurn && !isEliminated && !battleEnded) {
                        setMyTurn(true);
                    }
                });
                break;
            case "ROUND_RESULT":
                try {
                    JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                    long resultUserId = jo.get("userId").getAsLong();
                    boolean isCorrect = jo.get("isCorrect").getAsBoolean();
                    int faultRemaining = jo.has("faultRemaining") ? jo.get("faultRemaining").getAsInt() : 0;
                    String poemContent = jo.has("poemContent") && !jo.get("poemContent").isJsonNull() ? jo.get("poemContent").getAsString() : "";
                    String nickname = jo.get("nickname").getAsString();
                    int roundNum = jo.get("roundNum").getAsInt();
                    long nextPlayerId = jo.has("nextPlayerId") ? jo.get("nextPlayerId").getAsLong() : 0;
                    String nextPlayerNickname = jo.has("nextPlayerNickname") ? jo.get("nextPlayerNickname").getAsString() : "";
                    handler.post(() -> {
                        updateSinglePlayerStatus(resultUserId, isCorrect, false);
                        updatePlayerFaultCount(resultUserId, faultRemaining);
                        updatePlayerAnswer(resultUserId, poemContent, isCorrect);
                        updateRoundInfo(jo);

                        if (resultUserId == myUserId) {
                            lastSubmittedUserId = 0;
                        }

                        if (isCorrect) {
                            broadcast("[正确] " + nickname + ": " + (poemContent.isEmpty() ? "--" : "「" + poemContent + "」") + "  |  容错剩余 " + faultRemaining);
                            if (resultUserId == myUserId) {
                                FeedbackManager.getInstance().speakCorrect(MultiBattleActivity.this);
                            }
                        } else {
                            broadcast("[错误] " + nickname + ": " + (poemContent.isEmpty() ? "(空)" : "「" + poemContent + "」") + "  |  容错剩余 " + faultRemaining);
                            if (resultUserId == myUserId) {
                                FeedbackManager.getInstance().speakWrong(MultiBattleActivity.this, "回答错误");
                            }
                        }

                        if (nextPlayerId > 0) {
                            updateSinglePlayerStatus(nextPlayerId, false, true);
                            if (nextPlayerId == myUserId) {
                                setMyTurn(true);
                                broadcast("[系统] 轮到你作答了！");
                            } else {
                                setMyTurn(false);
                                String waitingText = "等待 " + (nextPlayerNickname.isEmpty() ? "对方" : nextPlayerNickname) + " 作答...";
                                tvRoundInfo.setText(waitingText);
                                broadcast("[系统] " + waitingText);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "ROUND_RESULT解析失败", e);
                }
                break;
            case "PLAYER_ELIMINATED":
                try {
                    JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                    long eliminatedId = jo.get("userId").getAsLong();
                    String reason = jo.get("reason").getAsString();
                    String eliminatedNickname = jo.get("nickname").getAsString();
                    long nextPlayerId = jo.has("nextPlayerId") ? jo.get("nextPlayerId").getAsLong() : 0;
                    String nextPlayerNickname = jo.has("nextPlayerNickname") ? jo.get("nextPlayerNickname").getAsString() : "";
                    handler.post(() -> {
                        markPlayerEliminated(eliminatedId);
                        broadcast("[淘汰] " + eliminatedNickname + ": " + reason);
                        if (eliminatedId == myUserId) {
                            isEliminated = true;
                            setMyTurn(false);
                            etAnswer.setEnabled(false);
                            btnSubmit.setEnabled(false);
                            btnVoiceInput.setEnabled(false);
                            tvRoundInfo.setText("你已被淘汰，正在观战中...");
                            FeedbackManager.getInstance().announceEliminated(MultiBattleActivity.this);
                            showEliminatedDialog();
                        }

                        if (nextPlayerId > 0) {
                            updateSinglePlayerStatus(nextPlayerId, false, true);
                            if (nextPlayerId == myUserId) {
                                setMyTurn(true);
                                broadcast("[系统] 轮到你作答了！");
                            } else {
                                setMyTurn(false);
                                String waitingText = "等待 " + (nextPlayerNickname.isEmpty() ? "对方" : nextPlayerNickname) + " 作答...";
                                tvRoundInfo.setText(waitingText);
                                broadcast("[系统] " + waitingText);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "PLAYER_ELIMINATED解析失败", e);
                }
                break;
            case "PLAYER_DISCONNECTED":
                try {
                    JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                    long disconnectedId = jo.get("userId").getAsLong();
                    String disconnectedNickname = jo.get("nickname").getAsString();
                    handler.post(() -> {
                        View card = playerStatusViews.get(disconnectedId);
                        if (card != null && !Boolean.TRUE.equals(eliminatedPlayers.get(disconnectedId))) {
                            TextView tvName = card.findViewById(R.id.tv_player_name);
                            String name = tvName.getText().toString();
                            name = name.replace(" [断线]", "").replace(" [重连中]", "").replace(" [✓]", "").replace(" [✗]", "").replace(" [轮]", "");
                            tvName.setText(name + " [断线]");
                            tvName.setTextColor(Color.parseColor("#F59E0B"));
                            View statusDot = card.findViewById(R.id.v_status_dot);
                            statusDot.setBackgroundColor(Color.parseColor("#F59E0B"));
                        }
                        broadcast("[系统] " + disconnectedNickname + " 网络中断");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "PLAYER_DISCONNECTED解析失败", e);
                }
                break;
            case "BATTLE_END":
                Log.i(TAG, "收到BATTLE_END: " + msg.getPayload());
                broadcast("--- 对局结束 ---");
                handler.post(() -> {
                    hideReconnectingOverlay();
                    cancelReconnectTimeout();
                    navigateToResult(msg.getPayload());
                });
                break;
            case "ROOM_DISSOLVED":
                broadcast("[系统] 房间已被解散");
                handler.post(() -> {
                    hideReconnectingOverlay();
                    cancelReconnectTimeout();
                    finish();
                });
                break;
            case "PLAYER_TIMEOUT":
                try {
                    JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                    long timeoutId = jo.get("userId").getAsLong();
                    int faultRemaining = jo.get("faultRemaining").getAsInt();
                    String nickname = jo.get("nickname").getAsString();
                    boolean eliminated = jo.has("isEliminated") && jo.get("isEliminated").getAsBoolean();
                    int roundNum = jo.has("roundNum") ? jo.get("roundNum").getAsInt() : 0;
                    long nextPlayerId = jo.has("nextPlayerId") ? jo.get("nextPlayerId").getAsLong() : 0;
                    String nextPlayerNickname = jo.has("nextPlayerNickname") ? jo.get("nextPlayerNickname").getAsString() : "";
                    handler.post(() -> {
                        updatePlayerFaultCount(timeoutId, faultRemaining);
                        updatePlayerAnswer(timeoutId, "超时", false);
                        String roundInfo = roundNum > 0 ? "第" + roundNum + "轮 " : "";
                        broadcast("[" + roundInfo + "超时] " + nickname + ": 容错剩余 " + faultRemaining
                                + (eliminated ? " (已淘汰)" : ""));
                        if (timeoutId == myUserId) {
                            setMyTurn(false);
                        }
                        if (roundNum > 0) {
                            tvRoundInfo.setText("第" + roundNum + "轮 - " + nickname + " [超时]");
                        }

                        if (nextPlayerId > 0) {
                            if (nextPlayerId == myUserId) {
                                setMyTurn(true);
                                broadcast("[系统] 轮到你作答了！");
                            } else {
                                setMyTurn(false);
                                String waitingText = "等待 " + (nextPlayerNickname.isEmpty() ? "对方" : nextPlayerNickname) + " 作答...";
                                tvRoundInfo.setText(waitingText);
                                broadcast("[系统] " + waitingText);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "PLAYER_TIMEOUT解析失败", e);
                }
                break;
            case "PLAYER_RECONNECTED":
                try {
                    JsonObject jo = gson.fromJson(msg.getPayload(), JsonObject.class);
                    long reconnectedId = jo.get("userId").getAsLong();
                    String reconnectedNickname = jo.get("nickname").getAsString();
                    handler.post(() -> {
                        View card = playerStatusViews.get(reconnectedId);
                        if (card != null) {
                            TextView tvName = card.findViewById(R.id.tv_player_name);
                            String name = tvName.getText().toString();
                            name = name.replace(" [断线]", "").replace(" [重连中]", "");
                            if (Boolean.TRUE.equals(eliminatedPlayers.get(reconnectedId))) {
                                tvName.setTextColor(Color.parseColor("#9CA3AF"));
                            } else {
                                tvName.setTextColor(Color.parseColor("#374151"));
                            }
                            tvName.setText(name);
                            View statusDot = card.findViewById(R.id.v_status_dot);
                            if (!Boolean.TRUE.equals(eliminatedPlayers.get(reconnectedId))) {
                                statusDot.setBackgroundColor(Color.parseColor("#10B981"));
                            }
                        }
                        broadcast("[系统] " + reconnectedNickname + " 已重新连接");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "PLAYER_RECONNECTED解析失败", e);
                }
                break;
        }
    }

    private void setMyTurn(boolean turn) {
        isMyTurn = turn;
        timeoutNotified = false;
        boolean canInput = turn && !isEliminated && !isDisconnected;
        etAnswer.setEnabled(canInput);
        btnSubmit.setEnabled(canInput);
        btnVoiceInput.setEnabled(canInput);
        btnHelp.setEnabled(canInput && helpUsed < helpLimit);
        if (turn && !isEliminated && !isDisconnected) {
            etAnswer.setText("");
            etAnswer.requestFocus();
            FeedbackManager.getInstance().announceTurn(this, true);
            startTimer();
        } else {
            stopTimer();
            if (!isEliminated && !isDisconnected) {
                tvTimer.setText(String.valueOf(timeLimit));
            }
        }
    }

    private void updateHelpButtonText() {
        int remaining = helpLimit - helpUsed;
        btnHelp.setText("求助(" + remaining + ")");
    }

    private void handleHelp() {
        if (helpUsed >= helpLimit) {
            Toast.makeText(this, "求助次数已用完", Toast.LENGTH_SHORT).show();
            return;
        }

        String encodedKeyword = java.net.URLEncoder.encode(keyword != null ? keyword : "", java.nio.charset.StandardCharsets.UTF_8);
        String url = "room/help?gameMode=" + (gameType != null ? gameType : "ENTERTAINMENT")
            + "&keyword=" + encodedKeyword;

        OkHttpUtil.getWithAuth(this, url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MultiBattleActivity.this, "求助请求失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    try {
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        if (json.has("content") && !json.get("content").getAsString().isEmpty()) {
                            helpUsed++;
                            updateHelpButtonText();

                            if ("ENTERTAINMENT".equals(gameType)) {
                                etAnswer.setText(json.get("content").getAsString());
                                Toast.makeText(MultiBattleActivity.this, "已自动填入诗句", Toast.LENGTH_SHORT).show();
                            } else {
                                String hint = json.get("content").getAsString();
                                String author = json.has("author") ? json.get("author").getAsString() : "";
                                String title = json.has("title") ? json.get("title").getAsString() : "";
                                String display = hint + (title.isEmpty() ? "" : " 《" + title + "》") + (author.isEmpty() ? "" : " " + author);
                                Toast.makeText(MultiBattleActivity.this, "提示: " + display, Toast.LENGTH_LONG).show();
                            }

                            if (helpUsed >= helpLimit) {
                                btnHelp.setEnabled(false);
                            }
                        } else {
                            Toast.makeText(MultiBattleActivity.this, "未找到符合规则的诗句", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(MultiBattleActivity.this, "求助解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void startTimer() {
        stopTimer();
        timeoutNotified = false;
        countDownTimer = new CountDownTimer(timeLimit * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(String.valueOf(millisUntilFinished / 1000));
            }
            @Override
            public void onFinish() {
                tvTimer.setText("0");
                setMyTurn(false);
                notifyTimeout();
            }
        };
        countDownTimer.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void notifyTimeout() {
        if (timeoutNotified || battleEnded || isEliminated) return;
        timeoutNotified = true;
        JsonObject body = new JsonObject();
        body.addProperty("battleId", battleId);
        OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_TIMEOUT + roomId + "/timeout", body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "超时通知失败: " + e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {}
        });
    }

    private void submitAnswer() {
        String rawAnswer = etAnswer.getText().toString().trim();
        if (rawAnswer.isEmpty()) {
            Toast.makeText(this, "请输入诗句", Toast.LENGTH_SHORT).show();
            return;
        }

        String answer = PoetryCleanUtil.cleanPoetry(rawAnswer);

        if (answer.length() < 2) {
            Toast.makeText(this, "请输入完整的诗句（至少2个字）", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPureChinese(answer)) {
            Toast.makeText(this, "答案只能包含中文汉字", Toast.LENGTH_SHORT).show();
            AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
            return;
        }

        if (keyword != null && !keyword.isEmpty() && !PoetryCleanUtil.containsKeyword(answer, keyword)) {
            Toast.makeText(this, "答案必须包含关键字「" + keyword + "」", Toast.LENGTH_SHORT).show();
            AudioController.getInstance().playSound(AudioController.SOUND_WRONG);
            return;
        }

        stopTimer();
        setMyTurn(false);
        lastSubmittedUserId = myUserId;

        JsonObject body = new JsonObject();
        body.addProperty("battleId", battleId);
        body.addProperty("roomId", roomId);
        body.addProperty("userId", myUserId);
        body.addProperty("poemContent", answer);

        OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_SUBMIT + roomId + "/submit", body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> {
                    Toast.makeText(MultiBattleActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
                    setMyTurn(true);
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                handler.post(() -> {
                    try {
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            JsonObject data = json.getAsJsonObject("data");
                            boolean correct = data.get("isCorrect").getAsBoolean();
                            int faultRemaining = data.get("faultRemaining").getAsInt();
                            boolean isEliminatedFlag = data.has("isEliminated") && data.get("isEliminated").getAsBoolean();
                            updatePlayerFaultCount(myUserId, faultRemaining);
                            if (isEliminatedFlag) {
                                isEliminated = true;
                                markPlayerEliminated(myUserId);
                            }
                            if (data.has("battleEnd")) {
                                navigateToResult(data.getAsJsonObject("battleEnd").toString());
                            }
                        } else {
                            lastSubmittedUserId = 0;
                            String msg = json.has("message") ? json.get("message").getAsString() : "提交失败";
                            Toast.makeText(MultiBattleActivity.this, msg, Toast.LENGTH_SHORT).show();
                            setMyTurn(true);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "提交解析失败", e);
                    }
                });
            }
        });
    }

    private boolean isPureChinese(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '\u4e00' || c > '\u9fff') {
                return false;
            }
        }
        return true;
    }

    private void updateRoundInfo(JsonObject data) {
        try {
            String nickname = data.get("nickname").getAsString();
            boolean correct = data.get("isCorrect").getAsBoolean();
            int roundNum = data.get("roundNum").getAsInt();
            String poem = data.has("poemContent") && !data.get("poemContent").isJsonNull()
                    ? data.get("poemContent").getAsString() : "";
            String text = "第" + roundNum + "轮 - " + nickname + " " + (correct ? "[正确]" : "[错误]");
            if (!poem.isEmpty()) {
                text += "\n「" + poem + "」";
            }
            tvRoundInfo.setText(text);
        } catch (Exception ignored) {}
    }

    private void loadRoomDetail() {
        OkHttpUtil.getWithAuth(this, ApiConstant.ROOM_DETAIL + roomId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "加载房间详情失败: " + e.getMessage());
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                handler.post(() -> {
                    try {
                        JsonObject json = gson.fromJson(body, JsonObject.class);
                        if (json.get("code").getAsInt() == 200) {
                            JsonObject data = json.getAsJsonObject("data");
                            if (data.has("players")) {
                                JsonArray players = data.getAsJsonArray("players");
                                buildPlayerStatusPanel(players);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析房间信息失败", e);
                    }
                });
            }
        });
    }

    private void buildPlayerStatusPanel(JsonArray players) {
        llPlayerStatus.removeAllViews();
        playerStatusViews.clear();
        eliminatedPlayers.clear();

        for (int i = 0; i < players.size(); i++) {
            JsonObject p = players.get(i).getAsJsonObject();
            long uid = p.get("userId").getAsLong();
            String nickname = p.get("nickname").getAsString();
            String status = p.get("status").getAsString();
            int faultCount = p.get("faultCount").getAsInt();

            View card = getLayoutInflater().inflate(R.layout.item_multi_battle_player, llPlayerStatus, false);
            TextView tvName = card.findViewById(R.id.tv_player_name);
            TextView tvFault = card.findViewById(R.id.tv_player_fault);
            View statusDot = card.findViewById(R.id.v_status_dot);

            tvName.setText(nickname);
            tvFault.setText("容错:" + faultCount);

            if ("ELIMINATED".equals(status) || "QUIT".equals(status)) {
                eliminatedPlayers.put(uid, true);
                statusDot.setBackgroundColor(Color.parseColor("#EF4444"));
                tvName.setTextColor(Color.parseColor("#9CA3AF"));
            } else if ("DISCONNECTED".equals(status)) {
                statusDot.setBackgroundColor(Color.parseColor("#F59E0B"));
                tvName.setText(nickname + " [断线]");
                tvName.setTextColor(Color.parseColor("#F59E0B"));
            } else {
                statusDot.setBackgroundColor(Color.parseColor("#10B981"));
            }

            llPlayerStatus.addView(card);
            playerStatusViews.put(uid, card);
        }
    }

    private void updateSinglePlayerStatus(long uid, boolean correct, boolean isCurrent) {
        View card = playerStatusViews.get(uid);
        if (card == null) return;
        if (Boolean.TRUE.equals(eliminatedPlayers.get(uid))) return;

        TextView tvName = card.findViewById(R.id.tv_player_name);
        String name = tvName.getText().toString();
        name = name.replace(" [✓]", "").replace(" [✗]", "").replace(" [轮]", "")
                .replace(" [断线]", "").replace(" [重连中]", "");

        if (isCurrent) {
            tvName.setText(name + " [轮]");
            tvName.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            tvName.setText(name);
            tvName.setTextColor(Color.parseColor("#374151"));
        }
    }

    private void markPlayerEliminated(long uid) {
        eliminatedPlayers.put(uid, true);
        View card = playerStatusViews.get(uid);
        if (card == null) return;

        View statusDot = card.findViewById(R.id.v_status_dot);
        TextView tvName = card.findViewById(R.id.tv_player_name);

        statusDot.setBackgroundColor(Color.parseColor("#EF4444"));
        tvName.setTextColor(Color.parseColor("#9CA3AF"));
    }

    private void updatePlayerFaultCount(long uid, int faultRemaining) {
        View card = playerStatusViews.get(uid);
        if (card == null) return;

        TextView tvFault = card.findViewById(R.id.tv_player_fault);
        tvFault.setText("容错:" + faultRemaining);
        if (faultRemaining <= 1) {
            tvFault.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void updatePlayerAnswer(long uid, String poemContent, boolean isCorrect) {
        View card = playerStatusViews.get(uid);
        if (card == null) return;

        TextView tvAnswer = card.findViewById(R.id.tv_player_answer);
        if (poemContent != null && !poemContent.isEmpty()) {
            String prefix = isCorrect ? "[✓] " : "[✗] ";
            tvAnswer.setText(prefix + poemContent);
            tvAnswer.setTextColor(isCorrect ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
            tvAnswer.setVisibility(View.VISIBLE);
        } else {
            tvAnswer.setVisibility(View.GONE);
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出诗句");

        try {
            voiceInputLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "语音输入不可用", Toast.LENGTH_SHORT).show();
        }
    }

    private void broadcast(String message) {
        String text = tvBroadcast.getText().toString();
        String timestamp = "";
        if (!text.isEmpty()) {
            text += "\n";
        }
        tvBroadcast.setText(text + timestamp + message);
        svBroadcast.post(() -> svBroadcast.fullScroll(View.FOCUS_DOWN));
    }

    private void showEliminatedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("你已被淘汰")
                .setMessage("很遗憾，你的容错次数已耗尽。\n\n你可以继续观战，等待本局结束后查看最终排名。")
                .setPositiveButton("继续观战", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNegativeButton("认输出局", (dialog, which) -> {
                    dialog.dismiss();
                    doSurrender();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBackPressed() {
        userInitiatedExit = true;
        confirmSurrender();
    }

    private void confirmSurrender() {
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("退出对局将被判定为认输出局，系统立即结算你的成绩，其他玩家继续对局。\n\n确定要退出吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    dialog.dismiss();
                    doSurrender();
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void doSurrender() {
        if (battleEnded) {
            finish();
            return;
        }
        battleEnded = true;
        userInitiatedExit = true;
        stopTimer();
        cancelReconnectTimeout();
        stopHealthCheck();
        hideReconnectingOverlay();
        syncInProgress.set(false);

        JsonObject body = new JsonObject();
        body.addProperty("battleId", battleId);
        OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_SURRENDER + roomId + "/surrender", body.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> {
                    WebSocketClient.getInstance().disconnect();
                    finish();
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                handler.post(() -> {
                    WebSocketClient.getInstance().disconnect();
                    try {
                        JsonObject json = gson.fromJson(respBody, JsonObject.class);
                        if (json.get("code").getAsInt() == 200 && json.has("data")) {
                            JsonObject data = json.getAsJsonObject("data");
                            if (data.has("battleEnd")) {
                                Intent intent = new Intent(MultiBattleActivity.this, ResultActivity.class);
                                intent.putExtra("multiBattleResult", data.getAsJsonObject("battleEnd").toString());
                                intent.putExtra("isMulti", true);
                                intent.putExtra("myUserId", myUserId);
                                startActivity(intent);
                            }
                        }
                    } catch (Exception ignored) {}
                    finish();
                });
            }
        });
    }

    private void navigateToResult(String resultJson) {
        if (battleEnded) return;
        battleEnded = true;
        stopTimer();
        cancelReconnectTimeout();
        stopHealthCheck();
        hideReconnectingOverlay();
        WebSocketClient.getInstance().disconnect();
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("multiBattleResult", resultJson);
        intent.putExtra("isMulti", true);
        intent.putExtra("myUserId", myUserId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        cancelReconnectTimeout();
        stopHealthCheck();
        syncInProgress.set(false);
        if (wsCallback != null) {
            WebSocketClient.getInstance().removeCallback(wsCallback);
        }
        WebSocketClient.getInstance().setBattleMode(false);
        if (networkCallback != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                if (cm != null) cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "注销网络监听异常: " + e.getMessage());
            }
            networkCallback = null;
        }
        if (userInitiatedExit && !battleEnded) {
            JsonObject body = new JsonObject();
            body.addProperty("battleId", battleId);
            OkHttpUtil.postWithAuth(this, ApiConstant.ROOM_SURRENDER + roomId + "/surrender", body.toString(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                @Override
                public void onResponse(Call call, Response response) throws IOException {}
            });
            WebSocketClient.getInstance().disconnect();
        } else if (!battleEnded && isDisconnected) {
            WebSocketClient.getInstance().cancelReconnect();
            WebSocketClient.getInstance().disconnectWithoutIntentionalFlag();
        } else if (!battleEnded) {
            WebSocketClient.getInstance().disconnect();
        }
        AudioController.getInstance().playBGM(AudioController.SCENE_MAIN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AudioController.getInstance().pauseBGM();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!battleEnded) {
            AudioController.getInstance().resumeBGM();
        }
    }
}
