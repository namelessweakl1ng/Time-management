package com.yourapp.timemanagement.core

import app.cash.turbine.test
import com.yourapp.timemanagement.MainDispatcherRule
import com.yourapp.timemanagement.data.repository.CalendarRepository
import com.yourapp.timemanagement.data.repository.SettingsRepository
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.domain.ProductivityCalculator
import com.yourapp.timemanagement.domain.FlowStateDetector
import com.yourapp.timemanagement.domain.GamificationEngine
import com.yourapp.timemanagement.domain.OnDeviceTaskTimePredictor
import com.yourapp.timemanagement.domain.SmartSuggestionEngine
import com.yourapp.timemanagement.domain.UserSettings
import com.yourapp.timemanagement.widgets.TimeWidgetUpdater
import com.yourapp.timemanagement.work.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TimeManagementViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    private val timeRepository = mockk<TimeRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val calendarRepository = mockk<CalendarRepository>(relaxed = true)
    private val widgetUpdater = mockk<TimeWidgetUpdater>(relaxed = true)

    @Test
    fun init_seedsDataAndPublishesLoadedState() = runTest {
        every { settingsRepository.settings } returns MutableStateFlow(UserSettings())
        every { timeRepository.tasksForDate(any()) } returns MutableStateFlow(emptyList())
        every { timeRepository.sessionsForDate(any()) } returns MutableStateFlow(emptyList())
        every { timeRepository.allTasks } returns MutableStateFlow(emptyList())
        every { timeRepository.categories } returns MutableStateFlow(emptyList())
        every { timeRepository.sessions } returns MutableStateFlow(emptyList())
        every { timeRepository.subTasks } returns MutableStateFlow(emptyList())
        every { timeRepository.tags } returns MutableStateFlow(emptyList())
        every { timeRepository.taskTagIds } returns MutableStateFlow(emptyMap())
        every { timeRepository.activeSession } returns MutableStateFlow(null)
        every { calendarRepository.hasReadPermission() } returns false
        coEvery { timeRepository.seedSampleDataIfEmpty() } just runs
        coEvery { settingsRepository.setSeededSampleData(true) } just runs
        coEvery { widgetUpdater.updateAll() } just runs

        val viewModel = TimeManagementViewModel(
            timeRepository = timeRepository,
            settingsRepository = settingsRepository,
            calendarRepository = calendarRepository,
            calculator = ProductivityCalculator(),
            suggestionEngine = SmartSuggestionEngine(),
            taskTimePredictor = OnDeviceTaskTimePredictor(),
            flowStateDetector = FlowStateDetector(),
            gamificationEngine = GamificationEngine(),
            reminderScheduler = mockk<ReminderScheduler>(relaxed = true),
            widgetUpdater = widgetUpdater,
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.operationState is Result.Success)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { timeRepository.seedSampleDataIfEmpty() }
        coVerify { settingsRepository.setSeededSampleData(true) }
        coVerify { widgetUpdater.updateAll() }
    }
}
