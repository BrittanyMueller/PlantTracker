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
import java.util.concurrent.TimeUnit;

import java.util.logging.*;

public class PlantTrackerServer {

  private final static Logger logger = Logger.getGlobal(); 
  private Server server;

  /* The port on which the server should run */
  private int port;

  public PlantTrackerServer(PlantTrackerConfig config) {
    server = null;
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
      logger.severe("addPlant Not Implemented");
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
      String sql = "SELECT * FROM plants";
      
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
          plantList.add(buildPlant(res, fetchImage));
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
    private PlantInfo buildPlant(ResultSet res, boolean fetchImage) throws SQLException {
      PlantInfo.Builder plant = PlantInfo.newBuilder().setId(res.getLong("id"))
                                      .setName(res.getString("name"))
                                      .setLightLevelValue(res.getInt("light_level"))
                                      .setMinMoisture(res.getInt("min_moisture"))
                                      .setMinHumidity(res.getInt("min_humidity"))
                                      .setImageUrl(res.getString("image_url"))
                                      .setPid(res.getLong("pid"));
      if (fetchImage) {
        // TODO image as byte blob
        plant.setImage(null);
      }
      return plant.build();
    }

    public void getPlantData(GetPlantDataRequest request, io.grpc.stub.StreamObserver<PlantSensorDataList> responseObserver) {
      logger.severe("getPlantData Not Implemented");
    }
  }

}
