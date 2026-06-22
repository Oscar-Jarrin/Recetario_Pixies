package com.pixies.recetario.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_plans", indices = [Index(value = ["planName"], unique = true)])
data class WeeklyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planName: String
)
