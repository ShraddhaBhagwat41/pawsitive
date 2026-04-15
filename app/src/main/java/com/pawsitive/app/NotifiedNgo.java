package com.pawsitive.app;

public class NotifiedNgo {
    public final String name;
    public final String phone;
    public final double latitude;
    public final double longitude;
    public final float distanceMeters;

    public NotifiedNgo(String name, String phone, double latitude, double longitude, float distanceMeters) {
        this.name = name;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }
}

