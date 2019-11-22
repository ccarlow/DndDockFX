package dockfx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;

public class DockGroup extends Group {
  private String groupName;
  private String groupId = "";

  public DockGroup() {
    getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
      @Override
      public void onChanged(Change<? extends Node> c) {
        if (c.next()) {
          for (Node node : c.getAddedSubList()) {
            if (node instanceof DockPane) {
              DockPane dockPane = (DockPane) node;
              dockPane.setDockGroup(DockGroup.this);
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
    for (Node child : getChildren()) {
      if (child instanceof DockGroup) {
        ((DockGroup) child).childrenToFront();
      } else if (child instanceof DockPane) {
        //TODO: child stage to front
      }
    }
  }
}
