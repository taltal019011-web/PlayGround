package com.example.playground.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.playground.R
import com.example.playground.SignInActivity
import com.example.playground.repository.AuthRepository
import com.example.playground.viewmodel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel

    private lateinit var profileImage: ImageView
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var savedCourtsInput: TextInputEditText
    private lateinit var editProfileButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                Picasso.get()
                    .load(uri)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(profileImage)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

        profileImage = view.findViewById(R.id.profileImage)
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
            currentImageUrl = user?.profileImageUrl

            if (!user?.profileImageUrl.isNullOrBlank()) {
                Picasso.get()
                    .load(user?.profileImageUrl)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(android.R.drawable.sym_def_app_icon)
            }
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
        profileImage.setOnClickListener {
            if (viewModel.isEditing.value == true) {
                pickImageLauncher.launch("image/*")
            }
        }

        editProfileButton.setOnClickListener {
            if (viewModel.isEditing.value == true) {
                val uri = selectedImageUri
                if (uri != null) {
                    uploadProfileImage(uri)
                } else {
                    viewModel.saveProfile(nameInput.text.toString().trim(), currentImageUrl)
                }
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

    private fun uploadProfileImage(uri: Uri) {
        val user = viewModel.user.value ?: return
        editProfileButton.isEnabled = false

        val ref = FirebaseStorage.getInstance()
            .reference
            .child("profile_images/${user.firebaseUid}_${System.currentTimeMillis()}.jpg")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                currentImageUrl = downloadUri.toString()
                selectedImageUri = null
                editProfileButton.isEnabled = true
                viewModel.saveProfile(nameInput.text.toString().trim(), currentImageUrl)
            }
            .addOnFailureListener {
                editProfileButton.isEnabled = true
                Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
            }
    }
}