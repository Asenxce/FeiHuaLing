package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.dto.AddFriendRequest;
import com.example.feihualingbattle.entity.Friendship;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.FriendshipService;
import com.example.feihualingbattle.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friend")
public class FriendshipController {
    private static final Logger log = LoggerFactory.getLogger(FriendshipController.class);

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/add")
    public ApiResponse<Map<String, Object>> addFriend(@RequestBody AddFriendRequest request, HttpServletRequest httpRequest) {
        Map<String, Object> data = new HashMap<>();
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            log.info("添加好友请求: userId={}, targetIdentityCode={}",
                    userId, request.getTargetIdentityCode());

            Friendship friendship = friendshipService.sendFriendRequest(
                userId,
                request.getTargetIdentityCode()
            );

            log.info("好友申请已发送: id={}, status={}", friendship.getId(), friendship.getStatus());

            data.put("success", true);
            data.put("message", "好友申请已发送");
            data.put("friendship", friendship);

            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("添加好友失败: {}", e.getMessage(), e);
            data.put("success", false);
            data.put("message", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取待处理的好友申请列表
     */
    @GetMapping("/pending-requests")
    public ApiResponse<List<Map<String, Object>>> getPendingRequests(HttpServletRequest request) {
        try {
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            
            List<Friendship> pendingRequests = friendshipService.getPendingRequests(currentUserId);
            
            List<Long> requesterIds = pendingRequests.stream()
                .map(Friendship::getUserId)
                .distinct()
                .collect(Collectors.toList());
            Map<Long, User> userMap = userService.loadUsersByIds(requesterIds);
            
            List<Map<String, Object>> result = pendingRequests.stream().map(f -> {
                Map<String, Object> map = new HashMap<>();
                User requester = userMap.get(f.getUserId());
                map.put("requestId", f.getId());
                map.put("requesterId", f.getUserId());
                map.put("username", requester != null ? requester.getUsername() : "");
                map.put("nickname", requester != null ? requester.getNickname() : "");
                map.put("avatarUrl", requester != null ? requester.getAvatarUrl() : "");
                map.put("status", f.getStatus());
                return map;
            }).collect(Collectors.toList());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取待处理申请失败: {}", e.getMessage(), e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/send-request")
    public ResponseEntity<Map<String, Object>> sendRequest(
            @RequestParam String friendIdentityCode,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);
            Friendship friendship = friendshipService.sendFriendRequest(userId, friendIdentityCode);

            response.put("success", true);
            response.put("message", "好友申请已发送");
            response.put("data", friendship);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送好友请求失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/accept")
    public ApiResponse<Map<String, Object>> acceptRequest(
            @RequestParam Long friendId,
            HttpServletRequest request) {
        try {
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            Friendship friendship = friendshipService.acceptFriendRequest(currentUserId, friendId);

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "已接受好友请求");
            data.put("data", friendship);
            return ApiResponse.success(data);
        } catch (Exception e) {
            log.error("接受好友请求失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/reject")
    public ApiResponse<Void> rejectRequest(
            @RequestParam Long friendId,
            HttpServletRequest request) {
        try {
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            friendshipService.rejectFriendRequest(currentUserId, friendId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("拒绝好友请求失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeFriend(
            @RequestParam Long userId,
            @RequestParam Long friendId,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long currentUserId = jwtUtil.getCurrentUserId(request);
            if (!currentUserId.equals(userId)) {
                response.put("success", false);
                response.put("message", "无权操作其他用户的好友关系");
                return ResponseEntity.status(403).body(response);
            }

            friendshipService.removeFriend(userId, friendId);

            response.put("success", true);
            response.put("message", "已删除好友");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除好友失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getFriends(
            @RequestParam(required = false) String identityCode,
            @RequestParam(required = false) Long userId,
            HttpServletRequest request) {

        log.debug("查询好友列表: identityCode={}, userId={}", identityCode, userId);

        Map<String, Object> response = new HashMap<>();
        try {
            Long currentUserId = jwtUtil.getCurrentUserId(request);

            if (userId != null && !currentUserId.equals(userId)) {
                response.put("success", false);
                response.put("message", "无权查看其他用户的好友列表");
                return ResponseEntity.status(403).body(response);
            }

            Long targetUserId;

            if (identityCode != null && !identityCode.trim().isEmpty()) {
                User targetUser = userService.findByIdentityCode(identityCode);
                targetUserId = targetUser.getId();
                log.debug("通过identityCode查找到用户ID: {}", targetUserId);
            } else if (userId != null) {
                targetUserId = userId;
            } else {
                response.put("success", false);
                response.put("message", "必须提供 identityCode 或 userId 参数");
                return ResponseEntity.badRequest().body(response);
            }

            List<Friendship> friendships = friendshipService.getFriends(targetUserId);
            log.debug("查询到的Friendship记录数: {}", friendships.size());

            List<Long> friendIds = friendships.stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toList());

            Map<Long, User> userMap = userService.loadUsersByIds(friendIds);

            List<Map<String, Object>> friendList = friendIds.stream().map(friendId -> {
                User friend = userMap.get(friendId);
                Map<String, Object> friendMap = new HashMap<>();
                if (friend != null) {
                    friendMap.put("userId", friend.getId());
                    friendMap.put("username", friend.getUsername());
                    friendMap.put("nickname", friend.getNickname());
                    friendMap.put("identityCode", friend.getIdentityCode());
                    friendMap.put("avatarUrl", friend.getAvatarUrl());
                    friendMap.put("totalScore", friend.getTotalScore());
                    friendMap.put("winCount", friend.getWinCount());
                    friendMap.put("loseCount", friend.getLoseCount());
                    friendMap.put("drawCount", friend.getDrawCount());

                    String onlineKey = "online:user:" + friend.getId();
                    Boolean isOnline = redisTemplate.hasKey(onlineKey);
                    friendMap.put("online", isOnline != null && isOnline);
                }
                return friendMap;
            }).collect(Collectors.toList());

            response.put("success", true);
            response.put("count", friendList.size());
            response.put("data", friendList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询好友列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
