package senla.test.task.battleshipserverapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Player {
    @GeneratedValue
    @Id
    private Long id;

    @Column(unique = true)
    private String username;

    public Player(String username) {
        this.username = username;
    }
}
