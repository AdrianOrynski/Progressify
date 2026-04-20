package com.example.progressify.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Timestamp, Timestamp, RecurrenceRule) -> Unit
) {
    var title        by remember { mutableStateOf("") }
    var desc         by remember { mutableStateOf("") }
    var selectedCat  by remember { mutableStateOf("") }
    var selectedDiff by remember { mutableStateOf(Difficulty.MEDIUM.name) }

    var startDate   by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var startHour   by remember { mutableStateOf<Int?>(null) }
    var startMinute by remember { mutableStateOf<Int?>(null) }
    var endDate     by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var endHour     by remember { mutableStateOf<Int?>(null) }
    var endMinute   by remember { mutableStateOf<Int?>(null) }

    var titleError by remember { mutableStateOf(false) }
    var timeError  by remember { mutableStateOf(false) }

    var recurrenceType by remember { mutableStateOf(RecurrenceType.NONE.name) }
    var selectedDays   by remember { mutableStateOf(setOf<Int>()) }
    var interval       by remember { mutableIntStateOf(1) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun buildTimestamp(date: Triple<Int, Int, Int>?, hour: Int?, minute: Int?): Timestamp? {
        if (date == null || hour == null || minute == null) return null
        val cal = Calendar.getInstance()
        cal.set(date.first, date.second, date.third, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Timestamp(cal.time)
    }

    fun dateLabel(date: Triple<Int, Int, Int>?): String {
        if (date == null) return "Date"
        val cal = Calendar.getInstance().apply { set(date.first, date.second, date.third) }
        return dateFmt.format(cal.time)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(AncientBrown, DarkWood)),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.5.dp, FantasyGold, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text("POST A BOUNTY",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold, color = FantasyGold))
                Spacer(Modifier.height(20.dp))

                FantasyTextField(
                    value         = title,
                    onValueChange = { title = it; titleError = false },
                    label         = "Task Title *",
                    isError       = titleError
                )
                if (titleError) Text("Title is required",
                    color = DragonRedLight, style = MaterialTheme.typography.labelSmall)

                Spacer(Modifier.height(12.dp))
                FantasyTextField(value = desc, onValueChange = { desc = it }, label = "Description")
                Spacer(Modifier.height(16.dp))

                Text("CATEGORY", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                Spacer(Modifier.height(8.dp))

                val cats      = TaskCategory.entries
                val firstRow  = cats.take(3)
                val secondRow = cats.drop(3)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(firstRow, secondRow).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { cat ->
                                CategorySelectChip(
                                    label      = cat.label,
                                    isSelected = selectedCat == cat.label,
                                    onClick    = { selectedCat = if (selectedCat == cat.label) "" else cat.label },
                                    modifier   = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("DIFFICULTY", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { diff ->
                        val isSelected = selectedDiff == diff.name
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) difficultyColor(diff.name).copy(alpha = 0.3f) else AncientBrownLight,
                                    RoundedCornerShape(6.dp))
                                .border(1.dp,
                                    if (isSelected) difficultyColor(diff.name) else ParchmentDim.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp))
                                .clickable { selectedDiff = diff.name }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(diff.label,
                                style      = MaterialTheme.typography.labelMedium,
                                color      = if (isSelected) difficultyColor(diff.name) else ParchmentDim,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("TIME WINDOW *",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (timeError) DragonRedLight else ParchmentDim)
                Spacer(Modifier.height(8.dp))

                TimeRow(
                    label      = "Start",
                    date       = startDate,
                    hour       = startHour,
                    minute     = startMinute,
                    dateLabel  = ::dateLabel,
                    onPickDate = { showStartDatePicker = true },
                    onPickTime = { showStartTimePicker = true }
                )
                Spacer(Modifier.height(6.dp))
                TimeRow(
                    label      = "End",
                    date       = endDate,
                    hour       = endHour,
                    minute     = endMinute,
                    dateLabel  = ::dateLabel,
                    onPickDate = { showEndDatePicker = true },
                    onPickTime = { showEndTimePicker = true }
                )

                if (timeError) Text("Set start and end date & time",
                    color = DragonRedLight, style = MaterialTheme.typography.labelSmall)

                Spacer(Modifier.height(16.dp))

                Text("RECURRENCE", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceType.entries.forEach { type ->
                        CategorySelectChip(
                            label      = type.label,
                            isSelected = recurrenceType == type.name,
                            onClick    = { recurrenceType = type.name; selectedDays = setOf() }
                        )
                    }
                }

                if (recurrenceType == RecurrenceType.SELECTED_DAYS.name) {
                    Spacer(Modifier.height(10.dp))
                    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEachIndexed { index, label ->
                            val dayNum     = index + 1
                            val isSelected = selectedDays.contains(dayNum)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) DeepDragonRed else AncientBrownLight, CircleShape)
                                    .border(1.dp,
                                        if (isSelected) FantasyGold else ParchmentDim.copy(alpha = 0.3f),
                                        CircleShape)
                                    .clickable {
                                        selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                                    }
                            ) {
                                Text(label,
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = if (isSelected) Parchment else ParchmentDim,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                if (recurrenceType != RecurrenceType.NONE.name
                    && recurrenceType != RecurrenceType.DAILY.name
                    && recurrenceType != RecurrenceType.SELECTED_DAYS.name
                ) {
                    Spacer(Modifier.height(10.dp))
                    val intervalLabel = when (recurrenceType) {
                        RecurrenceType.WEEKLY.name  -> "Every X weeks"
                        RecurrenceType.MONTHLY.name -> "Every X months"
                        RecurrenceType.YEARLY.name  -> "Every X years"
                        else                        -> "Interval"
                    }
                    Row(verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(intervalLabel, style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                        Row(verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { if (interval > 1) interval-- },
                                modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Remove, null, tint = FantasyGold, modifier = Modifier.size(16.dp))
                            }
                            Text("$interval", style = MaterialTheme.typography.titleLarge,
                                color = FantasyGold, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { interval++ }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Add, null, tint = FantasyGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, ParchmentDim)) {
                        Text("CANCEL", color = ParchmentDim)
                    }
                    Button(
                        onClick = {
                            val startTs = buildTimestamp(startDate, startHour, startMinute)
                            val endTs   = buildTimestamp(endDate, endHour, endMinute)
                            titleError  = title.isBlank()
                            timeError   = startTs == null || endTs == null
                            if (!titleError && !timeError) {
                                onConfirm(
                                    title, desc, selectedCat, selectedDiff,
                                    startTs!!, endTs!!,
                                    RecurrenceRule(
                                        type         = recurrenceType,
                                        selectedDays = selectedDays.toList().sorted(),
                                        interval     = interval
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(4.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = DeepDragonRed)
                    ) { Text("CONFIRM", color = Parchment, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showStartDatePicker) {
        FantasyDatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onConfirm = { y, m, d -> startDate = Triple(y, m, d); timeError = false; showStartDatePicker = false }
        )
    }
    if (showStartTimePicker) {
        FantasyTimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = { h, min -> startHour = h; startMinute = min; timeError = false; showStartTimePicker = false }
        )
    }
    if (showEndDatePicker) {
        FantasyDatePickerDialog(
            onDismiss = { showEndDatePicker = false },
            onConfirm = { y, m, d -> endDate = Triple(y, m, d); timeError = false; showEndDatePicker = false }
        )
    }
    if (showEndTimePicker) {
        FantasyTimePickerDialog(
            onDismiss = { showEndTimePicker = false },
            onConfirm = { h, min -> endHour = h; endMinute = min; timeError = false; showEndTimePicker = false }
        )
    }
}

@Composable
private fun TimeRow(
    label    : String,
    date     : Triple<Int, Int, Int>?,
    hour     : Int?,
    minute   : Int?,
    dateLabel: (Triple<Int, Int, Int>?) -> String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ParchmentDim,
            modifier = Modifier.width(34.dp))

        OutlinedButton(
            onClick        = onPickDate,
            modifier       = Modifier.weight(1f),
            shape          = RoundedCornerShape(6.dp),
            border         = BorderStroke(1.dp, if (date != null) FantasyGold else ParchmentDim.copy(alpha = 0.4f)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.DateRange, null,
                tint = if (date != null) FantasyGold else ParchmentDim, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(dateLabel(date), color = if (date != null) FantasyGold else ParchmentDim,
                style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedButton(
            onClick        = onPickTime,
            modifier       = Modifier.weight(1f),
            shape          = RoundedCornerShape(6.dp),
            border         = BorderStroke(1.dp, if (hour != null) FantasyGold else ParchmentDim.copy(alpha = 0.4f)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.Schedule, null,
                tint = if (hour != null) FantasyGold else ParchmentDim, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                if (hour != null && minute != null) String.format("%02d:%02d", hour, minute) else "Time",
                color = if (hour != null) FantasyGold else ParchmentDim,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
