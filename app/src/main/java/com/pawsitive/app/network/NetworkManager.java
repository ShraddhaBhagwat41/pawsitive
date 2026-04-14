package com.pawsitive.app.network;

import android.content.Context;
import com.pawsitive.app.ApiClient;
import com.pawsitive.app.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

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

    public void checkEmail(String email, ApiCallback<Boolean> callback) {
        apiService.checkEmail(new ApiService.CheckEmailRequest(email)).enqueue(new Callback<ApiService.CheckEmailResponse>() {
            @Override
            public void onResponse(Call<ApiService.CheckEmailResponse> call, Response<ApiService.CheckEmailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().exists);
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.CheckEmailResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
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
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
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
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.RegisterResponse> call, Throwable t) {
                callback.onError(t.getMessage());
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
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.RegisterResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private String getError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorString = response.errorBody().string();
                try {
                    org.json.JSONObject jsonObject = new org.json.JSONObject(errorString);
                    if (jsonObject.has("error")) {
                        return jsonObject.getString("error");
                    }
                } catch (Exception e) {
                    // Ignore JSON parse exception and return raw string
                }
                return errorString;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Error " + response.code();
    }

    public void getNGOProfile(ApiCallback<ApiService.NGOResponse> callback) {
        apiService.getNGOProfile().enqueue(new Callback<ApiService.NGOResponse>() {
            @Override
            public void onResponse(Call<ApiService.NGOResponse> call, Response<ApiService.NGOResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.NGOResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getAllNGOs(ApiCallback<ApiService.NGOListResponse> callback) {
        apiService.getAllNGOs().enqueue(new Callback<ApiService.NGOListResponse>() {
            @Override
            public void onResponse(Call<ApiService.NGOListResponse> call, Response<ApiService.NGOListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.NGOListResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getAdminStats(ApiCallback<ApiService.AdminStatsResponse> callback) {
        apiService.getAdminStats().enqueue(new Callback<ApiService.AdminStatsResponse>() {
            @Override
            public void onResponse(Call<ApiService.AdminStatsResponse> call, Response<ApiService.AdminStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.AdminStatsResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void approveNGO(String id, String notes, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.approveNGO(id, new ApiService.ApproveRequest(notes)).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void rejectNGO(String id, String reason, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.rejectNGO(id, new ApiService.RejectRequest(reason)).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void addStaff(String email, String password, String name, String phone, String role, boolean isActive, ApiCallback<ApiService.BasicResponse> callback) {
        String status = isActive ? "active" : "inactive";
        apiService.addStaff(new ApiService.AddStaffRequest(email, password, name, phone, role, status)).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getStaffList(ApiCallback<ApiService.StaffListResponse> callback) {
        apiService.getStaffList().enqueue(new Callback<ApiService.StaffListResponse>() {
            @Override
            public void onResponse(Call<ApiService.StaffListResponse> call, Response<ApiService.StaffListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
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
        apiService.updateStaff(id, new ApiService.UpdateStaffRequest(role, status)).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
                }
            }
            @Override
            public void onFailure(Call<ApiService.BasicResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void deleteStaff(String id, ApiCallback<ApiService.BasicResponse> callback) {
        apiService.deleteStaff(id).enqueue(new Callback<ApiService.BasicResponse>() {
            @Override
            public void onResponse(Call<ApiService.BasicResponse> call, Response<ApiService.BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(getError(response));
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
