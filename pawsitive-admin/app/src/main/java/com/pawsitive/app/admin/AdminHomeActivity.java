package com.pawsitive.app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pawsitive.app.LoginActivity;
import com.pawsitive.app.R;
import com.pawsitive.app.TokenManager;
import com.pawsitive.app.network.ApiService;
import com.pawsitive.app.network.NetworkManager;

import java.util.ArrayList;
import java.util.List;

public class AdminHomeActivity extends AppCompatActivity implements NGOListAdapter.OnNgoClickListener, NGOListAdapter.OnListChangeListener {
    
    private NetworkManager networkManager;
    private TokenManager tokenManager;

    private TextView tvTotalNGOs, tvVerifiedNGOs, tvPendingNGOs, tvRejectedNGOs;
    private Button btnTabAll, btnTabPending, btnTabVerified, btnTabRejected;
    private ProgressBar progressBar;
    private View ivLogout;
    private TextView tvEmptyState;
    private RecyclerView recyclerViewAdmin;

    private NGOListAdapter adapter;
    private final List<ApiService.NGOProfile> allNgos = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenManager = TokenManager.getInstance(this);
        if (!tokenManager.isTokenValid()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_home);
        networkManager = new NetworkManager(this);

        tvTotalNGOs = findViewById(R.id.tvTotalNGOs);
        tvVerifiedNGOs = findViewById(R.id.tvVerifiedNGOs);
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        tvPendingNGOs = findViewById(R.id.tvPendingNGOs);
        tvRejectedNGOs = findViewById(R.id.tvRejectedNGOs);
        btnTabAll = findViewById(R.id.btnTabAll);
        btnTabPending = findViewById(R.id.btnTabPending);
        btnTabVerified = findViewById(R.id.btnTabVerified);
        btnTabRejected = findViewById(R.id.btnTabRejected);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerViewAdmin = findViewById(R.id.recyclerViewAdmin);
        ivLogout = findViewById(R.id.ivLogout);

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
        });
        btnTabRejected.setOnClickListener(v -> {
            currentFilter = "REJECTED";
            updateTabStyles();
            applyFilter();
        });

        updateTabStyles();
        refreshDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager.isTokenValid()) {
            refreshDashboard();
        }
    }

    private void refreshDashboard() {
        fetchStats();
        fetchNGOs();
            applyFilter();
public class AdminHomeActivity extends AppCompatActivity {
    }

    private void fetchStats() {
        networkManager.getAdminStats(new NetworkManager.ApiCallback<ApiService.AdminStatsResponse>() {
            @Override
            public void onSuccess(ApiService.AdminStatsResponse response) {
                if (response == null || response.data == null) return;
                tvTotalNGOs.setText(String.valueOf(response.data.totalNGOs));
                tvVerifiedNGOs.setText(String.valueOf(response.data.verifiedNGOs));
                tvPendingNGOs.setText(String.valueOf(response.data.pendingNGOs));
                tvRejectedNGOs.setText(String.valueOf(response.data.rejectedNGOs));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminHomeActivity.this, "Stats error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchNGOs() {
        progressBar.setVisibility(View.VISIBLE);
        networkManager.getAllNGOs(new NetworkManager.ApiCallback<ApiService.NGOListResponse>() {
            @Override
                progressBar.setVisibility(View.GONE);
        // ...existing code... (add admin-specific UI/logic here later)
                allNgos.clear();
                if (response != null && response.data != null) {
                    allNgos.addAll(response.data);
                }
                applyFilter();
            }
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("No NGOs found or failed to load.");
                Toast.makeText(AdminHomeActivity.this, "NGO load error: " + error, Toast.LENGTH_SHORT).show();

            @Override
            }
        });
    }

    private void applyFilter() {
        List<ApiService.NGOProfile> filtered = new ArrayList<>();
        for (ApiService.NGOProfile ngo : allNgos) {
            String status = ngo.verification_status == null ? "" : ngo.verification_status;
            if ("ALL".equals(currentFilter) || currentFilter.equalsIgnoreCase(status)) {
                filtered.add(ngo);
            }
        }
        adapter.updateList(filtered);
        boolean empty = filtered.isEmpty();
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewAdmin.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            tvEmptyState.setText("No NGOs for " + currentFilter.toLowerCase() + " status.");
        }
    }

    private void updateTabStyles() {
        setTabStyle(btnTabAll, "ALL".equals(currentFilter));
        setTabStyle(btnTabPending, "PENDING".equals(currentFilter));
        setTabStyle(btnTabVerified, "VERIFIED".equals(currentFilter));
        setTabStyle(btnTabRejected, "REJECTED".equals(currentFilter));
    }

    private void setTabStyle(Button button, boolean active) {
        button.setBackgroundResource(active ? R.drawable.btn_rounded_yellow : R.drawable.bg_card_rounded);
        button.setTextColor(getResources().getColor(active ? R.color.brown_dark : R.color.gray_dark));
    }

    private void logout() {
        tokenManager.clearToken();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onNgoClicked(ApiService.NGOProfile ngo) {
        Intent intent = new Intent(this, AdminNGODetailActivity.class);
        intent.putExtra("ngo_id", ngo.id);
        intent.putExtra("ngo_name", ngo.organization_name);
        intent.putExtra("ngo_phone", ngo.phone);
        intent.putExtra("ngo_address", ngo.address);
        intent.putExtra("ngo_license", ngo.license_number);
        intent.putExtra("ngo_description", ngo.description);
        intent.putExtra("ngo_status", ngo.verification_status);
        intent.putExtra("ngo_certificate_url", ngo.certificate_url);
        intent.putExtra("ngo_rejection_reason", ngo.rejection_reason);
        startActivity(intent);
    }

    @Override
    public void onListChanged() {
        refreshDashboard();
    }
}
