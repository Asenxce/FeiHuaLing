package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.dto.AddCollectionRequest;
import com.example.feihualingbattle.dto.CollectionDTO;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.CollectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collection")
public class CollectionController {
    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/list")
    public ApiResponse<List<CollectionDTO>> getCollectionList(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(request);
            List<CollectionDTO> collections = collectionService.getUserCollections(userId);
            return ApiResponse.success(collections);
        } catch (Exception e) {
            log.error("获取收藏列表失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "服务器内部错误，请稍后重试");
        }
    }

    @PostMapping("/add")
    public ApiResponse<String> addCollection(@RequestBody AddCollectionRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            boolean success = collectionService.addCollection(userId, request.getPoetryId());

            if (success) {
                return ApiResponse.success("收藏成功");
            } else {
                return ApiResponse.error(400, "已收藏");
            }
        } catch (Exception e) {
            log.error("添加收藏失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "收藏失败：" + e.getMessage());
        }
    }

    @PostMapping("/remove")
    public ApiResponse<String> removeCollection(@RequestParam Long poetryId, HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(request);
            collectionService.removeCollection(userId, poetryId);
            return ApiResponse.success("取消收藏成功");
        } catch (Exception e) {
            log.error("取消收藏失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "取消收藏失败：" + e.getMessage());
        }
    }
}
