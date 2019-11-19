package org.dhis2.usescases.programEventDetail

import com.mapbox.mapboxsdk.geometry.LatLng
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import org.dhis2.data.schedulers.SchedulerProvider
import org.dhis2.data.tuples.Pair
import org.dhis2.utils.filters.FilterManager
import org.hisp.dhis.android.core.common.Unit
import timber.log.Timber

class ProgramEventDetailPresenter(
    private var view: ProgramEventDetailView,
    private val eventRepository: ProgramEventDetailRepository,
    private val schedulerProvider: SchedulerProvider,
    private val filterManager: FilterManager
) {

    var compositeDisposable = CompositeDisposable()

    // Search fields
    private val eventInfoProcessor: FlowableProcessor<Pair<String, LatLng>> =
        PublishProcessor.create()
    private val mapProcessor: FlowableProcessor<Unit> =
        PublishProcessor.create()

    fun init() {
        compositeDisposable.add(
            eventRepository.featureType()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    { view.setFeatureType() },
                    Timber::e
                )
        )

        compositeDisposable.add(
            Observable.just(eventRepository.accessDataWrite)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    view::setWritePermission,
                    Timber::e
                )
        )

        compositeDisposable.add(
            eventRepository.hasAccessToAllCatOptions()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    view::setOptionComboAccess,
                    Timber::e
                )
        )

        compositeDisposable.add(
            eventRepository.program()
                .observeOn(schedulerProvider.ui())
                .subscribeOn(schedulerProvider.computation())
                .subscribe(
                    view::setProgram,
                    Timber::e
                )
        )

        compositeDisposable.add(
            eventRepository.catOptionCombos()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    view::setCatOptionComboFilter,
                    Timber::e
                )
        )

        compositeDisposable.add(
            filterManager.asFlowable()
                .startWith(FilterManager.getInstance())
                .map { filterManager ->
                    eventRepository.filteredProgramEvents(
                        filterManager.periodFilters,
                        filterManager.orgUnitUidsFilters,
                        filterManager.catOptComboFilters,
                        filterManager.eventStatusFilters,
                        filterManager.stateFilters
                    )
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    view::setLiveData,
                    Timber::e
                )
        )

        compositeDisposable.add(
            mapProcessor
                .flatMap {
                    filterManager.asFlowable()
                        .startWith(FilterManager.getInstance())
                        .flatMap { filterManager ->
                            eventRepository.filteredEventsForMap(
                                filterManager.periodFilters,
                                filterManager.orgUnitUidsFilters,
                                filterManager.catOptComboFilters,
                                filterManager.eventStatusFilters,
                                filterManager.stateFilters
                            )
                        }
                }
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    view.setMap(),
                    Consumer { Timber.e(it) }
                )
        )

        compositeDisposable.add(
            eventInfoProcessor
                .flatMap { eventInfo ->
                    eventRepository.getInfoForEvent(eventInfo.val0())
                        .map { eventData -> Pair.create(eventData, eventInfo.val1()) }
                }
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    view::setEventInfo,
                    Timber::e
                )
        )

        compositeDisposable.add(
            filterManager.ouTreeFlowable()
                .doOnNext {
                    if (view.isMapVisible) {
                        mapProcessor.onNext(Unit())
                    }
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    { view.openOrgUnitTreeSelector() },
                    Timber::e
                )
        )

        compositeDisposable.add(
            filterManager.asFlowable()
                .doOnNext {
                    if (view.isMapVisible) {
                        mapProcessor.onNext(Unit())
                    }
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    { filterManager -> view.updateFilters(filterManager.totalFilters) },
                    Timber::e
                )
        )

        compositeDisposable.add(
            filterManager.periodRequest
                .doOnNext {
                    if (view.isMapVisible) {
                        mapProcessor.onNext(Unit())
                    }
                }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                    view::showPeriodRequest,
                    Timber::e
                )
        )
    }

    fun onSyncIconClick(uid: String) {
        view.showSyncDialog(uid)
    }

    fun getEventInfo(eventUid: String, latLng: LatLng) {
        eventInfoProcessor.onNext(Pair.create(eventUid, latLng))
    }

    fun getMapData() {
        mapProcessor.onNext(Unit())
    }

    fun onEventClick(eventId: String, orgUnit: String) {
        view.navigateToEvent(eventId, orgUnit)
    }

    fun addEvent() {
        view.startNewEvent()
    }

    fun onBackClick() {
        view.back()
    }

    fun onDettach() {
        compositeDisposable.clear()
    }

    fun displayMessage(message: String) {
        view.displayMessage(message)
    }

    fun showFilter() {
        view.showHideFilter()
    }

    fun clearFilterClick() {
        FilterManager.getInstance().clearAllFilters()
        view.clearFilters()
    }
}
