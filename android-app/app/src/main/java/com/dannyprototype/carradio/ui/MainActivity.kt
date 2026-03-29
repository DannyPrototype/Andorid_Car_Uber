package com.dannyprototype.carradio.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dannyprototype.carradio.R
import com.dannyprototype.carradio.databinding.ActivityMainBinding
import com.dannyprototype.carradio.export.FileShareHelper
import com.dannyprototype.carradio.service.LocationTrackingService
import com.dannyprototype.carradio.ui.dashboard.DashboardFragment
import com.dannyprototype.carradio.ui.dialogs.FuelRegistrationDialog
import com.dannyprototype.carradio.ui.dialogs.TripIncomeDialog
import com.dannyprototype.carradio.ui.map.MapFragment
import com.dannyprototype.carradio.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MapFragment.FuelAlertListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var locationService: LocationTrackingService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as LocationTrackingService.LocalBinder).getService()
            locationService = service
            serviceBound = true

            // Collect location updates and forward to ViewModel
            lifecycleScope.launch {
                service.locationUpdates.collect { point ->
                    viewModel.onLocationUpdate(point)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            serviceBound = false
        }
    }

    // Permission request
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                getString(R.string.permission_location_rationale),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permissions
        if (!PermissionHelper.hasLocationPermission(this)) {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
        }

        // Load fragments
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentDashboard, DashboardFragment())
                .replace(R.id.fragmentMap, MapFragment())
                .commit()
        }

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener { onStartTrip() }
        binding.btnStop.setOnClickListener { onEndTrip() }
        binding.btnFuel.setOnClickListener { showFuelDialog() }
        binding.btnExport.setOnClickListener { onExportCsv() }
        binding.btnMode.setOnClickListener { onChangeMode() }
    }

    private fun onStartTrip() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions())
            return
        }

        viewModel.startTrip()

        // Start and bind location service
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        locationService?.startTracking()

        binding.btnStart.isEnabled = false
        binding.btnStop.isEnabled = true
        binding.btnMode.isEnabled = false

        Toast.makeText(
            this,
            getString(R.string.toast_trip_started, viewModel.dashboardState.value.currentMode.displayName),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onEndTrip() {
        // Stop location service
        locationService?.stopTracking()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }

        binding.btnStart.isEnabled = true
        binding.btnStop.isEnabled = false
        binding.btnMode.isEnabled = true

        // If Uber with passenger, show income dialog
        if (viewModel.isUberWithPassenger()) {
            TripIncomeDialog().show(supportFragmentManager, TripIncomeDialog.TAG)
        } else {
            viewModel.endTrip(0.0)
            Toast.makeText(
                this,
                getString(R.string.toast_trip_ended, viewModel.dashboardState.value.tripDistanceKm),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showFuelDialog() {
        FuelRegistrationDialog().show(supportFragmentManager, FuelRegistrationDialog.TAG)
    }

    override fun onFuelAlertRegister() {
        showFuelDialog()
    }

    private fun onExportCsv() {
        viewModel.exportCsvAsync { path ->
            runOnUiThread {
                if (path == null) {
                    Toast.makeText(this, getString(R.string.toast_no_trips), Toast.LENGTH_SHORT).show()
                } else {
                    FileShareHelper.shareFile(this, path)
                    Toast.makeText(this, getString(R.string.toast_csv_exported, 0), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onChangeMode() {
        if (viewModel.isTripActive()) {
            Toast.makeText(this, getString(R.string.toast_end_trip_first), Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.changeMode()
        Toast.makeText(
            this,
            getString(R.string.toast_mode_changed, viewModel.dashboardState.value.currentMode.displayName),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
        }
    }
}
