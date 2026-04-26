package com.example.progressify

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val surname: String = "",
    val nickname: String = "",
    val createdAt: Timestamp? = null,
    val experiencePoints: Int = 0,
    val level: Int = 1,
    val completedTasksCount: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val streakDates: List<String> = emptyList()
)