package com.example.feihualingbattle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class ScheduledTaskService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private RoomService roomService;

    @Autowired
    private MultiplayerBattleService multiplayerBattleService;

    @Scheduled(fixedRate = 30000)
    public void dismissExpiredRooms() {
        try {
            roomService.dismissExpiredRooms();
        } catch (Exception e) {
            log.error("清理过期房间失败: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 15000)
    public void checkIdleBattles() {
        try {
            multiplayerBattleService.checkAndEndIdleBattles();
        } catch (Exception e) {
            log.error("检查空闲对局失败: {}", e.getMessage());
        }
    }
}
