package dock;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class App extends Application {
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    
	  DockManager dockManager = new DockManager();
	  
    FXMLLoader fxmlLoader =
        new FXMLLoader(App.class.getClassLoader().getResource("dock/App.fxml"));
    try {
      DockPane dockPane = fxmlLoader.load();
      dockManager.addDockPane(dockPane);
      fxmlLoader.getController();
      
      dockPane.setDockManager(dockManager);
      
      dockManager.setLayoutConfigFile("resources/config/dock/app.xml");
      dockManager.loadDockLayout();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
