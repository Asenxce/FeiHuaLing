package com.example.feihualinggame.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.feihualinggame.R;
import com.example.feihualinggame.utils.AudioController;
import com.example.feihualinggame.utils.ButtonAnimationHelper;
import com.example.feihualinggame.utils.SystemUIUtil;
import com.example.feihualinggame.validator.engine.RuleContext;
import com.example.feihualinggame.validator.engine.rule.AtomicRule;
import com.example.feihualinggame.validator.engine.rule.CharCountMatchRule;
import com.example.feihualinggame.validator.engine.rule.ChainStartRule;
import com.example.feihualinggame.validator.engine.rule.ConflictDetector;
import com.example.feihualinggame.validator.engine.rule.ContainsRule;
import com.example.feihualinggame.validator.engine.rule.MaxLengthRule;
import com.example.feihualinggame.validator.engine.rule.MinLengthRule;
import com.example.feihualinggame.validator.engine.rule.NoRepeatRule;
import com.example.feihualinggame.validator.engine.rule.NotContainsRule;
import com.example.feihualinggame.validator.engine.rule.PositionRule;
import com.example.feihualinggame.validator.engine.rule.RuleComposer;

import java.util.ArrayList;
import java.util.List;

public class RuleEngineConfigActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etKeyword1;
    private EditText etKeyword2;
    private LinearLayout llKeyword2Row;
    private LinearLayout llPositionRow;
    private LinearLayout llRuleToggles;
    private LinearLayout llMaxLengthRow;
    private EditText etMaxLength;
    private LinearLayout llMinLengthRow;
    private EditText etMinLength;
    private TextView tvConflictWarn;
    private TextView tvRuleSummary;
    private Button btnStart;
    private ScrollView scrollView;

    private int selectedPosition = 1;
    private int keywordCount = 1;
    private RuleComposer currentComposer;
    private RuleContext ruleContext;

    private static class RuleToggle {
        String key;
        String label;
        String dynamicLabel;
        boolean checked = true;

        RuleToggle(String key, String label) {
            this.key = key;
            this.label = label;
            this.dynamicLabel = label;
        }
    }

    private final RuleToggle[] ruleToggles = {
        new RuleToggle("noRepeat", "不能重复使用诗句"),
        new RuleToggle("contains", "必须包含关键字"),
        new RuleToggle("notContains", "不能包含关键字"),
        new RuleToggle("position", "关键字固定在指定位置"),
        new RuleToggle("chainStart", "首尾接龙飞花令（首字接上一句）"),
        new RuleToggle("maxLength", "长度限制（不超过12字）"),
        new RuleToggle("charCountMatch", "字数匹配（与上一句同字数）"),
        new RuleToggle("minLength", "最小长度（至少4字）"),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_engine_config);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.poetry_card_background));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        SystemUIUtil.hideNavigationBarIndicator(this);

        btnBack = findViewById(R.id.btn_back);
        etKeyword1 = findViewById(R.id.et_keyword1);
        etKeyword2 = findViewById(R.id.et_keyword2);
        llKeyword2Row = findViewById(R.id.ll_keyword2_row);
        llPositionRow = findViewById(R.id.ll_position_row);
        llRuleToggles = findViewById(R.id.ll_rule_toggles);
        llMaxLengthRow = findViewById(R.id.ll_max_length_row);
        etMaxLength = findViewById(R.id.et_max_length);
        llMinLengthRow = findViewById(R.id.ll_min_length_row);
        etMinLength = findViewById(R.id.et_min_length);
        tvConflictWarn = findViewById(R.id.tv_conflict_warn);
        tvRuleSummary = findViewById(R.id.tv_rule_summary);
        btnStart = findViewById(R.id.btn_start);
        scrollView = findViewById(R.id.scrollView);

        btnBack.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            finish();
        });

        ruleToggles[0].checked = true;
        ruleToggles[1].checked = true;
        ruleToggles[2].checked = false;
        ruleToggles[3].checked = false;
        ruleToggles[4].checked = false;
        ruleToggles[5].checked = false;
        ruleToggles[6].checked = false;
        ruleToggles[7].checked = true;  // minLength默认勾选

        setupKeywordCountChips();
        etMaxLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) return;
                int maxLen = parseIntSafe(val, 12);
                if (maxLen < 1) { etMaxLength.setText("1"); return; }
                if (maxLen > 99) { etMaxLength.setText("99"); return; }
                ruleToggles[5].dynamicLabel = "长度限制（不超过" + maxLen + "字）";
                buildRuleToggles();
                refreshAll();
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        etMinLength.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString().trim();
                if (val.isEmpty()) return;
                int minLen = parseIntSafe(val, 4);
                if (minLen < 1) { etMinLength.setText("1"); return; }
                if (minLen > 99) { etMinLength.setText("99"); return; }
                ruleToggles[7].dynamicLabel = "最小长度（至少" + minLen + "字）";
                buildRuleToggles();
                refreshAll();
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        buildRuleToggles();
        setupPositionChips();
        refreshAll();

        etKeyword1.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) refreshAll(); });
        etKeyword2.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) refreshAll(); });

        btnStart.setOnClickListener(v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            startCustomGame();
        });

        ButtonAnimationHelper.addEntryAnimation(scrollView, 0);
    }

    private void setupKeywordCountChips() {
        View chip1 = findViewById(R.id.chip_kw_count_1);
        View chip2 = findViewById(R.id.chip_kw_count_2);

        View.OnClickListener listener = v -> {
            AudioController.getInstance().playSound(AudioController.SOUND_BUTTON);
            if (v == chip1) {
                keywordCount = 1;
                chip1.setBackgroundResource(R.drawable.chip_filter_active);
                ((TextView) chip1).setTextColor(getResources().getColor(R.color.text_on_primary));
                chip2.setBackgroundResource(R.drawable.chip_filter);
                ((TextView) chip2).setTextColor(getResources().getColor(R.color.poetry_text_secondary));
            } else {
                keywordCount = 2;
                chip2.setBackgroundResource(R.drawable.chip_filter_active);
                ((TextView) chip2).setTextColor(getResources().getColor(R.color.text_on_primary));
                chip1.setBackgroundResource(R.drawable.chip_filter);
                ((TextView) chip1).setTextColor(getResources().getColor(R.color.poetry_text_secondary));
            }
            refreshAll();
        };

        chip1.setOnClickListener(listener);
        chip2.setOnClickListener(listener);
    }

    private void buildRuleToggles() {
        llRuleToggles.removeAllViews();

        for (int i = 0; i < ruleToggles.length; i++) {
            final RuleToggle t = ruleToggles[i];
            final int idx = i;

            // 计算互斥禁用状态
            boolean disabledTmp = false;
            String disableReason = "";
            
            // position: 当contains未勾选时禁用
            if (idx == 3 && !ruleToggles[1].checked) {
                disabledTmp = true;
                disableReason = "请先勾选「必须包含关键字」";
            }
            // chainStart: 当position=1时禁用（冲突）
            if (idx == 4 && ruleToggles[3].checked && selectedPosition == 1) {
                disabledTmp = true;
                disableReason = "与「关键字固定在第1位」冲突";
            }
            // position=1: 当chainStart勾选时禁用（冲突）
            if (idx == 3 && ruleToggles[4].checked && !disabledTmp) {
                // 允许勾选position，但在位置选择时会提示
            }
            final boolean disabled = disabledTmp;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(4, 10, 4, 10);
            row.setClipToPadding(false);
            row.setBackground(ContextCompat.getDrawable(this, R.drawable.item_click_ripple));
            row.setAlpha(disabled ? 0.4f : 1.0f);

            ImageView checkBox = new ImageView(this);
            int resId = t.checked
                ? R.drawable.ic_checkbox_checked
                : R.drawable.ic_checkbox_unchecked;
            checkBox.setImageResource(resId);
            LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(100, 100);
            checkBox.setLayoutParams(checkBoxParams);
            checkBox.setPadding(28, 28, 16, 28);
            checkBox.setScaleType(ImageView.ScaleType.FIT_CENTER);
            checkBox.setAlpha(t.checked ? 1.0f : 0.6f);
            if (disabled) {
                checkBox.setAlpha(0.3f);
            }

            TextView label = new TextView(this);
            label.setText(t.dynamicLabel);
            label.setTextSize(14);
            label.setTextColor(getResources().getColor(R.color.poetry_text_primary));

            row.addView(checkBox);
            row.addView(label);

            row.setOnClickListener(v -> {
                if (disabled) return;

                int currentCount = countCheckedRules();
                boolean willBeChecked = !ruleToggles[idx].checked;

                if (willBeChecked && currentCount >= 5) {
                    Toast.makeText(RuleEngineConfigActivity.this, "最多选择5个规则", Toast.LENGTH_SHORT).show();
                    return;
                }

                ruleToggles[idx].checked = willBeChecked;

                // contains与notContains互斥
                if (idx == 1 && ruleToggles[1].checked) {
                    ruleToggles[2].checked = false;
                }
                if (idx == 2 && ruleToggles[2].checked) {
                    ruleToggles[1].checked = false;
                    ruleToggles[3].checked = false;
                }
                if (idx == 1 && !ruleToggles[1].checked) {
                    ruleToggles[3].checked = false;
                }
                
                // chainStart与position=1互斥
                if (idx == 4 && ruleToggles[4].checked && ruleToggles[3].checked && selectedPosition == 1) {
                    // 勾选chainStart时，如果position=1，取消position
                    ruleToggles[3].checked = false;
                    Toast.makeText(RuleEngineConfigActivity.this, "已自动取消「关键字位置」（与首尾接龙冲突）", Toast.LENGTH_SHORT).show();
                }
                if (idx == 3 && ruleToggles[3].checked && ruleToggles[4].checked && selectedPosition == 1) {
                    // 勾选position=1时，如果chainStart已勾选，取消chainStart
                    ruleToggles[4].checked = false;
                    Toast.makeText(RuleEngineConfigActivity.this, "已自动取消「首尾接龙」（与第1位位置冲突）", Toast.LENGTH_SHORT).show();
                }

                if (countCheckedRules() == 0) {
                    ruleToggles[idx].checked = true;
                    Toast.makeText(RuleEngineConfigActivity.this, "至少选择1个规则", Toast.LENGTH_SHORT).show();
                }

                buildRuleToggles();
                refreshAll();
            });

            llRuleToggles.addView(row);
        }
    }

    private int countCheckedRules() {
        int count = 0;
        for (RuleToggle t : ruleToggles) {
            if (t.checked) count++;
        }
        return count;
    }

    private void updateRuleSummary() {
        StringBuilder sb = new StringBuilder();
        for (RuleToggle t : ruleToggles) {
            if (t.checked) {
                if (sb.length() > 0) sb.append(" → ");
                sb.append(t.label);
            }
        }
        if (sb.length() == 0) {
            tvRuleSummary.setText("请至少勾选一个规则");
        } else {
            tvRuleSummary.setText("当前规则链: " + sb.toString());
        }
    }

    private void setupPositionChips() {
        View[] chips = {
            findViewById(R.id.chip_pos_1),
            findViewById(R.id.chip_pos_2),
            findViewById(R.id.chip_pos_3),
            findViewById(R.id.chip_pos_4),
            findViewById(R.id.chip_pos_5),
            findViewById(R.id.chip_pos_6),
            findViewById(R.id.chip_pos_7)
        };
        int[] positions = {1, 2, 3, 4, 5, 6, 7};

        for (int i = 0; i < chips.length; i++) {
            final int pos = positions[i];
            final View chip = chips[i];
            chip.setOnClickListener(v -> {
                selectedPosition = pos;
                for (View c : chips) {
                    c.setBackgroundResource(R.drawable.chip_filter);
                    ((TextView) c).setTextColor(getResources().getColor(R.color.poetry_text_secondary));
                }
                chip.setBackgroundResource(R.drawable.chip_filter_active);
                ((TextView) chip).setTextColor(getResources().getColor(R.color.text_on_primary));
                
                // 选择位置1时，如果chainStart已勾选，自动取消
                if (pos == 1 && ruleToggles[4].checked) {
                    ruleToggles[4].checked = false;
                    Toast.makeText(RuleEngineConfigActivity.this, "已自动取消「首尾接龙」（与第1位位置冲突）", Toast.LENGTH_SHORT).show();
                    buildRuleToggles();
                }
                
                refreshAll();
            });
        }
    }

    private void refreshAll() {
        llKeyword2Row.setVisibility(keywordCount >= 2 ? View.VISIBLE : View.GONE);
        llPositionRow.setVisibility(hasRule("position") ? View.VISIBLE : View.GONE);
        llMaxLengthRow.setVisibility(hasRule("maxLength") ? View.VISIBLE : View.GONE);
        llMinLengthRow.setVisibility(hasRule("minLength") ? View.VISIBLE : View.GONE);

        if (keywordCount == 2) {
            ruleToggles[1].dynamicLabel = "必须包含双关键字（同时含(1)和(2)）";
        } else {
            ruleToggles[1].dynamicLabel = "必须包含关键字";
        }

        List<AtomicRule> rules = buildRuleList();
        if (rules.isEmpty()) {
            rules.add(new MinLengthRule(4));
            rules.add(new NoRepeatRule());
        }

        currentComposer = new RuleComposer(rules);
        ruleContext = new RuleContext("custom", getKw1())
                .setKeyword2(keywordCount >= 2 ? getKw2() : null)
                .setKeywordPosition(selectedPosition)
                .setLastChar(getKw1().isEmpty() ? null : getKw1().charAt(0));

        updateRuleSummary();
        refreshConflicts();
    }

    private int getMaxLen() {
        String val = etMaxLength.getText().toString().trim();
        return parseIntSafe(val, 12);
    }

    private int getMinLen() {
        String val = etMinLength.getText().toString().trim();
        return parseIntSafe(val, 4);
    }

    private int parseIntSafe(String val, int defaultVal) {
        try {
            int n = Integer.parseInt(val);
            if (n >= 1 && n <= 99) return n;
        } catch (NumberFormatException e) {}
        return defaultVal;
    }

    private List<AtomicRule> buildRuleList() {
        List<AtomicRule> rules = new ArrayList<>();

        for (RuleToggle t : ruleToggles) {
            if (!t.checked) continue;
            switch (t.key) {
                case "contains":
                    if (!getKw1().isEmpty()) rules.add(new ContainsRule(getKw1()));
                    if (keywordCount >= 2 && !getKw2().isEmpty()) rules.add(new ContainsRule(getKw2()));
                    break;
                case "notContains":
                    if (!getKw1().isEmpty()) rules.add(new NotContainsRule(getKw1()));
                    break;
                case "position":
                    if (!getKw1().isEmpty()) rules.add(new PositionRule(getKw1(), selectedPosition));
                    break;
                case "chainStart":
                    rules.add(new ChainStartRule());
                    break;
                case "noRepeat":
                    rules.add(new NoRepeatRule());
                    break;
                case "maxLength":
                    rules.add(new MaxLengthRule(getMaxLen()));
                    break;
                case "charCountMatch":
                    rules.add(new CharCountMatchRule());
                    break;
                case "minLength":
                    rules.add(new MinLengthRule(getMinLen()));
                    break;
            }
        }
        return rules;
    }

    private boolean hasRule(String key) {
        for (RuleToggle t : ruleToggles) {
            if (t.key.equals(key) && t.checked) return true;
        }
        return false;
    }

    private void refreshConflicts() {
        List<ConflictDetector.Conflict> conflicts = ConflictDetector.detect(currentComposer.getRules());
        StringBuilder sb = new StringBuilder();

        // 检查是否缺少关键字相关规则
        boolean hasKeywordRule = hasRule("contains") || hasRule("notContains") || hasRule("position");
        if (!hasKeywordRule) {
            sb.append("[警告] 未选择关键字规则：请至少勾选「必须包含」「不能包含」或「关键字位置」中的一个\n");
        }

        // 检查是否填写了关键字
        if (hasKeywordRule && getKw1().isEmpty()) {
            sb.append("[警告] 关键字未填写：请先输入关键字\n");
        }
        
        // 检查minLength和maxLength的关系
        if (hasRule("minLength") && hasRule("maxLength")) {
            int minLen = getMinLen();
            int maxLen = getMaxLen();
            if (minLen > maxLen) {
                sb.append("[警告] 最小长度(" + minLen + ")不能大于最大长度(" + maxLen + ")\n");
            }
        }
        
        // 检查chainStart和position=1的冲突
        if (hasRule("chainStart") && hasRule("position") && selectedPosition == 1) {
            sb.append("[冲突] 「首尾接龙」与「关键字固定在第1位」不能同时启用\n");
        }

        // 检查规则间冲突
        if (conflicts.isEmpty()) {
            tvConflictWarn.setVisibility(sb.length() > 0 ? View.VISIBLE : View.GONE);
            tvConflictWarn.setText(sb.toString());
            btnStart.setEnabled(sb.length() == 0);
            btnStart.setAlpha(sb.length() == 0 ? 1.0f : 0.5f);
        } else {
            for (ConflictDetector.Conflict c : conflicts) {
                sb.append("\n").append(c.description);
            }
            tvConflictWarn.setText(sb.toString());
            tvConflictWarn.setVisibility(View.VISIBLE);
            btnStart.setEnabled(false);
            btnStart.setAlpha(0.5f);
        }
    }

    private void startCustomGame() {
        String kw1 = getKw1();
        if (!hasRule("contains") && !hasRule("notContains") && !hasRule("position")) {
            Toast.makeText(this, "请至少选择一个关键字相关规则", Toast.LENGTH_SHORT).show();
            return;
        }
        if (kw1.isEmpty()) {
            Toast.makeText(this, "请输入关键字", Toast.LENGTH_SHORT).show();
            return;
        }
        if (keywordCount >= 2 && getKw2().isEmpty()) {
            Toast.makeText(this, "双关键字模式需要填写关键字", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hasRule("maxLength") && getMaxLen() < 4) {
            Toast.makeText(this, "最大字数不能少于4字", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, BattleActivity.class);
        intent.putExtra("gameMode", "custom");
        intent.putExtra("keyword", kw1);
        intent.putExtra("keyword2", keywordCount >= 2 ? getKw2() : null);
        intent.putExtra("keywordCount", keywordCount);
        intent.putExtra("keywordPosition", selectedPosition);
        intent.putExtra("battleType", "ai");
        intent.putExtra("timeLimit", 30);

        StringBuilder ruleKeys = new StringBuilder();
        for (RuleToggle t : ruleToggles) {
            if (t.checked) {
                if (ruleKeys.length() > 0) ruleKeys.append(",");
                ruleKeys.append(t.key);
            }
        }
        intent.putExtra("customRules", ruleKeys.toString());
        intent.putExtra("maxLength", getMaxLen());

        AudioController.getInstance().playSound(AudioController.SOUND_NAV);
        startActivity(intent);
        finish();
    }

    private String getKw1() {
        return etKeyword1.getText().toString().trim();
    }

    private String getKw2() {
        return etKeyword2.getText().toString().trim();
    }
}
