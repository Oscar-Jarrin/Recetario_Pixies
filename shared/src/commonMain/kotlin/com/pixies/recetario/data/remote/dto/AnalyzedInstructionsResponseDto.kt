package com.pixies.recetario.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InstructionGroupDto(val name: String, val steps: List<StepDto>)

@Serializable
data class StepDto(val number: Int, val step: String)
