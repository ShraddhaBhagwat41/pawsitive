package com.example.pawsitive;

public class Pet {
    private String name;
    private String breed;
    private String age;
    private String description;
    private String distance;
    private boolean isFavorite;
    private String imageUrl;
    private int imageResId;

    public Pet(String name, String breed, String age, String description, String distance, boolean isFavorite, int imageResId) {
        this.name = name;
        this.breed = breed;
        this.age = age;
        this.description = description;
        this.distance = distance;
        this.isFavorite = isFavorite;
        this.imageResId = imageResId;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public int getImageResId() { return imageResId; }
}
