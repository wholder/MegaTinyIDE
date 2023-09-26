import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;

import static javax.swing.JOptionPane.*;

/*
 *  Fuse Bytes
 *    0   WDTCFG - Watchdog Configuration (after reset = 0x00)
 *        bits 7:4  WINDOW    - Watchdog Window Time-Out Period (loaded into WINDOW bit field of WDT.CTRLA during reset)
 *        bits 3:0  PERIOD    - Watchdog Time-Out Period (loaded into PERIOD bit field of WDT.CTRLA during reset)
 *    1   BODCFG - Brown Out Detector Configuration (after reset = 0x00)
 *        bits 7:5  LVL       - BOD Level (0 = 1.8V, 2 = 2.6V, 7 = 4.2V)
 *        bit 4     SAMPFREQ  - BOD Sample Frequency (0 = 1 kHz, 1 = 125 kHz)
 *        bits 3:2  ACTIVE    - BOD Operation Mode in Active and Idle (0 = Disabled, 1 = Enabled, 2 = Sampled, 3 = Enabled with wake halted)
 *        bits 1:0  SLEEP     - BOD Operation Mode in Sleep (0 = Disabled, 1 = Enabled, 2 = Sampled, 3 = Reserved)
 *    2   OSCCFG - Oscillator Configuration (after reset = 0x02
 *        bit 7     OSCLOCK   - Oscillator Lock (0 = OSC20M regs are accessible, 1 = OSC20M regs are locked)
 *        bits 1:0  FREQSEL   - Frequency Select (0 = 16 MHz, 2 = 20 MHz (default), others reserved)
 *    3   RESERVED
 *    4   TCD0CFG - Timer Counter Type D Configuration (sets corresponding bits of TCD.FAULTCTRL register of TCD0 at start-up, reset = 0xx00)
 *        bit 7     CMPDEN    - Compare D Enable (Compare D output on Pin is disabled)
 *        bit 6     CMPCEN    - Compare C Enable (Compare C output on Pin is disabled)
 *        bit 5     CMPBEN    - Compare B Enable (Compare B output on Pin is disabled)
 *        bit 4     CMPAEN    - Compare A Enable (Compare A output on Pin is disabled)
 *        bit 3     CMPD      - Compare D (Compare D default state is ‘0’)
 *        bit 2     CMPC      - Compare C (Compare C default state is ‘0’)
 *        bit 1     CMPB      - Compare B (Compare B default state is ‘0’)
 *        bit 0     CMPA      - Compare A (Compare A default state is ‘0’)
 *    5   SYSCFG0 - System Configuration 0 (after reset = 0xE4)
 *        bits 7:6  CRCSRC    - CRC Source (0 = CRC full Flash, 1 = CRC Boot section, 2 = CRC Boot and App sections, 3 = No CRC (default))
 *        bit 4     TOUTDIS   - Time-Out Disable (0 = NVM write block enabled, 1 = NVM write block disabled (default)
 *        bits 3:2  RSTPINCFG - Reset Pin Configuration (0 = GPIO, 1 = UPDI (default), 2 = RESET, 3 = reserved)
 *        bit 0     EESAVE    - EEPROM Save During Chip Erase (0 = EEPROM erased during chip erase (default), 1 = not erased)
 *    6   SYSCFG1 - System Configuration 1 (after reset = 0x07)
 *        bits 2:0  SUT       - Start-Up Time Setting (0 ms, 1 ms, 2 ms, 4 ms, 8 ms, 16 ms, 32 ms, 64 ms (default))
 *    7   APPEND - Application Code End (after reset = 0x00)
 *        bits 7:0  APPEND    - Application Code Section End (value in blocks of 256)
 *    8   BOOTEND - Boot End (after reset = 0x00)
 *        bits 7:0  BOOTEND   - Boot Section End (value in blocks of 256)
 *    9   RESERVED
 *    A   LOCKBIT - Lockbits (after reset = 0x00)
 *        bits 7:0  LOCKBIT   - Lockbits (any value other than 0xC5 locks system bus)
 *
 *    Note: TOUTDIS only present on some chips
 */

