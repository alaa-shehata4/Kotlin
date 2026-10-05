package com.example.carebrief

import com.example.carebrief.data.CareBriefRepository
import com.example.carebrief.data.CarePlanStore
import com.example.carebrief.data.DemoCareBriefRepository
import com.example.carebrief.data.TaskStore
import com.example.carebrief.data.ai.RemoteAiCareAssistant
import com.example.carebrief.core.model.CareRecipient
import com.example.carebrief.core.model.DailyNote
import com.example.carebrief.core.model.PlanStatus
import com.example.carebrief.core.model.StructuredObservations
import com.example.carebrief.presentation.careplan.CarePlanScreenState
import com.example.carebrief.presentation.careplan.CarePlanViewModel
import com.example.carebrief.presentation.dashboard.DashboardScreenState
import com.example.carebrief.presentation.dashboard.DashboardViewModel
import com.example.carebrief.presentation.editor.NoteEditorScreenState
import com.example.carebrief.presentation.editor.NoteEditorViewModel
import com.example.carebrief.presentation.notes.NotesScreenState
import com.example.carebrief.presentation.notes.NotesViewModel
import com.example.carebrief.presentation.profile.ProfileScreenState
import com.example.carebrief.presentation.profile.RecipientProfileViewModel
import com.example.carebrief.presentation.recipients.RecipientsScreenState
import com.example.carebrief.presentation.recipients.RecipientsViewModel
import com.example.carebrief.presentation.tasks.TasksScreenState
import com.example.carebrief.presentation.tasks.TasksViewModel
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

/**
 * Phase 28/29 — every major screen exposes an explicit Loading / Content /
 * Empty / Error state via StateFlow. Failing dependencies must surface Error,
 * never a crash or an eternal spinner.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class Phase28StateTest {
    class FailingRepo : CareBriefRepository {
        private fun <T> fail(): Flow<T> = flow { throw IOException("db down") }
        override fun observeRecipients(): Flow<List<CareRecipient>> = fail()
        override fun searchRecipients(query: String): Flow<List<CareRecipient>> = fail()
        override fun observeRecipient(recipientId: String): Flow<CareRecipient?> = fail()
        override fun observeNotes(recipientId: String): Flow<List<DailyNote>> = fail()
        override fun observeAllNotes(): Flow<List<DailyNote>> = fail()
        override suspend fun addNote(
            recipientId: String,
            content: String,
            categories: List<String>,
            author: String,
            structuredObservations: StructuredObservations?
        ): DailyNote = throw IOException("db down")
        override suspend fun setPlanStatus(recipientId: String, status: PlanStatus) =
            throw IOException("db down")
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
    fun dashboard_errorOnDbFailure() = runTest {
        val vm = DashboardViewModel(FailingRepo())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is DashboardScreenState.Error)
    }

    @Test
    fun dashboard_contentOnDemoData() = runTest {
        val vm = DashboardViewModel(DemoCareBriefRepository())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is DashboardScreenState.Content)
    }

    @Test
    fun recipients_errorOnDbFailure() = runTest {
        val vm = RecipientsViewModel(FailingRepo())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is RecipientsScreenState.Error)
    }

    @Test
    fun recipients_emptyOnUnknownQuery() = runTest {
        val vm = RecipientsViewModel(DemoCareBriefRepository())
        backgroundScope.launch { vm.screenState.collect {} }
        vm.onQueryChange("zzz-no-such-person")
        advanceUntilIdle()
        assertTrue(vm.screenState.value is RecipientsScreenState.Empty)
    }

    @Test
    fun recipients_contentOnBlankQuery() = runTest {
        val vm = RecipientsViewModel(DemoCareBriefRepository())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is RecipientsScreenState.Content)
    }

    @Test
    fun notes_errorOnDbFailure() = runTest {
        val vm = NotesViewModel("sarah", FailingRepo())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is NotesScreenState.Error)
    }

    @Test
    fun notes_contentOnDemoData() = runTest {
        val vm = NotesViewModel("sarah", DemoCareBriefRepository())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        val s = vm.screenState.value
        assertTrue(s is NotesScreenState.Content && s.groups.isNotEmpty())
    }

    @Test
    fun notes_filteredEmptyOnUnknownCategory() = runTest {
        val vm = NotesViewModel("sarah", DemoCareBriefRepository())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        vm.selectCategory("Vision")
        advanceUntilIdle()
        assertTrue(vm.screenState.value is NotesScreenState.FilteredEmpty)
    }

    @Test
    fun carePlan_errorOnRemoteAi() = runTest {
        val vm = CarePlanViewModel("sarah", DemoCareBriefRepository(), CarePlanStore(RemoteAiCareAssistant()))
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is CarePlanScreenState.Error)
    }

    @Test
    fun carePlan_draftOnDemoData() = runTest {
        val vm = CarePlanViewModel("sarah", DemoCareBriefRepository(), CarePlanStore(), TaskStore())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is CarePlanScreenState.Draft)
    }

    @Test
    fun tasks_emptyBeforeGeneration() = runTest {
        val vm = TasksViewModel("sarah", DemoCareBriefRepository(), CarePlanStore(), TaskStore())
        backgroundScope.launch { vm.screenState.collect {} }
        backgroundScope.launch { vm.draft.collect {} }
        advanceUntilIdle()
        // Fresh store: nothing generated yet -> explicit Empty (not a spinner).
        assertTrue(vm.screenState.value is TasksScreenState.Empty)
    }

    @Test
    fun tasks_contentAfterGeneration() = runTest {
        val tasks = TaskStore()
        val vm = TasksViewModel("sarah", DemoCareBriefRepository(), CarePlanStore(), tasks)
        backgroundScope.launch { vm.screenState.collect {} }
        backgroundScope.launch { vm.draft.collect {} }
        advanceUntilIdle()
        vm.ensureTasks()
        advanceUntilIdle()
        val s = vm.screenState.value
        assertTrue(s is TasksScreenState.Content && s.tasks.isNotEmpty())
    }

    @Test
    fun profile_contentOnDemoData() = runTest {
        val vm = RecipientProfileViewModel("sarah", DemoCareBriefRepository())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        val s = vm.screenState.value
        assertTrue(s is ProfileScreenState.Content && s.data.recipient?.id == "sarah")
    }

    @Test
    fun profile_errorOnDbFailure() = runTest {
        val vm = RecipientProfileViewModel("sarah", FailingRepo())
        backgroundScope.launch { vm.screenState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.screenState.value is ProfileScreenState.Error)
    }

    @Test
    fun editor_errorOnBlankContent() = runTest {
        val vm = NoteEditorViewModel(DemoCareBriefRepository())
        vm.save("sarah", "   ", emptyList(), "Nurse", null)
        advanceUntilIdle()
        assertTrue(vm.screenState.value is NoteEditorScreenState.Error)
    }

    @Test
    fun editor_savedOnValidContent() = runTest {
        val vm = NoteEditorViewModel(DemoCareBriefRepository())
        vm.save("sarah", "Sarah finished most of lunch and seemed brighter.", listOf("Nutrition"), "Nurse", null)
        advanceUntilIdle()
        assertTrue(vm.screenState.value is NoteEditorScreenState.Saved)
    }
}
