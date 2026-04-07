package com.pawsitive.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnGetStarted;
    private TextView tvLogin;
    private ImageView ivPawsitiveLogoWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize views
        btnGetStarted = findViewById(R.id.btnGetStarted);
        tvLogin = findViewById(R.id.tvLogin);
        ivPawsitiveLogoWelcome = findViewById(R.id.ivPawsitiveLogoWelcome);

        // Start pulsing animation on the Pawsitive logo
        Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_pulse);
        ivPawsitiveLogoWelcome.startAnimation(pulseAnimation);

        // Get Started button click listener
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Signup Activity so new users can register
                Intent intent = new Intent(WelcomeActivity.this, SignupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Login text click listener
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Login Activity
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
