package com.pawsitive.app;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private double incidentLat;
    private double incidentLng;
    private FirebaseFirestore db;

    // Radius for drawing circle and filtering NGOs (in meters)
    private static final double SEARCH_RADIUS_METERS = 5000.0; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: Make sure to create activity_map.xml with a fragment id="map" 
        // using com.google.android.gms.maps.SupportMapFragment
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();

        incidentLat = getIntent().getDoubleExtra("incidentLat", 0);
        incidentLng = getIntent().getDoubleExtra("incidentLng", 0);

        // Required Google Maps setup
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Map Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng incidentLocation = new LatLng(incidentLat, incidentLng);

        // Add marker for incident reported
        mMap.addMarker(new MarkerOptions()
                .position(incidentLocation)
                .title("Reported Incident")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Draw a radius circle around the incident (5 km)
        mMap.addCircle(new CircleOptions()
                .center(incidentLocation)
                .radius(SEARCH_RADIUS_METERS)
                .strokeWidth(2f)
                .strokeColor(0x550000FF)
                .fillColor(0x220000FF));

        // Center map to incident location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(incidentLocation, 12f));

        fetchNearbyNGOs(incidentLocation);
    }

    /**
     * Fetch NGOs from Firestore and plot them on the map.
     */
    private void fetchNearbyNGOs(LatLng incidentLocation) {
        // IMPORTANT: In this project, NGOs are stored under the "ngo_profiles" collection
        // (see NGORegistrationActivity / IncidentMapActivity). Querying "ngos" will fail or return 0.
        db.collection("ngo_profiles").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                String msg = "Failed to load nearby NGOs";
                if (task.getException() != null && task.getException().getMessage() != null) {
                    msg += ": " + task.getException().getMessage();
                } else {
                    msg += ".";
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                return;
            }

            int ngoNotifiedCount = 0;

            for (QueryDocumentSnapshot document : task.getResult()) {
                // Project schema:
                //  - organization_name
                //  - latitude
                //  - longitude
                //  - address
                String name = document.getString("organization_name");
                Double lat = document.getDouble("latitude");
                Double lng = document.getDouble("longitude");

                // If coordinates are missing (older records), derive them from address.
                if ((lat == null || lng == null) || (lat == 0.0 && lng == 0.0)) {
                    String address = document.getString("address");
                    Double[] derived = geocodeAddress(address);
                    lat = derived[0];
                    lng = derived[1];
                }

                if (lat == null || lng == null) continue;

                float[] results = new float[1];
                Location.distanceBetween(
                        incidentLocation.latitude, incidentLocation.longitude,
                        lat, lng,
                        results
                );

                float distanceInMeters = results[0];

                // If within 5km, add a marker for this NGO
                if (distanceInMeters <= SEARCH_RADIUS_METERS) {
                    LatLng ngoLatLng = new LatLng(lat, lng);
                    mMap.addMarker(new MarkerOptions()
                            .position(ngoLatLng)
                            .title(name != null ? name : "NGO")
                            .snippet("Distance: " + (int) (distanceInMeters / 1000) + " km")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                    ngoNotifiedCount++;
                }
            }

            Toast.makeText(this, "Notified " + ngoNotifiedCount + " NGOs nearby", Toast.LENGTH_LONG).show();
        });
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
        } catch (IOException | IllegalArgumentException ignored) {
            // Ignore and fallback to null; we simply won't plot that NGO.
        }

        return new Double[]{null, null};
    }
}

