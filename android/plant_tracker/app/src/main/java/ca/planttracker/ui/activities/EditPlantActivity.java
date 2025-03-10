package ca.planttracker.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ca.planttracker.PlantTrackerClient;
import ca.planttracker.R;
import ca.planttracker.data.models.MoistureDevice;
import ca.planttracker.data.models.Pi;
import ca.planttracker.data.models.Plant;
import planttracker.server.PlantInfo;

public class EditPlantActivity extends PlantFormActivity {
    private Plant existingPlant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initCustomToolbar(false, getString(R.string.edit_plant), Optional.empty());

        existingPlant = (Plant) getIntent().getSerializableExtra("plant");

        ImageView imageView = findViewById(R.id.plant_image_view);
        Glide.with(getBaseContext())
                .load(existingPlant.getStorageReference())
                .placeholder(R.drawable.plant_placeholder) // Fallback image
                .into(imageView);

        plantNameField.setText(existingPlant.getName());


        // TODO populate form with existing plant info
        // TODO pass list of available pi sensors, including currently selected
    }

    @Override
    public List<Pi> getAvailablePiSensors() {
        PlantTrackerClient client = PlantTrackerClient.getInstance();
        return client.getAvailablePiSensors(Optional.of(existingPlant.getId()));
    }

    @Override
    protected CompletableFuture<Void> handleSubmit() {
        if (existingPlant.getStorageReference() != null && imageUri == null) {
            existingPlant.getStorageReference().delete().addOnSuccessListener(_void -> {
                Log.d("FirebaseStorage", "Image successfully deleted.");
            }).addOnFailureListener((e) -> Log.e("FirebaseStorage", "Failed to delete image.", e));
        }
        // Waits for successful firebase upload before proceeding with GRPC
        return uploadImage().thenCompose(this::updatePlant);
    }

    private CompletableFuture<Void> updatePlant(String uploadUrl) {
        PlantInfo.Builder plant = PlantInfo.newBuilder()
                .setName(plantNameField.getText().toString())
                .setLightLevelValue((int) lightSlider.getValue())
                .setMinMoisture((int) moistureSlider.getValue())
                .setMinHumidity((int) humiditySlider.getValue())
                .setPid(selectedPi.getId())
                .setMoistureDeviceId(selectedDevice.getId())
                .setSensorPort(selectedPort);

        if (uploadUrl != null) {
            plant.setImageUrl(uploadUrl);
        }

        return updatePlantGRPC(plant.build()).thenAccept(success -> runOnUiThread(() -> {
            if (success) {
                Log.d("SubmitEditPlant", "Update plant successful.");
                Toast.makeText(EditPlantActivity.this, "Update plant successful!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();   // TODO return RESULT_OK to view plants activity
            } else {
                requestInProgress = false;
                Log.d("SubmitEditPlant", "Update plant failed.");
                Toast.makeText(EditPlantActivity.this, "Update plant failed.", Toast.LENGTH_LONG).show();
            }
        })).exceptionally(e -> {
            requestInProgress = false;
            Log.e("SubmitEditPlant", "Update plant failed exceptionally: " + e.getMessage(), e);
            runOnUiThread(() -> Toast.makeText(EditPlantActivity.this, "Failed to save plant changes. Check your network connection.", Toast.LENGTH_LONG).show());
            return null;
        });
    }

    private CompletableFuture<Boolean> updatePlantGRPC(PlantInfo plant) {
        return CompletableFuture.supplyAsync(() -> PlantTrackerClient.getInstance().updatePlant(plant), executorService);
    }

}
