package com.pawsitive.app.staff;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.R;

import java.util.ArrayList;
import java.util.List;

public class StaffTaskAdapter extends RecyclerView.Adapter<StaffTaskAdapter.TaskVH> {

    public interface TaskActionListener {
        void onOpenMap(@NonNull TaskItem item);
        void onCallNgo(@NonNull TaskItem item);
        void onMarkOnTheWay(@NonNull TaskItem item);
        void onMarkCompleted(@NonNull TaskItem item);
        void onTaskSelected(@NonNull TaskItem item);
    }

    public static class TaskItem {
        public final StaffTask task;
        public final String distanceText;
        public final String assignedTimeText;

        public TaskItem(@NonNull StaffTask task, @NonNull String distanceText, @NonNull String assignedTimeText) {
            this.task = task;
            this.distanceText = distanceText;
            this.assignedTimeText = assignedTimeText;
        }
    }

    private final List<TaskItem> items = new ArrayList<>();
    private final TaskActionListener listener;

    public StaffTaskAdapter(@NonNull TaskActionListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<TaskItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staff_task, parent, false);
        return new TaskVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskVH holder, int position) {
        TaskItem item = items.get(position);
        StaffTask task = item.task;

        holder.tvAnimalIcon.setText(iconForAnimal(task.animalType));
        holder.tvAnimalType.setText(task.animalType);
        holder.tvCondition.setText("Condition: " + task.condition);
        holder.tvAddress.setText(task.address);
        holder.tvDistance.setText(item.distanceText);
        holder.tvAssignedTime.setText(item.assignedTimeText);

        String statusLabel = statusLabel(task.status);
        holder.tvStatus.setText(statusLabel);
        @ColorRes int statusColor = statusColor(task.status);
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), statusColor));

        boolean hasCallNumber = !task.phoneForNgoCall().trim().isEmpty();
        holder.btnCall.setEnabled(hasCallNumber);
        holder.btnCall.setAlpha(hasCallNumber ? 1f : 0.5f);

        boolean isCompleted = task.isCompleted();
        holder.btnOnTheWay.setEnabled(!isCompleted && !StaffTask.STATUS_ON_THE_WAY.equals(task.status));
        holder.btnComplete.setEnabled(!isCompleted);
        holder.btnOnTheWay.setAlpha(holder.btnOnTheWay.isEnabled() ? 1f : 0.6f);
        holder.btnComplete.setAlpha(holder.btnComplete.isEnabled() ? 1f : 0.6f);

        holder.itemView.setOnClickListener(v -> listener.onTaskSelected(item));
        holder.btnMap.setOnClickListener(v -> listener.onOpenMap(item));
        holder.btnCall.setOnClickListener(v -> listener.onCallNgo(item));
        holder.btnOnTheWay.setOnClickListener(v -> listener.onMarkOnTheWay(item));
        holder.btnComplete.setOnClickListener(v -> listener.onMarkCompleted(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String iconForAnimal(String animalType) {
        String lower = animalType == null ? "" : animalType.toLowerCase();
        if (lower.contains("dog")) return "D";
        if (lower.contains("cat")) return "C";
        if (lower.contains("bird")) return "B";
        return "A";
    }

    private String statusLabel(String status) {
        if (StaffTask.STATUS_ON_THE_WAY.equals(status)) return "On the Way";
        if (StaffTask.STATUS_COMPLETED.equals(status)) return "Completed";
        return "Assigned";
    }

    @ColorRes
    private int statusColor(String status) {
        if (StaffTask.STATUS_ON_THE_WAY.equals(status)) return R.color.staff_status_on_way;
        if (StaffTask.STATUS_COMPLETED.equals(status)) return R.color.staff_status_done;
        return R.color.staff_status_assigned;
    }

    static class TaskVH extends RecyclerView.ViewHolder {
        TextView tvAnimalIcon;
        TextView tvAnimalType;
        TextView tvCondition;
        TextView tvAddress;
        TextView tvDistance;
        TextView tvAssignedTime;
        TextView tvStatus;
        Button btnMap;
        Button btnCall;
        Button btnOnTheWay;
        Button btnComplete;

        TaskVH(@NonNull View itemView) {
            super(itemView);
            tvAnimalIcon = itemView.findViewById(R.id.tvTaskAnimalIcon);
            tvAnimalType = itemView.findViewById(R.id.tvTaskAnimalType);
            tvCondition = itemView.findViewById(R.id.tvTaskCondition);
            tvAddress = itemView.findViewById(R.id.tvTaskAddress);
            tvDistance = itemView.findViewById(R.id.tvTaskDistance);
            tvAssignedTime = itemView.findViewById(R.id.tvTaskAssignedTime);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
            btnMap = itemView.findViewById(R.id.btnTaskMap);
            btnCall = itemView.findViewById(R.id.btnTaskCall);
            btnOnTheWay = itemView.findViewById(R.id.btnTaskOnTheWay);
            btnComplete = itemView.findViewById(R.id.btnTaskComplete);
        }
    }
}
