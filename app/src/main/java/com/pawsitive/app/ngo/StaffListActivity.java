package com.pawsitive.app.ngo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.util.ArrayList;

public class StaffListActivity extends AppCompatActivity implements StaffListAdapter.OnStaffInteractionListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddStaff;
    private ImageView ivBack;

    private StaffListAdapter adapter;
    private NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_list);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        fabAddStaff = findViewById(R.id.fabAddStaff);
        ivBack = findViewById(R.id.ivBack);

        networkManager = new NetworkManager(this);

        adapter = new StaffListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());
        fabAddStaff.setOnClickListener(v -> startActivity(new Intent(StaffListActivity.this, AddStaffActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStaff();
    }

    private void loadStaff() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        networkManager.getStaffList(new NetworkManager.ApiCallback<ApiService.StaffListResponse>() {
            @Override
            public void onSuccess(ApiService.StaffListResponse response) {
                progressBar.setVisibility(View.GONE);
                if (response.data == null || response.data.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    adapter.setStaffList(new ArrayList<>());
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    adapter.setStaffList(response.data);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StaffListActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEdit(ApiService.StaffProfile staff) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Staff");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final Spinner roleSpinner = new Spinner(this);
        String[] roles = new String[]{"Manager", "Volunteer", "Accountant", "Driver"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        roleSpinner.setAdapter(roleAdapter);

        if (staff.staff_role != null) {
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equalsIgnoreCase(staff.staff_role)) {
                    roleSpinner.setSelection(i);
                    break;
                }
            }
        }
        layout.addView(roleSpinner);

        final Switch statusSwitch = new Switch(this);
        statusSwitch.setText("Active");
        statusSwitch.setChecked(staff.status == null || staff.status.equalsIgnoreCase("active"));
        layout.addView(statusSwitch);

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newRole = roleSpinner.getSelectedItem().toString();
                boolean isActive = statusSwitch.isChecked();
                
                progressBar.setVisibility(View.VISIBLE);
                networkManager.updateStaff(staff.id, newRole, isActive, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
                    @Override
                    public void onSuccess(ApiService.BasicResponse response) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(StaffListActivity.this, "Updated successfully", Toast.LENGTH_SHORT).show();
                        loadStaff();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(StaffListActivity.this, "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onDelete(ApiService.StaffProfile staff) {
        progressBar.setVisibility(View.VISIBLE);
        networkManager.deleteStaff(staff.id, new NetworkManager.ApiCallback<ApiService.BasicResponse>() {
            @Override
            public void onSuccess(ApiService.BasicResponse response) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StaffListActivity.this, "Staff deleted", Toast.LENGTH_SHORT).show();
                loadStaff();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(StaffListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}