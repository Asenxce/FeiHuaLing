package com.example.feihualinggame.validator.engine;

/**
 * 规则上下文 —— 统一封装所有验证所需的参数
 * 作为规则引擎的数据载体，在管道节点间流转
 */
public class RuleContext {
    private String gameMode;
    private String keyword;
    private String keyword2;
    private int keywordPosition;
    private Character lastChar;
    private String answer;
    private String ruleHint;
    private int lastAnswerLength;

    public RuleContext() {}

    public RuleContext(String gameMode, String keyword) {
        this.gameMode = gameMode;
        this.keyword = keyword;
    }

    public String getGameMode() { return gameMode; }
    public RuleContext setGameMode(String gameMode) { this.gameMode = gameMode; return this; }

    public String getKeyword() { return keyword; }
    public RuleContext setKeyword(String keyword) { this.keyword = keyword; return this; }

    public String getKeyword2() { return keyword2; }
    public RuleContext setKeyword2(String keyword2) { this.keyword2 = keyword2; return this; }

    public int getKeywordPosition() { return keywordPosition; }
    public RuleContext setKeywordPosition(int keywordPosition) { this.keywordPosition = keywordPosition; return this; }

    public Character getLastChar() { return lastChar; }
    public RuleContext setLastChar(Character lastChar) { this.lastChar = lastChar; return this; }

    public String getAnswer() { return answer; }
    public RuleContext setAnswer(String answer) { this.answer = answer; return this; }

    public String getRuleHint() { return ruleHint; }
    public RuleContext setRuleHint(String ruleHint) { this.ruleHint = ruleHint; return this; }

    public int getLastAnswerLength() { return lastAnswerLength; }
    public RuleContext setLastAnswerLength(int lastAnswerLength) { this.lastAnswerLength = lastAnswerLength; return this; }
}
