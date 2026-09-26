package com.sagar.babystatus.core.domain

import com.sagar.babystatus.core.data.repository.TimelineRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deletes (softly) any timeline entry, the baby's or Mom's, and announces it, so one
 * app-level snackbar can offer Undo no matter where the delete happened: a History swipe
 * or the Delete button in an edit sheet.
 */
@Singleton
class DeleteEntryUseCase @Inject constructor(
    private val timelineRepository: TimelineRepository,
) {
    private val _deletedIds = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val deletedIds: SharedFlow<String> = _deletedIds.asSharedFlow()

    suspend operator fun invoke(id: String) {
        timelineRepository.delete(id)
        _deletedIds.emit(id)
    }

    suspend fun undo(id: String) = timelineRepository.undoDelete(id)
}
