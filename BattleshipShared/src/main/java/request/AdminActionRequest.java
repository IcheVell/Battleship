package request;

import enums.AdminAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminActionRequest extends BaseRequest {
    AdminAction adminAction;
    Long gameSessionId;
}
