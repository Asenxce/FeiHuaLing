package com.example.feihualingbattle.service.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 规则引擎策略工厂
 * 自动发现所有GameModeStrategy实现，按模式名称注册到调度表
 * 替代原来BattleService中的所有switch-case分发
 */
@Component
public class GameModeStrategyFactory {

    private final Map<String, GameModeStrategy> strategyMap = new HashMap<>();
    private final GameModeStrategy defaultStrategy;

    @Autowired
    public GameModeStrategyFactory(List<GameModeStrategy> strategies, SimpleKeywordStrategy simpleKeywordStrategy) {
        this.defaultStrategy = simpleKeywordStrategy;
        for (GameModeStrategy strategy : strategies) {
            String modeName = strategy.getModeName().toLowerCase();
            strategyMap.put(modeName, strategy);
            if ("simple".equals(modeName)) {
                strategyMap.put("single_keyword", strategy);
            }
        }
    }

    /**
     * 根据游戏模式获取对应策略
     */
    public GameModeStrategy getStrategy(String gameMode) {
        if (gameMode == null) {
            return defaultStrategy;
        }
        return strategyMap.getOrDefault(gameMode.toLowerCase(), defaultStrategy);
    }

    /**
     * 获取所有已注册的模式名称
     */
    public Set<String> getRegisteredModes() {
        return Collections.unmodifiableSet(strategyMap.keySet());
    }
}
