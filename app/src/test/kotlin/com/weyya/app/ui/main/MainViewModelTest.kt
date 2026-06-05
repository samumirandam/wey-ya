package com.weyya.app.ui.main

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.domain.ScheduleChecker
import com.weyya.app.domain.model.BlockingMode
import com.weyya.app.ui.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prefs = mockk<UserPreferences>(relaxed = true)
    private val blockedCallDao = mockk<BlockedCallDao>(relaxed = true)
    private val scheduleDao = mockk<ScheduleDao>(relaxed = true)
    private val scheduleChecker = ScheduleChecker() // pure domain object, used for real

    @Before
    fun setUp() {
        // ViewModel reads these flows eagerly in its property initializers.
        every { prefs.isActive } returns flowOf(false)
        every { prefs.blockingMode } returns flowOf(BlockingMode.UNKNOWN_CALLERS)
        every { prefs.batteryDismissed } returns flowOf(false)
        every { blockedCallDao.getBlockedCountSince(any()) } returns flowOf(0)
        every { blockedCallDao.getTotalBlockedCount() } returns flowOf(0)
        every { scheduleDao.getEnabled() } returns flowOf(emptyList())
    }

    private fun viewModel() = MainViewModel(prefs, blockedCallDao, scheduleDao, scheduleChecker)

    @Test
    fun `toggle flips active state via prefs`() = runTest {
        val vm = viewModel()

        vm.toggle() // isActive.value is the initial false (no active collector)
        advanceUntilIdle()

        coVerify { prefs.setActive(true) }
    }

    @Test
    fun `setBlockingMode persists the mode`() = runTest {
        val vm = viewModel()

        vm.setBlockingMode(BlockingMode.ALL_CALLERS)
        advanceUntilIdle()

        coVerify { prefs.setBlockingMode(BlockingMode.ALL_CALLERS) }
    }

    @Test
    fun `dismissBattery persists the flag`() = runTest {
        val vm = viewModel()

        vm.dismissBattery()
        advanceUntilIdle()

        coVerify { prefs.setBatteryDismissed(true) }
    }

    @Test
    fun `setHasScreeningRole updates exposed state`() = runTest {
        val vm = viewModel()

        assertThat(vm.hasScreeningRole.value).isFalse()
        vm.setHasScreeningRole(true)
        assertThat(vm.hasScreeningRole.value).isTrue()
    }

    @Test
    fun `isWithinSchedule is true when no schedules are enabled`() = runTest {
        viewModel().isWithinSchedule.test {
            // Initial value, then the combine emits: empty schedules => blocking active 24/7.
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
