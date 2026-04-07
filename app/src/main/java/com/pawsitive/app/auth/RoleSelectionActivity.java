package com.pawsitive.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pawsitive.app.R;
import com.pawsitive.app.ngo.NGORegistrationActivity;
import com.pawsitive.app.user.UserProfileActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    private EditText etFullName;
    private RadioGroup rgRoles;
    private RadioButton rbUser, rbNgo;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        etFullName = findViewById(R.id.etFullName);
        rgRoles = findViewById(R.id.rgRoles);
        rbUser = findViewById(R.id.rbUser);
        rbNgo = findViewById(R.id.rbNgo);
        btnContinue = findViewById(R.id.btnContinueRole);

        // Get credentials from SignupActivity
        String email = getIntent().getStringExtra("email");
        String password = getIntent().getStringExtra("password");

        btnContinue.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            int selectedId = rgRoles.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Full name is required");
                etFullName.requestFocus();
                return;
            }

            if (selectedId == -1) {
                Toast.makeText(RoleSelectionActivity.this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent;
            if (selectedId == R.id.rbUser) {
                intent = new Intent(RoleSelectionActivity.this, UserProfileActivity.class);
            } else if (selectedId == R.id.rbNgo) {
                intent = new Intent(RoleSelectionActivity.this, NGORegistrationActivity.class);
            } else {
                return;
            }
            
            intent.putExtra("full_name", fullName);
            intent.putExtra("email", email);
            intent.putExtra("password", password);
            startActivity(intent);
            finish();
        });
    }
}