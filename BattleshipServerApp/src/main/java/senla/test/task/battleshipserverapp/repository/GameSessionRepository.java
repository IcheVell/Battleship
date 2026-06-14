package senla.test.task.battleshipserverapp.repository;

import dto.GameSessionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import senla.test.task.battleshipserverapp.model.GameSession;

import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    @Query("""
        SELECT new dto.GameSessionDto(
            g.id, 
            p1.username, 
            p2.username,
            g.type
        ) 
        FROM GameSession g 
        JOIN g.player1 p1             
        LEFT JOIN g.player2 p2            
        WHERE g.gameStatus != enums.GameStatus.ENDED
    """)
    List<GameSessionDto> getGamesList();
}
