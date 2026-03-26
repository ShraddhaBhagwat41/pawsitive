// This file is obsolete. Logic has moved to com.pawsitive.app.TokenManager
// Keeping an empty class to avoid compilation issues if still referenced anywhere.
package com.example.pawsitive;

import android.content.Context;

// Thin wrapper that delegates to the real TokenManager in com.pawsitive.app
public class TokenManager {

    public static com.pawsitive.app.TokenManager getInstance(Context context) {
        return com.pawsitive.app.TokenManager.getInstance(context);
    }

    // Convenience static methods mirroring the real TokenManager
    public static String getToken(Context context) {
        return com.pawsitive.app.TokenManager.getInstance(context).getToken();
    }

    public static void clearToken(Context context) {
        com.pawsitive.app.TokenManager.getInstance(context).clearToken();
    }
}