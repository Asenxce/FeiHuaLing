package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.Battle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BattleRepository extends JpaRepository<Battle, Long> {

    /**
     * 根据battleId查找对战
     */
    Optional<Battle> findByBattleId(String battleId);

    /**
     * 查找用户创建的对战
     */
    List<Battle> findByCreatorId(Long creatorId);

    /**
     * 查找用户参与的对战（作为对手）
     */
    List<Battle> findByOpponentId(Long opponentId);

    /**
     * 查找用户相关的所有对战（创建者或对手）
     */
    List<Battle> findByCreatorIdOrOpponentId(Long creatorId, Long opponentId);

    /**
     * 查找进行中的对战
     */
    List<Battle> findByStatus(String status);

    /**
     * 查找指定创建者和对手的最新一场进行中好友对战
     */
    Optional<Battle> findTopByCreatorIdAndOpponentIdAndStatusAndBattleTypeOrderByCreateTimeDesc(
            Long creatorId, Long opponentId, String status, String battleType);

    /**
     * 查找指定两个用户之间的所有进行中好友对战
     */
    @Query("SELECT b FROM Battle b WHERE b.status = 'ONGOING' AND b.battleType = 'friend' " +
            "AND ((b.creatorId = :userId1 AND b.opponentId = :userId2) OR (b.creatorId = :userId2 AND b.opponentId = :userId1))")
    List<Battle> findOngoingFriendBattlesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 根据对战类型查找
     */
    List<Battle> findByBattleType(String battleType);

    /**
     * 检查battleId是否存在
     */
    boolean existsByBattleId(String battleId);
}
