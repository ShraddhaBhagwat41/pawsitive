package com.pawsitive.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RescueStoryAdapter extends RecyclerView.Adapter<RescueStoryAdapter.RescueStoryViewHolder> {

    private final Context context;
    private final List<RescueStory> stories;

    public RescueStoryAdapter(Context context, List<RescueStory> stories) {
        this.context = context;
        this.stories = stories;
    }

    @NonNull
    @Override
    public RescueStoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_rescue_story, parent, false);
        return new RescueStoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RescueStoryViewHolder holder, int position) {
        RescueStory story = stories.get(position);

        holder.tvStoryName.setText(story.getAnimalName());
        holder.tvStoryCondition.setText(story.getAnimalType() + " - " + story.getCondition());
        holder.tvStoryDescription.setText(story.getStory());
        holder.tvStoryLocation.setText(story.getLocation());
        holder.tvStoryTime.setText(story.getTimeAgo());
        holder.ivStoryImage.setImageResource(story.getImageResId());

        switch (story.getStatus()) {
            case RECOVERED:
                holder.tvStoryStatus.setText("Recovered");
                holder.tvStoryStatus.setBackgroundResource(R.drawable.bg_story_badge_recovered);
                break;
            case ADOPTED:
                holder.tvStoryStatus.setText("Adopted");
                holder.tvStoryStatus.setBackgroundResource(R.drawable.bg_story_badge_adopted);
                break;
            case RESCUED:
            default:
                holder.tvStoryStatus.setText("Rescued");
                holder.tvStoryStatus.setBackgroundResource(R.drawable.bg_story_badge_rescued);
                break;
        }

        // Subtle card fade-in to keep the feed feeling dynamic.
        holder.itemView.setAlpha(0f);
        holder.itemView.animate().alpha(1f).setDuration(220).start();
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    static class RescueStoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivStoryImage;
        final TextView tvStoryName;
        final TextView tvStoryCondition;
        final TextView tvStoryDescription;
        final TextView tvStoryStatus;
        final TextView tvStoryLocation;
        final TextView tvStoryTime;

        RescueStoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryImage = itemView.findViewById(R.id.ivStoryImage);
            tvStoryName = itemView.findViewById(R.id.tvStoryName);
            tvStoryCondition = itemView.findViewById(R.id.tvStoryCondition);
            tvStoryDescription = itemView.findViewById(R.id.tvStoryDescription);
            tvStoryStatus = itemView.findViewById(R.id.tvStoryStatus);
            tvStoryLocation = itemView.findViewById(R.id.tvStoryLocation);
            tvStoryTime = itemView.findViewById(R.id.tvStoryTime);
        }
    }
}
