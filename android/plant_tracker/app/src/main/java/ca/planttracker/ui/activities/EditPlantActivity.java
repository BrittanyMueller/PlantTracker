package ca.planttracker.ui.activities;

import android.content.Context;
import android.os.Bundle;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ca.planttracker.R;
import ca.planttracker.data.models.Plant;
import planttracker.server.PlantInfo;

public class EditPlantActivity extends PlantFormActivity {
    private Plant existingPlant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initCustomToolbar(false, getString(R.string.edit_plant), Optional.empty());

        existingPlant = (Plant) getIntent().getSerializableExtra("plant");

        // TODO populate form with existing plant info
    }

    @Override
    protected CompletableFuture<Void> handleSubmit(String uploadUrl) {
        return updatePlant(uploadUrl);
    }

    private CompletableFuture<Void> updatePlant(String uploadUrl) {
        // TODO
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Boolean> updatePlantGRPC(PlantInfo plant) {
        return CompletableFuture.supplyAsync(() -> PlantTrackerClient.getInstance().addPlant(plant), executorService);
    }

}
