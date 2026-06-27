package com.example.feihualingbattle.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.example.feihualingbattle.config.OssConfig;
import com.example.feihualingbattle.dto.*;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OssConfig ossConfig;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword(), request.getNickname());
            return ApiResponse.success("注册成功", user);
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());

            String sessionId = java.util.UUID.randomUUID().toString();
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), sessionId);

            String sessionKey = "user:session:" + user.getId();
            String oldSessionId = redisTemplate.opsForValue().get(sessionKey);
            if (oldSessionId != null) {
                log.info("顶号操作: userId={}, 旧session被踢下线", user.getId());
            }
            redisTemplate.opsForValue().set(sessionKey, sessionId, 7, TimeUnit.DAYS);

            String onlineKey = "online:user:" + user.getId();
            redisTemplate.opsForValue().set(onlineKey, "1", 10, TimeUnit.MINUTES);
            log.info("用户上线: userId={}, username={}", user.getId(), user.getUsername());

            LoginResponse response = new LoginResponse();
            response.setUsername(user.getUsername());
            response.setIdentityCode(user.getIdentityCode());
            response.setToken(token);
            response.setSessionId(sessionId);

            log.info("登录成功: userId={}, username={}", user.getId(), user.getUsername());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return ApiResponse.error(401, e.getMessage());
        }
    }

    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> getUserInfo(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(request);
            User user = userService.loadUserById(userId);

            UserInfoResponse response = buildUserInfoResponse(user);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "服务器内部错误，请稍后重试");
        }
    }

    @PostMapping("/update")
    public ApiResponse<UserInfoResponse> updateProfile(@RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            User updatedUser = userService.updateUser(userId, request.getNickname(), request.getAvatarUrl(),
                    request.getEmail(), request.getPhone(), request.getBio());

            UserInfoResponse response = buildUserInfoResponse(updatedUser);
            return ApiResponse.success("保存成功", response);
        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("更新用户资料失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "保存失败：" + e.getMessage());
        }
    }

    @PostMapping("/avatar")
    public ApiResponse<UserInfoResponse> updateAvatar(@RequestBody UpdateAvatarRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            String avatarUrl = request.getAvatarUrl();
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                return ApiResponse.error(400, "头像URL不能为空");
            }

            if (!avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
                return ApiResponse.error(400, "头像URL格式错误");
            }

            User updatedUser = userService.updateUser(userId, null, avatarUrl, null, null, null);
            UserInfoResponse response = buildUserInfoResponse(updatedUser);
            return ApiResponse.success("头像更新成功", response);
        } catch (Exception e) {
            log.error("更新头像失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "头像更新失败：" + e.getMessage());
        }
    }

    @GetMapping("/avatar/{userId}")
    public void getAvatar(@PathVariable Long userId, HttpServletResponse response) {
        OSS ossClient = null;
        InputStream inputStream = null;

        try {
            log.debug("头像代理请求: userId={}", userId);

            User user = userService.loadUserById(userId);
            String avatarUrl = user.getAvatarUrl();

            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String objectKey = extractObjectKey(avatarUrl);
            if (objectKey == null || objectKey.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret()
            );

            OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), objectKey);
            inputStream = ossObject.getObjectContent();

            response.setContentType("image/jpeg");
            response.setHeader("Cache-Control", "public, max-age=86400");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }

        } catch (Exception e) {
            log.error("获取头像失败: userId={}, error={}", userId, e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("头像加载失败: " + e.getMessage());
            } catch (Exception ex) {
                log.error("写入错误响应失败: {}", ex.getMessage());
            }
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            } catch (Exception e) {
                log.error("关闭资源失败: {}", e.getMessage());
            }
        }
    }

    private String extractObjectKey(String ossUrl) {
        try {
            String baseUrl;
            if (ossConfig.getDomain() != null && !ossConfig.getDomain().isEmpty()) {
                baseUrl = ossConfig.getDomain();
            } else {
                baseUrl = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint();
            }

            if (ossUrl.startsWith(baseUrl + "/")) {
                return ossUrl.substring(baseUrl.length() + 1);
            }

            URL url = new URL(ossUrl);
            String path = url.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (Exception e) {
            log.error("提取objectKey失败: {}", e.getMessage());
            return null;
        }
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Map<String, Object>> heartbeat(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(request);

            String onlineKey = "online:user:" + userId;
            redisTemplate.opsForValue().set(onlineKey, "1", 10, TimeUnit.MINUTES);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "心跳成功");

            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("心跳失败: {}", e.getMessage());
            return ApiResponse.error(500, "心跳失败");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getCurrentUserId(request);

            String onlineKey = "online:user:" + userId;
            redisTemplate.delete(onlineKey);
            String sessionKey = "user:session:" + userId;
            redisTemplate.delete(sessionKey);
            log.info("用户下线: userId={}", userId);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "退出成功");

            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("退出登录失败: {}", e.getMessage());
            return ApiResponse.error(500, "退出失败");
        }
    }

    @GetMapping("/hello")
    public String hello() {
        return "飞花令对战系统后端运行正常！";
    }

    private UserInfoResponse buildUserInfoResponse(User user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setIdentityCode(user.getIdentityCode());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setBio(user.getBio());
        response.setTotalScore(user.getTotalScore());
        response.setWinCount(user.getWinCount());
        response.setLoseCount(user.getLoseCount());
        response.setDrawCount(user.getDrawCount());
        return response;
    }
}
