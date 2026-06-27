package com.example.feihualinggame.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.activity.PoetryDetailActivity;
import com.example.feihualinggame.adapter.PoetryAdapter;
import com.example.feihualinggame.bean.Poetry;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 诗词查询Fragment
 */
public class PoetryQueryFragment extends Fragment {
    private EditText etQueryKeyword;
    private Button btnSearchPoetry;
    private ProgressBar progressLoading;
    private RecyclerView rvPoetryResults;
    private View layoutEmpty;
    private LinearLayout layoutDailyPoetry;
    private TextView tvDailyTitle, tvDailyAuthor, tvDailyContent;
    private LinearLayout layoutHotTags;
    private LinearLayout layoutSearchHistory;
    private ChipGroup chipGroupHistory;
    private TextView tvClearHistory;

    private List<Poetry> poetryList;
    private PoetryAdapter adapter;
    private boolean isLoading = false;
    private boolean isLoadingMore = false;
    private boolean hasMore = true;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;
    private String currentKeyword = "";

    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private static final String PREFS_NAME = "poetry_search_prefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final int MAX_HISTORY_COUNT = 10;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_poetry_query, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etQueryKeyword = view.findViewById(R.id.etQueryKeyword);
        btnSearchPoetry = view.findViewById(R.id.btnSearchPoetry);
        progressLoading = view.findViewById(R.id.progressLoading);
        rvPoetryResults = view.findViewById(R.id.rvPoetryResults);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        layoutDailyPoetry = view.findViewById(R.id.layout_daily_poetry);
        tvDailyTitle = view.findViewById(R.id.tv_daily_title);
        tvDailyAuthor = view.findViewById(R.id.tv_daily_author);
        tvDailyContent = view.findViewById(R.id.tv_daily_content);

        layoutHotTags = view.findViewById(R.id.layout_hot_tags);

        layoutSearchHistory = view.findViewById(R.id.layout_search_history);
        chipGroupHistory = view.findViewById(R.id.chip_group_history);
        tvClearHistory = view.findViewById(R.id.tv_clear_history);

        if (etQueryKeyword == null || btnSearchPoetry == null || rvPoetryResults == null ||
            layoutEmpty == null || layoutDailyPoetry == null || layoutHotTags == null ||
            layoutSearchHistory == null || chipGroupHistory == null) {
            android.util.Log.e("PoetryQuery", "关键控件初始化失败，请检查 XML 布局 ID");
            return;
        }

        poetryList = new ArrayList<>();
        adapter = new PoetryAdapter(requireContext(), poetryList);
        rvPoetryResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPoetryResults.setAdapter(adapter);

        adapter.setOnPoetryClickListener(poetry -> {
            Intent intent = new Intent(requireContext(), PoetryDetailActivity.class);
            intent.putExtra("poetry", poetry);
            startActivity(intent);
        });

        rvPoetryResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoadingMore && hasMore) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                            && firstVisibleItemPosition >= 0) {
                        loadMore();
                    }
                }
            }
        });

        btnSearchPoetry.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            performSearch();
        });

        etQueryKeyword.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        etQueryKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }
                String keyword = s.toString().trim();
                if (!keyword.isEmpty() && !keyword.equals(currentKeyword)) {
                    debounceRunnable = () -> performSearch();
                    debounceHandler.postDelayed(debounceRunnable, 500);
                }
            }
        });

        etQueryKeyword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && poetryList.isEmpty() && !isLoading) {
                loadSearchHistory();
                layoutSearchHistory.setVisibility(View.VISIBLE);
            } else {
                layoutSearchHistory.setVisibility(View.GONE);
            }
        });

        tvClearHistory.setOnClickListener(v -> {
            clearSearchHistory();
            layoutSearchHistory.setVisibility(View.GONE);
        });

        loadHotTags();
    }
    
    /**
     * 加载热门搜索标签
     */
    private void loadHotTags() {
        String[] hotTags = {"李白", "杜甫", "苏轼", "明月", "春风", "山水", "爱情", "思乡"};
        layoutHotTags.removeAllViews();

        for (String tag : hotTags) {
            TextView tagView = new TextView(requireContext());
            tagView.setText(tag);
            tagView.setTextSize(14);
            tagView.setTextColor(getResources().getColor(R.color.poetry_primary));
            tagView.setPadding(16, 8, 16, 8);
            tagView.setBackgroundResource(R.drawable.bg_hot_tag);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(8);
            tagView.setLayoutParams(params);

            tagView.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                etQueryKeyword.setText(tag);
                performSearch();
            });

            layoutHotTags.addView(tagView);
        }
    }
    
    /**
     * 加载搜索历史
     */
    private void loadSearchHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> history = prefs.getStringSet(KEY_SEARCH_HISTORY, new LinkedHashSet<>());
        
        chipGroupHistory.removeAllViews();
        
        if (history == null || history.isEmpty()) {
            layoutSearchHistory.setVisibility(View.GONE);
            return;
        }
        
        for (String keyword : history) {
            Chip chip = new Chip(requireContext());
            chip.setText(keyword);
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(getResources().getColor(R.color.poetry_text_primary));
            chip.setCloseIconVisible(false);
            
            chip.setOnClickListener(v -> {
                AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
                etQueryKeyword.setText(keyword);
                performSearch();
            });
            
            chipGroupHistory.addView(chip);
        }
        
        layoutSearchHistory.setVisibility(View.VISIBLE);
    }
    
    /**
     * 保存搜索历史
     */
    private void saveSearchHistory(String keyword) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, getContext().MODE_PRIVATE);
        Set<String> history = new LinkedHashSet<>(prefs.getStringSet(KEY_SEARCH_HISTORY, new LinkedHashSet<>()));
        
        // 移除旧记录（如果存在）
        history.remove(keyword);
        // 添加到最前面
        history.add(keyword);
        
        // 限制历史记录数量
        if (history.size() > MAX_HISTORY_COUNT) {
            List<String> historyList = new ArrayList<>(history);
            historyList = historyList.subList(0, MAX_HISTORY_COUNT);
            history = new LinkedHashSet<>(historyList);
        }
        
        prefs.edit().putStringSet(KEY_SEARCH_HISTORY, history).apply();
    }
    
    /**
     * 清空搜索历史
     */
    private void clearSearchHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, getContext().MODE_PRIVATE);
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply();
        chipGroupHistory.removeAllViews();
        layoutSearchHistory.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "搜索历史已清空", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 加载每日一诗（已移至 MainActivity 弹窗，此处保留空实现）
     */
    private void loadDailyPoetry() {
        // 功能已移至主界面每日诗签弹窗
    }

    /**
     * 显示/隐藏加载动画
     */
    private void showLoading(boolean show) {
        requireActivity().runOnUiThread(() -> {
            progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            isLoading = show;
            if (show) {
                rvPoetryResults.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
            }
        });
    }

    private void showEmptyState(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (show) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvPoetryResults.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                rvPoetryResults.setVisibility(View.VISIBLE);
            }
        });
    }

    private void performSearch() {
        if (isLoading) return;

        String keyword = etQueryKeyword.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(requireContext(), "请输入关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }

        saveSearchHistory(keyword);
        layoutSearchHistory.setVisibility(View.GONE);

        currentKeyword = keyword;
        currentPage = 0;
        hasMore = true;
        poetryList.clear();
        adapter.setHighlightKeyword(keyword);
        adapter.notifyDataSetChanged();

        showLoading(true);
        fetchPage(currentPage, false);
    }

    private void loadMore() {
        if (isLoadingMore || !hasMore || currentKeyword.isEmpty()) return;
        isLoadingMore = true;
        fetchPage(currentPage, true);
    }

    private void fetchPage(int page, boolean isLoadMore) {
        StringBuilder urlBuilder = new StringBuilder(ApiConstant.POETRY_SEARCH);
        urlBuilder.append("?keyword=").append(currentKeyword);
        urlBuilder.append("&page=").append(page);
        urlBuilder.append("&size=").append(PAGE_SIZE);

        OkHttpUtil.getWithAuth(requireContext(), urlBuilder.toString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isLoading = false;
                isLoadingMore = false;
                requireActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    if (!isLoadMore && poetryList.isEmpty()) {
                        showEmptyState(true);
                    }
                    Toast.makeText(requireContext(), "网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                int responseCode = response.code();

                android.util.Log.d("PoetryQuery", "响应码: " + responseCode + ", page: " + page);

                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JsonElement jsonElement = JsonParser.parseString(responseBody);
                            JsonArray dataArray = null;

                            if (jsonElement.isJsonArray()) {
                                dataArray = jsonElement.getAsJsonArray();
                            } else if (jsonElement.isJsonObject()) {
                                JsonObject jsonObject = jsonElement.getAsJsonObject();
                                int code = jsonObject.has("code") ? jsonObject.get("code").getAsInt() : 200;

                                if (code == 200 && jsonObject.has("data")) {
                                    JsonElement dataElement = jsonObject.get("data");
                                    if (dataElement.isJsonArray()) {
                                        dataArray = dataElement.getAsJsonArray();
                                    }
                                }
                            }

                            int parsedCount = 0;
                            if (dataArray != null && dataArray.size() > 0) {
                                if (!isLoadMore) {
                                    poetryList.clear();
                                }

                                for (JsonElement element : dataArray) {
                                    JsonObject poetryObj = element.getAsJsonObject();

                                    String fullContent = poetryObj.has("fullContent") ? poetryObj.get("fullContent").getAsString() : null;
                                    String content = poetryObj.has("content") ? poetryObj.get("content").getAsString() : "";
                                    String author = poetryObj.has("author") ? poetryObj.get("author").getAsString() : "未知";
                                    String title = poetryObj.has("title") ? poetryObj.get("title").getAsString() : "无题";
                                    String dynasty = poetryObj.has("dynasty") ? poetryObj.get("dynasty").getAsString() : null;

                                    long poetryId = (title + author).hashCode() & 0xFFFFFFFFL;

                                    Poetry poetry = new Poetry();
                                    poetry.setId(poetryId);
                                    poetry.setFullContent(fullContent);
                                    poetry.setContent(content);
                                    poetry.setAuthor(author);
                                    poetry.setTitle(title);
                                    poetry.setDynasty(dynasty);

                                    poetryList.add(poetry);
                                    parsedCount++;
                                }

                                hasMore = dataArray.size() >= PAGE_SIZE;
                                currentPage = page + 1;
                            } else {
                                hasMore = false;
                            }

                            adapter.notifyDataSetChanged();
                            progressLoading.setVisibility(View.GONE);
                            isLoading = false;
                            isLoadingMore = false;

                            if (!isLoadMore) {
                                if (poetryList.isEmpty()) {
                                    showEmptyState(true);
                                    Toast.makeText(requireContext(), "未找到相关诗词", Toast.LENGTH_SHORT).show();
                                } else {
                                    showEmptyState(false);
                                    Toast.makeText(requireContext(), "找到 " + poetryList.size() + " 首诗词", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("PoetryQuery", "解析失败", e);
                            progressLoading.setVisibility(View.GONE);
                            isLoading = false;
                            isLoadingMore = false;
                            if (!isLoadMore) showEmptyState(true);
                            Toast.makeText(requireContext(), "数据解析失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressLoading.setVisibility(View.GONE);
                        isLoading = false;
                        isLoadingMore = false;
                        if (!isLoadMore) showEmptyState(true);
                        Toast.makeText(requireContext(), "搜索失败: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

}
