package ca.planttracker;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AddPlantActivity extends AppCompatActivity {

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

        Button addPlantSubmit = findViewById(R.id.add_plant_submit);
        addPlantSubmit.setOnClickListener(view -> {

            Client client = new Client("10.0.2.2", 5050);
            long returnCode = client.addPlant();

            if (returnCode == 0) {
                Toast.makeText(AddPlantActivity.this, "Add plant successful!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddPlantActivity.this, "Add plant failed :(", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
