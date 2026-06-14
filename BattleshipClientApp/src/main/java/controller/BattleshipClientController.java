package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.ShipPlacementDto;
import dto.GameTurnDto;
import enums.PlayerRole;
import enums.TypeSession;
import enums.AdminAction;
import mapper.CoordinateMapper;
import network.BattleshipWebSocketClient;
import request.*;
import response.*;
import view.ConsoleView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BattleshipClientController {

    private final BattleshipWebSocketClient networkClient;
    private final ConsoleView view;
    private final ObjectMapper objectMapper;

    private final List<ShipPlacementDto> draftShips = new ArrayList<>();
    private String currentGameState = "MAIN_MENU";
    private Long currentGameId = null;
    private Long tempGameId = null;

    private char[][] myShotsBoard = null;
    private boolean canShoot = false;
    private final Random random = new Random();

    private final int[] maxShipQuantities = {0, 6, 5, 4, 3, 2, 1};

    private int currentAdminPage = 0;
    private List<GameTurnDto> activeReplayTurns = new ArrayList<>();
    private int currentReplayIndex = 0;
    private String adminCreatorName = "";
    private String adminOpponentName = "";

    public BattleshipClientController() {
        this.view = new ConsoleView();
        this.objectMapper = new ObjectMapper();
        this.networkClient = new BattleshipWebSocketClient(this);
    }

    public void startApplication() {
        networkClient.connect("ws://localhost:8080/connect");
    }

    public void showMainMenu() {
        this.currentGameState = "MAIN_MENU";
        this.currentGameId = null;
        this.tempGameId = null;
        this.myShotsBoard = null;
        this.canShoot = false;
        this.draftShips.clear();
        view.renderMainMenu();
    }

    public void handleServerMessage(String rawJson) {
        try {
            BaseResponse baseResponse = objectMapper.readValue(rawJson, BaseResponse.class);

            switch (baseResponse) {
                case WinResponse winResponse -> {
                    view.renderGameOver(winResponse.getWinner());
                    showMainMenu();
                }

                case ShootResponse ignored -> {
                    canShoot = true;
                    view.renderShootPrompt();
                }

                case ShipPlacementGameStateResponse shipPlacementGameStateResponse -> {
                    this.currentGameState = "SHIPS_PLACEMENT";
                    if (shipPlacementGameStateResponse.getGameSessionId() != null) {
                        this.currentGameId = shipPlacementGameStateResponse.getGameSessionId();
                    }
                    view.renderPlacementPhaseStart();
                }

                case GameStatePlayerResponse gameStatePlayerResponse -> {
                    String statusStr = gameStatePlayerResponse.getStatus() != null ? gameStatePlayerResponse.getStatus().toString() : "WAITING";
                    if ("FINISHED".equals(statusStr)) return;

                    this.currentGameState = statusStr;
                    if (gameStatePlayerResponse.getGameSessionId() != null) {
                        this.currentGameId = gameStatePlayerResponse.getGameSessionId();
                    }

                    this.myShotsBoard = gameStatePlayerResponse.getMyShotsBoard();

                    if (!"CREATED".equals(statusStr) && !"WAITING_FOR_PLAYER".equals(statusStr)) {
                        view.renderPlayerState(gameStatePlayerResponse);
                    }
                }

                case GameStateObserverResponse gameStateObserverResponse -> {
                    this.currentGameState = "OBSERVER_MODE";
                    if (gameStateObserverResponse.getGameSessionId() != null) {
                        this.currentGameId = gameStateObserverResponse.getGameSessionId();
                    }
                    view.renderObserverState(gameStateObserverResponse);
                }

                case ActiveGamesResponse activeGamesResponse -> {
                    view.renderGamesList(activeGamesResponse);
                    view.printPrompt("\nEnter game ID to connect (or type 'back'): ");
                }

                case ConnectToGameResponse connectToGameResponse -> {
                    if (connectToGameResponse.getGameSessionId() != null) {
                        this.currentGameId = connectToGameResponse.getGameSessionId();
                        view.printMessage("\n✅ Успешное подключение к лобби #" + this.currentGameId + "! Ожидайте...");
                    } else {
                        view.printMessage("\n❌ ОШИБКА ПОДКЛЮЧЕНИЯ: Лобби уже заполнено, игра началась или вы пытаетесь играть сами с собой!");
                        view.printMessage("Возврат к списку игр...\n");
                        sendActiveGamesRequest();
                        this.currentGameState = "SELECTING_GAME";
                    }
                }

                case CreateGameResponse createGameResponse -> {
                    if (createGameResponse.getGameSessionId() != null) {
                        this.currentGameId = createGameResponse.getGameSessionId();
                        this.currentGameState = "CREATED";
                        view.printMessage("\n✅ Лобби #" + this.currentGameId + " успешно создано! Ожидание оппонента... (для отмены введите 'back')");
                    } else {
                        view.printMessage("\n❌ ОШИБКА: Сервер отклонил создание лобби.");
                        showMainMenu();
                    }
                }

                case AdminGameHistoryResponse adminHistory -> {
                    this.currentGameState = "ADMIN_MENU";
                    this.currentAdminPage = adminHistory.getCurrentPage();
                    view.renderAdminMenu(adminHistory.getGameSessionList(), adminHistory.getCurrentPage(), adminHistory.getTotalPage());
                }

                case AdminActionWatchResponse adminWatch -> {
                    this.currentGameState = "ADMIN_REPLAY";
                    this.activeReplayTurns = adminWatch.getGameInfo().turns();
                    this.currentReplayIndex = 0;
                    this.adminCreatorName = adminWatch.getGameInfo().creatorName();
                    this.adminOpponentName = adminWatch.getGameInfo().opponentName();
                    if (activeReplayTurns == null || activeReplayTurns.isEmpty()) {
                        view.printMessage("❌ В этой игре не было совершено ни одного хода.");
                        sendAdminGameHistoryRequest(currentAdminPage);
                    } else {
                        showNextReplayTurn();
                    }
                }

                case AdminActionDeleteResponse ignored -> {
                    view.printMessage("✅ Действие администратора успешно выполнено.");
                    sendAdminGameHistoryRequest(currentAdminPage);
                }

                default -> {
                    throw new IllegalStateException("Unexpected value: " + baseResponse.toString());
                }
            }
        } catch (Exception e) {
            view.printMessage("Ошибка обработки ответа сервера: " + e.getMessage());
        }
    }

    public void login(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        networkClient.sendRequest(request);
    }

    public void sendCreateGameRequest(TypeSession type) {
        CreateGameRequest request = new CreateGameRequest(type);
        networkClient.sendRequest(request);
    }

    public void sendActiveGamesRequest() {
        ActiveGamesRequest request = new ActiveGamesRequest();
        networkClient.sendRequest(request);
    }

    public void sendAdminGameHistoryRequest(int page) {
        AdminGameHistoryRequest request = new AdminGameHistoryRequest(page, 20);
        networkClient.sendRequest(request);
    }

    public void sendAdminActionRequest(AdminAction action, Long id) {
        AdminActionRequest request = new AdminActionRequest();
        request.setAdminAction(action);
        request.setGameSessionId(id);
        networkClient.sendRequest(request);
    }

    public void sendConnectToGameRequest(long gameId, String roleStr) {
        ConnectToGameRequest request = new ConnectToGameRequest();
        request.setGameSessionId(gameId);
        try {
            request.setRole(PlayerRole.valueOf(roleStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            request.setRole(PlayerRole.PLAYER);
        }
        networkClient.sendRequest(request);
    }

    public void makeShot(int x, int y) {
        FireRequest fireRequest = new FireRequest(x, y);
        networkClient.sendRequest(fireRequest);
    }

    public void disconnect() {
        networkClient.disconnect();
    }

    private void showNextReplayTurn() {
        if (currentReplayIndex < activeReplayTurns.size()) {
            GameTurnDto turn = activeReplayTurns.get(currentReplayIndex);
            view.renderAdminReplayStep(turn, adminCreatorName, adminOpponentName);
            currentReplayIndex++;
        } else {
            view.printMessage("🏁 История матча полностью воспроизведена! Возврат в меню администратора.");
            sendAdminGameHistoryRequest(currentAdminPage);
        }
    }

    private void handleAdminMenuInput(String cleanInput) {
        String[] parts = cleanInput.split("\\s+");
        String cmd = parts[0].toLowerCase();

        if ("next".equals(cmd)) {
            sendAdminGameHistoryRequest(currentAdminPage + 1);
        } else if ("prev".equals(cmd)) {
            if (currentAdminPage > 0) {
                sendAdminGameHistoryRequest(currentAdminPage - 1);
            } else {
                view.printMessage("❌ Вы находитесь на первой странице.");
            }
        } else if ("watch".equals(cmd) || "delete".equals(cmd) || "archive".equals(cmd)) {
            if (parts.length < 2) {
                view.printMessage("❌ Укажите ID сессии. Пример: watch 15");
                return;
            }
            try {
                Long id = Long.parseLong(parts[1]);
                AdminAction action = AdminAction.valueOf(cmd.toUpperCase());
                sendAdminActionRequest(action, id);
            } catch (NumberFormatException e) {
                view.printMessage("❌ Неверный формат числового ID.");
            }
        } else {
            view.printMessage("❌ Неизвестная команда. Доступны: next, prev, watch <id>, delete <id>, archive <id>, back");
        }
    }

    public void handleUserInput(String input) {
        String cleanInput = input.trim();
        if (cleanInput.isEmpty()) {
            if ("ADMIN_REPLAY".equals(currentGameState)) {
                showNextReplayTurn();
            }
            return;
        }

        if ("back".equalsIgnoreCase(cleanInput)) {
            handleBackNavigation();
            return;
        }

        switch (currentGameState) {
            case "MAIN_MENU":
                if ("1".equals(cleanInput)) {
                    sendCreateGameRequest(TypeSession.SINGLEPLAYER);
                } else if ("2".equals(cleanInput)) {
                    sendCreateGameRequest(TypeSession.MULTIPLAYER);
                } else if ("3".equals(cleanInput)) {
                    sendActiveGamesRequest();
                    this.currentGameState = "SELECTING_GAME";
                } else if ("admin".equalsIgnoreCase(cleanInput)) {
                    sendAdminGameHistoryRequest(0);
                } else {
                    view.printMessage("❌ Неверный выбор. Введите 1, 2, 3 или admin.");
                }
                return;

            case "SELECTING_GAME":
                try {
                    this.tempGameId = Long.parseLong(cleanInput);
                    this.currentGameState = "SELECTING_ROLE";
                    view.renderRoleSelection();
                } catch (NumberFormatException e) {
                    view.printMessage("❌ Введите корректный числовой ID игры или 'back' для возврата.");
                }
                return;

            case "SELECTING_ROLE":
                String role = "PLAYER";
                if ("2".equals(cleanInput)) {
                    role = "OBSERVER";
                } else if (!"1".equals(cleanInput)) {
                    view.printMessage("❌ Неверный выбор роли. Введите 1 (Player), 2 (Observer) или 'back'.");
                    return;
                }
                sendConnectToGameRequest(tempGameId, role);
                return;

            case "ADMIN_MENU":
                handleAdminMenuInput(cleanInput);
                return;

            case "ADMIN_REPLAY":
                showNextReplayTurn();
                return;
        }

        String[] parts = cleanInput.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "fire":
                if (!canShoot) {
                    view.printMessage("❌ Ошибка: Сейчас не ваш ход, или вы уже сделали выстрел! Ожидайте своей очереди.");
                    break;
                }
                if (parts.length == 2) {
                    try {
                        int[] coords = CoordinateMapper.parseToNumbers(parts[1]);
                        int x = coords[0] - 1;
                        int y = coords[1] - 1;

                        if (x < 0 || x >= 16 || y < 0 || y >= 16) {
                            view.printMessage("❌ Ошибка: Координаты выстрела вне поля (допустимо A-P и 1-16)!");
                            break;
                        }

                        if (myShotsBoard != null) {
                            char currentCell = myShotsBoard[x][y];
                            if (currentCell == '.' || currentCell == 'X') {
                                String displayCoord = CoordinateMapper.toDisplayFormat(parts[1]);
                                view.printMessage("❌ Ошибка: Вы уже стреляли в клетку " + displayCoord + "! Там стоит '" + currentCell + "'. Выберите другую цель.");
                                break;
                            }
                        }

                        makeShot(x, y);
                        canShoot = false;
                        view.printMessage("▲ Выстрел по " + CoordinateMapper.toDisplayFormat(parts[1]) + " отправлен!");

                    } catch (IllegalArgumentException e) {
                        view.printMessage("❌ Ошибка: Неверный формат координат! Использование: fire <координата>, например: fire A15 или fire 15A");
                    }
                } else {
                    view.printMessage("Использование: fire <координата>, например: fire A15");
                }
                break;

            case "addship":
                if (!"SHIPS_PLACEMENT".equals(currentGameState)) {
                    view.printMessage("❌ Ошибка: Нельзя добавлять корабли! Дождитесь начала стадии расстановки.");
                    break;
                }
                if (parts.length == 4) {
                    try {
                        int size = Integer.parseInt(parts[1]);
                        int[] coords = CoordinateMapper.parseToNumbers(parts[2]);

                        int x = coords[1] - 1;
                        int y = coords[0] - 1;

                        String orientation = parts[3].toLowerCase();

                        if (!orientation.equals("v") && !orientation.equals("h")) {
                            view.printMessage("❌ Ошибка: Направление должно быть 'v' (вертикально) или 'h' (горизонтально)!");
                            break;
                        }
                        boolean isVertical = orientation.equals("v");

                        if (size < 1 || size > 6) {
                            view.printMessage("❌ Ошибка: Максимальный размер корабля по ТЗ — 6 палуб!");
                            break;
                        }
                        if (x < 0 || x >= 16 || y < 0 || y >= 16) {
                            view.printMessage("❌ Ошибка: Координаты начала должны быть от 1 до 16!");
                            break;
                        }
                        if (isVertical && (y + size) > 16) {
                            view.printMessage("❌ Ошибка: Корабль выходит за нижнюю границу поля!");
                            break;
                        }
                        if (!isVertical && (x + size) > 16) {
                            view.printMessage("❌ Ошибка: Корабль выходит за правую границу поля!");
                            break;
                        }

                        long currentSizeCount = draftShips.stream().filter(s -> s.size() == size).count();
                        if (currentSizeCount >= maxShipQuantities[size]) {
                            view.printMessage("❌ Ошибка: Превышено доступное количество кораблей размера " + size + " (максимум: " + maxShipQuantities[size] + " шт)!");
                            break;
                        }

                        boolean[][] tempBoard = new boolean[16][16];
                        for (ShipPlacementDto existingShip : draftShips) {
                            placeShipOnBitMap(tempBoard, existingShip.size(), existingShip.startX(), existingShip.startY(), existingShip.isVertical());
                        }

                        if (!canPlaceShip(tempBoard, size, x, y, isVertical)) {
                            view.printMessage("❌ Ошибка: Корабль пересекается с другими или стоит слишком близко (минимум 1 клетка дистанции)!");
                            break;
                        }

                        draftShips.add(new ShipPlacementDto(size, x, y, isVertical));
                        view.renderDraftBoard(draftShips);

                    } catch (NumberFormatException e) {
                        view.printMessage("❌ Ошибка: Неверный формат чисел! Пример: addship 6 1 1 h");
                    }
                } else {
                    view.printMessage("Использование: addship <размер> <x> <y> <v/h> (Пример: addship 6 1 1 h)");
                }
                break;

            case "submitships":
                if (!"SHIPS_PLACEMENT".equals(currentGameState)) {
                    view.printMessage("❌ Ошибка: Нельзя отправить расстановку! Игра не находится в этой стадии.");
                    break;
                }
                if (currentGameId == null) {
                    view.printMessage("❌ Ошибка: ID сессии неизвестен. Дождитесь обновления от сервера.");
                    break;
                }
                if (draftShips.size() != 21) {
                    view.printMessage("❌ Ошибка: На поле должно быть ровно 21 корабль! Сейчас: " + draftShips.size());
                    break;
                }

                int[] finalCounts = new int[7];
                for (ShipPlacementDto s : draftShips) {
                    finalCounts[s.size()]++;
                }
                boolean poolIsValid = finalCounts[6] == 1 && finalCounts[5] == 2 && finalCounts[4] == 3
                        && finalCounts[3] == 4 && finalCounts[2] == 5 && finalCounts[1] == 6;

                if (!poolIsValid) {
                    view.printMessage("❌ Ошибка: Неверный состав флота! Должно быть строго: 1x6, 2x5, 3x4, 4x3, 5x2, 6x1.");
                    break;
                }

                ShipPlacementRequest placeRequest = new ShipPlacementRequest(currentGameId, new ArrayList<>(draftShips));
                networkClient.sendRequest(placeRequest);
                draftShips.clear();
                view.printMessage("▲ Запрос на расстановку кораблей отправлен! Ожидайте готовности противника.");
                break;

            case "placeauto":
                if (!"SHIPS_PLACEMENT".equals(currentGameState)) {
                    view.printMessage("❌ Ошибка: Автоматическая расстановка недоступна до начала фазы игры.");
                    break;
                }
                generateRandomPlacement();
                break;

            default:
                view.printMessage("Неизвестная команда. Сверьтесь с инструкцией.");
        }
    }

    private void handleBackNavigation() {
        switch (currentGameState) {
            case "MAIN_MENU":
                view.printMessage("Вы уже находитесь в главном меню.");
                break;

            case "SELECTING_GAME":
                showMainMenu();
                break;

            case "SELECTING_ROLE":
                sendActiveGamesRequest();
                this.currentGameState = "SELECTING_GAME";
                break;

            case "ADMIN_MENU":
                showMainMenu();
                break;

            case "ADMIN_REPLAY":
                sendAdminGameHistoryRequest(currentAdminPage);
                break;

            case "CREATED":
            case "WAITING_FOR_PLAYER":
            case "SHIPS_PLACEMENT":
                view.printMessage("↩️ Вы покинули комнату до начала игры. Комната аннулирована.");
                sendSurrenderRequest();
                showMainMenu();
                break;

            case "OBSERVER_MODE":
                view.printMessage("↩️ Вы вышли из режима наблюдения.");
                sendSurrenderRequest();
                showMainMenu();
                break;

            default:
                view.printMessage("⚠️ Матч уже идет! Команда 'back' расценена как капитуляция. Вам засчитано поражение.");
                sendSurrenderRequest();
                showMainMenu();
                break;
        }
    }

    private void sendSurrenderRequest() {
        if (currentGameId != null) {
            SurrenderRequest surrenderRequest = new SurrenderRequest(currentGameId);
            networkClient.sendRequest(surrenderRequest);
        }
    }

    private void generateRandomPlacement() {
        draftShips.clear();
        boolean[][] board = new boolean[16][16];
        int[] shipSizes = {6, 5, 5, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1};

        for (int size : shipSizes) {
            boolean placed = false;
            int attempts = 0;

            while (!placed && attempts < 2000) {
                attempts++;
                int x = random.nextInt(16);
                int y = random.nextInt(16);
                boolean isVertical = random.nextBoolean();

                if (canPlaceShip(board, size, x, y, isVertical)) {
                    placeShipOnBitMap(board, size, x, y, isVertical);
                    draftShips.add(new ShipPlacementDto(size, x, y, isVertical));
                    placed = true;
                }
            }
        }
        view.printMessage("\n🎲 Сгенерирована случайная комбинация! Используйте 'placeauto' ещё раз для реролла или 'submitships' для отправки.");
        view.renderDraftBoard(draftShips);
    }

    private boolean canPlaceShip(boolean[][] board, int size, int x, int y, boolean isVertical) {
        if (isVertical) {
            if (y + size > 16) return false;
            for (int i = 0; i < size; i++) {
                if (hasNeighbor(board, x, y + i)) return false;
            }
        } else {
            if (x + size > 16) return false;
            for (int i = 0; i < size; i++) {
                if (hasNeighbor(board, x + i, y)) return false;
            }
        }
        return true;
    }

    private boolean hasNeighbor(boolean[][] board, int x, int y) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nx = x + i;
                int ny = y + j;
                if (nx >= 0 && nx < 16 && ny >= 0 && ny < 16) {
                    if (board[ny][nx]) return true;
                }
            }
        }
        return false;
    }

    private void placeShipOnBitMap(boolean[][] board, int size, int x, int y, boolean isVertical) {
        for (int i = 0; i < size; i++) {
            if (isVertical) {
                board[y + i][x] = true;
            } else {
                board[y][x + i] = true;
            }
        }
    }
}