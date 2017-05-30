package me.textmate.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.textmate.types.ILocation;

public class JSONObject {
  public ILocation vscodeTextmateLocation;

  List<Object> _array;
  Map<String, Object> _map;

  public JSONObject(boolean isArray) {
    if (isArray) {
      _array = new ArrayList<Object>();
    } else {
      _map = new HashMap<String, Object>();
    }
  }

  public void push(Object obj) {
    _array.add(obj);
  }

  public Object unshift() {
    return _array.remove(0);
  }

  public Object get(String key) {
    return _map.get(key);
  }

  public void put(String key, Object val) {
    _map.put(key, val);
  }

  public Map<String, Object> map() {
    return _map;
  }

  public List<Object> array() {
    return _array;
  }

  public boolean isArray() {
    return _array != null;
  }

  public static JSONObject newMap() {
    return new JSONObject(false);
  }

  public static JSONObject newArray() {
    return new JSONObject(true);
  }
}