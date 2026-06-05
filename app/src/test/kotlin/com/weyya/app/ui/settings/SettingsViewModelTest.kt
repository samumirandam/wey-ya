package com.weyya.app.ui.settings

import com.google.common.truth.Truth.assertThat
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.db.dao.WhitelistDao
import com.weyya.app.data.db.entity.ScheduleEntity
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.data.telephony.SimInfo
import com.weyya.app.data.telephony.SimResolver
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
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prefs = mockk<UserPreferences>(relaxed = true)
    private val scheduleDao = mockk<ScheduleDao>(relaxed = true)
    private val whitelistDao = mockk<WhitelistDao>(relaxed = true)
    private val simResolver = mockk<SimResolver>(relaxed = true)

    @Before
    fun setUp() {
        every { prefs.attemptThreshold } returns flowOf(3)
        every { prefs.timeWindowMinutes } returns flowOf(5)
        every { scheduleDao.getAll() } returns flowOf(emptyList())
        every { whitelistDao.getAll() } returns flowOf(emptyList())
    }

    private fun viewModel() = SettingsViewModel(prefs, scheduleDao, whitelistDao, simResolver)

    @Test
    fun `refreshSims publishes resolver result`() = runTest {
        val sims = listOf(mockk<SimInfo>())
        every { simResolver.getActiveSims() } returns sims
        val vm = viewModel()

        vm.refreshSims()

        assertThat(vm.activeSims.value).isEqualTo(sims)
    }

    @Test
    fun `addSchedule delegates to dao`() = runTest {
        val schedule = ScheduleEntity(daysOfWeek = "1,2,3", startTime = "22:00", endTime = "07:00")
        val vm = viewModel()

        vm.addSchedule(schedule)
        advanceUntilIdle()

        coVerify { scheduleDao.insert(schedule) }
    }

    @Test
    fun `toggleSchedule flips enabled and updates`() = runTest {
        val schedule = ScheduleEntity(id = 7, daysOfWeek = "1", startTime = "08:00", endTime = "09:00", enabled = true)
        val vm = viewModel()

        vm.toggleSchedule(schedule)
        advanceUntilIdle()

        coVerify { scheduleDao.update(schedule.copy(enabled = false)) }
    }

    @Test
    fun `deleteSchedule delegates to dao`() = runTest {
        val schedule = ScheduleEntity(id = 3, daysOfWeek = "5", startTime = "10:00", endTime = "11:00")
        val vm = viewModel()

        vm.deleteSchedule(schedule)
        advanceUntilIdle()

        coVerify { scheduleDao.delete(schedule) }
    }

    @Test
    fun `addToWhitelist inserts the given number`() = runTest {
        val vm = viewModel()

        vm.addToWhitelist("5559999", label = "Mom")
        advanceUntilIdle()

        coVerify { whitelistDao.insert(match { it.phoneNumber == "5559999" && it.label == "Mom" }) }
    }

    @Test
    fun `removeFromWhitelist delegates to dao`() = runTest {
        val vm = viewModel()

        vm.removeFromWhitelist("5559999")
        advanceUntilIdle()

        coVerify { whitelistDao.deleteByNumber("5559999") }
    }

    @Test
    fun `setThreshold persists value`() = runTest {
        val vm = viewModel()

        vm.setThreshold(4)
        advanceUntilIdle()

        coVerify { prefs.setAttemptThreshold(4) }
    }

    @Test
    fun `setWindowMinutes persists value`() = runTest {
        val vm = viewModel()

        vm.setWindowMinutes(15)
        advanceUntilIdle()

        coVerify { prefs.setTimeWindowMinutes(15) }
    }
}
