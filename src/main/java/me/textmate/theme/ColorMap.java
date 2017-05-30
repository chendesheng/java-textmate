package me.textmate.theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorMap {
  private List<String> _id2color;
  private Map<String, Integer> _color2id;

  public ColorMap() {
    this._id2color = new ArrayList<String>();
    this._color2id = new HashMap<String, Integer>();
  }

  public int getId(String color) {
    if (color == null) {
      return 0;
    }
    color = color.toUpperCase();
    Integer value = this._color2id.get(color);
    if (value != null) {
      return value;
    }
    value = _id2color.size();
    this._color2id.put(color, value);
    this._id2color.add(color);
    return value;
  }

  public String[] getColorMap() {
    return this._id2color.toArray(new String[0]);
  }

}