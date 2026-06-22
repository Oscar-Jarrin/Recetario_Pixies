package com.pixies.recetario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_plan_slots")
data class WeeklyPlanSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayIndex: Int,
    val recipeId: Int
)
