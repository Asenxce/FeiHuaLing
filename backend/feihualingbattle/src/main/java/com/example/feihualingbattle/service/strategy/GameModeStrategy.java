package com.example.feihualingbattle.service.strategy;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.Poetry;

import java.util.List;

/**
 * 游戏模式策略接口 —— 规则引擎核心
 * 每种游戏模式对应一个不同的策略实现，通过策略模式消除switch-case，
 * 实现规则的热插拔和动态扩展
 */
public interface GameModeStrategy {

    /**
     * 返回策略支持的游戏模式名称
     */
    String getModeName();

    /**
     * 验证用户答案是否符合该模式的规则
     * @param battle 对战信息
     * @param normalizedAnswer 规范化后的答案（简体无标点）
     * @return 错误消息，null表示验证通过
     */
    String validate(Battle battle, String normalizedAnswer);

    /**
     * 从诗词库中搜索匹配合规的诗词
     * @param ctx 查询上下文
     * @return 候选诗词列表
     */
    List<Poetry> searchPoetry(GameModeContext ctx);

    /**
     * 为AI获取符合该模式规则的诗词
     * @param ctx 查询上下文
     * @param usedPoems 已使用诗句列表（用于去重）
     * @return 候选诗词列表
     */
    List<Poetry> getAIAnswer(GameModeContext ctx, List<String> usedPoems);
}
