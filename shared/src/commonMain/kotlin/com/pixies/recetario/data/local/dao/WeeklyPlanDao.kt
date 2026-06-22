package com.pixies.recetario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixies.recetario.data.local.entity.WeeklyPlanEntity

@Dao
interface WeeklyPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WeeklyPlanEntity): Long

    @Query("SELECT * FROM weekly_plans")
    suspend fun getAllPlans(): List<WeeklyPlanEntity>

    @Query("SELECT * FROM weekly_plans WHERE planName = :name LIMIT 1")
    suspend fun getPlanByName(name: String): WeeklyPlanEntity?

    @Query("DELETE FROM weekly_plans WHERE id = :id")
    suspend fun deleteById(id: Long)
}
