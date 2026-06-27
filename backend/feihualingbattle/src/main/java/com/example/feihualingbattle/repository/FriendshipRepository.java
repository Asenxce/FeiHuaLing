package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    /**
     * 根据用户ID查找所有好友关系
     */
    List<Friendship> findByUserId(Long userId);
    
    /**
     * 根据用户ID和状态查找好友关系
     * 状态: 0-待确认, 1-已好友, 2-已拉黑
     */
    List<Friendship> findByUserIdAndStatus(Long userId, Integer status);
    
    /**
     * 查找特定用户对某好友的关系
     */
    Optional<Friendship> findByUserIdAndFriendId(Long userId, Long friendId);
    
    /**
     * 查找被某用户添加为好友的所有关系
     */
    List<Friendship> findByFriendId(Long friendId);
    
    /**
     * 根据被添加方ID和状态查找好友关系
     */
    List<Friendship> findByFriendIdAndStatus(Long friendId, Integer status);
    
    /**
     * 检查两个用户是否互为好友(双向查询)
     */
    default boolean areFriends(Long userId1, Long userId2) {
        Optional<Friendship> rel1 = findByUserIdAndFriendId(userId1, userId2);
        Optional<Friendship> rel2 = findByUserIdAndFriendId(userId2, userId1);
        return rel1.isPresent() && rel2.isPresent() 
            && rel1.get().getStatus() == 1 
            && rel2.get().getStatus() == 1;
    }
}