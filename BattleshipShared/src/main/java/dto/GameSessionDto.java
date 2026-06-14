package dto;

import enums.TypeSession;

public record GameSessionDto(Long gameSessionId, String creatorName, String opponentName, TypeSession typeSession) {
}
