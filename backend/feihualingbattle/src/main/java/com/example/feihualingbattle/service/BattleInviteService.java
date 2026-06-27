package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.Battle;
import com.example.feihualingbattle.entity.BattleInvite;
import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.repository.BattleInviteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BattleInviteService {
    private static final Logger log = LoggerFactory.getLogger(BattleInviteService.class);

    @Autowired
    private BattleInviteRepository battleInviteRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BattleService battleService;

    @Autowired
    private BattleCreateService battleCreateService;

    @Autowired
    private MailService mailService;

    /**
     * 创建对战邀请
     */
    @Transactional
    public Map<String, Object> createInvite(Long inviterId, String inviteeIdentityCode,
                                             String gameMode, String keyword, String keyword2, Short timeLimit) {
        log.info("创建对战邀请: inviterId={}, inviteeIdentityCode={}, gameMode={}", 
                inviterId, inviteeIdentityCode, gameMode);

        User invitee = userService.findByIdentityCode(inviteeIdentityCode);
        if (invitee == null) {
            throw new RuntimeException("找不到身份码为 " + inviteeIdentityCode + " 的用户");
        }
        if (invitee.getId().equals(inviterId)) {
            throw new RuntimeException("不能邀请自己");
        }

        List<BattleInvite> existingInvites = battleInviteRepository
                .findByInviterIdAndInviteeIdAndStatus(inviterId, invitee.getId(), "PENDING");
        if (!existingInvites.isEmpty()) {
            throw new RuntimeException("您已向该用户发送过待处理的邀请，请勿重复发送");
        }

        User inviter = userService.loadUserById(inviterId);
        String inviteToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        BattleInvite invite = new BattleInvite(
                inviteToken,
                inviterId,
                inviter.getNickname() != null ? inviter.getNickname() : inviter.getUsername(),
                invitee.getId(),
                invitee.getNickname() != null ? invitee.getNickname() : invitee.getUsername(),
                expireTime
        );
        invite.setGameMode(gameMode);
        invite.setKeyword(keyword);
        invite.setTimeLimit(timeLimit != null ? timeLimit : 60);
        if (keyword2 != null) {
            invite.setKeyword2(keyword2);
        }

        battleInviteRepository.save(invite);

        String creatorName = inviter.getNickname() != null ? inviter.getNickname() : inviter.getUsername();
        try {
            mailService.sendMail(
                    invitee.getId(),
                    "对战邀请",
                    creatorName + " 邀请你进行一场飞花令对战！请打开游戏查看邀请详情。",
                    creatorName,
                    3,
                    invite.getId()
            );
            log.info("邀请邮件发送成功: inviteId={}, inviteeId={}", invite.getId(), invitee.getId());
        } catch (Exception e) {
            log.warn("邀请邮件发送失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inviteToken", inviteToken);
        result.put("inviteeName", invite.getInviteeName());
        result.put("gameMode", invite.getGameMode());
        result.put("expireTime", invite.getExpireTime());
        return result;
    }

    /**
     * 接受对战邀请
     */
    @Transactional
    public Map<String, Object> acceptInvite(String inviteToken, Long userId) {
        log.info("接受对战邀请: inviteToken={}, userId={}", inviteToken, userId);

        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        if (!invite.getInviteeId().equals(userId)) {
            throw new RuntimeException("您不是该邀请的被邀请人");
        }

        if (!"PENDING".equals(invite.getStatus())) {
            throw new RuntimeException("该邀请已处理或已过期");
        }

        if (LocalDateTime.now().isAfter(invite.getExpireTime())) {
            invite.setStatus("EXPIRED");
            battleInviteRepository.save(invite);
            throw new RuntimeException("邀请已过期");
        }

        User inviter = userService.loadUserById(invite.getInviterId());
        User invitee = userService.loadUserById(userId);

        battleCreateService.cancelOldOngoingBattles(inviter.getId(), invitee.getId());

        Battle battle = battleService.createFriendBattle(
                inviter.getId(),
                invitee.getId(),
                invite.getGameMode(),
                invite.getKeyword(),
                invite.getKeyword2(),
                null,  // colorKeyword
                null,  // numberKeyword
                null,  // forbiddenWord
                null,  // keywordPosition
                invite.getTimeLimit().intValue()
        );

        invite.setStatus("ACCEPTED");
        invite.setAcceptedTime(LocalDateTime.now());
        invite.setBattleId(battle.getBattleId());
        battleInviteRepository.save(invite);

        String inviteeName = invitee.getNickname() != null ? invitee.getNickname() : invitee.getUsername();
        try {
            mailService.sendMail(
                    inviter.getId(),
                    "邀请已接受",
                    inviteeName + " 已接受你的对战邀请，点击游戏进入对战！",
                    inviteeName,
                    4,
                    invite.getId()
            );
            log.info("接受通知邮件发送成功: inviteId={}, inviterId={}", invite.getId(), inviter.getId());
        } catch (Exception e) {
            log.warn("接受通知邮件发送失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("battleId", battle.getBattleId());
        result.put("gameMode", battle.getGameMode());
        result.put("keyword", battle.getKeyword());
        result.put("keyword2", battle.getKeyword2());
        result.put("timeLimit", battle.getTimeLimit());
        result.put("opponentName", inviter.getNickname() != null ? inviter.getNickname() : inviter.getUsername());
        result.put("opponentId", inviter.getId());
        result.put("inviteId", invite.getId());
        result.put("isCreator", false);
        return result;
    }

    /**
     * 拒绝对战邀请
     */
    @Transactional
    public void rejectInvite(String inviteToken, Long userId) {
        log.info("拒绝对战邀请: inviteToken={}, userId={}", inviteToken, userId);

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
        battleInviteRepository.save(invite);

        try {
            User invitee = userService.loadUserById(userId);
            String inviteeName = invitee.getNickname() != null ? invitee.getNickname() : invitee.getUsername();
            mailService.sendMail(
                    invite.getInviterId(),
                    "邀请已被拒绝",
                    inviteeName + " 已拒绝你的对战邀请。",
                    inviteeName,
                    5,
                    invite.getId()
            );
        } catch (Exception e) {
            log.warn("拒绝通知邮件发送失败: {}", e.getMessage());
        }
    }

    /**
     * 取消对战邀请
     */
    @Transactional
    public void cancelInvite(String inviteToken, Long userId) {
        log.info("取消对战邀请: inviteToken={}, userId={}", inviteToken, userId);

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

    /**
     * 获取用户的待处理邀请列表（带详细信息）
     */
    public List<Map<String, Object>> getPendingInvitesWithDetails(Long userId) {
        log.info("获取待处理邀请: userId={}", userId);

        List<BattleInvite> invites = battleInviteRepository.findByInviteeIdAndStatus(userId, "PENDING");

        return invites.stream().map(invite -> {
            Map<String, Object> map = new HashMap<>();
            map.put("inviteToken", invite.getInviteToken());
            map.put("inviterId", invite.getInviterId());
            map.put("inviterName", invite.getInviterName());
            map.put("gameMode", invite.getGameMode());
            map.put("keyword", invite.getKeyword());
            map.put("keyword2", invite.getKeyword2());
            map.put("timeLimit", invite.getTimeLimit());
            map.put("expireTime", invite.getExpireTime());
            map.put("createTime", invite.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取邀请详情
     */
    public Map<String, Object> getInviteDetail(String inviteToken, Long userId) {
        BattleInvite invite = battleInviteRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("邀请不存在"));

        if (!invite.getInviterId().equals(userId) && !invite.getInviteeId().equals(userId)) {
            throw new RuntimeException("无权查看该邀请");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("inviteToken", invite.getInviteToken());
        detail.put("inviterId", invite.getInviterId());
        detail.put("inviterName", invite.getInviterName());
        detail.put("inviteeId", invite.getInviteeId());
        detail.put("inviteeName", invite.getInviteeName());
        detail.put("gameMode", invite.getGameMode());
        detail.put("keyword", invite.getKeyword());
        detail.put("keyword2", invite.getKeyword2());
        detail.put("timeLimit", invite.getTimeLimit());
        detail.put("status", invite.getStatus());
        detail.put("expireTime", invite.getExpireTime());
        detail.put("createTime", invite.getCreateTime());
        if (invite.getBattleId() != null) {
            detail.put("battleId", invite.getBattleId());
        }
        return detail;
    }

    /**
     * 获取邀请历史
     */
    public List<Map<String, Object>> getInviteHistory(Long userId, boolean sent) {
        List<BattleInvite> invites;
        if (sent) {
            invites = battleInviteRepository.findByInviterId(userId);
        } else {
            invites = battleInviteRepository.findByInviteeId(userId);
        }

        return invites.stream()
                .filter(invite -> !"PENDING".equals(invite.getStatus()) || 
                        LocalDateTime.now().isBefore(invite.getExpireTime()))
                .map(invite -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("inviteToken", invite.getInviteToken());
                    map.put("otherUserId", sent ? invite.getInviteeId() : invite.getInviterId());
                    map.put("otherUserName", sent ? invite.getInviteeName() : invite.getInviterName());
                    map.put("gameMode", invite.getGameMode());
                    map.put("status", invite.getStatus());
                    map.put("createTime", invite.getCreateTime());
                    if (invite.getBattleId() != null) {
                        map.put("battleId", invite.getBattleId());
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }
}
