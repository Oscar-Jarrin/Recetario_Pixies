package com.pixies.recetario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixies.recetario.data.local.entity.WeeklyPlanSlotEntity

@Dao
interface WeeklyPlanSlotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<WeeklyPlanSlotEntity>)

    @Query("SELECT * FROM weekly_plan_slots WHERE planId = :planId")
    suspend fun getSlotsForPlan(planId: Long): List<WeeklyPlanSlotEntity>

    @Query("DELETE FROM weekly_plan_slots WHERE planId = :planId")
    suspend fun deleteSlotsForPlan(planId: Long)
}
