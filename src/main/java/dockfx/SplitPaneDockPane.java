package dockfx;

import java.util.ArrayList;
import java.util.List;
import dockfx.DockManager.DockPos;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;

public class SplitPaneDockPane extends ParentDockPane {
  public static final String DOCK_PARENT_SPLITPANE_PROPERTY = "DockParentSplitPane";

  public SplitPaneDockPane(DockPane dockPane) {
    super(dockPane);

    TabPane tabPane = dockPane.getTab().getTabPane();
    // tabPane.getStyleClass().add(DOCK_HIDDEN_HEADER_TABPANE_STYLE_CLASS);
    int tabIndex = tabPane.getTabs().indexOf(dockPane.getTab());
    tabPane.getTabs().set(tabIndex, getTab());
    SplitPane splitPane = new SplitPane();
    setContent(splitPane);

    tabPane = new TabPane();
    tabPane.getProperties().put(DOCK_PARENT_SPLITPANE_PROPERTY, splitPane);
    tabPane.getTabs().add(dockPane.getTab());
    splitPane.getItems().add(tabPane);
  }

  @Override
  public void addChildDockPane(DockPane childDockPane, DockPane targetDockPane, DockPos dockPos) {
    Orientation orientation = DockManager.getOrientation(dockPos);
    int index = DockManager.getDockPosIndex(dockPos);
    TabPane tabPane = targetDockPane.getTab().getTabPane();
    SplitPane splitPane = (SplitPane) tabPane.getProperties().get(DOCK_PARENT_SPLITPANE_PROPERTY);
    int tabPaneIndex = splitPane.getItems().indexOf(tabPane);
    if (splitPane.getItems().size() == 1) {
      splitPane.setOrientation(orientation);
    } else {
      if (!splitPane.getOrientation().equals(orientation)) {
        SplitPane parentSplitPane = splitPane;
        splitPane = new SplitPane();
        splitPane.getProperties().put(DOCK_PARENT_SPLITPANE_PROPERTY, parentSplitPane);

        // Save the SplitPane divider positions because setting a new item triggers a new layout
        // which causes previous divider positions to be lost
        double[] dividerPositions = parentSplitPane.getDividerPositions();

        parentSplitPane.getItems().set(tabPaneIndex, splitPane);

        // Restore the original dividers positions after the SplitPane item has been set;
        // otherwise the difference between the old and new positions may be significant enough to
        // cause confusion
        parentSplitPane.setDividerPositions(dividerPositions);

        splitPane.setOrientation(orientation);
        tabPane.getProperties().put(DOCK_PARENT_SPLITPANE_PROPERTY, splitPane);
        splitPane.getItems().add(tabPane);
      } else {
        index += tabPaneIndex;
      }
    }

    double[] newDividerPositions = addDividerPositions(splitPane, tabPaneIndex);

    tabPane = new TabPane();
    tabPane.getProperties().put(DOCK_PARENT_SPLITPANE_PROPERTY, splitPane);
    tabPane.getTabs().add(childDockPane.getTab());
    splitPane.getItems().add(index, tabPane);
    splitPane.setDividerPositions(newDividerPositions);
    getChildDockPanes().add(childDockPane);
  }

  @Override
  public void removeChildDockPane(DockPane childDockPane) {
    TabPane tabPane = childDockPane.getTab().getTabPane();
    SplitPane splitPane =
        (SplitPane) tabPane.getProperties().remove(DOCK_PARENT_SPLITPANE_PROPERTY);
    double[] dividerPositions =
        removeDividerPosition(splitPane, splitPane.getItems().indexOf(tabPane));
    splitPane.getItems().remove(tabPane);
    if (dividerPositions != null) {
      splitPane.setDividerPositions(dividerPositions);
    }
    getChildDockPanes().remove(childDockPane);

    if (splitPane.getItems().size() == 1) {
      SplitPane parentSplitPane =
          (SplitPane) splitPane.getProperties().remove(DOCK_PARENT_SPLITPANE_PROPERTY);
      Node remainingItem = splitPane.getItems().remove(0);
      if (parentSplitPane != null) {
        int index = parentSplitPane.getItems().indexOf(splitPane);
        dividerPositions = parentSplitPane.getDividerPositions();
        parentSplitPane.getItems().set(index, remainingItem);
        parentSplitPane.setDividerPositions(dividerPositions);
        remainingItem.getProperties().put(DOCK_PARENT_SPLITPANE_PROPERTY, parentSplitPane);
      } else {
        if (remainingItem instanceof SplitPane) {
          getTab().setContent(remainingItem);
        } else {
          DockPane remainingDockPane = getChildDockPanes().remove(0);
          int index = getTab().getTabPane().getTabs().indexOf(getTab());
          getTab().getTabPane().getTabs().set(index, remainingDockPane.getTab());

          if (parentDockPane != null) {
            index = parentDockPane.getChildDockPanes().indexOf(this);
            parentDockPane.getChildDockPanes().set(index, remainingDockPane);
          }
        }
      }
    }
  }

  public double[] removeDividerPosition(SplitPane splitPane, int index) {
    double[] positions = splitPane.getDividerPositions();
    double[] newPositions = null;
    if (positions.length > 0) {
      newPositions = new double[positions.length - 1];
      int dividerIndex = 0;
      boolean indexFound = false;
      for (double dividerPosition : splitPane.getDividerPositions()) {
        if (!indexFound && dividerIndex == index) {
          indexFound = true;
          continue;
        }
        if (dividerIndex == newPositions.length) {
          break;
        }
        newPositions[dividerIndex++] = dividerPosition;
      }
    }
    return newPositions;
  }

