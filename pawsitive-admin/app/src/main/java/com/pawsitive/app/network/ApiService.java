package com.pawsitive.app.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    class LoginRequest {
        public String email;
        public String password;
    }

    public static class LoginResponse {
        public String token;
        public String role; // "ADMIN", "NGO", "USER"
    }

    public static class RegisterResponse {
        public boolean success;
        public String message;
    }

    public static class NGORegistrationRequest {
        public String email;
        public String password;
        public String organization_name;
        public String phone;
        public String address;
        public String license_number;
        public String description;
        public String profile_photo_url;
        public String certificate_url;
    }

    public static class NGOResponse {
        public String organization_name;
        public String verification_status;
        public String rejection_reason;
    }

    public static class UserRegistrationRequest {
        public String email;
        public String password;
        public String full_name;
        public String phone;
        public String description;
        public String profile_photo_url;
    }

    @POST("/api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("/api/ngo/register")
    Call<RegisterResponse> registerNGO(@Body NGORegistrationRequest request);

    @GET("/api/ngo/profile")
    Call<NGOResponse> getNGOProfile();

    @POST("/api/user/register")
    Call<RegisterResponse> registerUser(@Body UserRegistrationRequest request);
}



