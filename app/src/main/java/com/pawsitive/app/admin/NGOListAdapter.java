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
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.util.List;

public class NGOListAdapter extends RecyclerView.Adapter<NGOListAdapter.NGOViewHolder> {

    public interface OnNgoClickListener {
        void onNgoClicked(ApiService.NGOProfile ngo);
    }

    public interface OnListChangeListener {
        void onListChanged();
    }

    private Context context;
    private List<ApiService.NGOProfile> ngoList;
    private NetworkManager networkManager;
    private OnNgoClickListener clickListener;
    private OnListChangeListener changeListener;

    public NGOListAdapter(Context context, List<ApiService.NGOProfile> ngoList,
                          NetworkManager networkManager,
                          OnNgoClickListener clickListener,
                          OnListChangeListener changeListener) {
        this.context = context;
        this.ngoList = ngoList;
        this.networkManager = networkManager;
        this.clickListener = clickListener;
        this.changeListener = changeListener;
    }

    @NonNull
    @Override
    public NGOViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ngo_verification, parent, false);
        return new NGOViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NGOViewHolder holder, int position) {
        ApiService.NGOProfile ngo = ngoList.get(position);

        holder.tvNgoName.setText(ngo.organization_name != null ? ngo.organization_name : "Unnamed NGO");
        holder.tvNgoEmail.setText(ngo.ngo_email != null ? ngo.ngo_email : "No email");
        holder.tvStatus.setText(ngo.verification_status != null ? ngo.verification_status : "");

        String status = ngo.verification_status != null ? ngo.verification_status : "";
        if ("PENDING".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.blue_pending));
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
        } else if ("VERIFIED".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.green_success));
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.red_primary));
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onNgoClicked(ngo);
        });

        holder.btnApprove.setOnClickListener(v -> approveNGO(ngo));
        holder.btnReject.setOnClickListener(v -> showRejectDialog(ngo));
    }

    @Override
    public int getItemCount() {
        return ngoList == null ? 0 : ngoList.size();
    }

    public void updateList(List<ApiService.NGOProfile> newList) {
        this.ngoList = newList;
        notifyDataSetChanged();
    }

    private void approveNGO(ApiService.NGOProfile ngo) {
        networkManager.approveNGO(ngo.id, "Approved by admin", new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                Toast.makeText(context, response.message != null ? response.message : "Approved successfully", Toast.LENGTH_SHORT).show();
                if (changeListener != null) {
                    changeListener.onListChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(context, "Approval failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRejectDialog(ApiService.NGOProfile ngo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Reject NGO");

        final EditText input = new EditText(context);
        input.setHint("Reason for rejection");
        builder.setView(input);

        builder.setPositiveButton("Reject", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(context, "Rejection reason is required", Toast.LENGTH_SHORT).show();
                return;
            }
            rejectNGO(ngo, reason);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void rejectNGO(ApiService.NGOProfile ngo, String reason) {
        networkManager.rejectNGO(ngo.id, reason, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                Toast.makeText(context, response.message != null ? response.message : "Rejected successfully", Toast.LENGTH_SHORT).show();
                if (changeListener != null) {
                    changeListener.onListChanged();
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(context, "Rejection failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class NGOViewHolder extends RecyclerView.ViewHolder {
        TextView tvNgoName, tvNgoEmail, tvStatus;
        Button btnApprove, btnReject;

        NGOViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNgoName = itemView.findViewById(R.id.tvNgoName);
            tvNgoEmail = itemView.findViewById(R.id.tvNgoEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
