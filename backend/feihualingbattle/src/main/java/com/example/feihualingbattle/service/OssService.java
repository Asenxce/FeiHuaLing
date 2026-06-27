package com.example.feihualingbattle.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.example.feihualingbattle.config.OssConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OssService {
    private static final Logger log = LoggerFactory.getLogger(OssService.class);
    
    @Autowired
    private OssConfig ossConfig;
    
    /**
     * 生成OSS上传凭证（签名URL）
     * 
     * @param userId 用户ID
     * @param fileType 文件MIME类型
     * @return 上传凭证信息
     */
    public Map<String, Object> generateUploadToken(Long userId, String fileType) {
        // 生成唯一文件名
        String extension = getFileExtension(fileType);
        String objectKey = "avatars/" + userId + "/avatar_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        
        // 构建访问URL
        String accessUrl;
        if (ossConfig.getDomain() != null && !ossConfig.getDomain().isEmpty()) {
            accessUrl = ossConfig.getDomain() + "/" + objectKey;
        } else {
            accessUrl = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectKey;
        }
        
        // 生成签名URL（有效期1小时）
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossConfig.getBucketName(), objectKey);
        request.setExpiration(expiration);
        request.setMethod(com.aliyun.oss.HttpMethod.PUT);  // 明确指定PUT方法
        request.setContentType(fileType);
        
        log.debug("OSS签名URL生成: bucket={}, objectKey={}, contentType={}",
                ossConfig.getBucketName(), objectKey, fileType);

        OSS ossClient = new OSSClientBuilder().build(ossConfig.getEndpoint(), ossConfig.getAccessKeyId(), ossConfig.getAccessKeySecret());
        URL signedUrl = ossClient.generatePresignedUrl(request);
        ossClient.shutdown();

        String uploadUrl = signedUrl.toString().replace("http://", "https://");

        log.debug("Upload URL: {}", uploadUrl);
        log.debug("Access URL: {}", accessUrl);
        
        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("uploadToken", uploadUrl);
        result.put("uploadUrl", uploadUrl);
        result.put("objectKey", objectKey);
        result.put("accessUrl", accessUrl);
        result.put("expiration", 3600);
        
        return result;
    }
    
    /**
     * 根据MIME类型获取文件扩展名
     */
    private String getFileExtension(String mimeType) {
        if (mimeType == null) {
            return ".jpg";
        }
        
        switch (mimeType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/webp":
                return ".webp";
            default:
                return ".jpg";
        }
    }
}
