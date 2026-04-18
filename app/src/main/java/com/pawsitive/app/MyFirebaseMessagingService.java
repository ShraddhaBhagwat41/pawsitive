package com.pawsitive.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pawsitive.app.ngo.NGOHomeActivity;
import com.pawsitive.app.staff.StaffDashboardActivity;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = "Pawsitive Alert";
        String body = "A new incident has been reported near your location";

        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        sendNotification(title, body, remoteMessage.getData());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Save token to Firestore so Cloud Functions can push to this device
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("fcmToken", token);
            // NGO devices must have token in ngo_profiles for nearby incident notifications.
            db.collection("ngo_profiles").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to NGO profile"))
                    .addOnFailureListener(e -> Log.w(TAG, "Unable to save token to ngo_profiles", e));

            db.collection("ngos").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to ngos"))
                    .addOnFailureListener(e -> Log.w(TAG, "Unable to save token to ngos", e));

            db.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to user profile"))
                    .addOnFailureListener(userErr -> Log.w(TAG, "Unable to save token to profile", userErr));

            db.collection("ngo_staff").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to ngo_staff"))
                    .addOnFailureListener(e -> Log.w(TAG, "Unable to save token to ngo_staff", e));

            db.collection("staff").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to staff"))
                    .addOnFailureListener(e -> Log.w(TAG, "Unable to save token to staff", e));
        }
    }

    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        String type = data != null ? data.get("type") : null;
        String reportId = data != null ? data.get("reportId") : null;

        Intent intent;
        if ("ASSIGNED".equalsIgnoreCase(type)) {
            intent = new Intent(this, StaffDashboardActivity.class);
        } else {
            intent = new Intent(this, NGOHomeActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                reportId != null ? reportId.hashCode() : 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Uri chosenSoundUri = defaultSoundUri;
        if ("ASSIGNED".equalsIgnoreCase(type)) {
            chosenSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title != null ? title : "Pawsitive Alert")
                        .setContentText(messageBody)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setSound(chosenSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Rescue Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setDescription("Nearby rescue incident alerts");
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(reportId != null ? reportId.hashCode() : 0, notificationBuilder.build());
    }
}
