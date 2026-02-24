package com.example.pawsitive;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ImageView ivMenu, ivNotification, ivProfile, ivFilter;
    private ImageView ivCatAll, ivCatDog, ivCatCat, ivCatRabbit;
    private EditText etSearch;
    private TextView tvViewAll;
    private RecyclerView rvSavedPets;
    private Button btnReportIncident, btnEmergency, btnAddPet;
    private BottomNavigationView bottomNavigation;

    private PetAdapter petAdapter;
    private List<Pet> savedPetsList;
    private String selectedCategory = "Dog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views
        initializeViews();

        // Setup saved pets list
        setupSavedPets();

        // Setup listeners
        setupListeners();
    }

    private void initializeViews() {
        ivMenu = findViewById(R.id.ivMenu);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivFilter = findViewById(R.id.ivFilter);

        ivCatAll = findViewById(R.id.ivCatAll);
        ivCatDog = findViewById(R.id.ivCatDog);
        ivCatCat = findViewById(R.id.ivCatCat);
        ivCatRabbit = findViewById(R.id.ivCatRabbit);

        etSearch = findViewById(R.id.etSearch);
        tvViewAll = findViewById(R.id.tvViewAll);
        rvSavedPets = findViewById(R.id.rvSavedPets);

        btnReportIncident = findViewById(R.id.btnReportIncident);
        btnEmergency = findViewById(R.id.btnEmergency);
        btnAddPet = findViewById(R.id.btnAddPet);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Set Dog as selected initially
        highlightCategory(ivCatDog);
    }

    private void setupSavedPets() {
        savedPetsList = new ArrayList<>();

        // Sample data
        savedPetsList.add(new Pet(
                "Buddy",
                "Golden Retriever",
                "2 years old",
                "Friendly and energetic dog looking for a loving home. Great with kids and other pets.",
                "2.5 km away",
                false));

        savedPetsList.add(new Pet(
                "Whiskers",
                "Orange Tabby",
                "1 year old",
                "Playful kitten who loves to cuddle. Very social and loves attention from humans.",
                "1.8 km away",
                false));

        rvSavedPets.setLayoutManager(new LinearLayoutManager(this));
        rvSavedPets.setNestedScrollingEnabled(false);
        petAdapter = new PetAdapter(this, savedPetsList);
        rvSavedPets.setAdapter(petAdapter);
    }

    private void setupListeners() {
        // Menu click
        ivMenu.setOnClickListener(v -> {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
            // TODO: Open navigation drawer
        });

        // Notification click
        ivNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
            // TODO: Open notifications
        });

        // Profile click
        ivProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
            // TODO: Open profile
        });

        // Filter click
        ivFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter options", Toast.LENGTH_SHORT).show();
            // TODO: Open filter dialog
        });

        // Category clicks
        ivCatAll.setOnClickListener(v -> {
            selectedCategory = "All";
            highlightCategory(ivCatAll);
            filterPets("All");
        });

        ivCatDog.setOnClickListener(v -> {
            selectedCategory = "Dog";
            highlightCategory(ivCatDog);
            filterPets("Dog");
        });

        ivCatCat.setOnClickListener(v -> {
            selectedCategory = "Cat";
            highlightCategory(ivCatCat);
            filterPets("Cat");
        });

        ivCatRabbit.setOnClickListener(v -> {
            selectedCategory = "Rabbit";
            highlightCategory(ivCatRabbit);
            filterPets("Rabbit");
        });

        // View All click
        tvViewAll.setOnClickListener(v -> {
            Toast.makeText(this, "View all saved pets", Toast.LENGTH_SHORT).show();
            // TODO: Open full saved pets list
        });

        // Report Incident button
        btnReportIncident.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReportAnimalActivity.class);
            startActivity(intent);
        });

        // Emergency button
        btnEmergency.setOnClickListener(v -> {
            Toast.makeText(this, "Emergency Services", Toast.LENGTH_SHORT).show();
            // TODO: Open emergency contacts or call
        });

        // Add Pet button
        btnAddPet.setOnClickListener(v -> {
            Toast.makeText(this, "Add a new pet", Toast.LENGTH_SHORT).show();
            // TODO: Open add pet form
        });

        // Bottom navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_favorites) {
                Toast.makeText(this, "Favorites", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_messages) {
                Toast.makeText(this, "Messages", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_community) {
                Toast.makeText(this, "Community", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void highlightCategory(ImageView selectedView) {
        // Reset all categories to default
        ivCatAll.setBackgroundResource(R.drawable.category_unselected);
        ivCatDog.setBackgroundResource(R.drawable.category_unselected);
        ivCatCat.setBackgroundResource(R.drawable.category_unselected);
        ivCatRabbit.setBackgroundResource(R.drawable.category_unselected);

        // Highlight selected
        selectedView.setBackgroundResource(R.drawable.category_selected);
    }

    private void filterPets(String category) {
        Toast.makeText(this, "Showing " + category + " pets", Toast.LENGTH_SHORT).show();
        // TODO: Filter pets list based on category
    }
}
