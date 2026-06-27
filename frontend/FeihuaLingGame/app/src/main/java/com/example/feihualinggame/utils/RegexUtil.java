package com.example.feihualinggame.utils;

/**
 * 正则表达式工具类
 * 用于处理正则表达式相关的操作
 */
public class RegexUtil {
    
    /**
     * 获取英文标点符号的正则表达式
     * @return 英文标点符号正则表达式
     */
    public static String getEnglishPunctuationRegex() {
        StringBuilder regex  = new StringBuilder();
        regex.append("[");
        regex.append(",.!?;:");
        regex.append("'()");
        regex.append("\\[\\]");
        regex.append("{}]");
        regex.append("+");
        return regex.toString();
    }
    
    /**
     * 获取中文标点符号的正则表达式
     * @return 中文标点符号正则表达式
     */
    public static String getChinesePunctuationRegex() {
        return "[，。！？；：、''（）《》【】…—]+";
    }
    
    /**
     * 获取空白字符的正则表达式
     * @return 空白字符正则表达式
     */
    public static String getWhitespaceRegex() {
        return "\\s+";
    }
}