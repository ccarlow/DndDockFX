package dnddockfx;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Orientation;

public class DockLayout {

  private String id;
  private String title;
  private String type;
  private boolean isClosed;
  private double x;
  private double y;
  private double width;
  private double height;
  private Orientation orientation;
  private double[] dividerPositions;
  private List<DockLayout> children = new ArrayList<DockLayout>();

  public DockLayout() {

  }

  public List<DockLayout> getChildren() {
    return children;
  }

  public void setChildren(List<DockLayout> children) {
    this.children = children;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setIsClosed(boolean isClosed) {
    this.isClosed = isClosed;
  }

  public boolean getIsClosed() {
    return isClosed;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getX() {
    return x;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getY() {
    return y;
  }

  public void setWidth(double width) {
    this.width = width;
  }

  public double getWidth() {
    return width;
  }

  public void setHeight(double height) {
    this.height = height;
  }

  public double getHeight() {
    return height;
  }

  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
  }

  public Orientation getOrientation() {
    return orientation;
  }

  public void setDividerPositions(double[] dividerPositions) {
    this.dividerPositions = dividerPositions;
  }

  public double[] getDividerPositions() {
    return dividerPositions;
  }
}
