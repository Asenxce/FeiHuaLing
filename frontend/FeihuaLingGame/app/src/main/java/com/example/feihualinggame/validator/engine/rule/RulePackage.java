package com.example.feihualinggame.validator.engine.rule;

import android.content.Context;
import android.util.Log;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.engine.RuleContext;
import com.example.feihualinggame.utils.PoetryCleanUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则包 —— 一条游戏模式对应一个 RulePackage
 *
 * 职责：
 * 1. 从配置加载规则定义，根据上下文(Context)实例化原子规则
 * 2. 执行冲突检测（初始化时一次性检测）
 * 3. 提供 RuleComposer 执行规则链
 */
public class RulePackage {

    private static final String TAG = "RulePackage";

    private final String gameMode;
    private final String displayName;
    private final RuleComposer composer;
    private final List<AtomicRule> rules;

    public RulePackage(String gameMode, String displayName, RuleComposer composer, List<AtomicRule> rules) {
        this.gameMode = gameMode;
        this.displayName = displayName;
        this.composer = composer;
        this.rules = rules;
    }

    public String getGameMode() { return gameMode; }
    public String getDisplayName() { return displayName; }
    public RuleComposer getComposer() { return composer; }
    public List<AtomicRule> getRules() { return rules; }

    /**
     * 从配置定义构建 RulePackage
     */
    public static RulePackage fromConfig(Context context, String gameMode, RuleContext ctx) {
        Map<String, RuleConfig.ModeDef> configMap;
        try {
            configMap = RuleConfig.loadFromAssets(context, "rule_engine.json");
            if (configMap.isEmpty()) {
                configMap = RuleConfig.getBuiltin();
            }
        } catch (Exception e) {
            Log.w(TAG, "加载JSON配置失败，使用内置配置", e);
            configMap = RuleConfig.getBuiltin();
        }

        RuleConfig.ModeDef modeDef = configMap.get(gameMode);
        if (modeDef == null) {
            Log.w(TAG, "未找到模式 " + gameMode + "，回退到 single_keyword");
            modeDef = configMap.get("single_keyword");
        }

        // 根据 RuleDef 实例化 AtomicRule，注入上下文参数
        List<AtomicRule> rules = new ArrayList<>();
        rules.add(new CleanRule());  // 清洗步骤内置

        for (RuleConfig.RuleDef def : modeDef.rules) {
            AtomicRule rule = instantiate(def, ctx);
            if (rule != null) {
                rules.add(rule);
            }
        }

        // 冲突检测
        List<ConflictDetector.Conflict> conflicts = ConflictDetector.detect(rules);
        if (!conflicts.isEmpty()) {
            Log.w(TAG, "规则冲突检测到 " + conflicts.size() + " 个冲突:");
            for (ConflictDetector.Conflict c : conflicts) {
                Log.w(TAG, "  " + c);
            }
        }

        RuleComposer composer = new RuleComposer(rules);
        Log.i(TAG, "加载模式 [" + gameMode + "]: " + composer.getDescription());

        return new RulePackage(gameMode, modeDef.displayName, composer, rules);
    }

    private static AtomicRule instantiate(RuleConfig.RuleDef def, RuleContext ctx) {
        String keyword = resolve(def.keyword, ctx);
        String keyword2 = resolve(def.keyword2, ctx);

        switch (def.type) {
            case "minLength":
                return new MinLengthRule(def.value > 0 ? def.value : 4);
            case "contains":
                return keyword != null ? new ContainsRule(keyword) : null;
            case "notContains":
                return keyword != null ? new NotContainsRule(keyword) : null;
            case "position":
                return keyword != null ? new PositionRule(keyword, ctx.getKeywordPosition() > 0 ? ctx.getKeywordPosition() : def.position) : null;
            case "chainStart":
                return new ChainStartRule();
            case "noRepeat":
                return new NoRepeatRule();
            case "maxLength":
                return new MaxLengthRule(def.value > 0 ? def.value : 12);
            case "charCountMatch":
                return new CharCountMatchRule();
            default:
                Log.w(TAG, "未知规则类型: " + def.type);
                return null;
        }
    }

    /**
     * 解析占位符：${keyword} → ctx.getKeyword()，否则直接返回原值
     */
    private static String resolve(String template, RuleContext ctx) {
        if (template == null) return null;
        if ("${keyword}".equals(template)) return ctx.getKeyword();
        if ("${keyword2}".equals(template)) return ctx.getKeyword2();
        if ("${position}".equals(template)) return String.valueOf(ctx.getKeywordPosition());
        return template;
    }

    /**
     * 清洗步骤：去除标点 + 繁简转换
     */
    private static class CleanRule implements AtomicRule {
        @Override
        public String getType() { return "clean"; }
        @Override
        public String getDescription() { return "清洗标点与繁简转换"; }

        @Override
        public BaseValidationResult evaluate(RuleContext ctx) {
            String cleaned = PoetryCleanUtil.cleanPoetry(ctx.getAnswer());
            ctx.setAnswer(cleaned);
            return BaseValidationResult.success(cleaned);
        }

        @Override
        public String conflictsWith(AtomicRule other) { return null; }
    }
}
