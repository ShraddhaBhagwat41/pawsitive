package com.example.pawsitive;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.pawsitive.app.LoginActivity;

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
                            // Get token using wrapper in this package
                            String token = TokenManager.getToken(chain.request().tag(Context.class) != null
                                    ? chain.request().tag(Context.class)
                                    : context);

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
            TokenManager.clearToken(context);
            Intent intent = new Intent(context, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }
}
