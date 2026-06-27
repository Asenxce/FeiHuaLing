package com.example.feihualingbattle.repository;

import com.example.feihualingbattle.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomId(String roomId);

    Optional<Room> findByRoomCode(String roomCode);

    List<Room> findByCreatorIdOrderByCreateTimeDesc(Long creatorId);

    List<Room> findByStatus(String status);

    List<Room> findByStatusAndExpireTimeBefore(String status, LocalDateTime time);

    Optional<Room> findByBattleId(String battleId);

    boolean existsByRoomCode(String roomCode);
}