  /**
   * Retains the original splitPane dividers and inserts a new divider that is positioned halfway
   * between the divider(s) wrapping the position of the new item to be added
   */
  public double[] addDividerPositions(SplitPane splitPane, int index) {
    double[] positions = splitPane.getDividerPositions();
      
    if (index > positions.length) {
      index = positions.length;
    }

    double min = 0;
    double max = 1;
    if (index - 1 >= 0) {
      min = positions[index - 1];
    }
    if (index < positions.length) {
      max = positions[index];
    }
    double newPosition = min + ((max - min) / 2);

    double[] newPositions = new double[positions.length + 1];
    int indexOffset = 0;
    for (int i = 0; i < newPositions.length; i++) {
      if (i == index) {
        indexOffset = -1;
        newPositions[i] = newPosition;
      } else {
        newPositions[i] = positions[i + indexOffset];
      }
    }
    return newPositions;
  }
  
  @Override
  public DockLayout setDockLayout() {
    DockLayout dockLayout = new DockLayout();
    dockLayout.setType("SplitPane");
    SplitPane splitPane = ((SplitPane)getContent());
    dockLayout.setOrientation(splitPane.getOrientation());
    dockLayout.setDividerPositions(splitPane.getDividerPositions());
    setDockLayout(dockLayout, splitPane);
    return dockLayout;
  }
  
  private void setDockLayout(DockLayout parentDockLayout, SplitPane parentSplitPane) {
    for (Node item : parentSplitPane.getItems()) {
      if (item instanceof SplitPane) {
        DockLayout dockLayout = new DockLayout();
        parentDockLayout.getChildren().add(dockLayout);
        dockLayout.setType("SplitPane");
        SplitPane splitPane = ((SplitPane)item);
        dockLayout.setOrientation(splitPane.getOrientation());
        dockLayout.setDividerPositions(splitPane.getDividerPositions());
        setDockLayout(dockLayout, splitPane);
      } else if (item instanceof TabPane) {
        DockPane dockPane = ((DockPaneTab)((TabPane)item).getTabs().get(0)).getDockPane();
        DockLayout dockLayout = null;
        if (dockPane instanceof ParentDockPane) {
          dockLayout = ((ParentDockPane)dockPane).setDockLayout();
        } else {
          dockLayout = new DockLayout();
        }
        parentDockLayout.getChildren().add(dockLayout);
        dockLayout.setId(dockPane.getId());
        if (dockPane instanceof ParentDockPane) {
          dockLayout.setType(dockPane.getClass().getSimpleName());
        }
      }
    }
  }

  @Override
  public void mergeIntoParent() {
    if (getParentDockPane() != null) {
      TabPane tabPane = getTab().getTabPane();
      SplitPane splitPane = (SplitPane) tabPane.getProperties().remove(DOCK_PARENT_SPLITPANE_PROPERTY);
      if (splitPane != null) {
        int index = splitPane.getItems().indexOf(tabPane);
        
        SplitPane childSplitPane = (SplitPane)getContent();
        if (childSplitPane.getOrientation().equals(splitPane.getOrientation())) {
          
          double[] dividerPositions = mergeDividerPositions(splitPane, childSplitPane, index);
          
          List<Node> items = new ArrayList<Node>(childSplitPane.getItems());
          for (int i = items.size() - 1; i >= 0; i--) {
            items.get(i).getProperties().put(DOCK_PARENT_SPLITPANE_PROPERTY, splitPane);
            splitPane.getItems().add(index, items.get(i));
          }
          splitPane.getItems().remove(tabPane);
          
          splitPane.setDividerPositions(dividerPositions);
        } else {
          double[] dividerPositions = splitPane.getDividerPositions();
          childSplitPane.getProperties().put(DOCK_PARENT_SPLITPANE_PROPERTY, splitPane);
          splitPane.getItems().set(index, childSplitPane);
          splitPane.setDividerPositions(dividerPositions);
        }
        
        List<DockPane> childDockPanes = new ArrayList<DockPane>(getChildDockPanes());
        for (DockPane dockPane : childDockPanes) {
          getParentDockPane().getChildDockPanes().add(dockPane);
        }
        DockManager.getInstance().removeDockPaneById(getId());
        getParentDockPane().getChildDockPanes().remove(this);
      } 
    }
  }
  
  public double[] mergeDividerPositions(SplitPane splitPane, SplitPane childSplitPane, int index) {
    double[] positions = splitPane.getDividerPositions();
    double[] childPositions = childSplitPane.getDividerPositions();
    double[] newPositions = new double[positions.length + childPositions.length];
    
    double min = 0;
    double max = 1;
    if (index - 1 >= 0) {
      min = positions[index - 1]; 
    } 
    if (index < positions.length) {
      max = positions[index];
    }
    
    double width = max - min;
    for (int i = 0; i < childPositions.length; i++) {
      childPositions[i] = min + (childPositions[i] * width);
    }
    
    int childIndex = -1;
    for (int i = 0; i < newPositions.length; i++) {
      if (i > index - 1) {
        if (childIndex < childPositions.length - 1) {
          newPositions[i] = childPositions[++childIndex];
        } else {
          newPositions[i] = positions[i - childPositions.length];
        }
      } else {
        newPositions[i] = positions[i];
      }
    }
    return newPositions;
  }
}
