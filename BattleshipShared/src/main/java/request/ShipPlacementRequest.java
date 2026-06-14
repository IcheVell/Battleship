package request;

import dto.ShipPlacementDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipPlacementRequest extends BaseRequest {
    private Long gameSessionId;
    private List<ShipPlacementDto> ships;
}
