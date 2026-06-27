package com.example.feihualinggame.validator.engine.rule;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则冲突检测器
 *
 * 在规则组合前执行冲突扫描，检测：
 *   - 硬冲突（Hard）：contains("X") + notContains("X") → 阻止加载
 *   - 位置冲突：position("X",3) + position("X",7) → 阻止加载
 *   - 链式冲突：chainStart + position(1) → 阻止加载
 */
public class ConflictDetector {

    public static class Conflict {
        public final String ruleA;
        public final String ruleB;
        public final String description;

        Conflict(String ruleA, String ruleB, String description) {
            this.ruleA = ruleA;
            this.ruleB = ruleB;
            this.description = description;
        }

        @Override
        public String toString() {
            return "【冲突】" + ruleA + " ↔ " + ruleB + ": " + description;
        }
    }

    /**
     * 扫描规则列表，返回所有冲突
     * @return 冲突列表，空表示无冲突
     */
    public static List<Conflict> detect(List<AtomicRule> rules) {
        List<Conflict> conflicts = new ArrayList<>();

        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                AtomicRule a = rules.get(i);
                AtomicRule b = rules.get(j);

                String desc = a.conflictsWith(b);
                if (desc != null) {
                    conflicts.add(new Conflict(
                        a.getType() + "[" + a.getDescription() + "]",
                        b.getType() + "[" + b.getDescription() + "]",
                        desc
                    ));
                }
            }
        }

        return conflicts;
    }

    /**
     * 检测并抛出异常（用于初始化时）
     */
    public static void checkAndThrow(List<AtomicRule> rules) {
        List<Conflict> conflicts = detect(rules);
        if (!conflicts.isEmpty()) {
            StringBuilder sb = new StringBuilder("规则冲突检测失败：\n");
            for (Conflict c : conflicts) {
                sb.append("  ").append(c).append("\n");
            }
            throw new IllegalStateException(sb.toString());
        }
    }
}
