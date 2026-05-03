package com.example.playground

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.playground.auth.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)

        val emailLayout = findViewById<TextInputLayout>(R.id.usernameLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)
        val signInButton = findViewById<MaterialButton>(R.id.signInButton)
        val signUpLink = findViewById<MaterialButton>(R.id.signUpLink)

        signInButton.setOnClickListener {
            emailLayout.error = null
            passwordLayout.error = null

            val email = emailLayout.editText?.text.toString().trim()
            val password = passwordLayout.editText?.text.toString()

            signInButton.isEnabled = false
            lifecycleScope.launch {
                when (val result = authManager.signIn(email, password)) {
                    is AuthManager.AuthResult.Success -> {
                        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthManager.AuthResult.Error -> {
                        signInButton.isEnabled = true
                        when {
                            result.message.contains("Email", ignoreCase = true) ->
                                emailLayout.error = result.message
                            result.message.contains("Password", ignoreCase = true) ->
                                passwordLayout.error = result.message
                            else -> passwordLayout.error = result.message
                        }
                    }
                }
            }
        }

        signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
}
