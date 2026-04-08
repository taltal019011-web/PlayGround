package com.example.playground.ui.home

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class CreateEventActivity : AppCompatActivity() {

    private lateinit var viewModel: CreateEventViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_event)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val authRepository = AuthRepository.getInstance(this)
        val eventRepository = EventRepository.getInstance(this)
        viewModel = CreateEventViewModel(authRepository, eventRepository)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val sportLayout = findViewById<TextInputLayout>(R.id.sportLayout)
        val titleLayout = findViewById<TextInputLayout>(R.id.titleLayout)
        val descriptionLayout = findViewById<TextInputLayout>(R.id.descriptionLayout)
        val dateLayout = findViewById<TextInputLayout>(R.id.dateLayout)
        val timeLayout = findViewById<TextInputLayout>(R.id.timeLayout)
        val maxPlayersLayout = findViewById<TextInputLayout>(R.id.maxPlayersLayout)
        val locationLabelLayout = findViewById<TextInputLayout>(R.id.locationLabelLayout)
        val publishButton = findViewById<MaterialButton>(R.id.publishButton)

        val dateEditText = dateLayout.editText!!
        val timeEditText = timeLayout.editText!!

        dateEditText.setOnClickListener { showDatePicker() }
        timeEditText.setOnClickListener { showTimePicker() }

        publishButton.setOnClickListener {
            val sport = sportLayout.editText?.text.toString().trim()
            val title = titleLayout.editText?.text.toString().trim()
            val description = descriptionLayout.editText?.text.toString().trim()
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
            this,
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
            this,
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
        findViewById<TextInputLayout>(R.id.dateLayout).editText?.setText(sdf.format(calendar.time))
    }

    private fun updateTimeLabel() {
        val myFormat = "HH:mm"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        findViewById<TextInputLayout>(R.id.timeLayout).editText?.setText(sdf.format(calendar.time))
    }

    private fun observeState() {
        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar)
        val publishButton = findViewById<MaterialButton>(R.id.publishButton)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                            Toast.makeText(this@CreateEventActivity, "Event Published!", Toast.LENGTH_SHORT).show()
                            finish()
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
        when {
            message.contains("Sport", true) -> findViewById<TextInputLayout>(R.id.sportLayout).error = message
            message.contains("Title", true) -> findViewById<TextInputLayout>(R.id.titleLayout).error = message
            message.contains("Max players", true) -> findViewById<TextInputLayout>(R.id.maxPlayersLayout).error = message
            message.contains("time", true) -> {
                findViewById<TextInputLayout>(R.id.dateLayout).error = message
                findViewById<TextInputLayout>(R.id.timeLayout).error = message
            }
            message.contains("Location", true) -> findViewById<TextInputLayout>(R.id.locationLabelLayout).error = message
            else -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
