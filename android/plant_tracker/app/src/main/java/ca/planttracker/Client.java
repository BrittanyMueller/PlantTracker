package ca.planttracker;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.List;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import planttracker.server.GetPlantsRequest;
import planttracker.server.GetPlantsRequestType;
import planttracker.server.GetPlantsResponse;
import planttracker.server.PlantTrackerGrpc;

import planttracker.server.PlantInfo;
import planttracker.server.Result;

public class Client {

    private static PlantTrackerGrpc.PlantTrackerBlockingStub stub;
    private static ManagedChannel channel = null;
    private static final Client client = new Client();

    // TODO refactor into singleton with initialization method
    private  Client() {
    }

    public synchronized  void connect(String host, int port) {
        if (channel != null) {
            channel.shutdown();
        }
        // get host and port from preferences
        Log.i("TAG", "Connecting to " + host + ":" + String.valueOf(port));
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = PlantTrackerGrpc.newBlockingStub(channel);
    }

    public synchronized static  Client instance() {
        return client;
    }

    public long addPlant() {

        long returnCode = -1;
        PlantInfo newPlant = PlantInfo.newBuilder()
                                    .setName("Android Plant")
                                    .setLightLevelValue(2)
                                    .setMinMoisture(5)
                                    .setMinHumidity(50)
                                    .setPid(1)
                                    .setMoistureDeviceId(1)
                                    .setSensorPort(5).build();

        Result res = stub.addPlant(newPlant);
        returnCode = res.getReturnCode();

        return returnCode;
    }

    /**
     * RESPONSE:
     * message GetPlantsResponse {
     *   Result res = 1;
     *   repeated Plant plants = 2;
     * }
     * message Result {
     *   int64 return_code = 1;
     *   optional string error = 2;
     * }
     */
    public Plant getPlant(long id, boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANT).setId(id).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.getPlants(request);

        Plant plant = null;
        if (res.getPlantsCount() == 1 && res.getRes().getReturnCode() == 0) {
            // Request for 1 plant was successful, parse response
            Log.i("INFO", "Response found 1 plant");
            plant = new Plant(res.getPlants(0));
        }
        // TODO error handling, check return code, error string
        return plant;
    }

    public List<Plant> getPlantsByPi(long pid, boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANTS_BY_PI).setId(pid).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.getPlants(request);

        ArrayList<Plant> plants = new ArrayList<>();
        if (res.getRes().getReturnCode() == 0) {
            // Request was successful, parse response
            for (PlantInfo plant : res.getPlantsList()) {
                // Convert grpc info to Plant
                plants.add(new Plant(plant));
            }
            Log.i("INFO", "Successful response getPlantsByPi");
        }
        // TODO else error handling?
        return plants;
    }

    public List<Plant> getPlants(boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_ALL_PLANTS).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.getPlants(request);

        ArrayList<Plant> plants = new ArrayList<>();
        if (res.getRes().getReturnCode() == 0) {
            // Request was successful, parse response
            for (PlantInfo plant : res.getPlantsList()) {
                // Convert grpc info to Plant
                plants.add(new Plant(plant));
            }
            Log.i("INFO", "Successful response getPlants");
        }
        // TODO else error handling?
        return plants;
    }
}
