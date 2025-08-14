package com.example.chessapp2.engine

import com.example.chessapp2.models.Game
import kotlin.math.abs

/**
 * Complete chess rules engine:
 * - Legal move generation for all pieces
 * - Checks, checkmate, stalemate
 * - Castling, en passant, promotion
 * - 3-fold repetition (position key includes side/castling/EP), 50-move rule
 * - Common insufficient-material detection
 */
object ChessEngine {

    data class Move(
        val from: String,
        val to: String,
        val piece: String,
        val capture: String? = null,
        val promotion: String? = null,
        val isCastleKingSide: Boolean = false,
        val isCastleQueenSide: Boolean = false,
        val isEnPassant: Boolean = false,
        val isDoublePawnPush: Boolean = false
    )

    // --- Public API

    fun isInCheck(game: Game, color: String = game.currentTurn): Boolean {
        val kingSquare = game.boardState.entries.find { it.value == "${color}_king" }?.key ?: return false
        return isSquareAttacked(game, kingSquare, opponent(color))
    }

    fun generateLegalMoves(game: Game): List<Move> {
        val color = game.currentTurn
        return generatePseudoLegal(game, color).filter { mv -> !leavesOwnKingInCheck(game, mv) }
    }

    fun legalMovesFrom(game: Game, from: String): List<Move> {
        val piece = game.boardState[from] ?: return emptyList()
        if (!piece.startsWith(game.currentTurn)) return emptyList()
        return generatePseudoFrom(game, from, piece).filter { mv -> !leavesOwnKingInCheck(game, mv) }
    }

    fun applyMove(game: Game, move: Move, preferPromotion: String? = null): Game {
        val finalPromotion = move.promotion ?: if (isPromotionMove(game, move)) (preferPromotion ?: "queen") else null
        val updated = applyMoveUnchecked(game, move.copy(promotion = finalPromotion))

        val them = updated.currentTurn
        val us = opponent(them)
        val theirInCheck = isInCheck(updated, them)
        val theirMoves = generateLegalMoves(updated)

        val insufficient = isInsufficientMaterial(updated.boardState)
        val threeFold = isThreefold(updated)
        val fiftyMove = updated.halfMoveClock >= 100

        val checkmate = theirInCheck && theirMoves.isEmpty()
        val stalemate = !theirInCheck && theirMoves.isEmpty()
        val isDraw = insufficient || threeFold || fiftyMove || stalemate
        val winner = if (checkmate) us else null

        return updated.copy(
            check = theirInCheck,
            checkmate = checkmate,
            stalemate = stalemate,
            isDraw = isDraw,
            winner = winner
        )
    }

    // --- Pseudo-legal generators

    private fun generatePseudoLegal(game: Game, color: String): List<Move> {
        val moves = mutableListOf<Move>()
        for ((sq, piece) in game.boardState) {
            if (!piece.startsWith(color)) continue
            moves += generatePseudoFrom(game, sq, piece)
        }
        return moves
    }

    private fun generatePseudoFrom(game: Game, from: String, piece: String): List<Move> {
        val color = colorOf(piece)
        return when (typeOf(piece)) {
            "pawn"   -> pawnMoves(game, from, color)
            "knight" -> knightMoves(game, from, color)
            "bishop" -> slidingMoves(game, from, color, listOf(+1 to +1, +1 to -1, -1 to +1, -1 to -1))
            "rook"   -> slidingMoves(game, from, color, listOf(+1 to 0, -1 to 0, 0 to +1, 0 to -1))
            "queen"  -> slidingMoves(game, from, color, listOf(+1 to 0, -1 to 0, 0 to +1, 0 to -1, +1 to +1, +1 to -1, -1 to +1, -1 to -1))
            "king"   -> kingMoves(game, from, color)
            else -> emptyList()
        }
    }

