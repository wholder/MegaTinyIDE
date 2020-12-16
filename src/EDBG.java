import jssc.SerialPortException;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.jna.HidApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

  /*
    Peripherial Memory Map (Note: 32 General Purpose Working Registers in separate I/O space)
      0x0000  VPORTA      Virtual Port A Base
      0x0004  VPORTB      Virtual Port B Base
      0x0008  VPORTC      Virtual Port C Base
      0x001C  GPIOR0      General Purpose IO register 0
      0x001D  GPIOR1      General Purpose IO register 1
      0x001E  GPIOR2      General Purpose IO register 2
      0x001F  GPIOR3      General Purpose IO register 3
      0x0030  CPU         CPU Register Base (0x030 - 0x033 reserved)
      0x0034  CCP         Configuration Change Protection
      0x003D  SP          Stack Pointer LSB
      0x003E  SP          Stack Pointer MSB
      0x003F  SREG        Status Register
      0x0040  RSTCTRL     Reset Controller
      0x0050  SLPCTRL     Sleep Controller
      0x0060  CLKCTRL     Clock Controller
      0x0080  BOD         Brown-Out Detector
      0x00A0  VREF        Voltage Reference
      0x0100  WDT         Watchdog Timer
      0x0110  CPUINT      Interrupt Controller
      0x0120  CRCSCAN     Cyclic Redundancy Check Memory Scan
      0x0140  RTC         Real Time Counter
      0x0180  EVSYS       Event System
      0x01C0  CCL         Configurable Custom Logic
      0x0200  PORTMUX     Port Multiplexer
      0x0400  PORTA       Port A Base
      0x0420  PORTB       Port B Base
      0x0440  PORTC       Port C Base
      0x0600  ADC0        Analog to Digital Converter/Peripheral Touch Controller
      0x0670  AC0         Analog Comparator
      0x0680  DAC0        Digital to Analog Converter
      0x0800  USART0      Universal Synchronous Asynchronous Receiver Transmitter
      0x0810  TWI0        Two Wire Interface 0
      0x0820  SPI0        Serial Peripheral Interface 0
      0x0A00  TCA0        Timer/Counter Type A instance 0
      0x0A40  TCA1        Timer/Counter Type B instance 0
      0x0A80  TCA2        Timer/Counter Type D instance 0
      0x0F00  SYSCFG      System Configuration
      0x0F01  REVID       Device Revision ID Register ( 0x00 = A, 0x01 = B, etc.)
      0x1000  NVMCTRL     Non Volatile Memory Controller Base
      0x1100  SIGROW      Signature Row
      0x1280  FUSES       Device specific fuses
      0x1300  USERROW     User Row

    Reset Vectors
      0x0000  RESET
      0x0002  CRCSCAN_NMI Non-Maskable Interrupt from CRC
      0x0004  BOD_VLM     Voltage Level Monitor
      0x0006  PORTA_PORT  Port A
      0x0008  PORTB_PORT  Port B
      0x000A  PORTC_PORT  Port C
      0x000C  RTC_CNT     Real-Time Counter
      0x000E  RTC_PIT     Periodic Interrupt Timer (in RTC peripheral)
      0x0010  TCA0_xx     Timer Counter 0 Type A, LUNF/OVF
      0x0012  TCA0_HUNF
      0x0014  TCA0_xCMP0  TCA0, LCMP0/CMP0
      0x0016  TCA0_xCMP1  TCA0, LCMP1/CMP1
      0x0018  TCA0_xCMP2  TCA0, LCMP2/CMP2
      0x001A  TCB0_INT    Timer Counter 0 Type B
      0x001C  TCB1_INT    Timer Counter 1 Type B
      0x001E  TCD0_OVF    Timer Counter Type D, OVF
      0x0020  TCD0_TRIG   TRIG
      0x0022  AC0_AC      Analog Comparator 0
      0x0024  AC1_AC      Analog Comparator 1
      0x0026  AC2_AC      Analog Comparator 2
      0x0028  ADC0_RESRDY Analog-to-Digital Converter 0, RESRDY
      0x002A  ADC0_WCOMP  Analog-to-Digital Converter 0, WCOMP
      0x002C  ADC1_RESRDY Analog-to-Digital Converter 1, RESRDY
      0x002E  ADC1_WCOMP  Analog-to-Digital Converter 1, WCOMP
      0x0030  TWI0_TWIS   Two-Wire Interface 0 I2C, TWIS
      0x0032  TWI0_TWIM   Two-Wire Interface 0 I2C, TWIM
      0x0034  SPI0_INT    Serial Peripheral Interface
      0x0036  USART0_RXC  Universal Asynchronous Receiver-Transmitter 0, RXC
      0x0038  USART0_DRE  Universal Asynchronous Receiver-Transmitter 0, DRE
      0x003A  USART0_TXC  Universal Asynchronous Receiver-Transmitter 0, TXC
      0x003C  NVMCTRL_EE  Nonvolatile Memory
   */

public class EDBG /* implements JSSCPort.RXEvent */ {
  private static final int                    MaxPkt = 60;
  public  static final int                    UPDIClock = 500;      // UPDI CLock (in kHz)
  private static final boolean                DEBUG_PRINT = false;  // If true, show debug messages
  private static final boolean                DEBUG_DECODE = false; // If true, show decoded messages
  private static final boolean                DEBUG_IO = false;     // If true, show sendCmd() cmd and response data
  private static final Map<String,Programmer> programmers = new TreeMap<>();
  private static final Map<Integer,String>    memTypes = new HashMap<>();
  private final HidServices                   hidServices;
  public HidDevice                            device;
  private final boolean                       program;
  private final MegaTinyIDE.ChipInfo          chip;
  private double                              targetVcc;
  private int                                 sequence;
  private boolean                             sessionActive;
  private boolean                             physicalActive;
  private boolean                             debugActive;
  private boolean                             programActive;
  private OcdListener                         ocdListener;
  private final MegaTinyIDE                   ide;
  private JSSCPort                            jPort;
  private final ByteArrayOutputStream         rxOut = new ByteArrayOutputStream();

  //                                                                ( prog/debug)
  public static final int MEMTYPE_SRAM                   = 0x20;   // (--/RW) - Absolute SRAM address
  public static final int MEMTYPE_EEPROM                 = 0x22;   // (RW/RW) - Absolute EEPROM address
  public static final int MEMTYPE_APPL_FLASH             = 0xC0;   // (RW/RO) - Address from base 0x000000 if PROG_BASE set
  public static final int MEMTYPE_BOOT_FLASH             = 0xC1;   // (RW/RO) - Address from base 0x000000 if PROG_BASE set
  public static final int MEMTYPE_APPL_FLASH_ATOMIC      = 0xC2;   // (WR/--) - Address from base 0x000000 if PROG_BASE set
  public static final int MEMTYPE_BOOT_FLASH_ATOMIC      = 0xC3;   // (WR/--) - Address from base 0x000000 if PROG_BASE set
  public static final int MEMTYPE_EEPROM_ATOMIC          = 0xC4;   // (RW/RW) - Absolute EEPROM address
  public static final int MEMTYPE_USER_SIGNATURE         = 0xC5;   // (RW/RW) - Absolute user signature address
  public static final int MEMTYPE_CALIBRATION_SIGNATURE  = 0xC6;   // (RO/RO) - Absolute calibration signature address
  public static final int MEMTYPE_FLASH_PAGE             = 0xB0;   // (RW/RO) - Address from base 0x000000 if PROG_BASE set
  public static final int MEMTYPE_EEPROM_PAGE            = 0xB1;   // (RW/RW) - Absolute EEPROM address
  public static final int MEMTYPE_FUSES                  = 0xB2;   // (RW/--) - Absolute fuse address (1 byte at a time)
  public static final int MEMTYPE_LOCK_BITS              = 0xB3;   // (RW/RO) - Absolute lockbit address (1 byte at a time
  public static final int MEMTYPE_SIGNATURE              = 0xB4;   // (RO/RO) - Absolute signature address
  public static final int MEMTYPE_REGFILE                = 0xB8;   // (--/RW) - Address is from base 0x00
  // System base addresses
  public static final int SIGNATURES_BASE                = 0x1100; // SIGROW
  public static final int PROD_SIGNATURES_BASE           = 0x1103;
  public static final int EEPROM_BASE                    = 0x1400; // EEPROM
  public static final int FUSES_BASE                     = 0x1280; // FUSES
  public static final int LOCKBITS_BASE                  = 0x128A; // LOCKBITS
  public static final int USER_SIGNATURES_BASE           = 0x1300; // USERROW

