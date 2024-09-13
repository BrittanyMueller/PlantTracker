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

    public enum Status {
        HAPPY,
        FINE,
        OK,
        SAD,
    }

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
            holder.plantStatus = convertView.findViewById(R.id.status);
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder from the recycled view
            holder = (ViewHolder) convertView.getTag();
        }

        // For every plant in list, inflate card view
        Plant plant = getItem(position);
        holder.plantName.setText(plant != null ? plant.getName() : null);

        // Figure out status
        Status status = Status.HAPPY;

        if (plant.hasLastData()) {
            holder.plantStatus.setVisibility(View.VISIBLE);

            if (plant.getLastHumidity() < plant.getMinHumidity() - 5 || plant.getLastMoisture() < plant.getMinMoisture() * 10 - 5) {
                status = Status.SAD;
            } else if (plant.getLastHumidity() < plant.getMinHumidity() || plant.getLastMoisture() < plant.getMinMoisture() * 10) {
                status = Status.OK;
            } else if (plant.getLastHumidity() < plant.getMinHumidity() + 5 || plant.getLastMoisture() < plant.getMinMoisture() * 10 + 5) {
                status = Status.FINE;
            } else {
                status = Status.HAPPY;
            }

            switch(status) {
                case HAPPY:
                    holder.plantStatus.setImageResource(R.drawable.outline_sentiment_excited_24);
                    holder.plantStatus.setColorFilter(convertView.getResources().getColor(R.color.green, convertView.getContext().getTheme()));
                    break;
                case FINE:
                    holder.plantStatus.setImageResource(R.drawable.baseline_sentiment_satisfied_24);
                    holder.plantStatus.setColorFilter(convertView.getResources().getColor(R.color.yellow, convertView.getContext().getTheme()));
                    break;
                case OK:
                    holder.plantStatus.setImageResource(R.drawable.baseline_sentiment_neutral_24);
                    holder.plantStatus.setColorFilter(convertView.getResources().getColor(R.color.orange, convertView.getContext().getTheme()));
                    break;
                case SAD:
                    holder.plantStatus.setImageResource(R.drawable.baseline_sentiment_dissatisfied_24);
                    holder.plantStatus.setColorFilter(convertView.getResources().getColor(R.color.red, convertView.getContext().getTheme()));

                    break;
            }
        } else {
            holder.plantStatus.setVisibility(View.INVISIBLE);
        }



        if (plant.getImageUrl() == null) {
            // Default plant image placeholder
            holder.plantImage.setImageResource(R.drawable.plant_placeholder);
        } else {
            // Use Glide to load the image from the URL
            Glide.with(context)
                .load((plant.getStorageReference() != null) ?  plant.getStorageReference() : plant.getImageUrl())
                .placeholder(R.drawable.plant_placeholder) // Optional placeholder image? not sure if just while loading
                .into(holder.plantImage);
        }

        return convertView;
    }

    // ViewHolder holds references to plant cards
    private static class ViewHolder {
        ImageView plantImage;
        TextView plantName;
        ImageView plantStatus;
    }
}
