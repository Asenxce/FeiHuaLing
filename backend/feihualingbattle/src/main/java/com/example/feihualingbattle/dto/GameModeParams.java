package com.example.feihualingbattle.dto;

import lombok.Data;

/**
 * 游戏模式参数 - 用于存储模式特定的参数
 */
@Data
public class GameModeParams {
    /**
     * 第二关键字(双字模式)
     */
    private String keyword2;

    /**
     * 关键字位置要求(位置模式)
     */
    private Integer keywordPosition;

    /**
     * 颜色关键字(颜色模式)
     */
    private String colorKeyword;

    /**
     * 数字关键字(数字模式)
     */
    private String numberKeyword;

    /**
     * 禁止词(反飞花令)
     */
    private String forbiddenWord;

    /**
     * 创建默认参数对象
     */
    public static GameModeParams create(String keyword2, Integer keywordPosition, 
                                         String colorKeyword, String numberKeyword, 
                                         String forbiddenWord) {
        GameModeParams params = new GameModeParams();
        params.setKeyword2(keyword2);
        params.setKeywordPosition(keywordPosition);
        params.setColorKeyword(colorKeyword);
        params.setNumberKeyword(numberKeyword);
        params.setForbiddenWord(forbiddenWord);
        return params;
    }

    /**
     * 检查是否有任何非空参数
     */
    public boolean hasAnyParam() {
        return keyword2 != null || keywordPosition != null || 
               colorKeyword != null || numberKeyword != null || forbiddenWord != null;
    }
}
