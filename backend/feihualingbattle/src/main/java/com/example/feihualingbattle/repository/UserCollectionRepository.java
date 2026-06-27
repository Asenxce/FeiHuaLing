package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.UserCollection;
import com.example.feihualingbattle.dto.CollectionDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户收藏数据访问接口
 */
@Repository
public interface UserCollectionRepository extends JpaRepository<UserCollection, Long> {

    /**
     * 根据用户ID查询所有收藏（按收藏时间倒序）
     */
    List<UserCollection> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 根据用户ID查询所有收藏，包含诗词信息（按收藏时间倒序）
     * 关联PoetryMaster获取完整诗词内容
     */
    @Query("SELECT new com.example.feihualingbattle.dto.CollectionDTO(" +
           "uc.id, uc.userId, uc.poetryId, uc.createTime, " +
           "COALESCE(pm.title, p.title), COALESCE(pm.author, p.author), COALESCE(pm.dynasty, p.dynasty), " +
           "COALESCE(pm.fullContentSimplified, p.content), p.poetryType, p.masterId) " +
           "FROM UserCollection uc " +
           "LEFT JOIN Poetry p ON uc.poetryId = p.id " +
           "LEFT JOIN PoetryMaster pm ON p.masterId = pm.id " +
           "WHERE uc.userId = :userId ORDER BY uc.createTime DESC")
    List<CollectionDTO> findCollectionsWithPoetry(@Param("userId") Long userId);

    /**
     * 检查用户是否已收藏某首诗词
     */
    Optional<UserCollection> findByUserIdAndPoetryId(Long userId, Long poetryId);

    /**
     * 删除用户的某个收藏
     */
    void deleteByUserIdAndPoetryId(Long userId, Long poetryId);

    /**
     * 统计用户收藏数量
     */
    long countByUserId(Long userId);
}
