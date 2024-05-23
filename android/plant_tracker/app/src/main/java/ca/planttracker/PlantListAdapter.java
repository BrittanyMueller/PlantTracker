package ca.planttracker;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.List;

public class PlantListAdapter extends ArrayAdapter<Plant> {
    public PlantListAdapter(@NonNull Context context, List<Plant> plantList) {
        // Override default Android list view inflation
        super(context, 0, plantList);
    }
}
