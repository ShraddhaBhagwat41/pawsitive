package com.pawsitive.app.ngo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

public class AddStaffActivity extends AppCompatActivity {

    private EditText etStaffName, etStaffEmail, etStaffPhone, etStaffPassword;
    private Button btnAddStaff;
    private ProgressBar progressBar;

    private NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        networkManager = new NetworkManager(this);

        etStaffName = findViewById(R.id.etStaffName);
        etStaffEmail = findViewById(R.id.etStaffEmail);
        etStaffPhone = findViewById(R.id.etStaffPhone);
        etStaffPassword = findViewById(R.id.etStaffPassword);
        btnAddStaff = findViewById(R.id.btnAddStaff);
        progressBar = findViewById(R.id.progressBar);

        btnAddStaff.setOnClickListener(v -> addStaff());
    }

    private void addStaff() {
        String name = etStaffName.getText().toString().trim();
        String email = etStaffEmail.getText().toString().trim();
        String phone = etStaffPhone.getText().toString().trim();
        String password = etStaffPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(phone)) {
            if (phone.length() == 10 && !phone.startsWith("+")) {
                phone = "+91" + phone;
            } else if (!phone.startsWith("+")) {
                phone = "+" + phone;
            }
        }

        if (TextUtils.isEmpty(name)) {
            etStaffName.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etStaffEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etStaffPassword.setError("Password must be at least 6 characters");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAddStaff.setEnabled(false);

        networkManager.addStaff(email, password, name, phone, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                progressBar.setVisibility(View.GONE);
                btnAddStaff.setEnabled(true);

                Toast.makeText(AddStaffActivity.this, "Staff added successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnAddStaff.setEnabled(true);
                Toast.makeText(AddStaffActivity.this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}

