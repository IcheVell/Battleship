package senla.test.task.battleshipserverapp.service;

import dto.GameSessionDto;
import dto.ShipPlacementDto;
import dto.GameTurnDto;
import dto.GameInfoReplayDto;
import enums.GameStatus;
import enums.TypeSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import response.GameStateObserverResponse;
import response.GameStatePlayerResponse;
import response.AdminGameHistoryResponse;
import response.AdminActionWatchResponse;
import senla.test.task.battleshipserverapp.mapper.GameStateMapper;
import senla.test.task.battleshipserverapp.model.GameSession;
import senla.test.task.battleshipserverapp.model.GameTurn;
import senla.test.task.battleshipserverapp.model.Player;
import senla.test.task.battleshipserverapp.repository.GameSessionRepository;
import senla.test.task.battleshipserverapp.repository.GameTurnRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiplayerService {

    @Value("${game.rules.count-ships}")
    private Integer countShips;

    private final PlayerService playerService;
    private final GameSessionRepository gameSessionRepository;
    private final GameTurnRepository gameTurnRepository;

    public GameSession createGameSession(String username) {
        Player p = playerService.login(username);

        GameSession gameSession = new GameSession();
        gameSession.setPlayer1(p);
        gameSession.setPlayer1CountShots(0);
        gameSession.setPlayer1AliveShipsCount(countShips);
        gameSession.setType(TypeSession.MULTIPLAYER);
        gameSession.setGameStatus(GameStatus.WAITING_FOR_PLAYER);

        return gameSessionRepository.save(gameSession);
    }

    @Transactional
    public void updateGameSession(Long gameSessionId, String username, int targetX, int targetY) {
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));

        if (gameSession.getGameStatus() != GameStatus.PLAYER1_TURN && gameSession.getGameStatus() != GameStatus.PLAYER2_TURN) {
            throw new RuntimeException("Can't fire on this stage / Not your turn");
        }

        Player striker = playerService.login(username);
        if (striker == null) {
            throw new RuntimeException("Player not found");
        }

        Player p1 = gameSession.getPlayer1();

        GameTurn lastTurn = gameSession
                .getTurns()
                .stream()
                .max(java.util.Comparator.comparingInt(GameTurn::getTurnNumber))
                .orElseThrow(() -> new RuntimeException("Initial setup turn not found"));

        GameTurn gameTurn = new GameTurn();
        gameTurn.copyBoards(lastTurn);

        gameTurn.setPlayerWhoMoved(striker);
        gameTurn.setTargetX(targetX);
        gameTurn.setTargetY(targetY);
        gameTurn.setTurnNumber(lastTurn.getTurnNumber() + 1);

        if (striker.getId().equals(p1.getId())) {
            if (gameSession.getGameStatus() != GameStatus.PLAYER1_TURN) return;

            char[][] enemyShips = gameTurn.getPlayer2ShipsBoard();
            char[][] myShots = gameTurn.getPlayer1ShotsBoard();

            if (myShots[targetX][targetY] != '~') {
                return;
            }

            if (enemyShips[targetX][targetY] == '~') {
                gameSession.setGameStatus(GameStatus.PLAYER2_TURN);
                myShots[targetX][targetY] = '.';
                enemyShips[targetX][targetY] = '.';
            } else if (enemyShips[targetX][targetY] == 'S') {
                enemyShips[targetX][targetY] = 'X';
                myShots[targetX][targetY] = 'X';

                boolean isDestroyed = isShipDestroyed(enemyShips, targetX, targetY, new boolean[16][16]);
                if (isDestroyed) {
                    gameSession.setPlayer2AliveShipsCount(gameSession.getPlayer2AliveShipsCount() - 1);
                    if (gameSession.getPlayer2AliveShipsCount() == 0) {
                        gameSession.setWinner(gameSession.getPlayer1());
                        gameSession.setGameStatus(GameStatus.ENDED);
                    }
                }
            }
            gameSession.setPlayer1CountShots(gameSession.getPlayer1CountShots() + 1);
        } else {
            if (gameSession.getGameStatus() != GameStatus.PLAYER2_TURN) return;

            char[][] enemyShips = gameTurn.getPlayer1ShipsBoard();
            char[][] myShots = gameTurn.getPlayer2ShotsBoard();

            if (myShots[targetX][targetY] != '~') {
                throw new RuntimeException("You have already shot here!");
            }

            if (enemyShips[targetX][targetY] == '~') {
                gameSession.setGameStatus(GameStatus.PLAYER1_TURN);
                myShots[targetX][targetY] = '.';
                enemyShips[targetX][targetY] = '.';
            } else if (enemyShips[targetX][targetY] == 'S') {
                enemyShips[targetX][targetY] = 'X';
                myShots[targetX][targetY] = 'X';

                boolean isDestroyed = isShipDestroyed(enemyShips, targetX, targetY, new boolean[16][16]);
                if (isDestroyed) {
                    gameSession.setPlayer1AliveShipsCount(gameSession.getPlayer1AliveShipsCount() - 1);
                    if (gameSession.getPlayer1AliveShipsCount() == 0) {
                        gameSession.setWinner(gameSession.getPlayer2());
                        gameSession.setGameStatus(GameStatus.ENDED);
                    }
                }
            }
            gameSession.setPlayer2CountShots(gameSession.getPlayer2CountShots() + 1);
        }

        gameSession.getTurns().add(gameTurn);
        gameSessionRepository.save(gameSession);
    }

    @Transactional
    public void connectAsPlayer(Long gameSessionId, String username) {
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));

        if (gameSession.getPlayer1().getUsername().equals(username)) {
            throw new RuntimeException("Вы не можете играть сами с собой!");
        }
        if (gameSession.getPlayer2() != null) {
            throw new RuntimeException("В этой игре уже есть второй игрок!");
        }

        Player p2 = playerService.login(username);
        gameSession.setPlayer2(p2);
        gameSession.setPlayer2AliveShipsCount(countShips);
        gameSession.setPlayer2CountShots(0);
        gameSession.setGameStatus(GameStatus.SHIPS_PLACEMENT);

        gameSessionRepository.save(gameSession);
    }

    @Transactional
    public void setupShips(Long gameSessionId, String username, List<ShipPlacementDto> ships) {
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));

        if (gameSession.getGameStatus() != GameStatus.SHIPS_PLACEMENT) {
            throw new RuntimeException("Действие недоступно! Сейчас не стадия расстановки кораблей.");
        }

        char[][] shipSetup = getValidShipBoard(ships);
        if (shipSetup == null) {
            throw new RuntimeException("Not valid setup");
        }

        GameTurn zeroTurn = gameTurnRepository.findByGameSessionIdAndTurnNumber(gameSessionId, 0).orElseGet(() -> {
            GameTurn newTurn = new GameTurn();
            newTurn.setGameSession(gameSession);
            newTurn.setTurnNumber(0);
            return newTurn;
        });

        if (username.equals(gameSession.getPlayer1().getUsername())) {
            zeroTurn.setPlayer1ShipsBoard(shipSetup);
            if (zeroTurn.getPlayer1ShotsBoard() == null) {
                zeroTurn.setPlayer1ShotsBoard(createEmptyBoard());
            }
        } else if (gameSession.getPlayer2() != null && username.equals(gameSession.getPlayer2().getUsername())) {
            zeroTurn.setPlayer2ShipsBoard(shipSetup);
            if (zeroTurn.getPlayer2ShotsBoard() == null) {
                zeroTurn.setPlayer2ShotsBoard(createEmptyBoard());
            }
        } else {
            throw new RuntimeException("User is not a player in this session");
        }

        if (!gameSession.getTurns().contains(zeroTurn)) {
            gameSession.getTurns().add(zeroTurn);
        }

        if (zeroTurn.getPlayer1ShipsBoard() != null && zeroTurn.getPlayer2ShipsBoard() != null) {
            gameSession.setGameStatus(GameStatus.PLAYER1_TURN);
        }

        gameTurnRepository.save(zeroTurn);
        gameSessionRepository.save(gameSession);
    }

    @Transactional
    public String surrender(Long gameSessionId, String username) {
        GameSession session = gameSessionRepository.findById(gameSessionId).orElse(null);

        if (session == null || session.getGameStatus() == GameStatus.ENDED) {
            return null;
        }

        session.setGameStatus(GameStatus.ENDED);
        Player winner = null;

        if (session.getPlayer1() != null && username.equals(session.getPlayer1().getUsername())) {
            winner = session.getPlayer2();
        } else if (session.getPlayer2() != null && username.equals(session.getPlayer2().getUsername())) {
            winner = session.getPlayer1();
        }

        if (winner != null) {
            session.setWinner(winner);
            gameSessionRepository.save(session);
            return winner.getUsername();
        }

        gameSessionRepository.save(session);
        return null;
    }

    @Transactional
    public String finishGameDueToDisconnect(Long gameSessionId, String disconnectedUsername) {
        return surrender(gameSessionId, disconnectedUsername);
    }

    public GameStateObserverResponse getGameStateForObserver(Long gameSessionId) {
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));
        return GameStateMapper.toObserverResponse(gameSession);
    }

    public GameStatePlayerResponse getGameStateForPlayer(Long gameSessionId, String username) {
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));
        return GameStateMapper.toPlayerResponse(gameSession, username);
    }

    public List<GameSessionDto> getGamesList() {
        return gameSessionRepository.getGamesList();
    }

    public boolean setupIsReady(Long gameSessionId) {
        GameTurn zeroTurn = gameTurnRepository.findByGameSessionIdAndTurnNumber(gameSessionId, 0).orElseGet(() -> {return null;});
        if (zeroTurn == null) {
            return false;
        }

        return zeroTurn.getPlayer1ShotsBoard() != null && zeroTurn.getPlayer2ShotsBoard() != null;
    }

    @Transactional
    public void deleteGameSession(Long gameSessionId) {
        gameSessionRepository.deleteById(gameSessionId);
    }

    @Transactional
    public void archiveGameSession(Long gameSessionId) {
        GameSession session = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));
        session.setGameStatus(GameStatus.ENDED);
        gameSessionRepository.save(session);
    }

    public AdminGameHistoryResponse getAdminGameHistory(int page, int size) {
        Page<GameSession> sessionPage = gameSessionRepository.findAll(PageRequest.of(page, size));

        List<GameSessionDto> dtos = sessionPage.getContent().stream()
                .map(s -> new GameSessionDto(
                        s.getId(),
                        s.getPlayer1() != null ? s.getPlayer1().getUsername() : "Unknown",
                        s.getPlayer2() != null ? s.getPlayer2().getUsername() : null,
                        s.getType()
                ))
                .collect(Collectors.toList());

        AdminGameHistoryResponse response = new AdminGameHistoryResponse();
        response.setGameSessionList(dtos);
        response.setCurrentPage(sessionPage.getNumber());
        response.setTotalPage(sessionPage.getTotalPages());
        return response;
    }

    public AdminActionWatchResponse getWatchGameInfo(Long gameSessionId) {
        GameSession session = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));

        String creator = session.getPlayer1() != null ? session.getPlayer1().getUsername() : "Unknown";
        String opponent = session.getPlayer2() != null ? session.getPlayer2().getUsername() : "AI_BOT";

        List<GameTurnDto> turnDtos = session.getTurns().stream()
                .sorted(java.util.Comparator.comparingInt(GameTurn::getTurnNumber))
                .map(t -> {
                    String shooter = t.getPlayerWhoMoved() != null ? t.getPlayerWhoMoved().getUsername() : "AI_BOT";

                    int x = t.getTargetX();
                    int y = t.getTargetY();
                    char[][] p2Ships = t.getPlayer2ShipsBoard();

                    String result = "ПРОМАХ";
                    if (p2Ships != null && x >= 0 && x < p2Ships.length && y >= 0 && y < p2Ships[x].length) {
                        if (p2Ships[x][y] == 'X') {
                            result = "ПОПАДАНИЕ";
                        }
                    }

                    String coord = (char)('A' + y) + String.valueOf(x + 1);

                    return new GameTurnDto(t.getTurnNumber(), shooter, coord, result, t.getPlayer1ShipsBoard(), t.getPlayer2ShipsBoard());
                })
                .collect(Collectors.toList());

        GameInfoReplayDto gameInfoReplayDto = new GameInfoReplayDto(creator, opponent, turnDtos);

        return new AdminActionWatchResponse(gameInfoReplayDto);
    }

    public Long findActiveGameSessionIdByUsername(String username) {
        return gameSessionRepository
                .findAll()
                .stream()
                .filter(s -> s.getGameStatus() != GameStatus.ENDED)
                .filter(s -> (s.getPlayer1() != null && s.getPlayer1().getUsername().equals(username)) ||
                        (s.getPlayer2() != null && s.getPlayer2().getUsername().equals(username)))
                .map(GameSession::getId)
                .findFirst()
                .orElse(null);
    }

    public boolean isPlayerTurn(Long gameSessionId, String username) {
        GameSession session = gameSessionRepository.findById(gameSessionId).orElse(null);
        if (session == null) return false;

        if (session.getGameStatus() == GameStatus.PLAYER1_TURN && session.getPlayer1().getUsername().equals(username)) {
            return true;
        }
        if (session.getGameStatus() == GameStatus.PLAYER2_TURN && session.getPlayer2() != null && session.getPlayer2().getUsername().equals(username)) {
            return true;
        }
        return false;
    }

    private boolean isShipDestroyed(char[][] board, int x, int y, boolean[][] visited) {
        if (x < 0 || x >= board.length || y < 0 || y >= board[0].length) return true;
        if (visited[x][y]) return true;
        visited[x][y] = true;

        if (board[x][y] == '~' || board[x][y] == '.') return true;
        if (board[x][y] == 'S') return false;

        return isShipDestroyed(board, x - 1, y, visited) &&
                isShipDestroyed(board, x + 1, y, visited) &&
                isShipDestroyed(board, x, y - 1, visited) &&
                isShipDestroyed(board, x, y + 1, visited);
    }

    private char[][] getValidShipBoard(List<ShipPlacementDto> ships) {
        if (ships == null || ships.size() != 21) return null;
        int[] expectedCounts = {0, 6, 5, 4, 3, 2, 1};
        int[] actualCounts = new int[7];

        for (ShipPlacementDto ship : ships) {
            int size = ship.size();
            if (size < 1 || size > 6) return null;
            actualCounts[size]++;
        }

        for (int i = 1; i <= 6; i++) {
            if (actualCounts[i] != expectedCounts[i]) return null;
        }

        char[][] tempBoard = createEmptyBoard();
        for (ShipPlacementDto ship : ships) {
            int size = ship.size();
            int startX = ship.startX();
            int startY = ship.startY();
            boolean isVertical = ship.isVertical();

            if (startX < 0 || startX >= 16 || startY < 0 || startY >= 16) return null;
            if (isVertical && (startY + size > 16)) return null;
            if (!isVertical && (startX + size > 16)) return null;

            for (int i = 0; i < size; i++) {
                int currentX = isVertical ? startX : startX + i;
                int currentY = isVertical ? startY + i : startY;

                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        int neighborX = currentX + offsetX;
                        int neighborY = currentY + offsetY;

                        if (neighborX >= 0 && neighborX < 16 && neighborY >= 0 && neighborY < 16) {
                            if (tempBoard[neighborX][neighborY] == 'S') return null;
                        }
                    }
                }
            }

            for (int i = 0; i < size; i++) {
                int currentX = isVertical ? startX : startX + i;
                int currentY = isVertical ? startY + i : startY;
                tempBoard[currentX][currentY] = 'S';
            }
        }
        return tempBoard;
    }

    private char[][] createEmptyBoard() {
        char[][] board = new char[16][16];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                board[i][j] = '~';
            }
        }
        return board;
    }
}