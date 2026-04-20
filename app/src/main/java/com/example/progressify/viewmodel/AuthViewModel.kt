package com.example.progressify.viewmodel

import androidx.lifecycle.ViewModel
import com.example.progressify.User
import com.example.progressify.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String?     = null,
    val isSuccess: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val auth           = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(error = "Fill in all fields, adventurer!")
            return
        }
        _state.value = AuthUiState(isLoading = true)
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { _state.value = AuthUiState(isSuccess = true) }
            .addOnFailureListener { _state.value = AuthUiState(error = mapError(it.message)) }
    }

    fun register(email: String, password: String, confirm: String,
                 nickname: String, name: String, surname: String) {
        when {
            email.isBlank() || password.isBlank() || nickname.isBlank() ->
                _state.value = AuthUiState(error = "Fill in all required fields!")
            password != confirm ->
                _state.value = AuthUiState(error = "Passwords don't match!")
            password.length < 6 ->
                _state.value = AuthUiState(error = "Password must be at least 6 characters!")
            else -> {
                _state.value = AuthUiState(isLoading = true)
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        val newUser = User(
                            uid              = uid,
                            name             = name.trim(),
                            surname          = surname.trim(),
                            nickname         = nickname.trim(),
                            createdAt        = Timestamp.now(),
                            experiencePoints = 0,
                            level            = 1
                        )
                        userRepository.createNewUser(newUser) { success ->
                            _state.value = if (success) AuthUiState(isSuccess = true)
                            else AuthUiState(error = "Profile creation failed. Try again.")
                        }
                    }
                    .addOnFailureListener { _state.value = AuthUiState(error = mapError(it.message)) }
            }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    private fun mapError(msg: String?) = when {
        msg == null                              -> "Unknown error."
        msg.contains("no user record")           -> "No hero found with this email!"
        msg.contains("password is invalid")      -> "Wrong password, impostor!"
        msg.contains("email address is badly")   -> "That doesn't look like a valid email."
        msg.contains("email address is already") -> "This email is already registered!"
        msg.contains("network error")            -> "Connection lost. Check your internet."
        else                                     -> "Something went wrong. Try again."
    }
}
