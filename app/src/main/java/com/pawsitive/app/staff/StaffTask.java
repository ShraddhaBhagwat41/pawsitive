package com.pawsitive.app.staff;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.Map;

public class StaffTask {
    public static final String STATUS_ASSIGNED = "assigned";
    public static final String STATUS_ON_THE_WAY = "on_the_way";
    public static final String STATUS_COMPLETED = "completed";

    public final String reportId;
    public final String animalType;
    public final String condition;
    public final String address;
    public final String reporterPhone;
    public final String ngoPhone;
    public final String status;
    public final double lat;
    public final double lng;
    public final Date assignedAt;
    public final Date completedAt;
    public final String assignedNgoId;
    public final String reporterUid;

    public StaffTask(
            @NonNull String reportId,
            @NonNull String animalType,
            @NonNull String condition,
            @NonNull String address,
            @NonNull String reporterPhone,
            @NonNull String ngoPhone,
            @NonNull String status,
            double lat,
            double lng,
            @Nullable Date assignedAt,
            @Nullable Date completedAt,
            @NonNull String assignedNgoId,
            @NonNull String reporterUid
    ) {
        this.reportId = reportId;
        this.animalType = animalType;
        this.condition = condition;
        this.address = address;
        this.reporterPhone = reporterPhone;
        this.ngoPhone = ngoPhone;
        this.status = status;
        this.lat = lat;
        this.lng = lng;
        this.assignedAt = assignedAt;
        this.completedAt = completedAt;
        this.assignedNgoId = assignedNgoId;
        this.reporterUid = reporterUid;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static StaffTask fromDocument(@NonNull DocumentSnapshot doc) {
        Map<String, Object> location = (Map<String, Object>) doc.get("location");
        if (location == null) return null;

        Double lat = toDouble(location.get("lat"));
        Double lng = toDouble(location.get("lng"));
        if (lat == null || lng == null) return null;

        String animalType = safe(doc.getString("animalType"), "Animal");
        String condition = safe(doc.getString("condition"), "Unknown");
        String address = safe((String) location.get("address"), "Location unavailable");
        String reporterPhone = safe(doc.getString("reporterPhone"), "");
        String ngoPhone = safe(doc.getString("ngoPhone"), "");
        String status = normalizeStatus(doc.getString("status"));

        Date assignedAt = toDate(doc.get("assignedAt"));
        Date completedAt = toDate(doc.get("completedAt"));
        String assignedNgoId = safe(doc.getString("assignedNgoId"), "");
        String reporterUid = safe(doc.getString("reporterUid"), "");

        return new StaffTask(
                doc.getId(),
                animalType,
                condition,
                address,
                reporterPhone,
                ngoPhone,
                status,
                lat,
                lng,
                assignedAt,
                completedAt,
                assignedNgoId,
                reporterUid
        );
    }

    @NonNull
    public static String normalizeStatus(@Nullable String raw) {
        if (raw == null) return STATUS_ASSIGNED;
        String normalized = raw.trim().toLowerCase();
        if ("on the way".equals(normalized) || "ontheway".equals(normalized) || "in_progress".equals(normalized)) {
            return STATUS_ON_THE_WAY;
        }
        if (STATUS_COMPLETED.equals(normalized)) {
            return STATUS_COMPLETED;
        }
        return STATUS_ASSIGNED;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    @NonNull
    public String phoneForNgoCall() {
        if (!ngoPhone.trim().isEmpty()) return ngoPhone;
        return reporterPhone;
    }

    @Nullable
    private static Double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    private static Date toDate(Object value) {
        if (value instanceof Date) return (Date) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toDate();
        return null;
    }

    @NonNull
    private static String safe(@Nullable String value, @NonNull String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}

