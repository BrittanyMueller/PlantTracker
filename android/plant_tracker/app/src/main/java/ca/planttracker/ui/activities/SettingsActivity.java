package ca.planttracker.ui.activities;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

import ca.planttracker.R;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        initCustomToolbar(false, "Settings", Optional.empty());
        getSupportFragmentManager().beginTransaction().replace(R.id.preference, new MainPreference()).commit();
    }

    public static class MainPreference extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preference, rootKey);
        }
    }

}
