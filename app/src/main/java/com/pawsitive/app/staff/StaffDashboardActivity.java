package com.pawsitive.app.staff;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pawsitive.app.LoginActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.network.NetworkManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StaffDashboardActivity extends AppCompatActivity implements StaffTaskAdapter.TaskActionListener {

    private TextView tvStaffName;
    private TextView tvStatusLabel;
    private TextView tvNoTasks;

    private TextView tvActiveAnimal;
    private TextView tvActiveCondition;
    private TextView tvActiveDistance;
    private TextView tvActiveAddress;
    private TextView tvActiveAssignedAt;

    private TextView tvMapCurrent;
    private TextView tvMapIncident;
    private TextView tvMapEta;

    private TextView tvTotalCompleted;
    private TextView tvTasksToday;
    private TextView tvAvgResponse;

    private View activeTaskCard;
    private View availabilityDot;

    private View btnActiveNavigate;
    private View btnActiveCall;
    private View btnActiveOnTheWay;
    private View btnActiveCompleted;
    private View btnMapOpen;

    private ImageView ivLogout;
    private ImageView ivNotification;

    private RecyclerView rvAssignedTasks;

    private StaffTaskAdapter adapter;
    private StaffTaskRepository repository;
    private FirebaseFirestore db;
    private NetworkManager networkManager;

    private String staffUid;
    private String staffName = "Staff";
    private String pendingReportId;

    @Nullable
    private StaffTask currentActiveTask;
    @Nullable
    private Location currentLocation;

    private final List<StaffTask> latestTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        db = FirebaseFirestore.getInstance();
        repository = new StaffTaskRepository(db);
        networkManager = new NetworkManager(this);
        staffUid = FirebaseAuth.getInstance().getUid();

        if (staffUid == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        bindViews();
        setupRecycler();
        setupActions();
        loadStaffProfile();
        handleIntent(getIntent());
        startTaskListener();
        loadCurrentLocation();
    }

    private void bindViews() {
        tvStaffName = findViewById(R.id.tvStaffName);
        tvStatusLabel = findViewById(R.id.tvStaffStatusLabel);
        tvNoTasks = findViewById(R.id.tvNoStaffTasks);

        tvActiveAnimal = findViewById(R.id.tvActiveAnimalType);
        tvActiveCondition = findViewById(R.id.tvActiveCondition);
        tvActiveDistance = findViewById(R.id.tvActiveDistance);
        tvActiveAddress = findViewById(R.id.tvActiveAddress);
        tvActiveAssignedAt = findViewById(R.id.tvActiveAssignedAt);

        tvMapCurrent = findViewById(R.id.tvMapCurrentLocation);
        tvMapIncident = findViewById(R.id.tvMapIncidentLocation);
        tvMapEta = findViewById(R.id.tvMapEta);

        tvTotalCompleted = findViewById(R.id.tvSummaryTotalCompleted);
        tvTasksToday = findViewById(R.id.tvSummaryTasksToday);
        tvAvgResponse = findViewById(R.id.tvSummaryAvgResponse);

        activeTaskCard = findViewById(R.id.cardActiveTask);
        availabilityDot = findViewById(R.id.viewAvailabilityDot);

        btnActiveNavigate = findViewById(R.id.btnActiveNavigate);
        btnActiveCall = findViewById(R.id.btnActiveCallNgo);
        btnActiveOnTheWay = findViewById(R.id.btnActiveOnTheWay);
        btnActiveCompleted = findViewById(R.id.btnActiveCompleted);
        btnMapOpen = findViewById(R.id.btnOpenMapSection);

        ivLogout = findViewById(R.id.ivStaffLogout);
        ivNotification = findViewById(R.id.ivStaffNotifications);

        rvAssignedTasks = findViewById(R.id.rvStaffTasks);
    }

    private void setupRecycler() {
        adapter = new StaffTaskAdapter(this);
        rvAssignedTasks.setLayoutManager(new LinearLayoutManager(this));
        rvAssignedTasks.setAdapter(adapter);
    }

    private void setupActions() {
        ivLogout.setOnClickListener(v -> logout());

        ivNotification.setOnClickListener(v -> {
            if (pendingReportId != null && !pendingReportId.isEmpty()) {
                Toast.makeText(this, "Focused on latest assignment", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show();
            }
        });

        btnActiveNavigate.setOnClickListener(v -> {
            if (currentActiveTask != null) openNavigation(currentActiveTask);
        });

        btnMapOpen.setOnClickListener(v -> {
            if (currentActiveTask != null) openNavigation(currentActiveTask);
            else Toast.makeText(this, "No active incident to open", Toast.LENGTH_SHORT).show();
        });

        btnActiveCall.setOnClickListener(v -> {
            if (currentActiveTask != null) callNgo(currentActiveTask);
        });

        btnActiveOnTheWay.setOnClickListener(v -> {
            if (currentActiveTask != null) {
                updateTaskStatus(currentActiveTask, StaffTask.STATUS_ON_THE_WAY, "Marked On the Way");
            }
        });

        btnActiveCompleted.setOnClickListener(v -> {
            if (currentActiveTask != null) {
                updateTaskStatus(currentActiveTask, StaffTask.STATUS_COMPLETED, "Marked Completed");
            }
        });
    }

    private void startTaskListener() {
        repository.listenAssignedTasks(staffUid, new StaffTaskRepository.TaskStreamListener() {
            @Override
            public void onUpdated(@NonNull List<StaffTask> tasks) {
                latestTasks.clear();
                latestTasks.addAll(tasks);

                StaffTask pinned = findByReportId(tasks, pendingReportId);
                currentActiveTask = pinned != null ? pinned : choosePriorityTask(tasks);

                renderTaskCards(tasks);
                renderActiveTask(currentActiveTask);
                renderMapSection(currentActiveTask);
                renderSummary(tasks);
                renderAvailability(tasks);
                tvNoTasks.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);

                if (pinned != null) {
                    pulseActiveCard();
                    pendingReportId = null;
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Toast.makeText(StaffDashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderTaskCards(@NonNull List<StaffTask> tasks) {
        List<StaffTaskAdapter.TaskItem> items = new ArrayList<>();
        for (StaffTask task : tasks) {
            items.add(new StaffTaskAdapter.TaskItem(
                    task,
                    formatDistance(task),
                    formatAssignedTime(task.assignedAt)
            ));
        }
        adapter.submitList(items);
    }

    private void renderActiveTask(@Nullable StaffTask task) {
        if (task == null) {
            activeTaskCard.setVisibility(View.GONE);
            return;
        }

        activeTaskCard.setVisibility(View.VISIBLE);
        tvActiveAnimal.setText(task.animalType);
        tvActiveCondition.setText("Condition: " + task.condition);
        tvActiveDistance.setText(formatDistance(task));
        tvActiveAddress.setText(task.address);
        tvActiveAssignedAt.setText(formatAssignedTime(task.assignedAt));

        boolean completed = task.isCompleted();
        btnActiveOnTheWay.setEnabled(!completed && !StaffTask.STATUS_ON_THE_WAY.equals(task.status));
        btnActiveCompleted.setEnabled(!completed);
        btnActiveOnTheWay.setAlpha(btnActiveOnTheWay.isEnabled() ? 1f : 0.6f);
        btnActiveCompleted.setAlpha(btnActiveCompleted.isEnabled() ? 1f : 0.6f);
    }

    private void renderMapSection(@Nullable StaffTask task) {
        if (currentLocation != null) {
            tvMapCurrent.setText(String.format(Locale.getDefault(), "Current: %.5f, %.5f",
                    currentLocation.getLatitude(), currentLocation.getLongitude()));
        } else {
            tvMapCurrent.setText("Current: waiting for GPS");
        }

        if (task == null) {
            tvMapIncident.setText("Incident: no active task");
            tvMapEta.setText("ETA: --");
            return;
        }

        tvMapIncident.setText(String.format(Locale.getDefault(), "Incident: %.5f, %.5f", task.lat, task.lng));
        tvMapEta.setText("ETA: " + estimateEta(task));
    }

    private void renderSummary(@NonNull List<StaffTask> tasks) {
        StaffPerformanceSummary summary = StaffPerformanceSummary.fromTasks(tasks);
        tvTotalCompleted.setText(String.valueOf(summary.totalCompleted));
        tvTasksToday.setText(String.valueOf(summary.tasksToday));
        tvAvgResponse.setText(summary.avgResponseMinutes + " min");
    }

    private void renderAvailability(@NonNull List<StaffTask> tasks) {
        boolean hasActive = false;
        for (StaffTask task : tasks) {
            if (!task.isCompleted()) {
                hasActive = true;
                break;
            }
        }

        boolean isAvailable = !hasActive;
        tvStatusLabel.setText(isAvailable ? "Available" : "Busy");
        availabilityDot.setBackgroundResource(isAvailable ? R.drawable.bg_status_active : R.drawable.bg_status_inactive);
        int statusColor = isAvailable ? R.color.green_success : R.color.red_primary;
        tvStatusLabel.setTextColor(ContextCompat.getColor(this, statusColor));

        repository.updateStaffAvailability(staffUid, isAvailable);
    }

    private void updateTaskStatus(@NonNull StaffTask task, @NonNull String status, @NonNull String successMessage) {
        repository.updateTaskStatus(task.reportId, status, staffUid, (success, errorMessage) -> {
            if (!success) {
                Toast.makeText(this, errorMessage != null ? errorMessage : "Failed to update task", Toast.LENGTH_SHORT).show();
                return;
            }

            if (StaffTask.STATUS_COMPLETED.equals(status)) {
                repository.pushCompletionNotification(task, staffName);
            }
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onOpenMap(@NonNull StaffTaskAdapter.TaskItem item) {
        currentActiveTask = item.task;
        renderActiveTask(currentActiveTask);
        renderMapSection(currentActiveTask);
        openNavigation(item.task);
    }

    @Override
    public void onCallNgo(@NonNull StaffTaskAdapter.TaskItem item) {
        currentActiveTask = item.task;
        renderActiveTask(currentActiveTask);
        callNgo(item.task);
    }

    @Override
    public void onMarkOnTheWay(@NonNull StaffTaskAdapter.TaskItem item) {
        currentActiveTask = item.task;
        renderActiveTask(currentActiveTask);
        updateTaskStatus(item.task, StaffTask.STATUS_ON_THE_WAY, "Marked On the Way");
    }

    @Override
    public void onMarkCompleted(@NonNull StaffTaskAdapter.TaskItem item) {
        currentActiveTask = item.task;
        renderActiveTask(currentActiveTask);
        updateTaskStatus(item.task, StaffTask.STATUS_COMPLETED, "Marked Completed");
    }

    @Override
    public void onTaskSelected(@NonNull StaffTaskAdapter.TaskItem item) {
        currentActiveTask = item.task;
        renderActiveTask(currentActiveTask);
        renderMapSection(currentActiveTask);
    }

    private void openNavigation(@NonNull StaffTask task) {
        String routeUrl;
        if (currentLocation != null) {
            routeUrl = "https://www.google.com/maps/dir/?api=1&origin="
                    + currentLocation.getLatitude() + "," + currentLocation.getLongitude()
                    + "&destination=" + task.lat + "," + task.lng + "&travelmode=driving";
        } else {
            routeUrl = "https://www.google.com/maps/search/?api=1&query=" + task.lat + "," + task.lng;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(routeUrl));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(routeUrl)));
        }
    }

    private void callNgo(@NonNull StaffTask task) {
        String number = task.phoneForNgoCall().trim();
        if (number.isEmpty()) {
            Toast.makeText(this, "NGO contact not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        startActivity(dialIntent);
    }

    private void loadStaffProfile() {
        db.collection("staff").document(staffUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        applyStaffProfile(doc);
                    } else {
                        db.collection("ngo_staff").document(staffUid).get().addOnSuccessListener(this::applyStaffProfile);
                    }
                });
    }

    private void applyStaffProfile(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            tvStaffName.setText("Hi, Staff");
            return;
        }

        String name = doc.getString("name");
        if (name == null || name.trim().isEmpty()) {
            name = doc.getString("full_name");
        }
        staffName = name == null || name.trim().isEmpty() ? "Staff" : name.trim();
        tvStaffName.setText("Hi, " + staffName);
    }

    @SuppressLint("MissingPermission")
    private void loadCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            tvMapCurrent.setText("Current: location permission required");
            return;
        }

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) return;
            currentLocation = location;
            renderTaskCards(latestTasks);
            renderMapSection(currentActiveTask);
            if (currentActiveTask != null) {
                renderActiveTask(currentActiveTask);
            }
        });
    }

    private void handleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        pendingReportId = intent.getStringExtra("reportId");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);

        StaffTask pinned = findByReportId(latestTasks, pendingReportId);
        if (pinned != null) {
            currentActiveTask = pinned;
            renderActiveTask(pinned);
            renderMapSection(pinned);
            pulseActiveCard();
            pendingReportId = null;
        }
    }

    private void pulseActiveCard() {
        if (activeTaskCard.getVisibility() != View.VISIBLE) return;
        AlphaAnimation pulse = new AlphaAnimation(0.4f, 1f);
        pulse.setDuration(220);
        pulse.setRepeatMode(AlphaAnimation.REVERSE);
        pulse.setRepeatCount(3);
        activeTaskCard.startAnimation(pulse);
    }

    @Nullable
    private StaffTask choosePriorityTask(@NonNull List<StaffTask> tasks) {
        for (StaffTask task : tasks) {
            if (!task.isCompleted()) {
                return task;
            }
        }
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    @Nullable
    private StaffTask findByReportId(@NonNull List<StaffTask> tasks, @Nullable String reportId) {
        if (reportId == null || reportId.trim().isEmpty()) return null;
        for (StaffTask task : tasks) {
            if (reportId.equals(task.reportId)) {
                return task;
            }
        }
        return null;
    }

    private String formatDistance(@NonNull StaffTask task) {
        if (currentLocation == null) return "Distance: --";
        float[] results = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), task.lat, task.lng, results);
        return String.format(Locale.getDefault(), "Distance: %.1f km", results[0] / 1000f);
    }

    private String estimateEta(@NonNull StaffTask task) {
        if (currentLocation == null) return "--";
        float[] results = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), task.lat, task.lng, results);
        int mins = Math.max(1, Math.round((results[0] / 1000f) * 2.5f));
        return mins + " min";
    }

    private String formatAssignedTime(@Nullable Date assignedAt) {
        if (assignedAt == null) return "Assigned: --";
        CharSequence relative = DateUtils.getRelativeTimeSpanString(
                assignedAt.getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
        return "Assigned: " + relative;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repository.stop();
    }

    private void logout() {
        networkManager.clearAuth();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

