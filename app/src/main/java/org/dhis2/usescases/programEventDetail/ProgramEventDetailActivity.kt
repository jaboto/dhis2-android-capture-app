/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.dhis2.usescases.programEventDetail

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.SparseBooleanArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import io.reactivex.functions.Consumer
import javax.inject.Inject
import org.dhis2.App
import org.dhis2.R
import org.dhis2.R.layout.activity_program_event_detail
import org.dhis2.data.tuples.Pair
import org.dhis2.databinding.ActivityProgramEventDetailBinding
import org.dhis2.databinding.InfoWindowEventBinding
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.EventCaptureActivity
import org.dhis2.usescases.eventsWithoutRegistration.eventInitial.EventInitialActivity
import org.dhis2.usescases.general.ActivityGlobalAbstract
import org.dhis2.usescases.orgunitselector.OUTreeActivity
import org.dhis2.utils.ColorUtils
import org.dhis2.utils.Constants
import org.dhis2.utils.Constants.ORG_UNIT
import org.dhis2.utils.Constants.PROGRAM_UID
import org.dhis2.utils.DateUtils
import org.dhis2.utils.HelpManager
import org.dhis2.utils.analytics.CLICK
import org.dhis2.utils.analytics.CREATE_EVENT
import org.dhis2.utils.analytics.DATA_CREATION
import org.dhis2.utils.analytics.SHOW_HELP
import org.dhis2.utils.filters.FilterManager
import org.dhis2.utils.filters.FiltersAdapter
import org.dhis2.utils.granularsync.GranularSyncContracts
import org.dhis2.utils.granularsync.SyncStatusDialog
import org.dhis2.utils.maps.MapLayerManager.Companion.POINT_LAYER_ID
import org.dhis2.utils.maps.MapLayerManager.Companion.POLYGON_LAYER_ID
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.category.CategoryOptionCombo
import org.hisp.dhis.android.core.common.FeatureType
import org.hisp.dhis.android.core.program.Program
import timber.log.Timber

