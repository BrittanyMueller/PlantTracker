package ca.planttracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddPlantActivity extends AppCompatActivity {

    private ExecutorService executorService;
    StorageReference storageReference;
    Uri imageUri;

    Button selectImageBtn;
    ImageView plantImageView;
    EditText plantNameField;
    Slider lightSlider;

    private final ActivityResultLauncher<Intent> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        plantImageView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Toast.makeText(AddPlantActivity.this, "Error selecting image.", Toast.LENGTH_SHORT).show();
                        Log.e("PhotoPicker", Objects.requireNonNull(e.getMessage()));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_plant_activity);

        FirebaseApp.initializeApp(AddPlantActivity.this);
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize threads for async tasks
        // executorService = Executors.newFixedThreadPool(2);
        executorService = Executors.newSingleThreadExecutor();

        ImageView hamburger = findViewById(R.id.hamburger_menu);
        hamburger.setVisibility(View.GONE);
        ImageView backButton = findViewById(R.id.back_arrow);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener((View v) -> {
            finish(); // returns to previous activity
        });


        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getString(R.string.add_plant));

        selectImageBtn = findViewById(R.id.upload_image_button);
        plantImageView = findViewById(R.id.plant_image_view);

        selectImageBtn.setOnClickListener(view -> selectImage());
        plantImageView.setOnClickListener(view -> selectImage());

        plantNameField = findViewById(R.id.plant_name_field);

        lightSlider = findViewById(R.id.light_slider);
        // Set readable labels on light slider
//        lightSlider.setLabelFormatter(value -> {
//            switch ((int) value) {
//                case 0:
//                    return "Low";
//                case 1:
//                    return "Medium";
//                case 2:
//                    return "High";
//                default:
//                    // Impossible, restricted by UI
//                    return "";
//            }
//        });

        Button addPlantSubmit = findViewById(R.id.add_plant_submit);
        addPlantSubmit.setOnClickListener(view -> {
            if (imageUri != null) {
                submitAddPlant();
            }
        });
    }

    private void submitAddPlant() {
        String plantName = plantNameField.getText().toString();
        int lightLevel = (int) lightSlider.getValue();

        // Waits for firebase image upload before proceeding with GRPC
        uploadImage(imageUri)
                .thenCompose(imageUrl -> {
                    Log.d("SubmitAddPlant", "Image upload completed, proceeding to create plant.");
                    return createPlant(plantName, imageUrl, lightLevel);
                })
                .thenRun(() -> runOnUiThread(() -> {
                    Log.d("SubmitAddPlant", "Add plant successful.");
                    Toast.makeText(AddPlantActivity.this, "Add plant successful!", Toast.LENGTH_SHORT).show();
                }))
                .exceptionally(e -> {
                    Log.e("SubmitAddPlant", "Add plant failed: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(AddPlantActivity.this, "Failed to add plant: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    return null;
                });
    }

    private void selectImage() {
        // Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        selectImageLauncher.launch(intent);
    }

    private CompletableFuture<String> uploadImage(Uri imageUri) {
        return CompletableFuture.supplyAsync(() -> {

            String path = "images/" + UUID.randomUUID();
            StorageReference ref = storageReference.child(path);

            CompletableFuture<String> future = new CompletableFuture<>();

            UploadTask uploadTask = ref.putFile(imageUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Upload successful, returns promised image path
                Log.d("UploadImage", "Firebase image upload successful.");
                future.complete(path);
                runOnUiThread(() -> Toast.makeText(AddPlantActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show());
            }).addOnFailureListener(e -> {
                future.completeExceptionally(e);
                Log.e("FirebaseImageUpload", "Image upload failed.", e);
            });
            return future.join();   // Waits for future image url
        }, executorService);
    }

    // TODO what is the best way to pass form data here? Will be a lot of params...
    private CompletableFuture<Void> createPlant(String name, String imageUrl, int lightLevel) {
        return CompletableFuture.runAsync(() -> {
            Log.d("CreatePlant", "Starting GRPC request with imageUrl: ." + imageUrl);
            try {
                // TODO GRPC addPlant request
                Thread.sleep(4000); // Time to stare at toast
                runOnUiThread(() -> Toast.makeText(AddPlantActivity.this, "Sending GRPC request.", Toast.LENGTH_LONG).show());
                Thread.sleep(4000); // Time to stare at toast
            } catch (Exception e) {
                Log.e("AddPlantRequest", "Failed to create new plant.", e);
            }
        }, executorService);
    }
}
