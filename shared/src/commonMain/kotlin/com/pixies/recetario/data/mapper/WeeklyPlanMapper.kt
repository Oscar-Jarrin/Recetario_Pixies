package com.pixies.recetario.data.mapper

import com.pixies.recetario.data.local.entity.WeeklyPlanEntity
import com.pixies.recetario.data.local.entity.WeeklyPlanSlotEntity
import com.pixies.recetario.domain.model.RecipeOverview
import com.pixies.recetario.domain.model.WeeklyPlan

fun WeeklyPlan.toEntity(): WeeklyPlanEntity =
    WeeklyPlanEntity(id = id, planName = planName)

fun WeeklyPlan.toSlotEntities(resolvedPlanId: Long): List<WeeklyPlanSlotEntity> =
    daySlots.map { (dayIndex, recipe) ->
        WeeklyPlanSlotEntity(planId = resolvedPlanId, dayIndex = dayIndex, recipeId = recipe.id)
    }

fun WeeklyPlanEntity.toDomain(daySlots: Map<Int, RecipeOverview>): WeeklyPlan =
    WeeklyPlan(id = id, planName = planName, daySlots = daySlots)
