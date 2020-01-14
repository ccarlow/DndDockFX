package dock;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.stage.Stage;



public class DockGroup extends Group {
  private String groupName;
  private String groupId = "";
  private List<DockPane> dockPanes = new ArrayList<DockPane>();
  private List<DockGroup> dockGroups = new ArrayList<DockGroup>();

  public DockGroup() {
    getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
      @Override
      public void onChanged(Change<? extends Node> c) {
        if (c.next()) {
          for (Node node : c.getAddedSubList()) {
            if (node instanceof DockGroup) {
              dockGroups.add((DockGroup)node);
            } else if (node instanceof DockPane) {
              DockPane dockPane = (DockPane) node;
              dockPane.setDockGroup(DockGroup.this);
              dockPanes.add(dockPane);
            }
          }
        }
      }
    });

    idProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
          String newValue) {
        groupId = newValue;
      }
    });
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public String getGroupName() {
    return groupName;
  }

  public void showChildren() {
    for (DockPane child : dockPanes) {
      DockPane rootDockPane = ((DockPane)child).getRootDockPane();
      rootDockPane.show();
    }
    for (DockGroup child : dockGroups) {
      ((DockGroup) child).showChildren();
    }
  }
}
