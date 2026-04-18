package com.pawsitive.app.staff;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StaffPerformanceSummary {
    public final int totalCompleted;
    public final int tasksToday;
    public final int avgResponseMinutes;

    public StaffPerformanceSummary(int totalCompleted, int tasksToday, int avgResponseMinutes) {
        this.totalCompleted = totalCompleted;
        this.tasksToday = tasksToday;
        this.avgResponseMinutes = avgResponseMinutes;
    }

    @NonNull
    public static StaffPerformanceSummary fromTasks(@NonNull List<StaffTask> tasks) {
        int totalCompleted = 0;
        int tasksToday = 0;
        long responseMinutesSum = 0;
        int responseSamples = 0;

        Date now = new Date();
        for (StaffTask task : tasks) {
            if (!task.isCompleted()) {
                continue;
            }

            totalCompleted++;
            if (isSameDay(task.completedAt != null ? task.completedAt : task.assignedAt, now)) {
                tasksToday++;
            }

            if (task.assignedAt != null && task.completedAt != null && task.completedAt.after(task.assignedAt)) {
                long diffMs = task.completedAt.getTime() - task.assignedAt.getTime();
                responseMinutesSum += (diffMs / 60000L);
                responseSamples++;
            }
        }

        int avgMinutes = responseSamples == 0 ? 0 : (int) (responseMinutesSum / responseSamples);
        return new StaffPerformanceSummary(totalCompleted, tasksToday, avgMinutes);
    }

    private static boolean isSameDay(Date first, Date second) {
        if (first == null || second == null) return false;
        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(first);
        c2.setTime(second);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}

