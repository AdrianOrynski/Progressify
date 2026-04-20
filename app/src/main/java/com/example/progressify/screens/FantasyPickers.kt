package com.example.progressify.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.progressify.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FantasyDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                            .apply { timeInMillis = millis }
                        onConfirm(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    }
                    onDismiss()
                },
                shape  = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepDragonRed)
            ) { Text("CONFIRM", color = Parchment, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, ParchmentDim)) {
                Text("CANCEL", color = ParchmentDim)
            }
        },
        shape  = RoundedCornerShape(12.dp),
        colors = DatePickerDefaults.colors(containerColor = AncientBrown)
    ) {
        DatePicker(
            state  = state,
            colors = DatePickerDefaults.colors(
                containerColor             = AncientBrown,
                titleContentColor          = FantasyGold,
                headlineContentColor       = Parchment,
                weekdayContentColor        = ParchmentDim,
                subheadContentColor        = ParchmentDim,
                navigationContentColor     = FantasyGold,
                yearContentColor           = Parchment,
                currentYearContentColor    = FantasyGold,
                selectedYearContentColor   = Parchment,
                selectedYearContainerColor = DeepDragonRed,
                dayContentColor            = Parchment,
                disabledDayContentColor    = IronGray,
                selectedDayContentColor    = DarkWood,
                selectedDayContainerColor  = FantasyGold,
                todayContentColor          = FantasyGold,
                todayDateBorderColor       = FantasyGold,
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FantasyTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(AncientBrown, DarkWood)), RoundedCornerShape(12.dp))
                .border(1.5.dp, FantasyGold, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SELECT TIME",
                    style    = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color    = FantasyGold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 20.dp))
                TimePicker(
                    state  = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor                       = AncientBrownLight,
                        clockDialSelectedContentColor        = DarkWood,
                        clockDialUnselectedContentColor      = Parchment,
                        selectorColor                        = FantasyGold,
                        containerColor                       = Color.Transparent,
                        periodSelectorBorderColor            = FantasyGold,
                        timeSelectorSelectedContainerColor   = DeepDragonRed,
                        timeSelectorUnselectedContainerColor = AncientBrownLight,
                        timeSelectorSelectedContentColor     = FantasyGold,
                        timeSelectorUnselectedContentColor   = ParchmentDim,
                    )
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, ParchmentDim)) {
                        Text("CANCEL", color = ParchmentDim)
                    }
                    Button(
                        onClick  = { onConfirm(state.hour, state.minute); onDismiss() },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = DeepDragonRed)
                    ) { Text("CONFIRM", color = Parchment, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
