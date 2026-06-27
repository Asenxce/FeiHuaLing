package com.example.feihualinggame.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.feihualinggame.R;
import com.example.feihualinggame.adapter.PoetryAdapter;
import com.example.feihualinggame.bean.Poetry;
import com.example.feihualinggame.constant.ApiConstant;
import com.example.feihualinggame.utils.OkHttpUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 诗词查询页面
 * 优化：搜索防抖、分页加载、类型切换联动
 */
public class PoetryQueryActivity extends BaseActivity {
    private Spinner spnQueryType;
    private EditText etQueryKeyword;
    private Button btnSearchPoetry;
    private RecyclerView rvPoetryResults;
    private ProgressBar progressLoading;

    private List<Poetry> poetryList;
    private PoetryAdapter adapter;
    private String currentKeyword;

    private boolean isLoading = false;
    private boolean isLoadingMore = false;
    private boolean hasMore = true;
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private String[] queryTypes = {"按关键字", "按作者", "按标题", "随机推荐"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentViewWithBars(R.layout.activity_poetry_query_content);

        spnQueryType = findViewById(R.id.spnQueryType);
        etQueryKeyword = findViewById(R.id.etQueryKeyword);
        btnSearchPoetry = findViewById(R.id.btnSearchPoetry);
        rvPoetryResults = findViewById(R.id.rvPoetryResults);
        progressLoading = findViewById(R.id.progressLoading);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, queryTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnQueryType.setAdapter(spinnerAdapter);

        poetryList = new ArrayList<>();
        adapter = new PoetryAdapter(this, poetryList);
        rvPoetryResults.setLayoutManager(new LinearLayoutManager(this));
        rvPoetryResults.setAdapter(adapter);

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

        btnSearchPoetry.setOnClickListener(v -> performSearch());

        spnQueryType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String keyword = etQueryKeyword.getText().toString().trim();
                if (!keyword.isEmpty() || "随机推荐".equals(queryTypes[position])) {
                    performSearch();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        poetryList.clear();
        adapter.notifyDataSetChanged();
        etQueryKeyword.setText("");
        currentKeyword = "";
        currentPage = 0;
        hasMore = true;
    }

    private void performSearch() {
        if (isLoading) return;

        String keyword = etQueryKeyword.getText().toString().trim();
        int queryTypeIndex = spnQueryType.getSelectedItemPosition();
        String type = queryTypes[queryTypeIndex];

        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }

        if ("随机推荐".equals(type)) {
            currentKeyword = null;
            adapter.setHighlightKeyword(null);
            poetryList.clear();
            adapter.notifyDataSetChanged();
            progressLoading.setVisibility(View.VISIBLE);
            fetchRandomPoetry();
            return;
        }

        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        currentKeyword = keyword;
        currentPage = 0;
        hasMore = true;
        poetryList.clear();
        adapter.setHighlightKeyword(keyword);
        adapter.notifyDataSetChanged();

        progressLoading.setVisibility(View.VISIBLE);
        fetchPage(currentPage, false);
    }

    private void loadMore() {
        if (isLoadingMore || !hasMore || currentKeyword == null || currentKeyword.isEmpty()) return;
        isLoadingMore = true;
        fetchPage(currentPage, true);
    }

