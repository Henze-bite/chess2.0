package com.example.chessapp2.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.chessapp2.engine.ChessEngine
import com.example.chessapp2.models.Game
import kotlin.math.min

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    interface Listener {
        fun onGameUpdated(game: Game)
    }

    var listener: Listener? = null

    private var cellSize = 0f
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 12f * resources.displayMetrics.scaledDensity
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 80, 170, 80) }
    private val movePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 170, 200, 80) }
    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 0, 0) }

    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private var game: Game? = null
    private var selectedSquare: String? = null
    private var legalFromSelected: List<ChessEngine.Move> = emptyList()

    fun setGame(g: Game) {
        game = g
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
        cellSize = size / 8f
        piecePaint.textSize = cellSize * 0.6f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBoard(canvas)
        drawHighlights(canvas)
        drawPieces(canvas)
        drawCoordinates(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val x = col * cellSize
                val y = row * cellSize
                boardPaint.color = if ((row + col) % 2 == 0) Color.parseColor("#F0D9B5") else Color.parseColor("#B58863")
                canvas.drawRect(x, y, x + cellSize, y + cellSize, boardPaint)
            }
        }
        val g = game ?: return
        if (g.check) {
            val kingSquare = g.boardState.entries.find { it.value == "${g.currentTurn}_king" }?.key
            if (kingSquare != null) {
                val (row, col) = squareToRowCol(kingSquare)
                val x = col * cellSize
                val y = row * cellSize
                canvas.drawRect(x, y, x + cellSize, y + cellSize, checkPaint)
            }
        }
    }

    private fun drawCoordinates(canvas: Canvas) {
        val files = "abcdefgh"
        for (col in 0 until 8) {
            canvas.drawText(files[col].toString(), col * cellSize + cellSize - 12f, height - 6f, coordPaint)
        }
        for (row in 0 until 8) {
            canvas.drawText("${8 - row}", 6f, row * cellSize + 14f, coordPaint)
        }
    }

    private fun drawHighlights(canvas: Canvas) {
        selectedSquare?.let { sel ->
            val (r, c) = squareToRowCol(sel)
            canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, selectedPaint)
            for (m in legalFromSelected) {
                val (mr, mc) = squareToRowCol(m.to)
                canvas.drawRect(mc * cellSize, mr * cellSize, (mc + 1) * cellSize, (mr + 1) * cellSize, movePaint)
            }
        }
    }

    private fun drawPieces(canvas: Canvas) {
        val g = game ?: return
        for ((sq, piece) in g.boardState) {
            val (row, col) = squareToRowCol(sq)
            val cx = col * cellSize + cellSize / 2f
            val cy = row * cellSize + cellSize / 2f + (piecePaint.textSize * 0.33f)
            piecePaint.color = if (piece.startsWith("white")) Color.BLACK else Color.WHITE
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (piece.startsWith("white")) Color.parseColor("#EFEFEF") else Color.parseColor("#333333")
            }
            canvas.drawCircle(col * cellSize + cellSize / 2f, row * cellSize + cellSize / 2f, cellSize * 0.42f, bgPaint)
            canvas.drawText(pieceSymbol(piece), cx, cy, piecePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        if (event.action != MotionEvent.ACTION_DOWN) return true

        val col = (event.x / cellSize).toInt().coerceIn(0, 7)
        val row = (event.y / cellSize).toInt().coerceIn(0, 7)
        val sq = rowColToSquare(row, col)

        val g = game ?: return true
        if (g.checkmate || g.stalemate || g.isDraw) {
            Toast.makeText(context, "Game over", Toast.LENGTH_SHORT).show()
            return true
        }

        val piece = g.boardState[sq]
        if (selectedSquare == null) {
            if (piece != null && piece.startsWith(g.currentTurn)) {
                selectedSquare = sq
                legalFromSelected = ChessEngine.legalMovesFrom(g, sq)
                invalidate()
            }
        } else {
            val move = legalFromSelected.firstOrNull { it.to == sq }
            if (move != null) {
                if (move.promotion != null) {
                    showPromotionDialog { choice -> applyMove(move, choice) }
                } else {
                    applyMove(move, null)
                }
            }
            selectedSquare = null
            legalFromSelected = emptyList()
            invalidate()
        }
        return true
    }

    private fun applyMove(move: ChessEngine.Move, promotion: String?) {
        val g = game ?: return
        val next = ChessEngine.applyMove(g, move, preferPromotion = promotion)
        game = next
        invalidate()
        listener?.onGameUpdated(next)

        when {
            next.checkmate -> Toast.makeText(context, "Checkmate! ${next.winner} wins", Toast.LENGTH_SHORT).show()
            next.stalemate -> Toast.makeText(context, "Stalemate (draw)", Toast.LENGTH_SHORT).show()
            next.isDraw -> Toast.makeText(context, "Draw", Toast.LENGTH_SHORT).show()
            next.check -> Toast.makeText(context, "Check!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPromotionDialog(onChosen: (String) -> Unit) {
        val options = arrayOf("queen", "rook", "bishop", "knight")
        AlertDialog.Builder(context)
            .setTitle("Promote to")
            .setItems(options) { _, which -> onChosen(options[which]) }
            .setCancelable(false)
            .show()
    }

    // --- utils

    private fun squareToRowCol(square: String): Pair<Int, Int> {
        val file = square[0] - 'a'
        val rank = square[1].digitToInt()
        val row = 8 - rank
        val col = file
        return row to col
    }

    private fun rowColToSquare(row: Int, col: Int): String {
        val file = ('a'.code + col).toChar()
        val rank = 8 - row
        return "$file$rank"
    }

    private fun pieceSymbol(piece: String): String = when (piece) {
        "white_king" -> "♔"
        "white_queen" -> "♕"
        "white_rook" -> "♖"
        "white_bishop" -> "♗"
        "white_knight" -> "♘"
        "white_pawn" -> "♙"
        "black_king" -> "♚"
        "black_queen" -> "♛"
        "black_rook" -> "♜"
        "black_bishop" -> "♝"
        "black_knight" -> "♞"
        "black_pawn" -> "♟"
        else -> "?"
    }
}
