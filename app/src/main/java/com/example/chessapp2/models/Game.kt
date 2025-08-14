package com.example.chessapp2.models

data class Game(
    val gameId: String? = null,
    val boardState: MutableMap<String, String> = mutableMapOf(),
    val currentTurn: String = "white",
    val castlingAvailability: MutableMap<String, Boolean> = mutableMapOf(
        "white_king_side" to true,
        "white_queen_side" to true,
        "black_king_side" to true,
        "black_queen_side" to true
    ),
    val enPassantTarget: String? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val moveHistory: MutableList<String> = mutableListOf(),
    val positionHistory: MutableMap<String, Int> = mutableMapOf(),
    val check: Boolean = false,
    val checkmate: Boolean = false,
    val stalemate: Boolean = false,
    val isDraw: Boolean = false,
    val winner: String? = null
)
