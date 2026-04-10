package com.example.playground.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.playground.R
import com.example.playground.SignInActivity
import com.example.playground.auth.AuthManager
import com.google.android.material.button.MaterialButton

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

        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val emailText = view.findViewById<TextView>(R.id.emailText)
        val logoutButton = view.findViewById<MaterialButton>(R.id.logoutButton)

        usernameText.text = user?.username ?: "Guest"
        emailText.text = if (user != null) user.username else "guest" + "@example.com"

        logoutButton.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
        }
    }
}
