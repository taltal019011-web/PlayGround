package com.example.playground.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.playground.R
import com.example.playground.SignInActivity
import com.example.playground.auth.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    private lateinit var authManager: AuthManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = AuthManager(requireContext())
        val user = authManager.getCurrentUser()

        val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val savedCourtsInput = view.findViewById<TextInputEditText>(R.id.savedCourtsInput)
        val editProfileButton = view.findViewById<MaterialButton>(R.id.editProfileButton)
        val logoutButton = view.findViewById<MaterialButton>(R.id.logoutButton)

        nameInput.setText(user?.username ?: "Guest")
        emailInput.setText((if (user != null) user.username else "guest") + "@example.com")
        savedCourtsInput.setText("0 saved courts")

        editProfileButton.setOnClickListener {
            Toast.makeText(requireContext(), "Edit profile not implemented in demo", Toast.LENGTH_SHORT).show()
        }

        logoutButton.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
        }
    }
}
