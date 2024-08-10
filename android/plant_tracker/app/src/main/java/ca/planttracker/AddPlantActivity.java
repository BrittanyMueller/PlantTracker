package ca.planttracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.Objects;

public class AddPlantActivity extends AppCompatActivity {

    Button selectImageBtn;
    ImageView plantImageView;

    Uri imageUri;

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
                } else {
                    // No image selected
                    Toast.makeText(AddPlantActivity.this, "No image selected.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_arrow_24);
            getSupportActionBar().setTitle("");
        }
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getString(R.string.add_plant));

        selectImageBtn = findViewById(R.id.upload_image_button);
        plantImageView = findViewById(R.id.plant_image_view);

        selectImageBtn.setOnClickListener(view -> selectImage());
        plantImageView.setOnClickListener(view -> selectImage());

        TextInputEditText plantNameField = findViewById(R.id.plant_name_field);

        Slider lightSlider = findViewById(R.id.light_slider);
        // Set readable labels on light slider
        lightSlider.setLabelFormatter(value -> {
            switch ((int) value) {
                case 0:
                    return "Low";
                case 1:
                    return "Medium";
                case 2:
                    return "High";
                default:
                    // Impossible base on UI ?
                    return "";
            }
        });


        Button addPlantSubmit = findViewById(R.id.add_plant_submit);
        addPlantSubmit.setOnClickListener(view -> {

            String plantName = Objects.requireNonNull(plantNameField.getText()).toString();
            int lightLevel = (int) lightSlider.getValue();

            Toast.makeText(AddPlantActivity.this, "Plant Name: " + plantName, Toast.LENGTH_LONG).show();

//            Client client = new Client("10.0.2.2", 5050);
//            long returnCode = client.addPlant();
//
//            if (returnCode == 0) {
//                Toast.makeText(AddPlantActivity.this, "Add plant successful!", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(AddPlantActivity.this, "Add plant failed :(", Toast.LENGTH_SHORT).show();
//            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        selectImageLauncher.launch(intent);
    }
}
