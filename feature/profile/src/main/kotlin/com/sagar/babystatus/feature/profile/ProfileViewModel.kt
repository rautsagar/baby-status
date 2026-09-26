package com.sagar.babystatus.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagar.babystatus.core.data.repository.BabyRepository
import com.sagar.babystatus.core.data.repository.PhotoStore
import com.sagar.babystatus.core.ui.PhotoSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class ProfileUiState(
    val mode: ProfileMode,
    val isLoading: Boolean,
    val name: String = "",
    val birthDate: LocalDate? = null,
    /** A picked photo stays a content uri until Save copies it into app storage. */
    val photo: PhotoSource? = null,
    val today: LocalDate,
    val isSaving: Boolean = false,
    val photoFailed: Boolean = false,
    val isDone: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && !isSaving && name.isNotBlank() && birthDate != null
}

@HiltViewModel(assistedFactory = ProfileViewModel.Factory::class)
class ProfileViewModel @AssistedInject constructor(
    @Assisted private val key: ProfileKey,
    private val babyRepository: BabyRepository,
    private val photoStore: PhotoStore,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(key: ProfileKey): ProfileViewModel
    }

    private val _uiState = MutableStateFlow(
        ProfileUiState(mode = key.mode, isLoading = key.mode == ProfileMode.EDIT, today = LocalDate.now(clock)),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        if (key.mode == ProfileMode.EDIT) {
            viewModelScope.launch {
                val baby = babyRepository.getBaby()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = baby?.name.orEmpty(),
                        birthDate = baby?.birthDate,
                        photo = baby?.photoPath?.let(PhotoSource::Stored),
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name) }

    fun onBirthDateChanged(date: LocalDate) = _uiState.update { it.copy(birthDate = date) }

    fun onPhotoPicked(uri: String) = _uiState.update { it.copy(photo = PhotoSource.Picked(uri), photoFailed = false) }

    fun save() {
        val state = _uiState.value
        val birthDate = state.birthDate
        if (!state.canSave || birthDate == null) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val newPhotoPath = when (val photo = state.photo) {
                is PhotoSource.Picked -> try {
                    photoStore.importPhoto(photo.uri)
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // The picked image couldn't be read; keep the form so they can pick another.
                    _uiState.update { it.copy(isSaving = false, photoFailed = true) }
                    return@launch
                }
                is PhotoSource.Stored -> photo.path
                null -> null
            }
            when (key.mode) {
                ProfileMode.CREATE -> babyRepository.create(state.name, birthDate, newPhotoPath)
                ProfileMode.EDIT -> {
                    val baby = checkNotNull(babyRepository.getBaby())
                    babyRepository.update(baby.copy(name = state.name.trim(), birthDate = birthDate, photoPath = newPhotoPath))
                    val oldPath = baby.photoPath
                    if (oldPath != null && oldPath != newPhotoPath) photoStore.delete(oldPath)
                }
            }
            _uiState.update { it.copy(isSaving = false, isDone = true) }
        }
    }
}
