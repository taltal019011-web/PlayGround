package com.example.playground.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.playground.R
import com.example.playground.repository.AuthRepository
import com.example.playground.repository.EventRepository
import com.example.playground.viewmodel.CreateEventViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class CreateEventFragment : Fragment() {

    private lateinit var viewModel: CreateEventViewModel

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

        val sportGroup = view.findViewById<ChipGroup>(R.id.sportChipGroup)
        val descriptionEdit = view.findViewById<TextInputEditText>(R.id.descriptionEditText)
        val addressEdit = view.findViewById<TextInputEditText>(R.id.addressEditText)
        val publishButton = view.findViewById<MaterialButton>(R.id.publishButton)

        publishButton.setOnClickListener {
            val selectedChipId = sportGroup.checkedChipId
            if (selectedChipId == View.NO_ID) {
                Toast.makeText(context, "Please select a sport", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val sport = view.findViewById<Chip>(selectedChipId).text.toString()
            val description = descriptionEdit.text.toString()
            val address = addressEdit.text.toString()

            viewModel.createEvent(
                sport = sport,
                title = "$sport Game",
                description = description,
                startTime = System.currentTimeMillis() + 86400000,
                maxPlayers = 10,
                latitude = 32.0853,
                longitude = 34.7818,
                locationLabel = address
            )
            Toast.makeText(context, "Event created!", Toast.LENGTH_SHORT).show()
        }
    }
}
