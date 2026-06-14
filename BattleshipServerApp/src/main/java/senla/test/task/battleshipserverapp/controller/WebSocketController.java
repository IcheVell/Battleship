package senla.test.task.battleshipserverapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import enums.PlayerRole;
import enums.TypeSession;
import enums.AdminAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import request.*;
import response.*;
import senla.test.task.battleshipserverapp.model.GameSession;
import senla.test.task.battleshipserverapp.model.Player;
import senla.test.task.battleshipserverapp.service.MultiplayerService;
import senla.test.task.battleshipserverapp.service.PlayerService;
import senla.test.task.battleshipserverapp.service.SingleplayerService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketController extends TextWebSocketHandler {
    private final ConcurrentHashMap<String, WebSocketSession> userSessionRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> userToGameId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> gameObservers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> gamePlayers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PlayerService playerService;
    private final MultiplayerService multiplayerService;
    private final SingleplayerService singleplayerService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username == null) return;

        userSessionRegistry.remove(username);
        Long gameSessionId = userToGameId.remove(username);

        if (gameSessionId == null) {
            gameSessionId = multiplayerService.findActiveGameSessionIdByUsername(username);
        }

        if (gameSessionId != null) {
            String winnerUsername = multiplayerService.finishGameDueToDisconnect(gameSessionId, username);
            if (winnerUsername != null) {
                broadcastWin(gameSessionId, winnerUsername);
            }
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message: " + payload);

        BaseRequest baseRequest = objectMapper.readValue(payload, BaseRequest.class);

        switch (baseRequest) {
            case LoginRequest loginRequest -> {
                String username = loginRequest.getUsername();
                if (username.equals("AI_BOT")) {
                    session.sendMessage(new TextMessage("You can't use this reserved name"));
                    break;
                }

                Player player = playerService.login(loginRequest.getUsername());
                session.getAttributes().put("username", player.getUsername());
                userSessionRegistry.put(player.getUsername(), session);
            }

            case CreateGameRequest createGameRequest -> {
                String username = (String) session.getAttributes().get("username");
                if (username == null) throw new Exception("Invalid username");

                GameSession gameSession = createGameRequest.getType().equals(TypeSession.MULTIPLAYER) ? multiplayerService.createGameSession(username) : singleplayerService.createGameSession(username);
                userToGameId.put(username, gameSession.getId());
                gamePlayers.computeIfAbsent(gameSession.getId(), k -> new CopyOnWriteArraySet<>()).add(session);

                CreateGameResponse createGameResponse = new CreateGameResponse(gameSession.getId());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(createGameResponse)));

                broadcastGameState(gameSession.getId());

                if (createGameRequest.getType().equals(TypeSession.SINGLEPLAYER)) {
                    broadcastShipPlacementEvent(gameSession.getId());
                }
            }

            case FireRequest fireRequest -> {
                String username = (String) session.getAttributes().get("username");
                if (username == null) break;

                Long gameSessionId = userToGameId.get(username);
                if (gameSessionId == null) break;

                try {
                    boolean isSingle = singleplayerService.isSingleplayerSession(gameSessionId);
                    if (isSingle) {
                        singleplayerService.updateGameSession(gameSessionId, username, fireRequest.getTargetX(), fireRequest.getTargetY());
                        broadcastGameState(gameSessionId);

                        java.util.concurrent.CompletableFuture.runAsync(() -> {
                            try {
                                boolean botHits = true;

                                while (botHits) {
                                    botHits = singleplayerService.executeBotTurnStep(gameSessionId);
                                    broadcastGameState(gameSessionId);
                                }
                            } catch (Exception e) {
                                log.error("Bot turn error: {}", e.getMessage());
                            }
                        });
                    } else {
                        multiplayerService.updateGameSession(gameSessionId, username, fireRequest.getTargetX(), fireRequest.getTargetY());
                        broadcastGameState(gameSessionId);
                    }
                } catch (RuntimeException e) {
                    log.warn("Fire request rejected for user {}: {}", username, e.getMessage());
                }
            }

            case ActiveGamesRequest ignored -> {
                ActiveGamesResponse activeGamesResponse = new ActiveGamesResponse(multiplayerService.getGamesList());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(activeGamesResponse)));
            }

            case ConnectToGameRequest connectToGameRequest -> {
                Long gameSessionId = connectToGameRequest.getGameSessionId();
                if (connectToGameRequest.getRole() == PlayerRole.OBSERVER) {
                    gameObservers.computeIfAbsent(gameSessionId, k -> new CopyOnWriteArraySet<>()).add(session);
                    GameStateObserverResponse response = multiplayerService.getGameStateForObserver(gameSessionId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                }

                if (connectToGameRequest.getRole() == PlayerRole.PLAYER) {
                    String username = (String) session.getAttributes().get("username");
                    if (username == null) throw new Exception("Username is null!");

                    try {
                        multiplayerService.connectAsPlayer(gameSessionId, username);

                        userToGameId.put(username, gameSessionId);
                        gamePlayers.computeIfAbsent(gameSessionId, k -> new CopyOnWriteArraySet<>()).add(session);

                        ConnectToGameResponse connectResponse = new ConnectToGameResponse();
                        connectResponse.setGameSessionId(gameSessionId);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connectResponse)));

                        broadcastGameState(gameSessionId);
                        broadcastShipPlacementEvent(gameSessionId);
                    } catch (RuntimeException e) {
                        log.warn("Player {} failed to connect: {}", username, e.getMessage());
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new ConnectToGameResponse())));
                    }
                }
            }

            case ShipPlacementRequest shipPlacementRequest -> {
                String username = (String) session.getAttributes().get("username");
                if (username == null) throw new Exception("Username is null!");

                Long activeGameId = userToGameId.get(username);
                if (activeGameId == null || !activeGameId.equals(shipPlacementRequest.getGameSessionId())) {
                    throw new Exception("Not your game");
                }

                try {
                    boolean isSingle = singleplayerService.isSingleplayerSession(activeGameId);
                    if (isSingle) {
                        singleplayerService.setupShips(activeGameId, username, shipPlacementRequest.getShips());
                        broadcastGameState(activeGameId);
                    } else {
                        multiplayerService.setupShips(activeGameId, username, shipPlacementRequest.getShips());

                        if (multiplayerService.setupIsReady(activeGameId)) {
                            broadcastGameState(activeGameId);
                        }
                    }

                } catch (RuntimeException e) {
                    log.warn("Ship placement failed for {}: {}", username, e.getMessage());
                }
            }

            case SurrenderRequest surrenderRequest -> {
                String username = (String) session.getAttributes().get("username");

                if (username == null) throw new Exception("Username is null!");

                Long gameSessionId = userToGameId.get(username);

                if (gameSessionId == null || !gameSessionId.equals(surrenderRequest.getGameSessionId())) {
                    throw new Exception("Not your game");
                }

                String winner = multiplayerService.surrender(gameSessionId, username);

                if  (winner == null) {
                    closeGameSession(gameSessionId);
                } else {
                    broadcastWin(gameSessionId, winner);
                }
            }

            case AdminGameHistoryRequest adminHistoryRequest -> {
                AdminGameHistoryResponse historyResponse = multiplayerService.getAdminGameHistory(adminHistoryRequest.getPage(), adminHistoryRequest.getSize());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(historyResponse)));
            }

            case AdminActionRequest adminActionRequest -> {
                Long targetId = adminActionRequest.getGameSessionId();
                AdminAction action = adminActionRequest.getAdminAction();

                if (action == AdminAction.WATCH) {
                    AdminActionWatchResponse watchResponse = multiplayerService.getWatchGameInfo(targetId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(watchResponse)));
                } else if (action == AdminAction.DELETE) {
                    broadcastWin(targetId, "МАТЧ УДАЛЕН АДМИНИСТРАТОРОМ");
                    multiplayerService.deleteGameSession(targetId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new AdminActionDeleteResponse())));
                } else if (action == AdminAction.ARCHIVE) {
                    multiplayerService.archiveGameSession(targetId);
                    broadcastWin(targetId, "МАТЧ АРХИВИРОВАН АДМИНИСТРАТОРОМ");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new AdminActionDeleteResponse())));
                }
            }

            default -> throw new IllegalStateException("Unexpected value: " + baseRequest);
        }
    }

    private void broadcastGameState(Long gameSessionId) {
        Set<WebSocketSession> players = gamePlayers.get(gameSessionId);
        Set<WebSocketSession> observers = gameObservers.get(gameSessionId);

        if (players == null || players.isEmpty()) return;

        try {
            for (WebSocketSession playerSocket : players) {
                if (!playerSocket.isOpen()) continue;

                String username = (String) playerSocket.getAttributes().get("username");
                if (username != null) {
                    GameStatePlayerResponse response = multiplayerService.getGameStateForPlayer(gameSessionId, username);
                    playerSocket.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

                    if (multiplayerService.isPlayerTurn(gameSessionId, username)) {
                        ShootResponse shootResponse = new ShootResponse();
                        shootResponse.setGameSessionId(gameSessionId);
                        playerSocket.sendMessage(new TextMessage(objectMapper.writeValueAsString(shootResponse)));
                    }
                }
            }

            if (observers != null && !observers.isEmpty()) {
                GameStateObserverResponse response = multiplayerService.getGameStateForObserver(gameSessionId);
                String jsonString = objectMapper.writeValueAsString(response);

                for (WebSocketSession observerSocket : observers) {
                    if (observerSocket.isOpen()) {
                        observerSocket.sendMessage(new TextMessage(jsonString));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Broadcast state error: {}", e.getMessage());
        }
    }

    private void broadcastShipPlacementEvent(Long gameSessionId) {
        Set<WebSocketSession> players = gamePlayers.get(gameSessionId);
        if (players == null || players.isEmpty()) return;

        try {
            ShipPlacementGameStateResponse placementResponse = new ShipPlacementGameStateResponse();
            placementResponse.setGameSessionId(gameSessionId);
            String jsonNotification = objectMapper.writeValueAsString(placementResponse);

            for (WebSocketSession playerSocket : players) {
                if (playerSocket.isOpen()) {
                    playerSocket.sendMessage(new TextMessage(jsonNotification));
                }
            }
        } catch (Exception e) {
            log.error("ShipPlacement event broadcast error: {}", e.getMessage());
        }
    }

    public void broadcastWin(Long gameSessionId, String winnerUsername) {
        try {
            WinResponse winResponse = new WinResponse();
            winResponse.setGameSessionId(gameSessionId);
            winResponse.setWinner(winnerUsername);

            String winJson = objectMapper.writeValueAsString(winResponse);

            Set<WebSocketSession> players = gamePlayers.remove(gameSessionId);
            if (players != null) {
                for (WebSocketSession playerSession : players) {
                    if (playerSession.isOpen()) {
                        playerSession.sendMessage(new TextMessage(winJson));
                    }
                    String pName = (String) playerSession.getAttributes().get("username");
                    if (pName != null) {
                        userToGameId.remove(pName);
                    }
                }
            }

            Set<WebSocketSession> observers = gameObservers.remove(gameSessionId);
            if (observers != null) {
                for (WebSocketSession observerSession : observers) {
                    if (observerSession.isOpen()) {
                        observerSession.sendMessage(new TextMessage(winJson));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Win broadcast error: {}", e.getMessage());
        }
    }

    private void closeGameSession(Long gameSessionId) {
        log.info("Closing game session {} due to early exit (no winner).", gameSessionId);

        try {
            Set<WebSocketSession> players = gamePlayers.remove(gameSessionId);
            if (players != null) {
                for (WebSocketSession playerSession : players) {
                    String pName = (String) playerSession.getAttributes().get("username");
                    if (pName != null) {
                        userToGameId.remove(pName);
                    }
                }
            }

            Set<WebSocketSession> observers = gameObservers.remove(gameSessionId);
            if (observers != null) {
                for (WebSocketSession observerSession : observers) {
                    String oName = (String) observerSession.getAttributes().get("username");
                    if (oName != null) {
                        userToGameId.remove(oName);
                    }
                }
            }

            multiplayerService.deleteGameSession(gameSessionId);

        } catch (Exception e) {
            log.error("Error while closing game session {}: {}", gameSessionId, e.getMessage());
        }
    }
}