  public static final int STACK_POINTER                  = 0x003D; // Stack Pointer offset
  public static final int STATUS_REGISTER                = 0x003F; // Status Register (flags) offset

  static {
    memTypes.put(0x20, "SRAM");
    memTypes.put(0x22, "EEPROM");
    memTypes.put(0xB0, "FLASH_PAGE");
    memTypes.put(0xB2, "FUSES");
    memTypes.put(0xB4, "SIGNATURE");
    memTypes.put(0xB8, "REGFILE");
  }

  private static String getTypeDesc (int memType) {
    if (memTypes.containsKey(memType)) {
      return memTypes.get(memType);
    }
    return String.format("0x%02X", (int) memType & 0xFF);
  }

  private void debugPrint(String msg) {
    if (DEBUG_PRINT) {
      System.out.println(msg);
    }
  }

  interface OcdListener {
    void msgReceived (String text);
  }

  public void setOcdListener (OcdListener ocdListener) {
    this.ocdListener = ocdListener;
  }

  static class Programmer {
    public final  String  key;
    public final  int     pid;
    public final  int     vid;
    public final  String  name;
    public        String  product, serial;
    public        int     release;
    public        boolean hasVRef;

    private Programmer (PropertyMap.ParmSet parmSet, String key) {
      this.key = key;
      String[] parts = key.split("-");
      if (parts.length == 2) {
        this.vid = Integer.parseInt(parts[0], 16);
        this.pid = Integer.parseInt(parts[1], 16);
        this.name = parmSet.get("name");
        this.hasVRef = parmSet.getBoolean("vRef", false);
      } else {
        throw new IllegalArgumentException("Unable to parse key: " + key);
      }
    }

    private Programmer (Programmer prog, String product, String serial, int release) {
      this.key = prog.key;
      this.vid = prog.vid;
      this.pid = prog.pid;
      this.name = prog.name;
      this.hasVRef = prog.hasVRef;
      this.product = product;
      this.serial = serial;
      this.release = release;
    }
  }

