package senla.test.task.battleshipserverapp.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter()
public class BoardConverter implements AttributeConverter<char[][], String> {

    private final int SIZE = 16;

    @Override
    public String convertToDatabaseColumn(char[][] board) {
        if (board == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                sb.append(board[i][j]);
            }
        }

        return sb.toString();
    }

    @Override
    public char[][] convertToEntityAttribute(String data) {
        if (data == null) {
            return null;
        }

        char[][] board = new char[SIZE][SIZE];

        int idx = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = data.charAt(idx++);
            }
        }

        return board;
    }
}
