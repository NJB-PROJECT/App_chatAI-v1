package com.example.geminichat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.geminichat.R
import com.example.geminichat.databinding.DialogSettingsBinding
import com.example.geminichat.ui.viewmodel.ChatViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    // Scoping to Activity so we share the same ViewModel instance if needed,
    // but here we just need to save prefs, so a new VM or Activity scoped VM is fine.
    // Let's use Activity scoped to be safe.
    private val viewModel: ChatViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            binding.switchCustomKey.isChecked = viewModel.isCustomKeyEnabled.first()
            binding.etApiKey.setText(viewModel.customApiKey.first() ?: "")

            val model = viewModel.modelType.first()
            when (model) {
                "pro" -> binding.rbPro.isChecked = true
                "flash" -> binding.rbFlash.isChecked = true
                "flash8b" -> binding.rbFlash8b.isChecked = true
                else -> {
                    binding.rbCustom.isChecked = true
                    binding.etCustomModel.setText(model)
                    binding.tilCustomModel.visibility = View.VISIBLE
                }
            }

            val safety = viewModel.safetyLevel.first()
            binding.seekBarSafety.progress = safety
            updateSafetyLabel(safety)

            updateInputState(binding.switchCustomKey.isChecked)
        }
    }

    private fun setupListeners() {
        binding.switchCustomKey.setOnCheckedChangeListener { _, isChecked ->
            updateInputState(isChecked)
        }

        binding.rgModel.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbCustom) {
                binding.tilCustomModel.visibility = View.VISIBLE
            } else {
                binding.tilCustomModel.visibility = View.GONE
            }
        }

        binding.seekBarSafety.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSafetyLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnSaveSettings.setOnClickListener {
            val isCustomKey = binding.switchCustomKey.isChecked
            val customKey = binding.etApiKey.text.toString().trim()

            val model = when (binding.rgModel.checkedRadioButtonId) {
                R.id.rbPro -> "pro"
                R.id.rbFlash -> "flash"
                R.id.rbFlash8b -> "flash8b"
                R.id.rbCustom -> binding.etCustomModel.text.toString().trim().ifEmpty { "gemini-1.5-flash" }
                else -> "flash"
            }

            val safety = binding.seekBarSafety.progress

            viewModel.updateSettings(isCustomKey, customKey, model, safety)
            Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun updateInputState(enabled: Boolean) {
        binding.tilApiKey.isEnabled = enabled
    }

    private fun updateSafetyLabel(progress: Int) {
        val text = when(progress) {
            0 -> "Current: Allow All (18+)"
            1 -> "Current: Block Some (Default)"
            2 -> "Current: Block All (Strict)"
            else -> "Current: Block Some"
        }
        binding.tvSafetyDescription.text = text
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
