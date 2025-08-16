package com.example.chessapp2.models
data class GameDoc(
    val whiteUid: String = "",
    val blackUid: String = "",
    val participants: List<String> = emptyList(),

    val status: String = "waiting",    // waiting | in_progress | finished
    val rated: Boolean = true,
    val mode: String = "blitz",
    val timeControl: String = "5+0",

    val initialFen: String = "startpos",
    val moves: List<String> = emptyList(), // UCI or SAN
    val pgn: String? = null,

    val whiteTimeMs: Long = 0L,
    val blackTimeMs: Long = 0L,
    val turn: String = "white",
    val moveNumber: Int = 1,

    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val endedAt: Long? = null,

    val result: String? = null,        // "1-0", "0-1", "1/2-1/2"
    val winnerUid: String? = null,
    val termination: String? = null    // checkmate | resign | timeout | stalemate
)
