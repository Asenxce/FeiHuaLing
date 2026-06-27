package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRecordRepository extends JpaRepository<UserRecord, Long> {

    /**
     * 根据用户ID查找战绩记录
     */
    List<UserRecord> findByUserId(Long userId);

    /**
     * 根据用户ID查找战绩记录,按创建时间降序
     */
    List<UserRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 获取用户最近的N条对战记录
     */
    @Query("SELECT ur FROM UserRecord ur WHERE ur.userId = :userId ORDER BY ur.createTime DESC LIMIT :limit")
    List<UserRecord> findTopByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 根据对战ID查找战绩记录
     */
    List<UserRecord> findByBattleId(String battleId);

    /**
     * 根据用户ID和对战类型查找
     */
    List<UserRecord> findByUserIdAndBattleType(Long userId, String battleType);

    /**
     * 根据用户ID和游戏模式查找
     */
    List<UserRecord> findByUserIdAndGameMode(Long userId, String gameMode);

    /**
     * 统计用户的胜利次数
     */
    @Query("SELECT COUNT(ur) FROM UserRecord ur WHERE ur.userId = :userId AND ur.result = 'WIN'")
    long countWinsByUserId(@Param("userId") Long userId);

    /**
     * 统计用户的失败次数
     */
    @Query("SELECT COUNT(ur) FROM UserRecord ur WHERE ur.userId = :userId AND ur.result = 'LOSE'")
    long countLosesByUserId(@Param("userId") Long userId);

    /**
     * 获取用户最近的对战记录
     */
    @Query("SELECT ur FROM UserRecord ur WHERE ur.userId = :userId ORDER BY ur.createTime DESC")
    List<UserRecord> findRecentRecordsByUserId(@Param("userId") Long userId);
}
