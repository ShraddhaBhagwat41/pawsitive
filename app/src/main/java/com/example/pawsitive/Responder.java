package com.example.pawsitive;

public class Responder {
    private String name;
    private String status;
    private boolean isSent;

    public Responder(String name, String status, boolean isSent) {
        this.name = name;
        this.status = status;
        this.isSent = isSent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }
}
