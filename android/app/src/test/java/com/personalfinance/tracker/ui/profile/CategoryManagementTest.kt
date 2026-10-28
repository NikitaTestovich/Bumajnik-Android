package com.personalfinance.tracker.ui.profile

import com.personalfinance.tracker.data.FakeFinanceRepository
import com.personalfinance.tracker.data.repository.restoredCategory
import com.personalfinance.tracker.domain.model.Category
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.first
import com.personalfinance.tracker.domain.model.CategoryIcons
import com.personalfinance.tracker.domain.model.UserProfile
import com.personalfinance.tracker.ui.onboarding.WelcomeViewModel

class CategoryManagementTest {
    @Test fun renameAndArchiveRetainIdentityAndRejectInvalidNames() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveCategory(Category("groceries", "Продукты", "Groceries", false))
        repository.saveCategory(Category("custom", "Такси", "Taxi", true))
        val vm = ProfileViewModel(repository)
        assertFalse(vm.renameCategory("groceries", " ", "home"))
        assertFalse(vm.renameCategory("groceries", " taxi ", "home"))
        assertTrue(vm.renameCategory("groceries", "Food", "dining"))
        assertTrue(vm.renameCategory("custom", "Rides", "transport"))
        assertTrue(vm.archiveCategory("groceries"))
        val row = repository.observeCategories().first().first { it.id == "groceries" }
        assertEquals("Food", row.nameRu)
        assertEquals("Food", row.nameEn)
        assertEquals("dining", row.iconKey)
        assertTrue(row.isArchived)
        assertTrue(vm.addCustomCategory("Food", "en"))
        assertTrue(restoredCategory("Food", listOf(row)).isArchived)
    }

    @Test fun archivedStandardAndRenamedCustomSurviveReseeding() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveProfile(UserProfile("Demo", "ru", "USD", true))
        repository.saveCategory(Category("groceries", "Еда", "Еда", false, true, "dining"))
        WelcomeViewModel(repository, "en").completeOnboarding("Demo", "USD")
        ProfileViewModel(repository).updateProfile("Demo", "en", "USD")
        WelcomeViewModel(repository, "en").completeOnboarding("Demo", "USD")
        val row = repository.observeCategories().first().first { it.id == "groceries" }
        assertTrue(row.isArchived)
        assertEquals("Еда", row.nameEn)
        assertEquals("dining", row.iconKey)
    }

    @Test fun iconRegistryHasTwentyStableKeysAndFallback() {
        assertEquals(20, CategoryIcons.keys.toSet().size)
        assertEquals("other", CategoryIcons.resolve("unknown"))
        assertEquals("home", CategoryIcons.forStandard("housing_utilities"))
    }
    @Test fun duplicateActiveNameIsRejectedAfterNormalization() = runBlocking {
        val repository = FakeFinanceRepository()
        repository.saveCategory(Category("groceries", "Продукты", "Groceries", false))
        assertFalse(ProfileViewModel(repository).addCustomCategory("  GROCERIES  ", "en"))
    }

    @Test fun restoredIdCollisionDoesNotReuseDifferentlyRenamedCategory() {
        val original = restoredCategory("Food", emptyList())
        val renamed = original.copy(nameRu = "Travel", nameEn = "Travel")
        val restored = restoredCategory("Food", listOf(renamed))
        assertNotEquals(renamed.id, restored.id)
        assertEquals("Food", restored.nameEn)
    }
}
