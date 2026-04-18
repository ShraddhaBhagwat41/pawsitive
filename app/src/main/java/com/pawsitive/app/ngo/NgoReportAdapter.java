package com.pawsitive.app.ngo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NgoReportAdapter extends RecyclerView.Adapter<NgoReportAdapter.ReportVH> {

    public interface ReportActionListener {
        void onOpenDetails(@NonNull ReportItem item);
        void onAccept(@NonNull ReportItem item);
        void onAssignStaff(@NonNull ReportItem item);
        void onOpenMap(@NonNull ReportItem item);
        void onCallReporter(@NonNull ReportItem item);
    }

    public static class ReportItem {
        public final String id;
        public final String animalType;
        public final String condition;
        public final String address;
        public final String description;
        public final double lat;
        public final double lng;
        public final float distanceMeters;
        public final boolean isNew;
        public final String status;
        public final String assignedStaffName;
        public final String reporterPhone;
        public final String reporterName;

        public ReportItem(String id, String animalType, String condition, String address,
                          String description, double lat, double lng, float distanceMeters, boolean isNew,
                          String status, String assignedStaffName, String reporterPhone, String reporterName) {
            this.id = id;
            this.animalType = animalType;
            this.condition = condition;
            this.address = address;
            this.description = description;
            this.lat = lat;
            this.lng = lng;
            this.distanceMeters = distanceMeters;
            this.isNew = isNew;
            this.status = status;
            this.assignedStaffName = assignedStaffName;
            this.reporterPhone = reporterPhone;
            this.reporterName = reporterName;
        }
    }

    private final List<ReportItem> items = new ArrayList<>();
    private final ReportActionListener listener;

    public NgoReportAdapter(@NonNull ReportActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<ReportItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ngo_report, parent, false);
        return new ReportVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportVH holder, int position) {
        ReportItem item = items.get(position);
        holder.tvAnimalType.setText(item.animalType);
        holder.tvCondition.setText("Condition: " + item.condition);
        holder.tvDistance.setText(formatDistance(item.distanceMeters));
        holder.tvAddress.setText(item.address != null && !item.address.isEmpty() ? item.address : "Location unavailable");
        holder.tvNewBadge.setVisibility(item.isNew ? View.VISIBLE : View.GONE);
        if (item.reporterPhone != null && !item.reporterPhone.isEmpty()) {
            holder.tvReporterContact.setVisibility(View.VISIBLE);
            String name = item.reporterName != null && !item.reporterName.isEmpty() ? item.reporterName : "Reporter";
            holder.tvReporterContact.setText("Reporter: " + name + " • " + item.reporterPhone);
        } else {
            holder.tvReporterContact.setVisibility(View.GONE);
        }

        String normalizedStatus = item.status != null ? item.status.toLowerCase(Locale.ROOT) : "pending";
        if ("completed".equals(normalizedStatus)) {
            holder.tvStatusIndicator.setVisibility(View.VISIBLE);
            holder.tvStatusIndicator.setText("✅✅ Rescue Completed");
            holder.btnAccept.setVisibility(View.GONE);
        } else if ("assigned".equals(normalizedStatus)) {
            holder.tvStatusIndicator.setVisibility(View.VISIBLE);
            String staffName = item.assignedStaffName != null && !item.assignedStaffName.isEmpty()
                    ? item.assignedStaffName
                    : "Staff";
            holder.tvStatusIndicator.setText("✅ Staff Assigned: " + staffName);
            holder.btnAccept.setVisibility(View.GONE);
        } else if ("accepted".equals(normalizedStatus)) {
            holder.tvStatusIndicator.setVisibility(View.VISIBLE);
            holder.tvStatusIndicator.setText("Accepted. Assign staff from dropdown");
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("Assign Staff");
            holder.btnAccept.setOnClickListener(v -> listener.onAssignStaff(item));
        } else {
            holder.tvStatusIndicator.setVisibility(View.GONE);
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnAccept.setText("Accept Request");
            holder.btnAccept.setOnClickListener(v -> listener.onAccept(item));
        }

        holder.btnViewMap.setOnClickListener(v -> listener.onOpenMap(item));
        holder.btnCallReporter.setOnClickListener(v -> listener.onCallReporter(item));
        holder.itemView.setOnClickListener(v -> listener.onOpenDetails(item));
    }

    private String formatDistance(float distanceMeters) {
        if (distanceMeters == Float.MAX_VALUE) {
            return "Distance unavailable";
        }
        if (distanceMeters >= 1000f) {
            return String.format(Locale.getDefault(), "%.1f km away", distanceMeters / 1000f);
        }
        return String.format(Locale.getDefault(), "%.0f m away", distanceMeters);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReportVH extends RecyclerView.ViewHolder {
        TextView tvAnimalType;
        TextView tvCondition;
        TextView tvDistance;
        TextView tvAddress;
        TextView tvReporterContact;
        TextView tvNewBadge;
        TextView tvStatusIndicator;
        Button btnAccept;
        Button btnViewMap;
        Button btnCallReporter;

        ReportVH(@NonNull View itemView) {
            super(itemView);
            tvAnimalType = itemView.findViewById(R.id.tvReportAnimalType);
            tvCondition = itemView.findViewById(R.id.tvReportCondition);
            tvDistance = itemView.findViewById(R.id.tvReportDistance);
            tvAddress = itemView.findViewById(R.id.tvReportAddress);
            tvReporterContact = itemView.findViewById(R.id.tvReporterContact);
            tvNewBadge = itemView.findViewById(R.id.tvNewBadge);
            tvStatusIndicator = itemView.findViewById(R.id.tvReportStatusIndicator);
            btnAccept = itemView.findViewById(R.id.btnAcceptRequest);
            btnViewMap = itemView.findViewById(R.id.btnViewMap);
            btnCallReporter = itemView.findViewById(R.id.btnCallReporter);
        }
    }
}




