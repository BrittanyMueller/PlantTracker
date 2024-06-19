package planttracker.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import planttracker.server.exceptions.PlantTrackerException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PlantListenerServer {
  private Server server;

  /* The port on which the server should run */
  private int port;

  public PlantListenerServer(PlantTrackerConfig config) {
    server = null;
    port = config.listenerPort;
  }

  public void start() throws PlantTrackerException {
    try {
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
      System.out.println("Received init request from " + config.getName());
      Result res = Result.newBuilder().setReturnCode(0).build();
      // TODO add pi in config to database if doesn't exist 
      // TODO return plants associated with that pi from database
      InitializeResponse response = InitializeResponse.newBuilder().setRes(res).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
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
