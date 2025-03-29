package planttracker.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import planttracker.server.exceptions.PlantTrackerException;

public class PlantListenerServer {
  private final static Logger logger = Logger.getGlobal();
  private Server server;
  private PlantListenerServerImpl impl;

  /* The port on which the server should run */
  private int port;

  public PlantListenerServer(PlantTrackerConfig config) {
    server = null;
    port = config.listenerPort;
  }

  public void start() throws PlantTrackerException {
    try {
      impl = new PlantListenerServerImpl();
      logger.finer("Starting plant listener server on port " + port);
      server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create()).addService(impl).build().start();

    } catch (IOException e) {
      throw new PlantTrackerException("server start fail", e);
    }
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
      server.getServices().get(0);
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

  public PlantListenerServerImpl getImpl() {
    return impl;
  }
}
