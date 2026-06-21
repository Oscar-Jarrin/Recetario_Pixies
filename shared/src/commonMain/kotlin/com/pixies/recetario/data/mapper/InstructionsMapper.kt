package com.pixies.recetario.data.mapper

import com.pixies.recetario.data.local.entity.RecipeInstructionStepEntity
import com.pixies.recetario.data.remote.dto.InstructionGroupDto

fun List<InstructionGroupDto>.toEntities(recipeId: Int): List<RecipeInstructionStepEntity> =
    flatMap { group ->
        group.steps.map { step ->
            RecipeInstructionStepEntity(
                recipeId = recipeId,
                groupName = group.name,
                stepOrder = step.number,
                stepText = step.step
            )
        }
    }

fun List<RecipeInstructionStepEntity>.toInstructionsMap(): Map<String, List<String>> =
    groupBy { it.groupName }
        .mapValues { (_, steps) -> steps.sortedBy { it.stepOrder }.map { it.stepText } }