    private fun pawnMoves(game: Game, from: String, color: String): List<Move> {
        val (fr, fc) = toRC(from)
        val dir = if (color == "white") +1 else -1
        val res = mutableListOf<Move>()

        // one forward
        val oneR = fr + dir
        if (oneR in 1..8) {
            val one = rc(oneR, fc)
            if (game.boardState[one] == null) {
                res += Move(from, one, "${color}_pawn", promotion = if (isPromotionRank(oneR, color)) "queen" else null)
                // two forward from start
                val startRank = if (color == "white") 2 else 7
                if (fr == startRank) {
                    val twoR = fr + 2 * dir
                    val two = rc(twoR, fc)
                    if (twoR in 1..8 && game.boardState[two] == null) {
                        res += Move(from, two, "${color}_pawn", isDoublePawnPush = true)
                    }
                }
            }
        }

        // captures
        for (dc in listOf(-1, +1)) {
            val r = fr + dir
            val c = fc + dc
            if (r in 1..8 && c in 1..8) {
                val target = rc(r, c)
                val victim = game.boardState[target]
                if (victim != null && !victim.startsWith(color)) {
                    res += Move(from, target, "${color}_pawn", capture = victim, promotion = if (isPromotionRank(r, color)) "queen" else null)
                }
            }
        }

        // en passant
        val ep = game.enPassantTarget
        if (ep != null) {
            val (er, ec) = toRC(ep)
            if (er == fr + dir && abs(ec - fc) == 1) {
                val capturedSquare = rc(fr, ec)
                val victim = game.boardState[capturedSquare]
                if (victim == "${opponent(color)}_pawn") {
                    res += Move(from, ep, "${color}_pawn", capture = victim, isEnPassant = true)
                }
            }
        }
        return res
    }

    private fun knightMoves(game: Game, from: String, color: String): List<Move> {
        val (r, c) = toRC(from)
        val deltas = listOf(+2 to +1, +2 to -1, -2 to +1, -2 to -1, +1 to +2, +1 to -2, -1 to +2, -1 to -2)
        val res = mutableListOf<Move>()
        for ((dr, dc) in deltas) {
            val nr = r + dr
            val nc = c + dc
            if (nr !in 1..8 || nc !in 1..8) continue
            val to = rc(nr, nc)
            val victim = game.boardState[to]
            if (victim == null || !victim.startsWith(color)) {
                res += Move(from, to, "${color}_knight", capture = victim)
            }
        }
        return res
    }

    private fun slidingMoves(game: Game, from: String, color: String, directions: List<Pair<Int, Int>>): List<Move> {
        val (r, c) = toRC(from)
        val type = typeOf(game.boardState[from]!!)
        val res = mutableListOf<Move>()
        for ((dr, dc) in directions) {
            var nr = r + dr
            var nc = c + dc
            while (nr in 1..8 && nc in 1..8) {
                val to = rc(nr, nc)
                val victim = game.boardState[to]
                if (victim == null) {
                    res += Move(from, to, "${color}_$type")
                } else {
                    if (!victim.startsWith(color)) {
                        res += Move(from, to, "${color}_$type", capture = victim)
                    }
                    break
                }
                nr += dr
                nc += dc
            }
        }
        return res
    }

    private fun kingMoves(game: Game, from: String, color: String): List<Move> {
        val (r, c) = toRC(from)
        val res = mutableListOf<Move>()
        for (nr in (r - 1)..(r + 1)) {
            for (nc in (c - 1)..(c + 1)) {
                if (nr == r && nc == c) continue
                if (nr !in 1..8 || nc !in 1..8) continue
                val to = rc(nr, nc)
                val victim = game.boardState[to]
                if (victim == null || !victim.startsWith(color)) {
                    res += Move(from, to, "${color}_king", capture = victim)
                }
            }
        }
        res += castleMoves(game, from, color)
        return res
    }

