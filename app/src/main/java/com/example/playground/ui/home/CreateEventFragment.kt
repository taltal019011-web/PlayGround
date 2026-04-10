package com.example.playground.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.playground.R
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import com.example.playground.viewmodel.CreateEventViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateEventFragment : Fragment() {

    private lateinit var viewModel: CreateEventViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepository = AuthRepository.getInstance(requireContext())
        val eventRepository = EventRepository.getInstance(requireContext())
        viewModel = CreateEventViewModel(authRepository, eventRepository)

        setupUI(view)
        observeState(view)
    }

    private fun setupUI(view: View) {
        val sportLayout = view.findViewById<TextInputLayout>(R.id.sportLayout)
        val titleLayout = view.findViewById<TextInputLayout>(R.id.titleLayout)
        val descriptionLayout = view.findViewById<TextInputLayout>(R.id.descriptionLayout)
        val addressLayout = view.findViewById<TextInputLayout>(R.id.addressLayout)
        val dateLayout = view.findViewById<TextInputLayout>(R.id.dateLayout)
        val timeLayout = view.findViewById<TextInputLayout>(R.id.timeLayout)
        val maxPlayersLayout = view.findViewById<TextInputLayout>(R.id.maxPlayersLayout)
        val locationLabelLayout = view.findViewById<TextInputLayout>(R.id.locationLabelLayout)
        val publishButton = view.findViewById<MaterialButton>(R.id.publishButton)

        val dateEditText = dateLayout.editText!!
        val timeEditText = timeLayout.editText!!

        dateEditText.setOnClickListener { showDatePicker() }
        timeEditText.setOnClickListener { showTimePicker() }

        publishButton.setOnClickListener {
            val sport = sportLayout.editText?.text.toString().trim()
            val title = titleLayout.editText?.text.toString().trim()
            val description = descriptionLayout.editText?.text.toString().trim()
            val address = addressLayout.editText?.text.toString().trim()
            val maxPlayers = maxPlayersLayout.editText?.text.toString().toIntOrNull() ?: 0
            val locationLabel = locationLabelLayout.editText?.text.toString().trim()

            // Reset errors
            sportLayout.error = null
            titleLayout.error = null
            dateLayout.error = null
            timeLayout.error = null
            maxPlayersLayout.error = null
            locationLabelLayout.error = null

            viewModel.createEvent(
                sport = sport,
                title = title,
                description = if (description.isEmpty()) null else description,
                startTime = calendar.timeInMillis,
                maxPlayers = maxPlayers,
                latitude = 32.0853, // Simulated default
                longitude = 34.7818, // Simulated default
                locationLabel = locationLabel
            )
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateLabel()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                updateTimeLabel()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateLabel() {
        val myFormat = "MMM dd, yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        view?.findViewById<TextInputLayout>(R.id.dateLayout)?.editText?.setText(sdf.format(calendar.time))
    }

    private fun updateTimeLabel() {
        val myFormat = "HH:mm"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        view?.findViewById<TextInputLayout>(R.id.timeLayout)?.editText?.setText(sdf.format(calendar.time))
    }

    private fun observeState(view: View) {
        val progressBar = view.findViewById<LinearProgressIndicator>(R.id.progressBar)
        val publishButton = view.findViewById<MaterialButton>(R.id.publishButton)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createEventState.collect { state ->
                    when (state) {
                        is CreateEventViewModel.CreateEventState.Idle -> {
                            progressBar.visibility = View.GONE
                            publishButton.isEnabled = true
                        }
                        is CreateEventViewModel.CreateEventState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            publishButton.isEnabled = false
                        }
                        is CreateEventViewModel.CreateEventState.Success -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Event Published!", Toast.LENGTH_SHORT).show()
                            // Option: switch to map tab
                        }
                        is CreateEventViewModel.CreateEventState.Error -> {
                            progressBar.visibility = View.GONE
                            publishButton.isEnabled = true
                            handleError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun handleError(message: String) {
        val view = view ?: return
        when {
            message.contains("Sport", true) -> view.findViewById<TextInputLayout>(R.id.sportLayout).error = message
            message.contains("Title", true) -> view.findViewById<TextInputLayout>(R.id.titleLayout).error = message
            message.contains("Max players", true) -> view.findViewById<TextInputLayout>(R.id.maxPlayersLayout).error = message
            message.contains("time", true) -> {
                view.findViewById<TextInputLayout>(R.id.dateLayout).error = message
                view.findViewById<TextInputLayout>(R.id.timeLayout).error = message
            }
            message.contains("Location", true) -> view.findViewById<TextInputLayout>(R.id.locationLabelLayout).error = message
            else -> Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }
}
