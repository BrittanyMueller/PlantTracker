package ca.planttracker;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        ImageView hamburger = findViewById(R.id.hamburger_menu);
        hamburger.setVisibility(View.GONE);
        ImageView backButton = findViewById(R.id.back_arrow);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener((View v) -> {
            finish(); // Returns to previous activity
        });


        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText("Settings");

        getSupportFragmentManager().beginTransaction().replace(R.id.preference, new MainPreference()).commit();
    }

    public static class MainPreference extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preference, rootKey);
        }
    }

}
