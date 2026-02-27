package com.example.pawsitive;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private Context context;
    private List<Pet> petList;

    public PetAdapter(Context context, List<Pet> petList) {
        this.context = context;
        this.petList = petList;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);

        holder.tvPetName.setText(pet.getName());
        holder.tvPetBreedAge.setText(pet.getBreed() + " • " + pet.getAge());
        holder.tvPetDescription.setText(pet.getDescription());
        holder.tvPetDistance.setText(pet.getDistance());
        holder.ivPetImage.setImageResource(pet.getImageResId());

        // Set favorite icon
        if (pet.isFavorite()) {
            holder.ivFavorite.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.ivFavorite.setImageResource(R.drawable.ic_heart_outline);
        }

        // Favorite click listener
        holder.ivFavorite.setOnClickListener(v -> {
            pet.setFavorite(!pet.isFavorite());
            notifyItemChanged(position);
        });
        // Long press listener
        holder.itemView.setOnLongClickListener(v -> {
            Intent intent = new Intent(context, PetProfileActivity.class);
            intent.putExtra("name", pet.getName());
            intent.putExtra("breed", pet.getBreed());
            intent.putExtra("age", pet.getAge());
            intent.putExtra("description", pet.getDescription());
            intent.putExtra("distance", pet.getDistance());
            intent.putExtra("isFavorite", pet.isFavorite());
            intent.putExtra("imageResId", pet.getImageResId());
            context.startActivity(intent);
            return true;
        });

    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public Pet getPetAt(int position) {
        return petList.get(position);
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPetImage, ivFavorite;
        TextView tvPetName, tvPetBreedAge, tvPetDescription, tvPetDistance;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPetImage = itemView.findViewById(R.id.ivPetImage);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvPetName = itemView.findViewById(R.id.tvPetName);
            tvPetBreedAge = itemView.findViewById(R.id.tvPetBreedAge);
            tvPetDescription = itemView.findViewById(R.id.tvPetDescription);
            tvPetDistance = itemView.findViewById(R.id.tvPetDistance);
        }
    }
    public void updateList(List<Pet> newList) {
        this.petList = newList;
        notifyDataSetChanged();
    }
}
