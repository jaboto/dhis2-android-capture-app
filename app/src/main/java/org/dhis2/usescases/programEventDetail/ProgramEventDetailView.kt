package org.dhis2.usescases.programEventDetail

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLng
import io.reactivex.functions.Consumer
import org.dhis2.data.tuples.Pair
import org.dhis2.usescases.general.AbstractActivityContracts
import org.dhis2.utils.filters.FilterManager
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.category.CategoryOptionCombo
import org.hisp.dhis.android.core.common.FeatureType
import org.hisp.dhis.android.core.program.Program

interface ProgramEventDetailView : AbstractActivityContracts.View {

    val isMapVisible: Boolean

    fun setProgram(programModel: Program)

    fun renderError(message: String)

    fun showHideFilter()

    fun setWritePermission(aBoolean: Boolean)

    fun setLiveData(pagedListLiveData: LiveData<PagedList<ProgramEventViewModel>>)

    fun setOptionComboAccess(canCreateEvent: Boolean)

    fun updateFilters(totalFilters: Int)

    fun setCatOptionComboFilter(
        categoryOptionCombos: Pair<CategoryCombo, List<CategoryOptionCombo>>
    )

    fun openOrgUnitTreeSelector()

    fun setMap(): Consumer<kotlin.Pair<FeatureCollection, BoundingBox>>

    fun setEventInfo(programEventViewModel: Pair<ProgramEventViewModel, LatLng>)

    fun showPeriodRequest(periodRequest: FilterManager.PeriodRequest)

    fun clearFilters()

    fun setFeatureType(): Consumer<FeatureType>

    fun startNewEvent()

    fun showSyncDialog(uid: String)

    fun navigateToEvent(eventId: String, orgUnit: String)
}