  static {
    try {
      PropertyMap progs = new PropertyMap("programmers.props");
      for (String key : progs.keySet()) {
        PropertyMap.ParmSet parmSet = progs.get(key);
        Programmer prog = new Programmer(parmSet, key);
        programmers.put(key, prog);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  static class EDBGException extends IllegalStateException {
    EDBGException(String cause) {
      super(cause);
    }
  }

  public void printUpdi (String type) {
    if (ide.decodeUpdi()) {
      ide.infoPrintln(type);
      // Allow time for final bytes to trickle into rxOut buffer
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        // do nothing
      }
      byte[] temp = rxOut.toByteArray();
      //Utility.printHex(temp);
      String updi = UPDIDecoder.decode(temp);
      ide.infoPrintln(updi);
      rxOut.reset();
    }
  }


  EDBG (MegaTinyIDE ide, boolean program) {
    this.ide = ide;
    if (ide.decodeUpdi()) {
      jPort = ide.getSerialPort();
      if (jPort != null) {
        try {
          jPort.open(new JSSCPort.RXEvent() {
            @Override
            public void rxChar (byte cc) {
              rxOut.write(cc);
            }
            @Override
            public void breakEvent () {
              ide.infoPrintln("BREAK");
            }
          });
        } catch (SerialPortException ex) {
          ex.printStackTrace();
        }
      }
    }
    Programmer prog = ide.getSelectedProgrammer();
    this.chip = MegaTinyIDE.chipTypes.get(ide.getAvrChip());
    hidServices = HidManager.getHidServices();
    this.program = program;
    device = hidServices.getHidDevice(prog.vid, prog.pid, prog.serial);
    if (device != null) {
      if (device.isOpen()) {
        device.close();
      }
      if (device.open() && device.isOpen()) {
        device.setNonBlocking(true);
      } else {
        throw new EDBGException("Unable to open programmer: " + prog.name);
      }
      // Verify target has voltage
      if ((targetVcc = getAnalogVoltageRef()) < 1.0) {
        throw new EDBGException(prog.name + " indicates Target Vcc < 1 volt");
      }
      // Connect to target
      startSession();
      // Configure programmer for UPDI in Debug Mode with 500 kHz clock
      setVariantUPDI();
      setPhysicalInterfaceUPDI();
      setClockUPDI(UPDIClock);
      setUPDIDeviceInfo(chip);
      activatePhysical(true);
      if (program) {
        setFunctionProgram();
        enterProgramMode();
      } else {
        setFunctionDebug();
        attachDebugger(true);
      }
    } else {
      throw new EDBGException("Unable to connect to programmer: " + prog.name);
    }
  }

  public double targetVoltage () {
    return targetVcc;
  }

  public static Programmer getProgrammer (String progVidPid) {
    return programmers.get(progVidPid);
  }

  public static List<Programmer> getProgrammers () {
    List<Programmer> list = new ArrayList<>();
    HidServices hidServices = HidManager.getHidServices();
    for (String key : programmers.keySet()) {
      Programmer prog = programmers.get(key);
      HidDevice device = null;
      try {
        device = hidServices.getHidDevice(prog.vid, prog.pid, null);
        if (device != null) {
          list.add(new Programmer(prog, device.getProduct(), device.getSerialNumber(), device.getReleaseNumber()));
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

  void close () {
    if (program) {
      exitProgramMode();
    } else {
      detachDebugger();
    }
    deactivatePhysical();
    endSession();
    device.close();
    hidServices.shutdown();
    HidApi.exit();
    if (jPort != null) {
      jPort.close();
    }
  }

  static class ProgrammerInfo {
    int     vendId, prodId, iFace, release;
    String  manf, product, serial;
    boolean isOpen;

    public ProgrammerInfo (HidDevice device) {
      vendId = device.getVendorId();
      prodId = device.getProductId();
      manf = device.getManufacturer();
      product = device.getProduct();
      serial = device.getSerialNumber();
      release = device.getReleaseNumber();
      isOpen = device.isOpen();
    }

    public String toString () {
      return "vendId:  " + String.format("0x%04X", vendId) + "\n" +
          "prodId:  " + String.format("0x%04X", prodId) + "\n" +
          "manf:    " + manf + "\n" +
          "product: " + product + "\n" +
          "serial:  " + serial + "\n" +
          "iFace:   " + iFace + "\n" +
          "isOpen:  " + isOpen + "\n";
    }
  }

  public ProgrammerInfo getProgrammerInfo () {
    return new ProgrammerInfo(device);
  }

  // Response frame format
  //  0  0x81  AVR_RSP
  //  1  0x11  packet n of n
  //  2  0x00  len MSB
  //  3  0x06  len LSB
  //  - - - - - - - - - - -
  //  4  0x0E  SOF
  //  5  0x00  sequence LSB
  //  6  0x00  sequence MSB
  //  7  0x00  Source sub-protocol handler ID

  byte[] sendAvrCmd (byte[] cmd) {
    try {
      if (DEBUG_DECODE) {
        debugPrint("\nsendAvrCmd(): " + AvrPacketDecoder.decode(cmd));
      }
      ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
      bout1.write(0x0E);                    // SOF
      bout1.write(0x00);                    // Protocol version (always 0x00)
      bout1.write(Utility.lsb(sequence));   // LSB of Sequence ID
      bout1.write(Utility.msb(sequence));   // MSB  of Sequence ID
      bout1.write(cmd, 0, cmd.length);      // Append CMD starting with Destination sub-protocol handler ID
      sequence++;
      byte[] cmd8 = bout1.toByteArray();
      int numPkts = (cmd8.length / MaxPkt) + (cmd8.length % MaxPkt > 0 ? 1 : 0);
      for (int ii = 0; ii < numPkts; ii++) {
        int index = ii * MaxPkt;
        int len = Math.min(MaxPkt, cmd8.length - index);
        ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
        bout2.write(0x80);                  // AVR_CMD
        byte tmp = (byte) ((ii + 1 << 4) + numPkts);
        bout2.write(tmp);                   // Packet n of m (starts at 1)
        bout2.write(Utility.msb(len));      // MSB of Number of bytes in the wrapped AVR packet
        bout2.write(Utility.lsb(len));      // LSB of Number of bytes in the wrapped AVR packet
        bout2.write(cmd8, index, len);
        sequence++;
        byte[] pkt = bout2.toByteArray();
        byte[] resp = sendCmd(pkt);
        if (resp.length > 0 && resp[0] == (byte) 0x80) {
          if (resp[1] == 0x01) {
            ByteArrayOutputStream bout3 = new ByteArrayOutputStream();
            int frameNum, frameCount;
            do {
              byte[] data = sendCmd(new byte[] {(byte) 0x81});
              frameNum = ((data[1] >> 4) & 0x0F);
              frameCount = (data[1] & 0x0F);
              if (data[0] == (byte) 0x81) {
                // Note: value is big endian ordered
                int clen = ((((int) data[2] & 0xFF) << 8) | ((int) data[3] & 0xFF));
                bout3.write(data, 4, clen);
              } else {
                throw new EDBGException("sendAvrCmd() Response not 0x81");
              }
            } while (frameNum < frameCount);
            byte[] temp = bout3.toByteArray();
            byte[] data;
            switch (temp[4]) {                  // Response ID
            case (byte) 0x80:                   // RSP_AVR8_OK, RSP_EDBG_OK
              debugPrint("OK");
              return new byte[0];
            case (byte) 0x81:                   // AVR8_RSP_LIST, RSP_EDBG_LIST
              data = new byte[temp.length - 6];
              System.arraycopy(temp, 6, data, 0, data.length);
              return data;
            case (byte) 0x82:                   // AVR_EVENT
              debugPrint("EVENT");
              // should never get here
              throw new EDBGException("sendAvrCmd() Error AVR_EVENT response");
            case (byte) 0x83:                   // RSP_AVR8_PC- 4 bytes
              debugPrint("PC");
              data = new byte[4];
              System.arraycopy(temp, 6, data, 0, data.length);
              return data;
            case (byte) 0x84:                   // RSP_AVR8_DATA, RSP_EDBG_DATA
              debugPrint("DATA");
              data = new byte[temp.length - 6 - 1];
              System.arraycopy(temp, 6, data, 0, data.length);
              return data;
            case (byte) 0xA0:                   // RSP_AVR8_FAILED, RSP_EDBG_FAILED, RSP_HOUSEKEEPING_FAILED, etc.
              debugPrint("FAILED");
              String failMsg = AvrPacketDecoder.getFailMessage(temp[3], temp[5]);
              throw new EDBGException("sendAvrCmd() RSP_AVR8_FAILED:" + failMsg);
            }
            throw new EDBGException("sendAvrCmd() Error parsing response");
          }
        } else {
          throw new EDBGException("sendAvrCmd() Response not 0x80");
        }
      }
      return null;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new EDBGException("sendAvrCmd() unexpected exception: " + ex.getMessage());
    }
  }

  byte[] sendCmd (byte[] cmd) {
    if (DEBUG_IO) {
      System.out.println("sendCmd()");
      Utility.printHex(cmd);
    }
    byte[] buf = new byte[64];
    System.arraycopy(cmd, 0, buf, 0, cmd.length);
    device.write(buf, buf.length, (byte) 0);
    Byte[] bytes = device.read();
    byte[] data = new byte[bytes.length];
    for (int ii = 0; ii < bytes.length; ii++) {
      data[ii] = bytes[ii];
    }
    if (DEBUG_IO) {
      System.out.println("response:");
      Utility.printHex(data);
    }
    return data;
  }

  private static int getUnsigned16 (byte[] data, int off) {
    return ((int) data[off] & 0xFF) + (((int) data[off + 1] & 0xFF) << 8);
  }

  /*
   * = = = = = = = = = = = = = =
   *    DISCOVERY Commands
   * = = = = = = = = = = = = = =
   */

  /**
   * Get Tool Name
   *
   * @return tool name String
   */
  public String getToolName () {
    byte[] rsp = sendAvrCmd(new byte[] {
        0x00,                 // DISCOVERY
        0x00,                 // Command ID (QUERY)Using
        0x00,                 // Command version (always (0x00)
        (byte) 0x80           // Query context (Tool Name)
    });
    printUpdi("getToolName()");
    return new String(rsp, StandardCharsets.UTF_8);
  }

  /**
   * Get Tool Serial Number
   *
   * @return tool serial number String
   */
  public String getToolSerial () {
    byte[] rsp = sendAvrCmd(new byte[] {
        0x00,                 // DISCOVERY
        0x00,                 // Command ID (QUERY)
        0x00,                 // Command version (always (0x00)
        (byte) 0x81           // Query context (Serial Number)
    });
    printUpdi("getToolSerial()");
    return new String(rsp, StandardCharsets.UTF_8);
  }

  /*
   * = = = = = = = = = = = = = =
   *    HOUSEKEEPING Commands
   * = = = = = = = = = = = = = =
   */

  /**
   * Starts a session with the tool. The tool may now publish events to the host on the EVENT channel.
   */
  public void startSession () {
    sendAvrCmd(new byte[] {
        0x01,                 // HOUSEKEEPING
        0x10,                 // Command ID (CMD_HOUSEKEEPING_START_SESSION)
    });
    printUpdi("startSession()");
    sessionActive = true;
  }

  /**
   * Ends a session with the tool. The tool will cease to publish events.
   */
  public void endSession () {
    sendAvrCmd(new byte[] {
        0x01,                 // HOUSEKEEPING
        0x11,                 // Command ID (CMD_HOUSEKEEPING_END_SESSION)
    });
    printUpdi("endSession()");
    sessionActive = false;
  }

  public static class ProgInfo {
    public int   hwRev, fwMajor, fwMinor, fwBuild;

    ProgInfo (byte[] data) {
      hwRev = Utility.lsb(data[0]);
      fwMajor = Utility.lsb(data[1]);
      fwMinor = Utility.lsb(data[2]);
      fwBuild = getUnsigned16(data, 3);
    }

    public String toString () {
      return String.format("Tool HW: %d, SW: %d.%d.%d\n", hwRev, fwMajor, fwMinor, fwBuild);
    }
  }

  /**
   * Get Programmer Info:
   *    HOUSEKEEPING_CONFIG_HWREV           = 0x00  HW version
   *    HOUSEKEEPING_CONFIG_FWREV_MAJ       = 0x01  FW version major
   *    HOUSEKEEPING_CONFIG_FWREV_MIN       = 0x02  FW version minor
   *    HOUSEKEEPING_CONFIG_BUILD           = 0x03  FW build
   *    HOUSEKEEPING_CONFIG_CHIP            = 0x05  Host info (chip)
   *    HOUSEKEEPING_CONFIG_BLDR_MAJ        = 0x06  Bootloader major
   *    HOUSEKEEPING_CONFIG_BLDR_MIN        = 0x07  Bootloader minor
   *    HOUSEKEEPING_CONFIG_DEBUG_BUILD     = 0x08  Debug build
   *    HOUSEKEEPING_CONFIG_FIRMWARE_IMAGE  = 0x09  May fail
   *
   * @return ProgInfo object
   */
  public ProgInfo getToolInfo () {
    byte[] rsp = sendAvrCmd(new byte[] {
        0x01,                 // HOUSEKEEPING
        0x02,                 // Command ID (GET)
        0x00,                 // Command version (always 0x00)
        0x00,                 // context = HK_CONTEXT_CONFIG
        0x00,                 // address = HOUSEKEEPING_CONFIG_HWREV (Hardware revision)
        0x05                  // read 5 bytes
    });
    printUpdi("getToolInfo()");
    return new ProgInfo(rsp);
  }

  /**
   * Get Target reference voltage
   *
   * @return voltage in Volts
   */
  public double getAnalogVoltageRef () {
    byte[] rsp = sendAvrCmd(new byte[] {
        0x01,                 // HOUSEKEEPING
        0x02,                 // Command ID (GET)
        0x00,                 // Command version (always (0x00)
        0x01,                 // context = HK_CONTEXT_ANALOG (Analog parameters)
        0x00,                 // address = HOUSEKEEPING_ANALOG_VTREF = (Target reference voltage)
        0x02                  // bytes to read
    });
    printUpdi("getAnalogVoltageRef()");
    return (double) getUnsigned16(rsp, 0) / 1000.0;
  }

  /**
   * Gets a list of the commands supported by the EDBG Control Protocol:
   * 0x00 = CMD_EDBG_QUERY
   * 0x01 = CMD_EDBG_SET
   * 0x02 = CMD_EDBG_GET
   *
   * @return byte[] array of supported commands
   */
  public byte[] queryEdbgCommands () {
    byte[] rsp = sendAvrCmd(new byte[] {
        0x20,                 // EDBG_CTRL
        0x00,                 // Command ID (CMD_EDBG_QUERY)
        0x00,                 // Command version (always 0x00)
        0x00,                 // Command context (EDBG_QUERY_COMMANDS)
    });
    printUpdi("queryEdbgCommands()");
    return rsp;
  }

  /*
   * = = = = = = = = = = = = = =
   *    AVR8GENERIC Commands
   * = = = = = = = = = = = = = =
   */

  /**
   * Setup Programmer context parameters
   *
   * @param pBase   Program Base (typically 0x8000)
   * @param fBytes  Page size of Flash, in bytes (typically 64)
   * @param eeBytes Page size of EEPROM, in bytes (typically 32)
   * @param nvmMod  Base address of NVMCTRL_MODULE (typically 0x1000 for ATTiny UPDI devices)
   * @param ocdMod  Base address of OCD_MODULE (typically 0x0F80)
   */
  public void setUPDIDeviceInfo (int pBase, int fBytes, int eeBytes, int nvmMod, int ocdMod) {
    if (sessionActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x01,                 // Command ID (CMD_AVR8_SET)
          0x00,                 // Command version (always (0x00)
          0x02,                 // Command context (AVR8_CTXT_DEVICE)
          0x00,                 // address (PROG_BASE)
          0x08,                 // write 8 bytes
          Utility.lsb(pBase),   // PROG_BASE (LSB)
          Utility.msb(pBase),   // PROG_BASE (MSB)
          Utility.lsb(fBytes),  // FLASH_PAGE_BYTES
          Utility.lsb(eeBytes), // EEPROM_PAGE_BYTES
          Utility.lsb(nvmMod),  // NVMCTRL_MODULE (LSB)
          Utility.msb(nvmMod),  // NVMCTRL_MODULE (MSB)
          Utility.lsb(ocdMod),  // OCD_MODULE (LSB)
          Utility.msb(ocdMod),  // OCD_MODULE (MSB)
      });
      printUpdi(String.format("setUPDIDeviceInfo(0x%04X, %d, %d, 0x%04X, 0x%04X)", pBase, fBytes, eeBytes, nvmMod, ocdMod));
    } else {
      throw new EDBGException("Call to setUPDIDeviceInfo() when session is not active");
    }
  }

  public void setUPDIDeviceInfo (MegaTinyIDE.ChipInfo chip) {
    int fBase = chip.getInt("fbase");
    int fPage = chip.getInt("fpage");
    int ePage = chip.getInt("epage");
    int nvmBase = chip.getInt("nvmBase");
    int ocdBase = chip.getInt("ocdBase");
    setUPDIDeviceInfo(fBase, fPage, ePage, nvmBase, ocdBase);
  }

  /**
   * Gets a list of the commands supported by the debugger:
   * 0x00 = CMD_AVR8_QUERY                 // Capability discovery
   * 0x01 = CMD_AVR8_SET                   // Set parameters
   * 0x02 = CMD_AVR8_GET                   // Get parameters
   * 0x10 = CMD_AVR8_ACTIVATE_PHYSICAL     // Connect physically
   * 0x11 = CMD_AVR8_DEACTIVATE_PHYSICAL   // Disconnect physically
   * 0x12 = CMD_AVR8_GET_ID                // Read the ID
   * 0x13 = CMD_AVR8_ATTACH                // Attach to OCD module
   * 0x14 = CMD_AVR8_DETACH                // Detach from OCD module
   * 0x15 = CMD_AVR8_PROG_MODE_ENTER       // Enter programming mode
   * 0x16 = CMD_AVR8_PROG_MODE_LEAVE       // Leave programming mode
   * 0x17 = CMD_AVR8_DISABLE_DEBUGWIRE     // Disable debugWIRE interface
   * 0x20 = CMD_AVR8_ERASE                 // Erase the chip
   * 0x21 = CMD_AVR8_MEMORY_READ           // Read memory
   * 0x22 = CMD_AVR8_MEMORY_READ_MASKED    // Read memory while via a mask
   * 0x23 = CMD_AVR8_MEMORY_WRITE          // Write memory
   * 0x24 = CMD_AVR8_CRC                   // Calculate CRC
   * 0x30 = CMD_AVR8_RESET                 // Reset the MCU
   * 0x31 = CMD_AVR8_STOP                  // Stop the MCU
   * 0x32 = CMD_AVR8_RUN                   // Resume execution
   * 0x33 = CMD_AVR8_RUN_TO_ADDRESS        // Resume with breakpoint
   * 0x34 = CMD_AVR8_STEP                  // Single step
   * 0x35 = CMD_AVR8_PC_READ               // Read Program Counter
   * 0x36 = CMD_AVR8_PC_WRITE              // Write Program Counter
   * 0x40 = CMD_AVR8_HW_BREAK_SET          // Set hardware breakpoint
   * 0x41 = CMD_AVR8_HW_BREAK_CLEAR        // Clear hardware breakpoint
   * 0x43 = CMD_AVR8_SW_BREAK_SET          // Set software breakpoints
   * 0x44 = CMD_AVR8_SW_BREAK_CLEAR        // Clear software breakpoints
   * 0x45 = CMD_AVR8_SW_BREAK_CLEAR_ALL    // Clear all software breakpoints
   * 0x50 = CMD_AVR8_PAGE_ERASE            // Erase page
   *
   * @return byte[] array of supported commands
   */
  public byte[] queryAvr8Commands () {
    if (physicalActive) {
      byte[] rsp = sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x00,                 // Command ID (QUERY)
          0x00,                 // Command version (always 0x00)
          0x00,                 // Command context (AVR8_QUERY_COMMANDS)
      });
      printUpdi("queryAvr8Commands()");
      return rsp;
    } else {
      throw new EDBGException("Call to queryAvr8Commands() when physicalActive is not active");
    }
  }

  /**
   * Activate the Physical Interface
   *
   * @param reset 0 = No Reset, 1 = apply external reset during activation
   */
  public void activatePhysical (boolean reset) {
    if (sessionActive) {
      byte res = (byte) (reset ? 1 : 0);
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x10,                 // Command ID (CMD_AVR8_ACTIVATE_PHYSICAL)
          0x00,                 // Command version (always (0x00)
          res                   // 0 = No Reset, 1 = apply external reset during activation
      });
      physicalActive = true;
      printUpdi(String.format("activatePhysical(%b)", reset));
    } else {
      throw new EDBGException("Call to activatePhysical() when session is not active");
    }
  }

