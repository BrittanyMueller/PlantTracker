package ca.planttracker.ui.activities;


import android.content.Context;
import android.os.Bundle;

import android.util.Log;

import android.widget.Toast;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ca.planttracker.R;
import planttracker.server.PlantInfo;


public class AddPlantActivity extends PlantFormActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initCustomToolbar(false, getString(R.string.add_plant), Optional.empty());
    }

    @Override
    protected CompletableFuture<Void> handleSubmit(String uploadUrl) {
        return addPlant(uploadUrl);
    }

    private CompletableFuture<Void> addPlant(String uploadUrl) {

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

        return createPlantGRPC(plant.build()).thenAccept(success -> runOnUiThread(() -> {
            if (success) {
                Log.d("SubmitAddPlant", "Add plant successful.");
                Toast.makeText(AddPlantActivity.this, "Add plant successful!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();   // TODO return RESULT_OK to view plants activity
            } else {
                requestInProgress = false;
                Log.d("SubmitAddPlant", "Add plant failed.");
                Toast.makeText(AddPlantActivity.this, "Add plant failed.", Toast.LENGTH_LONG).show();
            }
            })).exceptionally(e -> {
                requestInProgress = false;
                Log.e("SubmitAddPlant", "Add plant failed exceptionally: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(AddPlantActivity.this, "Add plant failed. Check your network connection.", Toast.LENGTH_LONG).show());
                return null;
            });
        }

    private CompletableFuture<Boolean> createPlantGRPC(PlantInfo plant) {
        return CompletableFuture.supplyAsync(() -> PlantTrackerClient.getInstance().addPlant(plant), executorService);
    }



}



