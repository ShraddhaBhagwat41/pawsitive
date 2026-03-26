package com.example.pawsitive.util

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun uploadProfileImage(
        uid: String,
        imageUri: Uri?,
        isNgo: Boolean,
        callback: (url: String?, error: String?) -> Unit
    ) {
        if (imageUri == null) {
            callback(null, null)
            return
        }

        val folder = if (isNgo) "ngo_profile_images" else "users_profile_images"
        val ref = storage.reference.child("$folder/$uid.jpg")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString(), null)
                }.addOnFailureListener { e -> callback(null, e.localizedMessage) }
            }
            .addOnFailureListener { e -> callback(null, e.localizedMessage) }
    }

    fun uploadCertificate(
        uid: String,
        fileUri: Uri,
        callback: (url: String?, error: String?) -> Unit
    ) {
        val ref = storage.reference.child("ngo_certificates/${uid}_${System.currentTimeMillis()}")

        ref.putFile(fileUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString(), null)
                }.addOnFailureListener { e -> callback(null, e.localizedMessage) }
            }
            .addOnFailureListener { e -> callback(null, e.localizedMessage) }
    }

    fun saveUserProfile(
        uid: String,
        fullName: String,
        email: String,
        phone: String?,
        description: String?,
        latitude: Double?,
        longitude: Double?,
        callback: (Boolean, String?) -> Unit
    ) {
        val data = hashMapOf(
            "uid" to uid,
            "full_name" to fullName,
            "email" to email,
            "phone" to (phone ?: ""),
            "description" to (description ?: ""),
            "latitude" to (latitude ?: 0.0),
            "longitude" to (longitude ?: 0.0),
            "role" to "USER",
            "created_at" to Timestamp.now()
        )

        db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.localizedMessage) }
    }

    fun saveNgoProfile(
        uid: String,
        name: String,
        phone: String,
        address: String,
        licenseNumber: String,
        description: String,
        profilePhotoUrl: String,
        certificateUrl: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val data = hashMapOf(
            "uid" to uid,
            "organization_name" to name,
            "phone" to phone,
            "address" to address,
            "license_number" to licenseNumber,
            "certificate_url" to certificateUrl,
            "profile_photo_url" to profilePhotoUrl,
            "verification_status" to "PENDING",
            "created_at" to Timestamp.now(),
            "role" to "NGO"
        )

        db.collection("ngo_profiles").document(uid)
            .set(data)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.localizedMessage) }
    }

    fun fetchUserRole(uid: String, callback: (role: String?, error: String?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    callback(userDoc.getString("role") ?: "USER", null)
                } else {
                    db.collection("ngo_profiles").document(uid).get()
                        .addOnSuccessListener { ngoDoc ->
                            if (ngoDoc.exists()) {
                                callback(ngoDoc.getString("role") ?: "NGO", null)
                            } else {
                                callback(null, null)
                            }
                        }
                        .addOnFailureListener { e -> callback(null, e.localizedMessage) }
                }
            }
            .addOnFailureListener { e -> callback(null, e.localizedMessage) }
    }
}

