package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.BattleInviteService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/battle/invite")
public class BattleInviteController {
    private static final Logger log = LoggerFactory.getLogger(BattleInviteController.class);

    @Autowired
    private BattleInviteService battleInviteService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建对战邀请
     * POST /api/battle/invite/create
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createInvite(HttpServletRequest httpRequest,
                                                          @RequestBody Map<String, Object> request) {
        try {
            Long inviterId = jwtUtil.getCurrentUserId(httpRequest);
            
            String inviteeIdentityCode = (String) request.get("inviteeIdentityCode");
            String gameMode = (String) request.get("gameMode");
            String keyword = (String) request.get("keyword");
            String keyword2 = (String) request.get("keyword2");
            Integer timeLimit = request.containsKey("timeLimit") ? ((Number) request.get("timeLimit")).intValue() : 60;

            if (inviteeIdentityCode == null || inviteeIdentityCode.trim().isEmpty()) {
                return ApiResponse.error("被邀请方身份码不能为空");
            }
            if (gameMode == null || gameMode.trim().isEmpty()) {
                return ApiResponse.error("游戏模式不能为空");
            }

            Map<String, Object> result = battleInviteService.createInvite(
                    inviterId, inviteeIdentityCode, gameMode, keyword, keyword2, timeLimit.shortValue());

            return ApiResponse.success("邀请创建成功", result);
        } catch (Exception e) {
            log.error("创建对战邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 接受对战邀请
     * POST /api/battle/invite/{inviteToken}/accept
     */
    @PostMapping("/{inviteToken}/accept")
    public ApiResponse<Map<String, Object>> acceptInvite(HttpServletRequest httpRequest,
                                                          @PathVariable String inviteToken) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            Map<String, Object> result = battleInviteService.acceptInvite(inviteToken, userId);

            return ApiResponse.success("已接受邀请", result);
        } catch (Exception e) {
            log.error("接受邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 拒绝对战邀请
     * POST /api/battle/invite/{inviteToken}/reject
     */
    @PostMapping("/{inviteToken}/reject")
    public ApiResponse<Map<String, Object>> rejectInvite(HttpServletRequest httpRequest,
                                                          @PathVariable String inviteToken) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            battleInviteService.rejectInvite(inviteToken, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已拒绝邀请");
            return ApiResponse.success("已拒绝邀请", result);
        } catch (Exception e) {
            log.error("拒绝邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 取消对战邀请
     * POST /api/battle/invite/{inviteToken}/cancel
     */
    @PostMapping("/{inviteToken}/cancel")
    public ApiResponse<Map<String, Object>> cancelInvite(HttpServletRequest httpRequest,
                                                          @PathVariable String inviteToken) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            battleInviteService.cancelInvite(inviteToken, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "已取消邀请");
            return ApiResponse.success("已取消邀请", result);
        } catch (Exception e) {
            log.error("取消邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户的待处理邀请列表
     * GET /api/battle/invite/pending
     */
    @GetMapping("/pending")
    public ApiResponse<List<Map<String, Object>>> getPendingInvites(HttpServletRequest httpRequest) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            List<Map<String, Object>> invites = battleInviteService.getPendingInvitesWithDetails(userId);

            return ApiResponse.success(invites);
        } catch (Exception e) {
            log.error("获取待处理邀请失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取邀请详情
     * GET /api/battle/invite/{inviteToken}
     */
    @GetMapping("/{inviteToken}")
    public ApiResponse<Map<String, Object>> getInviteDetail(HttpServletRequest httpRequest,
                                                             @PathVariable String inviteToken) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            Map<String, Object> inviteDetail = battleInviteService.getInviteDetail(inviteToken, userId);

            return ApiResponse.success(inviteDetail);
        } catch (Exception e) {
            log.error("获取邀请详情失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户的历史邀请列表
     * GET /api/battle/invite/history?sent=true|false
     */
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getInviteHistory(HttpServletRequest httpRequest,
                                                                     @RequestParam(defaultValue = "false") boolean sent) {
        try {
            Long userId = jwtUtil.getCurrentUserId(httpRequest);

            List<Map<String, Object>> invites = battleInviteService.getInviteHistory(userId, sent);

            return ApiResponse.success(invites);
        } catch (Exception e) {
            log.error("获取邀请历史失败: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }
}
