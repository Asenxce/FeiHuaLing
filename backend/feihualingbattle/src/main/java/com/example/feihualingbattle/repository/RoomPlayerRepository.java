package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, Long> {

    List<RoomPlayer> findByRoomIdOrderByJoinOrderAsc(String roomId);

    Optional<RoomPlayer> findByRoomIdAndUserId(String roomId, Long userId);

    long countByRoomIdAndStatus(String roomId, String status);

    long countByRoomIdAndIsReadyTrue(String roomId);

    List<RoomPlayer> findByRoomIdAndStatusOrderByJoinOrderAsc(String roomId, String status);

    @Query("SELECT COUNT(rp) FROM RoomPlayer rp WHERE rp.roomId = :roomId AND rp.status != 'QUIT'")
    long countActivePlayers(@Param("roomId") String roomId);

    @Query("SELECT COUNT(rp) FROM RoomPlayer rp WHERE rp.roomId = :roomId AND rp.isReady = true AND rp.status != 'QUIT'")
    long countActiveReadyPlayers(@Param("roomId") String roomId);

    @Query("SELECT rp FROM RoomPlayer rp WHERE rp.roomId = :roomId AND rp.status != 'QUIT' ORDER BY rp.joinOrder ASC")
    List<RoomPlayer> findActivePlayersByRoomId(@Param("roomId") String roomId);

    @Modifying
    @Transactional
    @Query("UPDATE RoomPlayer rp SET rp.status = :status WHERE rp.roomId = :roomId AND rp.userId = :userId")
    int updatePlayerStatus(@Param("roomId") String roomId, @Param("userId") Long userId, @Param("status") String status);

    @Modifying
    @Transactional
    @Query("UPDATE RoomPlayer rp SET rp.faultCount = rp.faultCount - 1 WHERE rp.roomId = :roomId AND rp.userId = :userId")
    int decrementFaultCount(@Param("roomId") String roomId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE RoomPlayer rp SET rp.isHost = true WHERE rp.roomId = :roomId AND rp.userId = :userId")
    int transferHost(@Param("roomId") String roomId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE RoomPlayer rp SET rp.isReady = :ready WHERE rp.roomId = :roomId AND rp.userId = :userId")
    int updateReadyStatus(@Param("roomId") String roomId, @Param("userId") Long userId, @Param("ready") boolean ready);
}
