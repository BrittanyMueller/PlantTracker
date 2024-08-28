package ca.planttracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import planttracker.server.PlantInfo;

public class AddPlantActivity extends AppCompatActivity {

    private ExecutorService executorService;
    StorageReference storageReference;
    Uri imageUri;

    Button selectImageBtn;
    ImageView plantImageView;
    EditText plantNameField;

    // Dropdown selection views
    AutoCompleteTextView piTextView;
    AutoCompleteTextView deviceTextView;
    AutoCompleteTextView portTextView;

    // Selected device data
    Pi selectedPi;
    MoistureDevice selectedDevice;
    int selectedPort;

    Slider lightSlider;
    Slider moistureSlider;
    Slider humiditySlider;

    Button addPlantBtn;

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
        executorService = Executors.newSingleThreadExecutor();

        // Set custom toolbar navigation & title
        ImageView hamburger = findViewById(R.id.hamburger_menu);
        hamburger.setVisibility(View.GONE);
        ImageView backButton = findViewById(R.id.back_arrow);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener((View v) -> {
            finish(); // returns to previous activity
        });
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getString(R.string.add_plant));

        // Initialize ref to form elements
        selectImageBtn = findViewById(R.id.upload_image_button);
        plantImageView = findViewById(R.id.plant_image_view);
        plantNameField = findViewById(R.id.plant_name_field);
        lightSlider = findViewById(R.id.light_slider);
        moistureSlider = findViewById(R.id.moisture_slider);
        humiditySlider = findViewById(R.id.humidity_slider);

        // Auto complete dropdowns to populate
        piTextView = findViewById(R.id.select_pi);
        deviceTextView = findViewById(R.id.select_moisture_device);
        portTextView = findViewById(R.id.select_sensor_port);

        Client client = Client.getInstance();
        executorService.execute(() -> {
            // Fetch available pi with grpc to populate dropdowns
            List<Pi> piList = client.getAvailablePiSensors();
            Log.i("GetPiRequestUI", piList.toString());
            runOnUiThread(() -> {
                ArrayAdapter<Pi> piAdapter = new ArrayAdapter<>(AddPlantActivity.this, R.layout.dropdown_item, piList);
                piTextView.setAdapter(piAdapter);
            });
        });

        // Find dropdown layouts to enable/disable based on selected Pi
        TextInputLayout deviceDropdown = findViewById(R.id.select_device_dropdown);
        TextInputLayout portDropdown = findViewById(R.id.select_sensor_dropdown);

        piTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int pos, long id) {
                selectedPi = (Pi) parentView.getItemAtPosition(pos);
                // Populate device dropdown based on selected Pi
                ArrayAdapter<MoistureDevice> deviceAdapter = new ArrayAdapter<>(AddPlantActivity.this, R.layout.dropdown_item, selectedPi.getMoistureDevices());

                portTextView.setText("");
                deviceTextView.setText("");    // Reset previous selection
                deviceTextView.setAdapter(deviceAdapter);
                deviceDropdown.setEnabled(true);
                portDropdown.setEnabled(false);
            }
        });

        deviceTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int pos, long id) {
                selectedDevice = (MoistureDevice) parentView.getItemAtPosition(pos);
                // Populate available sensor ports based on selected MoistureDevice
                ArrayAdapter<Integer> portAdapter = new ArrayAdapter<>(AddPlantActivity.this, R.layout.dropdown_item, selectedDevice.getAvailablePorts());

                portTextView.setText("");    // Reset previous selection
                portTextView.setAdapter(portAdapter);
                portDropdown.setEnabled(true);
            }
        });

        portTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int pos, long id) {
                selectedPort = (int) parentView.getItemAtPosition(pos);
            }
        });

        // Set listeners for selecting plant image
        selectImageBtn.setOnClickListener(view -> selectImage());
        plantImageView.setOnClickListener(view -> selectImage());

        // Finally, submit plant and upload image if needed
        addPlantBtn = findViewById(R.id.add_plant_submit);
        addPlantBtn.setOnClickListener(view -> {
            submitAddPlant();

            // TODO if successful, return to main activity 
        });
    }

    private void submitAddPlant() {
        CompletableFuture<String> imageUrlFuture;

        if (imageUri == null) {
            // Skip upload and create plant with null (default) image
            imageUrlFuture = CompletableFuture.completedFuture(null);
        } else {
            // Upload image to firebase if one is provided
            imageUrlFuture = uploadImage(imageUri);
        }

        // Waits for successful firebase upload before proceeding with GRPC
        imageUrlFuture
                .thenCompose(imageUrl -> {
                    Log.d("SubmitAddPlant", "Image upload completed, proceeding to create plant.");
                    PlantInfo.Builder plant = PlantInfo.newBuilder()
                            .setName(plantNameField.getText().toString())
                            .setLightLevelValue((int) lightSlider.getValue())
                            .setMinMoisture((int) moistureSlider.getValue())
                            .setMinHumidity((int) humiditySlider.getValue())
                            .setPid(selectedPi.getId())
                            .setMoistureDeviceId(selectedDevice.getId())
                            .setSensorPort(selectedPort);
                    if (imageUrl != null) {
                        plant.setImageUrl(imageUrl);
                    }
                    return createPlant(plant.build());
                })
                .thenRun(() -> runOnUiThread(() -> {
                    Log.d("SubmitAddPlant", "Add plant successful.");
                    Toast.makeText(AddPlantActivity.this, "Add plant successful!", Toast.LENGTH_SHORT).show();
                }))
                .exceptionally(e -> {
                    Log.e("SubmitAddPlant", "Add plant failed: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(AddPlantActivity.this, "Failed to add plant: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    return null;
                });
    }

    private void selectImage() {
        // Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // TODO allow to upload from camera app directly
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

    private CompletableFuture<Void> createPlant(PlantInfo plant) {
        return CompletableFuture.runAsync(() -> {
            Log.d("CreatePlant", "Starting GRPC request with imageUrl: " + plant.getImageUrl());
            try {
                // TODO GRPC addPlant request, handle response? success?
                Client.getInstance().addPlant(plant);
            } catch (Exception e) {
                Log.e("AddPlantRequest", "Failed to create new plant.", e);
            }
        }, executorService);
    }
}
