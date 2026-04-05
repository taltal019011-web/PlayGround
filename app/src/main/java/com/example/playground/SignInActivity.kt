package com.example.playground

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.playground.auth.AuthManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

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

        val usernameLayout = findViewById<TextInputLayout>(R.id.usernameLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)
        val signInButton = findViewById<MaterialButton>(R.id.signInButton)
        val signUpLink = findViewById<MaterialButton>(R.id.signUpLink)

        signInButton.setOnClickListener {
            usernameLayout.error = null
            passwordLayout.error = null

            val username = usernameLayout.editText?.text.toString().trim()
            val password = passwordLayout.editText?.text.toString()

            when (val result = authManager.signIn(username, password)) {
                is AuthManager.AuthResult.Success -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthManager.AuthResult.Error -> {
                    when {
                        result.message.contains("Username is required") ->
                            usernameLayout.error = result.message
                        result.message.contains("Password is required") ->
                            passwordLayout.error = result.message
                        else -> passwordLayout.error = result.message
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
