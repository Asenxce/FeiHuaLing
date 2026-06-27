package com.example.feihualingbattle.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户实体类
 * 对应数据库表: t_user
 * 实现 UserDetails 以对接 Spring Security
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_user")
public class User implements UserDetails {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名，用于登录，唯一
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码，BCrypt加密存储
     */
    @Column(nullable = false, length = 100)
    private String password;

    /**
     * 昵称，显示名称
     */
    @Column(length = 50, columnDefinition = "VARCHAR(50) DEFAULT ''")
    private String nickname;

    /**
     * 8位身份码，唯一，用于好友搜索
     */
    @Column(name = "identity_code", nullable = false, unique = true, length = 8)
    private String identityCode;

    /**
     * 头像URL
     */
    @Column(name = "avatar_url", length = 255, columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String avatarUrl;

    /**
     * 邮箱
     */
    @Column(length = 100, columnDefinition = "VARCHAR(100) DEFAULT NULL")
    private String email;

    /**
     * 手机号
     */
    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT NULL")
    private String phone;

    /**
     * 个性签名
     */
    @Column(length = 500, columnDefinition = "VARCHAR(500) DEFAULT NULL")
    private String bio;

    /**
     * 总积分
     */
    @Column(name = "total_score", columnDefinition = "INT DEFAULT 0")
    private Integer totalScore;

    /**
     * 胜利次数
     */
    @Column(name = "win_count", columnDefinition = "INT DEFAULT 0")
    private Integer winCount;

    /**
     * 失败次数
     */
    @Column(name = "lose_count", columnDefinition = "INT DEFAULT 0")
    private Integer loseCount;

    /**
     * 平局次数
     */
    @Column(name = "draw_count", columnDefinition = "INT DEFAULT 0")
    private Integer drawCount;

    /**
     * 注册时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 构造函数：初始化默认值
     */
    public User(String username, String password, String nickname, String identityCode) {
        this.username = username;
        this.password = password;
        this.nickname = nickname != null ? nickname : "";
        this.identityCode = identityCode;
        this.avatarUrl = "";
        this.totalScore = 0;
        this.winCount = 0;
        this.loseCount = 0;
        this.drawCount = 0;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getIdentityCode() {
        return identityCode;
    }

    public void setIdentityCode(String identityCode) {
        this.identityCode = identityCode;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getWinCount() {
        return winCount;
    }

    public void setWinCount(Integer winCount) {
        this.winCount = winCount;
    }

    public Integer getLoseCount() {
        return loseCount;
    }

    public void setLoseCount(Integer loseCount) {
        this.loseCount = loseCount;
    }

    public Integer getDrawCount() {
        return drawCount;
    }

    public void setDrawCount(Integer drawCount) {
        this.drawCount = drawCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
