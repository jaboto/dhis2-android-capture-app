package org.dhis2.usescases.programEventDetail

import com.mapbox.mapboxsdk.geometry.LatLng
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import org.dhis2.data.schedulers.TrampolineSchedulerProvider
import org.dhis2.data.tuples.Pair
import org.dhis2.utils.filters.FilterManager
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.category.CategoryOptionCombo
import org.hisp.dhis.android.core.common.FeatureType
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.common.Unit
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.EventStatus
import org.hisp.dhis.android.core.program.Program
import org.hisp.dhis.rules.models.RuleEvent
import org.junit.Before
import org.junit.Test
import java.util.Date

class ProgramEventDetailPresenterTest {

    private lateinit var presenter: ProgramEventDetailPresenter

    private val view: ProgramEventDetailView = mock()
    private val repository: ProgramEventDetailRepository = mock()
    private val scheduler = TrampolineSchedulerProvider()
    private val filterManager: FilterManager = mock()
    private var eventInfoProcessor: FlowableProcessor<Pair<String, LatLng>> = mock()
    private var mapProcessor: FlowableProcessor<Unit> = mock()

    @Before
    fun setUp() {
        presenter = ProgramEventDetailPresenter(view, repository, scheduler, filterManager)
    }

    @Test
    fun `Should init screen`() {

        val filterProcessor: FlowableProcessor<FilterManager> = PublishProcessor.create()
        val periodRequest: FlowableProcessor<FilterManager.PeriodRequest> =
            BehaviorProcessor.create()
        val eventInfoProcessor: FlowableProcessor<Pair<String, LatLng>> =
            PublishProcessor.create()
        val mapProcessor: FlowableProcessor<Unit> =
            PublishProcessor.create()
        val filterManagerFlowable = Flowable.just(filterManager).startWith(filterProcessor)
        val program = Program.builder().uid("programUid").build()
        val catOptionComboPair = Pair.create(dummyCategoryCombo(), dummyListCatOptionCombo())

        val programEventViewModel = ProgramEventViewModel.create(
            "uid",
            "orgUnitUid",
            "orgUnit",
            Date(),
            State.TO_UPDATE,
            mutableListOf(),
            EventStatus.ACTIVE,
            true,
            "attr"
        );

        whenever(repository.featureType()) doReturn Single.just(FeatureType.POINT)
        whenever(repository.accessDataWrite) doReturn true
        whenever(repository.hasAccessToAllCatOptions()) doReturn Single.just(true)
        whenever(repository.program()) doReturn Observable.just(program)
        whenever(repository.catOptionCombos()) doReturn Single.just(catOptionComboPair)
        whenever(
            repository.getInfoForEvent(dummyEvent().uid())
        ) doReturn Flowable.just(programEventViewModel)
        whenever(filterManager.asFlowable()) doReturn filterManagerFlowable
        whenever(filterManager.ouTreeFlowable()) doReturn Flowable.just(true)
        whenever(filterManager.periodRequest) doReturn periodRequest
        filterProcessor.onNext(filterManager)
        periodRequest.onNext(FilterManager.PeriodRequest.FROM_TO)
        eventInfoProcessor.onNext(Pair.create("eventUid", LatLng()))
        mapProcessor.onNext(Unit())

        presenter.init()

        verify(view).setFeatureType()
        verify(view).setWritePermission(true)
        verify(view).setOptionComboAccess(true)
        verify(view).setProgram(program)
        verify(view).setCatOptionComboFilter(catOptionComboPair)
        verify(view).setEventInfo(Pair.create(programEventViewModel, LatLng()))
    }

    @Test
    fun `Should show sync dialog`() {
        presenter.onSyncIconClick("uid")

        verify(view).showSyncDialog("uid")
    }

    @Test
    fun `Should get event info`() {
        presenter.getEventInfo("uid", LatLng())

        verify(eventInfoProcessor).onNext(Pair.create("uid", LatLng()))
    }

    @Test
    fun `Should get map data`() {
        presenter.getMapData()

        verify(mapProcessor).onNext(Unit())
    }

    @Test
    fun `Should navigate to event`() {
        presenter.onEventClick("eventId","orgUnit")

        verify(view).navigateToEvent("eventId","orgUnit")
    }
    @Test
    fun `Should start new event`() {
        presenter.addEvent()

        verify(view).startNewEvent()
    }
    @Test
    fun `Should go back when back button is pressed`() {
        presenter.onBackClick()

        verify(view).back()
    }

    @Test
    fun `Should dispose of all disposables`() {
        presenter.onDettach()

        val result = presenter.compositeDisposable.size()

        assert(result == 0)
    }

    @Test
    fun `Should display message`() {
        val message = "message"

        presenter.displayMessage(message)

        verify(view).displayMessage(message)
    }

    @Test
    fun `Should show or hide filter`() {
        presenter.showFilter()

        verify(view).showHideFilter()
    }

    @Test
    fun `Should clear all filters when reset filter button is clicked`() {
        presenter.clearFilterClick()

        verify(filterManager).clearAllFilters()
        verify(view).clearFilters()
    }



        private fun dummyEvent() = Event.builder().uid("uid").build()

    private fun dummyCategoryCombo() = CategoryCombo.builder().uid("uid").build()

    private fun dummyListCatOptionCombo(): List<CategoryOptionCombo> =
        listOf(CategoryOptionCombo.builder().uid("uid").build())
}
