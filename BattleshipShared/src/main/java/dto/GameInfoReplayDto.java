package dto;

import enums.TypeSession;

import java.util.List;

public record GameInfoReplayDto(String creatorName, String opponentName, List<GameTurnDto> turns) {
}
