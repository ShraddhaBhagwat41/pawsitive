package com.pawsitive.app.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/ngo/register")
    Call<RegisterResponse> registerNGO(@Body NGORegistrationRequest request);

    @POST("api/ngo/staff")
    Call<BasicResponse> addStaff(@Header("Authorization") String token, @Body AddStaffRequest request);

    @GET("api/ngo/staff")
    Call<StaffListResponse> getStaffList(@Header("Authorization") String token);

    @PUT("api/ngo/staff/{id}")
    Call<BasicResponse> updateStaff(@Header("Authorization") String token, @Path("id") String id, @Body UpdateStaffRequest request);

    @DELETE("api/ngo/staff/{id}")
    Call<BasicResponse> deleteStaff(@Header("Authorization") String token, @Path("id") String id);

    @POST("api/user/register")
    Call<RegisterResponse> registerUser(@Body UserRegistrationRequest request);

    @GET("api/ngo/profile")
    Call<NGOResponse> getNGOProfile(@Header("Authorization") String token);

    // Admin APIs
    @GET("api/ngos")
    Call<NGOListResponse> getAllNGOs(@Header("Authorization") String token);

    @GET("api/admin/stats")
    Call<AdminStatsResponse> getAdminStats(@Header("Authorization") String token);

    @POST("api/ngos/{id}/approve")
    Call<BasicResponse> approveNGO(@Header("Authorization") String token,
                                   @Path("id") String id,
                                   @Body ApproveRequest request);

    @POST("api/ngos/{id}/reject")
    Call<BasicResponse> rejectNGO(@Header("Authorization") String token,
                                  @Path("id") String id,
                                  @Body RejectRequest request);

    class LoginRequest {
        private String email;
        private String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    class LoginResponse {
        public String token;
        @SerializedName("role")
        public String role;

        public String getToken() {
            return token;
        }
    }

    class NGORegistrationRequest {
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

    class AddStaffRequest {
        public String email;
        public String password;
        public String full_name;
        public String phone;
        public String staff_role;
        public String status;

        public AddStaffRequest(String email, String password, String full_name, String phone, String staff_role, String status) {
            this.email = email;
            this.password = password;
            this.full_name = full_name;
            this.phone = phone;
            this.staff_role = staff_role;
            this.status = status;
        }
    }

    class UpdateStaffRequest {
        public String staff_role;
        public String status;

        public UpdateStaffRequest(String staff_role, String status) {
            this.staff_role = staff_role;
            this.status = status;
        }
    }

    class UserRegistrationRequest {
        public String email;
        public String password;
        public String full_name;
        public String phone;
        public String description;
        public String profile_photo_url;
    }

    class RegisterResponse {
        private String message;

        public String getMessage() {
            return message;
        }
    }

    class NGOResponse {
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

    class NGOProfile {
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

    class NGOListResponse {
        public boolean success;
        public List<NGOProfile> data;
        public String error;
    }

    class BasicResponse {
        public boolean success;
        public String message;
        public String error;
    }

    class ApproveRequest {
        public String admin_notes;

        public ApproveRequest(String admin_notes) {
            this.admin_notes = admin_notes;
        }
    }

    class RejectRequest {
        public String rejection_reason;

        public RejectRequest(String rejection_reason) {
            this.rejection_reason = rejection_reason;
        }
    }

    class AdminStatsResponse {
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

    class StaffProfile {
        public String id;
        public String uid;
        public String full_name;
        public String email;
        public String phone;
        public String role;
        public String staff_role;
        public String status;
        public String ngo_id;
        public String organization_name;
    }

    class StaffListResponse {
        public boolean success;
        public List<StaffProfile> data;
        public String error;
    }
}
