package request;

import enums.PlayerRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConnectToGameRequest extends BaseRequest {
    private Long gameSessionId;
    private PlayerRole role;
}
