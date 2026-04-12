package com.pawsitive.app.ngo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pawsitive.app.LoginActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.network.NetworkManager;
import com.pawsitive.app.network.ApiService;

public class NGOHomeActivity extends AppCompatActivity {

    private TextView tvVerificationStatus, tvStatusMessage, tvRejectionReason, tvNgoName;
    private ImageView ivStatusIcon, ivLogout;
    private Button btnPostAnimal, btnViewAnimals, btnManageProfile;
    private ProgressBar progressBar;

    private NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ngo_home);

        // Initialize NetworkManager
        networkManager = new NetworkManager(this);

        // Initialize views
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvNgoName = findViewById(R.id.tvNgoName);
        ivLogout = findViewById(R.id.ivLogout);
        progressBar = findViewById(R.id.progressBar);
        btnPostAnimal = findViewById(R.id.btnPostAnimal);
        btnViewAnimals = findViewById(R.id.btnViewAnimals);
        btnManageProfile = findViewById(R.id.btnManageProfile);

        // Setup logout
        ivLogout.setOnClickListener(v -> logout());

        // Setup button listeners
        btnPostAnimal.setOnClickListener(v -> Toast.makeText(this, "Post Animal - Coming Soon", Toast.LENGTH_SHORT).show());
        btnViewAnimals.setOnClickListener(v -> Toast.makeText(this, "View Animals - Coming Soon", Toast.LENGTH_SHORT).show());
        btnManageProfile.setOnClickListener(v -> Toast.makeText(this, "Manage Profile - Coming Soon", Toast.LENGTH_SHORT).show());

        // Load NGO status
        loadNgoStatus();
    }

    private void loadNgoStatus() {
        progressBar.setVisibility(View.VISIBLE);

        // Call REST API to get NGO profile
        networkManager.getNGOProfile(new NetworkManager.ApiCallback<ApiService.NGOResponse>() {
            @Override
            public void onSuccess(ApiService.NGOResponse ngo) {
                progressBar.setVisibility(View.GONE);
                if (ngo == null || ngo.data == null) {
                    Toast.makeText(NGOHomeActivity.this, "Invalid NGO data", Toast.LENGTH_SHORT).show();
                    return;
                }

                ApiService.NGOResponse.NGOData data = ngo.data;
                tvNgoName.setText((data.organization_name != null ? data.organization_name : "NGO") + " Dashboard");
                String status = data.verification_status != null ? data.verification_status : "UNKNOWN";
                tvVerificationStatus.setText(status);

                switch (status) {
                    case "PENDING":
                        tvStatusMessage.setText("Your application is awaiting admin verification. This may take 1-2 business days.");
                        ivStatusIcon.setImageResource(R.drawable.ic_clock_blue);
                        tvVerificationStatus.setTextColor(getResources().getColor(R.color.blue_pending));
                        tvRejectionReason.setVisibility(View.GONE);
                        enableButtons(false);
                        break;

                    case "VERIFIED":
                        tvStatusMessage.setText("✓ Your NGO has been verified! You can now start using all features.");
                        ivStatusIcon.setImageResource(R.drawable.ic_check_green);
                        tvVerificationStatus.setTextColor(getResources().getColor(R.color.green_success));
                        tvRejectionReason.setVisibility(View.GONE);
                        enableButtons(true);
                        break;

                    case "REJECTED":
                        String reason = data.rejection_reason;
                        tvStatusMessage.setText("✗ Your application has been rejected.");
                        tvRejectionReason.setVisibility(View.VISIBLE);
                        tvRejectionReason.setText("Reason: " + (reason != null && !reason.isEmpty() ? reason : "Not specified"));
                        ivStatusIcon.setImageResource(R.drawable.ic_warning);
                        tvVerificationStatus.setTextColor(getResources().getColor(R.color.red_primary));
                        enableButtons(false);
                        break;
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NGOHomeActivity.this, "Error loading NGO data: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableButtons(boolean enabled) {
        btnPostAnimal.setEnabled(enabled);
        btnViewAnimals.setEnabled(enabled);
        btnManageProfile.setEnabled(enabled);

        if (!enabled) {
            btnPostAnimal.setAlpha(0.5f);
            btnViewAnimals.setAlpha(0.5f);
            btnManageProfile.setAlpha(0.5f);
        } else {
            btnPostAnimal.setAlpha(1f);
            btnViewAnimals.setAlpha(1f);
            btnManageProfile.setAlpha(1f);
        }
    }

    private void logout() {
        networkManager.clearAuth();
        Intent intent = new Intent(NGOHomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
