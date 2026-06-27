package com.example.feihualingbattle.service;

import com.example.feihualingbattle.entity.User;
import com.example.feihualingbattle.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码（明文）
     * @param nickname 昵称（可选）
     * @return 注册成功的用户信息
     */
    @Transactional
    public User register(String username, String password, String nickname) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建用户对象
        User user = new User();
        user.setUsername(username);
        // BCrypt加密密码
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.isEmpty() ? nickname : username);
        // 生成8位随机身份码
        user.setIdentityCode(generateIdentityCode());
        user.setAvatarUrl("");
        user.setTotalScore(0);
        user.setWinCount(0);
        user.setLoseCount(0);
        user.setDrawCount(0);

        return userRepository.save(user);
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功的用户信息
     */
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("登录失败 - 用户不存在: username={}", username);
            throw new RuntimeException("用户名或密码错误");
        }

        User user = userOpt.get();
        // 验证密码（BCrypt比对）
        boolean passwordMatch = passwordEncoder.matches(password, user.getPassword());
        log.info("登录验证: username={}, passwordMatch={}, storedPasswordHash={}", 
                username, passwordMatch, user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "...");
        
        if (!passwordMatch) {
            log.warn("登录失败 - 密码错误: username={}, inputPasswordLength={}", username, password.length());
            throw new RuntimeException("用户名或密码错误");
        }

        // 修复：如果旧账号的 identityCode 为空，生成一个新的
        if (user.getIdentityCode() == null || user.getIdentityCode().isEmpty()) {
            user.setIdentityCode(generateIdentityCode());
            log.info("身份码修复: username={}, newIdentityCode={}", username, user.getIdentityCode());
            userRepository.save(user);
        }

        return user;
    }

    /**
     * 根据ID加载用户
     */
    public User loadUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 批量根据ID加载用户（避免N+1问题）
     */
    public java.util.Map<Long, User> loadUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<User> users = userRepository.findByIdIn(userIds);
        return users.stream().collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
    }

    /**
     * 根据身份码查找用户
     */
    public User findByIdentityCode(String identityCode) {
        return userRepository.findByIdentityCode(identityCode)
                .orElseThrow(() -> new RuntimeException("未找到该身份码对应的用户"));
    }

    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(Long userId, String nickname, String avatarUrl, String email, String phone, String bio) {
        User user = loadUserById(userId);
        
        // 更新昵称
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        
        // 更新头像
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        
        // 校验并更新邮箱
        if (email != null) {
            if (!email.isEmpty()) {
                // 正则校验邮箱格式
                String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
                if (!email.matches(emailRegex)) {
                    throw new IllegalArgumentException("邮箱格式不正确");
                }
            }
            user.setEmail(email.isEmpty() ? null : email);
        }
        
        // 校验并更新手机号
        if (phone != null) {
            if (!phone.isEmpty()) {
                // 正则校验手机号格式（中国大陆）
                String phoneRegex = "^1[3-9]\\d{9}$";
                if (!phone.matches(phoneRegex)) {
                    throw new IllegalArgumentException("手机号格式不正确");
                }
            }
            user.setPhone(phone.isEmpty() ? null : phone);
        }
        
        // 更新个性签名
        if (bio != null) {
            // 限制长度不超过500字符
            if (bio.length() > 500) {
                throw new IllegalArgumentException("个性签名不能超过500个字符");
            }
            user.setBio(bio.isEmpty() ? null : bio);
        }
        
        return userRepository.save(user);
    }

    /**
     * 获取用户排行榜（按总积分）
     */
    public List<User> getRankingByScore() {
        return userRepository.findAllByOrderByTotalScoreDesc();
    }

    /**
     * 获取用户排行榜（按胜利次数）
     */
    public List<User> getRankingByWins() {
        return userRepository.findAllByOrderByWinCountDesc();
    }

    /**
     * 更新用户积分和战绩
     */
    @Transactional
    public void updateUserInfo(User user) {
        userRepository.save(user);
    }

    /**
     * 生成8位随机身份码
     * 确保唯一性
     */
    private String generateIdentityCode() {
        Random random = new Random();
        String code;
        int attempts = 0;
        do {
            code = String.format("%08d", random.nextInt(100000000));
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("生成身份码失败，请稍后重试");
            }
        } while (userRepository.existsByIdentityCode(code));
        return code;
    }
}