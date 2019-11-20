package org.dhis2.usescases.programEventDetail

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
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.program.Program
import org.junit.Before
import org.junit.Test

class ProgramEventDetailPresenterTest {

    private lateinit var presenter: ProgramEventDetailPresenter

    private val view: ProgramEventDetailView = mock()
    private val repository: ProgramEventDetailRepository = mock()
    private val scheduler = TrampolineSchedulerProvider()
    private val filterManager: FilterManager = mock()

    @Before
    fun setUp() {
        presenter = ProgramEventDetailPresenter(view, repository, scheduler, filterManager)
    }

    @Test
    fun `Should init screen`() {

        val filterProcessor: FlowableProcessor<FilterManager> = PublishProcessor.create()
        val periodRequest: FlowableProcessor<FilterManager.PeriodRequest> =
            BehaviorProcessor.create()
        val filterManagerFlowable = Flowable.just(filterManager).startWith(filterProcessor)
        val program = Program.builder().uid("programUid").build()
        val catOptionComboPair = Pair.create(dummyCategoryCombo(), dummyListCatOptionCombo())


        whenever(repository.featureType()) doReturn Single.just(FeatureType.POINT)
        whenever(repository.accessDataWrite) doReturn true
        whenever(repository.hasAccessToAllCatOptions()) doReturn Single.just(true)
        whenever(repository.program()) doReturn Observable.just(program)
        whenever(repository.catOptionCombos()) doReturn Single.just(catOptionComboPair)
        whenever(filterManager.asFlowable()) doReturn filterManagerFlowable
        whenever(filterManager.ouTreeFlowable()) doReturn Flowable.just(true)
        whenever(filterManager.periodRequest) doReturn periodRequest
        filterProcessor.onNext(filterManager)
        periodRequest.onNext(FilterManager.PeriodRequest.FROM_TO)

        presenter.init()

        verify(view).setFeatureType()
        verify(view).setWritePermission(true)
        verify(view).setOptionComboAccess(true)
        verify(view).setProgram(program)
        verify(view).setCatOptionComboFilter(catOptionComboPair)



    }
    private fun dummyEvent() = Event.builder().uid("uid").build()

    private fun dummyCategoryCombo() = CategoryCombo.builder().uid("uid").build()

    private fun dummyListCatOptionCombo(): List<CategoryOptionCombo> =
        listOf(CategoryOptionCombo.builder().uid("uid").build())
    }
}
