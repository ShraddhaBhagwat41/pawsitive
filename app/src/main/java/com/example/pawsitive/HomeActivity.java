package com.example.pawsitive;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

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
    private String selectedCategory = "All";
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tokenManager = TokenManager.getInstance(this);

        // Initialize views
        initializeViews();

        // Setup saved pets list
        setupSavedPets();

        // Setup listeners
        setupListeners();

        // Setup Swipe Gestures
        setupSwipeGestures();
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

        // Set All as selected initially
        highlightCategory(ivCatAll);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupSavedPets() {
        savedPetsList = new ArrayList<>();

        // Dogs
        savedPetsList.add(new Pet(
                "Buddy",
                "Golden Retriever",
                "2 years old",
                "Friendly and energetic dog looking for a loving home. Great with kids and other pets.",
                "2.5 km away",
                false,
                R.drawable.pet_buddy
        ));

        savedPetsList.add(new Pet(
                "Max",
                "Labrador",
                "3 years old",
                "Calm and obedient dog. Loves swimming and playing outdoors. Very good with children.",
                "3.2 km away",
                false,
                R.drawable.pet_max
        ));

        // Cats
        savedPetsList.add(new Pet(
                "Whiskers",
                "Orange Tabby",
                "1 year old",
                "Playful kitten who loves to cuddle. Very social and loves attention from humans.",
                "1.8 km away",
                false,
                R.drawable.pet_whiskers
        ));

        savedPetsList.add(new Pet(
                "Luna",
                "Persian Cat",
                "2 years old",
                "Calm and gentle cat who loves to sit on laps. Very quiet and great for apartments.",
                "2.1 km away",
                false,
                R.drawable.pet_luna
        ));

        // Rabbits
        savedPetsList.add(new Pet(
                "Coco",
                "Holland Lop",
                "8 months old",
                "Adorable bunny with floppy ears. Loves to hop around and enjoys fresh vegetables.",
                "4.0 km away",
                false,
                R.drawable.pet_coco
        ));

        savedPetsList.add(new Pet(
                "Snowball",
                "Angora Rabbit",
                "1 year old",
                "Fluffy white rabbit who is very gentle. Easy to handle and great for families.",
                "3.5 km away",
                false,
                R.drawable.pet_snowball
        ));

        rvSavedPets.setLayoutManager(new LinearLayoutManager(this));
        rvSavedPets.setNestedScrollingEnabled(false);
        petAdapter = new PetAdapter(this, savedPetsList);
        rvSavedPets.setAdapter(petAdapter);

        // Ensure the initial view matches the default category
        filterPets(selectedCategory);
    }

    private void setupSwipeGestures() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                // Get the pet from the current filtered list in the adapter
                Pet pet = petAdapter.getPetAt(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    // Right Swipe: Add to Favorites
                    if (!pet.isFavorite()) {
                        pet.setFavorite(true);
                        Toast.makeText(HomeActivity.this, pet.getName() + " added to favorites!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HomeActivity.this, pet.getName() + " is already in favorites", Toast.LENGTH_SHORT).show();
                    }
                } else if (direction == ItemTouchHelper.LEFT) {
                    // Left Swipe: Remove from Favorites
                    if (pet.isFavorite()) {
                        pet.setFavorite(false);
                        Toast.makeText(HomeActivity.this, pet.getName() + " removed from favorites", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(HomeActivity.this, pet.getName() + " is not in favorites", Toast.LENGTH_SHORT).show();
                    }
                }

                // Refresh the item to show updated heart icon and reset swipe position
                petAdapter.notifyItemChanged(position);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvSavedPets);
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

        // Profile click - Show Logout Option
        ivProfile.setOnClickListener(v -> {
            showProfileMenu();
        });
        // Real-time search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                List<Pet> filtered = new ArrayList<>();
                for (Pet pet : savedPetsList) {
                    if (pet.getName().toLowerCase().contains(query) ||
                            pet.getBreed().toLowerCase().contains(query)) {
                        filtered.add(pet);
                    }
                }
                petAdapter.updateList(filtered);
            }
        });

        // Search keyboard enter key
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                return true;
            }
            return false;
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
        btnReportIncident.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                Intent intent = new Intent(HomeActivity.this, ReportAnimalActivity.class);
                startActivity(intent);
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
            }
            return true;
        });

        // Emergency button
        btnEmergency.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();

                // Show emergency options dialog
                new AlertDialog.Builder(this)
                        .setTitle("Emergency Contacts")
                        .setItems(new String[]{
                                "Animal Helpline - 1962",
                                "Police - 100",
                                "Ambulance - 102",
                                "National Emergency - 112"
                        }, (dialog, which) -> {
                            String[] numbers = {"1962", "100", "102", "112"};
                            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                            dialIntent.setData(Uri.parse("tel:" + numbers[which]));
                            startActivity(dialIntent);
                        })
                        .show();

            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
            }
            return true;
        });

        // Add Pet button
        btnAddPet.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                Toast.makeText(this, "Add a new pet", Toast.LENGTH_SHORT).show();
                // TODO: Replace Toast with Intent when AddPetActivity is ready
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
            }
            return true;
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
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_red_dark // matches your app's red theme
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Simulate reloading the pet list
            refreshPetList();
        });
    }

    private void showProfileMenu() {
        String[] options = {"Logout"};
        new AlertDialog.Builder(this)
                .setTitle("Profile")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        logoutUser();
                    }
                })
                .show();
    }

    private void logoutUser() {
        // Clear token from secure storage
        tokenManager.clearToken();

        // Navigate back to LoginActivity and clear back stack
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        if (category.equalsIgnoreCase("All")) {
            petAdapter.updateList(new ArrayList<>(savedPetsList));
            return;
        }

        List<Pet> filtered = new ArrayList<>();
        for (Pet pet : savedPetsList) {
            if (category.equalsIgnoreCase("Dog") &&
                    (pet.getBreed().toLowerCase().contains("retriever") ||
                            pet.getBreed().toLowerCase().contains("labrador") ||
                            pet.getBreed().toLowerCase().contains("dog"))) {
                filtered.add(pet);
            } else if (category.equalsIgnoreCase("Cat") &&
                    (pet.getBreed().toLowerCase().contains("tabby") ||
                            pet.getBreed().toLowerCase().contains("persian") ||
                            pet.getBreed().toLowerCase().contains("cat"))) {
                filtered.add(pet);
            } else if (category.equalsIgnoreCase("Rabbit") &&
                    (pet.getBreed().toLowerCase().contains("lop") ||
                            pet.getBreed().toLowerCase().contains("angora") ||
                            pet.getBreed().toLowerCase().contains("rabbit"))) {
                filtered.add(pet);
            }
        }

        petAdapter.updateList(filtered);
    }

    private void refreshPetList() {
        // Keep track of which pets are currently favorited
        List<String> favoritePetNames = new ArrayList<>();
        for (Pet pet : savedPetsList) {
            if (pet.isFavorite()) {
                favoritePetNames.add(pet.getName());
            }
        }

        // Clear current list
        savedPetsList.clear();

        // Reload all 6 pets
        savedPetsList.add(new Pet("Buddy", "Golden Retriever", "2 years old", "Friendly and energetic dog looking for a loving home.", "2.5 km away", false, R.drawable.pet_buddy));
        savedPetsList.add(new Pet("Max", "Labrador", "3 years old", "Calm and obedient dog. Loves swimming.", "3.2 km away", false, R.drawable.pet_max));
        savedPetsList.add(new Pet("Whiskers", "Orange Tabby", "1 year old", "Playful kitten who loves to cuddle.", "1.8 km away", false, R.drawable.pet_whiskers));
        savedPetsList.add(new Pet("Luna", "Persian Cat", "2 years old", "Calm and gentle cat who loves to sit on laps.", "2.1 km away", false, R.drawable.pet_luna));
        savedPetsList.add(new Pet("Coco", "Holland Lop", "8 months old", "Adorable bunny with floppy ears.", "4.0 km away", false, R.drawable.pet_coco));
        savedPetsList.add(new Pet("Snowball", "Angora Rabbit", "1 year old", "Fluffy white rabbit who is very gentle.", "3.5 km away", false, R.drawable.pet_snowball));

        // Restore favorites
        for (Pet pet : savedPetsList) {
            if (favoritePetNames.contains(pet.getName())) {
                pet.setFavorite(true);
            }
        }

        // Re-apply current category filter
        filterPets(selectedCategory);

        // Stop spinning after 1 second
        swipeRefreshLayout.postDelayed(() -> {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Pet list refreshed!", Toast.LENGTH_SHORT).show();
        }, 1000);
    }
}
