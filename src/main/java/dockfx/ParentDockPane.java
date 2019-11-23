package dockfx;

import java.util.UUID;
import dockfx.DockFX.DockPos;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

abstract class ParentDockPane extends DockPane {
  private ObservableList<DockPane> childDockPanes = FXCollections.observableArrayList();

  public ParentDockPane(DockPane dockPane) {
    super();
    setId(UUID.randomUUID().toString());

    childDockPanes.addListener(new ListChangeListener<DockPane>() {
      @Override
      public void onChanged(Change<? extends DockPane> c) {
        while (c.next()) {
          for (DockPane dockPane : c.getAddedSubList()) {
            if (dockPane.parentDockPane != null) {
              dockPane.parentDockPane.getChildDockPanes().remove(dockPane);
            }
            dockPane.parentDockPane = ParentDockPane.this;
          }

          for (DockPane dockPane : c.getRemoved()) {
            dockPane.parentDockPane = null;
          }
        }
      }
    });

    if (dockPane.getParentDockPane() != null) {
      int index = dockPane.getParentDockPane().getChildDockPanes().indexOf(dockPane);
      dockPane.getParentDockPane().getChildDockPanes().set(index, this);
    }
    getChildDockPanes().add(dockPane);
  }

  public ObservableList<DockPane> getChildDockPanes() {
    return childDockPanes;
  }

  abstract public void addChildDockPane(DockPane childDockPane, DockPane targetDockPane,
      DockPos dockPos);

  abstract public void removeChildDockPane(DockPane childDockPane);

  abstract public DockLayout setDockLayout();
  
  abstract public void mergeIntoParent();
}
