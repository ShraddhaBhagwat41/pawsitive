package com.pawsitive.app.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

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
        public boolean success;
        public NGOData data;
        public String error;

        public static class NGOData {
            public String organization_name;
            public String verification_status;
            public String rejection_reason;
            public String phone;
            public String address;
            public String license_number;
            public String description;
            public String certificate_url;
            public String ngo_email;
        }
    }

    public static class NGOProfile {
        public String id;
        public String organization_name;
        public String ngo_email;
        public String verification_status;
        public String rejection_reason;
        public String phone;
        public String address;
        public String license_number;
        public String description;
        public String certificate_url;
    }

    public static class NGOListResponse {
        public boolean success;
        public List<NGOProfile> data;
        public String error;
    }

    public static class BasicResponse {
        public boolean success;
        public String message;
        public String error;
    }

    public static class ApproveRequest {
        public String admin_notes;
        public ApproveRequest(String admin_notes) { this.admin_notes = admin_notes; }
    }

    public static class RejectRequest {
        public String rejection_reason;
        public RejectRequest(String rejection_reason) { this.rejection_reason = rejection_reason; }
    }

    public static class AdminStatsResponse {
        public boolean success;
        public StatsData data;

        public static class StatsData {
            public int totalNGOs;
            public int verifiedNGOs;
            public int pendingNGOs;
            public int rejectedNGOs;
            public int totalAnimalsPosts;
            public int animalsSaved;
        }
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
    Call<NGOResponse> getNGOProfile(@Header("Authorization") String token);

    @POST("/api/user/register")
    Call<RegisterResponse> registerUser(@Body UserRegistrationRequest request);

    @GET("/api/ngos")
    Call<NGOListResponse> getAllNGOs(@Header("Authorization") String token);

    @GET("/api/admin/stats")
    Call<AdminStatsResponse> getAdminStats(@Header("Authorization") String token);

    @POST("/api/ngos/{id}/approve")
    Call<BasicResponse> approveNGO(@Header("Authorization") String token, @Path("id") String id, @Body ApproveRequest request);

    @POST("/api/ngos/{id}/reject")
    Call<BasicResponse> rejectNGO(@Header("Authorization") String token, @Path("id") String id, @Body RejectRequest request);
}
