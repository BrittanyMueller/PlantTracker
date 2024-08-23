package planttracker.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import planttracker.server.exceptions.PlantTrackerException;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import java.util.logging.*;

import com.google.protobuf.Empty;

public class PlantTrackerServer {

  private final static Logger logger = Logger.getGlobal(); 
  private static PlantListenerServer plantListener;
  private Server server;

  /* The port on which the server should run */
  private int port;

  public PlantTrackerServer(PlantTrackerConfig config, PlantListenerServer listener) {
    server = null;
    plantListener = listener;
    port = config.trackerPort;
  }

  public void start() throws PlantTrackerException {
    try {
      logger.finer("Starting PlantTracker Server on port " + port);
      server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                   .addService(new PlantTrackerImpl())
                   .build().start();
    } catch (IOException e) {
      throw new PlantTrackerException("Failed to start PlantTracker Server", e);
    }
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  public void blockUntilShutdown() throws PlantTrackerException {
    try {
      if (server != null) {
        server.awaitTermination();
      }
    } catch (InterruptedException e) {
      throw new PlantTrackerException("failed to shutdown server", e);
    }
  }

  static class PlantTrackerImpl extends PlantTrackerGrpc.PlantTrackerImplBase {

    public void addPlant(PlantInfo request, io.grpc.stub.StreamObserver<Result> responseObserver) {

      Result res = Result.newBuilder().setReturnCode(0).build();

      try {
        // Insert new plant & update sensor, returning generated plant id
        long plantId = insertPlant(request);
        
        // Get device name to build PlantSensor for listener
        String deviceName = getMoistureDeviceName(request.getMoistureDeviceId());
        PlantSensor sensor = PlantSensor.newBuilder().setDeviceName(deviceName)
                                        .setSensorPort(request.getSensorPort())
                                        .setPlantId(plantId).build();
        
        // Request listener for new plant by pi
        ListenerRequest listenerRequest = ListenerRequest.newBuilder().setType(ListenerRequestType.NEW_PLANT).setPlant(sensor).build();
        plantListener.addRequestForPi(request.getPid(), listenerRequest);

      } catch (PlantTrackerException e) {
        res = Result.newBuilder().setReturnCode(1).setError(e.getMessage()).build();
        logger.severe("Failed to add new plant with: " + e);
      } finally {
        logger.finest("Add plant response sent.");
        responseObserver.onNext(res);
        responseObserver.onCompleted();
      }      
    }

    private String getMoistureDeviceName(long deviceId) throws PlantTrackerException {
      String name = null;
      Database db = Database.getInstance();

      String sql = "SELECT name FROM moisture_devices WHERE id = ?";

      try (PreparedStatement stmt = db.connection.prepareStatement(sql)) {
        stmt.setLong(1, deviceId);

        try (ResultSet resultSet = stmt.executeQuery()) {
          if (resultSet.next()) {
            // Select successful, retrieve name
            name = resultSet.getString("name");
          } else {
            // Device not found
            throw new PlantTrackerException("Failed to get device name. Device with ID " + deviceId + " does not exist.");
          }
        }
      } catch (SQLException e) {
        System.out.println(e.getMessage());
        throw new PlantTrackerException(e);
      }
      return name;
    }

    private int insertPlant(PlantInfo plant) throws PlantTrackerException {
      int plantId = -1;

      Database db = Database.getInstance();

      String insertPlantSql = "INSERT INTO plants (name, image_url, light_level, min_moisture, min_humidity, pid)"
                            + " VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
      String updateSensorSql = "UPDATE sensors SET plant_id = ? WHERE moisture_device_id = ? AND sensor_port = ?";

      try (PreparedStatement insertStmt = db.connection.prepareStatement(insertPlantSql);
           PreparedStatement updateStmt = db.connection.prepareStatement(updateSensorSql);) {
        
        db.connection.setAutoCommit(false);

        insertStmt.setString(1, plant.getName());
        insertStmt.setString(2, plant.getImageUrl());
        insertStmt.setInt(3, plant.getLightLevelValue());
        insertStmt.setInt(4, plant.getMinMoisture());
        insertStmt.setInt(5, plant.getMinHumidity());
        insertStmt.setLong(6, plant.getPid());
        
        try (ResultSet resultSet = insertStmt.executeQuery()) {
          if (resultSet.next()) {
            // Insert successful, retrieve generated plant
            plantId = resultSet.getInt("id");
  
            // Update moisture sensor associated with plant id
            updateStmt.setInt(1, plantId);
            updateStmt.setLong(2, plant.getMoistureDeviceId());
            updateStmt.setInt(3, plant.getSensorPort());

            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows != 1) {
              throw new SQLException("Expected to update 1 row, but updated " + affectedRows + " rows for sensor with device ID " + plant.getMoistureDeviceId());
            }
            // Full transaction successful, commit
            db.connection.commit();
            logger.info(String.format("New plant '%s' added.", plant.getName()));
          } else {
            throw new SQLException("Failed to insert new Plant with name '" + plant.getName() + "'");
          }
        }
      } catch (SQLException e) {
        db.rollback();
        System.out.println(e.getMessage());
        throw new PlantTrackerException(e);
      } finally {
        db.resetAutoCommit();
      }
      return plantId;
    }

    public void deletePlant(PlantId request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      logger.severe("deletePlant Not Implemented");
    }

    public void updatePlant(PlantInfo request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      logger.severe("updatePlant Not Implemented");
    }

    public void getPlants(GetPlantsRequest request, io.grpc.stub.StreamObserver<GetPlantsResponse> responseObserver) {
      GetPlantsResponse response = null;
      ArrayList<PlantInfo> plantList = null;
      String sql = "SELECT * FROM plants JOIN sensors ON plants.id = plant_id";
      
      try {
        switch(request.getType()) {
          case GET_PLANT:
            if (!request.hasId()) {
              throw new PlantTrackerException("Request type " + GetPlantsRequestType.GET_PLANT.toString() + " requires an ID.");
            }
            logger.info("Request to GET_PLANT with ID " + request.getId() + " received.");
            plantList = selectPlants(sql + " WHERE id = ?", request.getId(), request.getFetchImages());
            break;
          case GET_PLANTS_BY_PI:
            if (!request.hasId()) {
              throw new PlantTrackerException("Request type " + GetPlantsRequestType.GET_PLANTS_BY_PI.toString() + " requires an ID.");
            }
            logger.info("Request to GET_PLANTS_BY_PI with ID " + request.getId() + " received.");
            plantList = selectPlants(sql + " WHERE pid = ?", request.getId(), request.getFetchImages());
            break;
          case GET_ALL_PLANTS:
            logger.info("Request to GET_ALL_PLANTS received.");
            plantList = selectPlants(sql, -1, request.getFetchImages());
            break;
          default:
            throw new PlantTrackerException("Invalid request type.");
        }
        Result res = Result.newBuilder().setError("").setReturnCode(0).build();
        response = GetPlantsResponse.newBuilder().setRes(res).addAllPlants(plantList).build();
      } catch (PlantTrackerException e) {
        System.out.println(e.getMessage());
        Result res = Result.newBuilder().setReturnCode(1).setError(e.getMessage()).build();
        response = GetPlantsResponse.newBuilder().setRes(res).build();
      } finally {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    }

    /**
     * Executes the provided select statement to get PlantInfo from DB.
     * @param sql Select query to plants table, optional where clause.
     * @param id  Optional ID to be set as where condition.
     * @param fetchImage  Flag indicating if images should be...
     * @return  Array of PlantInfo selected from DB.
     * @throws PlantTrackerException
     */
    private ArrayList<PlantInfo> selectPlants(String sql, long id, boolean fetchImage) throws PlantTrackerException {

      ArrayList<PlantInfo> plantList = new ArrayList<PlantInfo>();
      Database db = Database.getInstance();

      try {
        PreparedStatement selectStmt = db.connection.prepareStatement(sql);
        if (id != -1) {
          // Set optional id field in where clause
          selectStmt.setLong(1, id);
        }      
        ResultSet res = selectStmt.executeQuery();
        while (res.next()) {
          plantList.add(buildPlantInfo(res, fetchImage));
        }
        selectStmt.close();
        res.close();
      } catch (SQLException e) {
        // TODO logging or better error message idk
        System.out.println(e.getMessage());
        throw new PlantTrackerException(e);
      }
      return plantList;
    }

    /**
     * Builds a new Protobuf PlantInfo from a JDBC ResultSet.
     * @param res ResultSet obtained after selecting a Plant from the DB. 
     * @param fetchImage Flag indicating if images should be...
     * @return PlantInfo built using the ResultSet data.
     * @throws SQLException
     */
    private PlantInfo buildPlantInfo(ResultSet res, boolean fetchImage) throws SQLException {
      PlantInfo.Builder plant = PlantInfo.newBuilder().setId(res.getLong("id"))
                                      .setName(res.getString("name"))
                                      .setLightLevelValue(res.getInt("light_level"))
                                      .setMinMoisture(res.getInt("min_moisture"))
                                      .setMinHumidity(res.getInt("min_humidity"))
                                      .setImageUrl(res.getString("image_url"))
                                      .setPid(res.getLong("pid"))
                                      .setMoistureDeviceId(res.getLong("moisture_device_id"))
                                      .setSensorPort(res.getInt("sensor_port"));
      if (fetchImage) {
        // TODO image as byte blob
        plant.setImage(null);
      }
      return plant.build();
    }

    public void getPlantData(GetPlantDataRequest request, io.grpc.stub.StreamObserver<PlantSensorDataList> responseObserver) {
      logger.severe("getPlantData Not Implemented");
    }
  
    public void getAvailablePiSensors(Empty request, io.grpc.stub.StreamObserver<GetAvailablePiResponse> responseObserver) {
      ArrayList<Pi> piList = null;
      GetAvailablePiResponse res = null;

      try {
        // Query for Pi with available sensor ports
        piList = selectAvailablePi();
        res = GetAvailablePiResponse.newBuilder().addAllPiList(piList).build();
      } catch (PlantTrackerException e) {
        logger.severe("Failed to retrieve available Pi sensors: " + e);
      } finally {
        responseObserver.onNext(res);
        responseObserver.onCompleted();
      }
    }

    private ArrayList<Pi> selectAvailablePi() throws PlantTrackerException {

      ArrayList<Pi> piList = new ArrayList<Pi>();
      Database db = Database.getInstance();

      String sql = "SELECT pi.id AS pid, pi.name AS pi_name, moisture_devices.id AS mid, moisture_devices.name AS device_name, sensor_port " +
                   "FROM pi JOIN moisture_devices ON pid = pi.id " +
                   "JOIN sensors ON moisture_device_id = moisture_devices.id AND sensors.plant_id IS NULL;";

      try (PreparedStatement stmt = db.connection.prepareStatement(sql);
          ResultSet resultSet = stmt.executeQuery()) {

        Map<Long, Pi.Builder> piMap = new HashMap<>();

        while (resultSet.next()) {
          // Available sensors found, build message for response
          long pid = resultSet.getLong("pid");
          String piName = resultSet.getString("pi_name");
          long mid = resultSet.getLong("mid");
          String deviceName = resultSet.getString("device_name");
          int port = resultSet.getInt("sensor_port");

          Pi.Builder pi = piMap.get(pid);
          if (pi != null) {
            // Pi exists already, add associated device if doesn't exist
            AvailableMoistureDevice.Builder device = null;
            for (AvailableMoistureDevice.Builder md : pi.getDeviceListBuilderList()) {
              if (md.getId() == mid) {
                device = md;  // Update sensor ports for existing device
                md.addSensorPorts(port);
                break;
              }
            }
            if (device == null) {
              // New device  found, initialize builder with data
              AvailableMoistureDevice.Builder newDevice = AvailableMoistureDevice.newBuilder().setId(mid).setName(deviceName).addSensorPorts(port);
              pi.addDeviceList(newDevice);
            }
          } else {
            // New Pi record, 
            AvailableMoistureDevice.Builder newDevice = AvailableMoistureDevice.newBuilder().setId(mid).setName(deviceName).addSensorPorts(port);
            Pi.Builder newPi = Pi.newBuilder().setPid(pid).setName(piName).addDeviceList(newDevice);
            piMap.put(pid, newPi);
          }
        }
        // Finished parsing, convert builders into list
        for (Pi.Builder piBuilder : piMap.values()) {
          piList.add(piBuilder.build());
        }
      } catch (SQLException e) {
        System.out.println(e.getMessage());
        throw new PlantTrackerException(e);
      }
      return piList;
    }
  }

}
