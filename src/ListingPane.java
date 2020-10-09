import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListingPane extends JScrollPane {
  private static final Pattern        DEBUG_LINE = Pattern.compile("\\s+([0-9a-fA-F]+):\\s[0-9a-fA-F]{2}\\s[0-9a-fA-F]{2}\\s");
  private static final int            FONT_SIZE = 12;
  private static final int            DEFAULT_R_MARGIN = 7;
  private static final int            DEFAULT_L_MARGIN = 5;
  private final static int            MAX_HEIGHT = Integer.MAX_VALUE - 1000000;
  private static final Color          FORE_COLOR  = new Color(51, 51, 51);
  private static final Color          OVAL_COLOR  = new Color(160, 160, 160);
  private static final Color          BACK_COLOR  = new Color(238, 238, 238);
  private static final Color          STATUS_BACK = new Color(221, 221, 221);
  private static final Color          CHANGED_COLOR = new Color(191, 196, 255);
  private static final boolean        SINGLE_BREAK = true;
  private final JTextPane             debugPane;
  private final FontMetrics           fontMetrics;
  private final BitSet                breakpoints = new BitSet();
  private final BitSet                breakLines = new BitSet();
  private final Set<Integer>          breakAddresses = new TreeSet<>();
  private final Map<Integer,Integer>  lineNumToAddress = new TreeMap<>();
  private final Map<Integer,Integer>  addressToLineNum = new HashMap<>();
  private final List<DebugListener>   debugListeners = new ArrayList<>();
  private final int                   lineHeight;
  StatusPane                          statusPane;
  JPanel                              outerPane;
  boolean                             showStatusPane;
  MegaTinyIDE                         ide;
  private boolean                     hasSelection;
  private int                         sPos;
  private int                         ePos;

  interface DebugListener {
    void debugState (boolean active);
  }

  public void addDebugListener (DebugListener debugListener) {
    debugListeners.add(debugListener);
  }

  ListingPane (JTabbedPane tabs, String tabName, String hoverText, MegaTinyIDE ide) {
    getVerticalScrollBar().setUnitIncrement(16);
    this.ide = ide;
    debugPane = new JTextPane();
    // Kludge needed to allow horizontal scrolling, which is needed to keep breakpoints on proper lines
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(ListingPane.this.debugPane);
    ListingPane.this.debugPane.setBorder(new EmptyBorder(0, 5, 0, 0));
    boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
    Font font = new Font(windows ? "Consolas" : "Menlo", Font.PLAIN, 12);
    fontMetrics = getFontMetrics(font);
    lineHeight = ListingPane.this.fontMetrics.getHeight();
    Document doc = ListingPane.this.debugPane.getDocument();
    doc.putProperty(PlainDocument.tabSizeAttribute, 4);
    setViewportView(panel);
    setRowHeaderView(new DebugRibbon());
    ListingPane.this.debugPane.setFont(font);
    ListingPane.this.debugPane.setEditable(false);
    outerPane = new JPanel(new BorderLayout());
    statusPane = new StatusPane();
    build();
    tabs.addTab(tabName, null, outerPane, hoverText);
  }

  private void build  () {
    outerPane.removeAll();
    if (showStatusPane) {
      outerPane.add(statusPane, BorderLayout.NORTH);
    }
    statusPane.setActive(false);
    JPanel list = new JPanel(new BorderLayout());
    list.add(this, BorderLayout.CENTER);
    outerPane.add(list, BorderLayout.CENTER);
  }

  void showStatusPane (boolean show) {
    showStatusPane = show;
    build();
    EventQueue.invokeLater(outerPane::updateUI);
  }

  public JTextPane getEditPane () {
    return debugPane;
  }

  private int getDocumentPosition (int line) {
    int lineHeight = debugPane.getFontMetrics(debugPane.getFont()).getHeight();
    int y = line * lineHeight;
    Point pt = new Point(0, y);
    return debugPane.viewToModel(pt);
  }

  public void clearSelection () {
    if (hasSelection) {
      SwingUtilities.invokeLater(() -> {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setBackground(sas, debugPane.getBackground());
        StyledDocument doc = debugPane.getStyledDocument();
        doc.setCharacterAttributes(sPos, ePos - sPos, sas, false);
      });
      hasSelection = false;
    }
  }

  public void selectLine (int lineNum) {
    clearSelection();
    SwingUtilities.invokeLater(() -> {
      hasSelection = true;
      sPos = getDocumentPosition(lineNum - 1);
      ePos = getDocumentPosition(lineNum);
      SimpleAttributeSet sas = new SimpleAttributeSet();
      StyleConstants.setBackground(sas, OVAL_COLOR);
      StyledDocument doc = debugPane.getStyledDocument();
      doc.setCharacterAttributes(sPos, ePos - sPos, sas, false);
    });
  }

  public void gotoLine (int lineNum) {
    Container container = SwingUtilities.getAncestorOfClass(JViewport.class, debugPane);
    if (container != null) {
      SwingUtilities.invokeLater(() -> {
        JViewport viewport = (JViewport) container;
        viewport.setViewPosition(new Point(0, lineHeight * (lineNum - 1)));
      });
    }
  }

  /**
   * Set test into debugPane and setup breakpoint controls
   * @param list listing
   */
  public void setText (String list) {
    debugPane.setContentType("text/lst");
    breakLines.clear();
    lineNumToAddress.clear();
    StringBuilder buf = new StringBuilder();
    String[] lines = list.split("\n");
    boolean lastBlank = false;
    int lineNum = 1;
    for (String line : lines) {
      boolean isBlank = line.trim().length() == 0;
      if (isBlank && lastBlank) {
        continue;
      }
      lastBlank = isBlank;
      Matcher mat = DEBUG_LINE.matcher(line);
      if (mat.find()) {
        breakLines.set(lineNum);
        String hexAdd = mat.group(1);
        int address = Integer.parseInt(hexAdd, 16);
        lineNumToAddress.put(lineNum, address);
        addressToLineNum.put(address, lineNum);
      } else {
        // 	SET_BIT(VPORTA.DIR, 3);							// Set bit 3 in PORTA (pin 7) to output
        StringBuilder tmp = new StringBuilder();
        boolean lastTab = false;
        for (char cc : line.toCharArray()) {
          boolean isTab = cc == '\t';
          if (isTab && lastTab) {
            continue;
          }
          tmp.append(cc);
          lastTab = isTab;
        }
        line = tmp.toString();
      }
      lineNum++;
      buf.append(line);
      buf.append("\n");
    }
    debugPane.setText(buf.toString());
  }

  public String getText () {
    return debugPane.getText();
  }

  public void setErrorText (String test) {
    debugPane.setContentType("text/html");
    debugPane.setText(test);
  }

  public void highlightAddress (int address) {
    if (addressToLineNum.containsKey(address)) {
      selectLine(addressToLineNum.get(address));
    }
  }

  class StatusPane extends JPanel {
    private final Map<Integer,Integer> portMasks = new HashMap<>();
    private final HexPanel          portC, portB, portA;
    private final HexPanel          flags;
    private final HexPanel          regs;
    private final HexPanel          sRegs;
    private final JButton           attach, run, stop, step, reset;
    private EDBG                    debugger;
    private boolean                 active, running;
    private Thread                  runThread;
    private int                     portMask;

    {
      portMasks.put( 8, 0x0000CF);
      portMasks.put(14, 0x000FFF);
      portMasks.put(20, 0x0F3FFF);
      portMasks.put(24, 0x3FFFFF);
    }

    public void setActive (boolean active) {
      if (active) {
        String avrChip = ide.getAvrChip();
        if (avrChip != null) {
          MegaTinyIDE.ChipInfo info = ide.getChipInfo(avrChip);
          int pins = info.getInt("pins");
          portMask = portMasks.get(pins);
          portC.setActiveMask((portMask >> 16) & 0xFF);
          portB.setActiveMask((portMask >> 8) & 0xFF);
          portA.setActiveMask(portMask & 0xFF);
          EDBG.Programmer prog = EDBG.getProgrammer(ide.getProgrammer());
          if (prog != null) {
            try {
              debugger = new EDBG(prog, info, false);
              debugger.resetTarget();
            } catch (Exception ex) {
              ex.printStackTrace();
              debugger = null;
              active = false;
              running = false;
            }
          } else {
            ide.showErrorDialog("Programmer not available");
            active = false;
          }
        } else {
          ide.showErrorDialog("Target device type not selected");
          active = false;
        }
      } else {
        if (debugger != null){
          debugger.close();
          debugger = null;
        }
        clearSelection();
      }
      attach.setText(active ? "DETACH" : "ATTACH");
      portC.setEnabled(active);
      portB.setEnabled(active);
      portA.setEnabled(active);
      flags.setEnabled(active);
      regs.setEnabled(active);
      sRegs.setEnabled(active);
      run.setEnabled(active && !running);
      stop.setEnabled(active && running);
      step.setEnabled(active && !running);
      reset.setEnabled(active && !running);
      if (active & !running) {
        updateState(false);
      }
      updateUI();
      for (DebugListener debugListener : debugListeners) {
        debugListener.debugState(active);
      }
    }

    private void setRunningState () {
      run.setEnabled(!running);
      stop.setEnabled(running);
      step.setEnabled(!running);
    }

    class HexPanel extends JPanel {
      private final Border              border = Utility.getBorder(BorderFactory.createLineBorder(Color.gray, 1), 2, 1, 2, 0);
      private final List<JLabel>        lbls = new ArrayList<>();
      private final List<HexTextfield>  vals = new ArrayList<>();
      private int                       activeBits = 0xFFFFFFFF;

      class HexTextfield extends JTextField {
        private final String  format;
        private int           value;

        HexTextfield (int width) {
          setEditable(false);
          format = "%0" + width + "X";
          setColumns(width);
          setHorizontalAlignment(CENTER);
          setBorder(border);
          setText(String.format(format, 0));
        }

        public void setValue (int value, boolean showChange) {
          setBackground(showChange && value != this.value ? CHANGED_COLOR : Color.white);
          this.value = value;
          setText(String.format(format, value));
        }
      }

      // Set mask which determine which bits can be set active by setEnabled()
      public void setActiveMask (int active) {
        activeBits = active;
      }

      @Override
      public void setEnabled (boolean enabled) {
        for (int ii = 0; ii < lbls.size(); ii++) {
          boolean active = (activeBits & (1 << (lbls.size() - 1 - ii))) != 0;
          lbls.get(ii).setEnabled(active && enabled);
        }
        for (int ii = 0; ii < vals.size(); ii++) {
          boolean active = (activeBits & (1 << (lbls.size() - 1 - ii))) != 0;
          vals.get(ii).setEnabled(active && enabled);
        }
      }

      class BinTextfield extends HexTextfield {
        BinTextfield () {
          super(1);
        }
      }

      HexPanel (String title, int rows, int cols, String[] fieldLabels, String[] tooltips, int fieldWidth) {
        super(new GridLayout(rows, 2 * cols));
        setBackground(STATUS_BACK);
        // Padding                                                          ot ol ob or it il ib ir
        setBorder(Utility.getBorder(BorderFactory.createTitledBorder(title), 2, 2, 0, 2, 1, 3, 0, 3));
        Font font = Utility.getCodeFont(FONT_SIZE);
        for (int row = 0; row < rows; row++) {
          for (int col = 0; col < cols; col++) {
            int idx = col + row * cols;
            JLabel lbl;
            if (fieldLabels != null) {
              add(lbl = new JLabel(fieldLabels[idx]));
              lbl.setFont(font);
            } else {
              add(lbl = new JLabel(Integer.toString(idx)));
            }
            lbls.add(lbl);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setVerticalAlignment(SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(11.0f));
            lbl.setBackground(Color.white);
            lbl.setOpaque(true);
            HexTextfield val = fieldWidth > 1 ? new HexTextfield(fieldWidth) : new BinTextfield();
            String tooltip = tooltips != null ? tooltips[idx] : null;
            if (tooltip != null) {
              if ("-".equals(tooltip)) {
                val.setEnabled(false);
                val.setToolTipText("reserved");
              } else {
                val.setToolTipText(tooltip);
              }
            }
            val.setFont(font);
            vals.add(val);
            val.setBackground(Color.white);
            val.setOpaque(true);
            add(val);
          }
        }
      }

      public void setValue (int index, int value, boolean showChange) {
        HexPanel.HexTextfield fld = vals.get(index);
        fld.setValue(value,  showChange);
      }
    }

    public StatusPane () {
      setLayout(new BorderLayout());
      // Build Ports Status Display
      String[] portLabels = new String[] {"7", "6", "5", "4", "3", "2", "1", "0"};
      JPanel ports = new JPanel(new GridLayout(1,3));
      ports.add(portC = new HexPanel("PORTC.IN", 1, 8, portLabels, null, 1));
      ports.add(portB = new HexPanel("PORTB.IN", 1, 8, portLabels, null, 1));
      ports.add(portA = new HexPanel("PORTA.IN", 1, 8, portLabels, null, 1));
      // Build Flags Status Display
      String[] flagLabels = new String[] {"I", "T", "H", "S", "V", "N", "Z", "C"};
      String[] flagNames = new String[] {"Global Interrupt Enable", "Copy Storage", "Half Carry", "Sign",
                                         "Two's Compliment Overflow", "Negative", "Zero", "Carry"};
      flags = new HexPanel("Flags", 1, 8, flagLabels, flagNames, 1);
      // Build Registers Status Display
      regs = new HexPanel("Registers", 2, 16, null, null, 2);
      String[] sRegLabels = new String[] {"PC", "SP", "X", "Y", "Z"};
      String[] sRegNames = new String[] {"Program Counter", "Stack Pointer", "X Register (regs 27:26)",
                                         "Y Register (regs 29:28)", "Z Register (regs 31:30)"};
      sRegs = new HexPanel("Special Registers", 1, 5, sRegLabels, sRegNames, 4);
      JPanel top = new JPanel(new BorderLayout());
      top.add(ports, BorderLayout.NORTH);
      top.add(regs, BorderLayout.CENTER);
      JPanel top2 = new JPanel(new GridLayout(1, 2));
      top2.add(flags);
      top2.add(sRegs);
      top.add(top2, BorderLayout.SOUTH);
      top.setBorder(Utility.getBorder(BorderFactory.createLineBorder(Color.BLACK, 1), 1, 1, 1, 1));
      add(top, BorderLayout.CENTER);
      JPanel buttons = new JPanel(new GridLayout(1, 5));
      buttons.setBorder(Utility.getBorder(BorderFactory.createLineBorder(Color.BLACK, 1), 1, 1, 1, 1));
      buttons.add(attach = new JButton("ATTACH"));
      attach.addActionListener(ev -> setActive(active = ! active));
      buttons.add(run = new JButton("RUN"));
      run.addActionListener(ev -> {
        if (debugger != null) {
          int[] breakpoints = breakAddresses.stream().mapToInt(Number::intValue).toArray();
          if (breakpoints.length == 0 || breakpoints.length == 1) {
            runThread = new Thread(() -> {
              running = true;
              setRunningState();
              try {
                if (breakpoints.length == 1) {
                  int address = breakpoints[0];
                  debugger.runToAddress(address);
                } else {
                  debugger.runTarget();
                }
              } catch (InterruptedException ex) {
                try {
                  debugger.stopTarget();
                } catch (InterruptedException ex2) {
                  ex2.printStackTrace();
                }
              }
              running = false;
              setRunningState();
              SwingUtilities.invokeLater(() -> updateState(true));
              runThread = null;
            });
            runThread.start();
          } else {
            // Note: can only get here if DebugPane.SINGLE_BREAK == false
            ide.showErrorDialog("Multiple breakpoints not supported");
          }
        }
      });
      buttons.add(stop = new JButton("STOP"));
      stop.addActionListener(ev -> {
        if (debugger != null) {
          if (running) {
            if (runThread != null && runThread.isAlive()) {
              runThread.interrupt();
            }
          }
        }
      });
      buttons.add(step = new JButton("STEP"));
      step.addActionListener(ev -> {
        if (debugger != null) {
          debugger.stepTarget();
          updateState(true);
        }
      });
      buttons.add(reset = new JButton("RESET"));
      reset.addActionListener(ev -> {
        if (debugger != null) {
          if (running) {
            if (runThread != null && runThread.isAlive()) {
              runThread.interrupt();
            }
          }
          debugger.resetTarget();
          updateState(false);
        }
      });
      setActive(false);
      add(buttons, BorderLayout.SOUTH);
    }

    public void updateState (boolean showChange) {
      try {
        byte[] regs = debugger.readRegisters(0, 32);
        int pc = debugger.getProgramCounter();
        highlightAddress(pc);
        byte flags = debugger.getStatusRegister();
        int sp = debugger.getStackPointer();
        boolean portAUsed = (portMask & 0xFF) != 0;
        byte prta = portAUsed ? debugger.readSRam(0x0002, 1)[0] : 0;      // PORTA.IN
        boolean portBUsed = (portMask & 0xFF00) != 0;
        byte prtb = portBUsed ? debugger.readSRam(0x0006, 1)[0] : 0;      // PORTB.IN
        boolean portCUsed = (portMask & 0xFF0000) != 0;
        byte prtc = portCUsed ? debugger.readSRam(0x000A, 1)[0] : 0;      // PORTC.IN
        SwingUtilities.invokeLater(() -> {
          setRegs(regs, showChange);
          setPC(pc, showChange);
          setBits(this.flags, flags, showChange);
          setSP(sp, showChange);
          if (portAUsed) {
            setBits(portA, prta, showChange);
          }
          if (portBUsed) {
            setBits(portB, prtb, showChange);
          }
          if (portCUsed) {
            setBits(portC, prtc, showChange);
          }
        });
      } catch (EDBG.EDBGException ex) {
        ex.printStackTrace();
        ide.showErrorDialog(ex.getMessage());
        debugger.close();
        debugger = null;
      }
    }

    public void setRegs (byte[] vals, boolean showChange) {
      for (int ii = 0; ii < vals.length; ii++) {
        regs.setValue(ii, (int) vals[ii] & 0xFF, showChange);
      }
      // Update X, Y and Z values
      sRegs.setValue(2, ((int) vals[26] & 0xFF) + (((int) vals[27] & 0xFF)<< 8), showChange);
      sRegs.setValue(3, ((int) vals[28] & 0xFF) + (((int) vals[29] & 0xFF)<< 8), showChange);
      sRegs.setValue(4, ((int) vals[30] & 0xFF) + (((int) vals[31] & 0xFF)<< 8), showChange);
    }

    public void setBits (HexPanel field, byte val, boolean showChange) {
      for (int ii = 0; ii < 8; ii++) {
        byte mask = (byte) (1 << (7 - ii));
        boolean bit = (val & mask) != 0;
        field.setValue(ii, bit ? 1 : 0, showChange);
      }
    }

    public void setPC (int val, boolean showChange) {
      sRegs.setValue(0, val, showChange);
    }

    public void setSP (int val, boolean showChange) {
      sRegs.setValue(1, val, showChange);
    }
  }

  private class DebugRibbon extends JPanel {
    private DebugRibbon () {
      setForeground(FORE_COLOR);
      setBackground(BACK_COLOR);
      setBorder(BorderFactory.createEmptyBorder(0, DEFAULT_L_MARGIN, 0, DEFAULT_R_MARGIN));
      Insets insets = getInsets();
      int width = insets.left + insets.right + fontMetrics.getAscent();
      Dimension dim = new Dimension(width, MAX_HEIGHT);
      setPreferredSize(dim);
      setSize(dim);
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked (MouseEvent ev) {
          super.mouseClicked(ev);
          if (SwingUtilities.isLeftMouseButton(ev)) {
            int line = ev.getY() / lineHeight + 1;
            if (SINGLE_BREAK) {
              breakAddresses.clear();
              if (breakpoints.get(line)) {
                breakpoints.clear();
              } else {
                breakpoints.clear();
                breakpoints.set(line);
                breakAddresses.add(lineNumToAddress.get(line));
              }
            } else {
              if (breakLines.get(line)) {
                breakpoints.flip(line);
                if (breakpoints.get(line)) {
                  breakAddresses.add(lineNumToAddress.get(line));
                } else {
                  breakAddresses.remove(lineNumToAddress.get(line));
                }
              }
            }
            repaint();
          } else if (SwingUtilities.isRightMouseButton(ev)) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Clear All Breakpoints");
            menuItem.setEnabled(breakAddresses.size() > 0);
            menuItem.addActionListener(e -> {
              breakpoints.clear();
              breakAddresses.clear();
              repaint();
            });
            popup.add(menuItem);
            popup.show(ev.getComponent(), ev.getX(), ev.getY());
          }
        }
      });
    }

    /**
     * Draw the Breakpoint controls
     */
    @Override
    public void paintComponent (Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHints(hints);
      Insets insets = getInsets();
      int maxLines = debugPane.getDocument().getDefaultRootElement().getElementIndex(debugPane.getDocument().getLength() - 1);
      Rectangle clip = g.getClip().getBounds();
      int topLine = (int) (clip.getY() / lineHeight);
      int bottomLine = Math.min(maxLines, (int) (clip.getHeight() + lineHeight - 1) / lineHeight + topLine + 1);
      // Draw available and active breakpoint controls
      for (int line = topLine; line <= bottomLine; line++) {
        int lineNum = line + 1;
        int yLoc = line * lineHeight + insets.top;
        if (breakLines.get(lineNum)) {
          Ellipse2D oval = new Ellipse2D.Float(insets.left, yLoc + 2, lineHeight - 4, lineHeight - 4);
          if (breakpoints.get(lineNum)) {
            g2.setColor(FORE_COLOR);
            g2.fill(oval);
          } else {
            g2.setColor(OVAL_COLOR);
            g2.draw(oval);
          }
        }
      }
    }
  }
}

