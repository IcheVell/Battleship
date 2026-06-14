package senla.test.task.battleshipserverapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senla.test.task.battleshipserverapp.model.GameTurn;

import java.util.Optional;

@Repository
public interface GameTurnRepository extends JpaRepository<GameTurn, Long> {
    Optional<GameTurn> findByGameSessionIdAndTurnNumber(Long gameSessionId, Integer turnNumber);
}
