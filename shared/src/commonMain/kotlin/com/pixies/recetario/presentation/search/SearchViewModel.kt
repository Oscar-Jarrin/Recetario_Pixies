package com.pixies.recetario.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixies.recetario.domain.usecase.SearchRecipesByIngredientsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val searchRecipes: SearchRecipesByIngredientsUseCase) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    fun search(query: String) {
        _currentQuery.value = query

        if (query.isBlank()) {
            _state.value = SearchState.Idle
            return
        }
        val ingredients = query.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            _state.value = SearchState.Loading
            _state.value = runCatching { SearchState.Success(searchRecipes(ingredients)) }
                .getOrElse { SearchState.Error(it as Exception) }
        }
    }

    fun updateQuery(newQuery: String) {
        _currentQuery.value = newQuery
    }

    fun clearSearch() {
        _currentQuery.value = ""
        _state.value = SearchState.Idle
    }
}
