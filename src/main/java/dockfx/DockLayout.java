package dockfx;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DockLayout {
  public static final String X_PROP_NAME = "xPos";
  public static final String Y_PROP_NAME = "yPos";
  public static final String WIDTH_PROP_NAME = "width";
  public static final String HEIGHT_PROP_NAME = "height";

  private String id;
  private String title;
  private Properties properties;
  private String type;
  private boolean isClosed;
  private List<DockLayout> children = new ArrayList<DockLayout>();

  public DockLayout() {
    this.properties = new Properties();
  }

  public List<DockLayout> getChildren() {
    return children;
  }

  public void setChildren(List<DockLayout> children) {
    this.children = children;
  }

  public void addProperty(Object key, Object value) {
    properties.put(key, value);
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

  public Properties getProperties() {
    return properties;
  }

  public void setProperties(Properties properties) {
    this.properties = properties;
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
}
