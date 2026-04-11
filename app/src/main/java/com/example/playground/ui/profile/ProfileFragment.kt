package com.example.playground.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.playground.R
import com.example.playground.auth.AuthManager
import com.example.playground.data.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    private lateinit var authManager: AuthManager
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
        val db = AppDatabase.getInstance(requireContext())
        val userDao = db.userDao()
        
        var user = authManager.getCurrentUser()

        val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val savedCourtsInput = view.findViewById<TextInputEditText>(R.id.savedCourtsInput)
        val editProfileButton = view.findViewById<MaterialButton>(R.id.editProfileButton)
        val logoutButton = view.findViewById<MaterialButton>(R.id.logoutButton)

        nameInput.setText(user?.username ?: "Guest")
        emailInput.setText((if (user != null) user.username else "guest") + "@example.com")
        savedCourtsInput.setText("0 saved courts")
        nameInput.isEnabled = false

        editProfileButton.setOnClickListener {
            if (isEditing) {
                // Save logic
                val newName = nameInput.text.toString().trim()
                if (newName.isNotEmpty() && user != null) {
                    user = user!!.copy(username = newName)
                    userDao.updateUser(user!!)
                    nameInput.isEnabled = false
                    editProfileButton.text = "Edit Profile"
                    isEditing = false
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Enter edit mode
                nameInput.isEnabled = true
                nameInput.requestFocus()
                editProfileButton.text = "Save Profile"
                isEditing = true
            }
        }

        logoutButton.setOnClickListener {
            authManager.signOut()
            requireActivity().finish()
            startActivity(android.content.Intent(requireContext(), com.example.playground.SignInActivity::class.java))
        }
    }
}
