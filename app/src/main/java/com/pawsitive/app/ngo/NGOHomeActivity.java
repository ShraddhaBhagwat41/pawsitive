package com.pawsitive.app.ngo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pawsitive.app.LoginActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NGOHomeActivity extends AppCompatActivity implements NgoReportAdapter.ReportActionListener {

    private static final String TAG = "NGOHomeActivity";
    private static final int REQUEST_POST_NOTIFICATIONS = 1401;
    private static final float NEARBY_RADIUS_METERS = 10000f;

    private TextView tvVerificationStatus, tvStatusMessage, tvRejectionReason, tvNgoName, tvNoReports;
    private ImageView ivStatusIcon, ivLogout;
    private Button btnPostAnimal, btnViewAnimals, btnManageProfile, btnAddStaff;
    private ProgressBar progressBar;
    private RecyclerView rvNgoReports;

    private NetworkManager networkManager;
    private FirebaseFirestore db;

    private NgoReportAdapter reportAdapter;
    private ListenerRegistration reportListener;

    private Double ngoLat;
    private Double ngoLng;
    private String ngoUid;
    private final List<StaffMember> ngoStaffMembers = new ArrayList<>();

    private static class StaffMember {
        final String id;
        final String name;
        final String phone;
        final boolean isAvailable;

        StaffMember(String id, String name, String phone, boolean isAvailable) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.isAvailable = isAvailable;
        }

        @NonNull
        @Override
        public String toString() {
            String availability = isAvailable ? "Available" : "Busy";
            return name + " • " + (phone != null ? phone : "No phone") + " • " + availability;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_home);

        networkManager = new NetworkManager(this);
        db = FirebaseFirestore.getInstance();

        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvNgoName = findViewById(R.id.tvNgoName);
        ivLogout = findViewById(R.id.ivLogout);
        progressBar = findViewById(R.id.progressBar);
        btnPostAnimal = findViewById(R.id.btnPostAnimal);
        btnViewAnimals = findViewById(R.id.btnViewAnimals);
        btnManageProfile = findViewById(R.id.btnManageProfile);
        btnAddStaff = findViewById(R.id.btnAddStaff);
        tvNoReports = findViewById(R.id.tvNoReports);
        rvNgoReports = findViewById(R.id.rvNgoReports);

        reportAdapter = new NgoReportAdapter(this);
        rvNgoReports.setLayoutManager(new LinearLayoutManager(this));
        rvNgoReports.setAdapter(reportAdapter);

        ivLogout.setOnClickListener(v -> logout());

        btnPostAnimal.setOnClickListener(v -> Toast.makeText(this, "Post Animal - Coming Soon", Toast.LENGTH_SHORT).show());
        btnViewAnimals.setOnClickListener(v -> Toast.makeText(this, "View Animals - Coming Soon", Toast.LENGTH_SHORT).show());
        btnManageProfile.setOnClickListener(v -> Toast.makeText(this, "Manage Profile - Coming Soon", Toast.LENGTH_SHORT).show());
        btnAddStaff.setOnClickListener(v -> startActivity(new Intent(NGOHomeActivity.this, StaffListActivity.class)));

        ngoUid = FirebaseAuth.getInstance().getUid();
        setupNotificationsForNgo();
        loadNgoStatus();
        loadNgoCoordinatesAndStartListener();
        loadNgoStaff();
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) return;
        String reportId = intent.getStringExtra("reportId");
        if (reportId == null || reportId.trim().isEmpty()) return;

        db.collection("reports").document(reportId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Report no longer available", Toast.LENGTH_SHORT).show();
                return;
            }
            NgoReportAdapter.ReportItem item = mapReport(doc, ngoLat, ngoLng);
            if (item != null) {
                showReportDetailsDialog(item);
            }
        });
    }

    private void setupNotificationsForNgo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            return;
        }
        fetchAndSaveFcmToken();
    }

    private void fetchAndSaveFcmToken() {
        if (ngoUid == null) return;

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token == null || token.trim().isEmpty()) return;

            Map<String, Object> update = new HashMap<>();
            update.put("fcmToken", token);

            db.collection("ngo_profiles").document(ngoUid)
                    .set(update, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> Log.d(TAG, "Token saved to ngo_profiles"))
                    .addOnFailureListener(e -> Log.w(TAG, "Unable to save token to ngo_profiles", e));

            db.collection("ngos").document(ngoUid)
                    .set(update, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(unused -> Log.d(TAG, "Token saved to ngos"))
                    .addOnFailureListener(e -> Log.w(TAG, "Unable to save token to ngos", e));
        }).addOnFailureListener(e -> Log.w(TAG, "Unable to fetch FCM token", e));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            fetchAndSaveFcmToken();
        }
    }

    private void loadNgoStatus() {
        progressBar.setVisibility(View.VISIBLE);

        networkManager.getNGOProfile(new NetworkManager.ApiCallback<ApiService.NGOResponse>() {
            @Override
            public void onSuccess(ApiService.NGOResponse ngo) {
                progressBar.setVisibility(View.GONE);
                if (ngo == null || ngo.data == null) {
                    Toast.makeText(NGOHomeActivity.this, "No NGO profile found", Toast.LENGTH_SHORT).show();
                    return;
                }

                ApiService.NGOResponse.NGOData data = ngo.data;
                tvNgoName.setText((data.organization_name != null ? data.organization_name : "NGO") + " Dashboard");

                String status = data.verification_status != null ? data.verification_status : "PENDING";
                tvVerificationStatus.setText(status);

                switch (status) {
                    case "PENDING":
                        tvStatusMessage.setText("Your application is awaiting admin verification. This may take 1-2 business days.");
                        ivStatusIcon.setImageResource(R.drawable.ic_clock_blue);
                        tvVerificationStatus.setTextColor(getResources().getColor(R.color.blue_pending));
                        enableButtons(false);
                        break;

                    case "VERIFIED":
                        tvStatusMessage.setText("✓ Your NGO has been verified! You can now start using all features.");
                        ivStatusIcon.setImageResource(R.drawable.ic_check_green);
                        tvVerificationStatus.setTextColor(getResources().getColor(R.color.green_success));
                        enableButtons(true);
                        break;

                    case "REJECTED":
                        String reason = data.rejection_reason;
                        tvStatusMessage.setText("✗ Your application has been rejected.");
                        tvRejectionReason.setVisibility(View.VISIBLE);
                        tvRejectionReason.setText("Reason: " + (reason != null && !reason.isEmpty() ? reason : "Not specified"));
                        ivStatusIcon.setImageResource(R.drawable.ic_warning);
                        tvVerificationStatus.setTextColor(getResources().getColor(R.color.red_primary));
                        enableButtons(false);
                        break;
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NGOHomeActivity.this, "Error loading NGO data: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNgoCoordinatesAndStartListener() {
        if (ngoUid == null) return;

        db.collection("ngo_profiles").document(ngoUid).get().addOnSuccessListener(doc -> {
            ngoLat = doc.getDouble("latitude");
            ngoLng = doc.getDouble("longitude");

            Map<String, Object> location = new HashMap<>();
            if (ngoLat != null && ngoLng != null) {
                Map<String, Object> loc = new HashMap<>();
                loc.put("lat", ngoLat);
                loc.put("lng", ngoLng);
                location.put("location", loc);
                db.collection("ngos").document(ngoUid).set(location, com.google.firebase.firestore.SetOptions.merge());
            }
            startReportListener();
        }).addOnFailureListener(e -> startReportListener());
    }

    private void loadNgoStaff() {
        if (ngoUid == null) return;

        db.collection("ngo_staff")
                .whereEqualTo("ngo_id", ngoUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Failed to load staff list", error);
                        return;
                    }
                    if (snapshots == null) return;

                    List<StaffMember> updated = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String status = safeString(doc.getString("status"), "active");
                        boolean isAvailable = "active".equalsIgnoreCase(status);
                        String name = safeString(doc.getString("full_name"), "Staff");
                        String phone = doc.getString("phone");
                        updated.add(new StaffMember(doc.getId(), name, phone, isAvailable));
                    }

                    Collections.sort(updated, (a, b) -> {
                        if (a.isAvailable == b.isAvailable) {
                            return a.name.compareToIgnoreCase(b.name);
                        }
                        return a.isAvailable ? -1 : 1;
                    });

                    ngoStaffMembers.clear();
                    ngoStaffMembers.addAll(updated);
                });
    }

    private void startReportListener() {
        stopReportListener();

        reportListener = db.collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Report listener error", error);
                            return;
                        }
                        if (snapshots == null) {
                            return;
                        }

                        List<NgoReportAdapter.ReportItem> reportItems = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            NgoReportAdapter.ReportItem item = mapReport(doc, ngoLat, ngoLng);
                            if (item == null) continue;

                            String status = doc.getString("status");
                            String assignedNgoId = doc.getString("assignedNgoId");
                            boolean nearby = item.distanceMeters <= NEARBY_RADIUS_METERS;
                            boolean assigned = ngoUid != null && ngoUid.equals(assignedNgoId);
                            boolean pendingFlow = status == null
                                    || "pending".equalsIgnoreCase(status)
                                    || "accepted".equalsIgnoreCase(status)
                                    || "assigned".equalsIgnoreCase(status)
                                    || "completed".equalsIgnoreCase(status);

                            if ((nearby && pendingFlow) || assigned) {
                                reportItems.add(item);
                            }
                        }

                        reportAdapter.submitList(reportItems);
                        tvNoReports.setVisibility(reportItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private NgoReportAdapter.ReportItem mapReport(DocumentSnapshot doc, Double baseLat, Double baseLng) {
        Map<String, Object> loc = (Map<String, Object>) doc.get("location");
        if (loc == null) return null;

        Double lat = toDouble(loc.get("lat"));
        Double lng = toDouble(loc.get("lng"));
        if (lat == null || lng == null) return null;

        String animalType = safeString(doc.getString("animalType"), "Animal");
        String condition = safeString(doc.getString("condition"), "Unknown");
        String address = safeString((String) loc.get("address"), "");
        String description = safeString(doc.getString("description"), "No description");

        float distance = Float.MAX_VALUE;
        if (baseLat != null && baseLng != null) {
            float[] results = new float[1];
            Location.distanceBetween(baseLat, baseLng, lat, lng, results);
            distance = results[0];
        }

        Timestamp createdAtTs = doc.getTimestamp("createdAt");
        Date createdAt = createdAtTs != null ? createdAtTs.toDate() : null;
        boolean isNew = createdAt != null && (System.currentTimeMillis() - createdAt.getTime()) <= (5 * 60 * 1000);

        String status = safeString(doc.getString("status"), "pending").toLowerCase(Locale.ROOT);
        Map<String, Object> assignedTo = (Map<String, Object>) doc.get("assignedTo");
        String assignedStaffName = "";
        if (assignedTo != null) {
            Object name = assignedTo.get("name");
            if (name != null) {
                assignedStaffName = String.valueOf(name);
            }
        }
        String reporterPhone = safeString(doc.getString("reporterPhone"), "");
        String reporterName = safeString(doc.getString("reporterName"), "");

        return new NgoReportAdapter.ReportItem(
                doc.getId(),
                animalType,
                condition,
                address,
                description,
                lat,
                lng,
                distance,
                isNew,
                status,
                assignedStaffName,
                reporterPhone,
                reporterName
        );
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String safeString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Override
    public void onOpenDetails(@NonNull NgoReportAdapter.ReportItem item) {
        showReportDetailsDialog(item);
    }

    @Override
    public void onAccept(@NonNull NgoReportAdapter.ReportItem item) {
        if (ngoUid == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("status", "ACCEPTED");
        update.put("assignedNgoId", ngoUid);
        update.put("acceptedAt", new Date());

        db.collection("reports").document(item.id)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Request accepted. Assign a staff member.", Toast.LENGTH_SHORT).show();
                    onAssignStaff(item);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAssignStaff(@NonNull NgoReportAdapter.ReportItem item) {
        if (ngoUid == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ngoStaffMembers.isEmpty()) {
            Toast.makeText(this, "No staff found. Add staff first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Spinner spinner = new Spinner(this);
        List<StaffMember> available = new ArrayList<>();
        for (StaffMember s : ngoStaffMembers) {
            if (s.isAvailable) {
                available.add(s);
            }
        }
        if (available.isEmpty()) {
            available.addAll(ngoStaffMembers);
        }

        ArrayAdapter<StaffMember> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, available);
        spinner.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Assign Staff")
                .setMessage("Select staff from dropdown")
                .setView(spinner)
                .setPositiveButton("Assign", (dialog, which) -> {
                    StaffMember selected = (StaffMember) spinner.getSelectedItem();
                    if (selected == null) {
                        Toast.makeText(this, "Select a staff member", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    assignReportToStaff(item, selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onOpenMap(@NonNull NgoReportAdapter.ReportItem item) {
        Uri gmmIntentUri = Uri.parse("geo:" + item.lat + "," + item.lng + "?q=" + item.lat + "," + item.lng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
            return;
        }

        Intent fallback = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + item.lat + "," + item.lng));
        startActivity(fallback);
    }

    @Override
    public void onCallReporter(@NonNull NgoReportAdapter.ReportItem item) {
        if (item.reporterPhone == null || item.reporterPhone.trim().isEmpty()) {
            Toast.makeText(this, "Reporter phone not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.reporterPhone.trim()));
        startActivity(dialIntent);
    }

    private void assignReportToStaff(@NonNull NgoReportAdapter.ReportItem item, @NonNull StaffMember staff) {
        Map<String, Object> assignedTo = new HashMap<>();
        assignedTo.put("staffId", staff.id);
        assignedTo.put("name", staff.name);
        assignedTo.put("phone", staff.phone != null ? staff.phone : "");

        Map<String, Object> update = new HashMap<>();
        update.put("status", "assigned");
        update.put("assignedTo", assignedTo);
        update.put("assignedAt", new Date());
        update.put("assignedNgoId", ngoUid);

        db.collection("reports").document(item.id)
                .update(update)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Assigned to " + staff.name, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Assignment failed", e);
                    Toast.makeText(this, "Failed to assign staff", Toast.LENGTH_SHORT).show();
                });
    }

    private void showReportDetailsDialog(NgoReportAdapter.ReportItem item) {
        String details = "Animal: " + item.animalType + "\n"
                + "Condition: " + item.condition + "\n"
                + "Location: " + item.address + "\n\n"
                + item.description;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Report Details")
                .setMessage(details)
                .setNegativeButton("Close", null);

        if ("pending".equalsIgnoreCase(item.status)) {
            builder.setPositiveButton("Accept Request", (dialog, which) -> onAccept(item));
        } else if ("accepted".equalsIgnoreCase(item.status)) {
            builder.setPositiveButton("Assign Staff", (dialog, which) -> onAssignStaff(item));
        }

        builder.show();
    }

    private void enableButtons(boolean enabled) {
        btnPostAnimal.setEnabled(enabled);
        btnViewAnimals.setEnabled(enabled);
        btnManageProfile.setEnabled(enabled);
        btnAddStaff.setEnabled(enabled);

        if (!enabled) {
            btnPostAnimal.setAlpha(0.5f);
            btnViewAnimals.setAlpha(0.5f);
            btnManageProfile.setAlpha(0.5f);
            btnAddStaff.setAlpha(0.5f);
        } else {
            btnPostAnimal.setAlpha(1f);
            btnViewAnimals.setAlpha(1f);
            btnManageProfile.setAlpha(1f);
            btnAddStaff.setAlpha(1f);
        }
    }

    private void stopReportListener() {
        if (reportListener != null) {
            reportListener.remove();
            reportListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopReportListener();
    }

    private void logout() {
        networkManager.clearAuth();
        Intent intent = new Intent(NGOHomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
