package com.pawsitive.app;

public class Responder {
    private String name;
    private String status;
    private boolean isNotified;

    public Responder(String name, String status, boolean isNotified) {
        this.name = name;
        this.status = status;
        this.isNotified = isNotified;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public boolean isNotified() {
        return isNotified;
    }
}
