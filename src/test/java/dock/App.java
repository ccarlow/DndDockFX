package dock;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class App extends Application {

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getClassLoader().getResource("dock/App.fxml"));
    try {
      DockPane dockPane = fxmlLoader.load();
      DockManager dockManager = new DockManager();
      dockManager.addDockPane(dockPane);
      dockPane.setDockManager(dockManager);

      dockManager.setLayoutConfigFile("resources/config/dock/app.xml");
      dockManager.loadDockLayout();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
