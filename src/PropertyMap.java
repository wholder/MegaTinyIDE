import java.io.IOException;
import java.io.InputStream;
import java.util.*;

  /*
   * Implements a Map<String,Map<String,String>> structure created from a Properties file, such as:
   *    valset1: val1 = 1, val2 = 4, val3 = 5
   *    valset2: val1 = 1, val2 = 4, val3 = 1

   * In addition, the "parent" keyword can be used to import a shared set of values from the top level
   * Mapsuch as:
   *    baseVals: val1 = 1, val2 = 4
   *    valset1: parent = baseVals, val3 = 5
   *    valset2: parent = baseVals, val3 = 1
   *
   * Note: baseVals is removed from the top level Map, so the full enumeration of the ProperyMap created
   * will look like this:
   *    valset1: val1 = 1, val2 = 4, val3 = 5
   *    valset2: val1 = 5, val2 = 3, val3 = 1
   */
public class PropertyMap {
  private final Map<String,ParmSet> properties = new TreeMap<>();

  public static class ParmSet extends TreeMap<String,String> {
    ParmSet () {
      super();
    }

    ParmSet (ParmSet m) {
      super(m);
    }

    public int getInt (String key) {
      String val = get(key);
      if (val.toLowerCase().startsWith("0x")) {
        return Integer.parseInt(val.substring(2), 16);
      }
      return Integer.parseInt(val);
    }
  }

  public PropertyMap (String file) throws IOException {
    Properties props = new Properties();
    InputStream fis = PropertyMap.class.getClassLoader().getResourceAsStream(file);
    if (fis != null) {
      props.load(fis);
      fis.close();
    }
    Set<String> keys = props.stringPropertyNames();
    for (String key : keys) {
      String val = props.getProperty(key);
      ParmSet propMap = new ParmSet();
      properties.put(key, propMap);
      String[] parts = val.split(",");
      for (String part : parts) {
        String[] exp = part.trim().split("=");
        if (exp.length == 2) {
          propMap.put(exp[0].trim(), exp[1].trim());
        }
      }
    }
    Set<String> exList = new HashSet<>();
    for (String key : properties.keySet()) {
      Map<String,String> propMap = properties.get(key);
      for (String key2 : propMap.keySet()) {
        if ("parent".equals(key2)) {
          String val = propMap.get(key2);
          Map<String,String> exMap = properties.get(val);
          propMap.putAll(exMap);
          exList.add(val);
          propMap.remove("parent");
          break;
        }
      }
    }
    for (String key : exList) {
      properties.remove(key);
    }
  }

  public Set<String> keySet () {
    return properties.keySet();
  }

  public ParmSet get (String key) {
    return properties.get(key);
  }

  public Map<String,ParmSet> getReverseMap (String revKey) {
    Map<String,ParmSet> revMap = new TreeMap<>();
    for (String key : properties.keySet()) {
      ParmSet newMap = new ParmSet( properties.get(key));
      String revVal = newMap.get(revKey);
      newMap.remove(revKey);
      revMap.put(revVal, newMap);
      newMap.put("master", key);
    }
    return revMap;
  }

  public static void main (String[] args) throws IOException {
    PropertyMap pMap = new PropertyMap("attinys.props");
    if (false) {
      for (String key : pMap.properties.keySet()) {
        System.out.println(key);
        Map<String, String> propMap = pMap.get(key);
        for (String key2 : propMap.keySet()) {
          String val = propMap.get(key2);
          System.out.println("  " + key2 + "\t = " + val);
        }
      }
    } else {
      // Test getReverseMap()
      Map<String, ParmSet> revMap = pMap.getReverseMap("sig");
      for (String key : revMap.keySet()) {
        System.out.println(key);
        Map<String, String> propMap = revMap.get(key);
        for (String key2 : propMap.keySet()) {
          String val = propMap.get(key2);
          System.out.println("  " + key2 + "\t = " + val);
        }
      }
    }
  }
}
