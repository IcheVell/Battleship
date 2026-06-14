import controller.BattleshipClientController;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        BattleshipClientController controller = new BattleshipClientController();

        System.out.println("=================================================================");
        System.out.println("          ⚓  ДОБРО ПОЖАЛОВАТЬ В КЛИЕНТ «МОРСКОЙ БОЙ»  ⚓          ");
        System.out.println("=================================================================");
        System.out.println("  • Чтобы выйти из игры в любой момент, введите команду: exit    ");
        System.out.println("=================================================================\n");

        System.out.print("[Система] Установка соединения с сервером... ");
        controller.startApplication();
        System.out.println("Успешно подключено!\n");

        Scanner scanner = new Scanner(System.in);

        System.out.print("👉 Введите ваш игровой псевдоним (никнейм): ");
        String nickname = scanner.nextLine().trim();

        if ("exit".equalsIgnoreCase(nickname)) {
            System.out.println("🚪 Выход из игры. До новых встреч, капитан!");
            controller.disconnect();
            scanner.close();
            return;
        }

        controller.login(nickname);

        controller.showMainMenu();

        while (true) {
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input.trim())) {
                System.out.println("\n🚪 Выходим из игры... Спасибо за службу, капитан!");
                break;
            }

            controller.handleUserInput(input);
        }

        controller.disconnect();
        scanner.close();
    }
}