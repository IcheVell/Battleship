package response;

import enums.GameStatus;
import response.BaseResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStatePlayerResponse extends BaseResponse {
    private Long gameSessionId;
    private String p1Name;
    private String p2Name;
    private LocalDateTime startTime;
    private int p1AliveShipsCount;
    private int p2AliveShipsCount;
    private char[][] myShipsBoard;
    private char[][] myShotsBoard;
    private GameStatus status;
}