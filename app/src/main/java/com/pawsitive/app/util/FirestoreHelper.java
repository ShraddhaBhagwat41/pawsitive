package com.pawsitive.app.util;

import android.net.Uri;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class FirestoreHelper {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    public interface UploadCallback {
        void onSuccess(String url);
        void onFailure(String error);
    }

    public void uploadProfileImage(String uid, Uri imageUri, boolean isNgo, UploadCallback callback) {
        if (imageUri == null) {
            // No image selected; continue without a URL
            callback.onSuccess(null);
            return;
        }

        String path = isNgo ? "ngo_profiles/" + uid + ".jpg" : "users/" + uid + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e -> {
                                    // Upload succeeded but URL fetch failed (rules, etc.)
                                    // Continue without a photo URL so profile can still be saved
                                    callback.onSuccess(null);
                                })
                )
                .addOnFailureListener(e -> {
                    // Upload failed (network/rules). Still let profile save without photo.
                    callback.onSuccess(null);
                });
    }
}
