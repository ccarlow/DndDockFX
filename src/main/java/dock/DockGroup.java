package dock;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.stage.Stage;



public class DockGroup extends Group implements DockType {
  private String groupName;
  private String groupId = "";
  private List<DockType> groupMembers = new ArrayList<DockType>();

  public DockGroup() {
    getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
      @Override
      public void onChanged(Change<? extends Node> c) {
        if (c.next()) {
          for (Node node : c.getAddedSubList()) {
            if (node instanceof DockType) {
              groupMembers.add((DockType)node);
              if (node instanceof DockPane) {
                DockPane dockPane = (DockPane) node;
                dockPane.setDockGroup(DockGroup.this);
              }
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

  public void childrenToFront() {
    for (DockType child : groupMembers) {
      if (child instanceof DockGroup) {
        ((DockGroup) child).childrenToFront();
      } else if (child instanceof DockPane) {
        DockPane rootDockPane = ((DockPane)child).getRootDockPane();
        if (rootDockPane.getScene() != null && rootDockPane.getScene().getWindow() != null) {
          ((Stage)rootDockPane.getScene().getWindow()).show();
        } else {
          DockManager.getInstance().newDockStage(rootDockPane);
        }
      }
    }
  }
}
