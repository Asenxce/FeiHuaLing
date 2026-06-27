package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.Mail;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.repository.MailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 邮件服务类
 */
@Service
public class MailService {

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private UserService userService;

    /**
     * 获取用户的邮件列表
     * 按send_time降序排列
     *
     * @param userId 用户ID
     * @return 邮件列表
     */
    public List<Mail> getMailList(Long userId) {
        return mailRepository.findByUserIdOrderBySendTimeDesc(userId);
    }

    /**
     * 标记邮件为已读
     *
     * @param mailId 邮件ID
     * @param userId 用户ID（用于权限验证）
     * @return 是否成功
     * @throws SecurityException 如果无权操作此邮件
     */
    @Transactional
    public boolean markAsRead(Long mailId, Long userId) {
        Optional<Mail> mailOpt = mailRepository.findById(mailId);
        
        if (mailOpt.isEmpty()) {
            return false;
        }
        
        Mail mail = mailOpt.get();
        
        // 验证邮件属于该用户
        if (!mail.getUserId().equals(userId)) {
            throw new SecurityException("无权操作此邮件");
        }
        
        // 如果已经是已读状态，直接返回成功
        if (mail.getIsRead()) {
            return true;
        }
        
        mail.setIsRead(true);
        mailRepository.save(mail);
        
        // 强制刷新，确保数据立即持久化到数据库
        mailRepository.flush();
        
        return true;
    }

    /**
     * 删除单封邮件
     *
     * @param mailId 邮件ID
     * @param userId 用户ID（用于权限验证）
     * @return 是否成功
     */
    @Transactional
    public boolean deleteMail(Long mailId, Long userId) {
        Optional<Mail> mailOpt = mailRepository.findById(mailId);
        
        if (mailOpt.isEmpty()) {
            return false;
        }
        
        Mail mail = mailOpt.get();
        
        // 验证邮件属于该用户
        if (!mail.getUserId().equals(userId)) {
            throw new SecurityException("无权删除此邮件");
        }
        
        mailRepository.delete(mail);
        return true;
    }

    /**
     * 清空用户的所有已读邮件
     *
     * @param userId 用户ID
     * @return 删除的邮件数量
     */
    @Transactional
    public long clearReadMails(Long userId) {
        // 先查询数量
        long count = mailRepository.countByUserIdAndIsRead(userId, true);
        
        // 删除所有已读邮件
        mailRepository.deleteByUserIdAndIsRead(userId, true);
        
        return count;
    }

    /**
     * 发送邮件（内部接口，用于系统发送通知）
     *
     * @param userId   接收者ID
     * @param title    标题
     * @param content  内容
     * @param sender   发件人
     * @param type     类型：1-系统通知，2-好友申请，3-对战邀请
     * @param relatedId 关联ID
     * @return 创建的邮件对象
     */
    @Transactional
    public Mail sendMail(Long userId, String title, String content, String sender, Integer type, Long relatedId) {
        Mail mail = new Mail();
        mail.setUserId(userId);
        mail.setTitle(title);
        mail.setContent(content);
        mail.setSender(sender != null ? sender : "系统");
        mail.setSendTime(LocalDateTime.now());
        mail.setType(type);
        mail.setIsRead(false);
        mail.setRelatedId(relatedId);
        
        return mailRepository.save(mail);
    }

    /**
     * 通过身份码发送邮件
     *
     * @param identityCode 接收者身份码
     * @param title    标题
     * @param content  内容
     * @param sender   发件人
     * @param type     类型：1-系统通知，2-好友申请，3-对战邀请
     * @param relatedId 关联ID
     * @return 创建的邮件对象
     * @throws RuntimeException 如果找不到对应用户
     */
    @Transactional
    public Mail sendMailByIdentityCode(String identityCode, String title, String content, String sender, Integer type, Long relatedId) {
        // 根据身份码查找用户
        User targetUser = userService.findByIdentityCode(identityCode);
        if (targetUser == null) {
            throw new RuntimeException("找不到身份码为 " + identityCode + " 的用户");
        }
        
        return sendMail(targetUser.getId(), title, content, sender, type, relatedId);
    }

    /**
     * 统计用户未读邮件数量
     *
     * @param userId 用户ID
     * @return 未读邮件数量
     */
    public long countUnreadMails(Long userId) {
        return mailRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * 统计用户各类型未读邮件数量
     *
     * @param userId 用户ID
     * @return 各类型未读数量：systemCount, friendRequestCount, battleInviteCount, totalCount
     */
    public java.util.Map<String, Long> countUnreadMailsByType(Long userId) {
        long systemCount = mailRepository.countByUserIdAndTypeAndIsRead(userId, 1, false);
        long friendRequestCount = mailRepository.countByUserIdAndTypeAndIsRead(userId, 2, false);
        long battleInviteCount = mailRepository.countByUserIdAndTypeAndIsRead(userId, 3, false);
        long totalCount = systemCount + friendRequestCount + battleInviteCount;
        
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("systemCount", systemCount);
        counts.put("friendRequestCount", friendRequestCount);
        counts.put("battleInviteCount", battleInviteCount);
        counts.put("totalCount", totalCount);
        return counts;
    }

    /**
     * 标记用户所有未读邮件为已读
     *
     * @param userId 用户ID
     * @return 标记为已读的邮件数量
     */
    @Transactional
    public int markAllRead(Long userId) {
        return mailRepository.markAllAsRead(userId);
    }

    /**
     * 根据身份码获取用户
     *
     * @param identityCode 身份码
     * @return 用户对象，不存在则返回null
     */
    public User getUserByIdentityCode(String identityCode) {
        return userService.findByIdentityCode(identityCode);
    }
}
