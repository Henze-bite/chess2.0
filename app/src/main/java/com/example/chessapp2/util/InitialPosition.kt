package com.example.chessapp2.util

import com.example.chessapp2.models.Game

fun initialGame(): Game {
    val board = mutableMapOf<String, String>()

    // Pawns
    for (f in 'a'..'h') {
        board["${f}2"] = "white_pawn"
        board["${f}7"] = "black_pawn"
    }

    // White back rank
    board["a1"] = "white_rook"
    board["b1"] = "white_knight"
    board["c1"] = "white_bishop"
    board["d1"] = "white_queen"
    board["e1"] = "white_king"
    board["f1"] = "white_bishop"
    board["g1"] = "white_knight"
    board["h1"] = "white_rook"

    // Black back rank
    board["a8"] = "black_rook"
    board["b8"] = "black_knight"
    board["c8"] = "black_bishop"
    board["d8"] = "black_queen"
    board["e8"] = "black_king"
    board["f8"] = "black_bishop"
    board["g8"] = "black_knight"
    board["h8"] = "black_rook"

    return Game(boardState = board.toMutableMap())
}
