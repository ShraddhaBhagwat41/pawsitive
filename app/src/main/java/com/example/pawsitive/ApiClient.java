package com.example.pawsitive;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static OkHttpClient client;

    public static synchronized OkHttpClient getClient(Context context) {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            TokenManager tokenManager = TokenManager.getInstance(context);
                            String token = tokenManager.getToken();

                            // Initial request with stored token
                            Request.Builder builder = chain.request().newBuilder();
                            if (token != null) {
                                builder.addHeader("Authorization", "Bearer " + token);
                            }
                            
                            Request request = builder.build();
                            Response response = chain.proceed(request);

                            // If 401 Unauthorized, token might be expired or invalid
                            if (response.code() == 401) {
                                // Redirect to login as we don't have a refresh mechanism without Firebase yet
                                redirectToLogin(context);
                            }

                            return response;
                        }
                    })
                    .build();
        }
        return client;
    }

    private static void redirectToLogin(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> {
            TokenManager.getInstance(context).clearToken();
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }
}
