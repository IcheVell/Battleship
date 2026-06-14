package senla.test.task.battleshipserverapp.mapper;

import response.GameStateObserverResponse;
import response.GameStatePlayerResponse;
import senla.test.task.battleshipserverapp.model.GameSession;

public class GameStateMapper {
    public static GameStateObserverResponse toObserverResponse(GameSession gameSession) {
        char[][] p1Ships = new char[16][16];
        char[][] p1Shots = new char[16][16];
        char[][] p2Ships = new char[16][16];
        char[][] p2Shots = new char[16][16];

        if (gameSession.getTurns().isEmpty()) {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    p1Ships[i][j] = '~'; p1Shots[i][j] = '~';
                    p2Ships[i][j] = '~'; p2Shots[i][j] = '~';
                }
            }
        } else {
            p1Ships = gameSession.getTurns().getLast().getPlayer1ShipsBoard();
            p1Shots = gameSession.getTurns().getLast().getPlayer1ShotsBoard();
            p2Ships = gameSession.getTurns().getLast().getPlayer2ShipsBoard();
            p2Shots = gameSession.getTurns().getLast().getPlayer2ShotsBoard();
        }

        return GameStateObserverResponse.builder()
                .p1Name(gameSession.getPlayer1().getUsername())
                .p2Name(gameSession.getPlayer2() == null ? null : gameSession.getPlayer2().getUsername())
                .p1AliveShipsCount(gameSession.getPlayer1AliveShipsCount())
                .p2AliveShipsCount(gameSession.getPlayer2AliveShipsCount())
                .status(gameSession.getGameStatus())
                .p1ShipsBoard(p1Ships)
                .p1ShotsBoard(p1Shots)
                .p2ShipsBoard(p2Ships)
                .p2ShotsBoard(p2Shots)
                .build();
    }

    public static GameStatePlayerResponse toPlayerResponse(GameSession gameSession, String currentUsername) {
        String p1Name = gameSession.getPlayer1().getUsername();
        boolean isPlayer1 = currentUsername.equals(p1Name);

        char[][] shipsBoard = new char[16][16];
        char[][] shotsBoard = new char[16][16];

        if (gameSession.getTurns().isEmpty()) {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    shipsBoard[i][j] = '~';
                    shotsBoard[i][j] = '~';
                }
            }
        } else {
            var lastTurn = gameSession.getTurns().getLast();
            if (isPlayer1) {
                shipsBoard = lastTurn.getPlayer1ShipsBoard();
                shotsBoard = lastTurn.getPlayer1ShotsBoard();
            } else {
                shipsBoard = lastTurn.getPlayer2ShipsBoard();
                shotsBoard = lastTurn.getPlayer2ShotsBoard();
            }
        }

        return GameStatePlayerResponse.builder()
                .p1Name(p1Name)
                .p2Name(gameSession.getPlayer2() != null ? gameSession.getPlayer2().getUsername() : null)
                .p1AliveShipsCount(gameSession.getPlayer1AliveShipsCount())
                .p2AliveShipsCount(gameSession.getPlayer2AliveShipsCount())
                .status(gameSession.getGameStatus())
                .myShipsBoard(shipsBoard)
                .myShotsBoard(shotsBoard)
                .build();
    }
}
