package com.example.carebrief

import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.ai.RemoteAiCareAssistant
import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.presentation.dashboard.DashboardViewModel
import com.example.carebrief.presentation.careplan.CarePlanViewModel
import com.example.carebrief.presentation.notes.NotesViewModel
import com.example.carebrief.presentation.recipients.RecipientsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Phase 21: load/AI failures must surface as retryable UI state, never a crash. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ErrorHandlingTest {
    class FailingRepo : CareBriefRepository {
        private fun <T> fail(): Flow<T> = flow { throw IOException("db down") }
        override fun observeRecipients(): Flow<List<CareRecipient>> = fail()
        override fun searchRecipients(query: String): Flow<List<CareRecipient>> = fail()
        override fun observeRecipient(recipientId: String): Flow<com.example.carebrief.core.model.CareRecipient?> = fail()
        override fun observeNotes(recipientId: String): Flow<List<DailyNote>> = fail()
        override fun observeAllNotes(): Flow<List<DailyNote>> = fail()
        override suspend fun addNote(recipientId: String, content: String, categories: List<String>, author: String, structuredObservations: com.example.carebrief.core.model.StructuredObservations?): DailyNote =
            throw IOException("db down")
        override suspend fun setPlanStatus(recipientId: String, status: PlanStatus) = throw IOException("db down")
        override suspend fun resetDemoData() = throw IOException("db down")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun recipients_dbFailureSurfacesError() = runTest {
        val vm = RecipientsViewModel(FailingRepo())
        backgroundScope.launch { vm.uiState.collect {} }
        backgroundScope.launch { vm.error.collect {} }
        advanceUntilIdle()
        assertTrue(vm.error.value)
    }

    @Test
    fun notes_dbFailureSurfacesError() = runTest {
        val vm = NotesViewModel("sarah", FailingRepo())
        backgroundScope.launch { vm.groups.collect {} }
        backgroundScope.launch { vm.loadError.collect {} }
        advanceUntilIdle()
        assertTrue(vm.loadError.value)
    }

    @Test
    fun dashboard_dbFailureSurfacesError() = runTest {
        val vm = DashboardViewModel(FailingRepo())
        backgroundScope.launch { vm.uiState.collect {} }
        backgroundScope.launch { vm.loadError.collect {} }
        advanceUntilIdle()
        assertTrue(vm.loadError.value)
    }

    @Test
    fun plan_remoteAiSurfacesError() = runTest {
        val vm = CarePlanViewModel("sarah", DemoCareBriefRepository(), CarePlanStore(RemoteAiCareAssistant()))
        backgroundScope.launch { vm.plan.collect {} }
        backgroundScope.launch { vm.failed.collect {} }
        advanceUntilIdle()
        assertTrue(vm.failed.value)
        vm.retry()
        advanceUntilIdle()
        assertTrue(vm.failed.value)
    }
}