    private fun castleMoves(game: Game, from: String, color: String): List<Move> {
        val res = mutableListOf<Move>()
        val start = if (color == "white") "e1" else "e8"
        if (from != start) return res
        if (isSquareAttacked(game, from, opponent(color))) return res

        // king-side
        if (game.castlingAvailability["${color}_king_side"] == true) {
            val f = if (color == "white") "f1" else "f8"
            val g = if (color == "white") "g1" else "g8"
            val h = if (color == "white") "h1" else "h8"
            if (game.boardState[f] == null && game.boardState[g] == null &&
                game.boardState[h] == "${color}_rook" &&
                !isSquareAttacked(game, f, opponent(color)) &&
                !isSquareAttacked(game, g, opponent(color))
            ) {
                res += Move(from, g, "${color}_king", isCastleKingSide = true)
            }
        }

        // queen-side
        if (game.castlingAvailability["${color}_queen_side"] == true) {
            val d = if (color == "white") "d1" else "d8"
            val c = if (color == "white") "c1" else "c8"
            val b = if (color == "white") "b1" else "b8"
            val a = if (color == "white") "a1" else "a8"
            if (game.boardState[d] == null && game.boardState[c] == null && game.boardState[b] == null &&
                game.boardState[a] == "${color}_rook" &&
                !isSquareAttacked(game, d, opponent(color)) &&
                !isSquareAttacked(game, c, opponent(color))
            ) {
                res += Move(from, c, "${color}_king", isCastleQueenSide = true)
            }
        }
        return res
    }

    // --- Attacks / legality

    private fun isSquareAttacked(game: Game, square: String, byColor: String): Boolean {
        val (tr, tc) = toRC(square)

        // pawns (from squares that could capture into target)
        val dir = if (byColor == "white") +1 else -1
        for (dc in listOf(-1, +1)) {
            val r = tr - dir
            val c = tc - dc
            if (r in 1..8 && c in 1..8) {
                val s = rc(r, c)
                if (game.boardState[s] == "${byColor}_pawn") return true
            }
        }

        // knights
        val knightD = listOf(+2 to +1, +2 to -1, -2 to +1, -2 to -1, +1 to +2, +1 to -2, -1 to +2, -1 to -2)
        for ((dr, dc) in knightD) {
            val r = tr + dr
            val c = tc + dc
            if (r in 1..8 && c in 1..8 && game.boardState[rc(r, c)] == "${byColor}_knight") return true
        }

        // bishops/queens (diagonals)
        if (rayAttacked(game, tr, tc, byColor, listOf(+1 to +1, +1 to -1, -1 to +1, -1 to -1), setOf("bishop", "queen"))) return true

        // rooks/queens (orthogonal)
        if (rayAttacked(game, tr, tc, byColor, listOf(+1 to 0, -1 to 0, 0 to +1, 0 to -1), setOf("rook", "queen"))) return true

        // king
        for (r in (tr - 1)..(tr + 1)) {
            for (c in (tc - 1)..(tc + 1)) {
                if (r == tr && c == tc) continue
                if (r in 1..8 && c in 1..8 && game.boardState[rc(r, c)] == "${byColor}_king") return true
            }
        }

        return false
    }

    private fun rayAttacked(
        game: Game,
        tr: Int,
        tc: Int,
        byColor: String,
        directions: List<Pair<Int, Int>>,
        okTypes: Set<String>
    ): Boolean {
        for ((dr, dc) in directions) {
            var r = tr + dr
            var c = tc + dc
            while (r in 1..8 && c in 1..8) {
                val s = rc(r, c)
                val p = game.boardState[s]
                if (p != null) {
                    if (p.startsWith(byColor) && okTypes.contains(typeOf(p))) return true
                    break
                }
                r += dr
                c += dc
            }
        }
        return false
    }

    private fun leavesOwnKingInCheck(game: Game, move: Move): Boolean {
        val after = applyMoveUnchecked(game, move)
        val meNow = opponent(after.currentTurn)
        return isInCheck(after, meNow)
    }

    // --- Apply move (no end-state flags here)

