package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.BattleRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BattleRoundRepository extends JpaRepository<BattleRound, Long> {

    // ==================== 通用查询 ====================

    List<BattleRound> findByBattleIdOrderByRoundNumAsc(String battleId);

    List<BattleRound> findByBattleIdAndUserIdOrderByRoundNumAsc(String battleId, Long userId);

    List<BattleRound> findByBattleIdAndRoundNum(String battleId, Integer roundNum);

    boolean existsByBattleIdAndUserIdAndRoundNum(String battleId, Long userId, Integer roundNum);

    boolean existsByBattleIdAndPoemContent(String battleId, String poemContent);

    int countByBattleId(String battleId);

    // ==================== 人机/好友对战专用（roomId IS NULL） ====================

    /**
     * 根据对战ID和玩家ID查找答题记录（兼容旧 BattleDetailRepository 接口）
     */
    List<BattleRound> findByBattleIdAndUserId(String battleId, Long userId);

    /**
     * 统计某玩家在对战中的正确答题数
     */
    @Query("SELECT COUNT(br) FROM BattleRound br WHERE br.battleId = :battleId AND br.userId = :userId AND br.isCorrect = 1")
    long countCorrectAnswers(@Param("battleId") String battleId, @Param("userId") Long userId);

    /**
     * 统计某玩家在对战中的总答题数
     */
    @Query("SELECT COUNT(br) FROM BattleRound br WHERE br.battleId = :battleId AND br.userId = :userId")
    long countTotalAnswers(@Param("battleId") String battleId, @Param("userId") Long userId);

    /**
     * 查找某玩家使用的诗句（用于检测重复）
     */
    @Query("SELECT br.poemContent FROM BattleRound br WHERE br.battleId = :battleId AND br.userId = :userId")
    List<String> findPoemContentsByBattleIdAndUserId(@Param("battleId") String battleId, @Param("userId") Long userId);

    // ==================== 多人对战专用（roomId IS NOT NULL） ====================

    List<BattleRound> findByRoomIdOrderByRoundNumAsc(String roomId);

    List<BattleRound> findByBattleIdAndIsEliminatedTrueOrderByRoundNumAsc(String battleId);

    /**
     * 查询某对战中最近一轮的正确答案（用于接龙模式首尾校验）
     */
    Optional<BattleRound> findTopByBattleIdAndIsCorrectOrderByRoundNumDesc(String battleId, Integer isCorrect);

    /**
     * 查找某房间的待处理邀请（兼容 RoomInviteRepository）
     * 注：此方法实际在 BattleInviteRepository 中，此处保留仅为参考
     */
}
