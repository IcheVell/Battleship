package response;

import dto.GameSessionDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveGamesResponse extends BaseResponse {
    private List<GameSessionDto> gamesList;
}
