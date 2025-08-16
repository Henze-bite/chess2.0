package com.example.chessapp2.data

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseProvider {
    // Call once from Application or first Activity if you like:
    fun init(appContext: android.content.Context) {
        if (FirebaseApp.getApps(appContext).isEmpty()) FirebaseApp.initializeApp(appContext)
    }

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
}
