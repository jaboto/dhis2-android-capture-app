package org.dhis2.usecases.teiDashboard.dashboardfragments.notes

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import org.dhis2.data.schedulers.SchedulerProvider
import org.dhis2.data.schedulers.TrampolineSchedulerProvider
import org.dhis2.data.tuples.Pair
import org.dhis2.usescases.teiDashboard.DashboardRepositoryImpl
import org.dhis2.usescases.teiDashboard.dashboardfragments.notes.NotesPresenter
import org.dhis2.usescases.teiDashboard.dashboardfragments.notes.NotesView
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.common.Access
import org.hisp.dhis.android.core.common.DataAccess
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.note.NoteCreateProjection
import org.hisp.dhis.android.core.program.Program
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotesPresenterTest {
    private lateinit var notesPresenter: NotesPresenter
    private val dashboardRepository: DashboardRepositoryImpl = mock()
    private val schedulers: SchedulerProvider = TrampolineSchedulerProvider()
    private val view: NotesView = mock()
    private val d2: D2 = mock()

    @Before
    fun setUp() {
        notesPresenter = NotesPresenter(
            d2, dashboardRepository, schedulers,
            view, "program_uid", "tei_uid"
        )
    }

    @Test
    fun `Should display message`() {
        val message = "message"

        notesPresenter.displayMessage(message)

        verify(view).displayMessage(message)
    }
    @Test
    fun `Should return true if program has write permission`() {
        programModuleMock()
        whenever(
            d2.programModule().programs().uid("program_uid")
                .blockingGet()
        ) doReturn getProgramDefaultAccessTrue()

        assertTrue(notesPresenter.hasProgramWritePermission())
    }

    @Test
    fun `Should return false if program has not write permission`() {
        programModuleMock()
        whenever(
            d2.programModule().programs().uid("program_uid")
                .blockingGet()
        ) doReturn getProgramDefaultAccessFalse()

        assertFalse(notesPresenter.hasProgramWritePermission())
    }

    @Test
    fun `Should return true if program has null write permission`() {
        programModuleMock()
        whenever(
            d2.programModule().programs().uid("program_uid")
                .blockingGet()
        ) doReturn getProgramDefaultAccessNull()

        assertTrue(notesPresenter.hasProgramWritePermission())
    }

    @Test
    fun `Should clear disposables`() {
        notesPresenter.onDettach()

        assertTrue(notesPresenter.compositeDisposable.size() == 0)
    }

    @Test
    fun `Should set note processor`() {
        val dummyPair = Pair.create("test", true)
        val noteProcessor: FlowableProcessor<Pair<String, Boolean>> = BehaviorProcessor.create()
        noteProcessor.onNext(dummyPair)

        notesPresenter.setNoteProcessor(noteProcessor)

        verify(dashboardRepository).handleNote(dummyPair)
    }

    @Test
    fun `Should subscribeToNotes`() {
        val notes = listOf<Note>(Note.builder().uid("note_uid").build())
        val noteProcessor: FlowableProcessor<Boolean> = BehaviorProcessor.create()
        noteProcessor.onNext(true)

        mockEnrollmentByProgramTeiStatus()

        whenever(d2.noteModule()) doReturn mock()
        whenever(d2.noteModule().notes()) doReturn mock()
        whenever(d2.noteModule().notes().byEnrollmentUid()) doReturn mock()
        whenever(d2.noteModule().notes().byEnrollmentUid().eq("enroll_uid")) doReturn mock()
        whenever(
            d2.noteModule().notes().byEnrollmentUid()
                .eq("enroll_uid").get()
        ) doReturn Single.just(notes)

        notesPresenter.subscribeToNotes()

        verify(view).swapNotes(notes)
    }

    @Test
    fun `Should save a note`() {
        val testingNoteUid = "note_uid"

        mockEnrollmentByProgramTeiStatus()

        whenever(d2.noteModule()) doReturn mock()
        whenever(d2.noteModule().notes()) doReturn mock()
        whenever(
            d2.noteModule().notes()
                .blockingAdd(noteCreationProject())
        ) doReturn testingNoteUid

        val testSubscriber = notesPresenter.processor.test()

        notesPresenter.saveNote("message")

        testSubscriber.assertValueCount(1)
        testSubscriber.assertValue(true)
    }

    private fun getProgramDefaultAccessTrue(): Program {
        return Program.builder()
            .uid("program_uid")
            .access(
                Access.create(
                    false, false,
                    DataAccess.create(false, true)
                )
            )
            .build()
    }

    private fun getProgramDefaultAccessFalse(): Program {
        return Program.builder()
            .uid("program_uid")
            .access(
                Access.create(
                    false, false,
                    DataAccess.create(false, false)
                )
            )
            .build()
    }

    private fun getProgramDefaultAccessNull(): Program {
        return Program.builder()
            .uid("program_uid")
            .access(
                Access.create(
                    false, false,
                    DataAccess.create(false, null)
                )
            )
            .build()
    }

    private fun noteCreationProject(): NoteCreateProjection {
        return NoteCreateProjection.builder()
            .enrollment("enroll_uid")
            .value("message")
            .build()
    }

    private fun programModuleMock() {
        whenever(d2.programModule()) doReturn mock()
        whenever(d2.programModule().programs()) doReturn mock()
        whenever(d2.programModule().programs().uid("program_uid")) doReturn mock()
    }

    private fun mockEnrollmentByProgramTeiStatus() {
        whenever(d2.enrollmentModule()) doReturn mock()
        whenever(d2.enrollmentModule().enrollments()) doReturn mock()
        whenever(d2.enrollmentModule().enrollments().byProgram()) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram()
                .eq("program_uid")
        ) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram().eq("program_uid")
                .byTrackedEntityInstance()
        ) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram().eq("program_uid")
                .byTrackedEntityInstance().eq("tei_uid")
        ) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram().eq("program_uid")
                .byTrackedEntityInstance().eq("tei_uid").byStatus()
        ) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram().eq("program_uid")
                .byTrackedEntityInstance().eq("tei_uid").byStatus()
                .eq(EnrollmentStatus.ACTIVE)
        ) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram().eq("program_uid")
                .byTrackedEntityInstance().eq("tei_uid").byStatus()
                .eq(EnrollmentStatus.ACTIVE).one()
        ) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram().eq("program_uid")
                .byTrackedEntityInstance().eq("tei_uid").byStatus()
                .eq(EnrollmentStatus.ACTIVE).one().blockingGet()
        ) doReturn mock()
        whenever(
            d2.enrollmentModule().enrollments().byProgram().eq("program_uid")
                .byTrackedEntityInstance().eq("tei_uid").byStatus()
                .eq(EnrollmentStatus.ACTIVE).one().blockingGet().uid()
        ) doReturn "enroll_uid"
    }
}
