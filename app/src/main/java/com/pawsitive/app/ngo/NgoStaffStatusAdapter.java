package com.pawsitive.app.ngo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.R;

import java.util.ArrayList;
import java.util.List;

public class NgoStaffStatusAdapter extends RecyclerView.Adapter<NgoStaffStatusAdapter.StaffVH> {

    public static class StaffItem {
        public final String id;
        public final String name;
        public final String phone;
        public final boolean isAvailable;

        public StaffItem(String id, String name, String phone, boolean isAvailable) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.isAvailable = isAvailable;
        }
    }

    private final List<StaffItem> items = new ArrayList<>();

    public void submitList(@NonNull List<StaffItem> updated) {
        items.clear();
        items.addAll(updated);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StaffVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ngo_staff_status, parent, false);
        return new StaffVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffVH holder, int position) {
        StaffItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.tvPhone.setText(item.phone == null || item.phone.trim().isEmpty() ? "No phone" : item.phone);

        int color = item.isAvailable ? R.color.green_success : R.color.red_primary;
        holder.tvStatusDot.setText(item.isAvailable ? "AVAILABLE" : "BUSY");
        holder.tvStatusDot.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StaffVH extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvPhone;
        TextView tvStatusDot;

        StaffVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvNgoStaffName);
            tvPhone = itemView.findViewById(R.id.tvNgoStaffPhone);
            tvStatusDot = itemView.findViewById(R.id.tvNgoStaffStatus);
        }
    }
}

