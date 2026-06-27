package com.example.feihualinggame.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.feihualinggame.R;
import com.example.feihualinggame.adapter.MailAdapter;
import com.example.feihualinggame.bean.Mail;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;
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

/**
 * 消息中心 - 分类展示系统通知、好友申请、对战邀请
 */
public class MailboxActivity extends AppCompatActivity {
    private ImageView btnBack;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvMailList;
    private View tvMailEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private TextView btnClearRead;
    private TextView btnMarkAllRead;
    
    private TextView tabAll, tabSystem, tabFriendRequest, tabBattle;

    private List<Mail> mailList;
    private List<Mail> filteredMailList;
    private MailAdapter mailAdapter;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mailbox);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.poetry_card_background));
        }

        SystemUIUtil.hideNavigationBarIndicator(this);
        SystemUIUtil.setLightStatusBar(this);

        btnBack = findViewById(R.id.btn_back);
        swipeRefresh = findViewById(R.id.swipe_refresh_mail);
        rvMailList = findViewById(R.id.rv_mail_list);
        tvMailEmpty = findViewById(R.id.tv_mail_empty);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_empty_desc);
        btnClearRead = findViewById(R.id.btn_clear_read);
        btnMarkAllRead = findViewById(R.id.btn_mark_all_read);
        
        tabAll = findViewById(R.id.tab_all);
        tabSystem = findViewById(R.id.tab_system);
        tabFriendRequest = findViewById(R.id.tab_friend_request);
        tabBattle = findViewById(R.id.tab_battle);

        swipeRefresh.setColorSchemeResources(R.color.poetry_primary, R.color.poetry_accent, R.color.poetry_purple_dark);

        mailList = new ArrayList<>();
        filteredMailList = new ArrayList<>();
        mailAdapter = new MailAdapter(filteredMailList, this::onMailClick);
        rvMailList.setLayoutManager(new LinearLayoutManager(this));
        rvMailList.setAdapter(mailAdapter);

        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        btnClearRead.setOnClickListener(v -> clearReadMails());

        btnMarkAllRead.setOnClickListener(v -> markAllRead());

        swipeRefresh.setOnRefreshListener(() -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            loadMailList();
        });

        setupTabs();
        loadMailList();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadMailList();
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> switchTab(0, tabAll));
        tabSystem.setOnClickListener(v -> switchTab(1, tabSystem));
        tabFriendRequest.setOnClickListener(v -> switchTab(2, tabFriendRequest));
        tabBattle.setOnClickListener(v -> switchTab(3, tabBattle));
    }
    
    private void switchTab(int tab, TextView selectedView) {
        AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
        if (currentTab == tab) return;
        currentTab = tab;
        
        TextView[] tabs = {tabAll, tabSystem, tabFriendRequest, tabBattle};
        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.card_white);
            t.setTextColor(getResources().getColor(R.color.poetry_text_secondary));
            t.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        
        selectedView.setBackgroundResource(R.drawable.btn_primary_bg);
        selectedView.setTextColor(getResources().getColor(R.color.white));
        selectedView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        applyFilter();
    }
    
    private void applyFilter() {
        filteredMailList.clear();
        for (Mail mail : mailList) {
            if (currentTab == 0 || mail.getType() == currentTab) {
                filteredMailList.add(mail);
            }
        }
        mailAdapter.notifyDataSetChanged();
        updateMailEmptyState();
    }

    private void loadMailList() {
        String identityCode = SharedPrefsUtil.getUserId(this);
        if (identityCode == null || identityCode.isEmpty()) {
            swipeRefresh.setRefreshing(false);
            updateMailEmptyState();
            return;
        }

        String url = ApiConstant.MAIL_LIST + "?identityCode=" + identityCode;
        OkHttpUtil.getWithAuth(this, url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(MailboxActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    updateMailEmptyState();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                        if (jsonObject.get("code").getAsInt() == 200) {
                            JsonArray dataArray = jsonObject.getAsJsonArray("data");
                            runOnUiThread(() -> {
                                mailList.clear();
                                for (JsonElement element : dataArray) {
                                    JsonObject mailObj = element.getAsJsonObject();
                                    Mail mail = new Mail();
                                    mail.setId(mailObj.get("id").getAsLong());
                                    mail.setTitle(mailObj.get("title").getAsString());
                                    mail.setContent(mailObj.get("content").getAsString());
                                    mail.setSender(mailObj.get("sender").getAsString());
                                    mail.setSendTime(mailObj.get("sendTime").getAsString());
                                    mail.setType(mailObj.get("type").getAsInt());
                                    mail.setRead(mailObj.get("isRead").getAsBoolean());
                                    if (mailObj.has("relatedId") && !mailObj.get("relatedId").isJsonNull()) {
                                        mail.setRelatedId(mailObj.get("relatedId").getAsLong());
                                    }
                                    mailList.add(mail);
                                }
                                applyFilter();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void onMailClick(Mail mail, int position) {
        if (!mail.isRead()) {
            mail.setRead(true);
            mailAdapter.notifyItemChanged(position);
            markMailAsRead(mail.getId());
        }
        showMailDetailDialog(mail);
    }

    private void showMailDetailDialog(Mail mail) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_mail_detail, null);
        
        TextView tvTitle = dialogView.findViewById(R.id.tv_mail_detail_title);
        TextView tvSender = dialogView.findViewById(R.id.tv_mail_detail_sender);
        TextView tvTime = dialogView.findViewById(R.id.tv_mail_detail_time);
        TextView tvContent = dialogView.findViewById(R.id.tv_mail_detail_content);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btn_close_mail);
        
        LinearLayout layoutFriendActions = dialogView.findViewById(R.id.layout_friend_request_actions);
        android.widget.Button btnAccept = dialogView.findViewById(R.id.btn_accept_friend);
        android.widget.Button btnReject = dialogView.findViewById(R.id.btn_reject_friend);
        LinearLayout layoutMailClose = dialogView.findViewById(R.id.layout_mail_close);
        
        tvTitle.setText(mail.getTitle());
        tvSender.setText(mail.getSender());
        tvTime.setText(mail.getSendTime());
        tvContent.setText(mail.getContent());
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        
        if (mail.getType() == 2) {
            layoutFriendActions.setVisibility(View.VISIBLE);
            layoutMailClose.setVisibility(View.GONE);
            
            btnAccept.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                handleFriendRequest(mail, true);
                dialog.dismiss();
            });
            
            btnReject.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                handleFriendRequest(mail, false);
                dialog.dismiss();
            });
        } else {
            layoutFriendActions.setVisibility(View.GONE);
            layoutMailClose.setVisibility(View.VISIBLE);
            btnClose.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                dialog.dismiss();
            });
        }
    }
    
    private void handleFriendRequest(Mail mail, boolean accept) {
        String action = accept ? "接受" : "拒绝";
        
        Long friendId = mail.getRelatedId();
        if (friendId == null) {
            Toast.makeText(this, "无效的好友申请", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String url = (accept ? ApiConstant.FRIEND_ACCEPT : ApiConstant.FRIEND_REJECT) + "?friendId=" + friendId;
        
        OkHttpUtil.postWithAuth(this, url, "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MailboxActivity.this, action + "失败，请重试", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(responseBody).getAsJsonObject();
                        boolean success = jsonObject.has("code") && jsonObject.get("code").getAsInt() == 200;
                        
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(MailboxActivity.this, "已" + action + "好友申请", Toast.LENGTH_SHORT).show();
                                mailList.remove(mail);
                                mailAdapter.notifyDataSetChanged();
                                updateMailEmptyState();
                            } else {
                                String message = jsonObject.has("message") ? jsonObject.get("message").getAsString() : action + "失败";
                                Toast.makeText(MailboxActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(MailboxActivity.this, action + "失败", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MailboxActivity.this, action + "失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void clearReadMails() {
        List<Mail> readMails = new ArrayList<>();
        for (Mail mail : mailList) {
            if (mail.isRead()) readMails.add(mail);
        }
        if (readMails.isEmpty()) {
            Toast.makeText(this, "没有已读消息", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("清空已读")
            .setMessage("确定要清空 " + readMails.size() + " 条已读消息吗？")
            .setPositiveButton("清空", (dialog, which) -> clearReadMailsFromServer(readMails))
            .setNegativeButton("取消", null)
            .show();
    }

    private void markMailAsRead(Long mailId) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("mailId", mailId);
            OkHttpUtil.postWithAuth(this, ApiConstant.MAIL_READ, json.toString(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) { e.printStackTrace(); }
                @Override
                public void onResponse(Call call, Response response) throws IOException { response.close(); }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void markAllRead() {
        List<Mail> unreadMails = new ArrayList<>();
        for (Mail mail : mailList) {
            if (!mail.isRead()) unreadMails.add(mail);
        }
        if (unreadMails.isEmpty()) {
            Toast.makeText(this, "没有未读消息", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("全部已读")
            .setMessage("确定要标记 " + unreadMails.size() + " 条消息为已读吗？")
            .setPositiveButton("确定", (dialog, which) -> markAllReadFromServer())
            .setNegativeButton("取消", null)
            .show();
    }

    private void markAllReadFromServer() {
        OkHttpUtil.postWithAuth(this, ApiConstant.MAIL_MARK_ALL_READ, "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MailboxActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        for (Mail mail : mailList) {
                            mail.setRead(true);
                        }
                        applyFilter();
                        Toast.makeText(MailboxActivity.this, "已全部标记为已读", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MailboxActivity.this, "操作失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void clearReadMailsFromServer(List<Mail> readMails) {
        try {
            JsonArray mailIds = new JsonArray();
            for (Mail mail : readMails) {
                mailIds.add(mail.getId());
            }
            JsonObject json = new JsonObject();
            json.add("mailIds", mailIds);
            
            OkHttpUtil.postWithAuth(this, ApiConstant.MAIL_CLEAR_READ, json.toString(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(MailboxActivity.this, "清空失败", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            mailList.removeAll(readMails);
                            applyFilter();
                            updateMailEmptyState();
                            Toast.makeText(MailboxActivity.this, "已清空", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateMailEmptyState() {
        if (filteredMailList.isEmpty()) {
            tvMailEmpty.setVisibility(View.VISIBLE);
            rvMailList.setVisibility(View.GONE);
            
            String[] emptyTitles = {"暂无消息", "暂无系统通知", "暂无好友申请", "暂无对战邀请"};
            String[] emptyDescs = {"系统通知、好友申请等将显示在这里", "系统通知会显示在这里", "好友申请会显示在这里", "对战邀请会显示在这里"};
            int idx = Math.min(currentTab, 3);
            tvEmptyTitle.setText(emptyTitles[idx]);
            tvEmptyDesc.setText(emptyDescs[idx]);
        } else {
            tvMailEmpty.setVisibility(View.GONE);
            rvMailList.setVisibility(View.VISIBLE);
        }
    }
}
