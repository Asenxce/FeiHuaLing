package com.example.feihualingbattle.service;

import com.example.feihualingbattle.dto.MultiBattleResult;
import com.example.feihualingbattle.dto.MultiBattleSubmitRequest;
import com.example.feihualingbattle.entity.*;
import com.example.feihualingbattle.enums.RoomPlayerStatus;
import com.example.feihualingbattle.enums.RoomStatus;
import com.example.feihualingbattle.repository.*;
import com.example.feihualingbattle.service.strategy.BattleServiceHelper;
import com.example.feihualingbattle.service.strategy.GameModeStrategy;
import com.example.feihualingbattle.service.strategy.GameModeStrategyFactory;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class MultiplayerBattleService {
    private static final Logger log = LoggerFactory.getLogger(MultiplayerBattleService.class);
    private static final String TURN_LOCK_PREFIX = "battle:turn:lock:";
    private static final String BATTLE_END_LOCK_PREFIX = "battle:end:lock:";
    private static final long LOCK_EXPIRE_SECONDS = 30;

    @Autowired
    private Gson gson;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomPlayerRepository roomPlayerRepository;

    @Autowired
    private BattleRepository battleRepository;

    @Autowired
    private BattleRoundRepository battleRoundRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRecordRepository userRecordRepository;

    @Autowired
    private PoetryRepository poetryRepository;

    @Autowired
    private WebSocketSessionManager webSocketSessionManager;

    @Autowired
    private GameModeStrategyFactory strategyFactory;

    @Autowired
    private BattleServiceHelper battleServiceHelper;

    @Autowired
    private RoomStateMachine stateMachine;

    @Autowired
    private BattleDedupService dedupService;

    @Transactional
    public Map<String, Object> startBattle(String roomId, Long userId) {
        log.info("startBattle: roomId={}, userId={}", roomId, userId);

        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        log.info("startBattle: roomStatus={}, creatorId={}, gameMode={}, keyword={}",
                room.getStatus(), room.getCreatorId(), room.getGameMode(), room.getKeyword());

        if (!room.getCreatorId().equals(userId)) {
            throw new RuntimeException("只有房主可以开始对局");
        }

        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        stateMachine.validateRoomTransition(roomStatus, RoomStatus.BATTLE, "开始对局");

        List<RoomPlayer> players = roomPlayerRepository.findActivePlayersByRoomId(roomId);
        long readyCount = players.stream().filter(RoomPlayer::getIsReady).count();
        if (players.size() < room.getMinPlayers() || readyCount != players.size()) {
            throw new RuntimeException("尚未达到开局条件");
        }

        String battleId = UUID.randomUUID().toString();

        Battle battle = new Battle();
        battle.setBattleId(battleId);
        battle.setBattleType("friend");
        battle.setBattleSubType("MULTI");
        battle.setGameMode(room.getGameMode());
        battle.setKeyword(room.getKeyword());
        battle.setKeyword2(room.getKeyword2());
        battle.setKeywordPosition(room.getKeywordPosition());
        battle.setColorKeyword(room.getColorKeyword());
        battle.setNumberKeyword(room.getNumberKeyword());
        battle.setForbiddenWord(room.getForbiddenWord());
        battle.setCreatorId(room.getCreatorId());
        battle.setRoomId(roomId);
        battle.setStatus("ONGOING");
        battle.setGameType(room.getGameType());
        battle.setTimeLimit(room.getTimeLimit());
        battle.setFaultLimit(room.getFaultLimit());
        battle.setTotalRounds(0);
        battle.setTotalPlayers(players.size());
        battle.setStartTime(LocalDateTime.now());
        battleRepository.save(battle);

        room.setStatus(RoomStatus.BATTLE.getValue());
        room.setBattleId(battleId);
        room.setCurrentTurnUserId(players.get(0).getUserId());
        roomRepository.save(room);

        for (RoomPlayer player : players) {
            stateMachine.validatePlayerTransition(
                    RoomPlayerStatus.fromValue(player.getStatus()), 
                    RoomPlayerStatus.BATTLE, 
                    "开始对局");
            player.setStatus(RoomPlayerStatus.BATTLE.getValue());
            player.setFaultCount(room.getFaultLimit());
            roomPlayerRepository.save(player);
        }

        User firstPlayer = userRepository.findById(players.get(0).getUserId()).orElse(null);
        String firstNickname = firstPlayer != null ? firstPlayer.getNickname() : "未知";

        String keywordInfo = buildKeywordInfo(room);

        webSocketSessionManager.broadcastToRoom(roomId,
                "BATTLE_START",
                "{\"battleId\":\"" + battleId + "\",\"keyword\":\"" + keywordInfo
                        + "\",\"timeLimit\":" + room.getTimeLimit()
                        + ",\"firstTurnUserId\":" + players.get(0).getUserId()
                        + ",\"firstTurnNickname\":\"" + firstNickname + "\""
                        + ",\"gameType\":\"" + room.getGameType() + "\""
                        + ",\"helpLimit\":" + 3 + "}");

        webSocketSessionManager.sendToUser(players.get(0).getUserId(),
                "TURN_NOTIFY",
                "轮到你了！请在" + room.getTimeLimit() + "秒内作答");

        Map<String, Object> result = new HashMap<>();
        result.put("battleId", battleId);
        result.put("roomId", roomId);
        result.put("keyword", keywordInfo);
        result.put("firstPlayerId", players.get(0).getUserId());
        result.put("firstPlayerNickname", firstNickname);
        return result;
    }

    @Transactional
    public Map<String, Object> submitAnswer(MultiBattleSubmitRequest request) {
        String battleId = request.getBattleId();
        String roomId = request.getRoomId();
        Long userId = request.getUserId();

        log.info("submitAnswer: battleId={}, userId={}", battleId, userId);

        // 获取 Redis 分布式锁，防止同一房间的并发轮次操作
        String turnLockKey = TURN_LOCK_PREFIX + roomId;
        if (!tryLock(turnLockKey)) {
            throw new RuntimeException("当前有操作正在进行中，请稍后重试");
        }

        try {
            return doSubmitAnswer(request, battleId, roomId, userId);
        } finally {
            unlock(turnLockKey);
        }
    }

    private Map<String, Object> doSubmitAnswer(MultiBattleSubmitRequest request,
                                                String battleId, String roomId, Long userId) {
        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));
        Battle battle = battleRepository.findByBattleId(battleId)
                .orElseThrow(() -> new RuntimeException("对战不存在"));

        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (roomStatus != RoomStatus.BATTLE) {
            throw new RuntimeException("对战未在进行中");
        }

        if (room.getCurrentTurnUserId() == null) {
            throw new RuntimeException("对战已结束");
        }
        if (!userId.equals(room.getCurrentTurnUserId())) {
            throw new RuntimeException("还没轮到你作答");
        }

        RoomPlayer player = roomPlayerRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("玩家不在该房间"));
        RoomPlayerStatus playerStatus = RoomPlayerStatus.fromValue(player.getStatus());
        if (playerStatus != RoomPlayerStatus.BATTLE) {
            throw new RuntimeException("你已被淘汰或不在对战中");
        }

        int nextRoundNum = battleRoundRepository.countByBattleId(battleId) + 1;
        long start = System.currentTimeMillis();

        boolean isCorrect = false;
        String poemContent = request.getPoemContent();

        if (poemContent != null && !poemContent.trim().isEmpty()) {
            String normalizedForDb = battleServiceHelper.normalizePoetry(poemContent);
            String errorMsg = null;

            if (dedupService.isDuplicate(battleId, normalizedForDb)) {
                errorMsg = "该诗句已经使用过，请换一句";
            }

            if (errorMsg == null && !isPureChinese(normalizedForDb)) {
                errorMsg = "答案只能包含中文汉字";
            }

            if (errorMsg == null) {
                GameModeStrategy strategy = strategyFactory.getStrategy(battle.getGameMode());
                String normalized = normalizePoem(poemContent);
                errorMsg = strategy.validate(battle, normalized);
            }

            if (errorMsg == null) {
                List<Poetry> matched = poetryRepository.findByExactContent(normalizedForDb);
                if (matched.isEmpty()) {
                    errorMsg = "该诗句不在诗库中，请检查是否正确";
                } else {
                    dedupService.markAsUsed(battleId, normalizedForDb);
                }
            }

            isCorrect = (errorMsg == null);
        }

        long end = System.currentTimeMillis();
        int timeUsed = (int) ((end - start) / 1000);

        boolean isEliminated = false;
        if (!isCorrect) {
            player.setFaultCount(player.getFaultCount() - 1);
            roomPlayerRepository.save(player);

            if (player.getFaultCount() <= 0) {
                stateMachine.validatePlayerTransition(playerStatus, RoomPlayerStatus.ELIMINATED, "容错耗尽淘汰");
                player.setStatus(RoomPlayerStatus.ELIMINATED.getValue());
                player.setEliminationRound(nextRoundNum);
                roomPlayerRepository.save(player);
                isEliminated = true;
            }
        }

        List<RoomPlayer> alivePlayers = roomPlayerRepository.findByRoomIdAndStatusOrderByJoinOrderAsc(
                roomId, RoomPlayerStatus.BATTLE.getValue());

        BattleRound round = new BattleRound();
        round.setBattleId(battleId);
        round.setRoomId(roomId);
        round.setRoundNum(nextRoundNum);
        round.setUserId(userId);
        round.setPoemContent(poemContent);
        round.setIsCorrect(isCorrect ? 1 : 0);
        round.setTimeUsed(timeUsed);
        round.setFaultAfter(player.getFaultCount());
        round.setIsTimeout(false);
        round.setIsEliminated(isEliminated);
        round.setSurvivorCount(alivePlayers.size());
        battleRoundRepository.save(round);

        battle.setTotalRounds(nextRoundNum);
        battleRepository.save(battle);

        User currentUser = userRepository.findById(userId).orElse(null);
        String userNickname = currentUser != null ? currentUser.getNickname() : "未知";

        if (isEliminated) {
            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_ELIMINATED",
                    "{\"userId\":" + userId + ",\"nickname\":\"" + userNickname
                            + "\",\"roundNum\":" + nextRoundNum + ",\"reason\":\"容错次数耗尽\"}");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("isEliminated", isEliminated);
        result.put("roundNum", nextRoundNum);
        result.put("faultRemaining", player.getFaultCount());
        result.put("survivorCount", alivePlayers.size());

        int aliveCount = alivePlayers.size();
        boolean isLastElimination = aliveCount <= 1;

        RoomPlayer nextPlayer = null;
        String nextNickname = "";
        if (!isLastElimination) {
            nextPlayer = getNextAlivePlayer(roomId, userId, alivePlayers);
        }

        if (isLastElimination || nextPlayer == null) {
            if (nextPlayer == null && !isLastElimination) {
                log.warn("submitAnswer: 无法找到下一活跃玩家, roomId={}, currentUserId={}, aliveCount={}",
                        roomId, userId, aliveCount);
            }
            webSocketSessionManager.broadcastToRoom(roomId,
                    "ROUND_RESULT",
                    "{\"userId\":" + userId + ",\"nickname\":\"" + userNickname
                            + "\",\"isCorrect\":" + isCorrect
                            + ",\"isEliminated\":" + isEliminated
                            + ",\"roundNum\":" + nextRoundNum
                            + ",\"faultRemaining\":" + player.getFaultCount()
                            + ",\"poemContent\":\"" + escapeJson(poemContent)
                            + "\",\"nextPlayerId\":0,\"nextPlayerNickname\":\"\"}");

            Map<String, Object> battleResult = endBattle(battleId, roomId);
            result.put("battleEnd", battleResult);
            return result;
        }

        room.setCurrentTurnUserId(nextPlayer.getUserId());
        roomRepository.save(room);

        User nextUser = userRepository.findById(nextPlayer.getUserId()).orElse(null);
        nextNickname = nextUser != null ? nextUser.getNickname() : "未知";

        result.put("nextPlayerId", nextPlayer.getUserId());
        result.put("nextPlayerNickname", nextNickname);

        webSocketSessionManager.broadcastToRoom(roomId,
                "ROUND_RESULT",
                "{\"userId\":" + userId + ",\"nickname\":\"" + userNickname
                        + "\",\"isCorrect\":" + isCorrect
                        + ",\"isEliminated\":" + isEliminated
                        + ",\"roundNum\":" + nextRoundNum
                        + ",\"faultRemaining\":" + player.getFaultCount()
                        + ",\"poemContent\":\"" + escapeJson(poemContent)
                        + "\",\"nextPlayerId\":" + nextPlayer.getUserId()
                        + ",\"nextPlayerNickname\":\"" + nextNickname + "\"}");

        webSocketSessionManager.sendToUser(nextPlayer.getUserId(),
                "TURN_NOTIFY", "轮到你了！请在" + room.getTimeLimit() + "秒内作答");

        return result;
    }

    @Transactional
    public void handleTimeout(String battleId, String roomId, Long userId) {
        log.info("handleTimeout: battleId={}, userId={}", battleId, userId);

        // 获取 Redis 分布式锁，防止超时处理与提交答案并发冲突
        String turnLockKey = TURN_LOCK_PREFIX + roomId;
        if (!tryLock(turnLockKey)) {
            log.warn("handleTimeout: 无法获取锁, roomId={}, userId={}", roomId, userId);
            return;
        }

        try {
            doHandleTimeout(battleId, roomId, userId);
        } finally {
            unlock(turnLockKey);
        }
    }

    private void doHandleTimeout(String battleId, String roomId, Long userId) {
        Room room = roomRepository.findByRoomId(roomId).orElse(null);
        if (room == null || !userId.equals(room.getCurrentTurnUserId())) return;

        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (roomStatus != RoomStatus.BATTLE) return;

        RoomPlayer player = roomPlayerRepository.findByRoomIdAndUserId(roomId, userId).orElse(null);
        if (player == null) return;
        
        RoomPlayerStatus playerStatus = RoomPlayerStatus.fromValue(player.getStatus());
        if (playerStatus != RoomPlayerStatus.BATTLE) return;

        int nextRoundNum = battleRoundRepository.countByBattleId(battleId) + 1;

        player.setFaultCount(player.getFaultCount() - 1);
        boolean isEliminated = false;
        if (player.getFaultCount() <= 0) {
            stateMachine.validatePlayerTransition(playerStatus, RoomPlayerStatus.ELIMINATED, "超时淘汰");
            player.setStatus(RoomPlayerStatus.ELIMINATED.getValue());
            player.setEliminationRound(nextRoundNum);
            isEliminated = true;
        }
        roomPlayerRepository.save(player);

        List<RoomPlayer> alivePlayers = roomPlayerRepository.findByRoomIdAndStatusOrderByJoinOrderAsc(
                roomId, RoomPlayerStatus.BATTLE.getValue());

        BattleRound round = new BattleRound();
        round.setBattleId(battleId);
        round.setRoomId(roomId);
        round.setRoundNum(nextRoundNum);
        round.setUserId(userId);
        round.setIsCorrect(null);
        round.setTimeUsed(room.getTimeLimit());
        round.setFaultAfter(player.getFaultCount());
        round.setIsTimeout(true);
        round.setIsEliminated(isEliminated);
        round.setSurvivorCount(alivePlayers.size());
        battleRoundRepository.save(round);

        Battle battle = battleRepository.findByBattleId(battleId).orElse(null);
        if (battle != null) {
            battle.setTotalRounds(nextRoundNum);
            battleRepository.save(battle);
        }

        User timeoutUser = userRepository.findById(userId).orElse(null);
        String nickname = timeoutUser != null ? timeoutUser.getNickname() : "未知";

        boolean isLastAlive = alivePlayers.size() <= 1;

        RoomPlayer nextPlayer = null;
        String nextNickname = "";
        if (!isLastAlive) {
            nextPlayer = getNextAlivePlayer(roomId, userId, alivePlayers);
        }

        if (isLastAlive || nextPlayer == null) {
            if (nextPlayer == null && !isLastAlive) {
                log.warn("handleTimeout: 无法找到下一活跃玩家, roomId={}, currentUserId={}, aliveCount={}",
                        roomId, userId, alivePlayers.size());
            }

            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_TIMEOUT",
                    "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                            + "\",\"faultRemaining\":" + player.getFaultCount()
                            + ",\"isEliminated\":" + isEliminated
                            + ",\"roundNum\":" + nextRoundNum
                            + ",\"nextPlayerId\":0,\"nextPlayerNickname\":\"\"}");

            if (isEliminated) {
                webSocketSessionManager.broadcastToRoom(roomId,
                        "PLAYER_ELIMINATED",
                        "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                                + "\",\"roundNum\":" + nextRoundNum + ",\"reason\":\"超时\"}");
            }

            endBattle(battleId, roomId);
            return;
        }

        User nextUser = userRepository.findById(nextPlayer.getUserId()).orElse(null);
        nextNickname = nextUser != null ? nextUser.getNickname() : "未知";

        webSocketSessionManager.broadcastToRoom(roomId,
                "PLAYER_TIMEOUT",
                "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                        + "\",\"faultRemaining\":" + player.getFaultCount()
                        + ",\"isEliminated\":" + isEliminated
                        + ",\"roundNum\":" + nextRoundNum
                        + ",\"nextPlayerId\":" + nextPlayer.getUserId()
                        + ",\"nextPlayerNickname\":\"" + nextNickname + "\"}");

        if (isEliminated) {
            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_ELIMINATED",
                    "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                            + "\",\"roundNum\":" + nextRoundNum + ",\"reason\":\"超时\"}");
        }

        room.setCurrentTurnUserId(nextPlayer.getUserId());
        roomRepository.save(room);
        webSocketSessionManager.sendToUser(nextPlayer.getUserId(),
                "TURN_NOTIFY", "轮到你了！请在" + room.getTimeLimit() + "秒内作答");
    }

    @Transactional
    public Map<String, Object> handleSurrender(String battleId, String roomId, Long userId) {
        log.info("handleSurrender: battleId={}, userId={}", battleId, userId);

        Room room = roomRepository.findByRoomId(roomId).orElse(null);
        if (room == null) throw new RuntimeException("房间不存在");

        RoomStatus roomStatus = RoomStatus.fromValue(room.getStatus());
        if (roomStatus != RoomStatus.BATTLE) {
            log.warn("handleSurrender: 对战未在进行中, roomId={}, roomStatus={}", roomId, roomStatus);
            return Map.of("surrendered", false);
        }

        RoomPlayer player = roomPlayerRepository.findByRoomIdAndUserId(roomId, userId).orElse(null);
        if (player == null) {
            log.warn("handleSurrender: 玩家不在房间中, roomId={}, userId={}", roomId, userId);
            return Map.of("surrendered", false);
        }

        RoomPlayerStatus playerStatus = RoomPlayerStatus.fromValue(player.getStatus());
        if (playerStatus != RoomPlayerStatus.BATTLE) {
            log.warn("handleSurrender: 玩家不在对战中, roomId={}, userId={}, status={}", roomId, userId, playerStatus);
            return Map.of("surrendered", false);
        }

        int nextRoundNum = battleRoundRepository.countByBattleId(battleId) + 1;

        stateMachine.validatePlayerTransition(playerStatus, RoomPlayerStatus.ELIMINATED, "认输出局");
        player.setStatus(RoomPlayerStatus.ELIMINATED.getValue());
        player.setEliminationRound(nextRoundNum);
        roomPlayerRepository.save(player);

        List<RoomPlayer> alivePlayers = roomPlayerRepository.findByRoomIdAndStatusOrderByJoinOrderAsc(
                roomId, RoomPlayerStatus.BATTLE.getValue());

        BattleRound round = new BattleRound();
        round.setBattleId(battleId);
        round.setRoomId(roomId);
        round.setRoundNum(nextRoundNum);
        round.setUserId(userId);
        round.setPoemContent(null);
        round.setIsCorrect(null);
        round.setTimeUsed(0);
        round.setFaultAfter(player.getFaultCount());
        round.setIsTimeout(false);
        round.setIsEliminated(true);
        round.setSurvivorCount(alivePlayers.size());
        battleRoundRepository.save(round);

        Battle battle = battleRepository.findByBattleId(battleId).orElse(null);
        if (battle != null) {
            battle.setTotalRounds(nextRoundNum);
            battleRepository.save(battle);
        }

        User surrenderUser = userRepository.findById(userId).orElse(null);
        String nickname = surrenderUser != null ? surrenderUser.getNickname() : "未知";

        Map<String, Object> result = new HashMap<>();
        result.put("surrendered", true);
        result.put("isEliminated", true);
        result.put("roundNum", nextRoundNum);

        boolean isLastAlive = alivePlayers.size() <= 1;

        if (isLastAlive) {
            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_ELIMINATED",
                    "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                            + "\",\"roundNum\":" + nextRoundNum + ",\"reason\":\"认输出局\"}");

            Map<String, Object> battleResult = endBattle(battleId, roomId);
            result.put("battleEnd", battleResult);
            return result;
        }

        Long currentTurnUserId = room.getCurrentTurnUserId();
        if (userId.equals(currentTurnUserId)) {
            RoomPlayer nextPlayer = getNextAlivePlayer(roomId, userId, alivePlayers);
            if (nextPlayer == null) {
                log.warn("handleSurrender: 无法找到下一活跃玩家");

                webSocketSessionManager.broadcastToRoom(roomId,
                        "PLAYER_ELIMINATED",
                        "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                                + "\",\"roundNum\":" + nextRoundNum + ",\"reason\":\"认输出局\"}");

                endBattle(battleId, roomId);
                return result;
            }

            User nextUser = userRepository.findById(nextPlayer.getUserId()).orElse(null);
            String nextNickname = nextUser != null ? nextUser.getNickname() : "未知";

            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_ELIMINATED",
                    "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                            + "\",\"roundNum\":" + nextRoundNum + ",\"reason\":\"认输出局\""
                            + ",\"nextPlayerId\":" + nextPlayer.getUserId()
                            + ",\"nextPlayerNickname\":\"" + nextNickname + "\"}");

            room.setCurrentTurnUserId(nextPlayer.getUserId());
            roomRepository.save(room);
            webSocketSessionManager.sendToUser(nextPlayer.getUserId(),
                    "TURN_NOTIFY", "轮到你了！请在" + room.getTimeLimit() + "秒内作答");
            result.put("nextPlayerId", nextPlayer.getUserId());
        } else {
            webSocketSessionManager.broadcastToRoom(roomId,
                    "PLAYER_ELIMINATED",
                    "{\"userId\":" + userId + ",\"nickname\":\"" + nickname
                            + "\",\"roundNum\":" + nextRoundNum + ",\"reason\":\"认输出局\"}");
        }

        return result;
    }

    @Transactional
    public Map<String, Object> endBattle(String battleId, String roomId) {
        log.info("endBattle: battleId={}, roomId={}", battleId, roomId);

        // 竞争条件保护：使用 Redis 锁防止 endBattle 被并发调用（如超时和答题同时触发）
        String endLockKey = BATTLE_END_LOCK_PREFIX + battleId;
        if (!tryLock(endLockKey)) {
            log.warn("endBattle: 无法获取结束锁, battleId={}, 可能已由其他线程处理", battleId);
            return Map.of("battleId", battleId, "alreadyEnded", true);
        }

        try {
            return doEndBattle(battleId, roomId);
        } finally {
            unlock(endLockKey);
        }
    }

    private Map<String, Object> doEndBattle(String battleId, String roomId) {
        Battle battle = battleRepository.findByBattleId(battleId)
                .orElseThrow(() -> new RuntimeException("对战不存在"));
        Room room = roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        // 竞争条件保护：如果对战已经结束，直接返回，避免双重结算
        if ("COMPLETED".equals(battle.getStatus())) {
            log.info("endBattle: 对战已结束, battleId={}, 跳过重复结算", battleId);
            return Map.of("battleId", battleId, "winnerId", battle.getWinnerId() != null ? battle.getWinnerId() : 0L, "alreadyEnded", true);
        }

        List<RoomPlayer> allPlayers = roomPlayerRepository.findByRoomIdOrderByJoinOrderAsc(roomId);
        List<RoomPlayer> survivors = allPlayers.stream()
                .filter(p -> RoomPlayerStatus.fromValue(p.getStatus()) == RoomPlayerStatus.BATTLE)
                .collect(Collectors.toList());
        List<RoomPlayer> eliminated = allPlayers.stream()
                .filter(p -> RoomPlayerStatus.fromValue(p.getStatus()) == RoomPlayerStatus.ELIMINATED)
                .sorted(Comparator.comparing(RoomPlayer::getEliminationRound,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        Long winnerId = survivors.isEmpty() ? null : survivors.get(0).getUserId();

        battle.setWinnerId(winnerId);
        battle.setStatus("COMPLETED");
        battle.setEndTime(LocalDateTime.now());
        battleRepository.save(battle);

        stateMachine.validateRoomTransition(RoomStatus.BATTLE, RoomStatus.FINISHED, "对战结束");
        room.setStatus(RoomStatus.FINISHED.getValue());
        room.setCurrentTurnUserId(null);
        roomRepository.save(room);

        Duration duration = Duration.between(battle.getStartTime(), battle.getEndTime());

        MultiBattleResult result = new MultiBattleResult();
        result.setBattleId(battleId);
        result.setRoomId(roomId);
        result.setTotalRounds(battle.getTotalRounds());
        result.setDuration(duration.getSeconds());
        result.setStartTime(battle.getStartTime());
        result.setEndTime(battle.getEndTime());

        List<MultiBattleResult.PlayerResult> playerResults = new ArrayList<>();

        int rank = 1;
        if (winnerId != null) {
            MultiBattleResult.PlayerResult pr = buildPlayerResult(winnerId, battleId, rank,
                    allPlayers.indexOf(survivors.get(0)) + 1, false, null);
            playerResults.add(pr);
            rank++;
        }
        for (RoomPlayer ep : eliminated) {
            MultiBattleResult.PlayerResult pr = buildPlayerResult(ep.getUserId(), battleId, rank,
                    allPlayers.indexOf(ep) + 1, true, ep.getEliminationRound());
            playerResults.add(pr);
            rank++;
        }
        result.setPlayers(playerResults);

        for (RoomPlayer p : allPlayers) {
            RoomPlayerStatus pStatus = RoomPlayerStatus.fromValue(p.getStatus());
            if (pStatus == RoomPlayerStatus.BATTLE) {
                stateMachine.validatePlayerTransition(pStatus, RoomPlayerStatus.FINISHED, "对战结束");
                p.setStatus(RoomPlayerStatus.FINISHED.getValue());
                roomPlayerRepository.save(p);
            }
        }

        for (MultiBattleResult.PlayerResult pr : playerResults) {
            UserRecord record = new UserRecord();
            record.setUserId(pr.getUserId());
            record.setBattleId(battleId);
            record.setBattleType("multi");
            record.setGameMode(battle.getGameMode());
            record.setGameModeName(getGameModeName(battle.getGameMode()));
            record.setKeyword(battle.getKeyword());
            record.setOpponentId(winnerId != null && !winnerId.equals(pr.getUserId()) ? winnerId : 0L);
            record.setOpponentName("多人对战");
            record.setResult(pr.getRank() == 1 ? "WIN" : "LOSE");
            record.setRank((short) pr.getRank());
            record.setScore((short) pr.getScore());
            record.setCorrectCount((short) pr.getCorrectCount());
            record.setWrongCount((short) pr.getWrongCount());
            record.setTotalCount((short) pr.getTotalCount());
            record.setAccuracy(pr.getAccuracy() != null ? java.math.BigDecimal.valueOf(pr.getAccuracy()).setScale(2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
            record.setTotalRounds(battle.getTotalRounds() != null ? battle.getTotalRounds().shortValue() : (short) 0);
            if (battle.getStartTime() != null) {
                java.time.LocalDateTime endTime = battle.getEndTime() != null ? battle.getEndTime() : java.time.LocalDateTime.now();
                long dur = java.time.Duration.between(battle.getStartTime(), endTime).getSeconds();
                record.setDuration((int) dur);
            }
            userRecordRepository.save(record);
        }

        String json = gson.toJson(result);
        webSocketSessionManager.broadcastToRoom(roomId, "BATTLE_END", json);

        Map<String, Object> map = new HashMap<>();
        map.put("battleId", battleId);
        map.put("winnerId", winnerId);
        map.put("duration", duration.getSeconds());
        map.put("totalRounds", battle.getTotalRounds());
        map.put("players", playerResults);
        return map;
    }

    private MultiBattleResult.PlayerResult buildPlayerResult(Long userId, String battleId, int rank,
                                                              int joinOrder, boolean isEliminated, Integer eliminationRound) {
        MultiBattleResult.PlayerResult pr = new MultiBattleResult.PlayerResult();
        pr.setUserId(userId);
        pr.setRank(rank);
        pr.setEliminated(isEliminated);
        pr.setEliminationRound(eliminationRound);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            pr.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            pr.setAvatarUrl(user.getAvatarUrl());
        }

        List<BattleRound> userRounds = battleRoundRepository.findByBattleIdAndUserIdOrderByRoundNumAsc(battleId, userId);
        int correct = (int) userRounds.stream().filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 1).count();
        int wrong = (int) userRounds.stream().filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 0).count();
        int total = correct + wrong;
        double accuracy = total > 0 ? (double) correct / total * 100 : 0;
        double avgTime = userRounds.stream()
                .filter(r -> r.getTimeUsed() != null)
                .mapToInt(BattleRound::getTimeUsed)
                .average().orElse(0);
        Integer fastest = userRounds.stream()
                .filter(r -> r.getTimeUsed() != null)
                .mapToInt(BattleRound::getTimeUsed)
                .min().orElse(0);

        pr.setCorrectCount(correct);
        pr.setWrongCount(wrong);
        pr.setTotalCount(total);
        pr.setAccuracy(Math.round(accuracy * 100.0) / 100.0);
        pr.setAvgTimeUsed(Math.round(avgTime * 100.0) / 100.0);
        pr.setFastestTime(fastest);

        int score = calculateCompetitiveScore(userId, battleId, rank, isEliminated);
        pr.setScore(score);

        return pr;
    }

    private int calculateCompetitiveScore(Long userId, String battleId, int rank, boolean isEliminated) {
        Battle battle = battleRepository.findByBattleId(battleId).orElse(null);
        boolean isCompetitive = battle != null && "COMPETITIVE".equals(battle.getGameType());
        if (!isCompetitive) return 0;

        int score = 0;

        score += Math.max(1, 12 - rank);

        if (!isEliminated) {
            score += 3;
        }

        List<BattleRound> rounds = battleRoundRepository.findByBattleIdAndUserIdOrderByRoundNumAsc(battleId, userId);
        int correct = (int) rounds.stream().filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 1).count();
        int total = (int) rounds.stream().filter(r -> r.getIsCorrect() != null).count();
        double accuracy = total > 0 ? (double) correct / total : 0;

        if (accuracy >= 0.8) score += 2;
        else if (accuracy >= 0.5) score += 1;

        double avgTime = rounds.stream()
                .filter(r -> r.getTimeUsed() != null)
                .mapToInt(BattleRound::getTimeUsed)
                .average().orElse(0);
        if (avgTime > 0 && avgTime < 15) score += 2;
        else if (avgTime > 0 && avgTime < 30) score += 1;

        if (rank == 1) score += 3;

        return score;
    }

    private RoomPlayer getNextAlivePlayer(String roomId, Long currentUserId, List<RoomPlayer> alivePlayers) {
        if (alivePlayers.isEmpty()) return null;

        List<RoomPlayer> allOrdered = roomPlayerRepository.findByRoomIdOrderByJoinOrderAsc(roomId);

        int currentPos = -1;
        for (int i = 0; i < allOrdered.size(); i++) {
            if (allOrdered.get(i).getUserId().equals(currentUserId)) {
                currentPos = i;
                break;
            }
        }

        if (currentPos < 0) return alivePlayers.get(0);

        for (int i = 1; i < allOrdered.size(); i++) {
            int idx = (currentPos + i) % allOrdered.size();
            RoomPlayer candidate = allOrdered.get(idx);
            if (RoomPlayerStatus.fromValue(candidate.getStatus()) == RoomPlayerStatus.BATTLE) {
                return candidate;
            }
        }

        return alivePlayers.get(0);
    }

    private String normalizePoem(String poem) {
        if (poem == null) return "";
        return poem.replaceAll("[，。！？、；：\u201c\u201d''（）《》\\[\\]【】\\s\u3000]", "")
                .trim();
    }

    private boolean isPureChinese(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '\u4e00' || c > '\u9fff') {
                return false;
            }
        }
        return true;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 尝试获取 Redis 分布式锁
     * @return true 表示获取成功，false 表示锁已被占用
     */
    private boolean tryLock(String lockKey) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 释放 Redis 分布式锁
     */
    private void unlock(String lockKey) {
        redisTemplate.delete(lockKey);
    }

    private String buildKeywordInfo(Room room) {
        switch (room.getGameMode()) {
            case "SIMPLE": return room.getKeyword();
            case "POSITION": return room.getKeyword() + "(第" + room.getKeywordPosition() + "字)";
            case "DOUBLE_KEYWORD": return room.getKeyword() + " + " + room.getKeyword2();
            case "CHAIN": return "首尾接龙飞花令";
            case "COLOR": return "颜色飞花令-" + (room.getColorKeyword() != null ? room.getColorKeyword() : "任意");
            case "NUMBER": return "数字飞花令-" + (room.getNumberKeyword() != null ? room.getNumberKeyword() : "任意");
            case "FORBIDDEN": return "反飞花令-禁" + room.getForbiddenWord();
            case "CUSTOM": return "自定义飞花令";
            default: return room.getKeyword();
        }
    }

    @Transactional
    public void checkAndEndIdleBattles() {
        List<Room> battlingRooms = roomRepository.findByStatus(RoomStatus.BATTLE.getValue());
        LocalDateTime now = LocalDateTime.now();

        for (Room room : battlingRooms) {
            List<BattleRound> rounds = battleRoundRepository.findByRoomIdOrderByRoundNumAsc(room.getRoomId());
            if (!rounds.isEmpty()) {
                BattleRound lastRound = rounds.get(rounds.size() - 1);
                if (Duration.between(lastRound.getCreateTime(), now).getSeconds() > 60) {
                    endBattle(room.getBattleId(), room.getRoomId());
                    log.info("checkAndEndIdleBattles: 解散空闲对局 roomId={}", room.getRoomId());
                }
            }
        }
    }

    private String getGameModeName(String gameMode) {
        if (gameMode == null) return "未知模式";
        switch (gameMode.toUpperCase()) {
            case "SIMPLE": case "SINGLE_KEYWORD": return "单关键字飞花令";
            case "POSITION": return "位置飞花令";
            case "DOUBLE_KEYWORD": return "双关键字飞花令";
            case "CHAIN": return "首尾接龙飞花令";
            case "COLOR": return "颜色飞花令";
            case "NUMBER": return "数字飞花令";
            case "FORBIDDEN": return "反飞花令";
            case "CUSTOM": return "自定义飞花令";
            default: return gameMode;
        }
    }
}
