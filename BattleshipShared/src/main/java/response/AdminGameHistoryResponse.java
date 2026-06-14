package response;

import dto.GameSessionDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminGameHistoryResponse extends BaseResponse {
    private List<GameSessionDto> gameSessionList;
    private int currentPage;
    private int totalPage;
}
