package com.example.feihualinggame.validator.engine.rule;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则配置 —— 从 JSON 文件加载规则，实现"规则定义与代码分离"
 *
 * JSON 格式示例：
 * {
 *   "single_keyword": {
 *     "displayName": "单关键字模式",
 *     "rules": [
 *       {"type": "minLength", "value": 4},
 *       {"type": "contains", "keyword": "${keyword}"},
 *       {"type": "noRepeat"}
 *     ]
 *   }
 * }
 *
 * 占位符 ${keyword}, ${keyword2}, ${position} 在运行时由 RuleContext 注入
 */
public class RuleConfig {

    private static final Gson gson = new Gson();

    public static class ModeDef {
        public final String displayName;
        public final List<RuleDef> rules;

        ModeDef(String displayName, List<RuleDef> rules) {
            this.displayName = displayName;
            this.rules = rules;
        }
    }

    public static class RuleDef {
        public final String type;
        public final String keyword;
        public final String keyword2;
        public final int value;       // minLength用
        public final int position;    // position用

        RuleDef(String type, String keyword, String keyword2, int value, int position) {
            this.type = type;
            this.keyword = keyword;
            this.keyword2 = keyword2;
            this.value = value;
            this.position = position;
        }
    }

    /**
     * 从 assets 目录加载规则配置文件
     */
    public static Map<String, ModeDef> loadFromAssets(Context context, String fileName) {
        Map<String, ModeDef> configMap = new HashMap<>();
        try {
            InputStream is = context.getAssets().open(fileName);
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            for (String modeName : root.keySet()) {
                JsonObject modeObj = root.getAsJsonObject(modeName);
                String displayName = modeObj.get("displayName").getAsString();
                JsonArray rulesArr = modeObj.getAsJsonArray("rules");
                List<RuleDef> ruleDefs = new ArrayList<>();

                for (int i = 0; i < rulesArr.size(); i++) {
                    JsonObject ruleObj = rulesArr.get(i).getAsJsonObject();
                    String type = ruleObj.get("type").getAsString();
                    String keyword = ruleObj.has("keyword") ? ruleObj.get("keyword").getAsString() : null;
                    String keyword2 = ruleObj.has("keyword2") ? ruleObj.get("keyword2").getAsString() : null;
                    int value = ruleObj.has("value") ? ruleObj.get("value").getAsInt() : 0;
                    int position = ruleObj.has("position") ? ruleObj.get("position").getAsInt() : 0;
                    ruleDefs.add(new RuleDef(type, keyword, keyword2, value, position));
                }

                configMap.put(modeName, new ModeDef(displayName, ruleDefs));
            }

            reader.close();
        } catch (Exception e) {
            android.util.Log.e("RuleConfig", "加载规则配置失败: " + fileName, e);
        }
        return configMap;
    }

    /**
     * 使用内置配置（JSON 文件缺失时的兜底方案）
     */
    public static Map<String, ModeDef> getBuiltin() {
        Map<String, ModeDef> configMap = new HashMap<>();

        configMap.put("single_keyword", new ModeDef("单关键字飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("contains", "${keyword}", null, 0, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));
        configMap.put("position", new ModeDef("位置飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("position", "${keyword}", null, 0, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));
        configMap.put("double_keyword", new ModeDef("双关键字飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("contains", "${keyword}", null, 0, 0),
            new RuleDef("contains", "${keyword2}", null, 0, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));
        configMap.put("chain", new ModeDef("首尾接龙飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("chainStart", null, null, 0, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));
        configMap.put("forbidden", new ModeDef("反飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("notContains", "${keyword}", null, 0, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));
        configMap.put("color", new ModeDef("颜色飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("contains", "${keyword}", null, 0, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));
        configMap.put("number", new ModeDef("数字飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("contains", "${keyword}", null, 0, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));
        configMap.put("custom", new ModeDef("自定义飞花令", List.of(
            new RuleDef("minLength", null, null, 2, 0),
            new RuleDef("noRepeat", null, null, 0, 0)
        )));

        return configMap;
    }
}
