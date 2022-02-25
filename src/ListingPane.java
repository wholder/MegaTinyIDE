import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION;
import static javax.swing.border.TitledBorder.DEFAULT_POSITION;

public class ListingPane extends JPanel {   // https://regex101.com
  private static final Pattern        DBG_LINE = Pattern.compile("\\s?([0-9a-fA-F]+):(?:\\s(?:[0-9a-fA-F]{2})){2,4}\\s+([a-z]+)([^;]+)");
  private static final Pattern        VAR_LINE = Pattern.compile("([0-9a-fA-F]{8}).{9}(.+)\t([0-9a-fA-F]{8})\\s+(.*)");
  private static final int            FONT_SIZE = 12;
  private static final boolean        EDIT_STATUS = true;
  private static final Font           codeFont = Utility.getCodeFont(FONT_SIZE);
  private static final int            DEFAULT_R_MARGIN = 7;
  private static final int            DEFAULT_L_MARGIN = 5;
  private final static int            MAX_HEIGHT = Integer.MAX_VALUE - 1000000;
  private static final Color          FORE_COLOR  = new Color(51, 51, 51);
  private static final Color          OVAL_COLOR  = new Color(160, 160, 160);
  private static final Color          BACK_COLOR  = new Color(238, 238, 238);
  private static final Color          STATUS_BACK = new Color(221, 221, 221);
  private static final Color          CHANGED_COLOR = new Color(191, 196, 255);
  private static final boolean        SINGLE_BREAK = true;
  private final MyJTextPane           listingPane;
  private final MyJTextPane           messagePane;
  private final MySplitPane           split;
  private final BitSet                breakpoints = new BitSet();
  private final BitSet                breakLines = new BitSet();
  private final Set<Integer>          breakAddresses = new TreeSet<>();
  private final Map<Integer,Integer>  lineNumToAddress = new TreeMap<>();
  private final Map<Integer,Integer>  addressToLineNum = new HashMap<>();
  private final List<DebugListener>   debugListeners = new ArrayList<>();
  private final Preferences           prefs;
  StatusPane                          statusPane;
  boolean                             showStatusPane;
  MegaTinyIDE                         ide;
  private boolean                     hasSelection;
  private int                         sPos;
  private int                         ePos;
  private int                         lineCount;
  private boolean                     attached, running;
  private EDBG                        debugger;
  private String                      rawSrc = "";

  interface DebugListener {
    void debugState (boolean active);
  }

  interface NewVal {
    void newVal (int newVal);
  }
  public void addDebugListener (DebugListener debugListener) {
    debugListeners.add(debugListener);
  }

  static class MySplitPane extends JSplitPane {
    boolean opened = false;

    MySplitPane (int newOrientation) {
      super(newOrientation);
      SwingUtilities.invokeLater(() -> setDividerLocation(0.9999));
    }

    @Override
    public void setDividerLocation (int loc) {
      Dimension dim = getSize();
      if (dim.height - loc < 60) {
        loc = dim.height;
        opened = false;
      } else {
        opened = true;
      }
      super.setDividerLocation(loc);
    }
  }

  static class AvrTooltipHandler extends JToolTip {
    Map<String,Object>    avrIns;
    final JTextPane       comp;
    final TitledBorder    border;
    final String          style;

