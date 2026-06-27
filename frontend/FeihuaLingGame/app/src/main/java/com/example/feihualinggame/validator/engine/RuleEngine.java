package com.example.feihualinggame.validator.engine;

import android.content.Context;
import android.util.Log;

import com.example.feihualinggame.validator.BaseValidationResult;
import com.example.feihualinggame.validator.FeiHuaLingValidator;
import com.example.feihualinggame.validator.engine.rule.AtomicRule;
import com.example.feihualinggame.validator.engine.rule.ConflictDetector;
import com.example.feihualinggame.validator.engine.rule.RuleComposer;
import com.example.feihualinggame.validator.engine.rule.RulePackage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则引擎 —— 全局调度中心
 *
 * 架构（自底向上）：
 *   AtomicRule（原子规则）
 *     → RuleComposer（规则组合器）
 *       → RulePackage（规则包 = 组合器 + 冲突检测 + 配置解析）
 *         → RuleEngine（引擎 = 注册表 + 统一入口）
 *
 * 用法：
 *   RuleEngine engine = new RuleEngine(context);
 *   engine.registerFromConfig(ctx);
 *   BaseValidationResult result = engine.validate(answer);
 */
public class RuleEngine implements FeiHuaLingValidator {

    private static final String TAG = "RuleEngine";

    private final Context context;

    /** 模式 → 规则包 注册表 */
    private final Map<String, RulePackage> registry = new HashMap<>();

    /** 当前激活的规则包 */
    private RulePackage activePackage;

    /** 当前的规则上下文 */
    private RuleContext contextData;

    /** 共享的去重记录 */
    private final Set<String> usedPoems = new HashSet<>();

    // ==================== 构造 & 初始化 ====================

    public RuleEngine(Context context) {
        this.context = context;
    }

    /**
     * 注册指定模式的规则包，并设置为激活状态
     */
    public void registerFromConfig(Context appContext, RuleContext ctx) {
        this.contextData = ctx;
        String mode = ctx.getGameMode();

        RulePackage pkg = RulePackage.fromConfig(appContext, mode, ctx);
        registry.put(mode, pkg);
        activePackage = pkg;

        Log.i(TAG, "规则引擎初始化完成 — 模式: " + mode
            + " (" + pkg.getDisplayName() + ")"
            + " | 规则数: " + pkg.getComposer().getRuleCount()
            + " | 规则链: " + pkg.getComposer().getDescription());
    }

    // ==================== 规则执行 ====================

    @Override
    public BaseValidationResult validate(String answer) {
        if (activePackage == null) {
            return BaseValidationResult.error("规则引擎未初始化，请先调用 registerFromConfig()");
        }
        if (contextData == null) {
            return BaseValidationResult.error("规则上下文未设置");
        }

        contextData.setAnswer(answer);
        return activePackage.getComposer().execute(contextData);
    }

    // ==================== 查询 ====================

    public RulePackage getActivePackage() { return activePackage; }

    public RuleContext getContext() { return contextData; }

    public String getRuleDescription() {
        return activePackage != null ? activePackage.getComposer().getDescription() : "未初始化";
    }

    public List<AtomicRule> getRules() {
        return activePackage != null ? activePackage.getRules() : List.of();
    }

    public String getDisplayName() {
        return activePackage != null ? activePackage.getDisplayName() : "未知模式";
    }

    // ==================== 冲突检测结果 ====================

    public List<ConflictDetector.Conflict> getConflicts() {
        return activePackage != null
            ? ConflictDetector.detect(activePackage.getRules())
            : List.of();
    }

    public boolean hasConflicts() {
        return !getConflicts().isEmpty();
    }

    // ==================== 生命周期 ====================

    @Override
    public int getUsedCount() { return usedPoems.size(); }

    @Override
    public void clear() {
        usedPoems.clear();
        activePackage = null;
        registry.clear();
    }
}
