package senla.test.task.battleshipserverapp.service;

import dto.ShipPlacementDto;
import enums.GameStatus;
import enums.TypeSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import senla.test.task.battleshipserverapp.model.GameSession;
import senla.test.task.battleshipserverapp.model.GameTurn;
import senla.test.task.battleshipserverapp.model.Player;
import senla.test.task.battleshipserverapp.repository.GameSessionRepository;
import senla.test.task.battleshipserverapp.repository.GameTurnRepository;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SingleplayerService {

    @Value("${game.rules.count-ships}")
    private Integer countShips;

    private final PlayerService playerService;
    private final GameSessionRepository gameSessionRepository;
    private final GameTurnRepository gameTurnRepository;
    private final Random random = new Random();

    public boolean isSingleplayerSession(Long gameSessionId) {
        return gameSessionRepository.findById(gameSessionId)
                .map(s -> s.getType() == TypeSession.SINGLEPLAYER)
                .orElse(false);
    }

    public GameSession createGameSession(String username) {
        Player p = playerService.login(username);
        Player bot = playerService.login("AI_BOT");

        GameSession gameSession = new GameSession();
        gameSession.setPlayer1(p);
        gameSession.setPlayer1CountShots(0);
        gameSession.setPlayer1AliveShipsCount(countShips);
        gameSession.setType(TypeSession.SINGLEPLAYER);

        gameSession.setPlayer2(bot);
        gameSession.setPlayer2CountShots(0);
        gameSession.setPlayer2AliveShipsCount(countShips);
        gameSession.setGameStatus(GameStatus.SHIPS_PLACEMENT);

        return gameSessionRepository.save(gameSession);
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
        } else {
            throw new RuntimeException("User is not player 1 in this singleplayer session");
        }

        char[][] botBoard = generateBotBoard();
        zeroTurn.setPlayer2ShipsBoard(botBoard);
        if (zeroTurn.getPlayer2ShotsBoard() == null) {
            zeroTurn.setPlayer2ShotsBoard(createEmptyBoard());
        }

        if (!gameSession.getTurns().contains(zeroTurn)) {
            gameSession.getTurns().add(zeroTurn);
        }

        gameSession.setGameStatus(GameStatus.PLAYER1_TURN);

        gameTurnRepository.save(zeroTurn);
        gameSessionRepository.save(gameSession);
    }

    @Transactional
    public void updateGameSession(Long gameSessionId, String username, int targetX, int targetY) {
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElseThrow(() -> new RuntimeException("Game session not found"));

        if (gameSession.getGameStatus() != GameStatus.PLAYER1_TURN) {
            throw new RuntimeException("Can't fire on this stage / Not your turn");
        }

        Player striker = playerService.login(username);
        if (striker == null || !striker.getId().equals(gameSession.getPlayer1().getId())) {
            throw new RuntimeException("Player not found or not part of this session");
        }

        GameTurn lastTurn = gameSession.getTurns().getLast();
        GameTurn gameTurn = new GameTurn();
        gameTurn.copyBoards(lastTurn);

        gameTurn.setPlayerWhoMoved(striker);
        gameTurn.setTargetX(targetX);
        gameTurn.setTargetY(targetY);
        gameTurn.setTurnNumber(lastTurn.getTurnNumber() + 1);

        char[][] enemyShips = gameTurn.getPlayer2ShipsBoard();
        char[][] myShots = gameTurn.getPlayer1ShotsBoard();

        if (myShots[targetX][targetY] != '~') {
            throw new RuntimeException("You have already shot here!");
        }

        if (enemyShips[targetX][targetY] == '~') {
            gameSession.setGameStatus(GameStatus.BOT_TURN);
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
        gameSession.getTurns().add(gameTurn);
        gameSessionRepository.save(gameSession);
    }

    @Transactional
    public boolean executeBotTurnStep(Long gameSessionId) {
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElse(null);
        if (gameSession == null || gameSession.getGameStatus() != GameStatus.BOT_TURN) {
            return false;
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Player bot = playerService.login("AI_BOT");
        GameTurn lastTurn = gameSession.getTurns().getLast();

        int[] target = calculateNextMove(lastTurn);
        int targetX = target[0];
        int targetY = target[1];

        GameTurn gameTurn = new GameTurn();
        gameTurn.copyBoards(lastTurn);
        gameTurn.setPlayerWhoMoved(bot);
        gameTurn.setTargetX(targetX);
        gameTurn.setTargetY(targetY);
        gameTurn.setTurnNumber(lastTurn.getTurnNumber() + 1);

        char[][] enemyShips = gameTurn.getPlayer1ShipsBoard();
        char[][] myShots = gameTurn.getPlayer2ShotsBoard();

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
        gameSession.getTurns().add(gameTurn);
        gameSessionRepository.save(gameSession);

        return gameSession.getGameStatus() == GameStatus.BOT_TURN;
    }

    public char[][] generateBotBoard() {
        char[][] board = createEmptyBoard();
        int[] expectedCounts = {0, 6, 5, 4, 3, 2, 1};

        for (int size = 6; size >= 1; size--) {
            int count = expectedCounts[size];
            for (int k = 0; k < count; k++) {
                boolean placed = false;
                while (!placed) {
                    int startX = random.nextInt(16);
                    int startY = random.nextInt(16);
                    boolean isVertical = random.nextBoolean();

                    if (isValidPlacement(board, startX, startY, size, isVertical)) {
                        placeShip(board, startX, startY, size, isVertical);
                        placed = true;
                    }
                }
            }
        }
        return board;
    }

    private int[] calculateNextMove(GameTurn lastTurn) {
        char[][] botShots = lastTurn.getPlayer2ShotsBoard();
        int x, y;
        do {
            x = random.nextInt(16);
            y = random.nextInt(16);
        } while (botShots[x][y] != '~');

        return new int[]{x, y};
    }

    private boolean isValidPlacement(char[][] board, int startX, int startY, int size, boolean isVertical) {
        if (isVertical && (startY + size > 16)) return false;
        if (!isVertical && (startX + size > 16)) return false;

        for (int i = 0; i < size; i++) {
            int currentX = isVertical ? startX : startX + i;
            int currentY = isVertical ? startY + i : startY;

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    int neighborX = currentX + offsetX;
                    int neighborY = currentY + offsetY;

                    if (neighborX >= 0 && neighborX < 16 && neighborY >= 0 && neighborY < 16) {
                        if (board[neighborX][neighborY] == 'S') {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void placeShip(char[][] board, int startX, int startY, int size, boolean isVertical) {
        for (int i = 0; i < size; i++) {
            int currentX = isVertical ? startX : startX + i;
            int currentY = isVertical ? startY + i : startY;
            board[currentX][currentY] = 'S';
        }
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