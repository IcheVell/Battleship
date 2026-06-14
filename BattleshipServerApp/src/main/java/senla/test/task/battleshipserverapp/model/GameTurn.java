package senla.test.task.battleshipserverapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import senla.test.task.battleshipserverapp.converter.BoardConverter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "game_turns")
public class GameTurn {
    private final int SIZE = 16;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_session_id")
    private GameSession gameSession;

    private int turnNumber;

    @ManyToOne
    @JoinColumn(name = "player_who_moved_id")
    private Player playerWhoMoved;

    private int targetX;

    private int targetY;

    @Convert(converter = BoardConverter.class)
    @Column(columnDefinition = "TEXT")
    private char[][] player1ShipsBoard;

    @Convert(converter = BoardConverter.class)
    @Column(columnDefinition = "TEXT")
    private char[][] player1ShotsBoard;

    @Convert(converter = BoardConverter.class)
    @Column(columnDefinition = "TEXT")
    private char[][] player2ShipsBoard;

    @Convert(converter = BoardConverter.class)
    @Column(columnDefinition = "TEXT")
    private char[][] player2ShotsBoard;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void copyBoards(GameTurn other) {
        this.gameSession = other.getGameSession();
        this.turnNumber = other.getTurnNumber();
        this.playerWhoMoved = other.getPlayerWhoMoved();
        this.targetX = other.getTargetX();
        this.targetY = other.getTargetY();

        this.player1ShipsBoard = other.getPlayer1ShipsBoard() != null ? other.getPlayer1ShipsBoard().clone() : null;
        this.player1ShotsBoard = other.getPlayer1ShotsBoard() != null ? other.getPlayer1ShotsBoard().clone() : null;
        this.player2ShipsBoard = other.getPlayer2ShipsBoard() != null ? other.getPlayer2ShipsBoard().clone() : null;
        this.player2ShotsBoard = other.getPlayer2ShotsBoard() != null ? other.getPlayer2ShotsBoard().clone() : null;
    }
}
