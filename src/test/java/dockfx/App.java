package dockfx;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class App extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader fxmlLoader =
        new FXMLLoader(App.class.getClassLoader().getResource("dockfx/App.fxml"));
    try {
      fxmlLoader.load();
      AppController appController = fxmlLoader.getController();
    } catch (IOException e) {
      e.printStackTrace();
    }
    DockFX.getInstance().setLayoutConfigFile("resources/config/dockfx/app.xml");
    DockFX.getInstance().loadDockLayout();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
