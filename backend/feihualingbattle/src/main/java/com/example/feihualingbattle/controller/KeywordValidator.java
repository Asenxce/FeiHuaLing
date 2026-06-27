package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.entity.KeywordDictionary;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.repository.KeywordDictionaryRepository;
import com.example.feihualingbattle.repository.PoetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/keyword")
public class KeywordValidator {
    private static final Logger log = LoggerFactory.getLogger(KeywordValidator.class);

    @Autowired
    private KeywordDictionaryRepository keywordDictionaryRepository;

    @Autowired
    private PoetryRepository poetryRepository;

    /**
     * 验证颜色或数字飞花令的关键字是否合法
     * GET /api/keyword/check?keyword=红&type=color
     * 
     * @param keyword 关键字
     * @param type 类型：color（颜色）或 number（数字）
     * @return 验证结果
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkKeyword(
            @RequestParam String keyword,
            @RequestParam(required = false) String type) {
        try {
            log.debug("checkKeyword: keyword={}, type={}", keyword, type);
            
            Map<String, Object> response = new HashMap<>();
            
            if (keyword == null || keyword.isEmpty() || keyword.length() > 1) {
                log.warn("Invalid keyword length: {}", keyword);
                response.put("code", 400);
                response.put("message", "关键字必须为单个字符");
                response.put("data", Map.of("valid", false, "exists", false));
                return ResponseEntity.badRequest().body(response);
            }

            // 如果type为空，尝试自动判断
            if (type == null || type.isEmpty()) {
                type = "color";
            }

            // 先从关键字表查询，如果不存在则 fallback 到诗词库检查
            boolean isValid = keywordDictionaryRepository.existsByKeywordAndTypeAndIsActive(keyword, type, true);
            if (!isValid) {
                // fallback: 检查诗词库中是否有包含该关键字的诗句
                List<Poetry> poems = poetryRepository.findByKeywordInContentLimit(keyword, 1);
                isValid = !poems.isEmpty();
                log.debug("关键字表未找到，fallback诗词库检查: keyword={}, found={}", keyword, isValid);
            }
            String message = "";

            if ("color".equalsIgnoreCase(type)) {
                message = isValid ? "合法的颜色关键字" : "不合法的颜色关键字，请选择标准颜色字";
            } else if ("number".equalsIgnoreCase(type)) {
                message = isValid ? "合法的数字关键字" : "不合法的数字关键字，请选择标准数字字";
            } else {
                log.warn("Invalid type: {}", type);
                response.put("code", 400);
                response.put("message", "类型参数错误，必须是color或number");
                response.put("data", Map.of("valid", false, "exists", false));
                return ResponseEntity.badRequest().body(response);
            }

            log.debug("Validation result: valid={}", isValid);
            response.put("code", 200);
            response.put("message", message);
            // 同时返回 valid 和 exists 字段以兼容前后端
            response.put("data", Map.of("valid", isValid, "exists", isValid, "keyword", keyword, "type", type));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in checkKeyword: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/colors")
    public ResponseEntity<Map<String, Object>> getAllColors() {
        try {
            List<KeywordDictionary> keywords = keywordDictionaryRepository.findAllByTypeAndIsActiveOrderBySortOrder("color", true);
            List<String> colorList = keywords.stream()
                    .map(KeywordDictionary::getKeyword)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取颜色关键字列表");
            response.put("data", colorList);
            response.put("count", colorList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getAllColors: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "服务器内部错误");
            errorResponse.put("data", Collections.emptyList());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取所有可用的数字字列表
     * GET /api/keyword/numbers
     */
    @GetMapping("/numbers")
    public ResponseEntity<Map<String, Object>> getAllNumbers() {
        try {
            List<KeywordDictionary> keywords = keywordDictionaryRepository.findAllByTypeAndIsActiveOrderBySortOrder("number", true);
            List<String> numberList = keywords.stream()
                    .map(KeywordDictionary::getKeyword)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取数字关键字列表");
            response.put("data", numberList);
            response.put("count", numberList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getAllNumbers: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "服务器内部错误");
            errorResponse.put("data", Collections.emptyList());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取关键字的详细信息(包括分类、描述等)
     * GET /api/keyword/detail?keyword=红&type=color
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getKeywordDetail(
            @RequestParam String keyword,
            @RequestParam String type) {
        try {
            Optional<KeywordDictionary> keywordOpt = 
                keywordDictionaryRepository.findByKeywordAndTypeAndIsActive(keyword, type, true);
            
            Map<String, Object> response = new HashMap<>();
            
            if (keywordOpt.isPresent()) {
                KeywordDictionary kd = keywordOpt.get();
                response.put("code", 200);
                response.put("message", "成功获取关键字详情");
                
                Map<String, Object> data = new HashMap<>();
                data.put("keyword", kd.getKeyword());
                data.put("type", kd.getType());
                data.put("category", kd.getCategory());
                data.put("description", kd.getDescription());
                data.put("usageCount", kd.getUsageCount());
                data.put("sortOrder", kd.getSortOrder());
                
                response.put("data", data);
                return ResponseEntity.ok(response);
            } else {
                response.put("code", 404);
                response.put("message", "关键字不存在或已禁用");
                response.put("data", null);
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            log.error("Exception in getKeywordDetail: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "服务器内部错误");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 按分类获取关键字
     * GET /api/keyword/by-category?category=基本色&type=color
     */
    @GetMapping("/by-category")
    public ResponseEntity<Map<String, Object>> getByCategory(
            @RequestParam String category,
            @RequestParam String type) {
        try {
            List<KeywordDictionary> keywords = 
                keywordDictionaryRepository.findByCategoryAndTypeOrderBySortOrder(category, type);
            
            List<Map<String, Object>> dataList = keywords.stream().map(kd -> {
                Map<String, Object> item = new HashMap<>();
                item.put("keyword", kd.getKeyword());
                item.put("category", kd.getCategory());
                item.put("description", kd.getDescription());
                item.put("sortOrder", kd.getSortOrder());
                return item;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "成功获取分类关键字");
            response.put("data", dataList);
            response.put("count", dataList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception in getByCategory: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "服务器内部错误");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
