package com.example.pawsitive;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResponderAdapter extends RecyclerView.Adapter<ResponderAdapter.ResponderViewHolder> {

    private Context context;
    private List<Responder> responderList;

    public ResponderAdapter(Context context, List<Responder> responderList) {
        this.context = context;
        this.responderList = responderList;
    }

    @NonNull
    @Override
    public ResponderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_responder, parent, false);
        return new ResponderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResponderViewHolder holder, int position) {
        Responder responder = responderList.get(position);

        // Set status text and color
        holder.tvStatus.setText(responder.getStatus());

        if (responder.isSent()) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.green_success));
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check_green);
        } else {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.blue_pending));
            holder.ivStatusIcon.setImageResource(R.drawable.ic_clock_blue);
        }
    }

    @Override
    public int getItemCount() {
        return responderList.size();
    }

    public static class ResponderViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserIcon, ivStatusIcon;
        TextView tvStatus;

        public ResponderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserIcon = itemView.findViewById(R.id.ivUserIcon);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
