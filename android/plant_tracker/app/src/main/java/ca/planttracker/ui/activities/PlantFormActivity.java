package ca.planttracker.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.planttracker.PlantTrackerClient;
import ca.planttracker.R;
import ca.planttracker.data.models.MoistureDevice;
import ca.planttracker.data.models.Pi;

public abstract class PlantFormActivity extends BaseActivity {

    protected ExecutorService executorService;
    StorageReference storageReference;
    Uri imageUri; // Local image reference

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

    Button submitBtn;

    protected boolean requestInProgress = false;

    private final ActivityResultLauncher<Intent> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == PlantFormActivity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        plantImageView.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e("PhotoPicker", "Error selecting image: ", e);
                        Toast.makeText(PlantFormActivity.this, "Error selecting image.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plant_form_activity);

        try {
            FirebaseApp.initializeApp(PlantFormActivity.this);
            storageReference = FirebaseStorage.getInstance().getReference();
            Log.d("PlantFormActivity", "Storage reference setup without errors.");
        } catch (Exception e) {
            Log.e("PlantFormActivity", "Rip firebase: " + e.getMessage(), e);
        }

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

        PlantTrackerClient client = PlantTrackerClient.getInstance();
        executorService.execute(() -> {
            // Fetch available pi with grpc to populate dropdowns
            List<Pi> piList = client.getAvailablePiSensors();

            runOnUiThread(() -> {
                if (piList.isEmpty()) {
                    // Disable form submission if no pi available
                    submitBtn.setEnabled(false);
                    piDropdown.setEnabled(false);
                    piTextView.setText("No Available Sensor Ports");
                    deviceTextView.setText("--");
                    portTextView.setText("--");
                } else {
                    ArrayAdapter<Pi> piAdapter = new ArrayAdapter<>(PlantFormActivity.this, R.layout.dropdown_item, piList);
                    piTextView.setAdapter(piAdapter);
                }
            });
        });

        piTextView.setOnItemClickListener((parentView, view, pos, id) -> {
            selectedPi = (Pi) parentView.getItemAtPosition(pos);
            // Populate device dropdown based on selected Pi
            ArrayAdapter<MoistureDevice> deviceAdapter = new ArrayAdapter<>(PlantFormActivity.this, R.layout.dropdown_item, selectedPi.getMoistureDevices());

            piDropdown.setErrorEnabled(false);
            portTextView.setText("");
            deviceTextView.setText("");    // Reset previous selection
            deviceTextView.setAdapter(deviceAdapter);
            deviceDropdown.setEnabled(true);
            portDropdown.setEnabled(false);
        });

        deviceTextView.setOnItemClickListener((parentView, view, pos, id) -> {
            selectedDevice = (MoistureDevice) parentView.getItemAtPosition(pos);
            // Populate available sensor ports based on selected MoistureDevice
            ArrayAdapter<Integer> portAdapter = new ArrayAdapter<>(PlantFormActivity.this, R.layout.dropdown_item, selectedDevice.getAvailablePorts());

            deviceDropdown.setErrorEnabled(false);
            portTextView.setText("");    // Reset previous selection
            portTextView.setAdapter(portAdapter);
            portDropdown.setEnabled(true);
        });

        portTextView.setOnItemClickListener((parentView, view, pos, id) -> {
            portDropdown.setErrorEnabled(false);
            selectedPort = (int) parentView.getItemAtPosition(pos);
        });

        plantNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                plantNameLayout.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set listeners for selecting plant image
        selectImageBtn.setOnClickListener(view -> selectImage());
        plantImageView.setOnClickListener(view -> selectImage());

        // Finally, submit plant and upload image if needed
        submitBtn = findViewById(R.id.plant_submit);
        submitBtn.setOnClickListener(view -> {
            if (validateForm()) {
                // TODO disable submit button + loading animation instead of jank flag
                requestInProgress = true;
                submitBtn.setEnabled(false);
                handleSubmit().thenRun(() -> requestInProgress = false);
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

    private void selectImage() {
        // Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // TODO allow to upload from camera app directly
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        selectImageLauncher.launch(intent);
    }

    protected CompletableFuture<String> uploadImage() {
        // TODO handle existing images, delete old pic if new one uploaded
        if (imageUri == null) {
            Log.d("FirebaseStorage", "No image selected - skipping upload.");
            return CompletableFuture.completedFuture(null);
        } else {
            return CompletableFuture.supplyAsync(() -> {

                String path = "images/" + UUID.randomUUID();
                StorageReference ref = storageReference.child(path);

                CompletableFuture<String> future = new CompletableFuture<>();

                ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                    // Upload successful, returns promised image path
                    Log.d("FirebaseStorage", "Image upload successful.");
                    future.complete(path);
                    runOnUiThread(() -> Toast.makeText(PlantFormActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show());
                }).addOnFailureListener(e -> {
                    future.completeExceptionally(e);
                    Log.e("FirebaseStorage", "Image upload failed.", e);
                });
                return future.join();   // Returns image url on complete
            }, executorService);
        }
    }

    protected abstract CompletableFuture<Void> handleSubmit();
}
