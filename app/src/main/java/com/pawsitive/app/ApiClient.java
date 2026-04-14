package com.pawsitive.app;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptor() {
                        @NonNull
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            TokenManager tokenManager = TokenManager.getInstance(context);
                            String token = tokenManager.getToken();

                            Request.Builder builder = chain.request().newBuilder();
                            if (token != null) {
                                builder.addHeader("Authorization", "Bearer " + token);
                            }

                            Request request = builder.build();
                            Response response = chain.proceed(request);

                            if (response.code() == 401) {
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
