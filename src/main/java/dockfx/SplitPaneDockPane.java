package dockfx;

import dockfx.DockFX.DockPos;
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
    Orientation orientation = DockFX.getOrientation(dockPos);
    int index = DockFX.getDockPosIndex(dockPos);
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
  public double[] addDividerPositions(SplitPane splitPane, int tabPaneIndex) {
    double[] positions = splitPane.getDividerPositions();
    double[] newPositions = new double[positions.length + 1];
    if (positions.length > 0) {
      double newPosition;
      int index = tabPaneIndex;
      if (index <= 0) {
        newPosition = positions[0] / 2;
      } else if (index >= positions.length) {
        index = positions.length;
        double minPos = positions[index - 1];
        newPosition = minPos + ((1.0 - minPos) / 2);
      } else {
        double minPos = positions[index - 1];
        newPosition = minPos + ((positions[index] - minPos) / 2);
      }

      boolean indexFound = false;
      for (int i = 0; i < newPositions.length; i++) {
        if (i == index) {
          indexFound = true;
          newPositions[i] = newPosition;
        } else {
          newPositions[i] = positions[indexFound ? i - 1 : i];
        }
      }
    } else {
      newPositions[0] = 0.5;
    }
    return newPositions;
  }
  
  @Override
  public DockLayout setDockLayout() {
    DockLayout dockLayout = new DockLayout();
    dockLayout.setType("SplitPane");
    SplitPane splitPane = ((SplitPane)getTab().getContent());
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
        SplitPane splitPane = ((SplitPane)getTab().getContent());
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
      }
    }
  }
}