    private void fetchPage(int page, boolean isLoadMore) {
        int queryTypeIndex = spnQueryType.getSelectedItemPosition();
        String type = queryTypes[queryTypeIndex];
        String paramName;
        String paramValue;

        switch (type) {
            case "按作者":
                paramName = "author";
                paramValue = currentKeyword;
                break;
            case "按标题":
                paramName = "title";
                paramValue = currentKeyword;
                break;
            default:
                paramName = "keyword";
                paramValue = currentKeyword;
                break;
        }

        String url = ApiConstant.POETRY_SEARCH + "?" + paramName + "=" + paramValue
                + "&page=" + page + "&size=" + PAGE_SIZE;

        OkHttpUtil.get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isLoading = false;
                isLoadingMore = false;
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(PoetryQueryActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    runOnUiThread(() -> {
                        parsePoetryResponse(result, currentKeyword, isLoadMore);
                        progressLoading.setVisibility(View.GONE);
                        isLoading = false;
                        isLoadingMore = false;
                    });
                } else {
                    isLoading = false;
                    isLoadingMore = false;
                    runOnUiThread(() -> progressLoading.setVisibility(View.GONE));
                }
            }
        });
    }

    private void fetchRandomPoetry() {
        OkHttpUtil.get(ApiConstant.POETRY_RANDOM, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isLoading = false;
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(PoetryQueryActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    runOnUiThread(() -> {
                        progressLoading.setVisibility(View.GONE);
                        isLoading = false;
                        parsePoetryResponse(result, null, false);
                    });
                }
            }
        });
    }

    private void parsePoetryResponse(String jsonResponse, String keyword, boolean isLoadMore) {
        try {
            if (!isLoadMore) {
                poetryList.clear();
            }

            Gson gson = new Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<java.util.Map<String, Object>>>(){}.getType();
            List<java.util.Map<String, Object>> dataList = gson.fromJson(jsonResponse, type);

            int successCount = 0;
            if (dataList != null && !dataList.isEmpty()) {
                for (int i = 0; i < dataList.size(); i++) {
                    try {
                        java.util.Map<String, Object> item = dataList.get(i);

                        Object fullContentObj = item.get("fullContent");
                        Object contentObj = item.get("content");
                        Object authorObj = item.get("author");
                        Object titleObj = item.get("title");
                        Object dynastyObj = item.get("dynasty");

                        String fullContent = fullContentObj != null ? fullContentObj.toString() : null;
                        String content = contentObj != null ? contentObj.toString() : null;
                        String author = authorObj != null ? authorObj.toString() : null;
                        String title = titleObj != null ? titleObj.toString() : null;
                        String dynasty = dynastyObj != null ? dynastyObj.toString() : null;

                        String displayContent = fullContent != null ? fullContent : content;

                        if (displayContent != null && !displayContent.isEmpty()) {
                            Poetry poetry = new Poetry();
                            poetry.setFullContent(fullContent);
                            poetry.setContent(content);
                            poetry.setAuthor(author != null ? author : "未知");
                            poetry.setTitle(title != null ? title : "无题");
                            poetry.setDynasty(dynasty);
                            poetryList.add(poetry);
                            successCount++;
                        }
                    } catch (Exception e) {
                        android.util.Log.w("PoetryQuery", "解析第" + (i+1) + "条数据失败", e);
                    }
                }

                hasMore = dataList.size() >= PAGE_SIZE;
                currentPage = isLoadMore ? currentPage + 1 : 1;
            } else {
                hasMore = false;
            }

            adapter.notifyDataSetChanged();

            if (!isLoadMore) {
                if (poetryList.isEmpty()) {
                    Toast.makeText(this, "未找到相关诗词", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "找到 " + poetryList.size() + " 首诗词", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PoetryQuery", "解析失败", e);
            if (!isLoadMore) {
                Toast.makeText(this, "数据解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addSamplePoetry(String keyword) {
        poetryList.clear();

        poetryList.add(new Poetry(
            "床前明月光，\n疑是地上霜。\n举头望明月，\n低头思故乡。",
            "李白",
            "静夜思",
            "唐"
        ));

        poetryList.add(new Poetry(
            "春眠不觉晓，\n处处闻啼鸟。\n夜来风雨声，\n花落知多少。",
            "孟浩然",
            "春晓",
            "唐"
        ));

        poetryList.add(new Poetry(
            "白日依山尽，\n黄河入海流。\n欲穷千里目，\n更上一层楼。",
            "王之涣",
            "登鹳雀楼",
            "唐"
        ));

        adapter.notifyDataSetChanged();

        if (!keyword.isEmpty()) {
            Toast.makeText(this, "找到 " + poetryList.size() + " 首诗词", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "查询完成", Toast.LENGTH_SHORT).show();
        }
    }
}
