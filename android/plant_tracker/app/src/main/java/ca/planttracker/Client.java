package ca.planttracker;

import android.util.Log;

import com.google.protobuf.Empty;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import planttracker.server.AvailableMoistureDevice;
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
import android.content.res.AssetManager;


public class Client {

    private static final int timeout = 15;
    private static PlantTrackerGrpc.PlantTrackerBlockingStub stub;
    private static ManagedChannel channel = null;
    private static final Client instance = new Client();
    private String host;
    private Context ctx;

    private  Client() {
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

    public synchronized static Client getInstance() {
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
        if (host.equals("0.0.0.0")) {
            return getPlantData().get(0);
        }

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
        if (host.equals("0.0.0.0")) {
            return getPlantData();
        }
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
        if (host.equals("0.0.0.0")) {
            return getPlantData();
        }

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

    public List<PlantSensorData> getPlantSensorData(long plantId, LocalDateTime start, LocalDateTime end) {
        if (host.equals("0.0.0.0")) {
            return new ArrayList<PlantSensorData>();
        }

        GetPlantDataRequest req = GetPlantDataRequest.newBuilder().setPlantId(plantId).setStartDate(start.toEpochSecond(ZoneOffset.UTC)).setEndDate(end.toEpochSecond(ZoneOffset.UTC)).build();
        try {
            PlantSensorDataList list = stub.withDeadlineAfter(timeout, TimeUnit.SECONDS).getPlantSensorData(req);
            return list.getDataList();
        } catch (StatusRuntimeException e) {
            Log.e("GetPlantSensorData", "Failed to get sensor data for plantId=" + String.valueOf(plantId), e);
            throw e;
        }
    }

    private List<Plant> getPlantData() {
        List<Plant> plantList = new ArrayList<>();
        try {
            JSONArray objArray = parseJSONArray("plants.json");
            if (objArray != null) {
                for (int i = 0; i < objArray.length(); i++) {
                    JSONObject plantObj = objArray.getJSONObject(i);
                    Log.i("TAG", plantObj.getString("name"));
                    Plant plant = new Plant(plantObj.getInt("id"), plantObj.getString("name"), plantObj.getString("imageUrl"), LightLevel.MED);
                    plantList.add(plant);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return plantList;
    }



    private JSONArray parseJSONArray(String fileName) {
        AssetManager assets = ctx.getAssets();
        try {
            InputStream inputStream = assets.open(fileName);
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
