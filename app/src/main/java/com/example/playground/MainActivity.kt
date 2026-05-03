package com.example.playground

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.playground.auth.AuthManager
import com.example.playground.ui.home.CreateEventFragment
import com.example.playground.ui.map.MapFragment
import com.example.playground.ui.myposts.MyPostsFragment
import com.example.playground.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)

        lifecycleScope.launch {
            val currentUser = authManager.getCurrentUser()
            if (currentUser == null) {
                startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                finish()
                return@launch
            }

            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_map -> replaceFragment(MapFragment())
                    R.id.nav_create -> replaceFragment(CreateEventFragment())
                    R.id.nav_my_posts -> replaceFragment(MyPostsFragment())
                    R.id.nav_profile -> replaceFragment(ProfileFragment())
                }
                true
            }

            if (savedInstanceState == null) {
                replaceFragment(MapFragment())
                bottomNav.selectedItemId = R.id.nav_map
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
