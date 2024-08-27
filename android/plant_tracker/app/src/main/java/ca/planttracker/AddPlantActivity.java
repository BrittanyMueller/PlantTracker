package ca.planttracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.annotation.Nullable;
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

public class AddPlantActivity extends AppBarActivity {

    private ExecutorService executorService;
    StorageReference storageReference;
    Uri imageUri;

    Button selectImageBtn;
    ImageView plantImageView;
    EditText plantNameField;
    TextInputLayout plantNameLayout;

    // Dropdown selectable text views
    AutoCompleteTextView piTextView;
    AutoCompleteTextView deviceTextView;
    AutoCompleteTextView portTextView;

    // Dropdown layouts
    TextInputLayout piDropdown;
    TextInputLayout deviceDropdown;
    TextInputLayout portDropdown;

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

        createAppBar(false, getString(R.string.add_plant));

        FirebaseApp.initializeApp(AddPlantActivity.this);
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize threads for async tasks
        executorService = Executors.newSingleThreadExecutor();


        // Initialize ref to form elements
        selectImageBtn = findViewById(R.id.upload_image_button);
        plantImageView = findViewById(R.id.plant_image_view);
        plantNameField = findViewById(R.id.plant_name_field);
        plantNameLayout = findViewById(R.id.plant_name_layout);
        lightSlider = findViewById(R.id.light_slider);
        moistureSlider = findViewById(R.id.moisture_slider);
        humiditySlider = findViewById(R.id.humidity_slider);

        // Auto complete dropdowns to populate
        piTextView = findViewById(R.id.select_pi);
        deviceTextView = findViewById(R.id.select_moisture_device);
        portTextView = findViewById(R.id.select_sensor_port);
        // Dropdown layouts to enable/disable based on selected Pi
        piDropdown = findViewById(R.id.select_pi_dropdown);
        deviceDropdown = findViewById(R.id.select_device_dropdown);
        portDropdown = findViewById(R.id.select_sensor_dropdown);

        Client client = Client.getInstance();
        executorService.execute(() -> {
            // Fetch available pi with grpc to populate dropdowns
            List<Pi> piList = client.getAvailablePiSensors();

            runOnUiThread(() -> {
                if (piList.isEmpty()) {
                    // Disable form submission if no pi available
                    addPlantBtn.setEnabled(false);
                    piDropdown.setEnabled(false);
                    piTextView.setText("No Available Sensor Ports");
                    deviceTextView.setText("--");
                    portTextView.setText("--");
                } else {
                    ArrayAdapter<Pi> piAdapter = new ArrayAdapter<>(AddPlantActivity.this, R.layout.dropdown_item, piList);
                    piTextView.setAdapter(piAdapter);
                }
            });
        });

        piTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int pos, long id) {
                selectedPi = (Pi) parentView.getItemAtPosition(pos);
                // Populate device dropdown based on selected Pi
                ArrayAdapter<MoistureDevice> deviceAdapter = new ArrayAdapter<>(AddPlantActivity.this, R.layout.dropdown_item, selectedPi.getMoistureDevices());

                piDropdown.setErrorEnabled(false);
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

                deviceDropdown.setErrorEnabled(false);
                portTextView.setText("");    // Reset previous selection
                portTextView.setAdapter(portAdapter);
                portDropdown.setEnabled(true);
            }
        });

        portTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parentView, View view, int pos, long id) {
                portDropdown.setErrorEnabled(false);
                selectedPort = (int) parentView.getItemAtPosition(pos);
            }
        });

        plantNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                plantNameLayout.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set listeners for selecting plant image
        selectImageBtn.setOnClickListener(view -> selectImage());
        plantImageView.setOnClickListener(view -> selectImage());

        // Finally, submit plant and upload image if needed
        addPlantBtn = findViewById(R.id.add_plant_submit);
        addPlantBtn.setOnClickListener(view -> {
            if (validateForm()) {
                submitAddPlant();
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;
        if (plantNameField.getText().toString().trim().isEmpty()) {
            plantNameLayout.setErrorEnabled(true);
            plantNameLayout.setError("Plant name required.");
            plantNameField.requestFocus();
            valid = false;
        }
        // Only show error on first empty dropdown
        if (piTextView.getText().toString().isEmpty()) {
            piDropdown.setErrorEnabled(true);
            piDropdown.setError("Select the Pi connected to the plant.");
            valid = false;
        } else if (deviceTextView.getText().toString().isEmpty()) {
            deviceDropdown.setErrorEnabled(true);
            deviceDropdown.setError("Select the Moisture Device connected to the plant.");
            valid = false;
        } else if (portTextView.getText().toString().isEmpty()) {
            portDropdown.setErrorEnabled(true);
            portDropdown.setError("Select the sensor port connected to thr plant.");
            valid = false;
        }
        return valid;
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
                }).thenAccept(success -> runOnUiThread(() -> {
                    if (success) {
                        Log.d("SubmitAddPlant", "Add plant successful.");
                        Toast.makeText(AddPlantActivity.this, "Add plant successful!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();   // TODO return RESULT_OK to view plants activity
                    } else {
                        Log.d("SubmitAddPlant", "Add plant failed.");
                        Toast.makeText(AddPlantActivity.this, "Add plant failed.", Toast.LENGTH_LONG).show();
                    }
                }))
                .exceptionally(e -> {
                    Log.e("SubmitAddPlant", "Add plant failed exceptionally: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(AddPlantActivity.this, "Add plant failed. Check your network connection.", Toast.LENGTH_LONG).show());
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

    private CompletableFuture<Boolean> createPlant(PlantInfo plant) {
        return CompletableFuture.supplyAsync(() -> Client.getInstance().addPlant(plant), executorService);
    }
}
