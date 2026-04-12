package com.pawsitive.app.network;

import android.content.Context;

import androidx.annotation.NonNull;

import com.pawsitive.app.ApiClient;
import com.pawsitive.app.TokenManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkManager {

    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onError(String error);
    }

    private final ApiService apiService;
    private final TokenManager tokenManager;

    public NetworkManager(Context context) {
        OkHttpClient client = ApiClient.getClient(context);
        String baseUrl = "http://10.143.57.191:3000/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
        tokenManager = TokenManager.getInstance(context);
    }

    public void login(String email, String password, ApiCallback<ApiService.LoginResponse> callback) {
        ApiService.LoginRequest request = new ApiService.LoginRequest();
        request.email = email;
        request.password = password;

        apiService.login(request).enqueue(new Callback<ApiService.LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.LoginResponse> call,
                                   @NonNull Response<ApiService.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveToken(response.body().token);
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Login failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.LoginResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void registerNGO(ApiService.NGORegistrationRequest request,
                             ApiCallback<ApiService.RegisterResponse> callback) {
        apiService.registerNGO(request).enqueue(new Callback<ApiService.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.RegisterResponse> call,
                                   @NonNull Response<ApiService.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Registration failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.RegisterResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getNGOProfile(ApiCallback<ApiService.NGOResponse> callback) {
        apiService.getNGOProfile("Bearer " + tokenManager.getToken()).enqueue(new Callback<ApiService.NGOResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.NGOResponse> call,
                                   @NonNull Response<ApiService.NGOResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load NGO profile: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.NGOResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void registerUser(ApiService.UserRegistrationRequest request,
                              ApiCallback<ApiService.RegisterResponse> callback) {
        apiService.registerUser(request).enqueue(new Callback<ApiService.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.RegisterResponse> call,
                                   @NonNull Response<ApiService.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Registration failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.RegisterResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getAllNGOs(ApiCallback<ApiService.NGOListResponse> callback) {
        apiService.getAllNGOs("Bearer " + tokenManager.getToken()).enqueue(new Callback<ApiService.NGOListResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.NGOListResponse> call,
                                   @NonNull Response<ApiService.NGOListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load NGOs: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.NGOListResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getAdminStats(ApiCallback<ApiService.AdminStatsResponse> callback) {
        apiService.getAdminStats("Bearer " + tokenManager.getToken()).enqueue(new Callback<ApiService.AdminStatsResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.AdminStatsResponse> call,
                                   @NonNull Response<ApiService.AdminStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load stats: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.AdminStatsResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void approveNGO(String id, String notes, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.approveNGO("Bearer " + tokenManager.getToken(), id, new ApiService.ApproveRequest(notes)).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.BasicResponse> call,
                                   @NonNull Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                else callback.onError("Failed to approve NGO: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.BasicResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void rejectNGO(String id, String reason, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.rejectNGO("Bearer " + tokenManager.getToken(), id, new ApiService.RejectRequest(reason)).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.BasicResponse> call,
                                   @NonNull Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) callback.onSuccess(response.body());
                else callback.onError("Failed to reject NGO: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.BasicResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void addStaff(String email, String password, String name, String phone, ApiCallback<ApiService.BasicResponse> callback) {
        ApiService.AddStaffRequest request = new ApiService.AddStaffRequest(email, password, name, phone);
        apiService.addStaff("Bearer " + tokenManager.getToken(), request).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiService.BasicResponse> call,
                                   @NonNull Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to add staff: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiService.BasicResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void clearAuth() {
        tokenManager.clearToken();
    }
}
