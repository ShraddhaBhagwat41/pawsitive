package com.pawsitive.app.staff;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffTaskRepository {

    public interface TaskStreamListener {
        void onUpdated(@NonNull List<StaffTask> tasks);
        void onError(@NonNull String errorMessage);
    }

    public interface CompletionListener {
        void onComplete(boolean success, @Nullable String errorMessage);
    }

    private final FirebaseFirestore db;
    private ListenerRegistration taskListener;

    public StaffTaskRepository(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    public void listenAssignedTasks(@NonNull String staffId, @NonNull TaskStreamListener listener) {
        stop();
        taskListener = db.collection("reports")
                .whereEqualTo("assignedTo.staffId", staffId)
                .orderBy("assignedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage() != null ? error.getMessage() : "Failed to load tasks");
                        return;
                    }
                    if (snapshots == null) {
                        listener.onUpdated(new ArrayList<>());
                        return;
                    }

                    List<StaffTask> tasks = new ArrayList<>();
                    snapshots.getDocuments().forEach(doc -> {
                        StaffTask task = StaffTask.fromDocument(doc);
                        if (task != null) tasks.add(task);
                    });
                    listener.onUpdated(tasks);
                });
    }

    public void updateTaskStatus(
            @NonNull String reportId,
            @NonNull String status,
            @Nullable String staffId,
            @NonNull CompletionListener listener
    ) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", status);
        update.put("updatedAt", FieldValue.serverTimestamp());

        if (StaffTask.STATUS_ON_THE_WAY.equals(status)) {
            update.put("startedAt", FieldValue.serverTimestamp());
        }

        if (StaffTask.STATUS_COMPLETED.equals(status)) {
            update.put("completedAt", FieldValue.serverTimestamp());
            if (staffId != null) {
                update.put("completedBy", staffId);
            }
        }

        db.collection("reports").document(reportId)
                .update(update)
                .addOnSuccessListener(unused -> listener.onComplete(true, null))
                .addOnFailureListener(e -> listener.onComplete(false, e.getMessage()));
    }

    public void updateStaffAvailability(@NonNull String staffId, boolean isAvailable) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("isAvailable", isAvailable);
        payload.put("lastActiveAt", FieldValue.serverTimestamp());

        db.collection("staff").document(staffId).set(payload, SetOptions.merge());
        db.collection("ngo_staff").document(staffId).set(payload, SetOptions.merge());
    }

    public void pushCompletionNotification(@NonNull StaffTask task, @NonNull String staffName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "TASK_COMPLETED");
        notification.put("reportId", task.reportId);
        notification.put("title", "Rescue completed");
        notification.put("body", staffName + " marked a " + task.animalType + " rescue as completed.");
        notification.put("createdAt", FieldValue.serverTimestamp());

        if (!task.assignedNgoId.isEmpty()) {
            notification.put("targetUserId", task.assignedNgoId);
            db.collection("notifications").add(notification);
        }

        if (!task.reporterUid.isEmpty()) {
            Map<String, Object> userNotification = new HashMap<>(notification);
            userNotification.put("targetUserId", task.reporterUid);
            db.collection("notifications").add(userNotification);
        }
    }

    public void stop() {
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }
    }
}

