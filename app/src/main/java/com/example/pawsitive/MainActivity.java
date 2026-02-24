package com.example.pawsitive;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView ivMap;
    private RecyclerView rvResponders;
    private ResponderAdapter responderAdapter;
    private List<Responder> responderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        ivMap = findViewById(R.id.ivMap);
        rvResponders = findViewById(R.id.rvResponders);

        // Setup RecyclerView
        rvResponders.setLayoutManager(new LinearLayoutManager(this));
        rvResponders.setNestedScrollingEnabled(false);

        // Initialize responder data
        initResponderData();

        // Setup adapter
        responderAdapter = new ResponderAdapter(this, responderList);
        rvResponders.setAdapter(responderAdapter);

        // Map click listener (for future Google Maps integration)
        ivMap.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Map clicked - Integrate Google Maps here", Toast.LENGTH_SHORT).show();
        });
    }

    private void initResponderData() {
        responderList = new ArrayList<>();
        // Sample data - Replace with actual data from API/Database
        responderList.add(new Responder("Responder 1", "Notification Send", true));
        responderList.add(new Responder("Responder 2", "Notification Send", true));
        responderList.add(new Responder("Responder 3", "Notification Pending", false));
    }
}
