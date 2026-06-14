package response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include =  JsonTypeInfo.As.PROPERTY,
        property = "action"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = response.GameStateObserverResponse.class, name = "GAME_STATE_OBSERVER"),
        @JsonSubTypes.Type(value = response.GameStatePlayerResponse.class, name = "GAME_STATE_PLAYER"),
        @JsonSubTypes.Type(value = ActiveGamesResponse.class, name = "ACTIVE_GAMES"),
        @JsonSubTypes.Type(value = CreateGameResponse.class, name = "CREATE_GAME"),
        @JsonSubTypes.Type(value = ShipPlacementGameStateResponse.class, name = "SHIP_PLACEMENT_GAME_STATE"),
        @JsonSubTypes.Type(value = ConnectToGameResponse.class, name = "CONNECT_TO_GAME"),
        @JsonSubTypes.Type(value = ShootResponse.class, name = "SHOOT"),
        @JsonSubTypes.Type(value = WinResponse.class, name = "WIN"),
        @JsonSubTypes.Type(value = AdminGameHistoryResponse.class, name = "ADMIN_GAME_HISTORY"),
        @JsonSubTypes.Type(value = AdminActionWatchResponse.class, name = "ADMIN_ACTION_WATCH"),
        @JsonSubTypes.Type(value = AdminActionDeleteResponse.class, name = "ADMIN_ACTION_DELETE"),
})
public abstract class BaseResponse {
}
