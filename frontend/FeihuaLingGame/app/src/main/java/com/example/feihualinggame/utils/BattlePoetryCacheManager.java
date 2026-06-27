package com.example.feihualinggame.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对战诗词缓存管理器
 * 用于记录已使用的诗句（防重复）和关键字记录
 */
public class BattlePoetryCacheManager {
    private static final String TAG = "BattlePoetryCache";
    
    // 使用HashSet提升查找效率：从O(n)优化到O(1)
    private static Set<String> usedPoemsSet = new HashSet<>();
    private static List<String> usedPoemsList = new ArrayList<>();
    
    /**
     * 记录玩家使用的诗句
     */
    public static void markPoemAsUsed(String poem) {
        if (poem != null && !poem.isEmpty()) {
            usedPoemsSet.add(poem);
            usedPoemsList.add(poem);
        }
    }
    
    /**
     * 检查诗句是否已被使用 - O(1)时间复杂度
     */
    public static boolean isPoemUsed(String poem) {
        return poem != null && usedPoemsSet.contains(poem);
    }
    
    /**
     * 清除已使用记录
     */
    public static void clearUsedPoems() {
        usedPoemsSet.clear();
        usedPoemsList.clear();
    }
    
    /**
     * 获取已使用诗句列表
     */
    public static List<String> getUsedPoems() {
        return new ArrayList<>(usedPoemsList);
    }
    
    /**
     * 获取已使用诗句数量
     */
    public static int getUsedCount() {
        return usedPoemsSet.size();
    }

    /**
     * 获取已使用诗句集合（供规则引擎共享去重）
     */
    public static java.util.Set<String> getUsedPoemsSet() {
        return usedPoemsSet;
    }
}
