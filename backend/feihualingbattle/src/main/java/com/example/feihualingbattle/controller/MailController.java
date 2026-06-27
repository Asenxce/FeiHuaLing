package com.example.feihualingbattle.controller;

import com.example.feihualingbattle.dto.DeleteMailRequest;
import com.example.feihualingbattle.dto.MarkReadRequest;
import com.example.feihualingbattle.dto.SendMailRequest;
import com.example.feihualingbattle.entity.Mail;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.security.JwtUtil;
import com.example.feihualingbattle.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mail")
public class MailController {
    @Autowired
    private MailService mailService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 1. 获取邮件列表
     * GET /api/mail/list
     * 按send_time降序排列
     */
    @GetMapping("/list")
    public ApiResponse<List<Mail>> getMailList(@RequestHeader("Authorization") String authorization) {
        try {
            Long userId = extractUserIdFromToken(authorization);
            List<Mail> mailList = mailService.getMailList(userId);
            return ApiResponse.success(mailList);
        } catch (IllegalArgumentException e) {
            // Token无效或格式错误
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取邮件列表失败: " + e.getMessage());
        }
    }

    /**
     * 2. 标记邮件为已读
     * POST /api/mail/mark-read
     * Body: {"mailId": 123}
     */
    @PostMapping("/mark-read")
    public ApiResponse<String> markAsRead(@RequestBody MarkReadRequest request,
                                          @RequestHeader("Authorization") String authorization) {
        try {
            Long userId = extractUserIdFromToken(authorization);
            
            // 参数校验
            if (request.getMailId() == null) {
                return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "邮件ID不能为空");
            }
            
            boolean success = mailService.markAsRead(request.getMailId(), userId);
            if (success) {
                return ApiResponse.success("标记成功");
            } else {
                return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "邮件不存在");
            }
        } catch (SecurityException e) {
            // 权限不足
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "标记失败: " + e.getMessage());
        }
    }

    /**
     * 2.1 标记邮件为已读(别名接口,兼容前端调用)
     * POST /api/mail/read
     * Body: {"mailId": 123}
     */
    @PostMapping("/read")
    public ApiResponse<String> markAsReadAlias(@RequestBody MarkReadRequest request,
                                               @RequestHeader("Authorization") String authorization) {
        // 直接调用markAsRead方法
        return markAsRead(request, authorization);
    }

    /**
     * 3. 删除邮件
     * POST /api/mail/delete
     * Body: {"mailId": 123}
     */
    @PostMapping("/delete")
    public ApiResponse<String> deleteMail(@RequestBody DeleteMailRequest request,
                                          @RequestHeader("Authorization") String authorization) {
        try {
            Long userId = extractUserIdFromToken(authorization);
            
            // 参数校验
            if (request.getMailId() == null) {
                return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "邮件ID不能为空");
            }
            
            boolean success = mailService.deleteMail(request.getMailId(), userId);
            if (success) {
                return ApiResponse.success("删除成功");
            } else {
                return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "邮件不存在或无权删除");
            }
        } catch (SecurityException e) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "删除失败: " + e.getMessage());
        }
    }

    /**
     * 4. 清空已读邮件
     * POST /api/mail/clear-read
     */
    @PostMapping("/clear-read")
    public ApiResponse<Map<String, Object>> clearReadMails(@RequestHeader("Authorization") String authorization) {
        try {
            Long userId = extractUserIdFromToken(authorization);
            
            long deletedCount = mailService.clearReadMails(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deletedCount", deletedCount);
            
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "清空失败: " + e.getMessage());
        }
    }

    /**
     * 5. 发送邮件（内部接口）
     * POST /api/mail/send
     * 用于系统发送通知、好友申请、对战邀请等
     * 
     * 支持两种参数传递方式：
     * 1. 使用 identityCode（推荐）: {"identityCode":"26928360","title":"...","content":"..."}
     * 2. 使用 userId（已废弃）: {"userId":123,"title":"...","content":"..."}
     */
    @PostMapping("/send")
    public ApiResponse<Mail> sendMail(@RequestBody SendMailRequest request,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            // 如果是系统调用，可以不传token；如果需要认证，则验证token
            if (authorization != null && !authorization.isEmpty()) {
                try {
                    extractUserIdFromToken(authorization);
                } catch (Exception e) {
                    return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "无效的Token");
                }
            }
            
            // 参数校验
            if ((request.getIdentityCode() == null || request.getIdentityCode().trim().isEmpty()) 
                && request.getUserId() == null) {
                return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "接收者身份码或用户ID至少提供一个");
            }
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "标题不能为空");
            }
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "内容不能为空");
            }
            if (request.getType() == null) {
                return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "邮件类型不能为空");
            }
            
            Mail mail;
            
            // 优先使用 identityCode，如果未提供则使用 userId（向后兼容）
            if (request.getIdentityCode() != null && !request.getIdentityCode().trim().isEmpty()) {
                // 通过身份码发送邮件
                mail = mailService.sendMailByIdentityCode(
                    request.getIdentityCode(),
                    request.getTitle(),
                    request.getContent(),
                    request.getSender(),
                    request.getType(),
                    request.getRelatedId()
                );
            } else {
                // 通过用户ID发送邮件（已废弃，保留兼容）
                mail = mailService.sendMail(
                    request.getUserId(),
                    request.getTitle(),
                    request.getContent(),
                    request.getSender(),
                    request.getType(),
                    request.getRelatedId()
                );
            }
            
            return ApiResponse.success("发送成功", mail);
        } catch (RuntimeException e) {
            // 业务异常（如找不到用户）
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取未读邮件数量
     * GET /api/mail/unread-count
     * 
     * 支持两种方式：
     * 1. 从Token中解析用户身份（推荐）
     * 2. 通过查询参数 identityCode 指定用户
     */
    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "identityCode", required = false) String identityCode) {
        try {
            Long userId;
            
            // 优先使用 identityCode 参数
            if (identityCode != null && !identityCode.trim().isEmpty()) {
                User targetUser = mailService.getUserByIdentityCode(identityCode);
                if (targetUser == null) {
                    return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "找不到身份码为 " + identityCode + " 的用户");
                }
                userId = targetUser.getId();
            } else {
                // 从 Token 中解析用户ID
                if (authorization == null || authorization.trim().isEmpty()) {
                    return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "请提供 Authorization Token 或 identityCode 参数");
                }
                userId = extractUserIdFromToken(authorization);
            }
            
            long unreadCount = mailService.countUnreadMails(userId);
            
            return ApiResponse.success((int) unreadCount);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取未读数失败: " + e.getMessage());
        }
    }

    /**
     * 标记所有未读邮件为已读
     * POST /api/mail/mark-all-read
     */
    @PostMapping("/mark-all-read")
    public ApiResponse<Map<String, Object>> markAllRead(@RequestHeader("Authorization") String authorization) {
        try {
            Long userId = extractUserIdFromToken(authorization);
            
            int count = mailService.markAllRead(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("markedCount", count);
            
            return ApiResponse.success("已标记 " + count + " 封邮件为已读", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "标记失败: " + e.getMessage());
        }
    }

    /**
     * 获取各类型未读邮件数量
     * GET /api/mail/unread-counts
     * 返回 systemCount, friendRequestCount, battleInviteCount, totalCount
     */
    @GetMapping("/unread-counts")
    public ApiResponse<Map<String, Long>> getUnreadCounts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "identityCode", required = false) String identityCode) {
        try {
            Long userId;
            
            if (identityCode != null && !identityCode.trim().isEmpty()) {
                User targetUser = mailService.getUserByIdentityCode(identityCode);
                if (targetUser == null) {
                    return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "找不到身份码为 " + identityCode + " 的用户");
                }
                userId = targetUser.getId();
            } else {
                if (authorization == null || authorization.trim().isEmpty()) {
                    return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "请提供 Authorization Token 或 identityCode 参数");
                }
                userId = extractUserIdFromToken(authorization);
            }
            
            Map<String, Long> counts = mailService.countUnreadMailsByType(userId);
            return ApiResponse.success(counts);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取未读数失败: " + e.getMessage());
        }
    }

    /**
     * 从JWT Token中提取用户ID
     * @throws IllegalArgumentException 如果Token无效
     */
    private Long extractUserIdFromToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("无效的Token格式");
        }
        
        String token = authorization.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        if (userId == null) {
            throw new IllegalArgumentException("无效的Token");
        }
        
        return userId;
    }
}
