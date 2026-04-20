package com.example.progressify

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
        val batch = db.batch()
        val userRef = db.collection("users").document(user.uid)

        batch.set(userRef, user)

        TaskCategory.entries.forEach { category ->
            val categoryRef = userRef.collection("categories").document(category.name)
            batch.set(categoryRef, CategoryStats())
        }
        batch.commit()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }
}