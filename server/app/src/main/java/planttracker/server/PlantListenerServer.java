package planttracker.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import planttracker.server.exceptions.PlantTrackerException;

import java.beans.Statement;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.util.logging.*;

public class PlantListenerServer {
  private final static Logger logger = Logger.getGlobal(); 
  private Server server;

  /* The port on which the server should run */
  private int port;

  public PlantListenerServer(PlantTrackerConfig config) {
    server = null;
    port = config.listenerPort;
  }

  public void start() throws PlantTrackerException {
    try {
      logger.finer("Starting plant listener server on port " + port);
      server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                   .addService(new PlantListenerImpl())
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

  static class PlantListenerImpl extends PlantListenerGrpc.PlantListenerImplBase {
    @Override
    public void initialize(PlantListenerConfig config, StreamObserver<InitializeResponse> responseObserver) {
      logger.info("Received init request from " + config.getName());
      Result res = Result.newBuilder().setReturnCode(0).build();  // Return code 0 for success
      
      String piName = config.getName();
      String mac = config.getMac();
      List<MoistureDevice> devices = config.getDevicesList();

      try {
        Database db = Database.getInstance();
        
        // Retrieve or insert pi and db pid
        int pid = retrievePid(piName, mac);

        // Prepared statement to check if device exists by name
        String checkDeviceQuery = "SELECT COUNT(*) FROM moisture_devices WHERE name = ?";
        PreparedStatement checkStmt = db.connection.prepareStatement(checkDeviceQuery);

        // Prepared statement to insert a new device 
        String insertDeviceQueryString = "INSERT INTO moisture_devices (name, num_sensors, pid) VALUES (?, ?, ?)";
        PreparedStatement insertStmt = db.connection.prepareStatement(insertDeviceQueryString);

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
      } catch (SQLException e) {

      } catch (PlantTrackerException e) {

      } finally {

      }

      // TODO add pi in config to database if doesn't exist 
      // TODO return plants associated with that pi from database, ignore device
      // if pi doesn't exist, plants cannot exist, skip that query 

      InitializeResponse response = InitializeResponse.newBuilder().setRes(res).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }

    // Returns db pid associated with existing or newly inserted Pi
    private int retrievePid(String name, String mac) throws PlantTrackerException, SQLException {
      int pid = -1;

      Database db = Database.getInstance();

      // Statement to select pi by mac address if exists, returns pid
      String selectPiQuery = "SELECT pid FROM pi WHERE mac = ?";
      PreparedStatement selectStmt = db.connection.prepareStatement(selectPiQuery);      
      selectStmt.setString(1, mac);
      ResultSet resultSet = selectStmt.executeQuery();

      if (resultSet.next()) {
        // Pi already exists, retrieve pid
        pid = resultSet.getInt("pid");
      } else {
        // Pi does not exist, insert and retrieve generated pid
        String insertPiQuery = "INSERT INTO pi (name, mac) VALUES (?, ?)";
        PreparedStatement insertStmt = db.connection.prepareStatement(insertPiQuery, PreparedStatement.RETURN_GENERATED_KEYS);
        insertStmt.setString(1, name);
        insertStmt.setString(2, mac);
        
        int affectedRows = insertStmt.executeUpdate();
        if (affectedRows == 1) {
          // TODO do i need to close result set before reusing it here?
          // Insert successful, retrieve generated pid
          resultSet = insertStmt.getGeneratedKeys();
          if (resultSet.next()) {
            pid = resultSet.getInt(1);
          }
        } else {
          // TODO things don't get closed properly yet, need finally try catch hell?? close() throws exception...
          throw new SQLException("Expected 1 affected row after Pi insert, but rows affected were: " + affectedRows);
        }
        insertStmt.close();
      }
      selectStmt.close();
      resultSet.close();
      return pid;
    }

    @Override
    public void reportSensor(PlantDataList request, StreamObserver<Result> responseObserver) {
      System.out.println("Sensor report");
      responseObserver.onNext(Result.newBuilder().setReturnCode(0).build());
      responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<ListenerResponse> pollRequest(StreamObserver<ListenerResponse> responseObserver) {
      return null;
    }
  }
}
