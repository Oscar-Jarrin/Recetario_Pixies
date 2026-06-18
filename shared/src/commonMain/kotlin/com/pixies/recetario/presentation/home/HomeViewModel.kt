package com.pixies.recetario.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixies.recetario.domain.usecase.GetRandomRecipesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val getRandomRecipes: GetRandomRecipesUseCase) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() { load() }

    private fun load() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            _state.value = runCatching { HomeState.Success(getRandomRecipes()) }
                .getOrElse { HomeState.Error(it as Exception) }
        }
    }
}
