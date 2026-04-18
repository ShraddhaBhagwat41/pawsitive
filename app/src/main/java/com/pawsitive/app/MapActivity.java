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

    private static final double SEARCH_RADIUS_METERS = 5000.0;

    private GoogleMap mMap;
    private double incidentLat;
    private double incidentLng;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        incidentLat = getIntent().getDoubleExtra("incidentLat", 0);
        incidentLng = getIntent().getDoubleExtra("incidentLng", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Map Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (incidentLat == 0 && incidentLng == 0) {
            Toast.makeText(this, "Invalid incident location", Toast.LENGTH_LONG).show();
            return;
        }

        LatLng incidentLocation = new LatLng(incidentLat, incidentLng);

        mMap.addMarker(new MarkerOptions()
                .position(incidentLocation)
                .title("Reported Incident")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.addCircle(new CircleOptions()
                .center(incidentLocation)
                .radius(SEARCH_RADIUS_METERS)
                .strokeColor(0x550000FF)
                .fillColor(0x220000FF));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(incidentLocation, 12f));
        fetchNearbyNGOs(incidentLocation);
    }

    private void fetchNearbyNGOs(LatLng incidentLocation) {
        db.collection("ngo_profiles").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Toast.makeText(this, "Failed to load nearby NGOs", Toast.LENGTH_SHORT).show();
                return;
            }

            int ngoNotifiedCount = 0;
            for (QueryDocumentSnapshot document : task.getResult()) {
                String name = document.getString("organization_name");
                Double lat = document.getDouble("latitude");
                Double lng = document.getDouble("longitude");

                if ((lat == null || lng == null) || (lat == 0.0 && lng == 0.0)) {
                    Double[] derived = geocodeAddress(document.getString("address"));
                    lat = derived[0];
                    lng = derived[1];
                }

                if (lat == null || lng == null) {
                    continue;
                }

                float[] results = new float[1];
                Location.distanceBetween(incidentLocation.latitude, incidentLocation.longitude, lat, lng, results);
                float distanceInMeters = results[0];

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
            // Ignore; we fall back to skipping that NGO marker.
        }

        return new Double[]{null, null};
    }
}

