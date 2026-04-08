package com.example.playground

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.playground.auth.AuthManager
import com.example.playground.ui.home.CreateEventActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)

        val currentUser = authManager.getCurrentUser()
        if (currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val signOutButton = findViewById<MaterialButton>(R.id.signOutButton)
        val createEventFab = findViewById<FloatingActionButton>(R.id.createEventFab)

        welcomeText.text = getString(R.string.welcome_message, currentUser.username)

        createEventFab.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }

        signOutButton.setOnClickListener {
            authManager.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}
