package com.pawsitive.app.network;

import android.content.Context;

import androidx.annotation.NonNull;

import com.pawsitive.app.ApiClient;

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

    public NetworkManager(Context context) {
        OkHttpClient client = ApiClient.getClient(context);

        // When running on a physical device with ADB reverse, use localhost:3000
        String baseUrl = "http://localhost:3000";
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
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
        apiService.getNGOProfile().enqueue(new Callback<ApiService.NGOResponse>() {
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

    public void clearAuth() {
        // Clear any stored auth token using ApiClient / TokenManager if needed
        // For now, nothing extra is required here because ApiClient reads token per request.
    }
}




