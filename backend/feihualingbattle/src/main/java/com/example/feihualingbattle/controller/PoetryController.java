package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.dto.PoetryLineResponse;
import com.example.feihualingbattle.dto.PoetryMasterResponse;
import com.example.feihualingbattle.entity.Poetry;
import com.example.feihualingbattle.entity.PoetryMaster;
import com.example.feihualingbattle.service.PoetryService;
import com.example.feihualingbattle.service.PoetryMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 诗词查询控制器
 * 优化：统一智能搜索接口、缓存、分页、筛选
 */
@RestController
@RequestMapping("/api/poetry")
public class PoetryController {
    private static final Logger log = LoggerFactory.getLogger(PoetryController.class);

    @Autowired
    private PoetryService poetryService;

    @Autowired
    private PoetryMasterService poetryMasterService;

    /**
     * 统一智能搜索接口（带缓存）
     * 后端返回按相关度排序的结果：标题匹配 > 作者匹配 > 内容匹配
     */
    @GetMapping("/search")
    @Cacheable(value = "poetryMaster", key = "'smart_search:' + #keyword + ':' + #page + ':' + #size")
    public ApiResponse<List<PoetryMasterResponse>> smartSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.debug("智能搜索请求 - keyword: {}, page: {}, size: {}", keyword, page, size);
            
            Page<PoetryMaster> pageResult = poetryMasterService.searchByKeyword(keyword, page, size);
            
            List<PoetryMasterResponse> response = pageResult.getContent().stream()
                    .map(PoetryMasterResponse::fromEntity)
                    .collect(Collectors.toList());
            
            log.debug("智能搜索返回 {} 条结果", response.size());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("智能搜索异常: {}", e.getMessage(), e);
            return ApiResponse.error(500, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 搜索诗句行（支持分页、朝代筛选、类型筛选）
     */
    @GetMapping("/search-line")
    public List<PoetryMasterResponse> searchPoetryLine(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String keyword2,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String dynasty,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PoetryMaster> pageResult;

        if (keyword != null && !keyword.isEmpty()) {
            if (dynasty != null && !dynasty.isEmpty()) {
                pageResult = poetryMasterService.searchByKeywordAndDynasty(keyword, dynasty, page, size);
            } else if (type != null && !type.isEmpty()) {
                pageResult = poetryMasterService.searchByKeywordAndType(keyword, type, page, size);
            } else if (keyword2 != null && !keyword2.isEmpty()) {
                pageResult = poetryMasterService.searchByTwoKeywords(keyword, keyword2, page, size);
            } else {
                pageResult = poetryMasterService.searchByKeyword(keyword, page, size);
            }
        } else if (author != null && !author.isEmpty()) {
            pageResult = poetryMasterService.findByAuthorContainingWithPage(author, page, size);
        } else if (title != null && !title.isEmpty()) {
            pageResult = poetryMasterService.findByTitleContainingWithPage(title, page, size);
        } else {
            return List.of();
        }

        return pageResult.getContent().stream()
                .map(PoetryMasterResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/search-by-position")
    public List<PoetryLineResponse> searchByKeywordAtPosition(
            @RequestParam String keyword,
            @RequestParam int position) {
        return poetryService.searchByKeywordAtPosition(keyword, position).stream()
                .map(PoetryLineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/search-two-keywords")
    public List<PoetryLineResponse> searchByTwoKeywords(
            @RequestParam String keyword1,
            @RequestParam String keyword2) {
        return poetryService.searchByTwoKeywords(keyword1, keyword2).stream()
                .map(PoetryLineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validatePoetry(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ApiResponse.error(400, "诗句内容不能为空");
        }

        String normalizedContent = content.replaceAll("[\\p{P}\\s]", "").trim();
        List<Poetry> matchingPoems = poetryService.searchByKeyword(normalizedContent);

        boolean exists = matchingPoems.stream()
            .anyMatch(p -> p.getContent().equals(normalizedContent));

        return ApiResponse.success(Map.of("exists", exists, "content", normalizedContent));
    }

    @GetMapping("/random")
    public ApiResponse<PoetryMasterResponse> getRandomPoetry() {
        try {
            log.debug("随机诗句请求");
            PoetryMaster master = poetryMasterService.getRandomPoetry();
            if (master == null) {
                return ApiResponse.error(404, "暂无诗词数据");
            }
            PoetryMasterResponse data = PoetryMasterResponse.fromEntity(master);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("随机诗句查询异常: {}", e.getMessage(), e);
            return ApiResponse.error(500, "服务器错误");
        }
    }
}
