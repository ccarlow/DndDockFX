package dock;

import dock.DockManager.DockPos;
import javafx.scene.control.TabPane;

public class TabPaneDockPane extends ParentDockPane {
  public TabPaneDockPane(DockPane dockPane) {
    super(dockPane);
    TabPane tabPane = dockPane.getTab().getTabPane();
    int tabIndex = tabPane.getTabs().indexOf(dockPane.getTab());
    tabPane.getTabs().set(tabIndex, getTab());
    tabPane = new TabPane();
    setContent(tabPane);
    tabPane.getTabs().add(dockPane.getTab());
  }

  @Override
  public void addChildDockPane(DockPane childDockPane, DockPane targetDockPane, DockPos dockPos) {
    int index = DockManager.getDockPosIndex(dockPos);
    TabPane tabPane = targetDockPane.getTab().getTabPane();
    index += tabPane.getTabs().indexOf(targetDockPane.getTab());
    tabPane.getTabs().add(index, childDockPane.getTab());
    getChildDockPanes().add(index, childDockPane);
    tabPane.getSelectionModel().select(index);
  }

  @Override
  public void removeChildDockPane(DockPane childDockPane) {
    TabPane tabPane = childDockPane.getTab().getTabPane();
    tabPane.getTabs().remove(childDockPane.getTab());
    getChildDockPanes().remove(childDockPane);
    if (tabPane.getTabs().size() == 1) {
      if (parentDockPane == null) {
        getChildDockPanes().remove(0);
        getTab().getTabPane().getTabs().set(0, tabPane.getTabs().get(0));
      } else {
        DockPane remainingDockPane = getChildDockPanes().get(0);
        tabPane = getTab().getTabPane();
        int index = tabPane.getTabs().indexOf(getTab());
        tabPane.getTabs().set(index, remainingDockPane.getTab());
        index = parentDockPane.getChildDockPanes().indexOf(this);
        parentDockPane.getChildDockPanes().set(index, remainingDockPane);
      }
    }
  }

  @Override
  public DockLayout setDockLayout() {
    DockLayout dockLayout = new DockLayout();
    dockLayout.setTitle(getTitle());
    dockLayout.setType(TabPaneDockPane.class.getSimpleName());
    for (DockPane dockPane : getChildDockPanes()) {
      DockLayout childDockLayout = null;
      if (dockPane instanceof ParentDockPane) {
        childDockLayout = ((ParentDockPane) dockPane).setDockLayout();
      } else {
        childDockLayout = new DockLayout();
      }
      childDockLayout.setId(dockPane.getId());
      dockLayout.getChildren().add(childDockLayout);
    }

    return dockLayout;
  }

  @Override
  public void mergeIntoParent() {}
}
