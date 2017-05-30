package me.textmate.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import me.textmate.types.IRawRepository;

public class Utils {
  /**
   * Returns a copy of the object, or null if the object cannot
   * be serialized.
   */
  public static Object clone(Object orig) {
    Object obj = null;
    try {
      // Write the object out to a byte array
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(orig);
      out.flush();
      out.close();

      // Make an input stream from the byte array and read
      // a copy of the object back in.
      ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
      obj = in.readObject();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException cnfe) {
      cnfe.printStackTrace();
    }
    return obj;
  }

  public static IRawRepository mergeObjects(IRawRepository... sources) {
    IRawRepository target = new IRawRepository();
    for (IRawRepository source : sources) {
      target.putAll(source);
    }
    return target;
  }
}