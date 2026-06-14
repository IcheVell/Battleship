package mapper;

public class CoordinateMapper {

    public static int[] parseToNumbers(String input) {
        String clean = input.toUpperCase().replaceAll("\\s+", "");
        clean = clean.replace('А', 'A')
                .replace('В', 'B')
                .replace('С', 'C')
                .replace('Е', 'E')
                .replace('Н', 'H')
                .replace('М', 'M')
                .replace('О', 'O')
                .replace('Р', 'P');

        String letters = clean.replaceAll("[^A-Z]", "");
        String numbers = clean.replaceAll("[^0-9]", "");

        if (letters.isEmpty() || numbers.isEmpty()) {
            throw new IllegalArgumentException("Invalid format");
        }

        int column = letters.charAt(0) - 'A' + 1;
        int row = Integer.parseInt(numbers);

        return new int[]{row, column};
    }

    public static String toDisplayFormat(String input) {
        try {
            int[] coords = parseToNumbers(input);
            char letter = (char) ('A' + coords[1] - 1);
            return letter + String.valueOf(coords[0]);
        } catch (Exception e) {
            return input;
        }
    }
}