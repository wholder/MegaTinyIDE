import java.util.*;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import jssc.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/*
 * Encapsulates JSSC functionality into an easy to use class
 * See: https://code.google.com/p/java-simple-serial-connector/
 * And: https://github.com/scream3r/java-simple-serial-connector/releases
 *
 *  Author: Wayne Holder, 2015-2020 (first version 10/30/2015)
 *
 *  Note: updated code to: 2.9.2, see: https://github.com/java-native/jssc/releases, requires slf4j-simple-1.7.9.jar
 *
 *  CH340E (~ indicates active Low, 3 mA source, 4 mA sink for all outputs)
 *
 *                  +--------+
 *     D+         1 |        | 10 V3 (0.1uF to Gnd for 5V Vcc)
 *     D-         2 |        | 9  RxD (IN)  ------------+
 *     Gnd        3 |        | 8  TxD (OUT) -----\/\/\--+---> UPDI
 *    ~RTS (OUT)) 4 |        | 7  Vcc             4.7K
 *    ~CTS (IN)   5 |        | 6  TNOW (OUT) Tx Activity (Active High)
 *                  +--------+
 */

public class JSSCPort implements SerialPortEventListener {
  private static final Map<String,Integer>  baudRates = new LinkedHashMap<>();
  private final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1000);
  private static Pattern                    macPat = Pattern.compile("cu.");
  private static final int                  flowCtrl = SerialPort.FLOWCONTROL_NONE;
  private static final int                  eventMasks = SerialPort.MASK_RXCHAR | SerialPort.MASK_BREAK;
  private final Preferences                 prefs;
  private String                            portName;
  private int                               baudRate, dataBits, stopBits, parity;
  private boolean                           setRTS = true;
  private boolean                           setDTR;
  private SerialPort                        serialPort;
  private final List<RXEvent>               rxHandlers = new ArrayList<>();
  private boolean                           hasRxHandler;

  interface RXEvent {
    void rxChar (byte cc);
    void breakEvent ();
  }

  static {
    baudRates.put("110",    SerialPort.BAUDRATE_110);
    baudRates.put("300",    SerialPort.BAUDRATE_300);
    baudRates.put("600",    SerialPort.BAUDRATE_600);
    baudRates.put("1200",   SerialPort.BAUDRATE_1200);
    //baudRates.put("2400",   SerialPort.BAUDRATE_2400);
    baudRates.put("4800",   SerialPort.BAUDRATE_4800);
    baudRates.put("9600",   SerialPort.BAUDRATE_9600);
    baudRates.put("14400",  SerialPort.BAUDRATE_14400);
    baudRates.put("19200",  SerialPort.BAUDRATE_19200);
    baudRates.put("38400",  SerialPort.BAUDRATE_38400);
    baudRates.put("57600",  SerialPort.BAUDRATE_57600);
    baudRates.put("115200", SerialPort.BAUDRATE_115200);
    baudRates.put("128000", SerialPort.BAUDRATE_128000);
    baudRates.put("256000", SerialPort.BAUDRATE_256000);
  }

  /**
   * Create JSSCPort and use prefs to select the port and baud rate (if previously set)
   * Note: does not open port.
   * @param prefs Preferences object
   */
  JSSCPort (Preferences prefs) {
    this.prefs = prefs;
    // Determine OS Type
    switch (SerialNativeInterface.getOsType()) {
    case SerialNativeInterface.OS_LINUX:
      macPat = Pattern.compile("(ttyS|ttyUSB|ttyACM|ttyAMA|rfcomm)[0-9]{1,3}");
      break;
    case SerialNativeInterface.OS_MAC_OS_X:
      break;
    case SerialNativeInterface.OS_WINDOWS:
      macPat = Pattern.compile("");
      break;
    default:
      macPat = Pattern.compile("tty.*");
      break;
    }
    portName = prefs.get("serial.port", null);
    baudRate = prefs.getInt("serial.baud", SerialPort.BAUDRATE_115200);
  }

  public byte[] readBytes (int size) throws SerialPortException {
    try {
      return serialPort.readBytes(size, 100);
    } catch (SerialPortTimeoutException ex) {
      throw new IllegalStateException("readBytes() Timeout");
    }
  }

  public void sendBreak () throws SerialPortException {
    serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
    serialPort.setParams(SerialPort.BAUDRATE_300, dataBits, stopBits, parity, setRTS, setDTR);
    serialPort.writeBytes(new byte[] {0});
    readBytes(1);
    try {
      Thread.sleep(5);
    } catch (InterruptedException ex) {}
    serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
    serialPort.setParams(baudRate, dataBits, stopBits, parity, setRTS, setDTR);
  }

  /**
   * Note: time is in milliseonds (1000 = 1 second)
   * @throws SerialPortException
   */
  public void sendDoubleBreak () throws SerialPortException {
    serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
    for (int ii = 0; ii < 2; ii++) {
      sendBreak();
      serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
    }
    serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
  }

  /**
   * Checks if user has selected a Serial Port
   * @return true if selected, else false
   */
  public boolean postSelected () {
    return portName != null;
  }

  /**
   * Determine if port is currently open
   * @return true if open, else false
   */
  public boolean isOpen () {
    if (serialPort != null) {
      return serialPort.isOpened();
    }
    return false;
  }

  /**
   * Select port that will be opened by call to open()
   * @param port port name (as returned by getPortNames())
   */
  public void setPort (String port) {
    portName = port;
  }

  public void setParameters (int baudRate, int dataBits, int stopBits, int parity) {
    this.baudRate = baudRate;
    this.dataBits = dataBits;
    this.stopBits = stopBits;
    this.parity = parity;
  }

  /**
   * Open serial port and assign RX handler
   * @param handler RX handler
   * @throws SerialPortException on error
   */
  void open (RXEvent handler) throws SerialPortException {
    if (serialPort != null) {
      if (serialPort.isOpened()) {
        close();
      }
    }
    if (portName != null) {
      synchronized (this) {
        if (handler != null) {
          hasRxHandler = true;
          rxHandlers.add(handler);
        }
      }
      serialPort = new SerialPort(portName);
      serialPort.openPort();
      serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
      serialPort.setParams(baudRate, dataBits, stopBits, parity, setRTS, setDTR);
      serialPort.setEventsMask(eventMasks);
      serialPort.setFlowControlMode(flowCtrl);
      purgePort(SerialPort.PURGE_RXCLEAR + SerialPort.PURGE_TXCLEAR);
      if (handler != null) {
        serialPort.addEventListener(this);
      }
    }
  }

  /**
   * Close serial port, open
   */
  public void close () throws SerialPortException {
    if (serialPort != null && serialPort.isOpened()) {
      synchronized (this) {
        rxHandlers.clear();
      }
      if (hasRxHandler) {
        serialPort.removeEventListener();
      }
      serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
      serialPort.closePort();
      serialPort = null;
    }
  }

  public void purgePort(int flags) throws SerialPortException {
    serialPort.purgePort(flags);
  }

  /**
   * Implements SerialPortEventListener for RXCHAR and BREAK
   * @param se serial event
   */
  public void serialEvent (SerialPortEvent se) {
    try {
      int type = se.getEventType();
      if (type == SerialPortEvent.RXCHAR) {
        int rxCount = se.getEventValue();
        byte[] inChars = serialPort.readBytes(rxCount);
        if (rxHandlers.size() > 0) {
          for (byte cc : inChars) {
            for (RXEvent handler : rxHandlers) {
              handler.rxChar(cc);
            }
          }
        } else {
          for (byte cc : inChars) {
            if (queue.remainingCapacity() > 0) {
              queue.add((int) cc);
            }
          }
        }
      } else if (type == SerialPortEvent.BREAK) {
        for (RXEvent handler : rxHandlers) {
          handler.breakEvent();
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Send bytes of data to TX
   * @param data bytes to send
   * @throws SerialPortException on error
   */
  void writeBytes (byte[] data) throws SerialPortException {
    try {
      serialPort.writeBytes(data);
    } catch (Exception ex) {
      int dum = 0;
    }
  }

  /**
   * Read bytes from the serial port
   * @param count number of bytes to read
   * @param timeout timeout vaue (in milliseconds)
   * @return data read
   * @throws SerialPortException
   */
  byte[] readBytes (int count, int timeout) throws SerialPortException, SerialPortTimeoutException {
    return serialPort.readBytes(count, timeout);
  }

  /**
   * Send string to TX
   * @param data string to send
   * @throws SerialPortException on error
   */
  void sendString (String data) throws SerialPortException {
    serialPort.writeString(data);
  }

  /**
   * Returns JMenu that can be used to select a serial port
   * @return Serial Port JMenu
   */
  JMenu getPortMenu (String menuName, ButtonGroup progGroup) {
    JMenu menu = new JMenu(menuName);
    menu.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected (MenuEvent e) {
        // Populate menu on demand
        menu.removeAll();
        ButtonGroup group = new ButtonGroup();
        for (String pName : SerialPortList.getPortNames(macPat)) {
          JRadioButtonMenuItem item = new JRadioButtonMenuItem(pName, pName.equals(portName));
          //menu.setVisible(true);
          menu.add(item);
          group.add(item);
          item.addActionListener((ev) -> {
            portName = ev.getActionCommand();
            prefs.put("serial.port", portName);
          });
        }
      }

      @Override
      public void menuDeselected (MenuEvent e) { }

      @Override
      public void menuCanceled (MenuEvent e) { }
    });
    return menu;
  }

  List<JRadioButtonMenuItem> getPortMenuItems () {
    List<JRadioButtonMenuItem> list = new ArrayList<>();
    for (String pName : SerialPortList.getPortNames(macPat)) {
      if (!pName.toLowerCase(Locale.ROOT).contains("bluetooth")) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(pName, pName.equals(portName));
        list.add(item);
        item.addActionListener((ev) -> {
          portName = ev.getActionCommand();
          prefs.put("serial.port", portName);
        });
      }
    }
    return list;
  }

  /**
   * Returns JMenu that can be used to select the serial port's baud rate
   * @return Baud rate JMenu
   */
  JMenu getBaudMenu () {
    JMenu menu = new JMenu("Baud Rate");
    ButtonGroup group = new ButtonGroup();
    for (String bRate : baudRates.keySet()) {
      int rate = baudRates.get(bRate);
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(bRate, baudRate == rate);
      menu.add(item);
      menu.setVisible(true);
      group.add(item);
      item.addActionListener((ev) -> {
        String cmd = ev.getActionCommand();
        prefs.putInt("serial.baud", baudRate = Integer.parseInt(cmd));
      });
    }
    return menu;
  }
}
