package dto;

public record GameTurnDto(int turnNumber, String shooterName, String coordinate, String shotResult, char[][] p1Board, char[][] p2Board) { }