public class FusePane extends JPanel {
  static Map<String,String>   tooltips = new HashMap<>();
  Map<String,MyJComboBox>     comps = new HashMap<>();
  private int                 col, row;
  private int                 appEnd, bootEnd;
  private final byte[]        priorVals = new byte[11];
  private final int[]         usedBitMasks = new int[] {0xFF, 0xFF, 0x83, 0x00, 0xFF, 0xDD, 0x07, 0xFF, 0xFF, 0x00, 0x00};
  boolean                     disableChangeDetect;

  static {
    try {
      tooltips = Utility.getResourceMap("fuseFields.props");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  class MyJComboBox extends JComboBox<String> {
    private final Map<Integer,Integer>  valueIndex = new HashMap<>();
    private final Map<Integer,Integer>  indexValue = new HashMap<>();
    private int                         initialValue;

    MyJComboBox (String name, String[] values) {
      String toolTip = tooltips.get(name);
      setToolTipText(toolTip != null ? (name + ": " + toolTip) : name);
      setPrototypeDisplayValue("xx");
      DefaultListCellRenderer listRenderer = new DefaultListCellRenderer();
      listRenderer.setHorizontalAlignment(DefaultListCellRenderer.CENTER);
      setRenderer(listRenderer);
      for (int ii = 0; ii < values.length; ii++) {
        String[] parts = values[ii].split(":");
        int val;
        if (parts.length > 1) {
          addItem(parts[0].trim());
          val = Integer.parseInt(parts[1].trim());
        } else {
         addItem(values[ii].trim());
          val = ii;
        }
        valueIndex.put(val, ii);
        indexValue.put(ii, val);
      }
    }

    public void setValue (int value) {
      disableChangeDetect = true;
      initialValue = value;
      setSelectedIndex(valueIndex.get(value));
      disableChangeDetect = false;
    }

    public boolean hasChanged () {
      return initialValue != getSelectedValue();
    }

    public int getSelectedValue () {
      int idx = getSelectedIndex();
      return indexValue.get(idx);
    }
  }

  public MyJComboBox addField (String desc) {
    String[] parts = desc.split("=");
    if (parts.length == 2) {
      String[] vals = parts[1].split(",");
      return addField(parts[0].trim(), vals);
    } else {
      throw new IllegalStateException("addField() no '=' delimiter");
    }
  }

  public MyJComboBox addField (String name, String[] values) {
    String[] parts = name.split(":");
    int bits;
    if (parts.length > 1) {
      bits = Integer.parseInt(parts[1]);
      name = parts[0];
      JPanel panel = new JPanel(new GridLayout(1, 1));
      panel.setBorder(Utility.getBorder(BorderFactory.createLineBorder(Color.gray), 1, 1, 2, 1));
      MyJComboBox comp = new MyJComboBox(name, values);
      comps.put(name, comp);
      panel.add(comp);
      add(panel, new Rectangle(col, row, bits, 1));
      col += bits;
      if (col >= 8) {
        col = 0;
        row++;
      }
      return comp;
    } else {
      throw new IllegalStateException("number of bits not specified");
    }
  }

  public void addReservedBits (int count) {
    for (int bit = 0; bit < count; bit++) {
      JPanel panel = new JPanel(new FlowLayout());
      panel.setBorder(Utility.getBorder(BorderFactory.createLineBorder(Color.gray), 1, 1, 2, 1));
      JLabel lbl = new JLabel(Integer.toString(7 - col));
      lbl.setHorizontalAlignment(SwingConstants.CENTER);
      lbl.setVerticalAlignment(SwingConstants.CENTER);
      panel.add(lbl);
      add(panel, new Rectangle(col, row, 1, 1));
      col += 1;
      if (col >= 8) {
        col = 0;
        row++;
      }
    }
  }

  public void setFieldValue (String name, int value) {
    if (comps.containsKey(name)) {
      comps.get(name).setValue(value);
    } else {
      throw new IllegalStateException("setFieldValue() unknown field name: " + name);
    }
  }

  public int getFieldValue (String name) {
    return comps.get(name).getSelectedValue();
  }

  public boolean hasChanged (String name) {
    return comps.get(name).hasChanged();
  }

  //    0     1     2     3     4     5     6     7     8     9    A
  // 0xFF, 0xFF, 0x83, 0x00, 0xFF, 0xDD, 0x07, 0xFF, 0xFF, 0x00, 0x00

  public boolean hasChanged (int offset) {
    return (priorVals[offset] & usedBitMasks[offset]) != (getFuse(offset) & usedBitMasks[offset]);
  }

  public void setFuse (int offset, byte value) {
    priorVals[offset] = value;
    switch (offset) {
    case 0x00:    // WDTCFG - Watchdog Configuration
      setFieldValue("WINDOW",     (value >> 4) & 0x0F);       // Mask - 0xF0
      setFieldValue("PERIOD",     value & 0x0F);              // Mask = 0x0F
                                                              // ORed = 0xFF
      break;
    case 0x01:    // BODCFG - Brown Out Detector Configuration
      setFieldValue("LVL",        (value >> 5) & 0x07);       // Mask = 0xE0
      setFieldValue("SAMPFREQ",   (value >> 4) & 0x01);       // Mask = 0x10
      setFieldValue("ACTIVE",     (value >> 2) & 0x03);// Mask = 0x0C
      setFieldValue("SLEEP",      value & 0x03);// Mask = 0x03
                                                              // ORed = 0xFF
      break;
    case 0x02:    // OSCCFG - Oscillator Configuration
      setFieldValue("OSCLOCK",    (value >> 7) & 0x01);       // Mask = 0x80
      setFieldValue("FREQSEL",    value & 0x03);              // Mask = 0x03
                                                              // ORed = 0x83
      break;
    case 0x04:    // Timer Counter Type D Configuration
      setFieldValue("CMPDEN",     (value >> 7) & 0x01);       // Mask = 0x80
      setFieldValue("CMPCEN",     (value >> 6) & 0x01);       // Mask = 0x40
      setFieldValue("CMPBEN",     (value >> 5) & 0x01);       // Mask = 0x20
      setFieldValue("CMPAEN",     (value >> 4) & 0x01);       // Mask = 0x10
      setFieldValue("CMPD",       (value >> 3) & 0x01);       // Mask = 0x08
      setFieldValue("CMPC",       (value >> 2) & 0x01);       // Mask = 0x04
      setFieldValue("CMPB",       (value >> 1) & 0x01);       // Mask = 0x02
      setFieldValue("CMPA",       value & 0x01);              // Mask = 0x01
                                                              // ORed = 0xFF
      break;
    case 0x05:    // SYSCFG0 - System Configuration 0
      setFieldValue("CRCSRC",     (value >> 6) & 0x03);       //  Mask = 0xC0
      setFieldValue("TOUTDIS",    (value >> 4) & 0x01);       //  Mask = 0x10   // ATtiny3216/3217
      setFieldValue("RSTPINCFG",  (value >> 2) & 0x03);       //  Mask = 0x0C
      setFieldValue("EESAVE",     value & 0x01);              //  Mask = 0x01
                                                              //  ORed = 0xDD
      break;
    case 0x06:    // SYSCFG1 - System Configuration 1
      setFieldValue("SUT",        value & 0x07);              // Mask = 0x07
                                                              // ORed = 0x07
      break;
    case 0x07:    // APPEND - Application Code End
      appEnd = value;                                         // Mask = 0xFF
      updateBootApp();
      break;
    case 0x08:    // BOOTEND - Boot End
      bootEnd = value;                                         // Mask = 0xFF
      break;
    case 0x0A:    // LOCKBIT - Lockbits
      break;                                                  // Mask = 0x00
    default:
      throw new IllegalStateException("Invalid fuse offset " + offset);
    }
  }

  private void updateBootApp () {
    setFieldValue("APPSIZE",    appEnd - bootEnd);
    setFieldValue("BOOTSIZE",   bootEnd);
  }

  public byte getFuse (int offset) {
    byte fuse = 0;
    switch (offset) {
    case 0x00:    // WDTCFG - Watchdog Configuration
      fuse |= getFieldValue("WINDOW") << 4;
      fuse |= getFieldValue("PERIOD");
      break;
    case 0x01:    // BODCFG - Brown Out Detector Configuration
      fuse |= getFieldValue("LVL") << 5;
      fuse |= getFieldValue("SAMPFREQ") << 4;
      fuse |= getFieldValue("ACTIVE") << 2;
      fuse |= getFieldValue("SLEEP");
      break;
    case 0x02:    // OSCCFG - Oscillator Configuration
      fuse |= getFieldValue("OSCLOCK") << 7;
      fuse |= getFieldValue("FREQSEL");
      break;
    case 0x04:    // Timer Counter Type D Configuration
      fuse |= getFieldValue("CMPDEN") << 7;
      fuse |= getFieldValue("CMPCEN") << 6;
      fuse |= getFieldValue("CMPBEN") << 5;
      fuse |= getFieldValue("CMPAEN") << 4;
      fuse |= getFieldValue("CMPD") << 3;
      fuse |= getFieldValue("CMPC") << 2;
      fuse |= getFieldValue("CMPB") << 1;
      fuse |= getFieldValue("CMPA");
      break;
    case 0x05:    // SYSCFG0 - System Configuration 0
      fuse |= getFieldValue("CRCSRC") << 6;
      fuse |= getFieldValue("TOUTDIS") << 4;
      fuse |= getFieldValue("RSTPINCFG") << 2;
      fuse |= getFieldValue("EESAVE");
      break;
    case 0x06:    // SYSCFG1 - System Configuration 1
      fuse |= getFieldValue("SUT");
      break;
    case 0x07:    // APPEND - Application Code End
      int appSize =  getFieldValue("APPSIZE");
      int bootSize =  getFieldValue("BOOTSIZE");
      if (appSize > 0) {
        return (byte) (bootSize + appSize);
      } else {
        return (byte) appSize;
      }
    case 0x08:    // BOOTEND - Boot End
      return (byte) getFieldValue("BOOTSIZE");
    case 0x0A:    // LOCKBIT - Lockbits
      return (byte) getFieldValue("LOCKBIT");
    default:
      throw new IllegalStateException("Invalid fuse offset " + offset);
    }
    return fuse;
  }

  FusePane (MegaTinyIDE.ChipInfo chip) {
    super(new GraphPaperLayout());
    addReservedBits(8);
    addField("WINDOW:4=OFF:0,8 CLK:1,16 CLK:2,32 CLK:3,64 CLK:4,128 CLK:5,256 CLK:6,512 CLK:7,1K CLK:8,2K CLK:9,4K CLK:10,8KC LK:11");
    addField("PERIOD:4=OFF:0,8 CLK:1,16 CLK:2,32 CLK:3,64 CLK:4,128 CLK:5,256 CLK:6,512 CLK:7,1K CLK:8,2K CLK:9,4K CLK:10,8KC LK:11");
    addField("LVL:3=1.8V:0,2.6V:2,4.2V:7");
    addField("SAMPFREQ:1=1 kHz:0,125 Hz:1");
    addField("ACTIVE:2=Disabled:0,Enabled:1,Sampled:2,Enabled with wake-up halted until BOD is ready:3");
    addField("SLEEP:2=Disabled:0,Enabled:1,Sampled:2");
    addField("OSCLOCK:1=20 MHz osc regs accessible:0,20 MHz osc regs locked:1");
    addReservedBits(5);
    addField("FREQSEL:2=16 MHz:1,20 MHz:2");
    addField("CMPDEN:1=Disabled:0,Enabled:1");
    addField("CMPCEN:1=Disabled:0,Enabled:1");
    addField("CMPBEN:1=Disabled:0,Enabled:1");
    addField("CMPAEN:1=Disabled:0,Enabled:1");
    addField("CMPD:1=Disabled:0,Enabled:1");
    addField("CMPC:1=Disabled:0,Enabled:1");
    addField("CMPB:1=Disabled:0,Enabled:1");
    addField("CMPA:1=Disabled:0,Enabled:1");
    addField("CRCSRC:2=All Flash:0,Boot:1,App and Boot,None:3");
    addReservedBits(1);
    addField("TOUTDIS:1=Enable NVM write block:0,Disable NVM write block:1");
    MyJComboBox tmp = addField("RSTPINCFG:2=GPIO:0,UPDI:1,RESET:2");
    tmp.addActionListener(ev -> {
      if (!disableChangeDetect && !"UPDI".equals(tmp.getSelectedItem())) {
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(Utility.class.getResource("images/warning-32x32.png")));
        showMessageDialog(tmp, "<html>Setting RSTPINCFG to anything other than \"UPDI\" will disable<br> the ability to edit fuses, " +
            "program and debug the target</html>", "Warning!", JOptionPane.PLAIN_MESSAGE, icon);
      }
    });
    addReservedBits(1);
    addField("EESAVE:1=Enable EEPROM erase:0,Disable EEPROM erase:1");
    addReservedBits(5);
    addField("SUT:3=0 ms:0,1 ms:1,2 ms:2,4 ms:3,8 ms:4,16 ms:5,32 ms:6,64 ms:7");
    int flashSize = chip.getInt("flash") * 1024 / 256;  // multiples of 256
    addField("APPSIZE:8=" + buildSelector(flashSize, 0));
    addField("BOOTSIZE:8=" + buildSelector(flashSize, 1));
  }

  private String buildSelector (int flashSize, int offset) {
    StringBuilder buf = new StringBuilder();
    for (int ii = 0; ii < flashSize; ii++) {
      if (ii > 0) {
        buf.append(",");
      }
      buf.append((ii + offset) * 256);
      buf.append(" bytes:");
      buf.append((ii + offset) % flashSize);
    }
    return buf.toString();
  }

  /**
   * Test Code for FusePane
   */
  public static void main (String[] args) {
    EventQueue.invokeLater(() -> {
      try {
        MegaTinyIDE.ChipInfo chip = MegaTinyIDE.chipTypes.get("attiny412");
        FusePane fusePane = new FusePane(chip);
        // Set reset data from attiny3217
        fusePane.setFuse(0x00, (byte) 0x00);   // WDTCFG
        fusePane.setFuse(0x01, (byte) 0x00);   // BODCFG
        fusePane.setFuse(0x02, (byte) 0x02);   // OSCCFG
        fusePane.setFuse(0x04, (byte) 0x00);   // TCD0CFG
        fusePane.setFuse(0x05, (byte) 0xF6);   // SYSCFG0
        fusePane.setFuse(0x06, (byte) 0x07);   // SYSCFG1
        fusePane.setFuse(0x07, (byte) 0x00);   // APPEND
        fusePane.setFuse(0x08, (byte) 0x00);   // BOOTEND
        fusePane.setFuse(0x0A, (byte) 0xC5);   // LOCKBIT

        if (JOptionPane.showConfirmDialog(null, fusePane, "FUSES for " + chip.name, OK_CANCEL_OPTION, PLAIN_MESSAGE) == 0) {
          System.out.println("exit");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}
