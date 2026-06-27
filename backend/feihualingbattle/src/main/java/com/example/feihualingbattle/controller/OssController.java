package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.dto.UploadTokenRequest;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.OssService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/oss")
public class OssController {
    private static final Logger log = LoggerFactory.getLogger(OssController.class);

    @Autowired
    private OssService ossService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload-token")
    public ApiResponse<Map<String, Object>> getUploadToken(
            @RequestBody UploadTokenRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            String fileType = request.getFileType();
            if (fileType == null || !isValidImageType(fileType)) {
                return ApiResponse.error(400, "不支持的文件类型，仅支持jpg、png、webp格式");
            }

            Map<String, Object> uploadInfo = ossService.generateUploadToken(userId, fileType);
            return ApiResponse.success(uploadInfo);
        } catch (Exception e) {
            log.error("获取上传凭证失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "获取上传凭证失败：" + e.getMessage());
        }
    }

    private boolean isValidImageType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return mimeType.equals("image/jpeg")
            || mimeType.equals("image/jpg")
            || mimeType.equals("image/png")
            || mimeType.equals("image/webp");
    }
}
