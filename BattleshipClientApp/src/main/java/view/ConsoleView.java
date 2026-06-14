package view;

import dto.GameSessionDto;
import dto.ShipPlacementDto;
import dto.GameTurnDto;
import response.ActiveGamesResponse;
import response.GameStateObserverResponse;
import response.GameStatePlayerResponse;

import java.util.List;

public class ConsoleView {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE_BOLD = "\u001B[1;37m";

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private String translateStatus(String status) {
        if (status == null) return "НЕИЗВЕСТЕН";
        return switch (status.toUpperCase()) {
            case "CREATED", "WAITING" -> "ОЖИДАНИЕ ИГРОКОВ";
            case "WAITING_FOR_PLAYER" -> "ОЖИДАНИЕ ПРОТИВНИКА";
            case "SHIPS_PLACEMENT" -> "РАССТАНОВКА СИЛ";
            case "BATTLE", "IN_GAME" -> "ИДЕТ СРАЖЕНИЕ";
            case "FINISHED" -> "МАТЧ ЗАВЕРШЕН";
            default -> status;
        };
    }

    public void printInstructions() {
        System.out.println(CYAN + "╔══════════════════════════════ СВОДКА СИСТЕМНЫХ КОМАНД ═══════════════════════════════╗" + RESET);
        System.out.println(CYAN + "║" + RESET + "  placeauto                    - Автоматически раскидать флот по сетке (21 корабль) " + CYAN + "  ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  addship <размер> <коорд> <h/v> - Выставить корабль (h - горизонт., v - вертикаль.) " + CYAN + " ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "                                 Пример: addship 6 A3 h                            " + CYAN + "   ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  submitships                  - Утвердить дислокацию и отправить флот в бой       " + CYAN + "   ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  fire <координата>            - Навести орудия и произвести выстрел (Пример: fire B5)" + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  back                         - Вернуться в меню / Объявить капитуляцию           " + CYAN + "   ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  exit                         - Полный выход из игрового приложения               " + CYAN + "   ║" + RESET);
        System.out.println(CYAN + "╚══════════════════════════════════════════════════════════════════════════════════════╝" + RESET);
    }

