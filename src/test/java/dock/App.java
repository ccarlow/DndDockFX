package dock;

import java.io.IOException;
import org.eclipse.fx.ui.controls.tabpane.DndTabPane;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory;
import org.eclipse.fx.ui.controls.tabpane.DndTabPaneFactory.FeedbackType;
import dock.DockManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.Pane;
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
