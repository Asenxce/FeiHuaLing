package com.example.feihualingbattle.service.strategy;

/**
 * 规则引擎上下文
 * 统一封装所有模式所需参数，避免方法签名膨胀
 */
public class GameModeContext {
    private final String gameMode;
    private final String keyword;
    private final String keyword2;
    private final String colorKeyword;
    private final String numberKeyword;
    private final String forbiddenWord;
    private final Integer keywordPosition;
    private final String lastChar;
    private final String answer;

    private GameModeContext(Builder builder) {
        this.gameMode = builder.gameMode;
        this.keyword = builder.keyword;
        this.keyword2 = builder.keyword2;
        this.colorKeyword = builder.colorKeyword;
        this.numberKeyword = builder.numberKeyword;
        this.forbiddenWord = builder.forbiddenWord;
        this.keywordPosition = builder.keywordPosition;
        this.lastChar = builder.lastChar;
        this.answer = builder.answer;
    }

    public String getGameMode() { return gameMode; }
    public String getKeyword() { return keyword; }
    public String getKeyword2() { return keyword2; }
    public String getColorKeyword() { return colorKeyword; }
    public String getNumberKeyword() { return numberKeyword; }
    public String getForbiddenWord() { return forbiddenWord; }
    public Integer getKeywordPosition() { return keywordPosition; }
    public String getLastChar() { return lastChar; }
    public String getAnswer() { return answer; }

    public static class Builder {
        private String gameMode;
        private String keyword;
        private String keyword2;
        private String colorKeyword;
        private String numberKeyword;
        private String forbiddenWord;
        private Integer keywordPosition;
        private String lastChar;
        private String answer;

        public Builder gameMode(String gameMode) { this.gameMode = gameMode; return this; }
        public Builder keyword(String keyword) { this.keyword = keyword; return this; }
        public Builder keyword2(String keyword2) { this.keyword2 = keyword2; return this; }
        public Builder colorKeyword(String colorKeyword) { this.colorKeyword = colorKeyword; return this; }
        public Builder numberKeyword(String numberKeyword) { this.numberKeyword = numberKeyword; return this; }
        public Builder forbiddenWord(String forbiddenWord) { this.forbiddenWord = forbiddenWord; return this; }
        public Builder keywordPosition(Integer keywordPosition) { this.keywordPosition = keywordPosition; return this; }
        public Builder lastChar(String lastChar) { this.lastChar = lastChar; return this; }
        public Builder answer(String answer) { this.answer = answer; return this; }

        public GameModeContext build() {
            return new GameModeContext(this);
        }
    }
}
