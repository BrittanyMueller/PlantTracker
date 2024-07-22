package planttracker.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import planttracker.server.exceptions.PlantTrackerException;

import java.io.IOException;
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
      logger.finer("Starting plant tracker server on port " + port);
      server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                   .addService(new PlantTrackerImpl())
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

    public void addPlant(Plant request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      logger.severe("addPlant Not Implemented");
    }

    public void deletePlant(PlantId request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      logger.severe("deletePlant Not Implemented");
    }

    public void updatePlant(Plant request, io.grpc.stub.StreamObserver<Result> responseObserver) {
      logger.severe("updatePlant Not Implemented");
    }

    public void getPlants(GetPlantsRequest request, io.grpc.stub.StreamObserver<PlantList> responseObserver) {
      logger.severe("getPlants Not Implemented");
    }

    public void getPlantData(GetPlantDataRequest request, io.grpc.stub.StreamObserver<PlantDataList> responseObserver) {
      logger.severe("getPlantData Not Implemented");
    }
  }

}
