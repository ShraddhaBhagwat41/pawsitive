package com.example.pawsitive.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pawsitive.R
import com.example.pawsitive.ngo.NGORegistrationActivity
import com.example.pawsitive.user.UserProfileActivity

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        val fullName = intent.getStringExtra("full_name") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        val rgRoles: RadioGroup = findViewById(R.id.rgRoles)
        val rbUser: RadioButton = findViewById(R.id.rbUser)
        val rbNgo: RadioButton = findViewById(R.id.rbNgo)
        val btnContinue: Button = findViewById(R.id.btnContinueRole)

        btnContinue.setOnClickListener {
            val selectedId = rgRoles.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedId == rbUser.id) {
                val intent = Intent(this, UserProfileActivity::class.java).apply {
                    putExtra("full_name", fullName)
                    putExtra("email", email)
                }
                startActivity(intent)
            } else if (selectedId == rbNgo.id) {
                val intent = Intent(this, NGORegistrationActivity::class.java).apply {
                    putExtra("email", email)
                }
                startActivity(intent)
            }

            finish()
        }
    }
}
