package org.dhis2.usescases.eventsWithoutRegistration.eventInitial

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Flowable
import io.reactivex.Observable
import org.dhis2.data.forms.FormSectionViewModel
import org.dhis2.data.schedulers.SchedulerProvider
import org.dhis2.data.schedulers.TrampolineSchedulerProvider
import org.dhis2.usescases.eventsWithoutRegistration.eventSummary.EventSummaryRepository
import org.dhis2.utils.analytics.AnalyticsHelper
import org.dhis2.utils.analytics.BACK_EVENT
import org.dhis2.utils.analytics.CLICK
import org.dhis2.utils.analytics.CREATE_EVENT
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.category.CategoryOption
import org.hisp.dhis.android.core.category.CategoryOptionCombo
import org.hisp.dhis.android.core.common.ObjectStyle
import org.hisp.dhis.android.core.program.ProgramStage
import org.junit.Before
import org.junit.Test

class EventInitialPresenterTest {

    private lateinit var presenter: EventInitialPresenter

    private val view: EventInitialContract.View = mock()
    private val eventInitialRepository: EventInitialRepository = mock()
    private val eventSummaryRepository: EventSummaryRepository = mock()
    private val schedulers: SchedulerProvider = TrampolineSchedulerProvider()
    private val analyticsHelper: AnalyticsHelper = mock()

    @Before
    fun setup() {
        presenter = EventInitialPresenter(view, eventSummaryRepository, eventInitialRepository, schedulers, analyticsHelper)
    }

    @Test
    fun `Should init`() {

    }

    @Test
    fun `Should set category option combos`() {
        val categoryCombo = CategoryCombo.builder().uid("catCombo").build()
        val stringCategoryOptionMap = mapOf<String, CategoryOption>()
        val categoryOptionCombos = listOf(
            CategoryOptionCombo.builder().uid("catOptionCombo").build()
        )
        whenever(
            eventInitialRepository.catOptionCombos(categoryCombo.uid())
        ) doReturn Observable.just(categoryOptionCombos)

        presenter.getCatOptionCombos(categoryCombo, stringCategoryOptionMap)

        verify(view).setCatComboOptions(categoryCombo, categoryOptionCombos, stringCategoryOptionMap)
    }

    @Test
    fun `Should set event sections`() {
        val sections = listOf(FormSectionViewModel.createForSection(
                "eventUid", "sectionUid", "label", "renderType"
        ))
        whenever(
            eventSummaryRepository.programStageSections("eventUid")
        ) doReturn Flowable.just(sections)

        presenter.getEventSections("eventUid")

        verify(view).onEventSections(sections)
    }


    @Test
    fun `Should show showQR when share button is clicked`() {
        presenter.onShareClick()

        verify(view).showQR()
    }

    @Test
    fun `Should delete event when eventid is not null`() {
        presenter.eventId = "event"
        presenter.deleteEvent("tei")

        verify(eventInitialRepository).deleteEvent("event", "tei")
        verify(view).showEventWasDeleted()
    }

    @Test
    fun `Should not delete event when eventid is null`() {
        presenter.eventId = null
        presenter.deleteEvent("tei")

        verify(view).displayEventAlert()
    }

    @Test
    fun `Should set object style`() {
        val objectStyle = ObjectStyle.builder().build()
        whenever(
            eventInitialRepository.getObjectStyle("uid")
        ) doReturn Observable.just(objectStyle)

        presenter.getStageObjectStyle("uid")

        verify(view).renderObjectStyle(objectStyle)
    }

    @Test
    fun `Should set program stage`() {
        val programStage = ProgramStage.builder().uid("uid").build()
        whenever(
            eventInitialRepository.programStageWithId("uid")
        ) doReturn Observable.just(programStage)

        presenter.getProgramStage("uid")

        verify(view).setProgramStage(programStage)
    }

    @Test
    fun `Should go back and set analytics`() {
        presenter.eventId = "uid"
        presenter.onBackClick()

        verify(analyticsHelper).setEvent(BACK_EVENT, CLICK, CREATE_EVENT)
        verify(view).back()
    }

    @Test
    fun `Should go back and does not set analytics`() {
        presenter.eventId = null
        presenter.onBackClick()

        verifyZeroInteractions(analyticsHelper)
        verify(view).back()
    }
}