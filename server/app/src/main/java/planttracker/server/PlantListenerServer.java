package planttracker.server;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import planttracker.server.exceptions.PlantTrackerException;

public class PlantListenerServer {
  private final static Logger logger = Logger.getGlobal(); 
  private Server server;

  private Map<Integer, LinkedBlockingQueue<ListenerRequest>> requestQueueMap;

  /* The port on which the server should run */
  private int port;

  public PlantListenerServer(PlantTrackerConfig config) {
    server = null;
    port = config.listenerPort;
    requestQueueMap = new HashMap<Integer, LinkedBlockingQueue<ListenerRequest>>();
  }

  public void start() throws PlantTrackerException {
    try {
      logger.finer("Starting plant listener server on port " + port);
      server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                   .addService(new PlantListenerImpl(requestQueueMap))
                   .build()
                   .start();
    } catch (IOException e) {
      throw new PlantTrackerException("server start fail", e);
    }
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  public void blockUntilShutdown() throws PlantTrackerException {
    try {
      if (server != null) {
        server.awaitTermination();
      }
    } catch (InterruptedException e) {
      throw new PlantTrackerException("failed to shutdown server", e);
    }
  }

  /**
   * Adds a request to a given Pi This function is thread safe and can be called from multiple threads at one.
   * 
   * @param pid The pi the request should be given to.
   * @param request The request to be executed.
   */
  public synchronized void addRequestForPi(int pid, ListenerRequest request) {
    requestQueueMap.get(pid).add(request);
  }

  static class PlantListenerImpl extends PlantListenerGrpc.PlantListenerImplBase {

    private Map<Integer, LinkedBlockingQueue<ListenerRequest>> requestQueueMap;

    PlantListenerImpl(Map<Integer, LinkedBlockingQueue<ListenerRequest>> requestQueueMap) {
      this.requestQueueMap = requestQueueMap;
    }
    @Override
    public void initialize(PlantListenerConfig config, StreamObserver<InitializeResponse> responseObserver) {
      logger.info("Received init request from " + config.getName());
      InitializeResponse response = null;
      String piName = config.getName();
      String uuid = config.getUuid();
      List<MoistureDevice> devices = config.getDevicesList();

      try {
        // Retrieve or insert pi and db pid
        int pid = retrievePid(piName, uuid);
        
        Database db = Database.getInstance();
        // Prepared statement to check if device exists by name
        String checkDeviceQuery = "SELECT COUNT(*) FROM moisture_devices WHERE name = ?";
        PreparedStatement checkStmt = db.connection.prepareStatement(checkDeviceQuery);

        // Prepared statement to insert a new device 
        String insertDeviceQuery = "INSERT INTO moisture_devices (name, num_sensors, pid) VALUES (?, ?, ?)";
        PreparedStatement insertStmt = db.connection.prepareStatement(insertDeviceQuery);

        for (MoistureDevice device : devices) {
          // Check if in database, insert if doesn't exist
          checkStmt.setString(1, device.getName());
          ResultSet resultSet = checkStmt.executeQuery();

          if (resultSet.next() && resultSet.getInt(1) == 0) {
            // Moisture device does not exist, insert new device
            insertStmt.setString(1, device.getName());
            insertStmt.setInt(2, device.getNumSensors());
            insertStmt.setInt(3, pid);

            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 1) {
              logger.info("Moisture device " + device.getName() + " inserted successfully.");
            } else {
              throw new SQLException("Expected 1 affected row after Device insert, but rows affected were: " + affectedRows);
            }
          }
          checkStmt.close();
          insertStmt.close();
          resultSet.close();
        }

        // Create a request queue for the pid
        requestQueueMap.put(pid, new LinkedBlockingQueue<ListenerRequest>());

        ArrayList<PlantSensor> plantList = getPlantSensors(pid);
        Result res = Result.newBuilder().setReturnCode(0).build();
        response = InitializeResponse.newBuilder().setRes(res).addAllPlants(plantList).build();
      } catch (SQLException | PlantTrackerException e) {
        Result res = Result.newBuilder().setReturnCode(1).setError(e.getMessage()).build();
        response = InitializeResponse.newBuilder().setRes(res).build();
      } finally {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
      }
    }

    /**
     * Retrieves all Plants for a pi ID from the database.
     * @param pid Database ID of the pi
     * @return ArrayList of protobuf PlantSensor type.
     * @throws PlantTrackerException
     */
    private ArrayList<PlantSensor> getPlantSensors(int pid) throws PlantTrackerException {

      ArrayList<PlantSensor> plantList = new ArrayList<PlantSensor>();
      Database db = Database.getInstance();

      String plantQuery = "SELECT plants.id AS plant_id, moisture_devices.name AS device_name, moisture_sensor_port"
                        + " FROM plants JOIN moisture_devices ON moisture_sensor_device_id = moisture_devices.id"
                        + " WHERE plants.pid = ?";
      
      try {
        PreparedStatement plantStmt = db.connection.prepareStatement(plantQuery);
        plantStmt.setInt(1, pid);
  
        ResultSet res = plantStmt.executeQuery();
  
        while (res.next()) {
          PlantSensor plant = PlantSensor.newBuilder().setDeviceName(res.getString("device_name"))
                                          .setDevicePort(res.getInt("moisture_sensor_port"))
                                          .setPlantId(res.getInt("plant_id")).build();
          plantList.add(plant);
        }
        plantStmt.close();
        res.close();
      } catch (SQLException e) {
        throw new PlantTrackerException("Failed to retrieve plants for Pi with pid: " + pid, e);
      }
      return plantList;
    }

    /**
     * Retrieves the ID of an existing Pi from the database.
     * If Pi doesn't exist, it is inserted and the new pid is returned. 
     * @param name  Name of the pi 
     * @param uuid  uuid pi
     * @return ID generated by DB on insertion or -1 on failure.
     * @throws PlantTrackerException
     * @throws SQLException
     */
    private int retrievePid(String name, String uuid) throws PlantTrackerException, SQLException {
      int pid = -1;
      Database db = Database.getInstance();

      // Statement to select pi by uuid if exists, returns pid
      String selectPiQuery = "SELECT id FROM pi WHERE uuid = ?";
      PreparedStatement selectStmt = db.connection.prepareStatement(selectPiQuery);      
      selectStmt.setString(1, uuid);
      ResultSet resultSet = selectStmt.executeQuery();

      if (resultSet.next()) {
        // Pi already exists, retrieve pid
        pid = resultSet.getInt("id");
      } else {
        // Pi does not exist, insert and retrieve generated pid
        pid = insertPi(name, uuid);
      }
      selectStmt.close();
      resultSet.close();
      return pid;
    }

    /**
     * Inserts a new Pi to the database.
     * @param name  Name of the pi 
     * @param uuid  uuid pi
     * @return ID generated by DB on insertion or -1 on failure.
     * @throws PlantTrackerException
     * @throws SQLException
     */
    private int insertPi(String name, String uuid) throws PlantTrackerException, SQLException {
      int pid = -1;
      Database db = Database.getInstance();

      String insertPiQuery = "INSERT INTO pi (name, uuid) VALUES (?, ?)";
      PreparedStatement insertStmt = db.connection.prepareStatement(insertPiQuery, PreparedStatement.RETURN_GENERATED_KEYS);
      insertStmt.setString(1, name);
      insertStmt.setString(2, uuid);
      
      int affectedRows = insertStmt.executeUpdate();
      if (affectedRows == 1) {
        // Insert successful, retrieve generated pid
        ResultSet resultSet = insertStmt.getGeneratedKeys();
        if (resultSet.next()) {
          pid = resultSet.getInt(1);
        } 
        resultSet.close();
      } else {
        throw new SQLException("Expected 1 affected row after Pi insert, but rows affected were: " + affectedRows);
      }
      insertStmt.close();
      return pid;
    }

    @Override
    public void reportSensor(PlantSensorDataList request, StreamObserver<Result> responseObserver) {
      logger.finest("Sensor report request received.");
      Result res = Result.newBuilder().setError("").setReturnCode(0).build();
      try {
        Database db = Database.getInstance();
        String insertData = "INSERT INTO plant_sensor_data VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        PreparedStatement insertStmt = db.connection.prepareStatement(insertData);
  
        List<PlantSensorData> dataList = request.getDataList();
        for (PlantSensorData data : dataList) {
          insertStmt.setLong(1, data.getPlantId());
          insertStmt.setFloat(2, data.getMoisture().getMoistureLevel());
          insertStmt.setFloat(3, data.getLight().getLumens());
          insertStmt.setFloat(4, data.getHumidity());
          insertStmt.setFloat(5, data.getTemp());

          int affectedRows = insertStmt.executeUpdate();
          if (affectedRows == 1) {
            logger.finest("Sensor data for Plant ID " + data.getPlantId() + " inserted successfully.");
          } else {
            throw new SQLException("Expected 1 affected row after inserting sensor data, but rows affected were: " + affectedRows);
          }
        }
        insertStmt.close();
      } catch (SQLException | PlantTrackerException e) {
        res = Result.newBuilder().setReturnCode(1).setError(e.getMessage()).build();
        logger.warning("Failed to send save sensor data with: " + e);
      } finally {
        logger.finest("Sensor report response sent.");
        responseObserver.onNext(res);
        responseObserver.onCompleted();
      }
    }

    @Override
    public void poll(PollRequest request, StreamObserver<ListenerRequest> responseObserver) {
      logger.info("Received poll request from " + request.getUuid());      
      LinkedBlockingQueue<ListenerRequest> requestQueue = null;

      try {
        // We can assume the uuid exists at this point because it would have been created in initialize().
        requestQueue = requestQueueMap.get(retrievePid("ignore", request.getUuid()));
      } catch(PlantTrackerException | SQLException e) {
        // This might cause a problem that we are returning a custom exception..... TODO(qawse3dr) look into this.
        responseObserver.onError(e);
        return;
      }
      
      try {
        while(true) {
          ListenerRequest req = requestQueue.take();;
          responseObserver.onNext(req);
        }
      } catch (InterruptedException e) {
        logger.warning("Request was interrupted with: " + e);
      }

    }
  }
}
