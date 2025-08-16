package com.example.chessapp2.data

import com.example.chessapp2.models.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun signInAnonymouslyIfNeeded(): String {
        val current = auth.currentUser
        if (current != null) {
            ensureUserDoc(current.uid, isGuest = current.isAnonymous)
            return current.uid
        }
        val cred = auth.signInAnonymously().await()
        val uid = cred.user!!.uid
        ensureUserDoc(uid, isGuest = true)
        return uid
    }

    suspend fun registerEmail(email: String, password: String, displayName: String? = null): String {
        // If user is anonymous, link to keep stats
        val user = auth.currentUser
        if (user != null && user.isAnonymous) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.linkWithCredential(credential).await()
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
            ).await()
            ensureUserDoc(user.uid, isGuest = false, displayName = displayName, email = email)
            return user.uid
        }
        // Normal create
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        res.user?.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        )?.await()
        val uid = res.user!!.uid
        ensureUserDoc(uid, isGuest = false, displayName = displayName, email = email)
        return uid
    }

    suspend fun signInEmail(email: String, password: String): String {
        val res = auth.signInWithEmailAndPassword(email, password).await()
        val uid = res.user!!.uid
        ensureUserDoc(uid, isGuest = false, email = email, displayName = res.user?.displayName)
        return uid
    }

    fun signOut() = auth.signOut()

    private suspend fun ensureUserDoc(
        uid: String,
        isGuest: Boolean,
        displayName: String? = null,
        email: String? = null
    ) {
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            val user = User(
                uid = uid, isGuest = isGuest,
                displayName = displayName, email = email
            )
            ref.set(user).await()
        } else {
            ref.update(
                mapOf(
                    "lastSeen" to System.currentTimeMillis(),
                    "isGuest" to isGuest
                )
            ).await()
        }
    }
}
