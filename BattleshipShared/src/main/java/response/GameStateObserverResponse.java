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
public class GameStateObserverResponse extends BaseResponse {
    private Long gameSessionId;
    private String p1Name;
    private String p2Name;
    private String currentTurnPlayerName;
    private LocalDateTime startTime;
    private int p1AliveShipsCount;
    private int p2AliveShipsCount;
    private char[][] p1ShipsBoard;
    private char[][] p1ShotsBoard;
    private char[][] p2ShipsBoard;
    private char[][] p2ShotsBoard;
    private GameStatus status;
}