package com.pawsitive.app.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pawsitive.app.R;
import com.pawsitive.app.TokenManager;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

public class AdminNGODetailActivity extends AppCompatActivity {

    private NetworkManager networkManager;
    private TokenManager tokenManager;
    private String ngoId;
    private String certificateUrl;
    private EditText etNotes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ngo_detail);

        networkManager = new NetworkManager(this);
        tokenManager = TokenManager.getInstance(this);

        Intent intent = getIntent();
        ngoId = intent.getStringExtra("ngo_id");
        certificateUrl = intent.getStringExtra("ngo_certificate_url");

        TextView tvOrgName = findViewById(R.id.tvOrgName);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvLicense = findViewById(R.id.tvLicense);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvStatus = findViewById(R.id.tvStatus);
        ImageView ivBack = findViewById(R.id.ivBack);
        Button btnViewCertificate = findViewById(R.id.btnViewCertificate);
        Button btnApprove = findViewById(R.id.btnApprove);
        Button btnReject = findViewById(R.id.btnReject);
        etNotes = findViewById(R.id.etNotes);

        tvOrgName.setText(intent.getStringExtra("ngo_name"));
        tvPhone.setText(intent.getStringExtra("ngo_phone"));
        tvAddress.setText(intent.getStringExtra("ngo_address"));
        tvLicense.setText(intent.getStringExtra("ngo_license"));
        tvDescription.setText(intent.getStringExtra("ngo_description"));
        tvStatus.setText(intent.getStringExtra("ngo_status"));

        ivBack.setOnClickListener(v -> finish());
        btnViewCertificate.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(certificateUrl)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(certificateUrl)));
            } else {
                Toast.makeText(this, "No certificate uploaded", Toast.LENGTH_SHORT).show();
            }
        });

        btnApprove.setOnClickListener(v -> approveNgo());
        btnReject.setOnClickListener(v -> rejectNgo());
    }

    private void approveNgo() {
        if (!tokenManager.isTokenValid()) {
            Toast.makeText(this, "Missing admin token", Toast.LENGTH_SHORT).show();
            return;
        }
        String notes = etNotes.getText().toString().trim();
        networkManager.approveNGO(ngoId, notes, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                Toast.makeText(AdminNGODetailActivity.this, "NGO approved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminNGODetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectNgo() {
        if (!tokenManager.isTokenValid()) {
            Toast.makeText(this, "Missing admin token", Toast.LENGTH_SHORT).show();
            return;
        }
        String reason = etNotes.getText().toString().trim();
        if (TextUtils.isEmpty(reason)) {
            etNotes.setError("Rejection reason required");
            return;
        }
        networkManager.rejectNGO(ngoId, reason, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                Toast.makeText(AdminNGODetailActivity.this, "NGO rejected", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminNGODetailActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
