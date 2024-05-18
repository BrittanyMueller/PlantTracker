package ca.planttracker;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ViewPlants extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.view_plants);
    Toolbar myToolbar = findViewById(R.id.top_toolbar);
    myToolbar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

    setSupportActionBar(myToolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

  }

//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    getMenuInflater().inflate(R.menu.plant_menu, menu);
//    return true;
//  }

  public void clickMe(View view) {
    EditText input = findViewById(R.id.inputField);
    Toast.makeText(this, input.getText().toString(), Toast.LENGTH_SHORT).show();
  }
}
