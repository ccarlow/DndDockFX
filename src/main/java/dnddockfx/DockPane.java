package dnddockfx;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.List;
import dnddockfx.DockManager.DockPaneTabContextMenu;
import dnddockfx.DockManager.DockPaneTabDragOverContextMenu;
import dnddockfx.DockManager.DockPos;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class DockPane extends StackPane {
  private String title;
  private Node content;
  protected ParentDockPane parentDockPane;
  private DockPane groupDockPane;
  private ObservableList<DockPane> dockPanes = FXCollections.observableArrayList();
  private Rectangle originalStage = new Rectangle();
  private DockPaneArea dockPaneArea = new DockPaneArea();
  private DockPaneTab dockPaneTab = new DockPaneTab();
  private Menu menuItem;
  protected DockManager dockManager;

  public static final String DOCK_HIDDEN_HEADER_TABPANE_STYLE_CLASS = "dock-hidden-header-tab-pane";

  public DockPane() {
    // dockManager.addDockPane(this);
    dockPaneTab.setContent(this);
    getChildren().add(dockPaneArea);
    dockPaneArea.setVisible(false);

    dockPanes.addListener(new ListChangeListener<DockPane>() {
      @Override
      public void onChanged(Change<? extends DockPane> c) {
        if (c.next()) {
          for (DockPane node : c.getAddedSubList()) {
            node.setGroupDockPane(DockPane.this);
            node.setDockManager(dockManager);
          }
        }
      }
    });
  }

  public void setDockManager(DockManager dockManager) {
    this.dockManager = dockManager;
  }

  public DockManager getDockManager() {
    return dockManager;
  }

  public DockPane(String title, Node content) {
    this();
    setTitle(title);
    setContent(content);
  }

  public void setContent(Node content) {
    if (this.content != null) {
      getChildren().set(0, content);
    } else {
      getChildren().add(0, content);
    }
    this.content = content;
  }

  protected void setMenuItem(Menu menuItem) {
    this.menuItem = menuItem;
  }

  public List<DockPane> getDockPanes() {
    return dockPanes;
  }

  protected Menu getMenuItem() {
    return menuItem;
  }

  public DockPane getRootDockPane() {
    if (parentDockPane != null) {
      return parentDockPane.getRootDockPane();
    }
    return this;
  }

  public Node getContent() {
    return content;
  }

  public ParentDockPane getParentDockPane() {
    return parentDockPane;
  }

  public DockPaneArea getDockArea() {
    return dockPaneArea;
  }

  public Rectangle getOriginalStage() {
    return originalStage;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
    dockPaneTab.getLabel().setText(title);
  }

  public DockPane getGroupDockPane() {
    return groupDockPane;
  }

  public void setGroupDockPane(DockPane groupDockPane) {
    this.groupDockPane = groupDockPane;
  }

  public DockPaneTab getTab() {
    return dockPaneTab;
  }

  public void setStateProperties() {
    if (getParentDockPane() == null) {
      Stage stage = (Stage) getScene().getWindow();
      originalStage.setX(stage.getX());
      originalStage.setY(stage.getY());
      originalStage.setWidth(stage.getWidth());
      originalStage.setHeight(stage.getHeight());
    }
  }

  public void toggleTabPaneHeaderVisibility() {
    TabPane tabPane = dockPaneTab.getTabPane();
    if (tabPane.getStyleClass().contains(DOCK_HIDDEN_HEADER_TABPANE_STYLE_CLASS)) {
      tabPane.getStyleClass().removeAll(DOCK_HIDDEN_HEADER_TABPANE_STYLE_CLASS);
    } else {
      tabPane.getStyleClass().add(DOCK_HIDDEN_HEADER_TABPANE_STYLE_CLASS);
    }
  }

  public void dock() {
    if (getScene() != null && getScene().getWindow() != null) {
      getScene().getWindow().hide();
    }
    DockPos targetDockPos = dockManager.getTargetDockPos();
    DockPane targetDockPane = dockManager.getTargetDockPane();
    if (targetDockPos != null) {
      ParentDockPane parentDockPane = (ParentDockPane) targetDockPane.getParentDockPane();
      if (DockManager.isSplitDockPos(targetDockPos)) {
        if (!(parentDockPane instanceof SplitPaneDockPane)) {
          parentDockPane = new SplitPaneDockPane(targetDockPane);
          dockManager.addDockPane(parentDockPane);
        }
      } else {
        if (!(parentDockPane instanceof TabPaneDockPane)) {
          parentDockPane = new TabPaneDockPane(targetDockPane);
          dockManager.addDockPane(parentDockPane);
        }
      }
      parentDockPane.addChildDockPane(this, targetDockPane, targetDockPos);
    }
  }

  public void show() {
    dockManager.showDockPane(this);
    for (DockPane dockPane : dockPanes) {
      dockPane.show();
    }
  }

  public String getDockId() {
    String dockId = "";
    if (groupDockPane != null) {
      dockId += groupDockPane.getDockId();
    }
    dockId += getId();
    return dockId;
  }

  public void undock() {
    originalStage.setWidth(getWidth());
    originalStage.setHeight(getHeight());

    parentDockPane.removeChildDockPane(this);
    dockManager.setTargetDockPane(null);
    dockManager.setSourceDockPane(null);
  }

  class DockPaneArea extends StackPane {
    private HBox horizontalArea = new HBox();
    private HBox leftArea = new HBox();
    private HBox rightArea = new HBox();
    private VBox verticalArea = new VBox();
    private VBox topArea = new VBox();
    private VBox bottomArea = new VBox();

    private static final String DOCK_AREA_STYLE_CLASS = "dock-area";

    public DockPaneArea() {
      leftArea.setVisible(false);
      rightArea.setVisible(false);
      topArea.setVisible(false);
      bottomArea.setVisible(false);

      topArea.getStyleClass().add(DOCK_AREA_STYLE_CLASS);
      bottomArea.getStyleClass().add(DOCK_AREA_STYLE_CLASS);
      leftArea.getStyleClass().add(DOCK_AREA_STYLE_CLASS);
      rightArea.getStyleClass().add(DOCK_AREA_STYLE_CLASS);

      horizontalArea.setVisible(false);
      horizontalArea.getChildren().add(leftArea);
      horizontalArea.getChildren().add(rightArea);
      HBox.setHgrow(leftArea, Priority.ALWAYS);
      HBox.setHgrow(rightArea, Priority.ALWAYS);
      getChildren().add(horizontalArea);

      verticalArea.setVisible(false);
      verticalArea.getChildren().add(topArea);
      verticalArea.getChildren().add(bottomArea);
      VBox.setVgrow(topArea, Priority.ALWAYS);
      VBox.setVgrow(bottomArea, Priority.ALWAYS);
      getChildren().add(verticalArea);
    }

    private void showHorizontalArea(boolean show) {
      setVisible(show);
      horizontalArea.setVisible(show);
    }

    private void showVerticalArea(boolean show) {
      setVisible(show);
      verticalArea.setVisible(show);
    }

    public void showLeftArea(boolean show) {
      showHorizontalArea(show);
      leftArea.setVisible(show);
    }

    public void showRightArea(boolean show) {
      showHorizontalArea(show);
      rightArea.setVisible(show);
    }

    public void showTopArea(boolean show) {
      showVerticalArea(show);
      topArea.setVisible(show);
    }

    public void showBottomArea(boolean show) {
      showVerticalArea(show);
      bottomArea.setVisible(show);
    }

    public void hideAllAreas() {
      showLeftArea(false);
      showRightArea(false);
      showTopArea(false);
      showBottomArea(false);
    }
  }

  class DockPaneTab extends Tab {
    private StackPane stackPane = new StackPane();
    private Label label = new Label();
    private TextField textField = new TextField();

    public DockPaneTab() {
      textField.setVisible(false);
      textField.setManaged(false);
      stackPane.getChildren().add(label);
      stackPane.getChildren().add(textField);
      setGraphic(stackPane);

      label.textProperty().addListener(new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue,
            String newValue) {
          textField.setText(label.getText());
        }
      });

      textField.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          label.setText(textField.getText());
          hideTextField(true);
        }
      });

      // Adding 20 to Label width to account for TextField padding that causes trailing characters
      // to be clipped
      // The width addition should probably be based on style properties such as Insets rather than
      // being hardcoded to 20
      textField.prefWidthProperty().bind(label.widthProperty().add(20));

      label.setText("New Dock Pane");
      stackPane.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
        @Override
        public void handle(ContextMenuEvent event) {
          dockManager.setSourceDockPane(DockPane.this);
          DockPaneTabContextMenu contextMenu = dockManager.getDockPaneTabContextMenu();
          if (contextMenu.isShowing()) {
            contextMenu.hide();
          }

          contextMenu.setDockPane(DockPane.this);

          contextMenu.show(stackPane, event.getScreenX(), event.getScreenY());
          if (DockPane.this instanceof ParentDockPane) {
            contextMenu.hideRenameMenu(false);
          } else {
            contextMenu.hideRenameMenu(true);
          }
        }
      });

      stackPane.setOnDragDetected(new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
          Node node = (Node) event.getSource();

          Stage stage = (Stage) node.getScene().getWindow();
          stage.setMaximized(false);

          originalStage.setX(event.getX());
          originalStage.setY(event.getY());

          WritableImage image = new WritableImage((int) getWidth(), (int) getHeight());
          DockPane.this.snapshot(null, image);

          Dragboard db = node.startDragAndDrop(TransferMode.ANY);
          ClipboardContent content = new ClipboardContent();
          content.putString("");
          db.setContent(content);
          db.setDragView(image);

          db.setDragViewOffsetX(event.getX());
          db.setDragViewOffsetY(event.getY());
          dockManager.setSourceDockPane(DockPane.this);

          if (parentDockPane != null) {
            undock();
          } else {
            stackPane.getScene().getWindow().setY(5000);
          }

          event.consume();
        }
      });

      stackPane.setOnDragDone(new EventHandler<DragEvent>() {
        @Override
        public void handle(DragEvent event) {
          Point point = MouseInfo.getPointerInfo().getLocation();
          double xPos = point.getX() - originalStage.getX();
          double yPos = point.getY() - originalStage.getY();
          DockPane target = dockManager.getTargetDockPane();
          if (target == null) {
            Stage stage = null;
            if (stackPane.getScene() != null && stackPane.getScene().getWindow() != null) {
              stage = (Stage) stackPane.getScene().getWindow();
              stage.show();
            } else {
              stage = DockManager.newDockStage(DockPane.this);
            }
            stage.setX(xPos);
            stage.setY(yPos);
          } else {
            dock();
          }
          dockManager.setTargetDockPane(null);
          dockManager.setSourceDockPane(null);

          event.consume();
        }
      });

      stackPane.setOnDragEntered(new EventHandler<DragEvent>() {
        @Override
        public void handle(DragEvent event) {
          if (!DockPane.this.equals(dockManager.getSourceDockPane())) {
            dockManager.setTargetDockPane(DockPane.this);
            // Subtracting 20 from the screen x and y coordinates to force the menu drag over event
            // Maybe there is a better way of setting the offset rather than hardcoding to 20
            DockPaneTabDragOverContextMenu dockPaneTabDragOverContextMenu =
                dockManager.getDockPaneTabDragOverContextMenu();
            if (dockPaneTabDragOverContextMenu.isShowing()) {
              dockPaneTabDragOverContextMenu.hide();
            }
            dockPaneTabDragOverContextMenu.show(stackPane.getScene().getWindow(),
                event.getScreenX() - 20, event.getScreenY() - 20);
            event.consume();
          }
        }
      });
    }

    public void hideTextField(boolean hide) {
      textField.setVisible(!hide);
      textField.setManaged(!hide);
    }

    public Label getLabel() {
      return label;
    }

    public DockPane getDockPane() {
      return DockPane.this;
    }
  }
}
