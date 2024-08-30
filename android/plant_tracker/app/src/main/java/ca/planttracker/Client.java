package ca.planttracker;

import android.util.Log;

import com.google.protobuf.Empty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import planttracker.server.AvailableMoistureDevice;
import planttracker.server.GetAvailablePiResponse;
import planttracker.server.GetPlantsRequest;
import planttracker.server.GetPlantsRequestType;
import planttracker.server.GetPlantsResponse;
import planttracker.server.PlantTrackerGrpc;

import planttracker.server.PlantInfo;
import planttracker.server.Result;

public class Client {

    private static final int timeout = 15;
    private static PlantTrackerGrpc.PlantTrackerBlockingStub stub;
    private static ManagedChannel channel = null;
    private static final Client instance = new Client();

    private  Client() {
    }

    public synchronized void connect(String host, int port) {
        if (channel != null) {
            channel.shutdown();
        }
        Log.i("ClientConnect", "Connecting to " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = PlantTrackerGrpc.newBlockingStub(channel);
    }

    public synchronized static Client getInstance() {
        return instance;
    }

    public boolean addPlant(PlantInfo plantInfo) {
        try {
            Result res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).addPlant(plantInfo);
            if (res.getReturnCode() == 0) {
                Log.i("AddPlantClient", "Plant added successfully.");
                return true;
            } else {
                // Non-zero return code, error expected
                Log.e("AddPlantClient", "Server failed to add plant: " + res.getError());
            }
        } catch (StatusRuntimeException e) {
            Log.e("AddPlantClient", "GRPC call failed with: " + e.getStatus().getDescription(), e);
        } catch (Exception e) {
            Log.e("AddPlantClient", e.getMessage(), e);
        }
        return false;
    }

    public List<Pi> getAvailablePiSensors() {
        ArrayList<Pi> piList = new ArrayList<>();

        try {
            Empty emptyRequest = Empty.newBuilder().build();
            GetAvailablePiResponse res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getAvailablePiSensors(emptyRequest);
            Log.i("GetPiRequest", "Response received: " + res.getPiListList().toString());

            // Parse protobuf types into objects for dropdown
            for (planttracker.server.Pi protoPi : res.getPiListList()) {

                ArrayList<MoistureDevice> deviceList = new ArrayList<>();
                for (AvailableMoistureDevice protoDevice : protoPi.getDeviceListList()) {
                    MoistureDevice device = new MoistureDevice(protoDevice.getId(), protoDevice.getName(), protoDevice.getSensorPortsList());
                    deviceList.add(device);
                }
                Pi pi = new Pi(protoPi.getPid(), protoPi.getName(), deviceList);
                piList.add(pi);
            }
        } catch (Exception e) {
            Log.e("GetPiRequest", "Failed to retrieve pi: " + e.getMessage());
        }
        return piList;
    }

    public Plant getPlant(long id, boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANT).setId(id).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getPlants(request);

        Plant plant = null;
        if (res.getPlantsCount() == 1 && res.getRes().getReturnCode() == 0) {
            // Request for 1 plant was successful, parse response
            Log.i("GetPlant", "Response found 1 plant");
            plant = new Plant(res.getPlants(0));
        } else {
            // Non-zero return code, error expected
            Log.e("GetPlant", "Server error: " + res.getRes().getError());
        }
        return plant;
    }

    public List<Plant> getPlantsByPi(long pid, boolean fetchImage) {
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANTS_BY_PI).setId(pid).setFetchImages(fetchImage).build();
        GetPlantsResponse res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getPlants(request);

        ArrayList<Plant> plants = new ArrayList<>();
        if (res.getRes().getReturnCode() == 0) {
            // Request was successful, parse response
            for (PlantInfo plant : res.getPlantsList()) {
                // Convert grpc info to Plant
                plants.add(new Plant(plant));
            }
            Log.i("GetPlantsByPi", "Successful response getPlantsByPi");
        } else {
            // Non-zero return code, error expected
            Log.e("GetPlantsByPi", "Server error: " + res.getRes().getError());
        }
        return plants;
    }

    public List<Plant> getPlants(boolean fetchImage) {

        ArrayList<Plant> plants = new ArrayList<>();
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_ALL_PLANTS)
                .setFetchImages(fetchImage)
                .build();

        try {
            GetPlantsResponse res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getPlants(request);
            if (res.getRes().getReturnCode() == 0) {
                // Request was successful, parse response
                for (PlantInfo plant : res.getPlantsList()) {
                    // Convert grpc info to Plant
                    plants.add(new Plant(plant));
                    Log.i("GetPlantsPLANT", plant.toString());
                }
                Log.i("GetPlants", "Successful response getPlants");
            } else {
                // Non-zero return code, error expected
                Log.e("GetPlants", "Server error: " + res.getRes().getError());
            }
        } catch (StatusRuntimeException e) {
            Log.e("GetPlants", "GRPC call failed with: " + e.getStatus().getDescription(), e);
            throw e;    // Rethrow GRPC exception
        } catch (Exception e) {
            Log.e("GetPlants", e.getMessage(), e);
            throw e;
        }
        return plants;
    }
}
