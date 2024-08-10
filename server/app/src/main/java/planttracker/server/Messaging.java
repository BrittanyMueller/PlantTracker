package planttracker.server;


public class Messaging {
  private static Messaging instance = null;

  private Messaging() {
  
    // FirebaseApp.initializeApp();
  }

  public static synchronized Messaging getInstance() {
      if (instance == null) {
        instance = new Messaging();
      }

      return instance;
  }

}
