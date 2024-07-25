package ca.planttracker;

import android.util.Log;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import planttracker.server.GetPlantsRequest;
import planttracker.server.GetPlantsRequestType;
import planttracker.server.GetPlantsResponse;
import planttracker.server.PlantTrackerGrpc;
import planttracker.server.Plant;

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
    public planttracker.server.Plant getPlant(long id, boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANT).setId(id).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.getPlants(request);
        /* TODO convert grpc server Plant to planttracker Plant type */
        // TODO handling for empty result
        // TODO error handling, check return code, error string
        return res.getPlants(0);
    }

    public List<planttracker.server.Plant> getPlantsByPi(long pid, boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANTS_BY_PI).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.getPlants(request);

        for (Plant plant : res.getPlantsList()) {
            Log.i("INFO", "Client: " + plant.getName());
        }
        /* TODO convert grpc server Plant to planttracker Plant type */
        return res.getPlantsList();
    }

    public List<planttracker.server.Plant> getPlants(boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_ALL_PLANTS).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.getPlants(request);

        for (Plant plant : res.getPlantsList()) {
            Log.i("INFO", "Client: " + plant.getName());
        }
        /* TODO convert grpc server Plant to planttracker Plant type */
        return res.getPlantsList();
    }
}
