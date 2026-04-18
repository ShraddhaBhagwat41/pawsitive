package com.pawsitive.app.ngo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.pawsitive.app.R;

import java.util.ArrayList;
import java.util.List;

public class NgoAnimalAdapter extends RecyclerView.Adapter<NgoAnimalAdapter.AnimalVH> {

    public interface AnimalActionListener {
        void onViewDetails(@NonNull AnimalItem item);
        void onEdit(@NonNull AnimalItem item);
    }

    public static class AnimalItem {
        public final String id;
        public final String imageUrl;
        public final String animalType;
        public final String condition;
        public final String rescueDate;
        public final String description;
        public final String location;

        public AnimalItem(String id, String imageUrl, String animalType, String condition,
                          String rescueDate, String description, String location) {
            this.id = id;
            this.imageUrl = imageUrl;
            this.animalType = animalType;
            this.condition = condition;
            this.rescueDate = rescueDate;
            this.description = description;
            this.location = location;
        }
    }

    private final List<AnimalItem> items = new ArrayList<>();
    private final AnimalActionListener listener;

    public NgoAnimalAdapter(@NonNull AnimalActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<AnimalItem> updated) {
        items.clear();
        items.addAll(updated);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AnimalVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ngo_animal, parent, false);
        return new AnimalVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimalVH holder, int position) {
        AnimalItem item = items.get(position);
        holder.tvType.setText(item.animalType);
        holder.tvCondition.setText("Condition: " + item.condition);
        holder.tvDate.setText(item.rescueDate);

        if (item.imageUrl != null && !item.imageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .into(holder.ivAnimal);
        } else {
            holder.ivAnimal.setImageResource(R.drawable.ic_pet_placeholder);
        }

        holder.btnDetails.setOnClickListener(v -> listener.onViewDetails(item));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AnimalVH extends RecyclerView.ViewHolder {
        ImageView ivAnimal;
        TextView tvType;
        TextView tvCondition;
        TextView tvDate;
        TextView btnDetails;
        TextView btnEdit;

        AnimalVH(@NonNull View itemView) {
            super(itemView);
            ivAnimal = itemView.findViewById(R.id.ivNgoAnimalImage);
            tvType = itemView.findViewById(R.id.tvNgoAnimalType);
            tvCondition = itemView.findViewById(R.id.tvNgoAnimalCondition);
            tvDate = itemView.findViewById(R.id.tvNgoAnimalDate);
            btnDetails = itemView.findViewById(R.id.tvNgoAnimalDetails);
            btnEdit = itemView.findViewById(R.id.tvNgoAnimalEdit);
        }
    }
}

