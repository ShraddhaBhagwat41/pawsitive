package com.pawsitive.app;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "secure_prefs";
    private static final String KEY_TOKEN = "id_token";
    private static TokenManager instance;
    private SharedPreferences sharedPreferences;

    private TokenManager(Context context) {
        try {
            // Using standard SharedPreferences to avoid device-specific EncryptedSharedPreferences bugs
            sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveToken(String token) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
        }
    }

    public String getToken() {
        if (sharedPreferences != null) {
            return sharedPreferences.getString(KEY_TOKEN, null);
        }
        return null;
    }

    public void clearToken() {
        if (sharedPreferences != null) {
            sharedPreferences.edit().remove(KEY_TOKEN).apply();
        }
    }

    public boolean isTokenValid() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }
}