package ca.planttracker.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;

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
    private static final String TAG = EditPlantActivity.class.getSimpleName();
    private Plant existingPlant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Must get called before on create
        existingPlant = (Plant) getIntent().getSerializableExtra("plant");

        super.onCreate(savedInstanceState);
        initCustomToolbar(false, getString(R.string.edit_plant), Optional.empty());

        Glide.with(getBaseContext())
                .load(existingPlant.getStorageReference())
                .placeholder(R.drawable.plant_placeholder) // Fallback image
                .into(plantImageView);

        plantNameField.setText(existingPlant.getName());
        lightSlider.setValue((float)existingPlant.getLightLevel().getNumber());
        moistureSlider.setValue((float)existingPlant.getMinMoisture());
        humiditySlider.setValue((float)existingPlant.getMinHumidity());
    }

    @Override
    public List<Pi> getAvailablePiSensors() {
        PlantTrackerClient client = PlantTrackerClient.getInstance();
        return client.getAvailablePiSensors(Optional.of(existingPlant.getId()));
    }

    @Override
    protected CompletableFuture<Void> handleSubmit() {
        // imageUri null means local image ref, meaning they selected an image from picker to upload
        if (existingPlant.getStorageReference() != null && imageUri != null) {
            Log.d(TAG, "Deleting stored image.");
            existingPlant.getStorageReference().delete().addOnSuccessListener(_void -> {
                Log.d(TAG, "Image successfully deleted.");
            }).addOnFailureListener((e) -> Log.e(TAG, "Failed to delete image.", e));
        }
        // Waits for successful firebase upload before proceeding with GRPC
        return uploadImage().thenCompose(this::updatePlant);
    }

    @Override
    protected void setPiDropdown() {
        super.setPiDropdown();

        selectedPi = null;
        for (Pi pi : piList) {
            if (pi.getId() == existingPlant.getPid()) {
                selectedPi = pi;
                break;
            }
        }
        if (selectedPi == null) {
            Log.e(TAG, "Failed to find existing Pi for plant " + existingPlant.getId());
            return;
        }
        setDeviceDropdown();

        selectedDevice = null;
        for (MoistureDevice dev : selectedPi.getMoistureDevices()) {
            if (dev.getId() == existingPlant.getMoistureDeviceId()) {
                selectedDevice = dev;
                break;
            }
        }
        if (selectedDevice == null) {
            Log.e(TAG, "Failed to find existing Moisture Device for plant " + existingPlant.getId());
            return;
        }
        selectedPort = existingPlant.getSensorPort();
        setPortDropdown();

        // Show selected items in dropdown
        piTextView.setText(selectedPi.getName(), false);
        deviceTextView.setText(selectedDevice.getName(), false);
        portTextView.setText(String.valueOf(selectedPort), false);
    }

    private CompletableFuture<Void> updatePlant(String uploadUrl) {

        PlantInfo.Builder plant = PlantInfo.newBuilder()
                .setId(existingPlant.getId())
                .setName(plantNameField.getText().toString())
                .setImageUrl(uploadUrl != null ? uploadUrl : existingPlant.getImageUrl())
                .setLightLevelValue((int) lightSlider.getValue())
                .setMinMoisture((int) moistureSlider.getValue())
                .setMinHumidity((int) humiditySlider.getValue())
                .setPid(selectedPi.getId())
                .setMoistureDeviceId(selectedDevice.getId())
                .setSensorPort(selectedPort);

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
