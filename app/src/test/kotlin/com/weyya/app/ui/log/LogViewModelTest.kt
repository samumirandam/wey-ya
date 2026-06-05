package com.weyya.app.ui.log

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.WhitelistDao
import com.weyya.app.data.db.entity.BlockedCallEntity
import com.weyya.app.ui.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val blockedCallDao = mockk<BlockedCallDao>(relaxed = true)
    private val whitelistDao = mockk<WhitelistDao>(relaxed = true)

    private fun viewModel() = LogViewModel(blockedCallDao, whitelistDao)

    @Test
    fun `allCalls emits rows from dao`() = runTest {
        val rows = listOf(
            BlockedCallEntity(id = 1, phoneNumber = "5551234", timestamp = 10, attemptCount = 1),
        )
        every { blockedCallDao.getAll() } returns flowOf(rows)

        viewModel().allCalls.test {
            assertThat(awaitItem()).isEmpty() // stateIn initial value
            assertThat(awaitItem()).isEqualTo(rows)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setFilter updates filter state`() = runTest {
        every { blockedCallDao.getAll() } returns flowOf(emptyList())
        val vm = viewModel()

        assertThat(vm.filter.value).isEqualTo(LogFilter.ALL)
        vm.setFilter(LogFilter.WEEK)
        assertThat(vm.filter.value).isEqualTo(LogFilter.WEEK)
    }

    @Test
    fun `addToWhitelist inserts entity with the given number`() = runTest {
        every { blockedCallDao.getAll() } returns flowOf(emptyList())
        val vm = viewModel()

        vm.addToWhitelist("5551234")
        advanceUntilIdle()

        // addedAt defaults to now, so match on phoneNumber rather than structural equality.
        coVerify { whitelistDao.insert(match { it.phoneNumber == "5551234" }) }
    }

    @Test
    fun `clearHistory deletes all rows`() = runTest {
        every { blockedCallDao.getAll() } returns flowOf(emptyList())
        val vm = viewModel()

        vm.clearHistory()
        advanceUntilIdle()

        coVerify { blockedCallDao.deleteAll() }
    }
}