  /**
   * Deactivate the Physical Interface
   */
  public void deactivatePhysical () {
    if (sessionActive && physicalActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x11,                 // Command ID (CMD_AVR8_DEACTIVATE_PHYSICAL)
          0x00                  // Command version (always 0x00)
      });
      physicalActive = false;
      printUpdi("deactivatePhysical()");
    } else {
      throw new EDBGException("Call to deactivatePhysical() when session of physical is not active");
    }
  }

  /**
   * Retrieves the ID of the target. The same ID is returned by Activate Physical command. Any previously read
   * value is not cached, but re-read from the target.
   * Note: can only call after activatePhysical()
   *
   * @return true
   */
  public byte[] getId () {
    if (physicalActive) {
      byte[] rsp = sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x12,                 // Command ID (CMD_AVR8_GET_ID)
          0x00                  // Command version (always (0x00)
      });
      printUpdi("getId()");
      return rsp;
    } else {
      throw new EDBGException("Call to getId() when physical interface is not active");
    }
  }

  /**
   * Enters debug mode on the target.
   *
   * @param stop 0 = Continue running, 1 = Break after attach
   */
  public void attachDebugger (boolean stop) {
    if (physicalActive) {
      byte brk = (byte) (stop ? 1 : 0);
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x13,                 // Command ID (CMD_AVR8_ATTACH)
          0x00,                 // Command version (always 0x00)
          brk,                  // 0x00 = Continue running, x01 = Break after attach
      });
      if (stop) {
        try {
          breakWait(true);
        } catch (InterruptedException ex) {
          ex.printStackTrace();
        }
      }
      debugActive = true;
      printUpdi(String.format("attachDebugger(%b)", stop));
    } else {
      throw new EDBGException("Call to attachDebugger() when physical interface is not active");
    }
  }

  /**
   * Terminates a debug session on the target.
   */
  public void detachDebugger () {
    if (physicalActive && debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x14,                 // Command ID (CMD_AVR8_DETACH)
          0x00                  // Command version (always 0x00)
      });
      debugActive = false;
      printUpdi("detachDebugger()");
    } else {
      throw new EDBGException("Call to detachDebugger() when physical interface is not active, or not attached");
    }
  }

  /**
   * Enters programming mode on the target.
   */
  public void enterProgramMode () {
    if (physicalActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x15,                 // Command ID (CMD_AVR8_PROG_MODE_ENTER)
          0x00,                 // Command version (always 0x00)
      });
      programActive = true;
      printUpdi("enterProgramMode()");
    } else {
      throw new EDBGException("Call to attachDebugger() when physical interface is not active");
    }
  }

  /**
   * Exits programming mode on the target.
   * If the ‘programming’ flag is set in the config context, the target will be released to run, and the debugger
   * deactivates the physical. This is the desired behavior for a pure programming session.
   * <p>
   * If the ‘debugging’ flag is set, the target will be stopped at the reset vector . A Break event will always be
   * sent when 'debugging' flag is set. If the 'debugging' flag is set and the Attach command has not yet been run
   * it will be run automatically during Prog Mode Leave.
   */
  public void exitProgramMode () {
    if (programActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x16,                 // Command ID (CMD_AVR8_PROG_MODE_LEAVE)
          0x00,                 // Command version (always 0x00)
      });
      programActive = false;
      printUpdi("exitProgramMode()");
    } else {
      throw new EDBGException("Call to exitProgramMode() when program mode is not active");
    }
  }

  /**
   * Resets the target and holds it stopped, in reset. A break event will be sent when the reset is done
   * and the target is stopped.
   */
  public byte[] resetTarget () {
    if (debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x30,                 // Command ID (CMD_AVR8_RESET)
          0x00,                 // Command version (always 0x00)
          0x01,                 // Level (0x01 = stop at boot reset vector)
      });
      try {
        breakWait(true);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
      printUpdi("resetTarget()");
      return new byte[0];
    } else {
      throw new EDBGException("Call to resetTarget() when debug mode is not active");
    }
  }

  /**
   * Stops execution on the target. A break event will be sent when the target is stopped.
   */
  public void stopTarget () throws InterruptedException {
    if (debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x31,                 // Command ID (CMD_AVR8_STOP)
          0x00,                 // Command version (always 0x00)
          0x01,                 // Level (0x01 = stop immediately)
      });
      breakWait(true);
      printUpdi("stopTarget()");
    } else {
      throw new EDBGException("Call to stopTarget() when debug mode is not active");
    }
  }

  /**
   * Resumes execution on the target.
   */
  public void runTarget () throws InterruptedException {
    if (debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x32,                 // Command ID (CMD_AVR8_RUN)
          0x00,                 // Command version (always 0x00)
      });
      breakWait(false);
      printUpdi("runTarget()");
    } else {
      throw new EDBGException("Call to runTarget() when debug mode is not active");
    }
  }

  /**
   * Resumes execution on the target, with an internal breakpoint at the given location. A break event
   * is sent when the target has reached the breakpoint. (This breakpoint cannot be accessed by any other means.)
   *
   * @param address word address for breakpoint
   */
  public void runToAddress (int address) throws InterruptedException {
    if (debugActive) {
      address >>= 1;            // Need word address
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x33,                 // Command ID (CMD_AVR8_RUN_TO_ADDRESS)
          0x00,                 // Command version (always 0x00)
          Utility.lsb(address), // Address LSB
          Utility.msb(address), // Address MSB
          0x00,                 //
          0x00,                 // Address (4 byte MSB)
      });
      breakWait(false);
      printUpdi(String.format("runToAddress(0x%04X)", address));
    } else {
      throw new EDBGException("Call to runToAddress() when debug mode is not active");
    }
  }

  /**
   * Performs a single step on the target. Returns OK once the step has successfully been initiated.
   * Once the step has completed a BREAK event is generated asynchronously.
   */
  public void stepTarget () {
    if (debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x34,                 // Command ID (CMD_AVR8_STEP)
          0x00,                 // Command version (always 0x00)
          0x01,                 // Level (0x01 = instruction level step)
          0x01,                 // Mode (0x01 = step into)
      });
      try {
        breakWait(true);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
      printUpdi("stepTarget()");
    } else {
      throw new EDBGException("Call to stepTarget() when debug mode is not active");
    }
  }

  /**
   * Reads the PC on the target.
   *
   * @return program counter as byte address
   */
  public int getProgramCounter () {
    if (debugActive) {
      byte[] rsp = sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x35,                 // Command ID (CMD_AVR8_PC_READ)
          0x00,                 // Command version (always 0x00)
      });
      printUpdi("getProgramCounter()");
      return getUnsigned16(rsp, 0) * 2;
    } else {
      throw new EDBGException("Call to getProgCounter() when debug mode is not active");
    }
  }

  /**
   * Modifies the PC on the target.
   *
   * @param address new Program Counter value (word address)
   * @return true
   */
  public void setProgramCounter (int address) {
    if (debugActive) {
      address >>= 1;            // Need word address
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x36,                 // Command ID (CMD_AVR8_PC_WRITE)
          0x00,                 // Command version (always 0x00)
          Utility.lsb(address), // PC LSB
          Utility.msb(address), // PC LSB
          0x00,                 //
          0x00,                 // PC (4 byte MSB)
      });
      printUpdi(String.format("setProgramCounter(0x%04X)", address));
    } else {
      throw new EDBGException("Call to setProgramCounter() when debug mode is not active");
    }
  }

  /**
   * Select UPDI Mode
   */
  public void setVariantUPDI () {
    if (sessionActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x01,                 // Command ID (CMD_AVR8_SET)
          0x00,                 // Command version (always (0x00)
          0x00,                 // Command context (AVR8_CTXT_CONFIG)
          0x00,                 // address (AVR8_CONFIG_VARIANT)
          0x01,                 // write 1 byte
          0x05,                 // AVR8_VARIANT_UPDI
      });
      printUpdi("setVariantUPDI()");
    } else {
      throw new EDBGException("Call to setVariantUPDI() when session is not active");
    }
  }

  /**
   * Configure Programmer for programming Flash memory
   */
  public void setFunctionProgram () {
    if (physicalActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x01,                 // Command ID (CMD_AVR8_SET)
          0x00,                 // Command version (always (0x00)
          0x00,                 // Command context (AVR8_CTXT_CONFIG)
          0x01,                 // address (AVR8_CONFIG_FUNCTION)
          0x01,                 // write 1 byte
          0x01,                 // AVR8_FUNC_PROGRAMMING
      });
      printUpdi("setFunctionProgram()");
    } else {
      throw new EDBGException("Call to setFunctionProgram() when physical interface is not active");
    }
  }

  /**
   * Configure Programmer for debugging
   */
  public void setFunctionDebug () {
    if (physicalActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x01,                 // Command ID (CMD_AVR8_SET)
          0x00,                 // Command version (always (0x00)
          0x00,                 // Command context (AVR8_CTXT_CONFIG)
          0x01,                 // address (AVR8_CONFIG_FUNCTION)
          0x01,                 // write 1 byte
          0x02,                 // AVR8_FUNC_DEBUGGING
      });
      printUpdi("setFunctionDebug()");
    } else {
      throw new EDBGException("Call to setFunctionDebug() when physical interface is not active");
    }
  }

  /**
   * Set Physical Interface to UPDI
   */
  public void setPhysicalInterfaceUPDI () {
    if (sessionActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x01,                 // Command ID (CMD_AVR8_SET)
          0x00,                 // Command version (always (0x00)
          0x01,                 // Command context (AVR8_CTXT_PHYSICAL)
          0x00,                 // address (AVR8_PHY_INTERFACE)
          0x01,                 // write 1 byte
          0x08,                 // UPDI Interface
      });
      printUpdi("setPhysicalInterfaceUPDI()");
    } else {
      throw new EDBGException("Call to setPhysicalInterfaceUPDI() when session is not active");
    }
  }

  /**
   * Set Clock Rate for UPDI Interface
   *
   * @param kHz clock rate in kHz
   */
  public void setClockUPDI (int kHz) {
    if (sessionActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x01,                 // Command ID (CMD_AVR8_SET)
          0x00,                 // Command version (always (0x00)
          0x01,                 // Command context (AVR8_CTXT_PHYSICAL)
          0x31,                 // address (AVR8_PHY_XM_PDI_CLK)
          0x02,                 // write 2 bytes
          Utility.lsb(kHz),     // UPDI Clock (kHz) (lsb)
          Utility.msb(kHz),     // UPDI Clock (kHz) (msb)
      });
      printUpdi(String.format("setClockUPDI(%d kHz)", kHz));
    } else {
      throw new EDBGException("Call to setClockUPDI() when session is not active");
    }
  }

  //  ATTiny416 Memory Addresses
  //    I/O Registers (64)                0x0000 - 0x003F
  //      Virtual Port A                  0x0000 - 0x0003 ( 4 bytes) DIR, OUT, IN, INTFLAGS
  //      Virtual Port B                  0x0004 - 0x0007 ( 4 bytes)
  //      Virtual Port C                  0x0008 - 0x000B ( 4 bytes)
  //    Ext I/O Registers (960)           0x0040 - 0x0FFF
  //      System Configuration            0x0F00 - 0x0FFF
  //      NVM I/O Registers and data      0x1000 - 0x13FF
  //      Non Volatile Memory Controller  0x1000 - 0x10FF
  //      Signature Row                   0x1100 - 0x127F
  //        Device ID (signature)         0x1100 - 0x1102 (3 bytes)
  //        Serial Num 0                  0x1103 - 0x110C (10 bytes)
  //          ATTiny416 Xplained = 0x1E 0x92 0x21 0x51 0x50 0x51 0x54 0x41 0x20 0x65 0x9E 0x4E 0x48  - QPQTA eﾞNH
  //          ATTiny817 Xplained = 0x1E 0x93 0x20 0x51 0x4E 0x48 0x59 0x32 0x20 0xCB 0x43 0x28 0x36  - QNHY2 ￋC(6
  //        Temp Sense Calibration        0x1120 - 0x1121 (2 bytes)
  //        OSC16 error at 3V             0x1122
  //        OSC16 error at 5V             0x1123
  //        OSC20 error at 3V             0x1124
  //        OSC20 error at 5V             0x1125
  //      Device specific fuses           0x1280 - 0x12FF
  //        Watchdog Configuration        0x1280
  //        BOD Configuration             0x1281
  //        Oscillator Configuration      0x1282
  //        Timer Counter Type D Conf     0x1284
  //        System Configuration 0        0x1285
  //        System Configuration 1        0x1286
  //        Application Code End          0x1287
  //        Boot End                      0x1288
  //        Lock Bits                     0x128A
  //      User Row                        0x1300 -
  //    EEPROM (128 bytes)                0x1400 - 0x147F
  //    Reserved                          0x1480 - 0x3DFF
  //    SRAM (256)                        0x3F00 - 0x3FFF
  //    SRAM (512)                        0x3E00 - 0x3FFF
  //    Reserved                          0x4000 - 0x7FFF
  //    Flash Code (4K)                   0x8000 - 0x8FFF
  //    Flash Code (4K)                   0x8000 - 0x9FFF

  /**
   * Performs an erase on the target. Note: Functionality varies according to target family.
   *
   * @param address Start address of section to erase (byte address)
   * @param mode    (see table 7-25)
   *                0x00 = Chip erase (use this mode for ATTiny)
   *                0x01 = Application erase
   *                0x02 = Boot section erase
   *                0x03 = EEPROM erase
   *                0x04 = Application page erase
   *                0x05 = Boot page erase
   *                0x06 = EEPROM page erase
   *                0x07 = User signature erase
   * @return null
   */
  public byte[] eraseTarget (int address, int mode) {
    if (programActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x20,                 // Command ID (CMD_AVR8_ERASE)
          0x00,                 // Command version (always (0x00)
          (byte) mode,          // Mode (see table 7-25)
          Utility.lsb(address), // Address LSB
          Utility.msb(address), // Address MSB
          0x00,
          0x00,                 // Address (4 byte MSB)
      });
      printUpdi(String.format("eraseTarget(0x%04X, 0x%02X)", address, mode));
    } else {
      throw new EDBGException("Call to eraseTarget() when program mode is not active");
    }
    return null;
  }

  /**
   * Reads memory from the target.
   * Note: Memories can only be accessed when the device is in STOPPED mode
   *
   * @param address Start address [byte address] of memory to read
   * @param memType Memory type to access, such as MEMTYPE_FLASH_PAGE
   * @param length  number of bytes to read
   */
  private byte[] memoryRead (int address, int memType, int length) {
    byte[] ret =  sendAvrCmd(new byte[] {
        0x12,                 // AVR8GENERIC
        0x21,                 // Command ID (CMD_AVR8_MEMORY_READ)
        0x00,                 // Command version (always (0x00)
        Utility.lsb(memType), // Type (UPDI - table 7-46)
        Utility.lsb(address), // Address LSB
        Utility.msb(address), // Address MSB
        0x00,
        0x00,                 // Address (4 byte MSB)
        Utility.lsb(length),  // Bytes to read (LSB)
        Utility.msb(length),  // Bytes to read (MSB)
        0x00,
        0x00,                 // Bytes to read (4 byte MSB)
    });
    printUpdi(String.format("memoryRead(0x%04X, %s, 0x%04X)", address, getTypeDesc(memType), length));
    return ret;
  }

  // Note: memoryWriteMasked() not implemented

  /**
   * Writes memory on the target.
   * Note: Memories can only be accessed when the device is in STOPPED mode
   *
   * @param address Start address [byte address] of memory to write
   * @param memType Memory type to access, such as MEMTYPE_FLASH_PAGE
   * @param data    byte[] array of data to write
   */
  private void memoryWrite (int address, int memType, byte[] data) {
    byte[] cmd = new byte[] {
        0x12,                     // AVR8GENERIC
        0x23,                     // Command ID (CMD_AVR8_MEMORY_WRITE)
        0x00,                     // Command version (always (0x00)
        Utility.lsb(memType),     // Type (UPDI - table 7-46)
        Utility.lsb(address),     // Address LSB
        Utility.msb(address),     // Address MSB
        0x00,
        0x00,                     // Address (4 byte MSB)
        Utility.lsb(data.length), // Bytes to write (LSB)
        Utility.msb(data.length), // Bytes to write (MSB)
        0x00,
        0x00,                     // Bytes to read (4 byte MSB)
        0x00,                     // 0x00 = write first, then reply, 0x01 = reply first, then write
    };
    byte[] tmp = new byte[cmd.length + data.length];
    System.arraycopy(cmd, 0, tmp, 0, cmd.length);
    System.arraycopy(data, 0, tmp, cmd.length, data.length);
    sendAvrCmd(tmp);
    printUpdi(String.format("memoryWrite(0x%04X, %s, length = 0x%04X)", address, getTypeDesc(memType), data.length));
  }

  private byte[] readMemLoop (int address, int memType, int len) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    for (int ii = 0; ii < len; ii += 64) {
      int remain = Math.min(len - ii, 64);
      byte[] chunk = memoryRead(address + ii, memType, remain);
      bout.write(chunk, 0, chunk.length);
    }
    return bout.toByteArray();
  }

  private void writeMemLoop (int address, int memType, byte[] data) {
    for (int ii = 0; ii < data.length; ii += 64) {
      int remain = Math.min(data.length - ii, 64);
      byte[] buf = new byte[remain];
      System.arraycopy(data, ii, buf, 0, remain);
      memoryWrite(address + ii, memType, buf);
    }
  }

  /**
   * Read "len" bytes from Flash starting at "address"
   * Note: base address of Flash is typically 0x0000
   *
   * @param address starting read address
   * @param len     number of bytes to read
   * @return true
   */
  public byte[] readFlash (int address, int len) {
    if (debugActive || programActive) {
      return readMemLoop(address, MEMTYPE_FLASH_PAGE, len);
    } else {
      throw new EDBGException("Call to readFlash() when debug or program mode is not active");
    }
  }

  /**
   * Write data[] array to Flash starting at "address"
   * Note: base address of Flash is typically 0x0000 and must be in program mode to call
   *
   * @param address starting write address (multiple of page size?)
   * @param data    data to write
   */
  public void writeFlash (int address, byte[] data) {
    if (programActive) {
      int fPage = chip.getInt("fpage");
      // round up to multiple of target's flash page size
      byte[] buf = new byte[((data.length + fPage - 1) / fPage) * fPage];
      Arrays.fill(buf, (byte) 0xFF);
      System.arraycopy(data, 0, buf, 0, data.length);
      writeMemLoop(address, MEMTYPE_FLASH_PAGE, buf);
    } else {
      throw new EDBGException("Call to readFlash() when program mode is not active");
    }
  }

  /**
   * Read "len" bytes from SRAM starting at "address"
   * Note: base address of SRAM varies with size of SRAM and must be in debug mode to call
   * base = 0x3F80 for 128 bytes of SRAM
   * base = 0x3F00 for 256 bytes of SRAM
   * base = 0x3E00 for 512 bytes of SRAM
   * base = 0x3C00 for 1024 bytes of SRAM
   * base = 0x3800 for 2048 bytes of SRAM
   *
   * @param address starting read address
   * @param len     number of bytes to read
   */
  public byte[] readSRam (int address, int len) {
    if (debugActive) {
      return readMemLoop(address, MEMTYPE_SRAM, len);
    } else {
      throw new EDBGException("Call to readSRam() when debug mode is not active");
    }
  }

  /**
   * Write data[] array to SRAM starting at "address"
   * Note: base address of SRAM varies with size of SRAM and must be in debug mode to call
   * base = 0x3F80 for 128 bytes of SRAM
   * base = 0x3F00 for 256 bytes of SRAM
   * base = 0x3E00 for 512 bytes of SRAM
   * base = 0x3C00 for 1024 bytes of SRAM
   * base = 0x3800 for 2048 bytes of SRAM
   *
   * @param address starting write address
   * @param data    data to write
   */
  public void writeSRam (int address, byte[] data) {
    if (debugActive) {
      writeMemLoop(address, MEMTYPE_SRAM, data);
    } else {
      throw new EDBGException("Call to writeSRam() when debug mode is not active");
    }
  }

  /**
   * Read "len" bytes from EEPROM starting at "address"
   * Note: base address for EEPROM starts at 0x1400 and must be in debug or program mode to call
   *
   * @param address starting read address (offset from EEPROM_BASE)
   * @param len     number of bytes to read
   */
  public byte[] readEeprom (int address, int len) {
    if (debugActive || programActive) {
      return readMemLoop(address, MEMTYPE_EEPROM, len);
    } else {
      throw new EDBGException("Call to readEeprom() when debug or program mode is not active");
    }
  }

  /**
   * Write data[] array to EEPROM starting at "address"
   * Note: base address for EEPROM starts at 0x1400 and must be in debug or program mode to call
   *
   * @param address starting write address (offset from EEPROM_BASE)
   * @param data    data to write
   */
  public void writeEeprom (int address, byte[] data) {
    if (debugActive || programActive) {
      writeMemLoop(address, MEMTYPE_EEPROM, data);
    } else {
      throw new EDBGException("Call to writeEeprom() when debug or program mode is not active");
    }
  }

  /**
   * Read and return data from registers
   * Note: base address for registers starts at 0x0000 and must be in debug mode to call
   *
   * @param address  starting register address to read
   * @param len number of registers to read
   * @return byte[len] array of register data
   */
  public byte[] readRegisters (int address, int len) {
    if (debugActive) {
      return memoryRead(address, MEMTYPE_REGFILE, len);
    } else {
      throw new EDBGException("Call to readRegisters() when debug mode is not active");
    }
  }

  /**
   * Write regs[] array to registers starting at address
   * Note: base address for registers starts at 0x0000 and must be in debug mode to call
   *
   * @param address starting register address to write
   * @param regs    byte[] array of register data to write
   */
  public void writeRegisters (int address, byte[] regs) {
    if (debugActive) {
      memoryWrite(address, MEMTYPE_REGFILE, regs);
    } else {
      throw new EDBGException("Call to writeRegisters() when debug mode is not active");
    }
  }

  /**
   * Read fuse starting at FUSES_BASE + offsets
   * Note: base offset for fuses starts at 0x1280 and must be in program mode to call
   *
   *  Default fuse values:          attiny416 attiny212 attiny3217
   *    0x00  WDTCFG    = 00000000    0x00      0x00      0x00
   *    0x01  BODCFG    = 00000000    0x00      0x00      0x00
   *    0x02  OSCCFG    = 0-----10    0x02      0x02      0x02
   *    0x03  Reserved                0xFF      0xFF      0xFF
   *    0x04  TCD0CFG   = 00000000    0x00      0x00      0x00
   *    0x05  SYSCFG0   = 11-101-0    0xCD      0xF6      0xF6
   *    0x06  SYSCFG1   = -----111    0x03      0x07      0x07
   *    0x07  APPEND    = 00000000    0x00      0x00      0x00
   *    0x08  BOOTEND   = 00000000    0x02      0x00      0x00
   *    0x09  Reserved                0xFF      0xFF      0xFF
   *    0x0A  LOCKBIT   = 00000000    0xC5      0xC5      0xC5
   *
   *    Note: attiny416  from ATTiny416-Xplained-Nano
   *          attiny3217 from ATTiny3217-Curiosity-Nano
   *          attiny212  from raw attiny212 chip
   *
   * @param offsets array of offsets to FUSE_BASE from which to read fuses
   * @return byte[offsets.length] array containing fuse data
   */
  public byte[] readFuses (int[] offsets) {
    if (programActive) {
      byte[] rsp = new byte[offsets.length];
      for (int ii = 0; ii < rsp.length; ii++) {
        byte[] tmp = memoryRead(FUSES_BASE + offsets[ii], MEMTYPE_FUSES, 1);
        rsp[ii] = tmp[0];
      }
      return rsp;
    } else {
      throw new EDBGException("Call to readFuses() when program mode is not active");
    }
  }

  /**
   * Write to fuse at FUSES_BASE + offsets (debugger can only write one byte at a time)
   * Note: base offset for fuses starts at 0x1280 and must be in program mode to call
   *
   * @param offsets array of offsets to FUSE_BASE from which to write fuses
   * @param fuses   fuse data byte  to write
   */
  public void writeFuses (int[] offsets, byte[] fuses) {
    if (programActive) {
      if (offsets.length != fuses.length) {
        throw new EDBGException("Call to writeFuses() length of offsets[] does not match length of fuses[]");
      }
      for (int ii = 0; ii < offsets.length; ii++) {
        memoryWrite(FUSES_BASE + offsets[ii], MEMTYPE_FUSES, new byte[] {fuses[ii]});
      }
    } else {
      throw new EDBGException("Call to writeFuses() when program mode is not active");
    }
  }

  /**
   * Read target's stack pointer
   * @return stack pointer
   */
  public int getStackPointer () {
    if (debugActive) {
      byte[] rsp = readSRam(STACK_POINTER, 2);
      return getUnsigned16(rsp, 0);
    } else {
      throw new EDBGException("Call to getStackPointer() when debug mode is not active");
    }
  }

  /**
   * Write target's stack pointer
   * @param sp new stack pointer
   */
  public void writeStackPointer (int sp) {
    if (debugActive) {
      writeSRam(STACK_POINTER, new byte[] {Utility.lsb(sp), Utility.msb(sp)});
    } else {
      throw new EDBGException("Call to writeStackPointer() when debug mode is not active");
    }
  }

  /**
   * Read target's status register (flags)
   * @return status register
   */
  public byte getStatusRegister () {
    if (debugActive) {
      byte[] rsp = readSRam(STATUS_REGISTER, 1);
      return rsp[0];
    } else {
      throw new EDBGException("Call to getStatusRegister() when debug mode is not active");
    }
  }

  /**
   * Write target's status register (flags)
   */
  public void writeStatusRegister (byte data) {
    if (debugActive) {
      writeSRam(STATUS_REGISTER, new byte[] {data});
    } else {
      throw new EDBGException("Call to writeStatusRegister() when debug mode is not active");
    }
  }

  /**
   * Get 3 byte device signature of target device
   * Note: must be in program, or debug to call
   *
   * @return 3 byte[] array with signature, such as 0x1E, 0x91, 0x21 (attiny212)
   */
  public byte[] getDeviceSignature () {
    if (debugActive || programActive) {
      byte[] rsp = memoryRead(0x1100, MEMTYPE_SIGNATURE, 3);
      printUpdi("getDeviceSignature()");
      return rsp;
    } else {
      throw new EDBGException("Call to getDeviceSignature() when debug or program mode is not active");
    }
  }

  /**
   * get 13 byte device serial number
   *
   * @return 13 byte[] array with serial number
   */
  public byte[] getDeviceSerialNumber () {
    if (debugActive || programActive) {
      return memoryRead(0x1100 + 3, MEMTYPE_SIGNATURE, 16 - 3);
    } else {
      throw new EDBGException("Call to getDeviceSerialNumber() when debug or program mode is not active");
    }
  }

  /**
   * Allocates hardware breakpoint resources on the target OCD module.
   * Note: AVR devices with UPDI have one hardware breakpoint in addition to the special breakpoint used
   * by the "run to address" and "step" commands.
   * IMPORTANT: uses byte address, not word address
   *
   * @param address Byte address for breakpoint
   * @param num     Breakpoint number to set (1, 2, or 3)
   */
  public void setHardwareBreakpoint (int address, int num) {
    if (debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x40,                 // Command ID (CMD_AVR8_HW_BREAK_SET)
          0x00,                 // Command version (always (0x00)
          0x01,                 // Type (0x01 = program break)
          Utility.lsb(num),     // Number (Breakpoint number to set (1, 2, or 3))
          Utility.lsb(address), // Address LSB (word address)
          Utility.msb(address), // Address MSB
          0x00,
          0x00,                 // Address (4 byte MSB)
          0x03,                 // Mode (0x03 = program break)
      });
      printUpdi(String.format("setHardwareBreakpoint(0x%04X, %d)", address, num));
    } else {
      throw new EDBGException("Call to setHardwareBreakpoint() when debug mode is not active");
    }
  }

  /**
   * Clears hardware breakpoint resources on the target OCD module.
   * Note: AVR devices with UPDI have one hardware breakpoint, which is '1'.
   *
   * @param num Breakpoint number to clear (1, 2, or 3)
   */
  public void clearHardwareBreakpoint (int num) {
    if (debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x41,                 // Command ID (CMD_AVR8_HW_BREAK_CLR)
          0x00,                 // Command version (always (0x00)
          Utility.lsb(num),      // Number (Breakpoint number to set (1, 2, or 3))
      });
      printUpdi(String.format("clearHardwareBreakpoint(%d)", num));
    } else {
      throw new EDBGException("Call to clearHardwareBreakpoint() when debug mode is not active");
    }
  }

  /**
   * Inserts a set of software breakpoints on the target. Breakpoints are only inserted to flash when the next
   * flow control command is executed.
   *
   * @param addresses array of breakpoints
   */
  public void setSoftwareBreakpointSet (int[] addresses) {
    if (debugActive) {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      bout.write(0x12);                 // AVR8GENERIC
      bout.write(0x43);                 // Command ID (CMD_AVR8_SW_BREAK_SET)
      bout.write(0x00);                 // Command version (always (0x00)
      for (int add : addresses) {
        add >>= 1;
        bout.write(Utility.lsb(add));   // Address LSB (word address)
        bout.write(Utility.msb(add));   // Address MSB
        bout.write(0x00);               //
        bout.write(0x00);               // Address (4 byte MSB)
      }
      sendAvrCmd(bout.toByteArray());
      printUpdi(String.format("setSoftwareBreakpointSet(%d)", addresses.length));
    } else {
      throw new EDBGException("Call to setSoftwareBreakpointSet() when debug mode is not active");
    }
  }

  /**
   * Clears a set of software breakpoints on the target. Breakpoints are only removed from flash when the next
   * flow control command is executed.
   *
   * @param addresses array of breakpoints
   */
  public void clearSoftwareBreakpointSet (int[] addresses) {
    if (debugActive) {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      bout.write(0x12);                 // AVR8GENERIC
      bout.write(0x44);                 // Command ID (CMD_AVR8_SW_BREAK_CLEAR)
      bout.write(0x00);                 // Command version (always (0x00)
      for (int add : addresses) {
        add >>= 1;
        bout.write(Utility.lsb(add));   // Address LSB (word address)
        bout.write(Utility.msb(add));   // Address MSB
        bout.write(0x00);               //
        bout.write(0x00);               // Address (4 byte MSB)
      }
      sendAvrCmd(bout.toByteArray());
      printUpdi(String.format("clearSoftwareBreakpointSet(%d)", addresses.length));
    } else {
      throw new EDBGException("Call to clearSoftwareBreakpointSet() when debug mode is not active");
    }
  }

  /**
   * Removes all software breakpoints immediately. Useful if you have forgotten where you put them.
   */
  public void clearAllSoftwareBreakpoints () {
    if (debugActive) {
      sendAvrCmd(new byte[] {
          0x12,                 // AVR8GENERIC
          0x45,                 // Command ID (CMD_AVR8_SW_BREAK_CLEAR_ALL)
          0x00,                 // Command version (always 0x00)
      });
      printUpdi("clearAllSoftwareBreakpoints()");
    } else {
      throw new EDBGException("Call to clearAllSoftwareBreakpoints() when debug mode is not active");
    }
  }

  /*
   *  Events
   *    AVR_EVENT       = 0x82;
   *    EVT_AVR8_BREAK  = 0x40;
   *    EVT_AVR8_IDR    = 0x41
   *
   *    0           0x82  AVR_EVENT Command
   *    1           0x00  size (msb)
   *    2           0x0D  size (lsb) = 0x0D = 13
   *    3    1      0x0E  SOF
   *    4    2      0x00  protocol version
   *    5    3      0x04  sequence (lsb)
   *    6    4      0x00  sequence (msb)
   *    7    5      0x12  Protocol handler ID = AVR8GENERIC
   *    8    6   0  0x40  EVT_AVR8_BREAK
   *    9    7   1  0x1B  PC (lsb)
   *   10    8   2  0x00
   *   11    9   3  0x00
   *   12   10   4  0x00  PC (msb)
   *   13   11   5  0x01  Break cause (0x00 = unspecified, 0x01 = program breakpoint)
   *   14   12   6  0x04  Extended Info (lsb)
   *   15   13   7  0x01  Extended Info (msb)
   */

  /*
    From:  Judd Emile Bard at Microchip

    LSB, low nibble: the 0x04 you are reading is "stopped" state - anything else is considered invalid from the "outside".
      bit 7: RESET - halt cause is a reset
      bit 6: STOP - halt cause is the debugger

    MSB:
      bit 5: SWBP (raise when a BREAK instruction is fetched)
      bit 1: BP1 (raised when BP1 is hit)
      bit 0: BP0 (raised when BP0 is hit)

    Break After:
     1. ATTACH
     2. RESET
     3. STOP
     4. STEP
     5. RUN_TO
     6. LEAVE_PROG_MODE
   */
  static class Break {
    int pc;
    int ext;
    boolean reset, stop, swbp, bp0, bp1;

    Break (byte[] data) {
      pc = getUnsigned16(data, 1);        // get byte aaddress for break
      if (data.length >= 8) {
        ext = getUnsigned16(data, 6);     // get raw extended info for break
        reset = (data[6] & 0x80) != 0;    // true if break resulted from RESET
        stop = (data[6] & 0x40) != 0;     // true if break resulted from ATTACH
        swbp = (data[7] & 0x20) != 0;
        bp0 = (data[7] & 0x02) != 0;      // true if hit hardware breakpoint
        bp1 = (data[7] & 0x01) != 0;      // true if break resulted from RUN_TO or STEP
      }
    }

    public String toString () {
      StringBuilder buf = new StringBuilder("BREAK at ");
      buf.append(String.format("0x%04X, ext 0x%04X", pc * 2, ext));
      if (reset) {
        buf.append(", RESET");
      }
      if (stop) {
        buf.append(", STOP");
      }
      if (swbp) {
        buf.append(", SWBP");
      }
      if (bp0) {
        buf.append(", BP0");
      }
      if (bp1) {
        buf.append(", BP1");
      }
      return buf.toString();
    }
  }

  private static byte[] decodeResponse (byte[] data) {
    if (data[0] == (byte) 0x82 && data.length >= 8) {
      int length = ((int) data[2] & 0xFF) + (((int) data[1] & 0xFF) << 8);
      if (length > 5 && data[3] == 0x0E && data[4] == 0x00) {
        byte[] tmp = new byte[length - 5];
        System.arraycopy(data, 8, tmp, 0, tmp.length);
        return tmp;
      }
    }
    return null;
  }

  /**
   * Wait for Break Event and handle IDR events, if any
   * @param doTimeout if true, return after timeout period
   */
  private void breakWait (boolean doTimeout) throws InterruptedException {
    int timeout = 20;
    StringBuilder msg = new StringBuilder();
    while (!doTimeout || timeout-- > 0) {
      byte[] data = sendCmd(new byte[] {(byte) 0x82});
      byte[] rsp = decodeResponse(data);
      if (rsp != null) {
        if (rsp[0] == 0x40) {                 // EVT_AVR8_BREAK
          if (DEBUG_PRINT) {
            Break brk = new Break(rsp);
            debugPrint(brk.toString());
          }
          return;
        } else if (rsp[0] == 0x41) {         // EVT_AVR8_IDR
          char cc = (char) rsp[2];
          msg.append(cc);
          if (cc == '\n') {
            if (ocdListener != null) {
              ocdListener.msgReceived(msg.toString());
            }
            msg.setLength(0);
          }
          timeout = 20;
          Thread.sleep(10);
          continue;
        }
      }
      Thread.sleep(50);
    }
    if (ocdListener != null) {
      ocdListener.msgReceived("timeout\n");
    }
    throw new EDBGException("breakWait() timeout");
  }
}
