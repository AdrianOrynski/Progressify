package com.example.progressify

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progressify.ui.theme.*

// ── Złoty divider z opcjonalnym labelem ─────────────────────────
@Composable
fun GoldDivider(modifier: Modifier = Modifier, label: String? = null) {
    if (label != null) {
        Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = FantasyGoldDim.copy(alpha = 0.5f))
            Text("  $label  ", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
            HorizontalDivider(modifier = Modifier.weight(1f), color = FantasyGoldDim.copy(alpha = 0.5f))
        }
    } else {
        HorizontalDivider(modifier = modifier.fillMaxWidth(), color = FantasyGoldDim.copy(alpha = 0.4f))
    }
}

// ── Karta z gradientem i złotym obramowaniem ─────────────────────
@Composable
fun FantasyCard(
    modifier: Modifier = Modifier,
    borderColor: Color = FantasyGold,
    borderWidth: Dp = 1.5.dp,
    gradient: List<Color> = listOf(Color(0xFF2A1B11), Color(0xFF1B1401)),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(8.dp),
        border    = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column { content() }
        }
    }
}

// ── Animowany pasek XP ───────────────────────────────────────────
@Composable
fun XpProgressBar(currentXp: Int, maxXp: Int, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue   = (currentXp.toFloat() / maxXp).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "xpBar"
    )
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("EXPERIENCE", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
            Text("$currentXp / $maxXp XP", style = MaterialTheme.typography.labelMedium,
                color = FantasyGold, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress      = { animatedProgress },
            modifier      = Modifier.fillMaxWidth().height(12.dp),
            color         = FantasyGold,
            trackColor    = DeepDragonRed.copy(alpha = 0.35f),
            strokeCap     = StrokeCap.Round
        )
    }
}

// ── Główny przycisk ──────────────────────────────────────────────
@Composable
fun FantasyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = DeepDragonRed
) {
    Button(
        onClick   = onClick,
        enabled   = enabled && !isLoading,
        modifier  = modifier.fillMaxWidth().height(52.dp),
        shape     = RoundedCornerShape(4.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor         = containerColor,
            contentColor           = Parchment,
            disabledContainerColor = IronGray,
            disabledContentColor   = ParchmentDim
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Parchment, strokeWidth = 2.dp)
        } else {
            Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        }
    }
}

// ── Pole tekstowe w fantasy stylu ────────────────────────────────
@Composable
fun FantasyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        modifier             = modifier.fillMaxWidth(),
        isError              = isError,
        visualTransformation = visualTransformation,
        keyboardOptions      = keyboardOptions,
        trailingIcon         = trailingIcon,
        singleLine           = true,
        shape                = RoundedCornerShape(6.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = FantasyGold,
            unfocusedBorderColor = ParchmentDim.copy(alpha = 0.4f),
            errorBorderColor     = DragonRedLight,
            focusedLabelColor    = FantasyGold,
            unfocusedLabelColor  = ParchmentDim,
            cursorColor          = FantasyGold,
            focusedTextColor     = Parchment,
            unfocusedTextColor   = ParchmentDim
        )
    )
}

// ── Pomocnik: pole tekstowe z togglem hasła ──────────────────────
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var hidden by remember { mutableStateOf(true) }
    FantasyTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = label,
        modifier             = modifier,
        isError              = isError,
        visualTransformation = if (hidden) androidx.compose.ui.text.input.PasswordVisualTransformation()
        else VisualTransformation.None,
        keyboardOptions      = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
        trailingIcon         = {
            IconButton(onClick = { hidden = !hidden }) {
                Icon(
                    if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint               = ParchmentDim
                )
            }
        }
    )
}

// ── Pulsujący wskaźnik ładowania ─────────────────────────────────
@Composable
fun FantasyLoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "alpha"
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = FantasyGold, modifier = Modifier.size(48.dp).alpha(alpha), strokeWidth = 3.dp)
            Text("LOADING...", style = MaterialTheme.typography.labelLarge, color = ParchmentDim.copy(alpha = alpha))
        }
    }
}