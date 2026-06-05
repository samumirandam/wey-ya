package com.weyya.app.ui.privacy

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.ui.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrivacyDashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prefs = mockk<UserPreferences>(relaxed = true)
    private val blockedCallDao = mockk<BlockedCallDao>(relaxed = true)

    @Before
    fun setUp() {
        every { blockedCallDao.getTotalBlockedCount() } returns flowOf(42)
        every { blockedCallDao.getBlockedCountSince(any()) } returns flowOf(7)
        every { blockedCallDao.getBypassCount() } returns flowOf(3)
        every { prefs.firstActivationDate } returns flowOf(null)
    }

    private fun viewModel() = PrivacyDashboardViewModel(prefs, blockedCallDao)

    @Test
    fun `counts surface dao values`() = runTest {
        val vm = viewModel()

        vm.totalBlocked.test {
            assertThat(awaitItem()).isEqualTo(0) // initial
            assertThat(awaitItem()).isEqualTo(42)
            cancelAndIgnoreRemainingEvents()
        }
        vm.blockedThisMonth.test {
            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(7)
            cancelAndIgnoreRemainingEvents()
        }
        vm.bypassCount.test {
            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(3)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `daysSinceFirstActivation is zero when never activated`() = runTest {
        val vm = viewModel()

        vm.daysSinceFirstActivation.test {
            // Mapped value (0 for a null date) equals the stateIn initial, so StateFlow
            // conflates them into a single emission.
            assertThat(awaitItem()).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
