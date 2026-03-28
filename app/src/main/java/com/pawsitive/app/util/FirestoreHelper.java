package com.pawsitive.app.util;

import android.net.Uri;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public interface Callback<T> {
        void onComplete(T result, String error);
    }

    public void uploadProfileImage(String uid, Uri imageUri, boolean isNgo, Callback<String> callback) {
        if (imageUri == null) {
            callback.onComplete(null, null);
            return;
        }

        String folder = isNgo ? "ngo_profile_images" : "users_profile_images";
        StorageReference ref = storage.getReference().child(folder + "/" + uid + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onComplete(uri.toString(), null))
                        .addOnFailureListener(e -> callback.onComplete(null, e.getLocalizedMessage())))
                .addOnFailureListener(e -> callback.onComplete(null, e.getLocalizedMessage()));
    }

    public void uploadCertificate(String uid, Uri fileUri, Callback<String> callback) {
        StorageReference ref = storage.getReference().child("ngo_certificates/" + uid + "_" + System.currentTimeMillis());

        ref.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onComplete(uri.toString(), null))
                        .addOnFailureListener(e -> callback.onComplete(null, e.getLocalizedMessage())))
                .addOnFailureListener(e -> callback.onComplete(null, e.getLocalizedMessage()));
    }

    public void saveUserProfile(String uid, String fullName, String email, String phone, String description, Double latitude, Double longitude, Callback<Boolean> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("full_name", fullName);
        data.put("email", email);
        data.put("phone", phone != null ? phone : "");
        data.put("description", description != null ? description : "");
        data.put("latitude", latitude != null ? latitude : 0.0);
        data.put("longitude", longitude != null ? longitude : 0.0);
        data.put("role", "USER");
        data.put("created_at", Timestamp.now());

        db.collection("users").document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getLocalizedMessage()));
    }

    public void saveNgoProfile(String uid, String name, String phone, String address, String licenseNumber, String description, String profilePhotoUrl, String certificateUrl, Callback<Boolean> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("organization_name", name);
        data.put("phone", phone);
        data.put("address", address);
        data.put("license_number", licenseNumber);
        data.put("certificate_url", certificateUrl);
        data.put("profile_photo_url", profilePhotoUrl);
        data.put("verification_status", "PENDING");
        data.put("created_at", Timestamp.now());
        data.put("role", "NGO");

        db.collection("ngo_profiles").document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e.getLocalizedMessage()));
    }

    public void fetchUserRole(String uid, Callback<String> callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        callback.onComplete(userDoc.getString("role") != null ? userDoc.getString("role") : "USER", null);
                    } else {
                        db.collection("ngo_profiles").document(uid).get()
                                .addOnSuccessListener(ngoDoc -> {
                                    if (ngoDoc.exists()) {
                                        callback.onComplete(ngoDoc.getString("role") != null ? ngoDoc.getString("role") : "NGO", null);
                                    } else {
                                        callback.onComplete(null, null);
                                    }
                                })
                                .addOnFailureListener(e -> callback.onComplete(null, e.getLocalizedMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(null, e.getLocalizedMessage()));
    }
}
