package com.pawsitive.app;

public class RescueStory {
    public enum Status {
        RESCUED,
        RECOVERED,
        ADOPTED
    }

    private final String animalName;
    private final String animalType;
    private final String condition;
    private final String story;
    private final Status status;
    private final String location;
    private final String timeAgo;
    private final int imageResId;

    public RescueStory(String animalName,
                       String animalType,
                       String condition,
                       String story,
                       Status status,
                       String location,
                       String timeAgo,
                       int imageResId) {
        this.animalName = animalName;
        this.animalType = animalType;
        this.condition = condition;
        this.story = story;
        this.status = status;
        this.location = location;
        this.timeAgo = timeAgo;
        this.imageResId = imageResId;
    }

    public String getAnimalName() {
        return animalName;
    }

    public String getAnimalType() {
        return animalType;
    }

    public String getCondition() {
        return condition;
    }

    public String getStory() {
        return story;
    }

    public Status getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public int getImageResId() {
        return imageResId;
    }
}
