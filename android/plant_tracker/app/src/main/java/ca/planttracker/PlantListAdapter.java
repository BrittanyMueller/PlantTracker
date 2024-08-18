package ca.planttracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

public class PlantListAdapter extends ArrayAdapter<Plant> {

    List<Plant> plantList;
    Context context;

    public PlantListAdapter(@NonNull Context context, List<Plant> plantList) {
        // Pass zero to override default Android list view inflation
        super(context, 0, plantList);
        this.context = context;
        this.plantList = plantList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            // Inflate new view if none available from recycling (scrolled out of view)
            convertView = LayoutInflater.from(context).inflate(R.layout.view_plants_card, parent, false);
            holder = new ViewHolder();  // Create new holder to reference view
            holder.plantImage = convertView.findViewById(R.id.plant_image);
            holder.plantName = convertView.findViewById(R.id.plant_title);
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder from the recycled view
            holder = (ViewHolder) convertView.getTag();
        }

        // For every plant in list, inflate card view
        Plant plant = getItem(position);
        holder.plantName.setText(plant != null ? plant.getName() : null);

        if (plant.getImageUrl() == null) {
            // Default plant image placeholder
            holder.plantImage.setImageResource(R.drawable.plant_placeholder);
        } else {
            // Use Glide to load the image from the URL
            Glide.with(context)
                .load(plant.getImageUrl())
                .placeholder(R.drawable.plant_placeholder) // Optional placeholder image? not sure if just while loading
                .into(holder.plantImage);
        }

        return convertView;
    }

    // ViewHolder holds references to plant cards
    private static class ViewHolder {
        ImageView plantImage;
        TextView plantName;
    }
}
