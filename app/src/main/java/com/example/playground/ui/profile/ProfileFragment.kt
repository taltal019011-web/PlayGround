package com.example.playground.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.playground.R
import com.example.playground.SignInActivity
import com.example.playground.auth.AuthManager
import com.example.playground.repository.AuthRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private lateinit var authRepository: AuthRepository
    private var isEditing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager(requireContext())
        authRepository = AuthRepository.getInstance(requireContext())

        val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val savedCourtsInput = view.findViewById<TextInputEditText>(R.id.savedCourtsInput)
        val editProfileButton = view.findViewById<MaterialButton>(R.id.editProfileButton)
        val logoutButton = view.findViewById<MaterialButton>(R.id.logoutButton)

        nameInput.isEnabled = false

        lifecycleScope.launch {
            var user = authManager.getCurrentUser()

            nameInput.setText(user?.displayName?.ifEmpty { user?.email } ?: "Guest")
            emailInput.setText(user?.email ?: "")
            savedCourtsInput.setText("0 saved courts")

            editProfileButton.setOnClickListener {
                if (isEditing) {
                    val newName = nameInput.text.toString().trim()
                    if (newName.isNotEmpty() && user != null) {
                        user = user!!.copy(displayName = newName)
                        lifecycleScope.launch {
                            authRepository.updateUser(user!!)
                        }
                        nameInput.isEnabled = false
                        editProfileButton.text = "Edit Profile"
                        isEditing = false
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    nameInput.isEnabled = true
                    nameInput.requestFocus()
                    editProfileButton.text = "Save Profile"
                    isEditing = true
                }
            }
        }

        logoutButton.setOnClickListener {
            authManager.signOut()
            requireActivity().finish()
            startActivity(Intent(requireContext(), SignInActivity::class.java))
        }
    }
}
