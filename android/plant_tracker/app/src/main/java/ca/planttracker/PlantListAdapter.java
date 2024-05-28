package ca.planttracker;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class PlantListAdapter extends ArrayAdapter<Plant> {

    ArrayList<Plant> plantList;
    Context context;

    public PlantListAdapter(@NonNull Context context, List<Plant> plantList) {
        // Override default Android list view inflation
        super(context, 0, plantList);
    }
}
