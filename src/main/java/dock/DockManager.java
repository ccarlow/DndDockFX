package dock;

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
import java.util.Map.Entry;
import dock.DockPane.DockPaneTab;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class DockManager {
  private static DockManager instance;
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

  private DockManager() {}

  public void addDockPane(DockPane dockPane) {
    dockPanes.add(dockPane);
  }

  public void setLayoutConfigFile(String layoutConfigFile) {
    this.layoutConfigFile = layoutConfigFile;
  }

  public String getLayoutConfigFile() {
    return layoutConfigFile;
  }

  public static DockManager getInstance() {
    if (instance == null) {
      instance = new DockManager();
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
        DockLayout dockLayout = null;
        if (dockPane instanceof ParentDockPane) {
          ParentDockPane parentDockPane = (ParentDockPane) dockPane;
          dockLayout = parentDockPane.setDockLayout();
        } else {
          dockLayout = new DockLayout();
        }
        dockLayoutList.add(dockLayout);
        dockLayout.setId(dockPane.getId());
        
        if (dockPane.getParentDockPane() == null) {
          Scene scene = dockPane.getScene();
          if (scene != null && scene.getWindow() != null) {
            dockLayout.setX(scene.getWindow().getX());
            dockLayout.setY(scene.getWindow().getY());
            dockLayout.setWidth(scene.getWindow().getWidth());
            dockLayout.setHeight(scene.getWindow().getHeight());
          } else {
            dockLayout.setX(dockPane.getOriginalStage().getX());
            dockLayout.setY(dockPane.getOriginalStage().getY());
            dockLayout.setWidth(dockPane.getOriginalStage().getWidth());
            dockLayout.setHeight(dockPane.getOriginalStage().getHeight());
          }
          if (scene == null || scene.getWindow() == null || !scene.getWindow().isShowing()) {
            dockLayout.setIsClosed(true);
          }
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
        dockLayout.setX(0.0);
        dockLayout.setY(0.0);
        dockLayout.setWidth(500.0);
        dockLayout.setHeight(500.0);
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
      DockPane dockPane = null;
      if (!dockLayout.getChildren().isEmpty()) {
        loadDockLayoutChildren(dockLayout);
      }
      dockPane = getDockPaneById(dockLayout.getId());
      dockPane.getOriginalStage().setX(dockLayout.getX());
      dockPane.getOriginalStage().setY(dockLayout.getY());
      dockPane.getOriginalStage().setWidth(dockLayout.getWidth());
      dockPane.getOriginalStage().setHeight(dockLayout.getHeight());
      if (!dockLayout.getIsClosed()) {
        newDockStage(dockPane);
      }
    }
    setTargetDockPane(null);
  }

  public DockPane loadDockLayoutChildren(DockLayout dockLayout) {
    DockPane rootDockPane = null;
    if (!dockLayout.getChildren().isEmpty()) {
      DockPane previousChild = null;
      DockLayout previousChildLayout = null;
      for (DockLayout childLayout : dockLayout.getChildren()) {
        DockPane child = loadDockLayoutChildren(childLayout);
        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(child.getTab());
        
        if (previousChild != null) {
          targetDockPos = dockLayout.getOrientation() != null
              ? Orientation.HORIZONTAL.equals(dockLayout.getOrientation()) ? DockPos.SPLIT_RIGHT : DockPos.SPLIT_BOTTOM
                  : DockPos.TAB_AFTER;
          setTargetDockPane(previousChild);
          child.dock();
          
          if (dockLayout.getType() != null && !SplitPane.class.getSimpleName().equals(dockLayout.getType())) {
            child.getParentDockPane().setId(dockLayout.getId());
          }
          
          if (SplitPane.class.getSimpleName().equals(childLayout.getType()) && child instanceof SplitPaneDockPane && child.getParentDockPane() != null) {
            ((SplitPaneDockPane)child).mergeIntoParent();
          } 

          if (SplitPane.class.getSimpleName().equals(previousChildLayout.getType()) && previousChild instanceof SplitPaneDockPane && previousChild.getParentDockPane() != null) {
            ((SplitPaneDockPane)previousChild).mergeIntoParent();
          }
          
          rootDockPane = child.getParentDockPane() == null ? previousChild.getParentDockPane() : child.getParentDockPane();
        }

        previousChild = child;
        previousChildLayout = childLayout;
      }
      
      SplitPane splitPane = null;
      if (rootDockPane == null) {
        rootDockPane = getDockPaneById(dockLayout.getId());
        splitPane = (SplitPane)rootDockPane.getContent();
      } else {
        DockPane child = ((ParentDockPane)rootDockPane).getChildDockPanes().get(0);
        TabPane tabPane = child.getTab().getTabPane();
        splitPane = (SplitPane)tabPane.getProperties().get(SplitPaneDockPane.DOCK_PARENT_SPLITPANE_PROPERTY);   
      }
      if (dockLayout.getTitle() != null) {
        rootDockPane.getTab().getLabel().setText(dockLayout.getTitle());
      }
      if (dockLayout.getDividerPositions() != null) {
        SplitPaneDockPane.setSplitPaneNeedsLayoutPropertyListener(splitPane);
        splitPane.getProperties().put(SplitPaneDockPane.DOCK_SPLITPANE_DIVIDER_POSITIONS, dockLayout.getDividerPositions());
      }
    } else {
      rootDockPane = getDockPaneById(dockLayout.getId());
    }
    
    return rootDockPane;
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
      return newDockStage(dockPane.getParentDockPane());
    }

    TabPane tabPane = new TabPane();
    tabPane.getTabs().add(dockPane.getTab());

    Stage stage = new Stage();
    stage.setTitle(dockPane.getTitle());
    stage.setScene(new Scene(tabPane));
    stage.getScene().getStylesheets().add("/dock/default.css");

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
            DockPaneTab tab = (DockPaneTab) tabPane.getTabs().get(0);
            tab.getDockPane().setStateProperties();
          }
        });
    return stage;
  }

  class DockPaneTabContextMenu extends ContextMenu {
    private MenuItem renameMenuItem = new MenuItem("Rename Container Window");
    private Menu dockPaneMenu = new Menu("Dock Panes");
    private Map<String, MenuItem> menuMap = new HashMap<String, MenuItem>();
    private DockPane dockPane;
    public static final String DOCK_PANE_MENU_ITEM_STYLE_CLASS = "dock-pane-menu-item";

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
          DockManager.getInstance().saveDockLayout();
        }
      });
      getItems().add(menuItem);

      menuItem = new MenuItem("Load Dock Layout");
      menuItem.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          loadDockLayout();
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
      
      menuItem = new MenuItem("Merge into Parent Dock");
      menuItem.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          if (sourceDockPane.getParentDockPane() != null && sourceDockPane instanceof ParentDockPane) {
            ((ParentDockPane)sourceDockPane).mergeIntoParent();
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
      if (!menuMap.isEmpty()) {
        return; 
      }
      
      for (DockPane dockPane : dockPanes) {
        DockGroup dockGroup = dockPane.getDockGroup();System.out.println(dockPane.getId() + " group = " + dockGroup);
        setDockGroupMenu(dockGroup);

        String key = dockGroup.getGroupId() + dockPane.getId();
        Menu menuItem = (Menu) menuMap.get(key);
        if (menuItem == null) {
          menuItem = new Menu(dockPane.getTitle());
          dockPane.setMenuItem(menuItem);
          menuItem.getStyleClass().remove("menu");
          menuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
              Window window = null;
              if (dockPane.getScene() != null) {
                window = dockPane.getScene().getWindow();
              }
              if (window != null) {
                ((Stage)window).show();
              } else {
                newDockStage(dockPane);
              }
            }
          });
          menuMap.put(key, menuItem);
        }
        ((Menu) menuMap.get(dockGroup.getGroupId())).getItems().add(menuItem);
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
    
    public void setDockPane(DockPane dockPane) {
      if (dockPane != null && dockPane.getMenuItem() != null) {
        if (this.dockPane != null) {
          this.dockPane.getMenuItem().getStyleClass().removeAll(DOCK_PANE_MENU_ITEM_STYLE_CLASS);
        }
      }
      if (dockPane != null && dockPane.getMenuItem() != null) {
        dockPane.getMenuItem().getStyleClass().add(DOCK_PANE_MENU_ITEM_STYLE_CLASS); 
      }
      this.dockPane = dockPane;
    }
  }

  // This ContextMenu exists in the DockFX singleton to guarantee only one ContextMenu is shown at any time
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
              DockManager.getInstance().setTargetDockPos(getMenuItemDockPos(node));
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
