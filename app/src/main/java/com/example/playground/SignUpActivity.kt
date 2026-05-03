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

class SignUpActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)

        val emailLayout = findViewById<TextInputLayout>(R.id.usernameLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)
        val signUpButton = findViewById<MaterialButton>(R.id.signUpButton)
        val signInLink = findViewById<MaterialButton>(R.id.signInLink)

        signUpButton.setOnClickListener {
            emailLayout.error = null
            passwordLayout.error = null

            val email = emailLayout.editText?.text.toString().trim()
            val password = passwordLayout.editText?.text.toString()

            signUpButton.isEnabled = false
            lifecycleScope.launch {
                when (val result = authManager.signUp(email, password)) {
                    is AuthManager.AuthResult.Success -> {
                        startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthManager.AuthResult.Error -> {
                        signUpButton.isEnabled = true
                        when {
                            result.message.contains("Email", ignoreCase = true) ->
                                emailLayout.error = result.message
                            result.message.contains("Password", ignoreCase = true) ->
                                passwordLayout.error = result.message
                            else -> emailLayout.error = result.message
                        }
                    }
                }
            }
        }

        signInLink.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}
