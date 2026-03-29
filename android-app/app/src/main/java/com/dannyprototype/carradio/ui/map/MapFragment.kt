package com.dannyprototype.carradio.ui.map

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dannyprototype.carradio.R
import com.dannyprototype.carradio.databinding.FragmentMapBinding
import com.dannyprototype.carradio.ui.MainViewModel
import com.dannyprototype.carradio.ui.state.MapUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var carMarker: Marker? = null
    private var routePolyline: Polyline? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()

        // Fuel alert buttons
        binding.btnFuelAlertRegister.setOnClickListener {
            (activity as? FuelAlertListener)?.onFuelAlertRegister()
        }
        binding.btnFuelAlertDismiss.setOnClickListener {
            viewModel.dismissFuelAlert()
        }

        // Observe states
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.mapState.collect { updateMap(it) } }
                launch {
                    viewModel.dashboardState.collect { state ->
                        // Fuel alert visibility
                        binding.fuelAlertOverlay.visibility =
                            if (state.fuelAlertVisible) View.VISIBLE else View.GONE

                        // Speed overlay
                        binding.mapSpeedValue.text = state.speedKmh.toString()
                        val speedColor = when {
                            state.isStopped -> R.color.speed_stopped
                            state.speedKmh < 20 -> R.color.speed_slow
                            state.speedKmh > 50 -> R.color.speed_fast
                            else -> R.color.speed_normal
                        }
                        binding.mapSpeedValue.setTextColor(
                            ContextCompat.getColor(requireContext(), speedColor)
                        )
                    }
                }
            }
        }
    }

    private fun setupMap() {
        // Configure Osmdroid
        Configuration.getInstance().userAgentValue = requireContext().packageName

        val mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapView.controller.setZoom(16.0)

        // Dark overlay to match car-radio theme
        mapView.overlayManager.tilesOverlay.setColorFilter(
            android.graphics.ColorMatrixColorFilter(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,   // Red
                     0f,-1f, 0f, 0f, 255f,   // Green
                     0f, 0f,-1f, 0f, 255f,   // Blue
                     0f, 0f, 0f, 1f, 0f      // Alpha
                )
            )
        )

        // Initialize route polyline
        routePolyline = Polyline().apply {
            outlinePaint.color = Color.parseColor("#00B4D8")
            outlinePaint.strokeWidth = 8f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.isAntiAlias = true
        }
        mapView.overlays.add(routePolyline)
    }

    private fun updateMap(state: MapUiState) {
        val pos = state.currentPosition ?: return
        val geoPoint = GeoPoint(pos.latitude, pos.longitude)
        val mapView = binding.mapView

        // Update or create car marker
        if (carMarker == null) {
            carMarker = Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Mi posición"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car_marker)
            }
            mapView.overlays.add(carMarker)
        } else {
            carMarker?.position = geoPoint
            carMarker?.rotation = -state.bearing
        }

        // Update route polyline
        if (state.routePoints.size >= 2) {
            val osmdroidPoints = state.routePoints.map {
                GeoPoint(it.latitude, it.longitude)
            }
            routePolyline?.setPoints(osmdroidPoints)
        }

        // Animate camera to follow car
        mapView.controller.animateTo(geoPoint)
        mapView.invalidate()
    }

    interface FuelAlertListener {
        fun onFuelAlertRegister()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
