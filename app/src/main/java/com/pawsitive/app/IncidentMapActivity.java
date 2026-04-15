package com.pawsitive.app;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncidentMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "IncidentMapActivity";
    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();

    private GoogleMap mMap;
    private double incidentLat, incidentLng;
    private TextView tvNgoStatus;
    private TextView tvNgoDebugStats;
    private TextView tvNoNgos;
    private RecyclerView rvNotifiedNgos;
    private NotifiedNgoAdapter notifiedNgoAdapter;
    private FirebaseFirestore db;

    // Use a slightly larger radius to avoid false negatives due to geocoding variance.
    private static final double NOTIFY_RADIUS_METERS = 7000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_map);

        db = FirebaseFirestore.getInstance();
        tvNgoStatus = findViewById(R.id.tvNgoStatus);
        tvNgoDebugStats = findViewById(R.id.tvNgoDebugStats);
        tvNoNgos = findViewById(R.id.tvNoNgos);
        rvNotifiedNgos = findViewById(R.id.rvNotifiedNgos);

        notifiedNgoAdapter = new NotifiedNgoAdapter(this);
        rvNotifiedNgos.setLayoutManager(new LinearLayoutManager(this));
        rvNotifiedNgos.setAdapter(notifiedNgoAdapter);

        Button btnDone = findViewById(R.id.btnDone);

        incidentLat = getIntent().getDoubleExtra("lat", 0);
        incidentLng = getIntent().getDoubleExtra("lng", 0);

        // Guard against missing/invalid incident coordinates to avoid weird map behaviour
        if (incidentLat == 0 && incidentLng == 0) {
            Toast.makeText(this, "Invalid incident location. Please try reporting again.", Toast.LENGTH_LONG).show();
            tvNgoStatus.setText("Invalid incident location.");
            tvNgoDebugStats.setText("Fetched 0 NGOs, geocoded 0, in-range 0");
            finish();
            return;
        }

        try {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Toast.makeText(this, "Map fragment missing", Toast.LENGTH_LONG).show();
            }
        } catch (RuntimeException e) {
            // If Google Play services / Maps is not available or misconfigured, avoid crashing.
            Toast.makeText(this, "Map failed to initialize: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        btnDone.setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap == null) {
            Toast.makeText(this, "Google Map unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        mMap = googleMap;

        LatLng incident = new LatLng(incidentLat, incidentLng);
        
        // Add incident marker (Red)
        mMap.addMarker(new MarkerOptions()
                .position(incident)
                .title("Reported Incident")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Draw 5km radius circle
        mMap.addCircle(new CircleOptions()
                .center(incident)
                .radius(NOTIFY_RADIUS_METERS)
                .strokeColor(Color.RED)
                .fillColor(0x22FF0000)
                .strokeWidth(2));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(incident, 13f));

        try {
            fetchNearbyNGOs();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Failed to load NGOs: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void fetchNearbyNGOs() {
        db.collection("ngo_profiles").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<QueryDocumentSnapshot> docs = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    docs.add(document);
                }

                if (!docs.isEmpty()) {
                    // Geocoding may use network on some devices; keep it off the main thread.
                    geoExecutor.execute(() -> processNgoDocsInBackground(docs, "ngo_profiles"));
                    return;
                }

                // Backward-compat fallback: some datasets may still use `ngos` collection.
                db.collection("ngos").get().addOnCompleteListener(fallbackTask -> {
                    if (fallbackTask.isSuccessful()) {
                        List<QueryDocumentSnapshot> fallbackDocs = new ArrayList<>();
                        for (QueryDocumentSnapshot document : fallbackTask.getResult()) {
                            fallbackDocs.add(document);
                        }
                        geoExecutor.execute(() -> processNgoDocsInBackground(fallbackDocs, "ngos"));
                    } else {
                        String msg = "Failed to load nearby NGOs";
                        if (fallbackTask.getException() != null && fallbackTask.getException().getMessage() != null) {
                            msg += ": " + fallbackTask.getException().getMessage();
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        tvNoNgos.setVisibility(View.VISIBLE);
                        tvNgoStatus.setText("Unable to fetch NGOs right now.");
                        tvNgoDebugStats.setText("Fetched 0 NGOs, geocoded 0, in-range 0");
                    }
                });
            }
            else {
                String msg = "Failed to load nearby NGOs";
                if (task.getException() != null && task.getException().getMessage() != null) {
                    msg += ": " + task.getException().getMessage();
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                tvNoNgos.setVisibility(View.VISIBLE);
                tvNgoStatus.setText("Unable to fetch NGOs right now.");
                tvNgoDebugStats.setText("Fetched 0 NGOs, geocoded 0, in-range 0");
            }
        });
    }

    private void processNgoDocsInBackground(List<QueryDocumentSnapshot> docs, String sourceCollection) {
        List<NotifiedNgo> inRange = new ArrayList<>();
        int fetchedCount = docs != null ? docs.size() : 0;
        int geocodedCount = 0;
        for (QueryDocumentSnapshot document : docs) {
            String docId = document.getId();
            String name = document.getString("organization_name");
            String phone = document.getString("phone");
            Double ngoLat = document.getDouble("latitude");
            Double ngoLng = document.getDouble("longitude");

            // If coordinates are missing, derive them from address.
            if ((ngoLat == null || ngoLng == null) || (ngoLat == 0.0 && ngoLng == 0.0)) {
                String address = document.getString("address");
                Double[] derived = geocodeAddress(address);
                ngoLat = derived[0];
                ngoLng = derived[1];

                // Persist derived coordinates for future fast lookups.
                if (ngoLat != null && ngoLng != null) {
                    geocodedCount++;
                    Map<String, Object> update = new HashMap<>();
                    update.put("latitude", ngoLat);
                    update.put("longitude", ngoLng);
                    db.collection("ngo_profiles").document(docId).update(update)
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to persist NGO lat/lng for " + docId, e));
                }
            }

            if (ngoLat == null || ngoLng == null) continue;

            float[] results = new float[1];
            Location.distanceBetween(incidentLat, incidentLng, ngoLat, ngoLng, results);
            float distanceMeters = results[0];
            Log.d(TAG, "NGO " + (name != null ? name : docId) + " @(" + ngoLat + "," + ngoLng + ") dist=" + distanceMeters + "m");
            if (distanceMeters > NOTIFY_RADIUS_METERS) {
                // Not within radius; don't notify / don't show as nearby.
                continue;
            }

            inRange.add(new NotifiedNgo(
                    name != null ? name : "NGO",
                    phone,
                    ngoLat,
                    ngoLng,
                    distanceMeters
            ));
        }

        Collections.sort(inRange, Comparator.comparingDouble(o -> o.distanceMeters));
        int inRangeCount = inRange.size();
        int finalGeocodedCount = geocodedCount;
        runOnUiThread(() -> renderNgoResults(inRange, fetchedCount, finalGeocodedCount, inRangeCount, sourceCollection));
    }

    private void renderNgoResults(List<NotifiedNgo> inRange, int fetchedCount, int geocodedCount, int inRangeCount, String sourceCollection) {
        if (isFinishing() || isDestroyed()) return;

        try {
            for (NotifiedNgo ngo : inRange) {
                LatLng ngoPos = new LatLng(ngo.latitude, ngo.longitude);
                mMap.addMarker(new MarkerOptions()
                        .position(ngoPos)
                        .title(ngo.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }

            tvNgoDebugStats.setText("Fetched " + fetchedCount + " NGOs, geocoded " + geocodedCount + ", in-range " + inRangeCount + " [" + sourceCollection + "]");

            notifiedNgoAdapter.submitList(inRange);
            if (fetchedCount == 0) {
                tvNoNgos.setVisibility(View.VISIBLE);
                tvNoNgos.setText("No NGO records found in database.");
                tvNgoStatus.setText("No NGOs are available yet.");
            } else if (inRange.isEmpty()) {
                tvNoNgos.setVisibility(View.VISIBLE);
                tvNoNgos.setText("No NGOs found within 7 km.");
                tvNgoStatus.setText("No NGOs found within 7 km.");
            } else {
                tvNoNgos.setVisibility(View.GONE);
                tvNgoStatus.setText(inRange.size() + " nearby NGOs have been notified.");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while rendering NGO results", e);
            tvNoNgos.setVisibility(View.VISIBLE);
            tvNoNgos.setText("Error displaying NGOs on map.");
            tvNgoStatus.setText("Unable to show NGOs due to an error.");
            tvNgoDebugStats.setText("Fetched " + fetchedCount + " NGOs, geocoded " + geocodedCount + ", in-range " + inRangeCount + " [" + sourceCollection + "] (render error)");
        }
    }

    /**
     * Best-effort geocoding for NGO address -> lat/lng.
     * Note: Android Geocoder may return null if network/service unavailable.
     */
    private Double[] geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new Double[]{null, null};
        }

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> results = geocoder.getFromLocationName(address, 1);
            if (results != null && !results.isEmpty()) {
                Address a = results.get(0);
                return new Double[]{a.getLatitude(), a.getLongitude()};
            }
        } catch (IOException | RuntimeException ignored) {
            // Ignore and fallback to null; we simply won't plot that NGO.
        }

        return new Double[]{null, null};
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geoExecutor.shutdownNow();
    }
}
