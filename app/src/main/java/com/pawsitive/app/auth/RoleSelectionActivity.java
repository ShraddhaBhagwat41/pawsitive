package com.pawsitive.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pawsitive.app.R;
import com.pawsitive.app.ngo.NGORegistrationActivity;
import com.pawsitive.app.user.UserProfileActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    private RadioGroup rgRoles;
    private RadioButton rbUser, rbNgo;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        rgRoles = findViewById(R.id.rgRoles);
        rbUser = findViewById(R.id.rbUser);
        rbNgo = findViewById(R.id.rbNgo);
        btnContinue = findViewById(R.id.btnContinueRole);

        // Get extras if any
        String email = getIntent().getStringExtra("email");
        String fullName = getIntent().getStringExtra("full_name");

        btnContinue.setOnClickListener(v -> {
            int selectedId = rgRoles.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(RoleSelectionActivity.this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent;
            if (selectedId == R.id.rbUser) {
                intent = new Intent(RoleSelectionActivity.this, UserProfileActivity.class);
                intent.putExtra("full_name", fullName);
                intent.putExtra("email", email);
            } else if (selectedId == R.id.rbNgo) {
                intent = new Intent(RoleSelectionActivity.this, NGORegistrationActivity.class);
                intent.putExtra("email", email);
            } else {
                return;
            }
            startActivity(intent);
            finish();
        });
    }
}