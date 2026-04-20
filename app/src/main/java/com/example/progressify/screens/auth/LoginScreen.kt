package com.example.progressify.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

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

            Text("⚔️", fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text  = "PROGRESSIFY",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = FantasyGold
                )
            )
            Text(
                text  = "ENTER THE REALM",
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentDim,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(48.dp))
            GoldDivider(label = "CREDENTIALS")
            Spacer(Modifier.height(28.dp))

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
                    modifier  = Modifier.fillMaxWidth().padding(top = 10.dp)
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

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { navController.navigate("register") }) {
                    Text(text = "New Hero? Register", color = FantasyGold,
                        style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = { }) {
                    Text(text = "Forgot password?", color = ParchmentDim,
                        style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
