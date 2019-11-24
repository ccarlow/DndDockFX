package dock;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class App extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader fxmlLoader =
        new FXMLLoader(App.class.getClassLoader().getResource("dock/App.fxml"));
    try {
      fxmlLoader.load();
      AppController appController = fxmlLoader.getController();
    } catch (IOException e) {
      e.printStackTrace();
    }
    DockManager.getInstance().setLayoutConfigFile("resources/config/dock/app.xml");
    DockManager.getInstance().loadDockLayout();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
