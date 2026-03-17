package com.example.progressify

import com.example.progressify.User
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUserData(uid: String, onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                onSuccess(user)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun createNewUser(user: User, onComplete: (Boolean) -> Unit) {
        db.collection("users").document(user.uid).set(user)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}