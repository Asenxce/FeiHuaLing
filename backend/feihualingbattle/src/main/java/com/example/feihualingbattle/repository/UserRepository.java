package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    // findById already exists in JpaRepository, no need to override
    // Optional<User> findById(Long id);
    Optional<User> findByIdentityCode(String identityCode);
    
    /**
     * 检查身份码是否存在
     */
    boolean existsByIdentityCode(String identityCode);
    
    /**
     * 按总积分降序查询所有用户(排行榜)
     */
    List<User> findAllByOrderByTotalScoreDesc();
    
    /**
     * 按胜利次数降序查询所有用户
     */
    List<User> findAllByOrderByWinCountDesc();

    /**
     * 批量按ID查询用户（避免N+1问题）
     */
    List<User> findByIdIn(List<Long> ids);
}