    AvrTooltipHandler (JComponent jComp) {
      setComponent(jComp);
      try {
        avrIns = JSONtoMap.parse(Utility.getFile("res:avrinstructions.jsn"));
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      ToolTipManager.sharedInstance().setDismissDelay(20000);
      setOpaque(true);
      comp = new JTextPane();
      comp.setContentType("text/html");
      Font tFont = jComp.getFont().deriveFont(22f);
      border = BorderFactory.createTitledBorder(null, "", DEFAULT_JUSTIFICATION, DEFAULT_POSITION, tFont);
      Border outside = BorderFactory.createEmptyBorder(4, 4, 4, 4);
      Border inside = BorderFactory.createEmptyBorder(0, 4, 4, 4);
      comp.setBorder(BorderFactory.createCompoundBorder(outside, BorderFactory.createCompoundBorder(border, inside)));
      Font iFont = new Font("Arial", Font.PLAIN, 18);
      comp.setFont(iFont);
      String fName = iFont.getFontName();
      int size = iFont.getSize();
      style = "font-family:" + fName + ";font-size:" + size + ";margin: 1em 0;display: block;";
    }

    public boolean isAvrInstruction (String ins) {
      return avrIns.containsKey(ins);
    }

    @Override
    public Dimension getPreferredSize () {
      Dimension dim = comp.getPreferredSize();
      return new Dimension((int) dim.getWidth(), (int) dim.getHeight());
    }

    @Override
    public void paintComponent (Graphics g) {
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Dimension d = comp.getPreferredSize();
      JPanel panel = new JPanel();
      SwingUtilities.paintComponent(g2, comp, panel, 0, 0, d.width, d.height);
    }

    @Override
    public void setTipText (String text) {
      if (text != null) {
        String[] tmp = text.split(":");
        text = tmp[0];
        border.setTitle(text);
        if (avrIns != null) {
          Object parms = avrIns.get(text.toUpperCase());
          if (parms instanceof List<?> && tmp.length > 1) {
            List<?> lst = (List<?>) parms;
            for (Object map : lst) {
              if (map instanceof Map<?,?>) {
                Object regx = ((Map<?, ?>) map).get("regx");
                if (regx instanceof String) {
                  if (tmp[1].matches((String) regx)) {
                    parms = map;
                    break;
                  }
                }
              }
            }
          }
          if (parms instanceof Map<?,?>) {
            Map<?,?> map = (Map<?,?>) parms;
            border.setTitle(text.toUpperCase() + " " + map.get("args"));
            StringBuilder buf = new StringBuilder("<html>");
            buf.append("<style>.desc{color:#444444;").append(style).append("}</style>");
            buf.append("<style>.ops{color:green;").append(style).append("}</style>");
            buf.append("<style>.flags{color:blue;").append(style).append("}</style>");
            buf.append("<style>.cycles{color:orange;").append(style).append("}</style>");
            buf.append("<p class=\"desc\">").append(map.get("desc")).append("</p>");
            Object ops = map.get("ops");
            if (ops instanceof String) {
              for (String op : ((String) ops).split(";")) {
                buf.append("<p class=\"ops\">").append(op).append("</p>");
              }
            }
            buf.append("<p class=\"flags\">Flags: ").append(map.get("flags")).append("</p>");
            buf.append("<p class=\"cycles\">Cycles: ").append(map.get("cycles")).append("</p>");
            buf.append("</html>");
            comp.setText(buf.toString());
          } else {
            comp.setText("Instruction lookup not available");
          }
        }
      }
    }
  }

  static class MyJTextPane extends JTextPane {
    // Allow horizontal scrollbar to appear
    @Override
    public boolean getScrollableTracksViewportWidth() {
      return false;
    }
  }

  class AvrListingPane extends MyJTextPane {
    private final AvrTooltipHandler tooltip = new AvrTooltipHandler(this);
    private String                  lastHit;
    private final int               lineHeight;

    AvrListingPane () {
      FontMetrics fontMetrics = getFontMetrics(codeFont);
      lineHeight = fontMetrics.getHeight();
    }

    @Override
    public JToolTip createToolTip () {
      return tooltip;
    }

    @Override
    public String getToolTipText (MouseEvent event) {
      Point point = event.getPoint();
      int lineNum = (int) point.getY() / lineHeight + 1;
      if (!lineNumToAddress.containsKey(lineNum)) {
        return null;
      }
      int offset = viewToModel(point);
      if (offset >= 0) {
        Document doc = getDocument();
        try {
          int start;
          if (Character.isAlphabetic(doc.getText(offset, 1).charAt(0)) ||
              (offset > 0 &&Character.isAlphabetic(doc.getText(offset - 1, 1).charAt(0)) )) {
            start = offset;
            while (start > 0 && Character.isAlphabetic(doc.getText(start - 1, 1).charAt(0))) {
              start--;
            }
            int end = offset;
            while (end < doc.getLength() && Character.isAlphabetic(doc.getText(end, 1).charAt(0))) {
              end++;
            }
            if (end > start) {
              String text = doc.getText(start, end - start);
              if (!text.equals(lastHit)) {
                int eol = end + 1;
                while (eol < doc.getLength() && doc.getText(eol, 1).charAt(0) != '\n' &&
                    doc.getText(eol, 1).charAt(0) != ';') {
                  eol++;
                }
                String remain = Utility.removeWhitespace(doc.getText(end, eol - end).trim());
                if (tooltip.isAvrInstruction(text.toUpperCase())) {
                  return text + ':' + remain;
                }
              }
              lastHit = text;
            }
          }
        } catch (BadLocationException ex) {
          ex.printStackTrace();
        }
      }
      return null;
    }
  }

