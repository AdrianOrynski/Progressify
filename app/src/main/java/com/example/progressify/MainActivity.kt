package com.example.progressify

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.progressify.screens.auth.LoginScreen
import com.example.progressify.screens.auth.RegisterScreen
import com.example.progressify.ui.theme.ProgressifyTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationScheduler.createChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
        enableEdgeToEdge()

        setContent {
            ProgressifyTheme {
                var userProfile by remember { mutableStateOf<User?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    val currentUser = auth.currentUser

                    if (currentUser == null) {
                        auth.signInWithEmailAndPassword("adrorynski@gmail.com", "AdminTest")
                            .addOnSuccessListener { result ->
                                loadUser(result.user?.uid) { userProfile = it; isLoading = false }
                            }
                            .addOnFailureListener {
                                errorMessage = "Login error"; isLoading = false
                            }
                    } else {
                        loadUser(currentUser.uid) { userProfile = it; isLoading = false }
                    }
                }

                if (isLoading) {
                    FantasyLoadingIndicator(modifier = Modifier.fillMaxSize())
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = "auth_graph",
                        enterTransition = { fadeIn(tween(400)) },
                        exitTransition = { fadeOut(tween(400)) }
                    ) {
                        navigation(startDestination = "login", route = "auth_graph") {
                            composable("login") { LoginScreen(navController) }
                            composable("register") { RegisterScreen(navController) }
                        }
                        composable("main_app/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: ""
                            var user by remember { mutableStateOf(userProfile) }

                            LaunchedEffect(uid) {
                                if (uid.isNotBlank()) {
                                    loadUser(uid) { user = it }
                                }
                            }

                            MainApp(user = user)
                        }
                    }
                }
            }
        }
    }

    private fun loadUser(uid: String?, onLoaded: (User?) -> Unit) {
        if (uid == null) return
        userRepository.getUserData(
            uid = uid,
            onSuccess = { user -> onLoaded(user) },
            onFailure = { onLoaded(null) }
        )
    }
}