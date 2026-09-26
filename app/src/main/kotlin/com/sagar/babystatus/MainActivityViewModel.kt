package com.sagar.babystatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.domain.DeleteEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainActivityUiState {
    /** Keeps the splash screen up until we know where to start. */
    data object Loading : MainActivityUiState

    data class Ready(val hasBaby: Boolean) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    babyRepository: BabyRepository,
    private val deleteEntry: DeleteEntryUseCase,
) : ViewModel() {

    val uiState: StateFlow<MainActivityUiState> = babyRepository.baby
        .map { MainActivityUiState.Ready(hasBaby = it != null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainActivityUiState.Loading)

    /** Every delete, wherever it happened, so one snackbar can offer Undo. */
    val deletedIds: SharedFlow<String> = deleteEntry.deletedIds

    fun undoDelete(id: String) {
        viewModelScope.launch { deleteEntry.undo(id) }
    }
}
