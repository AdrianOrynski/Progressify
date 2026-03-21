package com.example.progressify

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.progressify.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─────────────────────────────────────────────────────────────────
// AuthViewModel
// ─────────────────────────────────────────────────────────────────

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

    fun register(email: String, password: String, confirm: String, nickname: String, name: String, surname: String) {
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

// ─────────────────────────────────────────────────────────────────
// LoginScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(navController: NavController, vm: AuthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            navController.navigate("main_app/$uid") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0704))
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0A0704),
                        0.4f to Color(0xFF120D07),
                        0.75f to Color(0xFF1A1208),
                        1.0f to Color(0xFF0A0704)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // ── Logo ──────────────────────────────────────────
            Text("⚔️", fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "PROGRESSIFY",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = FantasyGold
                )
            )
            Text(
                text = "ENTER THE REALM",
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentDim,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(48.dp))
            GoldDivider(label = "CREDENTIALS")
            Spacer(Modifier.height(28.dp))

            // ── Pola ─────────────────────────────────────────
            FantasyTextField(
                value           = email,
                onValueChange   = { email = it; vm.clearError() },
                label           = "Hero's Email",
                isError         = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(16.dp))
            PasswordField(
                value         = password,
                onValueChange = { password = it; vm.clearError() },
                label         = "Secret Rune (Password)",
                isError       = state.error != null
            )

            // ── Błąd ─────────────────────────────────────────
            AnimatedVisibility(
                visible = state.error != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text      = state.error ?: "",
                    color     = DragonRedLight,
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            FantasyButton(
                text      = "OPEN THE GATES",
                onClick   = { vm.login(email, password) },
                isLoading = state.isLoading
            )

            Spacer(Modifier.height(24.dp))
            GoldDivider()
            Spacer(Modifier.height(16.dp))

            // ── Linki ─────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { navController.navigate("register") }) {
                    Text(
                        text  = "New Hero? Register",
                        color = FantasyGold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                TextButton(onClick = { }) {
                    Text(
                        text  = "Forgot password?",
                        color = ParchmentDim,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// RegisterScreen
// ─────────────────────────────────────────────────────────────────

@Composable
fun RegisterScreen(navController: NavController, vm: AuthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            navController.navigate("main_app/$uid") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var name     by remember { mutableStateOf("") }
    var surname  by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0704))
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0A0704),
                        0.4f to Color(0xFF120D07),
                        0.75f to Color(0xFF1A1208),
                        1.0f to Color(0xFF0A0704)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(bottom = 48.dp)
        ) {
            // ── Przycisk powrotu ──────────────────────────────
            Spacer(Modifier.height(16.dp))
            IconButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FantasyGold)
            }

            Spacer(Modifier.height(8.dp))

            // ── Nagłówek ──────────────────────────────────────
            Text(
                text  = "NEW ADVENTURER",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = FantasyGold
                )
            )
            Text(
                text     = "CREATE YOUR HERO",
                style    = MaterialTheme.typography.labelLarge,
                color    = ParchmentDim,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // ── Tożsamość ─────────────────────────────────────
            GoldDivider(label = "HERO IDENTITY")
            Spacer(Modifier.height(20.dp))

            FantasyTextField(
                value         = nickname,
                onValueChange = { nickname = it; vm.clearError() },
                label         = "Hero Name (Nickname) *",
                isError       = state.error != null
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FantasyTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = "First Name",
                    modifier      = Modifier.weight(1f)
                )
                FantasyTextField(
                    value         = surname,
                    onValueChange = { surname = it },
                    label         = "Last Name",
                    modifier      = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Dane logowania ────────────────────────────────
            GoldDivider(label = "CREDENTIALS")
            Spacer(Modifier.height(20.dp))

            FantasyTextField(
                value           = email,
                onValueChange   = { email = it; vm.clearError() },
                label           = "Email Address *",
                isError         = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(Modifier.height(12.dp))
            PasswordField(
                value         = password,
                onValueChange = { password = it; vm.clearError() },
                label         = "Password *",
                isError       = state.error != null
            )
            Spacer(Modifier.height(12.dp))
            PasswordField(
                value         = confirm,
                onValueChange = { confirm = it; vm.clearError() },
                label         = "Confirm Password *",
                isError       = state.error != null
            )

            // ── Błąd ─────────────────────────────────────────
            AnimatedVisibility(
                visible = state.error != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text      = state.error ?: "",
                    color     = DragonRedLight,
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            FantasyButton(
                text      = "BEGIN JOURNEY",
                onClick   = { vm.register(email, password, confirm, nickname, name, surname) },
                isLoading = state.isLoading
            )

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text  = "Already a hero? Sign in",
                    color = ParchmentDim,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}