package response;

import dto.GameSessionDto;

import java.util.List;

public class AdminActionDeleteResponse extends BaseResponse {
    private List<GameSessionDto> gameSessionList;
    private int currentPage;
    private int totalPage;
}
