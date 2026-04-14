package com.pawsitive.app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.LoginActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeActivity extends AppCompatActivity implements NGOListAdapter.OnNgoClickListener, NGOListAdapter.OnListChangeListener {

    private RecyclerView recyclerViewAdmin;
    private NGOListAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private View ivLogout;

    private View cardTotalNGOs, cardVerifiedNGOs, cardPendingNGOs, cardRejectedNGOs;
    private View cardAnimalsPosted, cardAnimalsSaved;

    private Button btnTabAll, btnTabPending, btnTabVerified, btnTabRejected;

    private NetworkManager networkManager;
    private List<ApiService.NGOProfile> allNgos = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        networkManager = new NetworkManager(this);

        recyclerViewAdmin = findViewById(R.id.recyclerViewAdmin);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ivLogout = findViewById(R.id.ivLogout);

        cardTotalNGOs = findViewById(R.id.cardTotalNGOs);
        cardVerifiedNGOs = findViewById(R.id.cardVerifiedNGOs);
        cardPendingNGOs = findViewById(R.id.cardPendingNGOs);
        cardRejectedNGOs = findViewById(R.id.cardRejectedNGOs);
        cardAnimalsPosted = findViewById(R.id.cardAnimalsPosted);
        cardAnimalsSaved = findViewById(R.id.cardAnimalsSaved);

        btnTabAll = findViewById(R.id.btnTabAll);
        btnTabPending = findViewById(R.id.btnTabPending);
        btnTabVerified = findViewById(R.id.btnTabVerified);
        btnTabRejected = findViewById(R.id.btnTabRejected);

        recyclerViewAdmin.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NGOListAdapter(this, new ArrayList<>(), networkManager, this, this);
        recyclerViewAdmin.setAdapter(adapter);

        ivLogout.setOnClickListener(v -> logout());

        btnTabAll.setOnClickListener(v -> {
            currentFilter = "ALL";
            updateTabStyles();
            applyFilter();
        });
        btnTabPending.setOnClickListener(v -> {
            currentFilter = "PENDING";
            updateTabStyles();
            applyFilter();
        });
        btnTabVerified.setOnClickListener(v -> {
            currentFilter = "VERIFIED";
            updateTabStyles();
            applyFilter();
        });
        btnTabRejected.setOnClickListener(v -> {
            currentFilter = "REJECTED";
            updateTabStyles();
            applyFilter();
        });

        updateTabStyles();
        refreshData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        fetchStats();
        fetchNGOs();
    }

    private void fetchStats() {
        networkManager.getAdminStats(new NetworkManager.ApiCallback<ApiService.AdminStatsResponse>() {
            @Override
            public void onSuccess(ApiService.AdminStatsResponse response) {
                if (response == null || response.data == null) return;

                setStat(cardTotalNGOs, "Total NGOs", response.data.totalNGOs);
                setStat(cardVerifiedNGOs, "Verified", response.data.verifiedNGOs);
                setStat(cardPendingNGOs, "Pending", response.data.pendingNGOs);
                setStat(cardRejectedNGOs, "Rejected", response.data.rejectedNGOs);
                setStatSmall(cardAnimalsPosted, "Animals Posted", response.data.totalAnimalsPosts);
                setStatSmall(cardAnimalsSaved, "Saved/Rescued", response.data.animalsSaved);
            }

            @Override
            public void onError(String errorMessage) {
                // Toast.makeText(AdminHomeActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setStat(View card, String label, int value) {
        TextView tvLabel = card.findViewById(R.id.tvStatLabel);
        TextView tvValue = card.findViewById(R.id.tvStatValue);
        if (tvLabel != null) tvLabel.setText(label);
        if (tvValue != null) tvValue.setText(String.valueOf(value));
    }

    private void setStatSmall(View card, String label, int value) {
        TextView tvLabel = card.findViewById(R.id.tvStatLabelSmall);
        TextView tvValue = card.findViewById(R.id.tvStatValueSmall);
        if (tvLabel != null) tvLabel.setText(label);
        if (tvValue != null) tvValue.setText(String.valueOf(value));
    }

    private void fetchNGOs() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewAdmin.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        networkManager.getAllNGOs(new NetworkManager.ApiCallback<ApiService.NGOListResponse>() {
            @Override
            public void onSuccess(ApiService.NGOListResponse response) {
                progressBar.setVisibility(View.GONE);
                allNgos.clear();
                if (response != null && response.data != null) {
                    allNgos.addAll(response.data);
                }
                applyFilter();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("No NGOs to show.");
            }
        });
    }

    private void applyFilter() {
        List<ApiService.NGOProfile> filtered = new ArrayList<>();
        for (ApiService.NGOProfile ngo : allNgos) {
            if ("ALL".equals(currentFilter)) {
                filtered.add(ngo);
            } else if (ngo.verification_status != null && currentFilter.equalsIgnoreCase(ngo.verification_status)) {
                filtered.add(ngo);
            }
        }

        if (filtered.isEmpty()) {
            recyclerViewAdmin.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("No " + currentFilter.toLowerCase() + " NGOs found.");
        } else {
            recyclerViewAdmin.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
        adapter.updateList(filtered);
    }

    private void updateTabStyles() {
        btnTabAll.setAlpha("ALL".equals(currentFilter) ? 1.0f : 0.5f);
        btnTabPending.setAlpha("PENDING".equals(currentFilter) ? 1.0f : 0.5f);
        btnTabVerified.setAlpha("VERIFIED".equals(currentFilter) ? 1.0f : 0.5f);
        btnTabRejected.setAlpha("REJECTED".equals(currentFilter) ? 1.0f : 0.5f);
    }

    @Override
    public void onListChanged() {
        refreshData();
    }

    private void logout() {
        networkManager.clearAuth();
        Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onNgoClicked(ApiService.NGOProfile ngo) {
        Intent intent = new Intent(this, AdminNGODetailActivity.class);
        intent.putExtra("ngo_id", ngo.id);
        intent.putExtra("ngo_name", ngo.organization_name);
        intent.putExtra("ngo_email", ngo.ngo_email);
        intent.putExtra("ngo_phone", ngo.phone);
        intent.putExtra("ngo_address", ngo.address);
        intent.putExtra("ngo_license", ngo.license_number);
        intent.putExtra("ngo_description", ngo.description);
        intent.putExtra("ngo_status", ngo.verification_status);
        intent.putExtra("ngo_rejection_reason", ngo.rejection_reason);
        intent.putExtra("ngo_certificate_url", ngo.certificate_url);
        startActivity(intent);
    }
}
