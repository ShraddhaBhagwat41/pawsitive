package com.pawsitive.app;

import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncidentMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "IncidentMapActivity";
    private static final double NOTIFY_RADIUS_METERS = 7000.0;

    private GoogleMap mMap;
    private double incidentLat;
    private double incidentLng;

    private TextView tvNgoStatus;
    private TextView tvNgoDebugStats;
    private TextView tvNoNgos;
    private RecyclerView rvNotifiedNgos;
    private NotifiedNgoAdapter notifiedNgoAdapter;

    private FirebaseFirestore db;
    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_map);

        db = FirebaseFirestore.getInstance();

        tvNgoStatus = findViewById(R.id.tvNgoStatus);
        tvNgoDebugStats = findViewById(R.id.tvNgoDebugStats);
        tvNoNgos = findViewById(R.id.tvNoNgos);
        rvNotifiedNgos = findViewById(R.id.rvNotifiedNgos);
        Button btnDone = findViewById(R.id.btnDone);

        notifiedNgoAdapter = new NotifiedNgoAdapter(this);
        rvNotifiedNgos.setLayoutManager(new LinearLayoutManager(this));
        rvNotifiedNgos.setAdapter(notifiedNgoAdapter);

        incidentLat = getIntent().getDoubleExtra("lat", 0);
        incidentLng = getIntent().getDoubleExtra("lng", 0);

        btnDone.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Map fragment missing", Toast.LENGTH_LONG).show();
            tvNgoStatus.setText("Map unavailable.");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (incidentLat == 0 && incidentLng == 0) {
            tvNgoStatus.setText("Invalid incident location.");
            tvNgoDebugStats.setText("Fetched 0 NGOs, geocoded 0, in-range 0");
            Toast.makeText(this, "Invalid incident location. Please report again.", Toast.LENGTH_LONG).show();
            return;
        }

        LatLng incident = new LatLng(incidentLat, incidentLng);

        mMap.addMarker(new MarkerOptions()
                .position(incident)
                .title("Reported Incident")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.addCircle(new CircleOptions()
                .center(incident)
                .radius(NOTIFY_RADIUS_METERS)
                .strokeColor(Color.RED)
                .fillColor(0x22FF0000)
                .strokeWidth(2f));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(incident, 13f));

        fetchNearbyNGOs();
    }

    private void fetchNearbyNGOs() {
        db.collection("ngo_profiles").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                handleNgoFetchFailure(task.getException());
                return;
            }

            List<QueryDocumentSnapshot> docs = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
                docs.add(document);
            }

            if (!docs.isEmpty()) {
                geoExecutor.execute(() -> processNgoDocsInBackground(docs, "ngo_profiles"));
                return;
            }

            db.collection("ngos").get().addOnCompleteListener(fallbackTask -> {
                if (!fallbackTask.isSuccessful()) {
                    handleNgoFetchFailure(fallbackTask.getException());
                    return;
                }

                List<QueryDocumentSnapshot> fallbackDocs = new ArrayList<>();
                for (QueryDocumentSnapshot document : fallbackTask.getResult()) {
                    fallbackDocs.add(document);
                }
                geoExecutor.execute(() -> processNgoDocsInBackground(fallbackDocs, "ngos"));
            });
        });
    }

    private void handleNgoFetchFailure(Exception exception) {
        String message = "Failed to load nearby NGOs";
        if (exception != null && exception.getMessage() != null) {
            message += ": " + exception.getMessage();
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        tvNoNgos.setVisibility(View.VISIBLE);
        tvNgoStatus.setText("Unable to fetch NGOs right now.");
        tvNgoDebugStats.setText("Fetched 0 NGOs, geocoded 0, in-range 0");
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

            if ((ngoLat == null || ngoLng == null) || (ngoLat == 0.0 && ngoLng == 0.0)) {
                String address = document.getString("address");
                Double[] derived = geocodeAddress(address);
                ngoLat = derived[0];
                ngoLng = derived[1];

                if (ngoLat != null && ngoLng != null) {
                    geocodedCount++;
                    Map<String, Object> update = new HashMap<>();
                    update.put("latitude", ngoLat);
                    update.put("longitude", ngoLng);
                    db.collection(sourceCollection).document(docId).update(update)
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to persist NGO lat/lng for " + docId, e));
                }
            }

            if (ngoLat == null || ngoLng == null) {
                continue;
            }

            float[] results = new float[1];
            Location.distanceBetween(incidentLat, incidentLng, ngoLat, ngoLng, results);
            float distanceMeters = results[0];
            if (distanceMeters > NOTIFY_RADIUS_METERS) {
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

        Collections.sort(inRange, Comparator.comparingDouble(item -> item.distanceMeters));
        int finalGeocodedCount = geocodedCount;
        int inRangeCount = inRange.size();

        runOnUiThread(() -> renderNgoResults(inRange, fetchedCount, finalGeocodedCount, inRangeCount, sourceCollection));
    }

    private void renderNgoResults(List<NotifiedNgo> inRange, int fetchedCount, int geocodedCount, int inRangeCount, String sourceCollection) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        for (NotifiedNgo ngo : inRange) {
            if (mMap == null) {
                break;
            }
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
            return;
        }

        if (inRange.isEmpty()) {
            tvNoNgos.setVisibility(View.VISIBLE);
            tvNoNgos.setText("No NGOs found within 7 km.");
            tvNgoStatus.setText("No NGOs found within 7 km.");
            return;
        }

        tvNoNgos.setVisibility(View.GONE);
        tvNgoStatus.setText(inRange.size() + " nearby NGOs have been notified.");
    }

    private Double[] geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new Double[]{null, null};
        }

        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> results = geocoder.getFromLocationName(address, 1);
            if (results != null && !results.isEmpty()) {
                Address first = results.get(0);
                return new Double[]{first.getLatitude(), first.getLongitude()};
            }
        } catch (IOException | RuntimeException ignored) {
            // If geocoding fails, NGO remains unplotted unless stored coordinates exist.
        }

        return new Double[]{null, null};
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geoExecutor.shutdownNow();
    }
}