package com.example.feihualinggame.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 诗词清洗工具类
 * 用于将诗句统一格式化为简体无标点格式
 */
public class PoetryCleanUtil {

    private static final Pattern CHINESE_PUNCTUATION = Pattern.compile(RegexUtil.getChinesePunctuationRegex());
    private static final Pattern ENGLISH_PUNCTUATION = Pattern.compile(RegexUtil.getEnglishPunctuationRegex());
    private static final Pattern WHITESPACE = Pattern.compile(RegexUtil.getWhitespaceRegex());

    public static String cleanPoetry(String poetry) {
        if (poetry == null || poetry.isEmpty()) {
            return "";
        }
        String result = CHINESE_PUNCTUATION.matcher(poetry).replaceAll("");
        result = ENGLISH_PUNCTUATION.matcher(result).replaceAll("");
        result = WHITESPACE.matcher(result).replaceAll("");
        result = toSimplified(result);
        return result;
    }

    public static String toSimplified(String traditional) {
        if (traditional == null) {
            return null;
        }
        return convertTraditional(traditional);
    }

    private static final Map<Character, Character> T2S_MAP = new HashMap<>();

    static {
        char[][] pairs = {
            {'\u9EBD','\u4E48'}, {'\u7232','\u4E3A'}, {'\u6703','\u4F1A'}, {'\u500B','\u4E2A'}, {'\u6642','\u65F6'},
            {'\u570B','\u56FD'}, {'\u4F86','\u6765'}, {'\u8AAA','\u8BF4'}, {'\u904E','\u8FC7'}, {'\u9084','\u8FD8'},
            {'\u9019','\u8FD9'}, {'\u6A23','\u6837'}, {'\u8457','\u7740'}, {'\u9577','\u957F'}, {'\u898B','\u89C1'},
            {'\u5F8C','\u540E'}, {'\u982D','\u5934'}, {'\u66F8','\u4E66'}, {'\u5152','\u513F'}, {'\u5C0D','\u5BF9'},
            {'\u958B','\u5F00'}, {'\u95DC','\u5173'}, {'\u9580','\u95E8'}, {'\u842C','\u4E07'}, {'\u7121','\u65E0'},
            {'\u6771','\u4E1C'}, {'\u5B78','\u5B66'}, {'\u6C23','\u6C14'}, {'\u611B','\u7231'}, {'\u8655','\u5904'},
            {'\u96F2','\u4E91'}, {'\u98A8','\u98CE'}, {'\u98DB','\u98DE'}, {'\u99AC','\u9A6C'}, {'\u9B5A','\u9C7C'},
            {'\u9CE5','\u9E1F'}, {'\u9F8D','\u9F99'}, {'\u554F','\u95EE'}, {'\u9593','\u95F4'}, {'\u88CF','\u91CC'},
            {'\u8863','\u88E4'}, {'\u885B','\u536B'}, {'\u96BB','\u53EA'}, {'\u5F9E','\u4ECE'}, {'\u8B8A','\u53D8'},
            {'\u52D5','\u52A8'}, {'\u83EF','\u534E'}, {'\u9054','\u8FBE'}, {'\u9032','\u8FDB'}, {'\u7D93','\u7ECF'},
            {'\u9AD4','\u4F53'}, {'\u6A02','\u4E50'}, {'\u767C','\u53D1'}, {'\u696D','\u4E1A'}, {'\u7BC0','\u8282'},
            {'\u89BA','\u89C9'}, {'\u96E3','\u96BE'}, {'\u8072','\u58F0'}, {'\u5BE6','\u5B9E'}, {'\u7576','\u5F53'},
            {'\u61C9','\u5E94'}, {'\u52DE','\u52B3'}, {'\u7AAE','\u7A77'}, {'\u820A','\u65E7'}, {'\u89AA','\u4EB2'},
            {'\u9858','\u613F'}, {'\u6A5F','\u673A'}, {'\u6EFF','\u6EE1'}, {'\u79AE','\u793C'}, {'\u8B58','\u8BC6'},
            {'\u9322','\u94B1'}, {'\u76E1','\u5C3D'}, {'\u8F15','\u8F7B'}, {'\u8B93','\u8BA9'}, {'\u8CB7','\u4E70'},
            {'\u8CE3','\u5356'}, {'\u8208','\u5174'}, {'\u9060','\u8FDC'}, {'\u6A39','\u6811'}, {'\u6B72','\u5C81'},
            {'\u6230','\u6218'}, {'\u7A31','\u79F0'}, {'\u8A08','\u8BA1'}, {'\u8A31','\u8BB8'}, {'\u8A69','\u8BD7'},
            {'\u8A5E','\u8BCD'}, {'\u8A9E','\u8BED'}, {'\u8ACB','\u8BF7'}, {'\u8AB0','\u8C01'}, {'\u865F','\u53F7'},
            {'\u96E2','\u79BB'}, {'\u96D9','\u53CC'}, {'\u7368','\u72EC'}, {'\u9DB4','\u9E64'}, {'\u9DD7','\u9E25'},
            {'\u9DFA','\u9E6D'}, {'\u9DF9','\u9E70'}, {'\u9CF3','\u51E4'}, {'\u52DD','\u80DC'}, {'\u5C07','\u5C06'},
            {'\u723E','\u5C14'}, {'\u61B6','\u5FC6'}, {'\u61F7','\u6000'}, {'\u61FC','\u60E7'}, {'\u50B7','\u4F24'},
            {'\u6182','\u5FE7'}, {'\u6190','\u601C'}, {'\u5922','\u68A6'}, {'\u7159','\u70DF'}, {'\u71C8','\u706F'},
            {'\u71D2','\u70E7'}, {'\u6BBA','\u6740'}, {'\u528D','\u5251'}, {'\u9280','\u94F6'}, {'\u9435','\u94C1'},
            {'\u9285','\u94DC'}, {'\u932F','\u9519'}, {'\u9418','\u949F'}, {'\u93E1','\u955C'}, {'\u9326','\u9526'},
            {'\u7DBA','\u7EE6'}, {'\u7DA0','\u7EFF'}, {'\u7D05','\u7EA2'}, {'\u9EC3','\u9EC4'}, {'\u85CD','\u84DD'},
            {'\u862D','\u5170'}, {'\u8449','\u53F6'}, {'\u967D','\u9633'}, {'\u9670','\u9634'}, {'\u96B1','\u9690'},
            {'\u96A8','\u968F'}, {'\u9928','\u9986'}, {'\u98EF','\u996D'}, {'\u98F2','\u996E'}, {'\u7F85','\u7F57'},
            {'\u7D61','\u7EE1'}, {'\u7D72','\u4E1D'}, {'\u7DDA','\u7EBF'}, {'\u7D55','\u7EDD'}, {'\u7D50','\u7ED3'},
            {'\u7D66','\u7ED9'}, {'\u7D0D','\u7EB3'}, {'\u7D30','\u7EC6'}, {'\u7D44','\u7EC4'}, {'\u5E7E','\u51E0'},
            {'\u7FA9','\u4E49'}, {'\u8207','\u4E0E'}, {'\u8209','\u4E3E'}, {'\u8F3F','\u8206'}, {'\u9E97','\u4E3D'},
            {'\u56B4','\u4E25'}, {'\u5EE3','\u5E7F'}, {'\u6F14','\u6F14'}, {'\u6B77','\u5386'}, {'\u7E6B','\u7CFB'},
        };
        for (char[] pair : pairs) {
            T2S_MAP.put(pair[0], pair[1]);
        }
    }