  ListingPane (JTabbedPane tabs, String tabName, String hoverText, MegaTinyIDE ide, Preferences prefs) {
    this.ide = ide;
    this.prefs = prefs;
    prefs.addPreferenceChangeListener(evt -> {
      if ("decode_updi".equals(evt.getKey())) {
        // Disable RUN button when decode_updi is active (interferes with UPDI timing)
        boolean state = prefs.getBoolean("decode_updi", false);
        if (statusPane != null && attached) {
          statusPane.run.setEnabled(!state);
        }
      }
    });
    setLayout(new BorderLayout());
    listingPane = new AvrListingPane();
    listingPane.setToolTipText("");
    listingPane.setBorder(new EmptyBorder(-4, 5, 0, 0));
    Document doc = listingPane.getDocument();
    doc.putProperty(PlainDocument.tabSizeAttribute, 8);
    listingPane.setEditable(false);
    JScrollPane listingScroll = new JScrollPane(listingPane);
    listingScroll.setWheelScrollingEnabled(true);
    listingScroll.setRowHeaderView(new DebugRibbon());
    listingScroll.getVerticalScrollBar().setUnitIncrement(16);
    statusPane = new StatusPane();
    // Setup OCD Message Pane
    messagePane = new MyJTextPane();
    messagePane.setBorder(new EmptyBorder(0, 5, 0, 0));
    messagePane.setFont(codeFont);
    messagePane.setEditable(false);
    JScrollPane messageScroll = new JScrollPane(messagePane);
    messageScroll.setBorder(BorderFactory.createTitledBorder("OCD Messages"));
    // Setup JSplitPane
    split = new MySplitPane(JSplitPane.VERTICAL_SPLIT);
    split.add(listingScroll, JSplitPane.TOP);
    split.add(messageScroll, JSplitPane.BOTTOM);
    messagePane.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate (DocumentEvent e) {
        // Open OCD Message window if not already open
        if (!split.opened) {
          SwingUtilities.invokeLater(() -> split.setDividerLocation(0.9));
        }
      }

      public void removeUpdate (DocumentEvent e) { }

      public void changedUpdate (DocumentEvent e) { }
    });
    listingPane.addHyperlinkListener(ev -> {
      if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        String link = ev.getDescription();
        String [] parts = link.split(":");
        if (parts.length == 3 && "err".equals(parts[0])) {
          ide.selectTab(MegaTinyIDE.Tab.SRC);
          int line = Integer.parseInt(parts[1]);
          int column = Integer.parseInt(parts[2]);
          ide.codePane.setPosition(line, column);
        } else if (parts.length >= 4 && "var".equals(parts[0])) {
          if (debugger != null && attached && !running) {
            int add = Integer.parseInt(parts[2]);
            int len = Integer.parseInt(parts[3]);
            byte[] data = debugger.readSRam(add, len);
            HexEditPane hexPane = new HexEditPane(ide, 4, 8);
            hexPane.showVariable("SRAM", parts[1], add, data, new HexEditPane.Update() {
              @Override
              public void setValue (int offset, int value) throws EDBG.EDBGException {
                debugger.writeSRam(add + offset, new byte[] {(byte) value});
              }
            });
          } else {
            ide.showErrorDialog("Debugger must be attached and in stop mode to view variables!");
          }
        }
      }
    });
    // Build complete layout
    build();
    tabs.addTab(tabName, null, this, hoverText);
  }

  private void build  () {
    removeAll();
    if (showStatusPane) {
      add(statusPane, BorderLayout.NORTH);
    }
    statusPane.setActive(false);
    add(split, BorderLayout.CENTER);
    SwingUtilities.invokeLater(() -> split.setDividerLocation(0.9999));
  }

  void showStatusPane (boolean show) {
    showStatusPane = show;
    build();
    EventQueue.invokeLater(this::updateUI);
  }

  private int getDocumentPosition (int line) {
    int lineHeight = listingPane.getFontMetrics(listingPane.getFont()).getHeight();
    int y = line * lineHeight;
    Point pt = new Point(0, y);
    return listingPane.viewToModel(pt);
  }

  public void clearSelection () {
    if (hasSelection) {
      SwingUtilities.invokeLater(() -> {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setBackground(sas, listingPane.getBackground());
        StyledDocument doc = listingPane.getStyledDocument();
        doc.setCharacterAttributes(sPos, ePos - sPos, sas, false);
      });
      hasSelection = false;
    }
  }

  /**
   * Position the scrolling listing view so that lineNum is the top line in the view
   * @param lineNum line number (1-n)
   */
  public void gotoLine (int lineNum) {
    Container container = SwingUtilities.getAncestorOfClass(JViewport.class, listingPane);
    if (container != null) {
      SwingUtilities.invokeLater(() -> {
        JViewport viewport = (JViewport) container;
        int lineHeight = listingPane.getFontMetrics(listingPane.getFont()).getHeight();
        viewport.setViewPosition(new Point(0, lineHeight * Math.max(lineNum - 1, 0)));
      });
    }
  }

  /**
   * Compute the number of lines visible in the listing pane
   * @return line count
   */
  private int visibleLines () {
    Rectangle rect = listingPane.getVisibleRect();
    int lineHeight = listingPane.getFontMetrics(listingPane.getFont()).getHeight();
    return rect.height / lineHeight;
  }

  /**
   * Set text into debugPane and setup breakpoint controls
   * @param text listing
   */
  public void setText (String text) {
    rawSrc = text;
    String src = text.replaceAll("<", "&lt;");
    src = src.replaceAll(">", "&gt;");
    listingPane.setContentType("text/html");
    listingPane.setFont(codeFont);
    breakLines.clear();
    lineNumToAddress.clear();
    Map<String,String> vecs = null;
    int maxVector = 0;
    int addressSize = 2;
    if (prefs.getBoolean("vector_names", false)) {
      try {
        MegaTinyIDE.ChipInfo chip = MegaTinyIDE.getChipInfo(ide.getAvrChip());
        vecs = Utility.getResourceMap("interrupts/vec" + chip.get("vecs") + ".props");
        addressSize = chip.getInt("flash") >= 16 ? 4 : 2;
        for (String key : vecs.keySet()) {
          maxVector = Math.max(maxVector, Integer.parseInt(key));
        }
      } catch (IOException ex) {
        ide.showErrorDialog("Unable to load vector set");
      }
    }
    StringBuilder buf = new StringBuilder("<html><pre " + Utility.getFontStyle(codeFont) + ">");
    lineCount = 0;
    String[] lines = src.split("\n");
    boolean lastBlank = false;
    int lineNum = 1;
    for (String line : lines) {
      boolean isBlank = line.trim().length() == 0;
      if (isBlank && lastBlank) {
        continue;
      }
      lastBlank = isBlank;
      Matcher matcher;
      if ((matcher = DBG_LINE.matcher(line)).find()) {
        breakLines.set(lineNum);
        String hexAdd = matcher.group(1);
        int address = Integer.parseInt(hexAdd, 16);
        lineNumToAddress.put(lineNum, address);
        addressToLineNum.put(address, lineNum);
        if (vecs != null) {
          int vector = address / addressSize;
          if (vector <= maxVector) {
            String vecName = vecs.get(String.format("%02d", vector));
            if (vecName.length() > 0) {
              int idx = vecName.indexOf('[');
              if (idx >= 0) {
                vecName = vecName.substring(0, idx);
              }
            } else {
              vecName = "not used";
            }
            // Add vector name to end of line
            line = line + " - " + vecName;
          }
        }
      } else if ((matcher = VAR_LINE.matcher(line)).find()) {
        String type = matcher.group(2);
        int num = Integer.parseInt(matcher.group(3), 16);
        if (".bss".equals(type) && num > 0) {
          int add = Integer.parseInt(matcher.group(1).substring(4), 16);
          String name = matcher.group(4);
          int start = matcher.start(3);
          String prefix = line.substring(0, start);
          line = prefix + "sram variable: " + "<a href=\"var:" + name + ":" + add + ":" + num + "\">" + name +
              " (" + num + " byte" + (num > 1 ? "s" : "") + ")</a>";
        } else if (!prefs.getBoolean("symbol_table", true)) {
          continue;
        }
      } else {
        line = Utility.condenseTabs(line);
      }
      lineNum++;
      buf.append(line);
      lineCount++;
      buf.append("\n");
    }
    buf.append("</pre></html>");
    listingPane.setText(buf.toString());
    validate();
    SwingUtilities.invokeLater(this::repaint);
  }

  public String getText () {
    return rawSrc;
  }

  public void setErrorText (String text) {
    listingPane.setContentType("text/html");
    listingPane.setText(text);
  }

  public void highlightAddress (int address) {
    if (addressToLineNum.containsKey(address)) {
      int lineNum = addressToLineNum.get(address) + 1;
      clearSelection();
      // Position the scrolling listing view so that lineNum is in the center of the view
      gotoLine(lineNum - visibleLines() / 2);
      SwingUtilities.invokeLater(() -> {
        hasSelection = true;
        sPos = getDocumentPosition(lineNum - 1);
        ePos = getDocumentPosition(lineNum);
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setBackground(sas, OVAL_COLOR);
        StyledDocument doc = listingPane.getStyledDocument();
        doc.setCharacterAttributes(sPos, ePos - sPos, sas, false);
      });
    }
  }

  class StatusPane extends JPanel {
    private final Map<Integer,Integer> portMasks = new HashMap<>();
    private final HexPanel          portC, portB, portA;
    private final HexPanel          flags;
    private final HexPanel          regs;
    private final HexPanel          sRegs;
    private final JButton           attach, run;
    private final JButton           step = new JButton("STEP");
    private final JButton           reset = new JButton("RESET");
    private Thread                  runThread;
    private int                     portMask;
    private byte                    vPrtA = 0, vPrtB = 0, vPrtC = 0;
    private byte                    vDirA = 0, vDirB = 0, vDirC = 0;

    {                                   //      Port C              Port B              Port A
      // |7|6|5|4|3|2|1|0|   |7|6|5|4|3|2|1|0|   |7|6|5|4|3|2|1|0|
      portMasks.put( 8, 0x0000CF);      //  - - - - - - - -     - - - - - - - -     x x - - x x x x
      portMasks.put(14, 0x000FFF);      //  - - - - - - - -     - - - - x x x x     x x x x x x x x
      portMasks.put(20, 0x0F3FFF);      //  - - - - x x x x     - - x x x x x x     x x x x x x x x
      portMasks.put(24, 0x3FFFFF);      //  - - x x x x x x     x x x x x x x x     x x x x x x x x
      // Note: 'x' indicates usable pins
    }

    public void setActive (boolean active) {
      if (active) {
        String avrChip = ide.getAvrChip();
        if (avrChip != null) {
          MegaTinyIDE.ChipInfo info = MegaTinyIDE.getChipInfo(avrChip);
          int pins = info.getInt("pins");
          portMask = portMasks.get(pins);
          portC.setActiveMask((portMask >> 16) & 0xFF);
          portB.setActiveMask((portMask >> 8) & 0xFF);
          portA.setActiveMask(portMask & 0xFF);
          try {
            debugger = ide.getDebugger(false);
            debugger.resetTarget();
            debugger.setOcdListener(text -> {
              if (running) {
                Document doc = messagePane.getDocument();
                try {
                  doc.insertString(doc.getLength(), text, null);
                  messagePane.setCaretPosition(messagePane.getCaretPosition() + text.length());
                  repaint();
                } catch (Exception ex) {
                  ex.printStackTrace();
                }
              }
            });
          } catch (Exception ex) {
            ide.showErrorDialog("Unable to open Programmer:\n  " + ex.getMessage());
            debugger = null;
            active = false;
            running = false;
          }
        } else {
          ide.showErrorDialog("Target device type not selected");
          active = false;
        }
      } else {
        if (running) {
          if (runThread != null && runThread.isAlive()) {
            try {
              runThread.interrupt();
              runThread.join(200);
            } catch (InterruptedException ex) {
              // do nothing
            }
          }
          running = false;
        }
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
      run.setEnabled(active && !ide.decodeUpdi());
      step.setEnabled(active && !running);
      reset.setEnabled(active && !running);
      if (active & !running) {
        updateState(false);
      }
      updateUI();
      for (DebugListener debugListener : debugListeners) {
        debugListener.debugState(active);
      }
      if (active) {
        ide.appendToInfoPane(String.format("Debugger Attached (%d bps, Vcc = %1.2f volts)\n", EDBG.UPDIClock * 1000, debugger.targetVoltage()));
      } else {
        ide.appendToInfoPane("Debugger Detached\n");
      }
    }

    class HexPanel extends JPanel {
      private final TitledBorder        titleBorder;
      private final List<JLabel>        lbls = new ArrayList<>();
      private final List<HexTextfield>  vals = new ArrayList<>();
      private int                       activeBits = 0xFFFFFFFF;

      class HexTextfield extends JTextField {
        private final Border  activeBorder = BorderFactory.createLineBorder(Color.black, 1);
        private final Border  inactiveBorder = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1);
        private final String  format;
        private int           value;
        private NewVal        valueChange;
        private boolean       enabled;

        HexTextfield (int width, boolean canEdit) {
          setEditable(false);
          format = "%0" + width + "X";
          setColumns(width);
          setHorizontalAlignment(CENTER);
          setEnabled(false);
          setText(String.format(format, 0));
          if (EDIT_STATUS && canEdit) {
            // Add listener to edit qualified fields
            addMouseListener(new MouseAdapter() {
              @Override
              public void mouseClicked (MouseEvent ev) {
                if (SwingUtilities.isRightMouseButton(ev) && isEnabled()) {
                  if (debugger != null && attached && !running && valueChange != null) {
                    setToolTipText(null);
                    repaint();
                    JPanel panel = new JPanel();
                    panel.setLayout(new FlowLayout());
                    JLabel lbl = new JLabel("New Value:");
                    panel.add(lbl);
                    JTextField field = new JTextField(width);
                    field.setText(String.format(format, 0));
                    field.setEnabled(true);
                    field.setEditable(true);
                    field.setHorizontalAlignment(SwingConstants.CENTER);
                    panel.add(field);
                    JButton okBtn = new JButton("OK");
                    okBtn.setPreferredSize(new Dimension(30, 20));
                    panel.add(okBtn);
                    JButton exitBtn = new JButton("X");
                    exitBtn.setForeground(Color.red);
                    exitBtn.setPreferredSize(new Dimension(20, 20));
                    panel.add(exitBtn);
                    JDialog popup = new JDialog(ide);
                    okBtn.addMouseListener(new MouseAdapter() {
                      @Override
                      public void mousePressed (MouseEvent ev) {
                        String val = field.getText();
                        try {
                          int nVal = Integer.parseInt(val, 16);
                          valueChange.newVal(nVal);
                          popup.dispose();
                        } catch (Exception ex) {
                          ImageIcon icon = new ImageIcon(Utility.class.getResource("images/warning-32x32.png"));
                          showMessageDialog(popup, "Must be hexadecimal", "Invalid value", JOptionPane.PLAIN_MESSAGE, icon);
                        }
                      }
                    });
                    exitBtn.addMouseListener(new MouseAdapter() {
                      @Override
                      public void mousePressed (MouseEvent ev) {
                        popup.dispose();
                      }
                    });
                    popup.setUndecorated(true);
                    popup.setModal(true);
                    popup.setLayout(new BorderLayout());
                    popup.getContentPane().add(panel, BorderLayout.CENTER);
                    popup.setLocationRelativeTo(HexTextfield.this);
                    Point temp = popup.getLocation();
                    popup.setLocation(temp.x + 20, temp.y - 20);
                    field.requestFocusInWindow();
                    field.selectAll();
                    popup.pack();
                    popup.setVisible(true);
                  } else {
                    ImageIcon icon = new ImageIcon(Utility.class.getResource("images/warning-32x32.png"));
                    showMessageDialog(HexTextfield.this, "Debugger must be attached and in stop mode", "Error", JOptionPane.PLAIN_MESSAGE, icon);
                  }
                }
              }
            });
          }
        }

        void addValueHandler (NewVal valueChange) {
          this.valueChange = valueChange;
        }

        public void setValue (int value, boolean showChange) {
          setBackground(showChange && value != this.value ? CHANGED_COLOR : Color.white);
          this.value = value;
          setText(String.format(format, value));
        }

        public void setDir (boolean state) {
          if (enabled) {
            setToolTipText(state ? "OUTPUT" : "INPUT");
          } else {
            setToolTipText("");
          }
        }

        @Override
        public void setEnabled (boolean enabled) {
          super.setEnabled(this.enabled = enabled);
          setBorder(Utility.getBorder(enabled ? activeBorder : inactiveBorder, 2, 1, 2, 0));
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
          vals.get(ii).setEnabled(active && enabled);
        }
        titleBorder.setTitleColor(attached ? Color.BLACK : Color.GRAY);
      }

      class BinTextfield extends HexTextfield {
        BinTextfield (boolean canEdit) {
          super(1, canEdit);
        }
      }

      HexPanel (String title, int rows, int cols, String[] fieldLabels, String[] tooltips, int fieldWidth, boolean canEdit) {
        super(new GridLayout(rows, 2 * cols));
        setBackground(STATUS_BACK);
        titleBorder = BorderFactory.createTitledBorder(title);
        // Padding                              ot ol ob or it il ib ir
        setBorder(Utility.getBorder(titleBorder, 2, 2, 0, 2, 1, 3, 0, 3));
        for (int row = 0; row < rows; row++) {
          for (int col = 0; col < cols; col++) {
            int idx = col + row * cols;
            JLabel lbl;
            if (fieldLabels != null) {
              add(lbl = new JLabel(fieldLabels[idx]));
              lbl.setFont(codeFont);
            } else {
              add(lbl = new JLabel(Integer.toString(idx)));
            }
            lbls.add(lbl);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setVerticalAlignment(SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(11.0f));
            lbl.setBackground(Color.white);
            lbl.setOpaque(true);
            HexTextfield field = fieldWidth > 1 ? new HexTextfield(fieldWidth, canEdit) : new BinTextfield(canEdit);
            if (EDIT_STATUS && canEdit) {
              int index = row * 16 + col;
              field.addValueHandler(newVal -> {
                try {
                  //System.out.printf("%s: %d , %d = 0x%02X\n", title, index, fieldWidth, newVal);
                  if ("Registers".equals(title)) {
                    debugger.writeSRam(index, new byte[] {(byte) newVal});
                    regs.setValue(index, newVal, true);
                    switch (index) {
                    case 26:
                      sRegs.setValue(2, (sRegs.getValue(2) & 0xFF00) + (newVal & 0xFF), true);
                      break;
                    case 27:
                      sRegs.setValue(2, (sRegs.getValue(2) & 0xFF) + (newVal << 8), true);
                      break;
                    case 28:
                      sRegs.setValue(3, (sRegs.getValue(2) & 0xFF00) + (newVal & 0xFF), true);
                      break;
                    case 29:
                      sRegs.setValue(3, (sRegs.getValue(2) & 0xFF) + (newVal << 8), true);
                      break;
                    case 30:
                      sRegs.setValue(4, (sRegs.getValue(2) & 0xFF00) + (newVal & 0xFF), true);
                      break;
                    case 31:
                      sRegs.setValue(4, (sRegs.getValue(2) & 0xFF) + (newVal << 8), true);
                      break;
                    }
                  } else if ("Special Registers".equals(title)) {
                    switch (index) {
                    case 0:   // Program Counter
                      debugger.setProgramCounter(newVal);
                      break;
                    case 1:   // Stack Pointer
                      debugger.writeStackPointer(newVal);
                      break;
                    case 2:   // X Register (regs 27:26)
                      debugger.writeSRam(26, new byte[] {Utility.lsb(newVal), Utility.msb(newVal)});
                      regs.setValue(26, Utility.lsb(newVal), true);
                      regs.setValue(27, Utility.msb(newVal), true);
                      break;
                    case 3:   // Y Register (regs 29:28)
                      debugger.writeSRam(28, new byte[] {Utility.lsb(newVal), Utility.msb(newVal)});
                      regs.setValue(28, Utility.lsb(newVal), true);
                      regs.setValue(29, Utility.msb(newVal), true);
                      break;
                    case 4:   // Z Register (regs 31:30)
                      debugger.writeSRam(30, new byte[] {Utility.lsb(newVal), Utility.msb(newVal)});
                      regs.setValue(30, Utility.lsb(newVal), true);
                      regs.setValue(31, Utility.msb(newVal), true);
                      break;
                    }
                    sRegs.setValue(index, newVal, true);
                  } else if ("Flags".equals(title)) {
                    int oldVal = debugger.getStatusRegister();
                    int bit = 1 << (7 - index);
                    int tmp = (oldVal & ~bit) | (newVal << (7 - index));
                    debugger.writeStatusRegister((byte) tmp);
                    setValue(index, newVal, true);
                  }
                } catch (Exception ex) {
                  ex.printStackTrace();
                }
              });
            }
            String tooltip = tooltips != null ? tooltips[idx] : null;
            if (tooltip != null) {
              if ("-".equals(tooltip)) {
                field.setEnabled(false);
                field.setToolTipText("reserved");
              } else {
                if (EDIT_STATUS && canEdit) {
                  field.setToolTipText("<html>" + tooltip + "<br>(Right click to edit value)</html>");
                } else {
                  field.setToolTipText("<html>" + tooltip + "</html>");
                }
              }
            }
            field.setFont(codeFont);
            vals.add(field);
            field.setBackground(Color.white);
            field.setOpaque(true);
            add(field);
          }
        }
      }

      public void setValue (int index, int value, boolean showChange) {
        HexPanel.HexTextfield fld = vals.get(index);
        fld.setValue(value,  showChange);
      }

      public void setDir (int index, boolean state) {
        HexPanel.HexTextfield fld = vals.get(index);
        fld.setDir(state);
      }

      public int getValue (int index) {
        return vals.get(index).value;
      }
    }

    public StatusPane () {
      setLayout(new BorderLayout());
      // Build Ports Status Display
      String[] portLabels = new String[] {"7", "6", "5", "4", "3", "2", "1", "0"};
      JPanel ports = new JPanel(new GridLayout(1,3));
      ports.add(portC = new HexPanel("PORTC.IN", 1, 8, portLabels, null, 1, false));
      ports.add(portB = new HexPanel("PORTB.IN", 1, 8, portLabels, null, 1, false));
      ports.add(portA = new HexPanel("PORTA.IN", 1, 8, portLabels, null, 1, false));
      // Build Flags Status Display
      String[] flagLabels = new String[] {"I", "T", "H", "S", "V", "N", "Z", "C"};
      String[] flagNames = new String[] {"Global Interrupt Enable", "Copy Storage", "Half Carry", "Sign",
                                         "Two's Compliment Overflow", "Negative", "Zero", "Carry"};
      flags = new HexPanel("Flags", 1, 8, flagLabels, flagNames, 1, true);
      // Build Registers Status Display
      regs = new HexPanel("Registers", 2, 16, null, null, 2, true);
      String[] sRegLabels = new String[] {"PC", "SP", "X", "Y", "Z"};
      String[] sRegNames = new String[] {"Program Counter", "Stack Pointer", "X Register (regs 27:26)",
                                         "Y Register (regs 29:28)", "Z Register (regs 31:30)"};
      sRegs = new HexPanel("Special Registers", 1, 5, sRegLabels, sRegNames, 4, true);
      JPanel top = new JPanel(new BorderLayout());
      top.add(ports, BorderLayout.NORTH);
      top.add(regs, BorderLayout.CENTER);
      JPanel top2 = new JPanel(new GridLayout(1, 2));
      top2.add(flags);
      top2.add(sRegs);
      top.add(top2, BorderLayout.SOUTH);
      top.setBorder(Utility.getBorder(BorderFactory.createLineBorder(Color.BLACK, 1), 1, 1, 1, 1));
      add(top, BorderLayout.CENTER);
      JPanel buttons = new JPanel(new GridLayout(1, 4));
      buttons.setBorder(Utility.getBorder(BorderFactory.createLineBorder(Color.BLACK, 1), 1, 1, 1, 1));
      buttons.add(attach = new JButton("ATTACH"));
      attach.addActionListener((ActionEvent ev) -> setActive(attached = !attached));
      buttons.add(run = new JButton("RUN"));
      run.addActionListener(ev -> {
        if (debugger != null) {
          if (running) {
            if (runThread != null && runThread.isAlive()) {
              run.setEnabled(false);
              try {
                runThread.interrupt();
                runThread.join(200);
              } catch (InterruptedException ex) {
                // do nothing
              }
            }
          } else {
            int[] breakpoints = breakAddresses.stream().mapToInt(Number::intValue).toArray();
            if (breakpoints.length == 0 || breakpoints.length == 1) {
              runThread = new Thread(() -> {
                running = true;
                run.setText("STOP");
                run.setEnabled(attached);
                step.setEnabled(false);
                reset.setEnabled(false);
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
                run.setText("RUN");
                run.setEnabled(attached);
                step.setEnabled(true);
                reset.setEnabled(true);
                SwingUtilities.invokeLater(() -> updateState(true));
                runThread = null;
              });
              runThread.start();
            } else {
              // Note: can only get here if DebugPane.SINGLE_BREAK == false
              ide.showErrorDialog("Multiple breakpoints not supported");
            }
          }
        }
      });
      buttons.add(step);
      step.addActionListener(ev -> {
        if (debugger != null) {
          debugger.stepTarget();
          updateState(true);
        }
      });
      buttons.add(reset);
      reset.addActionListener(ev -> {
        if (debugger != null) {
          if (running && runThread != null && runThread.isAlive()) {
            try {
              runThread.interrupt();
              runThread.join(200);
            } catch (InterruptedException ex) {
              // do nothing
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
        if (debugger != null) {
          byte[] regs = debugger.readRegisters(0, 32);
          int pc = debugger.getProgramCounter();
          highlightAddress(pc);
          byte flags = debugger.getStatusRegister();
          int sp = debugger.getStackPointer();
          //                             Offset = 8          Offset = 0
          // PORTA Base = 0x0400, PORTA.IN = 0x0408, PORTA.DIR = 0x0400
          // PORTB Base = 0x0420, PORTB.IN = 0x0428, PORTB.DIR = 0x0420
          // PORTC Base = 0x0440, PORTC.IN = 0x0448, PORTC.DIR = 0x0440
          //
          // PORTA.PINnCTRL Base = 0x0410 + pin number (set bit 3 to 1 to enable pullup, bits 0-3 for input/sense config)
          // PORTB.PINnCTRL Base = 0x0430 + pin number
          // PORTC.PINnCTRL Base = 0x0450 + pin number
          //
          boolean portAUsed = (portMask & 0xFF) != 0;
          if (portAUsed) {
            vPrtA = debugger.readSRam(0x0408, 1)[0];         // PORTA.IN
            vDirA = debugger.readSRam(0x0400, 1)[0];         // PORTA.DIR
          }
          boolean portBUsed = (portMask & 0xFF00) != 0;
          if (portBUsed) {
            vPrtB = debugger.readSRam(0x0428, 1)[0];         // PORTB.IN
            vDirB = debugger.readSRam(0x0420, 1)[0];         // PORTB.DIR
          }
          boolean portCUsed = (portMask & 0xFF0000) != 0;
          if (portCUsed) {
            vPrtC = debugger.readSRam(0x0448, 1)[0];         // PORTC.IN
            vDirC = debugger.readSRam(0x0440, 1)[0];         // PORTC.DIR
          }
          SwingUtilities.invokeLater(() -> {
            setRegs(regs, showChange);
            setPC(pc, showChange);
            setBits(this.flags, flags, showChange);
            setSP(sp, showChange);
            if (portAUsed) {
              setBits(portA, vPrtA, showChange);
              setDir(portA, vDirA);
            }
            if (portBUsed) {
              setBits(portB, vPrtB, showChange);
              setDir(portB, vDirB);
            }
            if (portCUsed) {
              setBits(portC, vPrtC, showChange);
              setDir(portC, vDirC);
            }
          });
        }
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

    public void setDir (HexPanel field, byte val) {
      for (int ii = 0; ii < 8; ii++) {
        byte mask = (byte) (1 << (7 - ii));
        boolean bit = (val & mask) != 0;
        field.setDir(ii, bit);
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
    DebugRibbon () {
      setForeground(FORE_COLOR);
      setBackground(BACK_COLOR);
      setBorder(BorderFactory.createEmptyBorder(0, DEFAULT_L_MARGIN, 0, DEFAULT_R_MARGIN));
      Insets insets = getInsets();
      FontMetrics fontMetrics = listingPane.getFontMetrics(codeFont);
      int width = insets.left + insets.right + fontMetrics.getAscent();
      Dimension dim = new Dimension(width, MAX_HEIGHT);
      setPreferredSize(dim);
      setSize(dim);
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked (MouseEvent ev) {
          super.mouseClicked(ev);
          if (SwingUtilities.isLeftMouseButton(ev)) {
            int lineHeight = fontMetrics.getHeight();
            int line = ev.getY() / lineHeight + 1;
            if (SINGLE_BREAK) {
              breakAddresses.clear();
              if (breakpoints.get(line)) {
                breakpoints.clear();
              } else if (lineNumToAddress.containsKey(line)) {
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
      //Document doc = listingPane.getDocument();
      int maxLines = lineCount - 1;// doc.getDefaultRootElement().getElementIndex(doc.getLength() - 1);
      Rectangle clip = g.getClip().getBounds();
      int lineHeight = listingPane.getFontMetrics(listingPane.getFont()).getHeight();
      int topLine = (int) (clip.getY() / lineHeight);
      int bottomLine = Math.min(maxLines, (int) (clip.getHeight() + lineHeight - 1) / lineHeight + topLine + 1);
      // Draw available and active breakpoint controls
      g2.setStroke(new BasicStroke(1.3f));
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

