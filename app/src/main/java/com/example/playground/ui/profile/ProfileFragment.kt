package com.example.playground.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.playground.R
import com.example.playground.SignInActivity
import com.example.playground.repository.AuthRepository
import com.example.playground.viewmodel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var savedCourtsInput: TextInputEditText
    private lateinit var editProfileButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepository = AuthRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(
            this,
            ProfileViewModel.Factory(authRepository)
        )[ProfileViewModel::class.java]

        nameInput = view.findViewById(R.id.nameInput)
        emailInput = view.findViewById(R.id.emailInput)
        savedCourtsInput = view.findViewById(R.id.savedCourtsInput)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        logoutButton = view.findViewById(R.id.logoutButton)

        savedCourtsInput.setText("0 saved courts")
        nameInput.isEnabled = false

        observeViewModel()
        setupActions()
        viewModel.loadUser()
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            nameInput.setText(user?.displayName?.ifEmpty { user.email } ?: "Guest")
            emailInput.setText(user?.email ?: "")
        }

        viewModel.isEditing.observe(viewLifecycleOwner) { editing ->
            nameInput.isEnabled = editing
            editProfileButton.text = if (editing) "Save Profile" else "Edit Profile"
            if (editing) nameInput.requestFocus()
        }

        viewModel.profileSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupActions() {
        editProfileButton.setOnClickListener {
            if (viewModel.isEditing.value == true) {
                viewModel.saveProfile(nameInput.text.toString().trim())
            } else {
                viewModel.startEditing()
            }
        }

        logoutButton.setOnClickListener {
            viewModel.signOut()
            requireActivity().finish()
            startActivity(Intent(requireContext(), SignInActivity::class.java))
        }
    }
}
