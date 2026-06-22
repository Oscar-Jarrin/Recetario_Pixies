package com.pixies.recetario.domain.exception

class QuotaExhaustedException : Exception("Spoonacular API quota exhausted")
class NetworkException(cause: Throwable) : Exception(cause)
class RecipeNotFoundException(id: Int) : Exception("Recipe $id not found in cache")
class PlanNotFoundException(name: String) : Exception("Plan '$name' not found")
