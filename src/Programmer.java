import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

import java.io.IOException;
import java.util.*;

abstract public class Programmer {
  // System base addresses
  static final int SIGNATURES_BASE = 0x1100; // SIGROW
  static final int SERIAL_NUM_BASE = 0x1103;
  static final int EEPROM_BASE = 0x1400; // EEPROM
  static final int FUSES_BASE = 0x1280; // FUSES
  static final int LOCKBITS_BASE = 0x128A; // LOCKBITS
  static final int USERROW_BASE = 0x1300; // USERROW
  static final Map<String, ProgDevice> programmers = new TreeMap<>();

  public static class EDBGException extends IllegalStateException {
    EDBGException (String cause) {
      super(cause);
    }
  }

  static {
    try {
      PropertyMap progs = new PropertyMap("programmers.props");
      for (String key : progs.keySet()) {
        PropertyMap.ParmSet parmSet = progs.get(key);
        ProgDevice prog = new ProgDevice(parmSet, key);
        programmers.put(key, prog);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static class ProgDevice {
    public final String key;
    public final int pid;
    public final int vid;
    public final String name;
    public final String type;
    public final boolean hasVRef;
    public String serial;
    private String product;
    private int release;

    public ProgDevice (PropertyMap.ParmSet parmSet, String key) {
      this.key = key;
      String[] parts = key.split("-");
      if (parts.length == 2) {
        this.vid = Integer.parseInt(parts[0], 16);
        this.pid = Integer.parseInt(parts[1], 16);
        this.name = parmSet.get("name");
        this.type = parmSet.get("type");
        this.hasVRef = parmSet.getBoolean("vRef", false);
      } else {
        throw new IllegalArgumentException("Unable to parse key: " + key);
      }
    }

    public String getInfo () {
      return String.format("<html><b>Product</b>: %s <br><b>VID</b>: 0x%04X<br><b>PID</b>: " +
        "0x%02X<br><b>Seria</b>l: %s <br><b>Release:</b> %d </html>", product, vid, pid, serial, release);
    }
  }

  public static ProgDevice getProgrammer (String progVidPid) {
    return programmers.get(progVidPid);
  }

  public static List<ProgDevice> getProgrammers (boolean decodeUpdi) {
    List<ProgDevice> list = new ArrayList<>();
    HidServices hidServices = HidManager.getHidServices();
    for (String key : programmers.keySet()) {
      ProgDevice prog = programmers.get(key);
      HidDevice device = null;
      try {
        device = hidServices.getHidDevice(prog.vid, prog.pid, null);
        if (device != null) {
          prog.product = device.getProduct();
          prog.serial = device.getSerialNumber();
          prog.release = device.getReleaseNumber();
          list.add(prog);
        }
      } catch (Exception ex) {
        // ignore
      } finally {
        if (device != null) {
          device.close();
        }
      }
    }
    return list;
  }

  // Progress Bar methods

  public void setProgressMessage (String meg) {
    // Override, as needed
  }

  public void setProgressValue (int value) {
    // Override, as needed
  }

  public void closeProgressBar () {
    // Override, as needed
  }

  // Target Programming Methods
  abstract public byte[] getDeviceSignature () throws EDBGException;

  abstract public byte[] readFlash (int address, int len) throws EDBGException;

  abstract public void eraseTarget (int address, int mode) throws EDBGException;

  abstract public void writeFlash (int address, byte[] data) throws EDBGException;

  abstract public byte[] readFuses (int[] offsets) throws EDBGException;

  abstract  public void writeFuses (int[] offsets, byte[] fuses) throws EDBGException;

  abstract public byte[] readEeprom (int address, int len) throws EDBGException;

  abstract public void writeEeprom (int address, byte[] data) throws EDBGException;

  abstract public byte[] readUserRow (int address, int len) throws EDBGException;

  abstract public void writeUserRow (int address, byte[] data) throws EDBGException;

  abstract public byte[] getDeviceSerialNumber () throws EDBGException;

  abstract public void close ();

  // Target Debugging Methods (Note: currently these are only implmeneted in the EDBG class)
  public void resetTarget () throws EDBGException {
    throw new EDBGException("Programmer.resetTarget() not implemented");
  }

  public void runTarget () throws InterruptedException, EDBGException {
    throw new EDBGException("Programmer.runTarget() not implemented");
  }

  public void stopTarget () throws InterruptedException, EDBGException {
    throw new EDBGException("Programmer.stopTarget() not implemented");
  }

  public void runToAddress (int address) throws InterruptedException, EDBGException {
    throw new EDBGException("Programmer.runToAddress(int address) not implemented");
  }

  public void stepTarget () throws EDBGException {
    throw new EDBGException("Programmer.runToAddress(int address) not implemented");
  }

  public int getProgramCounter () throws EDBGException {
    throw new EDBGException("Programmer.getProgramCounter () not implemented");
  }

  public void setProgramCounter (int address) throws EDBGException {
    throw new EDBGException("Programmer.not implemented");
  }

  public byte[] readSRam (int address, int len) throws EDBGException {
    throw new EDBGException("Programmer.setProgramCounter(int address) not implemented");
  }

  public void writeSRam (int address, byte[] data) throws EDBGException {
    throw new EDBGException("Programmer.writeSRam(int address, byte[] data) not implemented");
  }

  public byte[] readRegisters (int address, int len) throws EDBGException {
    throw new EDBGException("Programmer.readRegisters(int address, int len) not implemented");
  }

  public void writeRegisters (int address, byte[] regs) throws EDBGException {
    throw new EDBGException("Programmer.writeRegisters(int address, byte[] regs) not implemented");
  }

  public int getStackPointer () throws EDBGException {
    throw new EDBGException("Programmer.getStackPointer() not implemented");
  }

  public void writeStackPointer (int sp) throws EDBGException {
    throw new EDBGException("Programmer.writeStackPointer(int sp) not implemented");
  }

  public byte getStatusRegister () throws EDBGException {
    throw new EDBGException("Programmer.getStatusRegister() not implemented");
  }

  public void writeStatusRegister (byte data) throws EDBGException {
    throw new EDBGException("Programmer.getStatusRegister() not implemented");
  }

  public double targetVoltage () {
    throw new EDBGException("Programmer.targetVoltage() not implemented");
  }

  interface OcdListener {
    void msgReceived (String text);
  }

  public void setOcdListener (OcdListener ocdListener) {
  }
}
