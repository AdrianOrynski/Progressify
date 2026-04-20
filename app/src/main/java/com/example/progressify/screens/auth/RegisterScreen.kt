package com.example.progressify.screens.auth

import androidx.compose.animation.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

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
            Spacer(Modifier.height(16.dp))
            IconButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FantasyGold)
            }

            Spacer(Modifier.height(8.dp))

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
                text      = "BEGIN JOURNEY",
                onClick   = { vm.register(email, password, confirm, nickname, name, surname) },
                isLoading = state.isLoading
            )

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick  = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Already a hero? Sign in", color = ParchmentDim,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
