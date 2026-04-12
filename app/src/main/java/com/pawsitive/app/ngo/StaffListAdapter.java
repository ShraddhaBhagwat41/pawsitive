package com.pawsitive.app.ngo;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;

import java.util.ArrayList;
import java.util.List;

public class StaffListAdapter extends RecyclerView.Adapter<StaffListAdapter.StaffViewHolder> {

    private List<ApiService.StaffProfile> staffList = new ArrayList<>();
    private final OnStaffInteractionListener listener;

    public interface OnStaffInteractionListener {
        void onEdit(ApiService.StaffProfile staff);
        void onDelete(ApiService.StaffProfile staff);
    }

    public StaffListAdapter(OnStaffInteractionListener listener) {
        this.listener = listener;
    }

    public void setStaffList(List<ApiService.StaffProfile> list) {
        this.staffList = list;
        notifyDataSetDataSet();
    }
    
    private void notifyDataSetDataSet() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        ApiService.StaffProfile staff = staffList.get(position);

        holder.tvStaffName.setText(staff.full_name);
        holder.tvStaffEmail.setText(staff.email);
        holder.tvStaffRole.setText("Role: " + (staff.staff_role != null ? staff.staff_role : "Staff"));

        String status = staff.status != null ? staff.status.toUpperCase() : "ACTIVE";
        holder.tvStatus.setText(status);
        holder.tvStatus.setBackgroundColor(status.equals("ACTIVE") ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        holder.ivEdit.setOnClickListener(v -> listener.onEdit(staff));
        holder.ivDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Delete Staff")
                    .setMessage("Are you sure you want to remove " + staff.full_name + "?")
                    .setPositiveButton("Delete", (dialog, which) -> listener.onDelete(staff))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView tvStaffName, tvStaffRole, tvStaffEmail, tvStatus;
        ImageView ivEdit, ivDelete;

        public StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStaffName = itemView.findViewById(R.id.tvStaffName);
            tvStaffRole = itemView.findViewById(R.id.tvStaffRole);
            tvStaffEmail = itemView.findViewById(R.id.tvStaffEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}