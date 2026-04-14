                            Toast.makeText(context, "NGO rejected", Toast.LENGTH_SHORT).show();
        holder.btnApprove.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onNgoClicked(ngo);
        });

            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.yellow_dark));
        } else if ("REJECTED".equals(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red_primary));

        // Handle verified/rejected visibility
        if ("VERIFIED".equals(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_success));
        String status = ngo.verification_status == null ? "PENDING" : ngo.verification_status.toUpperCase();
package com.pawsitive.app.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.util.List;

public class NGOListAdapter extends RecyclerView.Adapter<NGOListAdapter.ViewHolder> {

    public interface OnNgoClickListener {
        void onNgoClicked(ApiService.NGOProfile ngo);
    }

    public interface OnListChangeListener {
        void onListChanged();
    }

    private final Context context;
    private List<ApiService.NGOProfile> ngos;
    private final NetworkManager networkManager;
    private final OnNgoClickListener clickListener;
    private final OnListChangeListener changeListener;

    public NGOListAdapter(Context context,
                          List<ApiService.NGOProfile> ngos,
                          NetworkManager networkManager,
                          OnNgoClickListener clickListener,
                          OnListChangeListener changeListener) {
        this.context = context;
        this.ngos = ngos;
        this.networkManager = networkManager;
        this.clickListener = clickListener;
        this.changeListener = changeListener;
                "Phone: " + safe(ngo.phone) + "\n" +
                "License: " + safe(ngo.license_number) + "\n" +

    @NonNull
    @Override
        String status = ngo.verification_status == null ? "PENDING" : ngo.verification_status;
        View view = LayoutInflater.from(context).inflate(R.layout.item_ngo_verification, parent, false);
        if ("VERIFIED".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.green_success));
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.btnReject.setVisibility(View.GONE);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
        holder.tvNgoEmail.setText(ngo.ngo_email != null ? ngo.ngo_email : "");
        holder.tvNgoDetails.setText(
            holder.btnReject.setVisibility(View.GONE);
                "Address: " + safe(ngo.address)
        );

        String status = ngo.verification_status == null ? "PENDING" : ngo.verification_status.toUpperCase();
        holder.tvStatus.setText(status);

        holder.itemView.setOnClickListener(v -> {
        } else if ("REJECTED".equals(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red_primary));
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.red_primary));
            holder.btnApprove.setVisibility(View.GONE);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.yellow_dark));
        } else {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.brown_dark));
            holder.btnApprove.setVisibility(View.VISIBLE);
    public int getItemCount() {
        return ngos == null ? 0 : ngos.size();
    }

    public void updateList(List<ApiService.NGOProfile> newList) {
        this.ngos = newList;
        notifyDataSetChanged();
    }

    private void approve(ApiService.NGOProfile ngo) {
        networkManager.approveNGO(ngo.id, "Approved by admin", new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                Toast.makeText(context, response.message != null ? response.message : "NGO approved", Toast.LENGTH_SHORT).show();
                if (changeListener != null) changeListener.onListChanged();
            holder.btnReject.setVisibility(View.VISIBLE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onNgoClicked(ngo);
        });

            if (clickListener != null) clickListener.onNgoClicked(ngo);
        });

        holder.btnApprove.setOnClickListener(v -> approve(ngo));
        holder.btnReject.setOnClickListener(v -> rejectWithReason(ngo));
    }

    @Override
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectWithReason(ApiService.NGOProfile ngo) {
        EditText input = new EditText(context);
        input.setHint("Enter rejection reason");

        new AlertDialog.Builder(context)
                .setTitle("Reject NGO")
                .setView(input)
                .setPositiveButton("Reject", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(context, "Reason is required", Toast.LENGTH_SHORT).show();
                .setNegativeButton("Cancel", null)
                    }
                    networkManager.rejectNGO(ngo.id, reason, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
                        @Override
                        public void onSuccess(ApiService.BasicResponse response) {
                            Toast.makeText(context, response.message != null ? response.message : "NGO rejected", Toast.LENGTH_SHORT).show();
                            if (changeListener != null) changeListener.onListChanged();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                            Toast.makeText(context, "NGO rejected", Toast.LENGTH_SHORT).show();
                .show();
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "N/A" : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNgoName, tvNgoEmail, tvStatus, tvNgoDetails;
        Button btnApprove, btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNgoName = itemView.findViewById(R.id.tvNgoName);
            tvNgoEmail = itemView.findViewById(R.id.tvNgoEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvNgoDetails = itemView.findViewById(R.id.tvNgoDetails);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
