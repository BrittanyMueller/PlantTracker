package ca.planttracker.ui.activities;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Optional;

import ca.planttracker.R;

public class BaseActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    protected PopupMenu popupMenu;

    protected void initCustomToolbar(boolean showHamburger, String title, Optional<Integer> menuResource) {
        ImageView hamburgerMenu = findViewById(R.id.hamburger_menu);
        ImageView menuButton = findViewById(R.id.more_icon);

        if (menuResource.isPresent()) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            popupMenu = new PopupMenu(this, toolbar);
            popupMenu.setGravity(GravityCompat.END);
            getMenuInflater().inflate(menuResource.get(), popupMenu.getMenu());
            menuButton.setOnClickListener((View v) -> {
                popupMenu.show();  // Show the menu
            });
            popupMenu.setOnMenuItemClickListener(this);
        } else {
            menuButton.setVisibility(View.GONE);
        }

        if (showHamburger) {
            hamburgerMenu.setOnClickListener((View v) -> {
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.openDrawer(GravityCompat.START);
            });

            NavigationView navView = findViewById(R.id.navigation);
            navView.setNavigationItemSelectedListener((MenuItem item) -> {
                Intent intent = null;
                switch(item.getItemId()) {
                    case R.id.home_menu:
                        intent = new Intent(this, PlantListActivity.class);
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

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }
}