    private static String convertTraditional(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Character simplified = T2S_MAP.get(c);
            sb.append(simplified != null ? simplified : c);
        }
        return sb.toString();
    }

    public static boolean containsKeyword(String poetry, String keyword) {
        if (poetry == null || keyword == null || keyword.isEmpty()) {
            return false;
        }
        return cleanPoetry(poetry).contains(cleanPoetry(keyword));
    }

    public static boolean containsBothKeywords(String poetry, String keyword1, String keyword2) {
        return containsKeyword(poetry, keyword1) && containsKeyword(poetry, keyword2);
    }

    public static boolean isKeywordAtPosition(String poetry, String keyword, int position) {
        if (poetry == null || keyword == null || position < 1) {
            return false;
        }
        String cleanedPoetry = cleanPoetry(poetry);
        String cleanedKeyword = cleanPoetry(keyword);
        if (position > cleanedPoetry.length()) {
            return false;
        }
        if (position + cleanedKeyword.length() - 1 > cleanedPoetry.length()) {
            return false;
        }
        return cleanedPoetry.substring(position - 1, position + cleanedKeyword.length() - 1)
                .equals(cleanedKeyword);
    }

    public static boolean startsWith(String poetry, String character) {
        if (poetry == null || character == null || character.isEmpty()) {
            return false;
        }
        return cleanPoetry(poetry).startsWith(cleanPoetry(character));
    }

    public static boolean endsWith(String poetry, String character) {
        if (poetry == null || character == null || character.isEmpty()) {
            return false;
        }
        return cleanPoetry(poetry).endsWith(cleanPoetry(character));
    }

    public static boolean containsColor(String poetry, String color) {
        return containsKeyword(poetry, color);
    }

    public static boolean containsNumber(String poetry, String number) {
        return containsKeyword(poetry, number);
    }

    public static boolean containsForbiddenWord(String poetry, String forbiddenWord) {
        return containsKeyword(poetry, forbiddenWord);
    }

    public static boolean hasRepeatedCharacters(String poetry) {
        String cleaned = cleanPoetry(poetry);
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (cleaned.indexOf(c) != cleaned.lastIndexOf(c)) {
                return true;
            }
        }
        return false;
    }

    public static Character extractFirstChar(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        String cleaned = cleanPoetry(content);
        return cleaned.isEmpty() ? null : cleaned.charAt(0);
    }

    public static Character extractLastChar(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        String cleaned = cleanPoetry(content);
        return cleaned.isEmpty() ? null : cleaned.charAt(cleaned.length() - 1);
    }
}
