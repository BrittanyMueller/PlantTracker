package ca.planttracker;

import android.util.Log;

import com.google.protobuf.Empty;
import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import ca.planttracker.data.models.MoistureDevice;
import ca.planttracker.data.models.Pi;
import ca.planttracker.data.models.Plant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import planttracker.server.AvailableMoistureDevice;
import planttracker.server.DeletePlantRequest;
import planttracker.server.GetAvailablePiRequest;
import planttracker.server.GetAvailablePiResponse;
import planttracker.server.GetPlantDataRequest;
import planttracker.server.GetPlantsRequest;
import planttracker.server.GetPlantsRequestType;
import planttracker.server.GetPlantsResponse;
import planttracker.server.LightLevel;
import planttracker.server.PlantSensorData;
import planttracker.server.PlantSensorDataList;
import planttracker.server.PlantTrackerGrpc;

import planttracker.server.PlantInfo;
import planttracker.server.Result;


public class PlantTrackerClient {

    private static final int timeout = 15;
    private static PlantTrackerGrpc.PlantTrackerBlockingStub stub;
    private static ManagedChannel channel = null;
    // TODO removing context will fix this leak, context only used for mock data
    private static final PlantTrackerClient instance = new PlantTrackerClient();
    private String host;
    private Context ctx;

    private PlantTrackerClient() {
    }

    public synchronized void connect(Context ctx, String host, int port) {
        this.host = host;
        this.ctx = ctx;
        if (channel != null) {
            channel.shutdown();
            channel = null;
        }
        if (host.equals("0.0.0.0")) {
            return;
        }
        Log.i("ClientConnect", "Connecting to " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        stub = PlantTrackerGrpc.newBlockingStub(channel);
    }

    public synchronized static PlantTrackerClient getInstance() {
        return instance;
    }

    public boolean addPlant(PlantInfo plantInfo) {
        if (host.equals("0.0.0.0")) {
            return true;
        }
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

    public List<Pi> getAvailablePiSensors(Optional<Long> plantId) {
        ArrayList<Pi> piList = new ArrayList<>();

        try {
            GetAvailablePiRequest.Builder request = GetAvailablePiRequest.newBuilder();
            plantId.ifPresent(request::setPlantId);
            GetAvailablePiResponse res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getAvailablePiSensors(request.build());
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
        } catch (StatusRuntimeException e) {
            Log.e("GetPiRequest", "GRPC call failed with: " + e.getStatus().getDescription(), e);
        } catch (Exception e) {
            Log.e("GetPiRequest", "Failed to retrieve available pi: " + e.getMessage());
        }
        return piList;
    }

    public Plant getPlant(long id) {
        if (host.equals("0.0.0.0")) {
            return getPlantData().get(0);
        }

        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANT).setId(id).build();
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

    public List<Plant> getPlantsByPi(long pid) {
        if (host.equals("0.0.0.0")) {
            return getPlantData();
        }
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_PLANTS_BY_PI).setId(pid).build();
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

    public List<Plant> getPlants() {
        if (host.equals("0.0.0.0")) {
            return getPlantData();
        }

        ArrayList<Plant> plants = new ArrayList<>();
        GetPlantsRequest request = GetPlantsRequest.newBuilder()
                .setType(GetPlantsRequestType.GET_ALL_PLANTS)
                .build();

        try {
            GetPlantsResponse res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getPlants(request);
            if (res.getRes().getReturnCode() == 0) {
                // Request was successful, parse response
                for (PlantInfo plant : res.getPlantsList()) {
                    // Convert grpc info to Plant
                    plants.add(new Plant(plant));
                    Log.i("GetPlants", plant.toString());
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

    public List<PlantSensorData> getPlantSensorData(long plantId, Instant start, Instant end) {
        if (host.equals("0.0.0.0")) {
            return new ArrayList<PlantSensorData>();
        }

        // TODO might be nice to no have to specify end date if you want most recent data.
        GetPlantDataRequest req = GetPlantDataRequest.newBuilder()
                .setPlantId(plantId).setStartDate(start.toEpochMilli())
                .setEndDate(end.toEpochMilli()
                ).build();
        try {
            PlantSensorDataList list = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getPlantSensorData(req);
            return list.getDataList();
        } catch (StatusRuntimeException e) {
            Log.e("GetPlantSensorData", "Failed to get sensor data for plantId=" + String.valueOf(plantId), e);
            throw e;
        }
    }

    public boolean updatePlant(PlantInfo plantInfo) {
        if (host.equals("0.0.0.0")) {
            return true;
        }
        try {
            Result res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).updatePlant(plantInfo);
            if (res.getReturnCode() == 0) {
                Log.i("UpdatePlantClient", "Plant update successfully.");
                return true;
            } else {
                // Non-zero return code, response has error
                Log.e("UpdatePlantClient", "Server failed to update plant: " + res.getError());
            }
        } catch (StatusRuntimeException e) {
            Log.e("UpdatePlantClient", "GRPC call failed with: " + e.getStatus().getDescription(), e);
        } catch (Exception e) {
            Log.e("UpdatePlantClient", e.getMessage(), e);
        }
        return false;
    }

    public boolean deletePlant(long plantId) {
        if (host.equals("0.0.0.0")) {
            return true;
        }
        DeletePlantRequest id = DeletePlantRequest.newBuilder().setPlantId(plantId).build();
        try {
            Result res = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).deletePlant(id);
            if (res.getReturnCode() == 0) {
                Log.i("DeletePlantClient", "Delete plant successfully.");
                return true;
            } else {
                // Non-zero return code, response has error
                Log.e("DeletePlantClient", "Server failed to delete plant: " + res.getError());
            }
        } catch (StatusRuntimeException e) {
            Log.e("DeletePlantClient", "GRPC call failed with: " + e.getStatus().getDescription(), e);
        } catch (Exception e) {
            Log.e("DeletePlantClient", e.getMessage(), e);
        }
        return false;
    }


    // ******************* MOCK DATA *******************
    private List<Plant> getPlantData() {
        List<Plant> plantList = new ArrayList<>();
        try {
            JSONArray objArray = parseJSONArray();
            if (objArray != null) {
                for (int i = 0; i < objArray.length(); i++) {
                    JSONObject plantObj = objArray.getJSONObject(i);
                    Log.i("TAG", plantObj.getString("name"));
                    Plant plant = new Plant(plantObj.getInt("id"), plantObj.getString("name"), LightLevel.MED);
                    plantList.add(plant);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return plantList;
    }

    private JSONArray parseJSONArray() {
        AssetManager assets = ctx.getAssets();
        try {
            InputStream inputStream = assets.open("plants.json");
            int fileSize = inputStream.available();
            byte[] buffer = new byte[fileSize];
            int bytes = inputStream.read(buffer);
            if (bytes == 0) {
                return null;
            }
            inputStream.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonString);
            return jsonObject.getJSONArray("plants");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
