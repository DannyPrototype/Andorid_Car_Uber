package com.dannyprototype.carradio.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.dannyprototype.carradio.R
import com.dannyprototype.carradio.databinding.DialogTripIncomeBinding
import com.dannyprototype.carradio.ui.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TripIncomeDialog : DialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogTripIncomeBinding.inflate(LayoutInflater.from(context))

        // Show trip summary
        val (distKm, estCost) = viewModel.getTripSummaryForDialog()
        binding.tvTripSummary.text = getString(
            R.string.income_dialog_message, distKm, estCost
        )

        binding.btnIncomeSkip.setOnClickListener {
            viewModel.endTrip(0.0)
            dismiss()
        }

        binding.btnIncomeAccept.setOnClickListener {
            val incomeStr = binding.etIncome.text.toString()
            val income = incomeStr.toDoubleOrNull()

            if (income == null || income <= 0) {
                Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.endTrip(income)
            val profit = income - estCost
            val sign = if (profit >= 0) "+" else ""
            Toast.makeText(
                context,
                String.format("Viaje: $%.2f | Ganancia: %s$%.2f", income, sign, profit),
                Toast.LENGTH_LONG
            ).show()
            dismiss()
        }

        isCancelable = false

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    companion object {
        const val TAG = "TripIncomeDialog"
    }
}
