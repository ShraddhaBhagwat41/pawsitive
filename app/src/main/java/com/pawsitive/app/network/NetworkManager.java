package com.pawsitive.app.network;

import android.content.Context;

import com.pawsitive.app.ApiClient;
import com.pawsitive.app.TokenManager;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkManager {

    private ApiService apiService;
    private TokenManager tokenManager;

    public NetworkManager(Context context) {
        this.apiService = new retrofit2.Retrofit.Builder()
                .baseUrl("http://10.143.57.191:3000/")
                .client(ApiClient.getClient(context))
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
        this.tokenManager = TokenManager.getInstance(context);
    }

    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }

    public void login(String email, String password, ApiCallback<ApiService.LoginResponse> callback) {
        ApiService.LoginRequest request = new ApiService.LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<ApiService.LoginResponse>() {
            @Override
            public void onResponse(Call<ApiService.LoginResponse> call, Response<ApiService.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tokenManager.saveToken(response.body().getToken());
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Login failed");
                }
            }

            @Override
            public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getNGOProfile(ApiCallback<ApiService.NGOResponse> callback) {
        apiService.getNGOProfile("Bearer " + tokenManager.getToken()).enqueue(new Callback<ApiService.NGOResponse>() {
            @Override
            public void onResponse(Call<ApiService.NGOResponse> call, Response<ApiService.NGOResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get NGO profile");
                }
            }

            @Override
            public void onFailure(Call<ApiService.NGOResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void registerNGO(ApiService.NGORegistrationRequest request, ApiCallback<ApiService.RegisterResponse> callback) {
        apiService.registerNGO(request).enqueue(new Callback<ApiService.RegisterResponse>() {
            @Override
            public void onResponse(Call<ApiService.RegisterResponse> call, Response<ApiService.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            callback.onError(errorStr);
                        } else {
                            callback.onError("NGO registration failed: " + response.code() + " body=" + response.body());
                        }
                    } catch (Exception e) {
                        callback.onError("NGO registration exception: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.RegisterResponse> call, Throwable t) {
                    callback.onError("Network/Parse Error: " + t.getMessage());
                }
            });
    }

    public void registerUser(ApiService.UserRegistrationRequest request, ApiCallback<ApiService.RegisterResponse> callback) {
        apiService.registerUser(request).enqueue(new Callback<ApiService.RegisterResponse>() {
            @Override
            public void onResponse(Call<ApiService.RegisterResponse> call, Response<ApiService.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            callback.onError(errorStr);
                        } else {
                            callback.onError("User registration failed: " + response.code());
                        }
                    } catch (Exception e) {
                        callback.onError("User registration failed");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.RegisterResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void addStaff(String email, String password, String name, String phone, String role, boolean isActive, ApiCallback<ApiService.BasicResponse> callback) {
        String status = isActive ? "active" : "inactive";
        ApiService.AddStaffRequest request = new ApiService.AddStaffRequest(email, password, name, phone, role, status);
        apiService.addStaff("Bearer " + tokenManager.getToken(), request).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            // Parse JSON if needed or just show the string
                            callback.onError(errorStr);
                        } else {
                            callback.onError("Failed to add staff: " + response.code());
                        }
                    } catch (Exception e) {
                        callback.onError("Failed to add staff");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getStaffList(ApiCallback<ApiService.StaffListResponse> callback) {
        apiService.getStaffList("Bearer " + tokenManager.getToken()).enqueue(new Callback<ApiService.StaffListResponse>() {
            @Override
            public void onResponse(Call<ApiService.StaffListResponse> call, Response<ApiService.StaffListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get staff list");
                }
            }

            @Override
            public void onFailure(Call<ApiService.StaffListResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void updateStaff(String id, String role, boolean isActive, ApiCallback<ApiService.BasicResponse> callback) {
        String status = isActive ? "active" : "inactive";
        ApiService.UpdateStaffRequest request = new ApiService.UpdateStaffRequest(role, status);
        apiService.updateStaff("Bearer " + tokenManager.getToken(), id, request).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to update staff");
                }
            }

            @Override
            public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void deleteStaff(String id, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.deleteStaff("Bearer " + tokenManager.getToken(), id).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to delete staff");
                }
            }

            @Override
            public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    // Admin helpers
    public void getAllNGOs(ApiCallback<ApiService.NGOListResponse> callback) {
        apiService.getAllNGOs("Bearer " + tokenManager.getToken())
                .enqueue(new Callback<ApiService.NGOListResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.NGOListResponse> call, Response<ApiService.NGOListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to fetch NGOs");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.NGOListResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void getAdminStats(ApiCallback<ApiService.AdminStatsResponse> callback) {
        apiService.getAdminStats("Bearer " + tokenManager.getToken())
                .enqueue(new Callback<ApiService.AdminStatsResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.AdminStatsResponse> call, Response<ApiService.AdminStatsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to fetch admin stats");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.AdminStatsResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void approveNGO(String id, String notes, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.approveNGO("Bearer " + tokenManager.getToken(), id, new ApiService.ApproveRequest(notes))
                .enqueue(new Callback<ApiService.BasicResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to approve NGO");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void rejectNGO(String id, String reason, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.rejectNGO("Bearer " + tokenManager.getToken(), id, new ApiService.RejectRequest(reason))
                .enqueue(new Callback<ApiService.BasicResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to reject NGO");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public void clearAuth() {
        tokenManager.clearToken();
    }
}

