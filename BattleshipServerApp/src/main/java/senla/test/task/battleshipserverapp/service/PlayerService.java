package senla.test.task.battleshipserverapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import senla.test.task.battleshipserverapp.model.Player;
import senla.test.task.battleshipserverapp.repository.PlayerRepository;

@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerRepository playerRepository;

    public Player login(String username) {
        return playerRepository.findByUsername(username).orElseGet(() -> playerRepository.save(new Player(username)));
    }
}
