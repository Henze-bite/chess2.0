package com.example.chessapp2

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.chessapp2.game.ChessBoardView
import com.example.chessapp2.models.Game
import com.example.chessapp2.util.initialGame
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity(), ChessBoardView.Listener {

    private lateinit var board: ChessBoardView
    private lateinit var toolbar: MaterialToolbar
    private var game: Game = initialGame()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        board = findViewById(R.id.chessBoard)
        board.listener = this
        board.setGame(game)

        updateToolbarSubtitle()

        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_reset -> {
                    game = initialGame()
                    board.setGame(game)
                    updateToolbarSubtitle()
                    true
                }
                else -> false
            }
        }
    }

    override fun onGameUpdated(game: Game) {
        this.game = game
        updateToolbarSubtitle()
    }

    private fun updateToolbarSubtitle() {
        val status = when {
            game.checkmate -> "Checkmate — ${game.winner?.replaceFirstChar { it.uppercase() }} wins"
            game.stalemate -> "Stalemate (draw)"
            game.isDraw -> "Draw"
            game.check -> "Check — ${game.currentTurn.replaceFirstChar { it.uppercase() }} to move"
            else -> "${game.currentTurn.replaceFirstChar { it.uppercase() }} to move"
        }
        toolbar.subtitle = status
    }
}
