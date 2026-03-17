package com.example.progressify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.progressify.ui.theme.ProgressifyTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProgressifyTheme {
                var userProfile by remember { mutableStateOf<User?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UserProfileScreen(
                        user = userProfile,
                        isLoading = isLoading,
                        error = errorMessage,
                        modifier = Modifier.padding(innerPadding)
                    )
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

@Composable
fun UserProfileScreen(user: User?, isLoading: Boolean, error: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (isLoading) {
            Text("Loading data...")
        } else if (error != null) {
            Text(text = error, color = androidx.compose.ui.graphics.Color.Red)
        } else if (user != null) {
            Column (
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Hello, ${user.nickname}!", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Text(text = "${user.name} ${user.surname}")

                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(text = "Level: ${user.level}", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Text(text = "XP: ${user.experiencePoints}")

                if (user.createdAt != null) {
                    Text(
                        text = "At Progressify since: ${user.createdAt.toDate().toLocaleString()}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
        } else {
            Text("User profile not found.")
        }
    }
}