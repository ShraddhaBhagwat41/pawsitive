package com.pawsitive.app.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

public class AdminNGODetailActivity extends AppCompatActivity {

    private TextView tvOrgName, tvPhone, tvAddress, tvLicense, tvDescription, tvStatus;
    private Button btnViewCertificate, btnApprove, btnReject;
    private EditText etNotes;
    private ImageView ivBack;

    private NetworkManager networkManager;
    private String ngoId;
    private String certificateUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ngo_detail);

        networkManager = new NetworkManager(this);

        ivBack = findViewById(R.id.ivBack);
        tvOrgName = findViewById(R.id.tvOrgName);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvLicense = findViewById(R.id.tvLicense);
        tvDescription = findViewById(R.id.tvDescription);
        tvStatus = findViewById(R.id.tvStatus);
        btnViewCertificate = findViewById(R.id.btnViewCertificate);
        etNotes = findViewById(R.id.etNotes);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);

        Intent intent = getIntent();
        ngoId = intent.getStringExtra("ngo_id");
        certificateUrl = intent.getStringExtra("ngo_certificate_url");

        tvOrgName.setText(intent.getStringExtra("ngo_name"));
        tvPhone.setText(intent.getStringExtra("ngo_phone"));
        tvAddress.setText(intent.getStringExtra("ngo_address"));
        tvLicense.setText(intent.getStringExtra("ngo_license"));
        tvDescription.setText(intent.getStringExtra("ngo_description"));
        String status = intent.getStringExtra("ngo_status");
        tvStatus.setText(status);

        ivBack.setOnClickListener(v -> finish());

        btnViewCertificate.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(certificateUrl)) {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(certificateUrl));
                startActivity(viewIntent);
            } else {
                Toast.makeText(this, "No certificate uploaded", Toast.LENGTH_SHORT).show();
            }
        });

        btnApprove.setOnClickListener(v -> approveNgo());
        btnReject.setOnClickListener(v -> rejectNgo());
    }

    private void approveNgo() {
        String notes = etNotes.getText().toString().trim();
        networkManager.approveNGO(ngoId, notes, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                Toast.makeText(AdminNGODetailActivity.this, "NGO approved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AdminNGODetailActivity.this, "Approve failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectNgo() {
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
            public void onError(String errorMessage) {
                Toast.makeText(AdminNGODetailActivity.this, "Reject failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

