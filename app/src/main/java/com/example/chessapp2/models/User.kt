package com.example.chessapp2.models

data class User(
    val uid: String = "",
    val displayName: String? = null,
    val username: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isGuest: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val theme: String = "dark",
    val boardStyle: String = "classic",
    val soundOn: Boolean = true,
    val vibrationOn: Boolean = true,
    val eloRapid: Int = 1200,
    val eloBlitz: Int = 1200,
    val eloBullet: Int = 1200,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val gamesPlayed: Int = 0
)
