package com.pawsitive.app.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @POST("api/auth/check-email")
    Call<CheckEmailResponse> checkEmail(@Body CheckEmailRequest request);

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/ngo/register")
    Call<RegisterResponse> registerNGO(@Body NGORegistrationRequest request);

    @POST("api/user/register")
    Call<RegisterResponse> registerUser(@Body UserRegistrationRequest request);

    @GET("api/ngo/profile")
    Call<NGOResponse> getNGOProfile();

    @GET("api/ngos")
    Call<NGOListResponse> getAllNGOs();

    @GET("api/admin/stats")
    Call<AdminStatsResponse> getAdminStats();

    @POST("api/ngos/{id}/approve")
    Call<BasicResponse> approveNGO(@Path("id") String id, @Body ApproveRequest request);

    @POST("api/ngos/{id}/reject")
    Call<BasicResponse> rejectNGO(@Path("id") String id, @Body RejectRequest request);

    @POST("api/ngo/staff")
    Call<BasicResponse> addStaff(@Body AddStaffRequest request);

    @GET("api/ngo/staff")
    Call<StaffListResponse> getStaffList();

    @PUT("api/ngo/staff/{id}")
    Call<BasicResponse> updateStaff(@Path("id") String id, @Body UpdateStaffRequest request);

    @DELETE("api/ngo/staff/{id}")
    Call<BasicResponse> deleteStaff(@Path("id") String id);

    @POST("api/ngos/notify-nearby")
    Call<NotifyNgosResponse> notifyNearbyNgos(@Body NotifyNgosRequest request);

    // --- Data Classes ---

    class CheckEmailRequest {
        public String email;
        public CheckEmailRequest(String email) { this.email = email; }
    }

    class CheckEmailResponse {
        public boolean exists;
    }

    class LoginRequest {
        public String email, password;
        public LoginRequest(String email, String password) { this.email = email; this.password = password; }
    }

    class LoginResponse {
        public String token;
        @SerializedName("role") public String role;
        public String getToken() { return token; }
    }

    class NGORegistrationRequest {
        public String uid, email, password, organization_name, phone, address, license_number, description, profile_photo_url, certificate_url;
    }

    class UserRegistrationRequest {
        public String email, password, full_name, phone, description, profile_photo_url, location_address;
        public Double latitude, longitude;
    }

    class RegisterResponse {
        private String message;
        public String getMessage() { return message; }
    }

    class NGOResponse {
        public boolean success;
        public NGOData data;
        public static class NGOData {
            public String organization_name, verification_status, rejection_reason, phone, address, license_number, description, certificate_url, ngo_email;
        }
    }

    class NGOProfile {
        public String id, organization_name, ngo_email, verification_status, rejection_reason, phone, address, license_number, description, certificate_url;
    }

    class NGOListResponse {
        public boolean success;
        public List<NGOProfile> data;
    }

    class AdminStatsResponse {
        public boolean success;
        public StatsData data;
        public static class StatsData {
            public int totalNGOs, verifiedNGOs, pendingNGOs, rejectedNGOs, totalAnimalsPosts, animalsSaved;
        }
    }

    class BasicResponse {
        public boolean success;
        public String message, error;
    }

    class ApproveRequest {
        public String admin_notes;
        public ApproveRequest(String admin_notes) { this.admin_notes = admin_notes; }
    }

    class RejectRequest {
        public String rejection_reason;
        public RejectRequest(String rejection_reason) { this.rejection_reason = rejection_reason; }
    }

    class AddStaffRequest {
        public String email, password, full_name, phone, staff_role, status;
        public AddStaffRequest(String email, String password, String full_name, String phone, String staff_role, String status) {
            this.email = email; this.password = password; this.full_name = full_name; this.phone = phone; this.staff_role = staff_role; this.status = status;
        }
    }

    class UpdateStaffRequest {
        public String staff_role, status;
        public UpdateStaffRequest(String staff_role, String status) { this.staff_role = staff_role; this.status = status; }
    }

    class StaffProfile {
        public String id, uid, full_name, email, phone, role, staff_role, status, ngo_id, organization_name;
    }

    class StaffListResponse {
        public boolean success;
        public List<StaffProfile> data;
    }

    class NotifyNgosRequest {
        public double incidentLat, incidentLng;
        public String incidentId, animalType;

        public NotifyNgosRequest(double incidentLat, double incidentLng, String incidentId, String animalType) {
            this.incidentLat = incidentLat;
            this.incidentLng = incidentLng;
            this.incidentId = incidentId;
            this.animalType = animalType;
        }
    }

    class NotifyNgosResponse {
        public boolean success;
        public String message, error;
        public int successCount, failureCount;
    }
}