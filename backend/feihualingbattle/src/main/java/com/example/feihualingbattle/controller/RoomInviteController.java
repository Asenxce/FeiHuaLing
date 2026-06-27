package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.RoomInviteService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room/invite")
public class RoomInviteController {
    private static final Logger log = LoggerFactory.getLogger(RoomInviteController.class);

    @Autowired
    private RoomInviteService roomInviteService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建房间邀请
     * POST /api/room/invite/create
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createInvite(HttpServletRequest httpRequest,
                                                          @RequestBody Map<String, Object> request) {
        try {
            Long inviterId = jwtUtil.getCurrentUserId(httpRequest);
            Long inviteeId = Long.valueOf(request.get("inviteeId").toString());
            String roomId = (String) request.get("roomId");

            if (inviteeId == null) {
                return ApiResponse.error("被邀请人ID不能为空");
            }
            if (roomId == null || roomId.trim().isEmpty()) {
                return ApiResponse.error("房间ID不能为空");
            }

            Map<String, Object> result = roomInviteService.createRoomInvite(inviterId, inviteeId, roomId);

            return ApiResponse.success("房间邀请创建成功", result);
        } catch (Exception e) {
            log.error("创建房间邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 接受房间邀请
     * POST /api/room/invite/{inviteToken}/accept
     */
    @PostMapping("/{inviteToken}/accept")
    public ApiResponse<Map<String, Object>> acceptInvite(HttpServletRequest httpRequest,
                                                          @PathVariable String inviteToken) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            Map<String, Object> result = roomInviteService.acceptRoomInvite(inviteToken, userId);

            return ApiResponse.success("已接受邀请并加入房间", result);
        } catch (Exception e) {
            log.error("接受房间邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 取消房间邀请
     * POST /api/room/invite/{inviteToken}/cancel
     */
    @PostMapping("/{inviteToken}/cancel")
    public ApiResponse<Map<String, Object>> cancelInvite(HttpServletRequest httpRequest,
                                                          @PathVariable String inviteToken) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            roomInviteService.cancelRoomInvite(inviteToken, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已取消邀请");
            return ApiResponse.success("已取消邀请", result);
        } catch (Exception e) {
            log.error("取消房间邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户的待处理房间邀请列表
     * GET /api/room/invite/pending
     */
    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> getPendingInvites(HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            List<Map<String, Object>> invites = roomInviteService.getPendingRoomInvites(userId);

            return ApiResponse.success(invites);
        } catch (Exception e) {
            log.error("获取待处理房间邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取邀请详情
     * GET /api/room/invite/{inviteToken}
     */
    @GetMapping("/{inviteToken}")
    public ApiResponse<Map<String, Object>> getInviteDetail(HttpServletRequest httpRequest,
                                                             @PathVariable String inviteToken) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            Map<String, Object> inviteDetail = roomInviteService.getInviteDetail(inviteToken, userId);

            return ApiResponse.success(inviteDetail);
        } catch (Exception e) {
            log.error("获取邀请详情失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
}
