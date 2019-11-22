package dockfx;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class DockFX {
  private static DockFX instance;
  private static DockPane sourceDockPane;
  private static DockPane targetDockPane;
  private static DockPos targetDockPos;
  private static List<DockPane> dockPanes;
  private String layoutConfigFile;

  private static DockPaneTabContextMenu dockPaneTabContextMenu;
  private static DockPaneTabDragOverContextMenu dockPaneTabDragOverContextMenu;

  public static enum DockPos {
    SPLIT_TOP, SPLIT_BOTTOM, SPLIT_LEFT, SPLIT_RIGHT, TAB_BEFORE, TAB_AFTER;
  }

  public DockFX() {}

  public void addDockPane(DockPane dockPane) {
    dockPanes.add(dockPane);
  }

  public void setLayoutConfigFile(String layoutConfigFile) {
    this.layoutConfigFile = layoutConfigFile;
  }

  public String getLayoutConfigFile() {
    return layoutConfigFile;
  }

  public static DockFX getInstance() {
    if (instance == null) {
      instance = new DockFX();
      dockPanes = new ArrayList<DockPane>();
      dockPaneTabContextMenu = instance.new DockPaneTabContextMenu();
      dockPaneTabDragOverContextMenu = instance.new DockPaneTabDragOverContextMenu();
    }
    return instance;
  }

  public void setSourceDockPane(DockPane dockPane) {
    sourceDockPane = dockPane;
  }

  public DockPane getSourceDockPane() {
    return sourceDockPane;
  }

  public void setTargetDockPane(DockPane dockPane) {
    targetDockPane = dockPane;
  }

  public DockPane getTargetDockPane() {
    return targetDockPane;
  }

  public void setTargetDockPos(DockPos dockPos) {
    targetDockPos = dockPos;
  }

  public DockPos getTargetDockPos() {
    return targetDockPos;
  }

  public void removeDockPaneById(String id) {
    DockPane dockPane = getDockPaneById(id);
    dockPanes.remove(dockPane);
  }

  List<DockLayout> dockLayoutList = new ArrayList<DockLayout>();

  public void saveDockLayout() {
    dockLayoutList.clear();
    for (DockPane dockPane : dockPanes) {
      if (dockPane.getParentDockPane() == null) {
        setDockLayout(dockPane);
        dockLayoutList.add(dockPane.getDockLayout());

        Scene scene = dockPane.getScene();
        if (scene == null || scene.getWindow() == null || !scene.getWindow().isShowing()) {
          dockPane.getDockLayout().setIsClosed(true);
        }
      }
    }

    try (XMLEncoder encoder =
        new XMLEncoder(new BufferedOutputStream(new FileOutputStream(layoutConfigFile)))) {
      encoder.writeObject(dockLayoutList);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public void setDockLayout(DockPane dockPane) {
    DockLayout dockLayout = new DockLayout();
    dockLayout.setId(dockPane.getId());
    dockPane.setDockLayout(dockLayout);
    if (dockPane.getParentDockPane() == null) {
      Scene scene = dockPane.getScene();
      if (scene != null && scene.getWindow() != null) {
        dockLayout.addProperty(DockLayout.X_PROP_NAME, scene.getWindow().getX());
        dockLayout.addProperty(DockLayout.Y_PROP_NAME, scene.getWindow().getY());
        dockLayout.addProperty(DockLayout.WIDTH_PROP_NAME, scene.getWindow().getWidth());
        dockLayout.addProperty(DockLayout.HEIGHT_PROP_NAME, scene.getWindow().getHeight());
      } else {
        dockLayout.addProperty(DockLayout.X_PROP_NAME, dockPane.getOriginalStage().getX());
        dockLayout.addProperty(DockLayout.Y_PROP_NAME, dockPane.getOriginalStage().getY());
        dockLayout.addProperty(DockLayout.WIDTH_PROP_NAME, dockPane.getOriginalStage().getWidth());
        dockLayout.addProperty(DockLayout.HEIGHT_PROP_NAME,
            dockPane.getOriginalStage().getHeight());
      }
    }

    if (dockPane instanceof ParentDockPane) {
      ParentDockPane parentDockPane = (ParentDockPane) dockPane;
      if (!parentDockPane.getChildDockPanes().isEmpty()) {
        for (DockPane child : parentDockPane.getChildDockPanes()) {
          setDockLayout(child);
          dockLayout.getChildren().add(child.getDockLayout());
        }
        dockLayout.setType(parentDockPane.getClass().getSimpleName());
        if (dockPane.getContent() instanceof SplitPane) {
          SplitPane splitPane = (SplitPane) dockPane.getContent();
          dockLayout.getProperties().put("orientation", splitPane.getOrientation());
          dockLayout.addProperty("dividerPositions", splitPane.getDividerPositions());
        }
      }
    }
  }

  public void loadDockLayout() {
    dockPaneTabContextMenu.setMenus();
    try (XMLDecoder decoder =
        new XMLDecoder(new BufferedInputStream(new FileInputStream(layoutConfigFile)),
            Thread.currentThread().getContextClassLoader())) {
      Map<String, Map<Integer, String>> parentMap = new HashMap<String, Map<Integer, String>>();
      dockLayoutList = (ArrayList<DockLayout>) decoder.readObject();
    } catch (FileNotFoundException e) {
      for (DockPane dockPane : dockPanes) {
        DockLayout dockLayout = new DockLayout();
        dockLayout.setId(dockPane.getId());
        dockLayout.getProperties().put(DockLayout.X_PROP_NAME, 0.0);
        dockLayout.getProperties().put(DockLayout.Y_PROP_NAME, 0.0);
        dockLayout.getProperties().put(DockLayout.WIDTH_PROP_NAME, 500.0);
        dockLayout.getProperties().put(DockLayout.HEIGHT_PROP_NAME, 500.0);
        dockLayoutList.add(dockLayout);
      }
    }

    List<DockPane> undockList = new ArrayList<DockPane>();
    for (DockPane dockPane : dockPanes) {
      if (dockPane.getChildren().isEmpty()) {
        undockList.add(dockPane);
      }
    }

    for (DockPane dockPane : undockList) {
      // dockPane.undock();
    }
    for (DockLayout dockLayout : dockLayoutList) {
      loadDockLayoutChildren(dockLayout);

      DockPane dockPane = getDockPaneById(dockLayout.getId());
      // dockController.hideTitleBar(false);
      Properties properties = dockLayout.getProperties();
      double xPos = (double) properties.get(DockLayout.X_PROP_NAME);
      double yPos = (double) properties.get(DockLayout.Y_PROP_NAME);
      double width = (double) properties.get(DockLayout.WIDTH_PROP_NAME);
      double height = (double) properties.get(DockLayout.HEIGHT_PROP_NAME);
      dockPane.getOriginalStage().setX(xPos);
      dockPane.getOriginalStage().setY(yPos);
      dockPane.getOriginalStage().setWidth(width);
      dockPane.getOriginalStage().setHeight(height);
      if (!dockLayout.getIsClosed()) {
        newDockStage(dockPane);
      }
    }
    setTargetDockPane(null);

    for (DockPane dockPane : dockPanes) {
      if (!dockPane.getChildren().isEmpty()) {
        if (dockPane.getContent() instanceof SplitPane) {
          SplitPane splitPane = (SplitPane) dockPane.getContent();
          // splitPane.setDividerPositions((double[])dockController.getDockLayout().getProperties().get("dividerPositions"));
        }
      }
    }
  }

  public void loadDockLayoutChildren(DockLayout dockLayout) {
    if (!dockLayout.getChildren().isEmpty()) {
      DockPane group = getDockPaneById(dockLayout.getId());
      DockPane previousChild = null;
      Orientation orientation = (Orientation) dockLayout.getProperties().get("orientation");
      for (DockLayout childLayout : dockLayout.getChildren()) {
        loadDockLayoutChildren(childLayout);
        DockPane child = getDockPaneById(childLayout.getId());
        if (previousChild != null) {
          targetDockPos =
              dockLayout.getType().equals("SplitPane")
                  ? orientation.equals(Orientation.HORIZONTAL) ? DockPos.SPLIT_RIGHT
                      : DockPos.SPLIT_BOTTOM
                  : DockPos.TAB_AFTER;
          setTargetDockPane(previousChild);
          child.dock();
          if (group == null) {
            group = child.getParentDockPane();
            group.setId(dockLayout.getId());
            group.setDockLayout(dockLayout);
            // group.hideTitleBar(true);
          }
        }

        previousChild = child;
      }
    }
  }

  public DockPane getDockPaneById(String id) {
    for (DockPane dockPane : dockPanes) {
      if (dockPane.getId() != null && dockPane.getId().equals(id)) {
        return dockPane;
      }
    }
    return null;
  }

  public DockPaneTabContextMenu getDockPaneTabContextMenu() {
    return dockPaneTabContextMenu;
  }

  public DockPaneTabDragOverContextMenu getDockPaneTabDragOverContextMenu() {
    return dockPaneTabDragOverContextMenu;
  }

  public static boolean isSplitDockPos(DockPos dockPos) {
    if (DockPos.TAB_BEFORE.equals(dockPos) || DockPos.TAB_AFTER.equals(dockPos)) {
      return false;
    }
    return true;
  }

  public static int getDockPosIndex(DockPos dockPos) {
    if (DockPos.SPLIT_RIGHT.equals(dockPos) || DockPos.SPLIT_BOTTOM.equals(dockPos)
        || DockPos.TAB_AFTER.equals(dockPos)) {
      return 1;
    }
    return 0;
  }

  public static Orientation getOrientation(DockPos dockPos) {
    return dockPos == DockPos.SPLIT_LEFT || dockPos == DockPos.SPLIT_RIGHT ? Orientation.HORIZONTAL
        : Orientation.VERTICAL;
  }

  public static Stage newDockStage(DockPane dockPane) {
    if (dockPane.getParentDockPane() != null) {
      dockPane.getParentDockPane().getChildren().remove(dockPane);
    }

    // DockPane parentDockPane = new DockPane();
    TabPane tabPane = new TabPane();
    // parentDockPane.setContent(tabPane);
    tabPane.getTabs().add(dockPane.getTab());

    Stage stage = new Stage();
    stage.setTitle(dockPane.getTitle());
    stage.setScene(new Scene(tabPane));
    stage.getScene().getStylesheets().add("/dockfx/default.css");

    stage.show();
    stage.setX(dockPane.getOriginalStage().getX());
    stage.setY(dockPane.getOriginalStage().getY());
    stage.setWidth(dockPane.getOriginalStage().getWidth());
    stage.setHeight(dockPane.getOriginalStage().getHeight());

    stage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST,
        new EventHandler<WindowEvent>() {
          @Override
          public void handle(WindowEvent event) {
            Stage stage = ((Stage) event.getSource());
            TabPane tabPane = (TabPane) stage.getScene().getRoot();
            // TabPane tabPane = (TabPane)dockPane.getContent();
            DockPane.DockPaneTab tab = ((DockPane.DockPaneTab) tabPane.getTabs().get(0));
            Rectangle originalStage = tab.getDockPane().getOriginalStage();
            originalStage.setX(stage.getX());
            originalStage.setY(stage.getY());
            originalStage.setWidth(stage.getWidth());
            originalStage.setHeight(stage.getHeight());
          }
        });
    return stage;
  }

  class DockPaneTabContextMenu extends ContextMenu {
    private MenuItem renameMenuItem = new MenuItem("Rename Container Window");
    private Menu dockPaneMenu = new Menu("Dock Panes");
    private Map<String, MenuItem> menuMap = new HashMap<String, MenuItem>();

    public DockPaneTabContextMenu() {
      renameMenuItem.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          sourceDockPane.getTab().hideTextField(false);
        }
      });
      renameMenuItem.setVisible(false);
      getItems().add(renameMenuItem);

      MenuItem menuItem = new MenuItem("Save Dock Layout");
      menuItem.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          DockFX.getInstance().saveDockLayout();
        }
      });
      getItems().add(menuItem);

      menuItem = new MenuItem("Load Dock Layout");
      menuItem.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          DockFX.getInstance().loadDockLayout();
        }
      });
      getItems().add(menuItem);

      menuItem = new MenuItem("Toggle Parent Visibility");
      menuItem.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          if (sourceDockPane.getParentDockPane() != null) {
            sourceDockPane.getParentDockPane().toggleTabPaneHeaderVisibility();
          }
        }
      });
      getItems().add(menuItem);
      getItems().add(dockPaneMenu);
    }

    public void hideRenameMenu(boolean hide) {
      renameMenuItem.setVisible(!hide);
    }

    public void setMenus() {
      if (!menuMap.isEmpty())
        return;
      for (DockPane dockPane : dockPanes) {

        if (dockPane.getParentDockPane() == null) {
          DockGroup dockGroup = dockPane.getDockGroup();
          setDockGroupMenu(dockGroup);

          String key = dockGroup.getGroupId() + dockPane.getId();
          Menu menuItem = (Menu) menuMap.get(key);
          if (menuItem == null) {
            menuItem = new Menu(dockPane.getTitle());
            menuItem.getStyleClass().remove("menu");
            menuItem.setOnAction(new EventHandler<ActionEvent>() {
              @Override
              public void handle(ActionEvent event) {
                // dockPane.getOrCreateStage().toFront();
              }
            });
            menuMap.put(key, menuItem);
          }
          ((Menu) menuMap.get(dockGroup.getGroupId())).getItems().add(menuItem);
        }
      }
    }

    private void setDockGroupMenu(DockGroup dockGroup) {
      Menu menuItem = (Menu) menuMap.get(dockGroup.getGroupId());
      if (menuItem == null) {
        menuItem = new Menu(dockGroup.getGroupName());
        menuItem.setOnAction(new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent event) {
            dockGroup.childrenToFront();
          }
        });
        menuMap.put(dockGroup.getGroupId(), menuItem);
      }

      if (dockGroup.getParent() != null && dockGroup.getParent() instanceof DockGroup) {
        DockGroup parent = (DockGroup) dockGroup.getParent();
        setDockGroupMenu(parent);
        Menu parentMenu = ((Menu) menuMap.get(parent.getGroupId()));
        if (!parentMenu.getItems().contains(menuItem)) {
          parentMenu.getItems().add(menuItem);
        }
      } else {
        Menu menu = (Menu) menuMap.get(dockGroup.getGroupId());
        if (!dockPaneMenu.getItems().contains(menu)) {
          dockPaneMenu.getItems().add(menu);
        }
      }
    }
  }

  // This ContextMenu exists in the DockFX singleton to guarantee only one ContextMenu is shown at
  // any time
  // Otherwise ContextMenu hide events are missed in some scenarios causing multiple DragOver
  // ContextMenus to be shown which degrades user experience
  class DockPaneTabDragOverContextMenu extends ContextMenu {
    private static final String SPLIT_LEFT_MENU_ITEM_STYLE_CLASS = "split-left-dock-menu-item";
    private static final String SPLIT_RIGHT_MENU_ITEM_STYLE_CLASS = "split-right-dock-menu-item";
    private static final String SPLIT_TOP_MENU_ITEM_STYLE_CLASS = "split-top-dock-menu-item";
    private static final String SPLIT_BOTTOM_MENU_ITEM_STYLE_CLASS = "split-bottom-dock-menu-item";
    private static final String TAB_BEFORE_MENU_ITEM_STYLE_CLASS = "tab-before-dock-menu-item";
    private static final String TAB_AFTER_MENU_ITEM_STYLE_CLASS = "tab-after-dock-menu-item";
    private boolean targetDragDropped = false;
    // Keep a copy of the targetDockPane since it may get set to null before all interested
    // EventHandlers are triggered
    private DockPane targetDockPaneCopy = null;

    public DockPaneTabDragOverContextMenu() {

      addEventHandler(DragEvent.DRAG_OVER, new EventHandler<DragEvent>() {
        @Override
        public void handle(DragEvent event) {
          if (event.getTarget() instanceof Node) {
            targetDragDropped = false;
            Node node = ((Node) event.getTarget());
            if (node.getStyleClass().contains("menu-item")) {
              targetDockPaneCopy = targetDockPane;
              handleMenuItemDragEvent(node, true);
              event.acceptTransferModes(TransferMode.ANY);
            }
          }
        }
      });

      addEventHandler(DragEvent.DRAG_DROPPED, new EventHandler<DragEvent>() {
        @Override
        public void handle(DragEvent event) {
          if (event.getTarget() instanceof Node) {
            Node node = ((Node) event.getTarget());
            if (node.getStyleClass().contains("menu-item")) {
              DockFX.getInstance().setTargetDockPos(getMenuItemDockPos(node));
              targetDragDropped = true;
            }
          }
        }
      });

      addEventHandler(DragEvent.DRAG_EXITED_TARGET, new EventHandler<DragEvent>() {
        @Override
        public void handle(DragEvent event) {
          if (event.getTarget() instanceof Node) {
            Node node = ((Node) event.getTarget());
            if (node.getStyleClass().contains("context-menu")) {
              if (!targetDragDropped) {
                targetDockPane = null;
              }
              hide();
            } else if (node.getStyleClass().contains("menu-item")) {
              handleMenuItemDragEvent(node, false);
            }
          }
        }
      });

      MenuItem menuItem = new MenuItem("Split Left");
      menuItem.getStyleClass().add(SPLIT_LEFT_MENU_ITEM_STYLE_CLASS);
      getItems().add(menuItem);

      menuItem = new MenuItem("Split Right");
      menuItem.getStyleClass().add(SPLIT_RIGHT_MENU_ITEM_STYLE_CLASS);
      getItems().add(menuItem);

      menuItem = new MenuItem("Split Top");
      menuItem.getStyleClass().add(SPLIT_TOP_MENU_ITEM_STYLE_CLASS);
      getItems().add(menuItem);

      menuItem = new MenuItem("Split Bottom");
      menuItem.getStyleClass().add(SPLIT_BOTTOM_MENU_ITEM_STYLE_CLASS);
      getItems().add(menuItem);

      menuItem = new MenuItem("Tab Before");
      menuItem.getStyleClass().add(TAB_BEFORE_MENU_ITEM_STYLE_CLASS);
      getItems().add(menuItem);

      menuItem = new MenuItem("Tab After");
      menuItem.getStyleClass().add(TAB_AFTER_MENU_ITEM_STYLE_CLASS);
      getItems().add(menuItem);
    }

    private void handleMenuItemDragEvent(Node node, boolean show) {
      // MenuItem PseudoClass is manually set because it is not automatically set by
      // Node.requestFocus if the node's Window is not focused
      // Window.requestFocus is not called because it brings the Window to the front which is not
      // the desired behavior
      node.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), show);

      if (node.getStyleClass().contains(SPLIT_LEFT_MENU_ITEM_STYLE_CLASS)) {
        targetDockPaneCopy.getDockArea().showLeftArea(show);
      } else if (node.getStyleClass().contains(SPLIT_RIGHT_MENU_ITEM_STYLE_CLASS)) {
        targetDockPaneCopy.getDockArea().showRightArea(show);
      } else if (node.getStyleClass().contains(SPLIT_TOP_MENU_ITEM_STYLE_CLASS)) {
        targetDockPaneCopy.getDockArea().showTopArea(show);
      } else if (node.getStyleClass().contains(SPLIT_BOTTOM_MENU_ITEM_STYLE_CLASS)) {
        targetDockPaneCopy.getDockArea().showBottomArea(show);
      }
    }

    private DockPos getMenuItemDockPos(Node node) {
      if (node.getStyleClass().contains(SPLIT_LEFT_MENU_ITEM_STYLE_CLASS)) {
        return DockPos.SPLIT_LEFT;
      } else if (node.getStyleClass().contains(SPLIT_RIGHT_MENU_ITEM_STYLE_CLASS)) {
        return DockPos.SPLIT_RIGHT;
      } else if (node.getStyleClass().contains(SPLIT_TOP_MENU_ITEM_STYLE_CLASS)) {
        return DockPos.SPLIT_TOP;
      } else if (node.getStyleClass().contains(SPLIT_BOTTOM_MENU_ITEM_STYLE_CLASS)) {
        return DockPos.SPLIT_BOTTOM;
      } else if (node.getStyleClass().contains(TAB_BEFORE_MENU_ITEM_STYLE_CLASS)) {
        return DockPos.TAB_BEFORE;
      } else if (node.getStyleClass().contains(TAB_AFTER_MENU_ITEM_STYLE_CLASS)) {
        return DockPos.TAB_AFTER;
      }
      return null;
    }
  }
}
