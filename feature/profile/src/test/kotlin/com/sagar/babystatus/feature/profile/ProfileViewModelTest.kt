package com.sagar.babystatus.feature.profile

import com.sagar.babystatus.core.model.Baby
import com.sagar.babystatus.core.testing.FakeBabyRepository
import com.sagar.babystatus.core.testing.FakePhotoStore
import com.sagar.babystatus.core.testing.MainDispatcherRule
import com.sagar.babystatus.core.testing.MutableClock
import com.sagar.babystatus.core.ui.PhotoSource
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock.at(LocalDateTime.of(2026, 9, 24, 15, 5))
    private val birth = LocalDate.of(2026, 6, 12)

    @Test
    fun `onboarding needs a name and birth date, then creates the baby with the photo`() = runTest {
        val babies = FakeBabyRepository()
        val photos = FakePhotoStore()
        val vm = ProfileViewModel(ProfileKey(ProfileMode.CREATE), babies, photos, clock)

        vm.onNameChanged("  Ava ")
        assertFalse(vm.uiState.value.canSave)
        vm.onBirthDateChanged(birth)
        vm.onPhotoPicked("content://picker/1")
        vm.save()

        assertEquals(Baby("baby-1", "Ava", birth, "photos/imported-1.jpg"), babies.current)
        assertEquals(listOf("content://picker/1"), photos.imported)
        assertTrue(vm.uiState.value.isDone)
    }

    @Test
    fun `editing prefills and replaces the old photo file`() = runTest {
        val babies = FakeBabyRepository(Baby("baby-1", "Ava", birth, "photos/old.jpg"))
        val photos = FakePhotoStore()
        val vm = ProfileViewModel(ProfileKey(ProfileMode.EDIT), babies, photos, clock)

        assertEquals("Ava", vm.uiState.value.name)
        assertEquals(PhotoSource.Stored("photos/old.jpg"), vm.uiState.value.photo)

        vm.onNameChanged("Ava Rose")
        vm.onPhotoPicked("content://picker/2")
        vm.save()

        assertEquals(Baby("baby-1", "Ava Rose", birth, "photos/imported-1.jpg"), babies.current)
        assertEquals(listOf("photos/old.jpg"), photos.deletedPaths)
    }

    @Test
    fun `keeping the photo doesn't import or delete anything`() = runTest {
        val babies = FakeBabyRepository(Baby("baby-1", "Ava", birth, "photos/old.jpg"))
        val photos = FakePhotoStore()
        val vm = ProfileViewModel(ProfileKey(ProfileMode.EDIT), babies, photos, clock)

        vm.onBirthDateChanged(birth.minusDays(1))
        vm.save()

        assertEquals("photos/old.jpg", babies.current?.photoPath)
        assertTrue(photos.imported.isEmpty() && photos.deletedPaths.isEmpty())
    }
}
