package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.BattleInvite;
import com.example.feihualingbattle.entity.Friendship;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.repository.BattleInviteRepository;
import com.example.feihualingbattle.repository.FriendshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FriendshipService {
    private static final Logger log = LoggerFactory.getLogger(FriendshipService.class);

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private BattleInviteRepository battleInviteRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    /**
     * 发送好友申请（申请模式，需对方确认）
     */
    @Transactional
    public Friendship sendFriendRequest(Long userId, String targetIdentityCode) {
        log.debug("sendFriendRequest: userId={}, targetIdentityCode={}", userId, targetIdentityCode);

        User targetUser = userService.findByIdentityCode(targetIdentityCode);
        log.debug("找到目标用户ID: {}, 用户名: {}", targetUser.getId(), targetUser.getUsername());

        if (userId.equals(targetUser.getId())) {
            log.warn("不能添加自己为好友: userId={}", userId);
            throw new RuntimeException("不能添加自己为好友");
        }

        Optional<Friendship> existingFriendship = friendshipRepository.findByUserIdAndFriendId(userId, targetUser.getId());
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == 1) {
                log.warn("已经是好友关系: userId={}, friendId={}", userId, targetUser.getId());
                throw new RuntimeException("已经是好友关系");
            }
            if (friendship.getStatus() == 0) {
                log.warn("好友申请已发送: userId={}, friendId={}", userId, targetUser.getId());
                throw new RuntimeException("已发送好友申请，等待对方确认");
            }
            if (friendship.getStatus() == 2) {
                log.warn("对方已拒绝: userId={}, friendId={}", userId, targetUser.getId());
                throw new RuntimeException("对方已拒绝您的好友申请");
            }
        }

        try {
            // 只创建单向申请记录（status=0 表示待确认）
            Friendship friendship = new Friendship(userId, targetUser.getId());
            friendship.setStatus(0);
            Friendship saved = friendshipRepository.save(friendship);
            log.debug("好友申请记录保存成功, ID: {}", saved.getId());

            // 发送好友申请通知邮件
            try {
                User currentUser = userService.loadUserById(userId);
                String nickname = currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername();
                String title = "收到新的好友申请";
                String content = "用户【" + nickname + "】想添加你为好友，请到好友页面处理。";
                mailService.sendMail(targetUser.getId(), title, content, nickname, 2, saved.getId());
                log.info("好友申请通知邮件已发送: 发送者={}, 接收者={}", userId, targetUser.getId());
            } catch (Exception e) {
                log.warn("发送好友申请通知邮件失败: {}", e.getMessage());
            }

            log.info("好友申请已发送: {} -> {}", userId, targetUser.getId());
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("好友申请已存在（并发冲突）: userId={}, friendId={}", userId, targetUser.getId());
            throw new RuntimeException("好友申请已存在，请勿重复发送");
        }
    }

    /**
     * 获取待处理的好友申请列表（作为被申请者收到的申请）
     */
    public List<Friendship> getPendingRequests(Long userId) {
        // 查找别人发给我的申请（friendId=我, status=0）
        List<Friendship> result = friendshipRepository.findByFriendIdAndStatus(userId, 0);
        log.debug("getPendingRequests: userId={}, count={}", userId, result.size());
        return result;
    }

    /**
     * 接受好友申请
     */
    @Transactional
    public Friendship acceptFriendRequest(Long userId, Long friendId) {
        log.debug("acceptFriendRequest: userId={}, friendId={}", userId, friendId);
        
        Friendship friendship = friendshipRepository.findByUserIdAndFriendId(friendId, userId)
                .orElseThrow(() -> new RuntimeException("未找到好友申请"));

        if (friendship.getStatus() != 0) {
            throw new RuntimeException("该申请已处理");
        }

        // 更新申请状态为已好友
        friendship.setStatus(1);
        friendship = friendshipRepository.save(friendship);

        // 创建反向好友关系
        Friendship reverseFriendship = new Friendship(userId, friendId);
        reverseFriendship.setStatus(1);
        friendshipRepository.save(reverseFriendship);
        
        // 发送接受通知邮件
        try {
            User currentUser = userService.loadUserById(userId);
            String nickname = currentUser.getNickname() != null ? currentUser.getNickname() : currentUser.getUsername();
            mailService.sendMail(friendId, "好友申请已通过", "【" + nickname + "】已通过你的好友申请，你们现在已经是好友了。", nickname, 1, friendship.getId());
        } catch (Exception e) {
            log.warn("发送接受通知邮件失败: {}", e.getMessage());
        }

        log.info("好友申请已通过: {} -> {}", friendId, userId);
        return friendship;
    }

    /**
     * 拒绝好友申请
     */
    @Transactional
    public void rejectFriendRequest(Long userId, Long friendId) {
        log.debug("rejectFriendRequest: userId={}, friendId={}", userId, friendId);
        
        Friendship friendship = friendshipRepository.findByUserIdAndFriendId(friendId, userId)
                .orElseThrow(() -> new RuntimeException("未找到好友申请"));

        if (friendship.getStatus() != 0) {
            throw new RuntimeException("该申请已处理");
        }

        // 更新状态为已拒绝
        friendship.setStatus(2);
        friendshipRepository.save(friendship);
        
        log.info("好友申请已拒绝: {} -> {}", friendId, userId);
    }

    /**
     * 删除好友
     */
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        // 删除双向好友关系
        Optional<Friendship> rel1 = friendshipRepository.findByUserIdAndFriendId(userId, friendId);
        rel1.ifPresent(friendshipRepository::delete);
        
        Optional<Friendship> rel2 = friendshipRepository.findByUserIdAndFriendId(friendId, userId);
        rel2.ifPresent(friendshipRepository::delete);
    }

    /**
     * 获取用户的好友列表
     */
    public List<Friendship> getFriends(Long userId) {
        log.debug("getFriends: userId={}", userId);

        List<Friendship> result = friendshipRepository.findByUserIdAndStatus(userId, 1);

        log.debug("查询结果数量: {}", result.size());
        if (result.isEmpty()) {
            log.debug("未找到任何好友记录: userId={}", userId);
        }

        return result;
    }

    /**
     * 统计好友数量
     */
    public long getFriendCount(Long userId) {
        return friendshipRepository.findByUserIdAndStatus(userId, 1).size();
    }

    /**
     * 创建对战邀请
     */
    @Transactional
    public BattleInvite createBattleInvite(Long inviterId, Long inviteeId, 
                                           String gameMode, String keyword, Short timeLimit) {
        // 获取用户信息
        User inviter = userService.loadUserById(inviterId);
        User invitee = userService.loadUserById(inviteeId);

        // 生成邀请令牌
        String inviteToken = UUID.randomUUID().toString().replace("-", "");

        // 设置过期时间（默认30分钟）
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        // 创建邀请
        BattleInvite invite = new BattleInvite(
            inviteToken,
            inviterId,
            inviter.getNickname(),
            inviteeId,
            invitee.getNickname(),
            expireTime
        );
        invite.setGameMode(gameMode);
        invite.setKeyword(keyword);
        invite.setTimeLimit(timeLimit != null ? timeLimit : 60);

        return battleInviteRepository.save(invite);
    }

    /**
     * 接受对战邀请
     */
    @Transactional
    public BattleInvite acceptBattleInvite(String inviteToken, Long userId) {
        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        // 验证被邀请人
        if (!invite.getInviteeId().equals(userId)) {
            throw new RuntimeException("您不是该邀请的被邀请人");
        }

        // 检查邀请状态
        if (!"PENDING".equals(invite.getStatus())) {
            throw new RuntimeException("该邀请已处理或已过期");
        }

        // 检查是否过期
        if (LocalDateTime.now().isAfter(invite.getExpireTime())) {
            invite.setStatus("EXPIRED");
            battleInviteRepository.save(invite);
            throw new RuntimeException("邀请已过期");
        }

        // 更新邀请状态
        invite.setStatus("ACCEPTED");
        invite.setAcceptedTime(LocalDateTime.now());
        return battleInviteRepository.save(invite);
    }

    /**
     * 拒绝对战邀请
     */
    @Transactional
    public BattleInvite rejectBattleInvite(String inviteToken, Long userId) {
        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        if (!invite.getInviteeId().equals(userId)) {
            throw new RuntimeException("您不是该邀请的被邀请人");
        }

        if (!"PENDING".equals(invite.getStatus())) {
            throw new RuntimeException("该邀请已处理");
        }

        invite.setStatus("REJECTED");
        invite.setRejectedTime(LocalDateTime.now());
        return battleInviteRepository.save(invite);
    }

    /**
     * 获取用户的待处理邀请
     */
    public List<BattleInvite> getPendingInvites(Long userId) {
        return battleInviteRepository.findByInviteeIdAndStatus(userId, "PENDING");
    }

    /**
     * 根据令牌获取邀请详情
     */
    public BattleInvite getInviteByToken(String inviteToken) {
        return battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));
    }

    /**
     * 取消邀请
     */
    @Transactional
    public void cancelInvite(String inviteToken, Long userId) {
        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        if (!invite.getInviterId().equals(userId)) {
            throw new RuntimeException("您不是该邀请的发起人");
        }

        if (!"PENDING".equals(invite.getStatus())) {
            throw new RuntimeException("该邀请已处理");
        }

        invite.setStatus("CANCELLED");
        battleInviteRepository.save(invite);
    }
}