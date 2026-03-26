package com.example.pawsitive.ngo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.pawsitive.R
import com.example.pawsitive.util.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth

class NGORegistrationActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etLicense: EditText
    private lateinit var etDescription: EditText
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var btnUploadCertificate: Button
    private lateinit var tvCertificateStatus: TextView
    private lateinit var btnSendForVerification: Button
    private lateinit var progressBar: ProgressBar

    private var profilePhotoUri: Uri? = null
    private var certificateUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val firestoreHelper = FirestoreHelper()

    private val pickProfileImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            profilePhotoUri = result.data?.data
            ivProfilePhoto.setImageURI(profilePhotoUri)
        }
    }

    private val pickCertificateLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            certificateUri = uri
            val name = getFileName(uri)
            tvCertificateStatus.text = "Selected: $name"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ngo_registration)

        etName = findViewById(R.id.etNgoName)
        etPhone = findViewById(R.id.etNgoPhone)
        etAddress = findViewById(R.id.etNgoAddress)
        etLicense = findViewById(R.id.etNgoLicense)
        etDescription = findViewById(R.id.etNgoDescription)
        ivProfilePhoto = findViewById(R.id.ivNgoProfilePhoto)
        btnChangePhoto = findViewById(R.id.btnChangeNgoPhoto)
        btnUploadCertificate = findViewById(R.id.btnUploadCertificate)
        tvCertificateStatus = findViewById(R.id.tvCertificateStatus)
        btnSendForVerification = findViewById(R.id.btnSendForVerification)
        progressBar = findViewById(R.id.progressNgo)

        btnChangePhoto.setOnClickListener { openProfileImagePicker() }
        btnUploadCertificate.setOnClickListener { openCertificatePicker() }
        btnSendForVerification.setOnClickListener { submitNgoProfile() }
    }

    private fun openProfileImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickProfileImageLauncher.launch(intent)
    }

    private fun openCertificatePicker() {
        pickCertificateLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    private fun submitNgoProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (!user.isEmailVerified) {
            Toast.makeText(this, "Please verify your email first", Toast.LENGTH_LONG).show()
            return
        }

        val uid = user.uid
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val license = etLicense.text.toString().trim()
        val desc = etDescription.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || license.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "All fields are mandatory", Toast.LENGTH_SHORT).show()
            return
        }
        if (profilePhotoUri == null) {
            Toast.makeText(this, "Profile photo is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (certificateUri == null) {
            Toast.makeText(this, "Certificate is required", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        firestoreHelper.uploadProfileImage(uid, profilePhotoUri, isNgo = true) { photoUrl, photoErr ->
            if (photoErr != null || photoUrl == null) {
                setLoading(false)
                Toast.makeText(this, photoErr ?: "Failed to upload profile photo", Toast.LENGTH_SHORT).show()
                return@uploadProfileImage
            }

            firestoreHelper.uploadCertificate(uid, certificateUri!!) { certUrl, certErr ->
                if (certErr != null || certUrl == null) {
                    setLoading(false)
                    Toast.makeText(this, certErr ?: "Failed to upload certificate", Toast.LENGTH_SHORT).show()
                    return@uploadCertificate
                }

                firestoreHelper.saveNgoProfile(
                    uid = uid,
                    name = name,
                    phone = phone,
                    address = address,
                    licenseNumber = license,
                    description = desc,
                    profilePhotoUrl = photoUrl,
                    certificateUrl = certUrl
                ) { success, errMsg ->
                    setLoading(false)
                    if (success) {
                        Toast.makeText(this, "Your application has been sent for verification.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, NGOHomeActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, errMsg ?: "Failed to save NGO profile", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSendForVerification.isEnabled = !loading
    }

    private fun getFileName(uri: Uri): String {
        var result = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                result = cursor.getString(nameIndex)
            }
        }
        return if (result.isNotEmpty()) result else uri.lastPathSegment ?: "file"
    }
}

