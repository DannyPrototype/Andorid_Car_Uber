package com.dannyprototype.carradio.ui.map

import android.graphics.Color
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private var googleMap: GoogleMap? = null
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

        // Initialize Google Map
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            setupMap(map)
        }

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

    private fun setupMap(map: GoogleMap) {
        // Dark map style
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark)
            )
        } catch (_: Exception) {
            // Fallback: no custom style
        }

        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }
    }

    private fun updateMap(state: MapUiState) {
        val map = googleMap ?: return
        val pos = state.currentPosition ?: return

        // Update or create car marker
        if (carMarker == null) {
            carMarker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("Mi posición")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                    .flat(true)
            )
        } else {
            carMarker?.position = pos
            carMarker?.rotation = state.bearing
        }

        // Update route polyline
        if (state.routePoints.size >= 2) {
            if (routePolyline == null) {
                routePolyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(state.routePoints)
                        .color(Color.parseColor("#00B4D8"))
                        .width(8f)
                )
            } else {
                routePolyline?.points = state.routePoints
            }
        }

        // Move camera
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, 16f)
        map.animateCamera(cameraUpdate)
    }

    interface FuelAlertListener {
        fun onFuelAlertRegister()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
