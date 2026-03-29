package com.dannyprototype.carradio.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.dannyprototype.carradio.databinding.DialogFuelRegistrationBinding
import com.dannyprototype.carradio.ui.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FuelRegistrationDialog : DialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogFuelRegistrationBinding.inflate(LayoutInflater.from(context))

        binding.btnFuelCancel.setOnClickListener { dismiss() }

        binding.btnFuelRegister.setOnClickListener {
            val costStr = binding.etFuelCost.text.toString()
            val kmStr = binding.etFuelKm.text.toString()

            val cost = costStr.toDoubleOrNull()
            val km = kmStr.toDoubleOrNull()

            if (cost == null || cost <= 0 || km == null || km <= 0) {
                Toast.makeText(context, "Ingresa valores válidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.registerFuel(cost, km)
            val costPerKm = cost / km
            Toast.makeText(
                context,
                String.format("Combustible: $%.2f / %.0f km ($%.3f/km)", cost, km, costPerKm),
                Toast.LENGTH_LONG
            ).show()
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    companion object {
        const val TAG = "FuelRegistrationDialog"
    }
}
