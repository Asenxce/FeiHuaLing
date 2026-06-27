package com.example.feihualinggame.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.feihualinggame.activity.PoetryDetailActivity;
import com.example.feihualinggame.adapter.CollectionAdapter;
import com.example.feihualinggame.bean.Poetry;
import com.example.feihualinggame.utils.PoetryCollectionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 经典收藏Fragment
 */
public class CollectionFragment extends Fragment {
    private RecyclerView rvCollection;
    private LinearLayout tvEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private CollectionAdapter adapter;
    private List<Poetry> collectionList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCollection = view.findViewById(R.id.rv_collection);
        tvEmpty = view.findViewById(R.id.tv_collection_empty);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_collection);

        collectionList = new ArrayList<>();
        adapter = new CollectionAdapter(collectionList, this::onPoetryClick);
        rvCollection.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCollection.setAdapter(adapter);

        // 设置下拉刷新
        swipeRefresh.setColorSchemeResources(
            R.color.poetry_primary,
            R.color.poetry_sakura,
            R.color.win_green
        );
        swipeRefresh.setOnRefreshListener(() -> {
            loadCollectionFromServer();
        });

        // 从服务器加载收藏列表
        loadCollectionFromServer();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 刷新收藏列表
        loadCollectionFromServer();
    }

    /**
     * 从服务器加载收藏列表
     */
    private void loadCollectionFromServer() {
        PoetryCollectionManager.loadCollectionFromServer(requireContext(), new PoetryCollectionManager.OnCollectionLoadListener() {
            @Override
            public void onSuccess(List<Poetry> collection) {
                requireActivity().runOnUiThread(() -> {
                    collectionList.clear();
                    collectionList.addAll(collection);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    // 停止刷新动画
                    if (swipeRefresh.isRefreshing()) {
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    private void onPoetryClick(Poetry poetry) {
        Intent intent = new Intent(requireContext(), PoetryDetailActivity.class);
        intent.putExtra("poetry", poetry);
        startActivity(intent);
    }

    private void updateEmptyState() {
        if (collectionList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvCollection.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvCollection.setVisibility(View.VISIBLE);
        }
    }
}
