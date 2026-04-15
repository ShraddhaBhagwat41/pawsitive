package com.pawsitive.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotifiedNgoAdapter extends RecyclerView.Adapter<NotifiedNgoAdapter.VH> {

    private final Context context;
    private final List<NotifiedNgo> items = new ArrayList<>();

    public NotifiedNgoAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<NotifiedNgo> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notified_ngo, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotifiedNgo ngo = items.get(position);
        holder.tvName.setText(ngo.name != null ? ngo.name : "NGO");

        String distText;
        if (ngo.distanceMeters >= 1000f) {
            distText = String.format(Locale.getDefault(), "%.1f km away", ngo.distanceMeters / 1000f);
        } else {
            distText = String.format(Locale.getDefault(), "%.0f m away", ngo.distanceMeters);
        }
        holder.tvDistance.setText(distText);

        holder.btnCall.setOnClickListener(view -> {
            if (ngo.phone == null || ngo.phone.trim().isEmpty()) {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + ngo.phone.trim()));
            context.startActivity(dialIntent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDistance;
        Button btnCall;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvNgoName);
            tvDistance = itemView.findViewById(R.id.tvNgoDistance);
            btnCall = itemView.findViewById(R.id.btnCallNgo);
        }
    }
}
