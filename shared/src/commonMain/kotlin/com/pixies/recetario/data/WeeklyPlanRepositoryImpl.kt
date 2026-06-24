package com.pixies.recetario.data

import com.pixies.recetario.data.local.dao.RecipeOverviewDao
import com.pixies.recetario.data.local.dao.WeeklyPlanDao
import com.pixies.recetario.data.local.dao.WeeklyPlanSlotDao
import com.pixies.recetario.data.mapper.toDomain
import com.pixies.recetario.data.mapper.toEntity
import com.pixies.recetario.data.mapper.toSlotEntities
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.model.WeeklyPlan
import com.pixies.recetario.domain.repository.WeeklyPlanReader
import com.pixies.recetario.domain.repository.WeeklyPlanWriter

class WeeklyPlanRepositoryImpl(
    private val planDao: WeeklyPlanDao,
    private val slotDao: WeeklyPlanSlotDao,
    private val overviewDao: RecipeOverviewDao
) : WeeklyPlanReader, WeeklyPlanWriter {

    override suspend fun getAllPlans(): List<WeeklyPlan> =
        planDao.getAllPlans().map { entity ->
            entity.toDomain(assembleSlots(entity.id))
        }

    override suspend fun savePlan(plan: WeeklyPlan) {
        val resolvedId = planDao.insert(plan.toEntity())
        slotDao.deleteSlotsForPlan(resolvedId)
        slotDao.insertAll(plan.toSlotEntities(resolvedId))
        val recipes = plan.daySlots.values.flatten()
        if (recipes.isNotEmpty()) overviewDao.insertAll(recipes.map { it.toEntity() })
    }

    override suspend fun deletePlan(id: Long) {
        planDao.deleteById(id)
        slotDao.deleteSlotsForPlan(id)
    }

    private suspend fun assembleSlots(planId: Long): Map<Int, List<RecipeOverview>> =
        slotDao.getSlotsForPlan(planId)
            .mapNotNull { slot ->
                overviewDao.getRecipeById(slot.recipeId)?.let { entity ->
                    slot.dayIndex to entity.toDomain()
                }
            }
            .groupBy({ it.first }, { it.second })
}
