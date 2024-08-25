package ca.planttracker;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;

import com.google.protobuf.Empty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import planttracker.server.AvailableMoistureDevice;
import planttracker.server.GetAvailablePiResponse;
import planttracker.server.GetPlantsRequest;
import planttracker.server.GetPlantsRequestType;
import planttracker.server.GetPlantsResponse;
import planttracker.server.PlantTrackerGrpc;

import planttracker.server.PlantInfo;
import planttracker.server.Result;

public class Client {

    private static PlantTrackerGrpc.PlantTrackerBlockingStub stub;
    private static ManagedChannel channel = null;
    private static final Client instance = new Client();

    private ExecutorService executorService;

    private  Client() {
    }

    public synchronized void connect(String host, int port) {
        if (channel != null) {
            channel.shutdown();
        }
        // TODO get host and port from preferences?
        Log.i("ClientConnect", "Connecting to " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = PlantTrackerGrpc.newBlockingStub(channel);

        // TODO handle threading within singleton, wrap functions in executor
        executorService = Executors.newFixedThreadPool(2);
    }

    public synchronized static Client getInstance() {
        return instance;
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

    public List<Pi> getAvailablePiSensors() {
        // TODO run in diff thread, callable?
        ArrayList<Pi> piList = new ArrayList<>();

        try {
            Empty emptyRequest = Empty.newBuilder().build();
            GetAvailablePiResponse res = stub.getAvailablePiSensors(emptyRequest);
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
            Log.i("GetPlant", "Response found 1 plant");
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
            Log.i("GetPlantsByPi", "Successful response getPlantsByPi");
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
            Log.i("GetPlants", "Successful response getPlants");
        }
        // TODO else error handling?
        return plants;
    }
}
