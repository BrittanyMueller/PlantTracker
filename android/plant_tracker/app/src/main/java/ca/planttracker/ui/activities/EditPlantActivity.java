package ca.planttracker.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.StorageReference;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import ca.planttracker.PlantTrackerClient;
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

        ImageView imageView = findViewById(R.id.plant_image_view);
        Glide.with(getBaseContext())
                .load(existingPlant.getStorageReference())
                .placeholder(R.drawable.plant_placeholder) // Fallback image
                .into(imageView);

        plantNameField.setText(existingPlant.getName());

        // TODO populate form with existing plant info
    }

    @Override
    protected CompletableFuture<Void> handleSubmit() {
        if (existingPlant.getStorageReference() != null && imageUri == null) {
            existingPlant.getStorageReference().delete().addOnSuccessListener(_void -> {
                Log.d("FirebaseStorage", "Image successfully deleted.");
            }).addOnFailureListener((e) -> Log.e("FirebaseStorage", "Failed to delete image", e));
        }
        // Waits for successful firebase upload before proceeding with GRPC
        return uploadImage().thenCompose(this::updatePlant);
    }

    private CompletableFuture<Void> updatePlant(String uploadUrl) {
        // TODO build updated plant and make grpc call
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Boolean> updatePlantGRPC(PlantInfo plant) {
        return CompletableFuture.supplyAsync(() -> PlantTrackerClient.getInstance().addPlant(plant), executorService);
    }

}
