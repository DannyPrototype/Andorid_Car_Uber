package com.dannyprototype.carradio.ui.dashboard

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
import com.dannyprototype.carradio.databinding.FragmentDashboardBinding
import com.dannyprototype.carradio.model.TripMode
import com.dannyprototype.carradio.ui.MainViewModel
import com.dannyprototype.carradio.ui.state.DashboardUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardState.collect { state -> updateUi(state) }
            }
        }
    }

    private fun updateUi(state: DashboardUiState) {
        // Speed
        binding.tvSpeed.text = state.speedKmh.toString()
        binding.speedIndicator.progress = state.speedBarPercent

        val speedColor = when {
            state.isStopped -> R.color.speed_stopped
            state.speedKmh < 20 -> R.color.speed_slow
            state.speedKmh > 50 -> R.color.speed_fast
            else -> R.color.speed_normal
        }
        binding.tvSpeed.setTextColor(ContextCompat.getColor(requireContext(), speedColor))

        // Distances
        binding.tvTripDistance.text = String.format("%.2f", state.tripDistanceKm)
        binding.tvDayKm.text = String.format("%.2f", state.dayKm)

        // Times
        binding.tvTripTime.text = state.tripTime
        binding.tvStoppedTime.text = state.stoppedTime
        binding.tvStoppedPercent.text = getString(R.string.format_percent, state.stoppedPercent)

        // Stopped card highlight
        val stoppedBg = if (state.consecutiveStoppedSec >= 5 && state.tripActive) {
            R.color.speed_stopped
        } else {
            R.color.border_card
        }
        binding.stoppedCard.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_data_card)

        // Mode bar
        val mode = state.currentMode
        binding.modeIcon.text = mode.iconEmoji
        binding.modeLabel.text = mode.displayName
        binding.modeLabel.setTextColor(ContextCompat.getColor(requireContext(), mode.colorRes))
        binding.modeBar.setBackgroundResource(mode.bgDrawableRes)
        binding.modeStats.text = getString(
            R.string.format_mode_stats,
            state.currentModeKm, state.currentModeTrips
        )

        // Fuel
        binding.fuelProgressBar.progress = state.fuelBarPercent
        binding.tvFuelBarText.text = getString(
            R.string.format_fuel_bar,
            state.fuelCurrentKm, state.fuelCapacityKm
        )
        binding.tvFuelCostPerKm.text = getString(R.string.format_cost_per_km, state.fuelCostPerKm)
        binding.tvFuelUsed.text = String.format("Usado: %.1f km", state.fuelCurrentKm)
        binding.tvFuelBlockCost.text = String.format("Bloque: $%.2f", state.fuelBlockCost)
        binding.tvFuelRemaining.text = String.format("Queda: %.1f km", state.fuelRemainingKm)
        binding.tvFuelRemainingDollars.text = String.format("Restante: $%.2f", state.fuelRemainingDollars)

        // Fuel bar color
        val fuelDrawable = when {
            state.fuelIsDanger -> R.drawable.bg_fuel_bar_danger
            state.fuelIsWarning -> R.drawable.bg_fuel_bar_warning
            else -> R.drawable.bg_fuel_bar
        }
        binding.fuelProgressBar.progressDrawable =
            ContextCompat.getDrawable(requireContext(), fuelDrawable)

        // Finances
        binding.tvUberIncome.text = String.format("$%.2f", state.uberIncome)
        binding.tvTripCost.text = String.format("$%.2f", state.tripCost)
        binding.tvTotalCost.text = String.format("$%.2f", state.totalDayCost)

        val profitSign = if (state.profit >= 0) "+$" else "-$"
        binding.tvProfit.text = String.format("%s%.2f", profitSign, kotlin.math.abs(state.profit))
        val profitColor = if (state.profit >= 0) R.color.finance_green else R.color.finance_red
        binding.tvProfit.setTextColor(ContextCompat.getColor(requireContext(), profitColor))

        // Mode summaries
        binding.tvSumPersonal.text = String.format("Personal: %.1f km", state.personalKm)
        binding.tvSumUberPass.text = String.format(
            "Con Pasaj: %.1f km · %d viajes",
            state.uberPassengerKm, state.uberPassengerTrips
        )
        binding.tvSumUberEmpty.text = String.format("Sin Pasaj: %.1f km", state.uberEmptyKm)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