class ProgramEventDetailActivity :
    ActivityGlobalAbstract(),
    ProgramEventDetailView,
    MapboxMap.OnMapClickListener {

    @Inject
    lateinit var presenter: ProgramEventDetailPresenter
    @Inject
    lateinit var filterManager: FilterManager

    private lateinit var binding: ActivityProgramEventDetailBinding
    private lateinit var programUid: String
    private lateinit var filtersAdapter: FiltersAdapter
    private lateinit var liveAdapter: ProgramEventDetailLiveAdapter

    private var backDropActive: Boolean = false
    private var map: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var markerViewManager: MarkerViewManager? = null
    private var currentMarker: MarkerView? = null
    private var featureType: FeatureType? = null

    override val isMapVisible: Boolean
        get() = binding.mapView.visibility == View.VISIBLE

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        this.programUid = intent.getStringExtra(EXTRA_PROGRAM_UID)
        (applicationContext as App).userComponent()!!.plus(
            ProgramEventDetailModule(this, programUid)
        ).inject(this)
        super.onCreate(savedInstanceState)

        filterManager.clearCatOptCombo()
        filterManager.clearEventStatus()
        filtersAdapter = FiltersAdapter()
        filtersAdapter.addEventStatus()

        binding = DataBindingUtil.setContentView(this, activity_program_event_detail)
        liveAdapter = ProgramEventDetailLiveAdapter(presenter)

        binding.apply {
            presenter = this@ProgramEventDetailActivity.presenter
            totalFilters = filterManager.totalFilters
            filterLayout.adapter = filtersAdapter
            recycler.adapter = liveAdapter
            recycler.addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.init()
        binding.mapView.onResume()
        binding.addEventButton.isEnabled = true
        binding.totalFilters = filterManager.totalFilters
        filtersAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        presenter.onDettach()
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        symbolManager?.onDestroy()
        markerViewManager?.onDestroy()
        binding.mapView.onDestroy()

        filterManager.clearEventStatus()
        filterManager.clearCatOptCombo()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun setProgram(program: Program) {
        binding.name = program.displayName()
    }

    override fun setLiveData(pagedListLiveData: LiveData<PagedList<ProgramEventViewModel>>) {
        pagedListLiveData.observe(
            this,
            Observer { pagedList ->
                binding.programProgress.visibility = View.GONE
                liveAdapter.submitList(pagedList) {
                    if (binding.recycler.adapter?.itemCount == 0) {
                        binding.emptyTeis.visibility = View.VISIBLE
                        binding.recycler.visibility = View.GONE
                    } else {
                        binding.emptyTeis.visibility = View.GONE
                        binding.recycler.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    override fun setOptionComboAccess(canCreateEvent: Boolean) {
        binding.addEventButton.visibility = when {
            binding.addEventButton.visibility == View.VISIBLE && canCreateEvent -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun renderError(message: String) {
        AlertDialog.Builder(activity)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(getString(R.string.error))
            .setMessage(message)
            .show()
    }

    override fun showHideFilter() {
        val transition = ChangeBounds()
        transition.duration = 200
        TransitionManager.beginDelayedTransition(binding.backdropLayout, transition)
        backDropActive = !backDropActive
        val initSet = ConstraintSet()
        initSet.clone(binding.backdropLayout)
        binding.filterOpen.visibility = if (backDropActive) View.VISIBLE else View.GONE

        if (backDropActive) {
            initSet.connect(
                R.id.eventsLayout,
                ConstraintSet.TOP,
                R.id.filterLayout,
                ConstraintSet.BOTTOM,
                50
            )
        } else {
            initSet.connect(
                R.id.eventsLayout,
                ConstraintSet.TOP,
                R.id.backdropGuideTop,
                ConstraintSet.BOTTOM,
                0
            )
        }

        initSet.applyTo(binding.backdropLayout)
    }

    override fun clearFilters() {
        filtersAdapter.notifyDataSetChanged()
    }

    override fun setFeatureType(): Consumer<FeatureType> {
        return Consumer { type -> this.featureType = type }
    }

    override fun startNewEvent() {
        analyticsHelper.setEvent(CREATE_EVENT, DATA_CREATION, CREATE_EVENT)
        binding.addEventButton.isEnabled = false
        val bundle = Bundle()
        bundle.putString(PROGRAM_UID, programUid)
        startActivity(EventInitialActivity::class.java, bundle, false, false, null)
    }

    override fun setWritePermission(canWrite: Boolean) {
        binding.addEventButton.visibility = when {
            binding.addEventButton.visibility == View.VISIBLE && canWrite -> View.VISIBLE
            else -> View.GONE
        }
        binding.emptyTeis.setText(
            when {
                binding.addEventButton.visibility == View.VISIBLE -> R.string.empty_tei_add
                else -> R.string.empty_tei_no_add
            }
        )
    }

    override fun setTutorial() {
        Handler().postDelayed(
            {
                val stepConditions = SparseBooleanArray()
                stepConditions.put(
                    2,
                    binding.addEventButton.visibility == View.VISIBLE
                )
                HelpManager.getInstance().show(
                    activity, HelpManager.TutorialName.PROGRAM_EVENT_LIST,
                    stepConditions
                )
            },
            500
        )
    }

    override fun updateFilters(totalFilters: Int) {
        binding.totalFilters = totalFilters
        binding.executePendingBindings()
    }

    override fun setCatOptionComboFilter(
        categoryOptionCombos: Pair<CategoryCombo, List<CategoryOptionCombo>>
    ) {
        filtersAdapter.addCatOptCombFilter(categoryOptionCombos)
    }

    override fun showPeriodRequest(periodRequest: FilterManager.PeriodRequest) =
        if (periodRequest == FilterManager.PeriodRequest.FROM_TO) {
            DateUtils.getInstance().showFromToSelector(this, filterManager::addPeriod)
        } else {
            DateUtils.getInstance().showPeriodDialog(
                this,
                filterManager::addPeriod,
                true
            )
        }

    override fun openOrgUnitTreeSelector() {
        val ouTreeIntent = Intent(this, OUTreeActivity::class.java)
        val bundle = OUTreeActivity.getBundle(programUid)
        ouTreeIntent.putExtras(bundle)
        startActivityForResult(ouTreeIntent, FilterManager.OU_TREE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FilterManager.OU_TREE && resultCode == Activity.RESULT_OK) {
            filtersAdapter.notifyDataSetChanged()
            updateFilters(filterManager.totalFilters)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun showTutorial(shaked: Boolean) {
        setTutorial()
    }

    override fun setMap(): Consumer<kotlin.Pair<FeatureCollection, BoundingBox>> {
        return Consumer { (featureCollection, boundingBox) ->
            map?.let {
                (it.style?.getSource(EVENTS) as GeoJsonSource).setGeoJson(featureCollection)
                initCameraPosition(boundingBox)
            } ?: binding.mapView.getMapAsync { mapBoxMap ->
                mapBoxMap.style?.let {
                    (mapBoxMap.style?.getSource(EVENTS) as GeoJsonSource)
                        .setGeoJson(featureCollection)
                    initCameraPosition(boundingBox)
                } ?: mapBoxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                    mapBoxMap.addOnMapClickListener(this)
                    // TODO: GET STAGE ICON
                    style.addImage(
                        ICON_ID,
                        BitmapFactory.decodeResource(
                            resources,
                            R.drawable.mapbox_marker_icon_default
                        )
                    )
                    setSource(style, featureCollection)
                    setLayer(style)
                    initCameraPosition(boundingBox)
                    markerViewManager = MarkerViewManager(binding.mapView, mapBoxMap)
                    symbolManager = SymbolManager(
                        binding.mapView, mapBoxMap, style, null,
                        GeoJsonOptions().withTolerance(0.4f)
                    ).also {
                        it.iconAllowOverlap = true
                        it.textAllowOverlap = true
                        it.create(featureCollection)
                    }
                }
                map = mapBoxMap
            }
        }
    }

    private fun initCameraPosition(boundingBox: BoundingBox) {
        val bounds = LatLngBounds.from(
            boundingBox.north(),
            boundingBox.east(),
            boundingBox.south(),
            boundingBox.west()
        )
        map?.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50), 1200)
    }

    private fun setSource(style: Style, featureCollection: FeatureCollection) {
        style.addSource(GeoJsonSource(EVENTS, featureCollection))
    }

    override fun setEventInfo(programEventViewModel: Pair<ProgramEventViewModel, LatLng>) {
        currentMarker?.let {
            markerViewManager?.removeMarker(it)
            markerViewManager = null
        }
        val binding = InfoWindowEventBinding.inflate(LayoutInflater.from(this))
        binding.run {
            event = programEventViewModel.val0()
            presenter = this@ProgramEventDetailActivity.presenter
        }
        binding.root.run {
            setOnClickListener {
                currentMarker?.let {
                    markerViewManager?.removeMarker(it)
                    markerViewManager = null
                }
            }
            setOnLongClickListener {
                presenter.onEventClick(
                    programEventViewModel.val0().uid(),
                    programEventViewModel.val0().orgUnitUid()
                )
                true
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        currentMarker = MarkerView(
            programEventViewModel.val1(),
            binding.root
        ).also {
            markerViewManager?.addMarker(it)
        }
    }

    private fun setLayer(style: Style) {
        val symbolLayer = SymbolLayer(POINT_LAYER_ID, EVENTS).withProperties(
            PropertyFactory.iconImage(ICON_ID),
            iconAllowOverlap(true),
            iconOffset(arrayOf(0f, -9f))
        )
        symbolLayer.minZoom = 0f
        style.addLayer(symbolLayer)

        if (featureType != FeatureType.POINT) {
            style.addLayerBelow(
                FillLayer(POLYGON_LAYER_ID, EVENTS).withProperties(
                    fillColor(
                        ColorUtils.getPrimaryColorWithAlpha(
                            this,
                            ColorUtils.ColorType.PRIMARY_LIGHT,
                            150f
                        )
                    )
                ),
                "settlement-label"
            )
        }
    }

    override fun showMoreOptions(view: View) {
        val popupMenu = PopupMenu(this, view, Gravity.BOTTOM)
        try {
            val fields = popupMenu.javaClass.declaredFields
            for (field in fields) {
                if ("mPopup" == field.name) {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popupMenu)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons = classPopupHelper.getMethod(
                        "setForceShowIcon",
                        Boolean::class.javaPrimitiveType!!
                    )
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        popupMenu.menuInflater.inflate(R.menu.event_list_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.showHelp -> {
                    analyticsHelper.setEvent(SHOW_HELP, CLICK, SHOW_HELP)
                    showTutorial(false)
                }
                R.id.menu_list -> showMap(false)
                R.id.menu_map -> showMap(true)
            }
            false
        }
        val mapVisible = binding.mapView.visibility != View.GONE
        val listVisible = binding.recycler.visibility != View.GONE
        val emptyVisible = !mapVisible && !listVisible
        popupMenu.menu.getItem(0).isVisible =
            !emptyVisible && !mapVisible && featureType != FeatureType.NONE
        popupMenu.menu.getItem(1).isVisible = !emptyVisible &&
            binding.recycler.visibility == View.GONE &&
            featureType != FeatureType.NONE
        popupMenu.show()
    }

    override fun showSyncDialog(uid: String) {
        SyncStatusDialog.Builder()
            .setConflictType(SyncStatusDialog.ConflictType.EVENT)
            .setUid(uid)
            .onDismissListener(object : GranularSyncContracts.OnDismissListener {
                override fun onDismiss(hasChanged: Boolean) {
                    if (hasChanged) {
                        filterManager.publishData()
                    }
                }
            })
            .build()
            .show(supportFragmentManager, "")
    }

    override fun navigateToEvent(eventId: String, orgUnit: String) {
        val bundle = Bundle().also {
            it.putString(PROGRAM_UID, programUid)
            it.putString(Constants.EVENT_UID, eventId)
            it.putString(ORG_UNIT, orgUnit)
        }
        startActivity(EventCaptureActivity::class.java, bundle, false, false, null)
    }

    private fun showMap(showMap: Boolean) {
        binding.recycler.visibility = if (showMap) View.GONE else View.VISIBLE
        binding.mapView.visibility = if (showMap) View.VISIBLE else View.GONE

        if (showMap) {
            presenter.getMapData()
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        val pointf = map!!.projection.toScreenLocation(point)
        val rectF = RectF(pointf.x - 10, pointf.y - 10, pointf.x + 10, pointf.y + 10)
        val features = map?.queryRenderedFeatures(
            rectF,
            if (featureType == FeatureType.POINT) POINT_LAYER_ID else POLYGON_LAYER_ID
        )
        features?.forEach { feature ->
            presenter.getEventInfo(feature.getStringProperty("eventUid"), point)
        } ?: return false
        return true
    }

    companion object {

        const val ICON_ID = "ICON_ID"
        const val EXTRA_PROGRAM_UID = "PROGRAM_UID"
        const val EVENTS = "events"

        fun getBundle(programUid: String): Bundle {
            val bundle = Bundle()
            bundle.putString(EXTRA_PROGRAM_UID, programUid)
            return bundle
        }
    }
}