    public void renderMainMenu() {
        System.out.println("\n" + CYAN + "╔═════════════════════════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + "║" + WHITE_BOLD + "                            ⚓  ГЛАВНОЕ МЕНЮ ШТАБА  ⚓                            " + CYAN + "   ║" + RESET);
        System.out.println(CYAN + "╠═════════════════════════════════════════════════════════════════════════════════════╣" + RESET);
        System.out.println(CYAN + "║" + RESET + "  [1] Одиночная игра (Тренировка с ИИ-ботом)                                        " + CYAN + " ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  [2] Сетевой режим  (Создать новое онлайн-лобби)                                   " + CYAN + " ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  [3] Список комнат  (Подключиться к активной игре)                                 " + CYAN + " ║" + RESET);
        System.out.println(CYAN + "╚═════════════════════════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.print("\n" + BOLD + "👉 Выберите действие (1-3) или введите 'admin' / 'exit': " + RESET);
    }

    public void renderRoleSelection() {
        System.out.println("\n" + CYAN + "╔═════════════════════════════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + "║" + WHITE_BOLD + "                          👤 ОПРЕДЕЛЕНИЕ ИГРОВОГО СТАТУСА                            " + CYAN + "║" + RESET);
        System.out.println(CYAN + "╠═════════════════════════════════════════════════════════════════════════════════════╣" + RESET);
        System.out.println(CYAN + "║" + RESET + "  [1] Боевой офицер (Вступить в полноценное сражение как игрок)                     " + CYAN + " ║" + RESET);
        System.out.println(CYAN + "║" + RESET + "  [2] Наблюдатель   (Следить за картой со стороны без права ведения огня)           " + CYAN + " ║" + RESET);
        System.out.println(CYAN + "╚═════════════════════════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.print("\n" + BOLD + "👉 Выберите роль (1 или 2) либо введите 'back' для отмены: " + RESET);
    }

    public void renderGameOver(String winner) {
        System.out.println("\n" + RED + "███████████████████████████████████████████████████████████████████████████████████████" + RESET);
        System.out.println(WHITE_BOLD + "                           🏆  ФИНАЛ СРАЖЕНИЯ: МАТЧ ОКОНЧЕН!                           " + RESET);
        if (winner != null && !winner.isEmpty()) {
            System.out.println(YELLOW + "                     ТРИУМФАТОР ВОЕННОЙ КАМПАНИИ: " + winner.toUpperCase() + RESET);
        } else {
            System.out.println("                        Боевые действия прекращены.");
        }
        System.out.println(RED + "███████████████████████████████████████████████████████████████████████████████████████\n" + RESET);
    }

    public void renderShootPrompt() {
        System.out.println("\n" + GREEN + "📡 ═══════════════════════════════════════════════════════════════════════════════════" + RESET);
        System.out.println(WHITE_BOLD + "🎯 ВАШ ЧЕРЕД ВЕСТИ ОГОНЬ! Орудийные расчеты ждут координат.");
        System.out.println("👉 Пример приказа: fire A15 или fire K8");
        System.out.println(GREEN + "═════════════════════════════════════════════════════════════════════════════════════" + RESET);
        System.out.print("\n" + BOLD + "⌨️  Штабной терминал > Введите команду: " + RESET);
    }

    public void renderPlacementPhaseStart() {
        System.out.println("\n" + YELLOW + "⚔️  ═══════════════════════════════════════════════════════════════════════════════════" + RESET);
        System.out.println(WHITE_BOLD + "⚓ ПОЗИЦИИ К БОЮ! Все участники на месте. Объявлена фаза скрытной дислокации флота.");
        System.out.println("👉 Сгенерируйте карту автоматом через 'placeauto' или соберите вручную через 'addship'.");
        System.out.println(YELLOW + "═════════════════════════════════════════════════════════════════════════════════════" + RESET);
        System.out.print("\n" + BOLD + "⌨️  Штабной терминал > Введите команду: " + RESET);
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void renderPlayerState(GameStatePlayerResponse model) {
        clearScreen();
        System.out.println(WHITE_BOLD + "╔════════════════════════════════ ТАКТИЧЕСКИЙ ЭКРАН ИГРОКА ═══════════════════════════╗" + RESET);
        System.out.println("  Статус сети:  [" + YELLOW + translateStatus(String.valueOf(model.getStatus())) + RESET + "]");
        System.out.println("  Союзные силы: " + CYAN + model.getP1Name() + RESET + " (Боеспособных кораблей: " + RED + model.getP1AliveShipsCount() + RESET + ")");
        System.out.println("  Вражеские силы: " + CYAN + (model.getP2Name() != null ? model.getP2Name() : "Поиск сигнала оппонента...") + RESET
                + " (Боеспособных кораблей: " + RED + model.getP2AliveShipsCount() + RESET + ")");
        System.out.println(WHITE_BOLD + "╚═════════════════════════════════════════════════════════════════════════════════════╝" + RESET);

        printInstructions();
        renderTwoBoards("НАШИ КОРАБЛИ (ДИСЛОКАЦИЯ)", model.getMyShipsBoard(), "КАРТА РАДАРНЫХ НАВЕДЕНИЙ", model.getMyShotsBoard());
        System.out.print("\n" + BOLD + "⌨️  Штабной терминал > Введите команду: " + RESET);
    }

    public void renderObserverState(GameStateObserverResponse model) {
        clearScreen();
        System.out.println(WHITE_BOLD + "╔═══════════════════════════════ МОНИТОР ПЕРЕХВАТА ДАННЫХ ════════════════════════════╗" + RESET);
        System.out.println("  Режим: НАБЛЮДАТЕЛЬ | Текущее состояние операции: [" + YELLOW + translateStatus(model.getStatus().toString()) + RESET + "]");
        System.out.println("  Огневую активность ведет флагман: " + GREEN + model.getCurrentTurnPlayerName() + RESET);
        System.out.println(WHITE_BOLD + "╚═════════════════════════════════════════════════════════════════════════════════════╝" + RESET);

        printInstructions();
        renderTwoBoards("ФЛОТ ИГРОКА А: " + model.getP1Name(), model.getP1ShipsBoard(), "ФЛОТ ИГРОКА Б: " + model.getP2Name(), model.getP2ShipsBoard());
        System.out.print("\n" + BOLD + "⌨️  Штабной терминал > Введите команду: " + RESET);
    }

    private String colorizeCell(char cell) {
        return switch (cell) {
            case '~' -> BLUE + "~" + RESET;
            case '■' -> CYAN + "■" + RESET;
            case 'X' -> RED + BOLD + "X" + RESET;
            case '.' -> YELLOW + "•" + RESET;
            default -> String.valueOf(cell);
        };
    }

    private void renderTwoBoards(String title1, char[][] board1, String title2, char[][] board2) {
        System.out.println();

        String cleanTitle1 = String.format("%-36s", title1);
        String cleanTitle2 = String.format("%-36s", title2);
        System.out.println("    " + BOLD + cleanTitle1 + RESET + "          " + BOLD + cleanTitle2 + RESET);

        String boardLetters = YELLOW + "A B C D E F G H I J K L M N O P" + RESET;
        String gap = "          ";

        System.out.println("    " + boardLetters + gap + "    " + boardLetters);

        for (int i = 0; i < 16; i++) {
            System.out.printf(WHITE_BOLD + " %2d " + RESET, (i + 1));
            for (int j = 0; j < 16; j++) {
                System.out.print(colorizeCell(board1 != null ? board1[i][j] : '~') + " ");
            }

            System.out.print(gap);

            System.out.printf(WHITE_BOLD + " %2d " + RESET, (i + 1));
            for (int j = 0; j < 16; j++) {
                System.out.print(colorizeCell(board2 != null ? board2[i][j] : '~') + " ");
            }
            System.out.println();
        }
    }

    public void renderDraftBoard(List<ShipPlacementDto> draftShips) {
        clearScreen();
        System.out.println(WHITE_BOLD + "╔════════════════════════════ ВАКАНТНЫЕ МЕСТА ДЛЯ КРЕЙСЕРОВ ══════════════════════════╗" + RESET);
        System.out.println("  Кораблей укомплектовано в доке: " + CYAN + draftShips.size() + RESET + " из " + GREEN + "21" + RESET + " необходимых.");
        System.out.println(WHITE_BOLD + "╚═════════════════════════════════════════════════════════════════════════════════════╝" + RESET);

        printInstructions();

        char[][] board = new char[16][16];
        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                board[i][j] = '~';
            }
        }

        for (ShipPlacementDto ship : draftShips) {
            int size = ship.size();
            int x = ship.startX();
            int y = ship.startY();
            boolean isVertical = ship.isVertical();

            for (int i = 0; i < size; i++) {
                if (isVertical && (y + i) < 16 && x < 16) {
                    board[y + i][x] = '■';
                } else if (!isVertical && (x + i) < 16 && y < 16) {
                    board[y][x + i] = '■';
                }
            }
        }

        String boardLetters = YELLOW + "A B C D E F G H I J K L M N O P" + RESET;
        System.out.println("\n    " + boardLetters);

        for (int i = 0; i < 16; i++) {
            System.out.printf(WHITE_BOLD + " %2d " + RESET, (i + 1));
            for (int j = 0; j < 16; j++) {
                System.out.print(colorizeCell(board[i][j]) + " ");
            }
            System.out.println();
        }

        System.out.print("\n" + BOLD + "⌨️  Штабной терминал > Введите команду: " + RESET);
    }

    public void renderGamesList(ActiveGamesResponse response) {
        clearScreen();
        System.out.println(WHITE_BOLD + "╔═══════════════════════════════ СПИСОК ДОСТУПНЫХ СЕКТОРОВ ═══════════════════════════╗" + RESET);

        if (response == null || response.getGamesList() == null || response.getGamesList().isEmpty()) {
            System.out.println("║" + YELLOW + "  На сервере сейчас нет открытых комнат.                                        " + RESET + "     ║");
            System.out.println("║" + GREEN + "  Станьте первопроходцем — создайте новую комнату через главное меню!          " + RESET + "      ║");
            System.out.println(WHITE_BOLD + "╚═════════════════════════════════════════════════════════════════════════════════════╝" + RESET);
            return;
        }

        System.out.printf("║ " + YELLOW + " %-10s " + RESET + "│" + YELLOW + " %-16s " + RESET + "│" + YELLOW + " %-16s " + RESET + "│" + YELLOW + " %-13s " + RESET + "║\n",
                "ID СЕССИИ", "ИГРОК-СОЗДАТЕЛЬ", "ОППОНЕНТ", "ТИП ИГРЫ");
        System.out.println("╠═════════════╬══════════════════╬══════════════════╬═══════════════╣");

        for (GameSessionDto game : response.getGamesList()) {
            String opponent = game.opponentName() != null ? game.opponentName() : "<ОЖИДАНИЕ...>";
            String typeStr = game.typeSession() != null ? game.typeSession().toString() : "MULTIPLAYER";
            String translatedType = typeStr.equalsIgnoreCase("SINGLEPLAYER") ? "С БОТОМ" : "ОНЛАЙН";

            System.out.printf("║  %-10d │ %-16s │ %-16s │ %-13s ║\n",
                    game.gameSessionId(),
                    game.creatorName(),
                    opponent,
                    translatedType
            );
        }
        System.out.println(WHITE_BOLD + "╚═════════════╩══════════════════╩══════════════════╩═══════════════╝" + RESET);
    }

    public void renderAdminMenu(List<GameSessionDto> games, int currentPage, int totalPage) {
        clearScreen();
        System.out.println("╔══════════════════════════════ ПАНЕЛЬ АДМИНИСТРАТОРА ═══════════════════════════════╗");
        System.out.println("║" + RESET + "  next / prev                  - Переключение страниц истории сессий                " + "║");
        System.out.println("║" + RESET + "  watch <ID>                   - Пошаговый просмотр (реплей) всей игры              " + "║");
        System.out.println("║" + RESET + "  delete <ID>                  - Полное безвозвратное удаление сессии               " + "║");
        System.out.println("║" + RESET + "  archive <ID>                 - Перенос сессии в архив базы данных                 " + "║");
        System.out.println("║" + RESET + "  back                         - Вернуться в главное меню                           " + "║");
        System.out.println("╠════════════════════════════════════════════════════════════════════════════════════╣");

        if (games == null || games.isEmpty()) {
            System.out.println("║" + YELLOW + "  Завершенных игровых сессий в глобальной истории не найдено.                   " + "    ║" + RESET);
            System.out.println("╚════════════════════════════════════════════════════════════════════════════════════╝" + RESET);
            return;
        }

        System.out.printf("║ " + YELLOW + " %-10s " + RESET + "║" + YELLOW + " %-16s " + RESET + "║" + YELLOW + " %-16s " + RESET + "║" + YELLOW + " %-13s " + RESET + "                 ║\n", "ID СЕССИИ", "ИГРОК-СОЗДАТЕЛЬ", "ОППОНЕНТ", "ТИП ИГРЫ");
        System.out.println("╠═════════════╬══════════════════╬══════════════════╬════════════════════════════════╣" + RESET);

        for (GameSessionDto game : games) {
            String opponent = game.opponentName() != null ? game.opponentName() : "<НЕТ>";
            String typeStr = game.typeSession() != null ? game.typeSession().toString() : "MULTIPLAYER";
            String translatedType = typeStr.equalsIgnoreCase("SINGLEPLAYER") ? "С БОТОМ" : "ОНЛАЙН";

            System.out.printf("║  %-10d ║ %-16s ║ %-16s ║ %-13s                  ║\n", game.gameSessionId(), game.creatorName(), opponent, translatedType);
        }
        System.out.println("╠═════════════╩══════════════════╩══════════════════╩════════════════════════════════╣");
        System.out.printf("║  " + GREEN + "Страница %d из %d" + RESET + "                                                                   ║\n", (currentPage + 1), totalPage);
        System.out.println(WHITE_BOLD + "╚════════════════════════════════════════════════════════════════════════════════════╝" + RESET);
        System.out.print("\n" + BOLD + "⌨️  Панель админа > Введите команду: " + RESET);
    }

    public void renderAdminReplayStep(GameTurnDto turn, String creator, String opponent) {
        clearScreen();
        System.out.println(WHITE_BOLD + "╔═══════════════════════════════ ПОШАГОВЫЙ РЕПЛЕЙ МАТЧА ══════════════════════════════╗" + RESET);
        System.out.printf("  Ход №: %d | Ходил: %s | Выстрел: %s | Результат: %s\n",
                turn.turnNumber(), turn.shooterName(), turn.coordinate(), turn.shotResult());
        System.out.println(WHITE_BOLD + "╚══════════════════════════════════════════════════════════════════════════════════════╝" + RESET);

        renderTwoBoards("ФЛОТ " + creator.toUpperCase(), turn.p1Board(), "ФЛОТ " + opponent.toUpperCase(), turn.p2Board());
        System.out.println("\n" + YELLOW + "⌨️ Нажмите [Enter] для воспроизведения следующего хода или введите 'back'..." + RESET);
    }
}