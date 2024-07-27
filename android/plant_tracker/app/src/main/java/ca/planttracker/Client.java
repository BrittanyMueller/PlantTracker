package ca.planttracker;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import planttracker.server.GetPlantsRequest;
import planttracker.server.GetPlantsRequestType;
import planttracker.server.GetPlantsResponse;
import planttracker.server.PlantTrackerGrpc;

import planttracker.server.PlantInfo;

public class Client {

    private final ManagedChannel channel;
    private final PlantTrackerGrpc.PlantTrackerBlockingStub stub;

    // TODO refactor into singleton with initialization method
    public Client(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = PlantTrackerGrpc.newBlockingStub(channel);
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
