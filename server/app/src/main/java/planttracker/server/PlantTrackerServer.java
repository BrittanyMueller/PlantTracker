package planttracker.server;

import com.google.protobuf.Empty;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import planttracker.server.exceptions.PlantTrackerException;

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
                   .build()
                   .start();
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
    @Override
    public void addPlant(PlantInfo request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      Result res = Result.newBuilder().setReturnCode(0).build();

      try {
        // Insert new plant & update sensor, returning generated plant id
        long plantId = insertPlant(request);

        // Get device name to build PlantSensor for listener
        String deviceName = getMoistureDeviceName(request.getMoistureDeviceId());
        PlantSensor sensor = PlantSensor.newBuilder()
                                 .setDeviceName(deviceName)
                                 .setSensorPort(request.getSensorPort())
                                 .setPlantId(plantId)
                                 .build();

        // Request listener for new plant by pi
        ListenerRequest listenerRequest =
            ListenerRequest.newBuilder().setType(ListenerRequestType.NEW_PLANT).setPlant(sensor).build();
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
            name = resultSet.getString("name");
          } else {
            throw new PlantTrackerException(
                "Failed to get device name. Device with ID " + deviceId + " does not exist.");
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
      db.lockDatabase();

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

        ResultSet resultSet = insertStmt.executeQuery();
        if (resultSet.next()) {
          // Insert successful, retrieve generated plant
          plantId = resultSet.getInt("id");

          // Update moisture sensor associated with plant id
          updateStmt.setInt(1, plantId);
          updateStmt.setLong(2, plant.getMoistureDeviceId());
          updateStmt.setInt(3, plant.getSensorPort());

          int affectedRows = updateStmt.executeUpdate();
          if (affectedRows != 1) {
            throw new SQLException(String.format("Failed to update sensor port %d for device %d, %d rows affected.",
                plant.getSensorPort(), plant.getMoistureDeviceId(), affectedRows));
          }
          // Full transaction successful, commit
          db.connection.commit();
          logger.info(String.format("New plant '%s' added.", plant.getName()));
        } else {
          throw new SQLException("Failed to insert new plant with name '" + plant.getName() + "'");
        }
      } catch (SQLException e) {
        db.rollback();
        throw new PlantTrackerException(e);
      } finally {
        db.resetAutoCommit();
        db.unlockDatabase();
      }
      return plantId;
    }

    @Override
    public void deletePlant(PlantId request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      Result response = Result.newBuilder().setReturnCode(0).build();

      String deletePlantSql = "DELETE FROM plants WHERE plants.id = ?";
      // TODO(bam) Create partition for each plant id, delete should drop partition
      String deleteDataSql = "DELETE FROM plant_sensor_data WHERE plant_id = ?";
      String updateSensorSql = "UPDATE sensors SET plant_id = null WHERE plant_id = ?";

      Database db = null;

      try {
        db = Database.getInstance();
      } catch (PlantTrackerException e) {
        responseObserver.onNext(Result.newBuilder().setReturnCode(-1).setError(updateSensorSql).build());
        responseObserver.onCompleted();
        return;
      }

      db.lockDatabase();
      try (PreparedStatement deletePlantStmt = db.connection.prepareStatement(deletePlantSql);
           PreparedStatement deleteDataStmt = db.connection.prepareStatement(deleteDataSql);
           PreparedStatement updateSensorStmt = db.connection.prepareStatement(updateSensorSql);) {
        db.connection.setAutoCommit(false);
        deletePlantStmt.setLong(1, request.getId());
        deleteDataStmt.setLong(1, request.getId());
        updateSensorStmt.setLong(1, request.getId());

        // Ignore return as we don't care if there is data associated with it.
        deleteDataStmt.executeUpdate();

        int affectedRows = updateSensorStmt.executeUpdate();
        if (affectedRows != 1) {
          throw new SQLException(String.format(
              "Error updating sensors for plant with id %d, %d rows affected.", request.getId(), affectedRows));
        }
        affectedRows = deletePlantStmt.executeUpdate();
        if (affectedRows != 1) {
          throw new SQLException(String.format(
              "Error deleting plant for plant with id %d, %d rows affected.", request.getId(), affectedRows));
        }
        db.connection.commit();
        logger.info(String.format("Successfully deleted plant with id %d.", request.getId()));

        // Notify Pi that the plant no longer exists
        // ListenerRequest listenerRequest =
        // ListenerRequest.newBuilder().setType(ListenerRequestType.DELETE_PLANT).setPlant(sensor).build();
        // plantListener.addRequestForPi(request.getPid(), listenerRequest);

      } catch (SQLException e) {
        db.rollback();
        String errStr = String.format("Failed to delete plant with id %d. %s", request.getId(), e.getMessage());
        logger.severe(errStr);
        response = Result.newBuilder().setReturnCode(1).setError(errStr).build();
      } finally {
        db.resetAutoCommit();
        db.unlockDatabase();
      }
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }

    @Override
    public void updatePlant(PlantInfo request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      Result res = Result.newBuilder().setReturnCode(0).build();

      try {
        String sql = "SELECT * FROM plants JOIN sensors ON plants.id = plant_id WHERE id = ?";
        ArrayList<PlantInfo> plantList = selectPlants(sql, request.getId());

        if (plantList.size() != 1) {
          throw new PlantTrackerException("Failed to retrieve existing plant with id %d.");
        }

        PlantInfo oldPlant = plantList.get(0);

        // Update plant record and sensors
        updatePlantTransaction(request, oldPlant);

        String deviceName = getMoistureDeviceName(request.getMoistureDeviceId());
        PlantSensor sensor = PlantSensor.newBuilder()
                                 .setPlantId(request.getId())
                                 .setDeviceName(deviceName)
                                 .setSensorPort(request.getSensorPort())
                                 .build();

        if (request.getPid() != oldPlant.getPid()) {
          // Remove listener from old pi
          ListenerRequest deleteListener =
              ListenerRequest.newBuilder().setType(ListenerRequestType.DELETE_PLANT).build();
          plantListener.addRequestForPi(oldPlant.getPid(), deleteListener);
          // Add new listener for plant to new pi
          ListenerRequest newListener =
              ListenerRequest.newBuilder().setType(ListenerRequestType.NEW_PLANT).setPlant(sensor).build();
          plantListener.addRequestForPi(request.getPid(), newListener);
        } else {
          // Update existing pi to use new sensor/device
          ListenerRequest listenerRequest =
              ListenerRequest.newBuilder().setType(ListenerRequestType.UPDATE_PLANT).setPlant(sensor).build();
          plantListener.addRequestForPi(request.getPid(), listenerRequest);
        }
      } catch (PlantTrackerException e) {
        logger.severe(String.format("Request to update plant with id %d failed." + e.getMessage(), request.getId()));
        res = Result.newBuilder().setReturnCode(1).setError(e.getMessage()).build();
      } finally {
        responseObserver.onNext(res);
        responseObserver.onCompleted();
      }
    }

    private void updatePlantTransaction(PlantInfo newPlant, PlantInfo oldPlant) throws PlantTrackerException {
      Database db = Database.getInstance();
      db.lockDatabase();

      String updatePlantSql =
          "UPDATE plants SET name = ?, image_url = ?, light_level = ?, min_moisture = ?, min_humidity = ?, pid = ? WHERE id = ?";
      String updateSensorSql = "UPDATE sensors SET plant_id = ? WHERE moisture_device_id = ? AND sensor_port = ?";
      String resetSensorSql = "UPDATE sensors SET plant_id = null WHERE plant_id = ?";

      try (PreparedStatement updatePlantStmt = db.connection.prepareStatement(updatePlantSql);
           PreparedStatement updateSensorStmt = db.connection.prepareStatement(updateSensorSql);
           PreparedStatement resetSensorStmt = db.connection.prepareStatement(resetSensorSql);) {
        db.connection.setAutoCommit(false);

        // Add plant data to update query
        updatePlantStmt.setString(1, newPlant.getName());
        updatePlantStmt.setString(2, newPlant.getImageUrl());
        updatePlantStmt.setInt(3, newPlant.getLightLevelValue());
        updatePlantStmt.setInt(4, newPlant.getMinMoisture());
        updatePlantStmt.setInt(5, newPlant.getMinHumidity());
        updatePlantStmt.setLong(6, newPlant.getPid());
        updatePlantStmt.setLong(7, newPlant.getId());

        int affectedRows = updatePlantStmt.executeUpdate();
        if (affectedRows == 0) {
          throw new SQLException(String.format("Update for plant with id %d failed.", newPlant.getId()));
        } else if (affectedRows > 1) {
          throw new SQLException(String.format(
              "Unexpected update: %d rows affected when updating plant with %d.", affectedRows, newPlant.getId()));
        }

        if (newPlant.getMoistureDeviceId() != oldPlant.getMoistureDeviceId()
            || oldPlant.getSensorPort() != oldPlant.getSensorPort()) {
          // Reset previous sensor to null
          resetSensorStmt.setLong(1, newPlant.getId());

          affectedRows = resetSensorStmt.executeUpdate();
          if (affectedRows == 0) {
            throw new SQLException(String.format("Reset to NULL for sensor port %d for device %d failed.",
                newPlant.getSensorPort(), newPlant.getMoistureDeviceId()));
          } else if (affectedRows > 1) {
            throw new SQLException(
                String.format("Unexpected update: %d rows affected when resetting sensor for plant with %d.",
                    affectedRows, newPlant.getId()));
          }

          // Add new sensor data to update query
          updateSensorStmt.setLong(1, newPlant.getId());
          updateSensorStmt.setLong(2, newPlant.getMoistureDeviceId());
          updateSensorStmt.setLong(3, newPlant.getSensorPort());

          affectedRows = updateSensorStmt.executeUpdate();
          if (affectedRows == 0) {
            throw new SQLException(String.format("Update to sensor port %d for device %d failed.",
                newPlant.getSensorPort(), newPlant.getMoistureDeviceId()));
          } else if (affectedRows > 1) {
            throw new SQLException(
                String.format("Unexpected update: %d rows affected when updating sensor for plant with %d.",
                    affectedRows, newPlant.getId()));
          }
        }
        db.connection.commit();
        logger.info(String.format("Successfully updated plant with id %d.", newPlant.getId()));
      } catch (SQLException e) {
        db.rollback();
        throw new PlantTrackerException(e);
      } finally {
        db.resetAutoCommit();
        db.unlockDatabase();
      }
    }

    @Override
    public void getPlants(GetPlantsRequest request, io.grpc.stub.StreamObserver<GetPlantsResponse> responseObserver) {
      GetPlantsResponse response = null;
      ArrayList<PlantInfo> plantList = null;
      String sql = "SELECT * FROM plants JOIN sensors ON plants.id = plant_id";

      try {
        switch (request.getType()) {
          case GET_PLANT:
            if (!request.hasId()) {
              throw new PlantTrackerException(
                  "Request type " + GetPlantsRequestType.GET_PLANT.toString() + " requires an ID.");
            }
            logger.info("Request to GET_PLANT with ID " + request.getId() + " received.");
            plantList = selectPlants(sql + " WHERE id = ?", request.getId());
            break;
          case GET_PLANTS_BY_PI:
            if (!request.hasId()) {
              throw new PlantTrackerException(
                  "Request type " + GetPlantsRequestType.GET_PLANTS_BY_PI.toString() + " requires an ID.");
            }
            logger.info("Request to GET_PLANTS_BY_PI with ID " + request.getId() + " received.");
            plantList = selectPlants(sql + " WHERE pid = ?", request.getId());
            break;
          case GET_ALL_PLANTS:
            logger.info("Request to GET_ALL_PLANTS received.");
            plantList = selectPlants(sql, -1);
            break;
          default:
            throw new PlantTrackerException("Invalid GetPlants request type.");
        }
        Result res = Result.newBuilder().setError("").setReturnCode(0).build();
        response = GetPlantsResponse.newBuilder().setRes(res).addAllPlants(plantList).build();
      } catch (PlantTrackerException e) {
        logger.severe("Request to getPlants failed." + e.getMessage());
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
     * @return  Array of PlantInfo selected from DB.
     * @throws PlantTrackerException
     */
    private ArrayList<PlantInfo> selectPlants(String sql, long id) throws PlantTrackerException {
      ArrayList<PlantInfo> plantList = new ArrayList<PlantInfo>();
      Database db = Database.getInstance();
      db.lockDatabase();

      try {
        PreparedStatement selectStmt = db.connection.prepareStatement(sql);
        if (id != -1) {
          // Set optional id field in where clause
          selectStmt.setLong(1, id);
        }
        ResultSet res = selectStmt.executeQuery();
        while (res.next()) {
          plantList.add(buildPlantInfo(res));
        }
        selectStmt.close();
        res.close();
      } catch (SQLException e) {
        logger.severe("Failed to select plants with: " + e.getMessage());
        throw new PlantTrackerException(e);
      } finally {
        db.unlockDatabase();
      }
      return plantList;
    }

    /**
     * Builds a new Protobuf PlantInfo from a JDBC ResultSet.
     * @param res ResultSet obtained after selecting a Plant from the DB.
     * @return PlantInfo built using the ResultSet data.
     * @throws SQLException
     */
    private PlantInfo buildPlantInfo(ResultSet res) throws SQLException {
      PlantInfo.Builder plant = PlantInfo.newBuilder()
                                    .setId(res.getLong("id"))
                                    .setName(res.getString("name"))
                                    .setLightLevelValue(res.getInt("light_level"))
                                    .setMinMoisture(res.getInt("min_moisture"))
                                    .setMinHumidity(res.getInt("min_humidity"))
                                    .setImageUrl(res.getString("image_url"))
                                    .setPid(res.getLong("pid"))
                                    .setMoistureDeviceId(res.getLong("moisture_device_id"))
                                    .setSensorPort(res.getInt("sensor_port"));
      PlantSensorData data = plantListener.getLastReport(plant.getId());
      if (data != null) {
        plant.setLastReport(data);
      }

      return plant.build();
    }

    @Override
    public void getPlantSensorData(GetPlantDataRequest request, StreamObserver<PlantSensorDataList> responseObserver) {
      PlantSensorDataList.Builder data = PlantSensorDataList.newBuilder();

      String sql = "SELECT * FROM plant_sensor_data "
          + "WHERE plant_id = ? AND ts BETWEEN ? and ? "
          + "ORDER BY ts ASC";
      Database db = null;
      try {
        db = Database.getInstance();
      } catch (PlantTrackerException e) {
        responseObserver.onError(e);
        return;
      }

      try (PreparedStatement selectStmt = db.connection.prepareStatement(sql);) {
        db.lockDatabase();

        selectStmt.setLong(1, request.getPlantId());
        selectStmt.setTimestamp(2, new Timestamp(request.getStartDate()));
        selectStmt.setTimestamp(3, new Timestamp(request.getEndDate()));

        logger.finest("Getting Sensor data for " + request.toString() + " QUERY " + selectStmt.toString());

        selectStmt.executeQuery();
        ResultSet resultSet = selectStmt.executeQuery();

        while (resultSet.next()) {
          data.addData(
                  PlantSensorData.newBuilder()
                      .setEpochTs(resultSet.getTimestamp("ts").getTime())
                      .setHumidity(resultSet.getFloat("humidity"))
                      .setTemp(resultSet.getFloat("temp"))
                      .setLight(LightSensorData.newBuilder().setLumens(resultSet.getFloat("light")).build())
                      .setMoisture(
                          MoistureSensorData.newBuilder().setMoistureLevel(resultSet.getFloat("moisture")).build()))
              .build();
        }
        resultSet.close();
        responseObserver.onNext(data.build());
      } catch (SQLException e) {
        logger.warning("Failed to retrieve plant data with: " + e);
        responseObserver.onError(e);
        return;
      } finally {
        if (db != null) {
          db.unlockDatabase();
        }
      }

      responseObserver.onCompleted();
    }

    @Override
    public void getAvailablePiSensors(
        Empty request, io.grpc.stub.StreamObserver<GetAvailablePiResponse> responseObserver) {
      ArrayList<Pi> piList = null;
      GetAvailablePiResponse response = null;
      Result.Builder res = Result.newBuilder();

      try {
        // Query for Pi with available sensor ports
        piList = selectAvailablePi();
        res.setReturnCode(0).build();
        response = GetAvailablePiResponse.newBuilder().setRes(res).addAllPiList(piList).build();
      } catch (PlantTrackerException e) {
        logger.severe("Failed to retrieve available Pi sensors: " + e);
        res.setReturnCode(1).setError(e.getMessage()).build();
        response = GetAvailablePiResponse.newBuilder().setRes(res).build();
      } finally {
        logger.finest("Response sent for getAvailablePiSensors.");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    }

    private ArrayList<Pi> selectAvailablePi() throws PlantTrackerException {
      ArrayList<Pi> piList = new ArrayList<Pi>();
      Database db = Database.getInstance();

      String sql =
          "SELECT pi.id AS pid, pi.name AS pi_name, moisture_devices.id AS mid, moisture_devices.name AS device_name, sensor_port "
          + "FROM pi JOIN moisture_devices ON pid = pi.id "
          + "JOIN sensors ON moisture_device_id = moisture_devices.id AND sensors.plant_id IS NULL;";

      try (PreparedStatement stmt = db.connection.prepareStatement(sql); ResultSet resultSet = stmt.executeQuery()) {
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
                device = md; // Update sensor ports for existing device
                md.addSensorPorts(port);
                break;
              }
            }
            if (device == null) {
              // New device  found, initialize builder with data
              AvailableMoistureDevice.Builder newDevice =
                  AvailableMoistureDevice.newBuilder().setId(mid).setName(deviceName).addSensorPorts(port);
              pi.addDeviceList(newDevice);
            }
          } else {
            // New Pi record,
            AvailableMoistureDevice.Builder newDevice =
                AvailableMoistureDevice.newBuilder().setId(mid).setName(deviceName).addSensorPorts(port);
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