    private fun applyMoveUnchecked(game: Game, move: Move): Game {
        val color = colorOf(move.piece)
        val their = opponent(color)

        val newBoard = game.boardState.toMutableMap()
        val fromPiece = newBoard[move.from] ?: error("No piece on ${move.from}")
        require(fromPiece.startsWith(color)) { "Wrong side to move" }

        var halfClock = game.halfMoveClock
        var enPassant: String? = null
        var fullMove = game.fullMoveNumber
        val newCastles = game.castlingAvailability.toMutableMap()

        // half-move clock
        val isPawnMove = typeOf(fromPiece) == "pawn"
        if (isPawnMove || move.capture != null || move.isEnPassant) halfClock = 0 else halfClock++

        // en passant capture removes pawn behind target
        if (move.isEnPassant) {
            val (_, toC) = toRC(move.to)
            val (fr, _) = toRC(move.from)
            val capSquare = rc(fr, toC)
            newBoard.remove(capSquare)
        }

        // move piece (with promotion if any)
        newBoard.remove(move.from)
        var placed = fromPiece
        if (move.promotion != null && typeOf(fromPiece) == "pawn") {
            placed = "${color}_${move.promotion}"
        }
        newBoard[move.to] = placed

        // king moves -> update castling + rook hop if castling
        if (typeOf(fromPiece) == "king") {
            newCastles["${color}_king_side"] = false
            newCastles["${color}_queen_side"] = false

            if (move.isCastleKingSide) {
                val rookFrom = if (color == "white") "h1" else "h8"
                val rookTo   = if (color == "white") "f1" else "f8"
                newBoard.remove(rookFrom)?.let { newBoard[rookTo] = it }
            } else if (move.isCastleQueenSide) {
                val rookFrom = if (color == "white") "a1" else "a8"
                val rookTo   = if (color == "white") "d1" else "d8"
                newBoard.remove(rookFrom)?.let { newBoard[rookTo] = it }
            }
        }

        // rook moved or captured -> update rights
        fun touchRook(square: String) {
            when (square) {
                "a1" -> newCastles["white_queen_side"] = false
                "h1" -> newCastles["white_king_side"] = false
                "a8" -> newCastles["black_queen_side"] = false
                "h8" -> newCastles["black_king_side"] = false
            }
        }
        if (typeOf(fromPiece) == "rook") touchRook(move.from)
        if (move.capture != null && typeOf(move.capture) == "rook") touchRook(move.to)

        // double pawn push -> set EP target
        if (move.isDoublePawnPush && typeOf(fromPiece) == "pawn") {
            val (fr, fc) = toRC(move.from)
            val (tr, _) = toRC(move.to)
            val midRank = (fr + tr) / 2
            enPassant = rc(midRank, fc)
        }

        // full move number increments after black's move
        if (color == "black") fullMove++

        val nextTurn = their

        // update 3-fold key counter
        val newGameTemp = game.copy(
            boardState = newBoard,
            currentTurn = nextTurn,
            castlingAvailability = newCastles,
            enPassantTarget = enPassant,
            halfMoveClock = halfClock,
            fullMoveNumber = fullMove
        )
        val key = positionKey(newGameTemp)
        val posHist = game.positionHistory.toMutableMap()
        posHist[key] = (posHist[key] ?: 0) + 1

        // simple SAN-like tag
        val tag = toSANish(game, move, placed)

        return game.copy(
            boardState = newBoard,
            currentTurn = nextTurn,
            castlingAvailability = newCastles,
            enPassantTarget = enPassant,
            halfMoveClock = halfClock,
            fullMoveNumber = fullMove,
            moveHistory = (game.moveHistory + tag).toMutableList(),
            positionHistory = posHist
        )
    }

    // --- Draw conditions

