package com.example.chessapp2.data


import com.example.chessapp2.models.GameDoc
import kotlinx.coroutines.tasks.await

class GameRepository(
    private val db: com.google.firebase.firestore.FirebaseFirestore = FirebaseProvider.db
) {
    suspend fun createGame(whiteUid: String, blackUid: String?, rated: Boolean, mode: String, timeControl: String): String {
        val game = GameDoc(
            whiteUid = whiteUid,
            blackUid = blackUid ?: "",
            participants = listOfNotNull(whiteUid, blackUid),
            status = if (blackUid == null) "waiting" else "in_progress",
            rated = rated,
            mode = mode,
            timeControl = timeControl
        )
        val ref = db.collection("games").add(game).await()
        return ref.id
    }

    suspend fun joinGame(gameId: String, blackUid: String) {
        val ref = db.collection("games").document(gameId)
        ref.update(
            mapOf(
                "blackUid" to blackUid,
                "participants" to com.google.firebase.firestore.FieldValue.arrayUnion(blackUid),
                "status" to "in_progress",
                "startedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    suspend fun appendMove(gameId: String, uciOrSan: String, turn: String, whiteTimeMs: Long, blackTimeMs: Long, moveNumber: Int) {
        val ref = db.collection("games").document(gameId)
        ref.update(
            mapOf(
                "moves" to com.google.firebase.firestore.FieldValue.arrayUnion(uciOrSan),
                "turn" to turn,
                "whiteTimeMs" to whiteTimeMs,
                "blackTimeMs" to blackTimeMs,
                "moveNumber" to moveNumber
            )
        ).await()
    }

    suspend fun finishGame(gameId: String, result: String, winnerUid: String?, termination: String, finalFen: String?) {
        val ref = db.collection("games").document(gameId)
        ref.update(
            mapOf(
                "status" to "finished",
                "result" to result,
                "winnerUid" to winnerUid,
                "termination" to termination,
                "finalFen" to finalFen,
                "endedAt" to System.currentTimeMillis()
            )
        ).await()
    }
}
