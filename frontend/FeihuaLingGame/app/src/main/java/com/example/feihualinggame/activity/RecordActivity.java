package com.example.feihualinggame.activity;

import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.bean.UserRecordBean;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.GsonUtil;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.example.feihualinggame.utils.SharedPrefsUtil;
import com.example.feihualinggame.utils.SystemUIUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecordActivity extends BaseActivity {
    private static final String TAG = "RecordActivity";

    private TextView tvTotalBattles;
    private TextView tvTotalScore;
    private TextView tvWinRate;
    private RecyclerView rvRecords;
    private TextView tvEmpty;
    private View btnSync;

    private LinearLayout selectionBar;
    private TextView tvSelectedCount;
    private TextView tvSelectAll;
    private TextView tvDeleteSelected;
    private boolean isSelectionMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private LinearLayout scrollViewContent;

    private GroupedRecordAdapter adapter;
    private List<UserRecordBean> allRecords = new ArrayList<>();
    private List<Object> displayItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        SystemUIUtil.setLightStatusBar(this);

        initViews();
        applyNavigationBarPadding();
        loadRecords();
    }

    private void initViews() {
        tvTotalBattles = findViewById(R.id.tvTotalBattles);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        tvWinRate = findViewById(R.id.tvWinRate);
        rvRecords = findViewById(R.id.rvRecords);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnSync = findViewById(R.id.btn_sync);

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        btnSync.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            loadRecords();
        });

        ButtonAnimationHelper.addCombinedEffect(btnSync);

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupedRecordAdapter(displayItems);
        rvRecords.setAdapter(adapter);

        adapter.setOnItemActionListener(new GroupedRecordAdapter.OnItemActionListener() {
            @Override
            public void onLongPress(int position) {
                enterSelectionMode(position);
            }

            @Override
            public void onToggleSelection(int position) {
                onItemSelectionChanged(position);
            }
        });

        selectionBar = findViewById(R.id.selection_bar);
        scrollViewContent = findViewById(R.id.scroll_content);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        tvSelectAll = findViewById(R.id.tv_select_all);
        tvDeleteSelected = findViewById(R.id.tv_delete_selected);

        tvSelectAll.setOnClickListener(v -> toggleSelectAll());
        tvDeleteSelected.setOnClickListener(v -> deleteSelectedRecords());
    }

    /**
     * 手动为滚动内容添加导航栏高度的底部内边距，
     * 确保最后的记录不会被系统导航栏遮挡。
     */
    private void applyNavigationBarPadding() {
        int navBarHeight = 0;
        Rect navBarRect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(navBarRect);
        navBarHeight = getWindow().getDecorView().getHeight() - navBarRect.bottom;
        if (navBarHeight > 0) {
            scrollViewContent.setPadding(
                    scrollViewContent.getPaddingLeft(),
                    scrollViewContent.getPaddingTop(),
                    scrollViewContent.getPaddingRight(),
                    navBarHeight);
        }
    }

    private void loadRecords() {
        String userId = SharedPrefsUtil.getUserId(this);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSync.setEnabled(false);

        OkHttpUtil.getWithAuth(this, ApiConstant.RECORD_PERSONAL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "请求失败: " + e.getMessage());
                runOnUiThread(() -> {
                    btnSync.setEnabled(true);
                    showEmpty("网络请求失败: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                Log.d(TAG, "API响应: " + body);
                runOnUiThread(() -> {
                    btnSync.setEnabled(true);

                    try {
                        JsonObject json = GsonUtil.getGson().fromJson(body, JsonObject.class);
                        if (json == null) {
                            showEmpty("响应为空");
                            return;
                        }

                        boolean success = json.has("success") && json.get("success").getAsBoolean();
                        if (!success) {
                            String msg = json.has("message") ? json.get("message").getAsString() : "获取失败";
                            showEmpty(msg);
                            return;
                        }

                        if (json.has("statistics") && !json.get("statistics").isJsonNull()) {
                            parseStatistics(json.getAsJsonObject("statistics"));
                        }

                        if (json.has("records") && json.get("records").isJsonArray()) {
                            int apiCount = json.getAsJsonArray("records").size();
                            parseRecords(json.getAsJsonArray("records"));
                            Log.d(TAG, "API返回 " + apiCount + " 条，解析到 " + allRecords.size() + " 条");
                        } else {
                            allRecords.clear();
                        }

                        updateDisplay();
                        Log.d(TAG, "displayItems=" + displayItems.size() + ", allRecords=" + allRecords.size());

                    } catch (Exception e) {
                        Log.e(TAG, "解析数据失败", e);
                        showEmpty("数据解析失败");
                    }
                });
            }
        });
    }

    private void parseStatistics(JsonObject stats) {
        int totalBattles = getJsonInt(stats, "totalBattles");
        tvTotalBattles.setText(String.valueOf(totalBattles));

        int totalScore = getJsonInt(stats, "totalScore");
        tvTotalScore.setText(String.valueOf(totalScore));

        if (stats.has("winRate")) {
            double winRate = getJsonDouble(stats, "winRate");
            tvWinRate.setText(String.format("%.0f%%", winRate));
        }
    }

    private void parseRecords(JsonArray recordsJson) {
        allRecords.clear();
        for (int i = 0; i < recordsJson.size(); i++) {
            try {
                JsonObject r = recordsJson.get(i).getAsJsonObject();
                UserRecordBean record = new UserRecordBean();
                record.setId(getJsonLong(r, "id"));
                record.setBattleId(getJsonString(r, "battleId"));
                record.setBattleType(getJsonString(r, "battleType"));
                record.setGameModeName(getJsonString(r, "gameModeName"));
                record.setKeyword(getJsonString(r, "keyword"));
                record.setOpponentName(getJsonString(r, "opponentName"));
                record.setResult(getJsonString(r, "result"));
                record.setScore(getJsonInt(r, "score"));
                record.setCorrectCount(getJsonInt(r, "correctCount"));
                record.setWrongCount(getJsonInt(r, "wrongCount"));
                record.setAccuracy(getJsonDouble(r, "accuracy"));
                record.setCreateTime(getJsonString(r, "createTime"));
                record.setDuration(getJsonInt(r, "duration"));
                record.setRank(getJsonInt(r, "rank"));
                record.setTotalRounds(getJsonInt(r, "totalRounds"));
                allRecords.add(record);
            } catch (Exception e) {
                Log.e(TAG, "解析单条记录失败", e);
            }
        }
    }

    private void updateDisplay() {
        displayItems.clear();
        LinkedHashMap<String, List<UserRecordBean>> grouped = new LinkedHashMap<>();
        for (UserRecordBean record : allRecords) {
            String mode = record.getGameModeName() != null ? record.getGameModeName() : "其他";
            grouped.computeIfAbsent(mode, k -> new ArrayList<>()).add(record);
        }
        for (Map.Entry<String, List<UserRecordBean>> entry : grouped.entrySet()) {
            displayItems.add(entry.getKey());
            displayItems.addAll(entry.getValue());
        }

        tvEmpty.setVisibility(displayItems.isEmpty() ? View.VISIBLE : View.GONE);
        rvRecords.setVisibility(displayItems.isEmpty() ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();

        // 修复 RecyclerView 在 NestedScrollView 中高度不展开的问题
        rvRecords.post(() -> {
            ViewGroup.LayoutParams params = rvRecords.getLayoutParams();
            int height = 0;
            for (int i = 0; i < rvRecords.getChildCount(); i++) {
                height += rvRecords.getChildAt(i).getHeight();
            }
            params.height = height > 0 ? height : ViewGroup.LayoutParams.WRAP_CONTENT;
            rvRecords.setLayoutParams(params);
        });
    }

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "--";
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0) {
            return min + "分" + sec + "秒";
        }
        return sec + "秒";
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private int getJsonInt(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsInt();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private long getJsonLong(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsLong();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private double getJsonDouble(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsDouble();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private void showEmpty(String errorMsg) {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(errorMsg != null ? errorMsg : "暂无对战记录");
        rvRecords.setVisibility(View.GONE);
    }

    private void enterSelectionMode(int firstPosition) {
        isSelectionMode = true;
        selectedPositions.clear();
        selectedPositions.add(firstPosition);
        // 先同步选中状态到 adapter，再设置选择模式
        adapter.setSelectedPositions(new HashSet<>(selectedPositions));
        adapter.setSelectionMode(true);
        updateSelectionUI();
        selectionBar.setVisibility(View.VISIBLE);
        // 选择栏遮挡底部内容，添加底部内边距
        int barHeight = (int) (56 * getResources().getDisplayMetrics().density + 0.5f);
        scrollViewContent.setPadding(scrollViewContent.getPaddingLeft(),
                scrollViewContent.getPaddingTop(),
                scrollViewContent.getPaddingRight(),
                barHeight);
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedPositions.clear();
        adapter.setSelectedPositions(new HashSet<>(selectedPositions));
        adapter.setSelectionMode(false);
        selectionBar.setVisibility(View.GONE);
        // 移除选择栏的底部内边距
        scrollViewContent.setPadding(scrollViewContent.getPaddingLeft(),
                scrollViewContent.getPaddingTop(),
                scrollViewContent.getPaddingRight(), 0);
    }

    private void toggleSelectAll() {
        if (selectedPositions.size() == adapter.getRecordItemCount()) {
            selectedPositions.clear();
        } else {
            selectedPositions.clear();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (adapter.getItem(i) instanceof UserRecordBean) {
                    selectedPositions.add(i);
                }
            }
        }
        adapter.setSelectedPositions(selectedPositions);
        updateSelectionUI();
    }

    private void onItemSelectionChanged(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        if (selectedPositions.isEmpty()) {
            exitSelectionMode();
        } else {
            updateSelectionUI();
        }
        adapter.setSelectedPositions(selectedPositions);
    }

    private void updateSelectionUI() {
        int count = selectedPositions.size();
        tvSelectedCount.setText("已选 " + count + " 项");
        if (count == adapter.getRecordItemCount()) {
            tvSelectAll.setText("取消全选");
        } else {
            tvSelectAll.setText("全选");
        }
    }

    private void deleteSelectedRecords() {
        List<Long> idsToDelete = new ArrayList<>();
        for (int pos : selectedPositions) {
            Object item = adapter.getItem(pos);
            if (item instanceof UserRecordBean) {
                idsToDelete.add(((UserRecordBean) item).getId());
            }
        }
        if (idsToDelete.isEmpty()) return;

        new AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除选中的 " + idsToDelete.size() + " 条记录吗？此操作不可撤销。")
            .setPositiveButton("删除", (dialog, which) -> {
                doDeleteRecords(idsToDelete);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void doDeleteRecords(List<Long> ids) {
        String userId = SharedPrefsUtil.getUserId(this);
        if (userId == null || userId.isEmpty()) return;

        JsonObject body = new JsonObject();
        com.google.gson.JsonArray idsArray = new com.google.gson.JsonArray();
        for (Long id : ids) {
            idsArray.add(id);
        }
        body.add("ids", idsArray);

        RequestBody requestBody = RequestBody.create(
            body.toString(), MediaType.parse("application/json"));

        OkHttpUtil.deleteWithAuth(this, ApiConstant.RECORD_DELETE, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RecordActivity.this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JsonObject json = GsonUtil.getGson().fromJson(respBody, JsonObject.class);
                        boolean success = json.has("success") && json.get("success").getAsBoolean();
                        if (success) {
                            Toast.makeText(RecordActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                            exitSelectionMode();
                            loadRecords();
                        } else {
                            String msg = json.has("message") ? json.get("message").getAsString() : "删除失败";
                            Toast.makeText(RecordActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(RecordActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    static class GroupedRecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_RECORD = 1;
        private final List<Object> items;

        private boolean selectionMode = false;
        private Set<Integer> selectedPositions = new HashSet<>();
        private OnItemActionListener listener;

        interface OnItemActionListener {
            void onLongPress(int position);
            void onToggleSelection(int position);
        }

        void setOnItemActionListener(OnItemActionListener l) { this.listener = l; }
        void setSelectionMode(boolean mode) { this.selectionMode = mode; notifyDataSetChanged(); }
        void setSelectedPositions(Set<Integer> positions) { this.selectedPositions = new HashSet<>(positions); notifyDataSetChanged(); }
        void toggleSelection(int position) {
            if (selectedPositions.contains(position)) selectedPositions.remove(position);
            else selectedPositions.add(position);
            notifyItemChanged(position);
        }
        int getRecordItemCount() {
            int count = 0;
            for (Object item : items) { if (item instanceof UserRecordBean) count++; }
            return count;
        }
        Object getItem(int position) { return items.get(position); }

        GroupedRecordAdapter(List<Object> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? TYPE_HEADER : TYPE_RECORD;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_record_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_record, parent, false);
                return new RecordViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).tvHeader.setText((String) items.get(position));
            } else if (holder instanceof RecordViewHolder) {
                UserRecordBean record = (UserRecordBean) items.get(position);
                RecordViewHolder rvh = (RecordViewHolder) holder;

                rvh.tvMode.setText(record.getGameModeName());
                if (record.getKeyword() != null && !record.getKeyword().isEmpty()) {
                    rvh.tvKeyword.setText("[" + record.getKeyword() + "]");
                    rvh.tvKeyword.setVisibility(View.VISIBLE);
                } else {
                    rvh.tvKeyword.setVisibility(View.GONE);
                }

                int rank = record.getRank();
                if (rank > 0) {
                    rvh.tvRank.setText("第" + rank + "名");
                    rvh.tvRank.setVisibility(View.VISIBLE);
                } else {
                    rvh.tvRank.setVisibility(View.GONE);
                }

                String time = record.getCreateTime();
                if (time != null && time.length() > 16) {
                    time = time.substring(0, 16);
                }
                rvh.tvTime.setText(time != null ? time : "--");

                rvh.tvDuration.setText(formatDuration(record.getDuration()));

                int totalRounds = record.getTotalRounds();
                rvh.tvRounds.setText(totalRounds > 0 ? String.valueOf(totalRounds) : "--");

                double acc = record.getAccuracy();
                if (acc > 1) {
                    rvh.tvAccuracy.setText(String.format("%.0f%%", acc));
                } else {
                    rvh.tvAccuracy.setText(String.format("%.0f%%", acc * 100));
                }

                int correct = record.getCorrectCount();
                int wrong = record.getWrongCount();
                rvh.tvCorrect.setText(correct + "/" + wrong);

                int score = record.getScore();
                if (score > 0) {
                    rvh.tvScore.setText("+" + score);
                } else {
                    rvh.tvScore.setText(String.valueOf(score));
                }

                // Selection state
                if (selectionMode) {
                    rvh.itemView.setActivated(selectedPositions.contains(position));
                    if (selectedPositions.contains(position)) {
                        rvh.itemView.setBackgroundResource(R.drawable.item_record_selected);
                    } else {
                        rvh.itemView.setBackgroundResource(R.drawable.card_white);
                    }
                } else {
                    rvh.itemView.setActivated(false);
                    rvh.itemView.setBackgroundResource(R.drawable.card_white);
                }

                rvh.itemView.setOnClickListener(v -> {
                    if (selectionMode && listener != null) {
                        listener.onToggleSelection(position);
                    } else {
                        AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                    }
                });

                rvh.itemView.setOnLongClickListener(v -> {
                    if (!selectionMode && listener != null) {
                        listener.onLongPress(position);
                    }
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private String formatDuration(int seconds) {
            if (seconds <= 0) return "--";
            int min = seconds / 60;
            int sec = seconds % 60;
            if (min > 0) {
                return min + "分" + sec + "秒";
            }
            return sec + "秒";
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvHeader;
            HeaderViewHolder(View itemView) {
                super(itemView);
                tvHeader = itemView.findViewById(R.id.tv_mode_header);
            }
        }

        static class RecordViewHolder extends RecyclerView.ViewHolder {
            TextView tvMode, tvKeyword, tvRank, tvTime, tvDuration;
            TextView tvRounds, tvAccuracy, tvCorrect, tvScore;

            RecordViewHolder(View itemView) {
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
}