    private fun isInsufficientMaterial(board: Map<String, String>): Boolean {
        val white = board.values.filter { it.startsWith("white") }.map { typeOf(it) }
        val black = board.values.filter { it.startsWith("black") }.map { typeOf(it) }

        // K vs K
        if (white.size == 1 && black.size == 1) return true

        // K+B vs K, K+N vs K
        if (white.toSet() == setOf("king", "bishop") && black == listOf("king")) return true
        if (black.toSet() == setOf("king", "bishop") && white == listOf("king")) return true
        if (white.toSet() == setOf("king", "knight") && black == listOf("king")) return true
        if (black.toSet() == setOf("king", "knight") && white == listOf("king")) return true

        // K+B vs K+B (bishops on same color)
        if (white.size == 2 && black.size == 2 &&
            white.contains("king") && white.contains("bishop") &&
            black.contains("king") && black.contains("bishop")
        ) {
            val wb = board.entries.find { it.value == "white_bishop" }?.key
            val bb = board.entries.find { it.value == "black_bishop" }?.key
            if (wb != null && bb != null) {
                val wColor = squareColor(wb)
                val bColor = squareColor(bb)
                if (wColor == bColor) return true
            }
        }

        // K+NN vs K
        if (white.count { it == "knight" } == 2 && white.size == 3 && black == listOf("king")) return true
        if (black.count { it == "knight" } == 2 && black.size == 3 && white == listOf("king")) return true

        return false
    }

    private fun isThreefold(game: Game): Boolean {
        val key = positionKey(game)
        return (game.positionHistory[key] ?: 0) >= 3
    }

    // --- Helpers

    private fun toSANish(game: Game, move: Move, placed: String): String {
        val pieceChar = when (typeOf(move.piece)) {
            "king" -> "K"; "queen" -> "Q"; "rook" -> "R"; "bishop" -> "B"; "knight" -> "N"
            else -> ""
        }
        if (move.isCastleKingSide) return "O-O"
        if (move.isCastleQueenSide) return "O-O-O"
        val capture = if (move.capture != null || move.isEnPassant) "x" else "-"
        val promo = move.promotion?.let { "=${it.first().uppercase()}" } ?: ""
        val fromFileIfPawnCapture = if (typeOf(move.piece) == "pawn" && capture == "x") fileOf(move.from).toString() else ""
        return "${pieceChar.ifEmpty { fromFileIfPawnCapture }}$capture${move.to}$promo"
    }

    private fun isPromotionMove(game: Game, move: Move): Boolean {
        if (typeOf(move.piece) != "pawn") return false
        val (tr, _) = toRC(move.to)
        val color = colorOf(move.piece)
        return isPromotionRank(tr, color)
    }

    private fun isPromotionRank(rank: Int, color: String) =
        (color == "white" && rank == 8) || (color == "black" && rank == 1)

    private fun positionKey(game: Game): String {
        // pieces + side + castling rights + EP target
        val sb = StringBuilder()
        for (rank in 8 downTo 1) {
            var empty = 0
            for (file in 1..8) {
                val sq = rc(rank, file)
                val p = game.boardState[sq]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(fenChar(p))
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 1) sb.append('/')
        }
        sb.append(' ')
        sb.append(if (game.currentTurn == "white") 'w' else 'b')
        sb.append(' ')
        val cWk = if (game.castlingAvailability["white_king_side"] == true) "K" else ""
        val cWq = if (game.castlingAvailability["white_queen_side"] == true) "Q" else ""
        val cBk = if (game.castlingAvailability["black_king_side"] == true) "k" else ""
        val cBq = if (game.castlingAvailability["black_queen_side"] == true) "q" else ""
        val castles = (cWk + cWq + cBk + cBq).ifEmpty { "-" }
        sb.append(castles)
        sb.append(' ')
        sb.append(game.enPassantTarget ?: "-")
        return sb.toString()
    }

    private fun fenChar(p: String): Char = if (p.startsWith("white"))
        typeOf(p).first().uppercaseChar() else typeOf(p).first().lowercaseChar()

    private fun squareColor(sq: String): Int {
        val (r, c) = toRC(sq)
        return (r + c) % 2
    }

    private fun colorOf(piece: String) = piece.substringBefore('_')
    private fun typeOf(piece: String) = piece.substringAfter('_')
    private fun opponent(color: String) = if (color == "white") "black" else "white"
    private fun fileOf(square: String): Char = square[0]

    private fun toRC(square: String): Pair<Int, Int> {
        val file = (square[0] - 'a') + 1
        val rank = square[1].digitToInt()
        return rank to file
    }
    private fun rc(rank: Int, file: Int): String {
        val f = ('a'.code + (file - 1)).toChar()
        return "$f$rank"
    }
}
