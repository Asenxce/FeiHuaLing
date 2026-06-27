package com.example.feihualingbattle.service.strategy.rule;

import com.example.feihualingbattle.repository.PoetryRepository;

/**
 * 诗词查询上下文 —— 传递给原子规则的参数载体
 */
public class QueryContext {

    private final String gameMode;
    private final String keyword;
    private final String keyword2;
    private final Integer keywordPosition;
    private final String lastChar;
    private final String answer;
    private final PoetryRepository repository;
    private final BattleRuleHelper helper;

    private QueryContext(Builder builder) {
        this.gameMode = builder.gameMode;
        this.keyword = builder.keyword;
        this.keyword2 = builder.keyword2;
        this.keywordPosition = builder.keywordPosition;
        this.lastChar = builder.lastChar;
        this.answer = builder.answer;
        this.repository = builder.repository;
        this.helper = builder.helper;
    }

    public String getGameMode() { return gameMode; }
    public String getKeyword() { return keyword; }
    public String getKeyword2() { return keyword2; }
    public Integer getKeywordPosition() { return keywordPosition; }
    public String getLastChar() { return lastChar; }
    public String getAnswer() { return answer; }
    public PoetryRepository getRepository() { return repository; }
    public BattleRuleHelper getHelper() { return helper; }

    public static class Builder {
        private String gameMode, keyword, keyword2, lastChar, answer;
        private Integer keywordPosition;
        private PoetryRepository repository;
        private BattleRuleHelper helper;

        public Builder gameMode(String v) { this.gameMode = v; return this; }
        public Builder keyword(String v) { this.keyword = v; return this; }
        public Builder keyword2(String v) { this.keyword2 = v; return this; }
        public Builder keywordPosition(Integer v) { this.keywordPosition = v; return this; }
        public Builder lastChar(String v) { this.lastChar = v; return this; }
        public Builder answer(String v) { this.answer = v; return this; }
        public Builder repository(PoetryRepository v) { this.repository = v; return this; }
        public Builder helper(BattleRuleHelper v) { this.helper = v; return this; }
        public QueryContext build() { return new QueryContext(this); }
    }
}
