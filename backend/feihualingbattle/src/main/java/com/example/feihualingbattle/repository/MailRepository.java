package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 邮件Repository接口
 */
@Repository
public interface MailRepository extends JpaRepository<Mail, Long> {

    /**
     * 根据用户ID查询所有邮件，按发送时间倒序排列
     */
    List<Mail> findByUserIdOrderBySendTimeDesc(Long userId);

    /**
     * 根据用户ID和已读状态查询邮件
     */
    List<Mail> findByUserIdAndIsReadOrderBySendTimeDesc(Long userId, Boolean isRead);

    /**
     * 统计用户未读邮件数量
     */
    long countByUserIdAndIsRead(Long userId, Boolean isRead);

    /**
     * 删除用户的已读邮件
     */
    void deleteByUserIdAndIsRead(Long userId, Boolean isRead);

    /**
     * 删除单封邮件（确保是用户的邮件）
     */
    void deleteByIdAndUserId(Long id, Long userId);

    /**
     * 根据用户ID和邮件类型查询邮件，按发送时间倒序排列
     */
    List<Mail> findByUserIdAndTypeOrderBySendTimeDesc(Long userId, Integer type);

    /**
     * 统计用户指定类型的未读邮件数量
     */
    long countByUserIdAndTypeAndIsRead(Long userId, Integer type, Boolean isRead);

    /**
     * 标记用户所有未读邮件为已读
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Mail m SET m.isRead = true WHERE m.userId = :userId AND m.isRead = false")
    int markAllAsRead(@org.springframework.data.repository.query.Param("userId") Long userId);
}
