package request;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "action"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LoginRequest.class, name = "LOGIN"),
        @JsonSubTypes.Type(value = CreateGameRequest.class, name = "CREATE_GAME"),
        @JsonSubTypes.Type(value = FireRequest.class, name = "FIRE"),
        @JsonSubTypes.Type(value = ActiveGamesRequest.class, name = "ACTIVE_GAMES"),
        @JsonSubTypes.Type(value = AdminGameHistoryRequest.class, name = "ADMIN_GAME_HISTORY"),
        @JsonSubTypes.Type(value = AdminActionRequest.class, name = "ADMIN_ACTION"),
        @JsonSubTypes.Type(value = ConnectToGameRequest.class, name = "CONNECT_TO_GAME"),
        @JsonSubTypes.Type(value = ShipPlacementRequest.class, name = "SHIP_PLACEMENT"),
        @JsonSubTypes.Type(value = SurrenderRequest.class, name = "SURRENDER"),
})
public abstract class BaseRequest {
}
