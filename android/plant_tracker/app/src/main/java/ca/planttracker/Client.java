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

    // TODO implement grpc request types
    public List<planttracker.server.Plant> getPlants() {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_ALL_PLANTS).setFetchImages(true).build();
        GetPlantsResponse res = stub.getPlants(request);

        for (Plant plant : res.getPlantsList()) {
            Log.i("INFO", "Client: " + plant.getName());
        }
        /* TODO convert grpc server Plant to planttracker Plant type */
        return res.getPlantsList();
    }

    public planttracker.server.Plant getPlant(long id, boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANT).setId(id).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.getPlants(request);
        /* TODO convert grpc server Plant to planttracker Plant type */
        /* TODO error handling for no result */
        return res.getPlants(0);
    }

}
