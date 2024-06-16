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
  private int port;

  public PlantListenerServer(PlantTrackerConfig config) {

  }

  public void start() throws PlantTrackerException {
    /* The port on which the server should run */
    int port = 50051;
    // todo replace port with from config
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
    public void initialize(PlantListenerConfig request, StreamObserver<InitializeResponse> responseObserver) {
      System.out.println("Got init request from " + request.getName());
      PlantListenerConfig cfg = PlantListenerConfig.newBuilder(request).build();
      Result res = Result.newBuilder().setReturnCode(0).build();
      InitializeResponse response = InitializeResponse.newBuilder().setCfg(cfg).setRes(res).build();
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
