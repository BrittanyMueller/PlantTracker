package ca.planttracker;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class AppBarActivity extends AppCompatActivity {

    protected void createAppBar(boolean hamburger, String title) {
        ImageView hamburgerMenu = findViewById(R.id.hamburger_menu);
        if (hamburger) {

            hamburgerMenu.setOnClickListener((View v) -> {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.openDrawer(GravityCompat.START);
            });

            NavigationView navView = findViewById(R.id.navigation);
            navView.setNavigationItemSelectedListener((MenuItem item) -> {
                Intent intent = null;
                switch(item.getItemId()) {
                    case R.id.home_menu:
                        intent = new Intent(this, ViewPlantsActivity.class);
                        break;
                    case R.id.setting_menu:
                        intent = new Intent(this, SettingsActivity.class);
                }
                startActivity(intent);
                return true; // TODO(qawse3dr) what does this return do
            });
        } else {
            hamburgerMenu.setVisibility(View.GONE);
            ImageView backButton = findViewById(R.id.back_arrow);
            backButton.setVisibility(View.VISIBLE);
            backButton.setOnClickListener((View v) -> {
                setResult(RESULT_CANCELED);
                finish(); // returns to previous activity
            });
        }

        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(title);
    }
}
