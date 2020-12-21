import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.net.URI;
import java.net.URL;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;

import jssc.SerialNativeInterface;
import jssc.SerialPort;

import static javax.swing.JOptionPane.*;

/**
   *  An IDE for tinyAVR® 1-series and 0-series Microcontrollers
   *  Author: Wayne Holder, 2011-2019
   *  License: MIT (https://opensource.org/licenses/MIT)
   */

public class MegaTinyIDE extends JFrame implements ListingPane.DebugListener {
  private static final String     VERSION_URL = "https://raw.githubusercontent.com/wholder/MegaTinyIDE/master/resources/version.props";
  private static final String     DOWNLOAD = "https://github.com/wholder/MegaTinyIDE/blob/master/out/artifacts/MegaTinyIDE_jar/MegaTinyIDE.jar";
  private static final String     fileSep =  System.getProperty("file.separator");
  private static String           tempBase = System.getProperty("java.io.tmpdir");
  private static final Font       tFont = Utility.getCodeFont(12);
  private static final int        cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  private static final KeyStroke  OPEN_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_O, cmdMask) ;
  private static final KeyStroke  SAVE_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_S, cmdMask) ;
  private static final KeyStroke  QUIT_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_Q, cmdMask) ;
  private static final KeyStroke  BUILD_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_B, cmdMask) ;
  private static final KeyStroke  DEBUG_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_D, cmdMask) ;
  static Map<String,ChipInfo>     chipTypes = new LinkedHashMap<>();
  static Map<String,ChipInfo>     chipSignatures = new LinkedHashMap<>();
  public enum                     Tab {DOC(0), SRC(1), LIST(2), HEX(3), INFO(4);
                                         final int num; Tab(int num) {this.num = num;}}
  private final String            osName = System.getProperty("os.name").toLowerCase();
  private enum                    OpSys {MAC, WIN, LINUX}
  private OpSys                   os;
  private String                  osCode;
  private final JTabbedPane       tabPane;
  public final CodeEditPane       codePane;
  private ListingPane             listPane;
  private MyTextPane              hexPane;
  private final MyTextPane        infoPane;
  private final JMenuItem         openMenu;
  private JMenuItem               saveMenu;
  private final JMenuItem         saveAsMenu;
  private final JMenuItem         newMenu;
  private final RadioMenu         targetMenu;
  private final JMenuItem         build;
  private final JMenuItem         progFlash;
  private final JMenuItem         readFuses;
  private final JMenuItem         idTarget;
  private final JMenu             progMenu;
  private final Preferences       prefs = Preferences.userRoot().node(this.getClass().getName());
  private final JSSCPort          jPort = new JSSCPort(prefs);
  private String                  tmpDir, tmpExe;
  private String                  progVidPid;
  private String                  avrChip;
  private String                  editFile;
  private boolean                 directHex, compiled, codeDirty, showDebugger;
  private File                    cFile;
  private Map<String, String>     compileMap;
  private Map<String, String>     versionInfo;

  {
    try {
      tempBase = tempBase.endsWith(fileSep) ? tempBase : tempBase + fileSep;
      if (osName.contains("win")) {
        os = OpSys.WIN;
        osCode = "w";
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } else if (osName.contains("mac")) {
        os = OpSys.MAC;
        osCode = "m";
      } else if (osName.contains("linux")) {
        os = OpSys.LINUX;
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        osCode = "l";
      } else {
        showErrorDialog("Unsupported os: " + osName);
        System.exit(1);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // Implement DebugListener
  public void debugState (boolean active) {
    // Disable these actions when debugger is attached to target
    build.setEnabled(!active);
    progFlash.setEnabled(!active);
    readFuses.setEnabled(!active);
    idTarget.setEnabled(!active);
    progMenu.setEnabled(!active);
  }

  public boolean decodeUpdi () {
    return prefs.getBoolean("decode_updi", false);
  }

  public JSSCPort getSerialPort () {
    return jPort;
  }

  public void infoPrintln (String msg) {
    infoPane.append(msg + "\n");
  }

  public String getAvrChip () {
    return avrChip;
  }

  public static ChipInfo getChipInfo (String name) {
    return chipTypes.get(name);
  }

  public EDBG.Programmer getSelectedProgrammer () {
    return EDBG.getProgrammer(progVidPid);
  }

  static class MyTextPane extends JEditorPane {
    MyTextPane (JTabbedPane tabs, String tabName, String hoverText) {
      setContentType("text/plain");
      setBorder(new EmptyBorder(0, 5, 0, 0));
      setFont(tFont);
      //ta.setTabSize(4);
      JScrollPane scroll = new JScrollPane(this);
      tabs.addTab(tabName, null, scroll, hoverText);
      setEditable(false);
    }

    @Override
    public void setText (String text) {
      setContentType("text/plain");
      setFont(tFont);
      super.setText(text);
    }

    void append (String text) {
      Document doc = getDocument();
      try {
        doc.insertString(doc.getLength(), text, null);
        repaint();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  static class RadioMenu extends JMenu {
    RadioMenu (String name) {
      super(name);
    }

    void setSelected (String name) {
      for (int ii = 0; ii < getItemCount(); ii++) {
        JMenuItem item = getItem(ii);
        if (item != null && name.equals(item.getText())) {
          item.setSelected(true);
          return;
        }
      }
    }

    String getSelected () {
      for (int ii = 0; ii < getItemCount(); ii++) {
        JMenuItem item = getItem(ii);
        if (item != null && item.isSelected()) {
          return item.getText();
        }
      }
      return null;
    }
  }

  public static class ChipInfo implements Comparable<ChipInfo> {
    private final PropertyMap.ParmSet parms;
    public final String               name, variant, series, sram, flash, eeprom, signature;
    public final int                  pins;

    ChipInfo (String name, PropertyMap.ParmSet parms) {
      this.name = name;
      this.parms = parms;
      variant = parms.get("variant");
      signature = parms.get("sig");
      series = parms.get("series");
      sram = parms.get("sram");
      flash = parms.get("flash");
      eeprom = parms.get("eeprom");
      pins = parms.getInt("pins");
    }

    public String get (String key) {
      return parms.get(key);
    }

    public int getInt (String key) {
      return parms.getInt(key);
    }

    public String getInfo () {
      return "series: " + series + ", signature: " + signature + ", flash: " + flash + "K, sram: " + sram + " bytes, eeprom: " +
          eeprom + " bytes";
    }

    // negative == less, zero = equals, positive = greater (than the specified object)
    public int compareTo (ChipInfo obj) {
      if (pins == obj.pins) {
        if (name.length() == obj.name.length()) {
          return this.name.compareTo(obj.name);
        } else {
          return name.length() - obj.name.length();
        }
      } else {
        return pins - obj.pins;
      }
    }
  }

  static {
    try {
      PropertyMap pMap = new PropertyMap("attinys.props");
      List<ChipInfo> chips = new ArrayList<>();
      for (String key : pMap.keySet()) {
        PropertyMap.ParmSet tiny = pMap.get(key);
        chips.add(new ChipInfo(key, tiny));
      }
      Collections.sort(chips);
      for (ChipInfo chip : chips) {
        chipTypes.put(chip.name, chip);
        chipSignatures.put(chip.get("sig"), chip);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  {
    prefs.putBoolean("gen_prototypes", prefs.getBoolean("gen_prototypes", false));
    prefs.putBoolean("interleave", prefs.getBoolean("interleave", false));
    prefs.putBoolean("symbol_table", prefs.getBoolean("symbol_table", false));
    prefs.putBoolean("vector_names", prefs.getBoolean("vector_names", true));
    prefs.putBoolean("enable_preprocessing", prefs.getBoolean("enable_preprocessing", false));
    prefs.putBoolean("developer_features", prefs.getBoolean("developer_features", false));
    prefs.putBoolean("decode_updi", prefs.getBoolean("decode_updi", false));
    prefs.putBoolean("show_dependencies", prefs.getBoolean("show_dependencies", false));
  }

  private JFileChooser getFileChooser () {
    JFileChooser fc = new JFileChooser();
    String fPath = prefs.get("default.dir", "/");
    int selIndex = tabPane.getSelectedIndex();
    if (selIndex == Tab.SRC.num) {
      FileNameExtensionFilter[] filters = {
          new FileNameExtensionFilter("AVR .c , .cpp or .ino files", "c", "cpp", "ino"),
          new FileNameExtensionFilter("AVR .asm or .s files", "asm", "s"),
          };
      String ext = prefs.get("default.extension", "c");
      for (FileNameExtensionFilter filter : filters) {
        fc.addChoosableFileFilter(filter);
        if (filter.getExtensions()[0].equals(ext)) {
          fc.setFileFilter(filter);
        }
      }
    } else if (selIndex == Tab.HEX.num) {
      FileNameExtensionFilter filter = new FileNameExtensionFilter(".hex files", "hex");
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter);
      int idx = fPath.lastIndexOf('.');
      if (idx > 0) {
        fPath = fPath.substring(0, idx + 1) + "hex";
      }
    } else if (selIndex == Tab.LIST.num) {
      FileNameExtensionFilter filter = new FileNameExtensionFilter(".lst files", "lst");
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter);
      int idx = fPath.lastIndexOf('.');
      if (idx > 0) {
        fPath = fPath.substring(0, idx + 1) + "lst";
      }
    }
    fc.setSelectedFile(new File(fPath));
    fc.setAcceptAllFileFilterUsed(true);
    fc.setMultiSelectionEnabled(false);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    return fc;
  }

  public void selectTab (Tab tab) {
    tabPane.setSelectedIndex(tab.num);
  }

  private void setDirtyIndicator (boolean dirty) {
    this.setTitle("MegaTinyIDE: " + (editFile != null ? editFile : "") + (dirty ? " [unsaved]" : ""));
  }

  private void showAboutBox () {
    ImageIcon icon = null;
    try {
      icon = new ImageIcon(getClass().getResource("images/MegaTinyIDE.png"));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    String version = versionInfo.get("version") + " " + versionInfo.get("status");
    showMessageDialog(this,
      "By: Wayne Holder\n" +
        "java.io.tmpdir: " + tempBase + "\n" +
        "tmpDir: " + tmpDir + "\n" +
        "tmpExe: " + tmpExe + "\n" +
        "Java Version: " + System.getProperty("java.version") + "\n" +
        "Java Simple Serial Connector: " + SerialNativeInterface.getLibraryVersion() + "\n" +
        "JSSC Native Code DLL Version: " + SerialNativeInterface.getNativeLibraryVersion() + "\n",
        "MegaTinyIDE " + version, INFORMATION_MESSAGE,  icon);
  }

  private void showPreferences (int modifiers) {
    List<ParmDialog.Item> items = new ArrayList<>();
    items.add(new ParmDialog.Item("Generate Prototypes (Experimental)", "*[GEN_PROTOS]*", "gen_prototypes", true));
    items.add(new ParmDialog.Item("Interleave Source and ASM", "*[INTERLEAVE]*", "interleave", true));
    items.add(new ParmDialog.Item("Add Vector Names in Listing", "*[VECNAMES]*", "vector_names", true));
    items.add(new ParmDialog.Item("Include Full Symbol Table in Listing", "*[SYMTABLE]*", "symbol_table", false));
    boolean devFeatures = (modifiers & InputEvent.CTRL_MASK) != 0;
    if (devFeatures) {
      items.add(new ParmDialog.Item("Enable Preprocessing (Developer)", "*[PREPROCESS]*", "enable_preprocessing", false));
      items.add(new ParmDialog.Item("Enable Developer Features", "*[DEV_ONLY]*", "developer_features", false));
      items.add(new ParmDialog.Item("Decode UPDI Commands", "*[UPDI_DECODE]*", "decode_updi", false));
      items.add(new ParmDialog.Item("Show Dependencies", "*[SHOW_DEPENDENCIES]*", "show_dependencies", false));
    }
    ParmDialog.Item[] parmSet = items.toArray(new ParmDialog.Item[0]);
    ParmDialog dialog = (new ParmDialog("Edit Preferences", parmSet, new String[] {"Save", "Cancel"}));
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);              // Note: this call invokes dialog
  }

  private MegaTinyIDE () {
    super("MegaTinyIDE");
    // Setup temp directory for code compilation and toolchain
    try {
      tmpDir = Utility.createDir(tempBase + "avr-temp-code");
      tmpExe = Utility.createDir(tempBase + "avr-toolchain");
    } catch (IOException ex) {
      showErrorDialog("Unable to create temporary working directories");
      System.exit(1);
    }
    // Load version info
    try {
      versionInfo = Utility.getResourceMap("version.props");
    } catch (IOException ex) {
      showErrorDialog("Unable to load version.props");
      ex.printStackTrace();
      System.exit(0);
    }
    // Setup interface
    setBackground(Color.white);
    setLayout(new BorderLayout(1, 1));
    // Create Tabbed Pane
    tabPane = new JTabbedPane();
    add("Center", tabPane);
    codePane = new CodeEditPane(prefs);
    codePane.setCodeChangeListener(() -> {
      setDirtyIndicator(codeDirty = true);
      updateChip(codePane.getText());
      compiled = false;
      listPane.setForeground(Color.red);
      hexPane.setForeground(Color.red);
    });
    MarkupView howToPane = new MarkupView("documentation/index.md", null);
    tabPane.addTab("How To", null, howToPane, "This is the documentation page");
    tabPane.addTab("Source Code", null, codePane, "This is the editor pane where you enter source code");
    listPane = new ListingPane(tabPane, "Listing", "Select this pane to view the assembler listing", this, prefs);
    listPane.addDebugListener(this);
    hexPane =  new MyTextPane(tabPane, "Hex Output", "Intel Hex Output file for programmer");
    infoPane = new MyTextPane(tabPane, "Monitor", "Displays additional information about IDE and OCD and error messages");
    hexPane.setEditable(false);
    infoPane.setEditable(false);
    infoPane.append("os.name:      " + osName + "\n");
    infoPane.append("os:           " + os.toString() + "\n");
    infoPane.append("java.home:    " + System.getProperty("java.home") + "\n");
    infoPane.append("java.version: " + System.getProperty("java.version") + "\n");

    if (prefs.getBoolean("developer_features", false)) {
      String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
      infoPane.append("Installed fonts:\n");
      for (String font : fonts) {
        infoPane.append("  " + font + "\n");
      }
    }
    infoPane.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked (MouseEvent ev) {
        super.mouseClicked(ev);
        if (SwingUtilities.isRightMouseButton(ev)) {
          JPopupMenu popup = new JPopupMenu();
          JMenuItem menuItem = new JMenuItem("Clear Screen");
          menuItem.addActionListener(e -> SwingUtilities.invokeLater(() -> infoPane.setText("")));
          popup.add(menuItem);
          popup.show(ev.getComponent(), ev.getX(), ev.getY());
        }
      }
    });
    infoPane.setToolTipText("<html>Click Right Mouse Button<br>for Command Menu</html>");
    // Add menu bar and menus
    JMenuBar menuBar = new JMenuBar();
    // Add "File" Menu
    JMenu fileMenu = new JMenu("File");
    JMenuItem mItem;
    fileMenu.add(mItem = new JMenuItem("About"));
    mItem.addActionListener(e -> showAboutBox());
    fileMenu.add(mItem = new JMenuItem("Preferences"));
    mItem.addActionListener(e -> showPreferences(e.getModifiers()));
    // Add "Check for Updates" Menu item
    fileMenu.add(mItem = new JMenuItem("Check for Updates"));
    mItem.addActionListener(ev -> {
      // Check for new version available
      // https://github.com/wholder/MegaTinyIDE/blob/master/resources/version.props
      try {
        Map<String,String> latest = Utility.getResourceMap(new URL(VERSION_URL));
        String oldVersion = versionInfo.get("version");
        String newVersion = latest.get("version");
        if (oldVersion != null && newVersion != null) {
          String[] oldParts = oldVersion.split("\\.");
          String[] newParts = newVersion.split("\\.");
          if (oldParts.length == newParts.length) {
            try {
              for (int ii = 0; ii < newParts.length; ii++) {
                int newVal = Integer.parseInt(newParts[ii]);
                int oldVal = Integer.parseInt(oldParts[ii]);
                if (newVal > oldVal) {
                  String status = latest.get("status");
                  String version = newVersion + (status != null && status.length() > 0 ? " " + status : "");
                  ImageIcon icon = new ImageIcon(Utility.class.getResource("images/info-32x32.png"));
                  if (JOptionPane.showConfirmDialog(this, "<html>A new version (" + version + ") is available!<br>" +
                                                        "Do you want to go to the download page?</html>", "Warning", JOptionPane.YES_NO_OPTION,
                                                    JOptionPane.WARNING_MESSAGE, icon) == JOptionPane.OK_OPTION) {
                    if (Desktop.isDesktopSupported()) {
                      try {
                        Desktop.getDesktop().browse(new URI(DOWNLOAD));
                      } catch (Exception ex) {
                        ex.printStackTrace();
                      }
                    }
                  }
                  return;
                } else if (newVal < oldVal) {
                  showErrorDialog("This version is newer than the github release.");
                  return;
                }
              }
              ImageIcon icon = new ImageIcon(Utility.class.getResource("images/info-32x32.png"));
              JOptionPane.showMessageDialog(this, "You have the latest version.", "Attention", INFORMATION_MESSAGE, icon);
            } catch (NumberFormatException ex) {
              ex.printStackTrace();
            }
          } else {
            showErrorDialog("New version numbering in use.  Please update");
          }
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    });
    fileMenu.addSeparator();
    fileMenu.add(newMenu = new JMenuItem("New"));
    newMenu.addActionListener(e -> {
      if (codePane.getText().length() == 0 || discardChanges()) {
        codePane.setForeground(Color.black);
        codePane.setCode("");
        directHex = false;
        compiled = false;
        editFile = null;
        setDirtyIndicator(codeDirty = false);
        selectTab(Tab.SRC);
        cFile = null;
      }
    });
    fileMenu.add(openMenu = new JMenuItem("Open"));
    openMenu.setAccelerator(OPEN_KEY);
    openMenu.addActionListener(e -> {
      JFileChooser fc = getFileChooser();
      if (!codeDirty || discardChanges()) {
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          try {
            File oFile = fc.getSelectedFile();
            FileFilter filter = fc.getFileFilter();
            if (filter instanceof FileNameExtensionFilter) {
              String[] exts = ((FileNameExtensionFilter) filter).getExtensions();
              prefs.put("default.extension",  exts[0]);
            }
            String src = Utility.getFile(oFile);
            cFile = oFile;
            codePane.setForeground(Color.black);
            String[] tmp = Utility.decodeMarkdown(src);
            codePane.setCode(tmp[0]);
            if (tmp.length > 1) {
              codePane.setMarkup(tmp[1]);
            }
            prefs.put("default.dir", editFile = oFile.getAbsolutePath());
            setDirtyIndicator(codeDirty = false);
            directHex = false;
            selectTab(Tab.SRC);
            saveMenu.setEnabled(true);
            prefs.put("default.dir", oFile.getAbsolutePath());
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    });
    fileMenu.add(saveMenu = new JMenuItem("Save"));
    saveMenu.setAccelerator(SAVE_KEY);
    saveMenu.setEnabled(false);
    saveMenu.addActionListener(e -> {
      Utility.saveFile(cFile, codePane.getText());
      setDirtyIndicator(codeDirty = false);
    });
    fileMenu.add(saveAsMenu = new JMenuItem("Save As..."));
    saveAsMenu.addActionListener(e -> {
      JFileChooser fc = getFileChooser();
      if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File sFile = fc.getSelectedFile();
        FileFilter filter = fc.getFileFilter();
        if (filter instanceof FileNameExtensionFilter) {
          String[] exts = ((FileNameExtensionFilter) filter).getExtensions();
          prefs.put("default.extension",  exts[0]);
        }
        if (sFile.exists() && !doWarningDialog("Overwrite Existing file?")) {
          return;
        }
        int selIndex = tabPane.getSelectedIndex();
        if (selIndex == Tab.SRC.num) {
          Utility.saveFile(sFile, codePane.getText());
        } else if (selIndex == Tab.HEX.num) {
          Utility.saveFile(sFile, hexPane.getText());
        } else if (selIndex == Tab.LIST.num) {
          Utility.saveFile(sFile, listPane.getText());
        }
        cFile = sFile;
        prefs.put("default.dir", editFile = sFile.getAbsolutePath());
        setDirtyIndicator(codeDirty = false);
        saveMenu.setEnabled(true);
      }
    });
    fileMenu.addSeparator();
    fileMenu.add(mItem = new JMenuItem("Quit MegaTinyIDE"));
    mItem.setAccelerator(QUIT_KEY);
    mItem.addActionListener(e -> {
      if (!codeDirty  ||  discardChanges()) {
        System.exit(0);
      }
    });
    menuBar.add(fileMenu);
    // Add "Edit" Menu
    JMenu editMenu = codePane.getEditMenu();
    editMenu.setEnabled(false);
    menuBar.add(editMenu);
    tabPane.addChangeListener(ev -> {
      int idx = tabPane.getSelectedIndex();
      editMenu.setEnabled(idx == Tab.SRC.num);
      openMenu.setEnabled(idx == Tab.SRC.num || idx == Tab.DOC.num);
      saveMenu.setEnabled(idx == Tab.SRC.num);
      saveAsMenu.setEnabled(idx == Tab.SRC.num || idx == Tab.HEX.num || idx == Tab.LIST.num);
      newMenu.setEnabled(idx == Tab.SRC.num);
    });
    // Add "Actions" Menu
    JMenu actions = new JMenu("Actions");
    actions.add(build = new JMenuItem("Build"));
    build.setToolTipText("Complile Code in Source Code Pane and Display Result in Listing and Hex Output Panes");
    build.setAccelerator(BUILD_KEY);
    build.addActionListener(e -> {
      if (cFile != null) {
        String fName = cFile.getName().toLowerCase();
        // Reinstall toolchain if there was an error last time we tried to build
        verifyToolchain();
        Thread cThread = new Thread(() -> {
          try {
            listPane.setForeground(Color.black);
            listPane.setText("");
            Map<String,String> tags = new HashMap<>();
            tags.put("TDIR", tmpDir);
            tags.put("TEXE", tmpExe);
            tags.put("IDIR", tmpExe + "avr" + fileSep + "include" + fileSep);
            tags.put("FNAME", fName);
            tags.put("EFILE", editFile);
            if (prefs.getBoolean("gen_prototypes", false)) {
              tags.put("PREPROCESS", "GENPROTOS");
            }
            compileMap = MegaTinyCompiler.compile(codePane.getText(), tags, prefs, this);
            if (compileMap == null) {
              return;
            }
            String compName = "Sketch.cpp";
            String trueName = cFile.getName();
            if (compileMap.containsKey("ERR")) {
              listPane.setForeground(Color.red);
              // Remove path to tmpDir from error messages
              String errText = compileMap.get("ERR").replace(tmpDir + compName, trueName);
              errText = errText.replaceAll("<", "&lt;");
              errText = errText.replaceAll(">", "&gt;");
              errText = errText.replace("\n", "<br>");
              Pattern lineRef = Pattern.compile("(" + trueName + ":([0-9]+?:[0-9]+?):) (fatal error|error|note):");
              Matcher mat = lineRef.matcher(errText);
              Font font = Utility.getCodeFont(12);
              StringBuffer buf = new StringBuffer("<html><pre " + Utility.getFontStyle(font) + ">");
              while (mat.find()) {
                String seq = mat.group(1);
                if (seq != null) {
                  mat.appendReplacement(buf, "<a href=\"err:" + mat.group(2) +  "\">" + seq + "</a>");
                }
              }
              mat.appendTail(buf);
              buf.append("</pre></html>");
              listPane.setErrorText(buf.toString());
              compiled = false;
            } else {
              listPane.setForeground(Color.black);
              StringBuilder tmp = new StringBuilder();
              tmp.append(compileMap.get("INFO"));
              tmp.append("\n\n");
              if (compileMap.containsKey("WARN")) {
                tmp.append(compileMap.get("WARN"));
                tmp.append("\n\n");
              }
              tmp.append( compileMap.get("SIZE"));
              tmp.append(compileMap.get("LST"));
              String listing = tmp.toString();
              listPane.setText(listing.replace(tmpDir, ""));
              hexPane.setForeground(Color.black);
              hexPane.setText(compileMap.get("HEX"));
              avrChip = compileMap.get("CHIP");
              compiled = true;
              listPane.statusPane.setActive(false);
            }
            selectTab(Tab.LIST);
          } catch (Exception ex) {
            prefs.putBoolean("reload_toolchain", true);
            ex.printStackTrace();
            listPane.setText("Compile error (see Error Info pane for details)\n" + ex.toString());
            infoPane.append("Stack Trace:\n");
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            PrintStream pOut = new PrintStream(bOut);
            ex.printStackTrace(pOut);
            pOut.close();
            infoPane.append(bOut.toString() + "\n");
            selectTab(Tab.INFO);
          }
        });
        cThread.start();
      } else {
        showErrorDialog("Please save file first!");
      }
    });
    JMenuItem preprocess = new JMenuItem("Run Preprocessor");
    preprocess.setToolTipText("Run GCC Propressor and Display Result in Listing Pane");
    actions.add(preprocess);
    prefs.addPreferenceChangeListener(evt -> preprocess.setVisible(prefs.getBoolean("enable_preprocessing", false)));
    preprocess.addActionListener(e -> {
      if (cFile != null) {
        String fName = cFile.getName().toLowerCase();
        if (fName.endsWith(".cpp") || fName.endsWith(".c")) {
          // Reinstall toolchain if there was an error last time we tried to build
          verifyToolchain();
          Thread cThread = new Thread(() -> {
            try {
              listPane.setForeground(Color.black);
              listPane.setText("");
              Map<String,String> tags = new HashMap<>();
              tags.put("TDIR", tmpDir);
              tags.put("TEXE", tmpExe);
              tags.put("IDIR", tmpExe + "avr" + fileSep + "include" + fileSep);
              tags.put("FNAME", fName);
              tags.put("PREPROCESS", "PREONLY");
              compileMap = MegaTinyCompiler.compile(codePane.getText(), tags, prefs, this);
              if (compileMap == null) {
                return;
              }
              if (compileMap.containsKey("ERR")) {
                listPane.setForeground(Color.red);
                listPane.setText(compileMap.get("ERR"));
                compiled = false;
              } else {
                listPane.setForeground(Color.black);
                listPane.setText(compileMap.get("PRE"));
              }
              selectTab(Tab.LIST);
            } catch (Exception ex) {
              ex.printStackTrace();
              infoPane.append("Stack Trace:\n");
              ByteArrayOutputStream bOut = new ByteArrayOutputStream();
              PrintStream pOut = new PrintStream(bOut);
              ex.printStackTrace(pOut);
              pOut.close();
              infoPane.append(bOut.toString() + "\n");
              selectTab(Tab.INFO);
            }
          });
          cThread.start();
        } else {
          listPane.setText("Must be .c or .cpp file");
        }
      } else {
        showErrorDialog("Please save file first!");
      }
    });
    actions.addSeparator();
    /*
     *    Program Chip Menu
     */
    actions.add(progFlash = new JMenuItem("Program Flash"));
    progFlash.setToolTipText("Used to Upload Compiled Program Code to Device");
    progFlash.addActionListener(e -> {
      if (avrChip != null && canProgram()) {
        ChipInfo info = chipTypes.get(avrChip);
        EDBG.Programmer prog = getSelectedProgrammer();
        if (prog != null) {
          try {
            Utility.CodeImage codeImg = Utility.parseIntelHex(hexPane.getText());
            EDBG edbg = new EDBG(this, true);
            try {
              edbg.eraseTarget(0, 0);
              edbg.writeFlash(0, codeImg.data);
            } finally {
              edbg.close();
            }
            showMessageDialog(this, "Done");
          } catch (EDBG.EDBGException ex) {
            showErrorDialog(ex.getMessage());
          }
        } else {
          showErrorDialog("Programmer not available");
        }
      }
    });
    /*
     *    Read/Modify Fuses Menu Item
     */
    actions.add(readFuses = new JMenuItem("Read/Modify Fuses"));
    readFuses.setToolTipText("Used to Read and optionally Modify Device's Fuse Bytes");
    readFuses.addActionListener(e -> {
      // Use chip type, if selected, else use attiny212 as proxy
      ChipInfo info = chipTypes.get(avrChip != null ? avrChip : "attiny212");
      EDBG.Programmer prog = getSelectedProgrammer();
      if (prog != null) {
        EDBG edbg = null;
        try {
          edbg = new EDBG(this, true);
          FusePane fusePane = new FusePane(info);
          int[] offsets = new int[] {0, 1, 2, 4, 5, 6, 7, 8 /*, 10*/};
          byte[] fuses = edbg.readFuses(offsets);
          for (int ii = 0; ii < offsets.length; ii++) {
            fusePane.setFuse(offsets[ii], fuses[ii]);
          }
          if (JOptionPane.showConfirmDialog(this, fusePane, "FUSES for " + info.name, OK_CANCEL_OPTION, PLAIN_MESSAGE) == 0) {
            List<Integer> changedOffsets = new ArrayList<>();
            for (int offset : offsets) {
              if (fusePane.hasChanged(offset)) {
                changedOffsets.add(offset);
              }
            }
            if (changedOffsets.size() > 0) {
              int[] cOffs = changedOffsets.stream().mapToInt(Integer::intValue).toArray();
              byte[] cFuses = new byte[cOffs.length];
              for (int ii = 0; ii < cOffs.length; ii++) {
                cFuses[ii] = fusePane.getFuse(changedOffsets.get(ii));
              }
              StringBuilder msg = new StringBuilder("<html>Confirm update to changed fuses?<br><br>");
              msg.append("<p style=\"font-family:Courier;font-size:12\">");
              for (int ii = 0; ii < cOffs.length; ii++) {
                msg.append(String.format("Fuse 0x%02X: 0x%02X -> 0x%02X<br>", cOffs[ii], fuses[cOffs[ii]], cFuses[ii]));
              }
              msg.append("</p>");
              msg.append("<br>Note: Changes will not take effect until<br>processor is RESET</html>");
              if (doWarningDialog(msg.toString())) {
                edbg.writeFuses(cOffs, cFuses);
              }
            }
          }
        } catch (EDBG.EDBGException ex) {
          showErrorDialog(ex.getMessage());
        } finally {
          if (edbg != null) {
            edbg.close();
          }
        }
      } else {
        showErrorDialog("Programmer not available");
      }
    });
    /*
     *    View EEPROM Contents
     */
    JMenuItem readEeprom;
    actions.add(readEeprom = new JMenuItem("Read/Modify EEPROM"));
    readEeprom.setToolTipText("Used to Read Device's EEPROM Bytes");
    readEeprom.addActionListener(e -> {
      EDBG.Programmer prog = getSelectedProgrammer();
      if (prog != null) {
        EDBG edbg = null;
        try {
          edbg = new EDBG(this, true);
          byte[] sig = edbg.getDeviceSignature();         // 3 bytes
          String code = String.format("%02X%02X%02X", sig[0], sig[1], sig[2]);
          ChipInfo chip = chipSignatures.get(code);
          int eBytes = chip.getInt("eeprom");
          byte[] data = edbg.readEeprom(0, eBytes);
          HexEditPane hexPane = new HexEditPane(8, 8);
          EDBG debugger = edbg;
          hexPane.showVariable("EEPROM", null, 0, data, new HexEditPane.Update() {
            @Override
            public void setValue (int offset, int value) throws Exception {
              debugger.writeEeprom(offset, new byte[] {(byte) value});
            }
          });
        } catch (EDBG.EDBGException ex) {
          showErrorDialog(ex.getMessage());
        } finally {
          if (edbg != null) {
            edbg.close();
          }
        }
      } else {
        showErrorDialog("Programmer not available");
      }
    });
    /*
     *    Read Signature and Serial Number
     */
    actions.add(idTarget = new JMenuItem("Identify Device"));
    idTarget.setToolTipText("Used to Read and Send Back Device's Signature & Serial Number");
    idTarget.addActionListener(e -> {
      // Use chip type, if selected, else use attiny212 as proxy
      ChipInfo info = chipTypes.get(avrChip != null ? avrChip : "attiny212");
      EDBG.Programmer prog = getSelectedProgrammer();
      if (prog != null) {
        EDBG edbg = null;
        try {
          edbg = new EDBG(this, true);
          byte[] sig = edbg.getDeviceSignature();         // 3 bytes
          String code = String.format("%02X%02X%02X", sig[0], sig[1], sig[2]);
          ChipInfo chip = chipSignatures.get(code);
          byte[] ser = edbg.getDeviceSerialNumber();      // 13 bytes
          Object[][] data = {
              {"Type:", chip != null ? chip.name : "unknown"},
              {"Pins:", chip != null ? chip.pins + " pins" : "unknown"},
              {"Signature:", String.format("%02X, %02X, %02X", sig[0] & 0xFF, sig[1] & 0xFF, sig[2] & 0xFF)},
              {"Serial Num:", String.format("%02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X, %02X",
                                            ser[0] & 0xFF, ser[1] & 0xFF, ser[2] & 0xFF, ser[3] & 0xFF, ser[4] & 0xFF,
                                            ser[5] & 0xFF, ser[6] & 0xFF, ser[7] & 0xFF, ser[8] & 0xFF, ser[9] & 0xFF,
                                            ser[10] & 0xFF, ser[11] & 0xFF, ser[12])},
              {"Flash:",  chip != null ? chip.get("flash") + "k bytes" : "unknown"},
              {"EEProm:", chip != null ? chip.get("eeprom") + " bytes" : "unknown"},
              {"SRam:",   chip != null ? chip.get("sram") + " bytes" : "unknown"},
          };
          JPanel panel = new JPanel(new BorderLayout());
          //                                                                            ot ol ob or it il ib ir
          panel.setBorder(Utility.getBorder(BorderFactory.createLineBorder(Color.black), 1, 1, 1, 1, 1, 4, 1, 1));
          JTable table = new JTable(data, new String[] {"", ""});
          table.setFont(Utility.getCodeFont(12));
          table.getColumnModel().getColumn(0).setPreferredWidth(100);
          table.getColumnModel().getColumn(1).setPreferredWidth(400);
          table.setRowHeight(20);
          panel.add(table, BorderLayout.CENTER);
          ImageIcon icon = new ImageIcon(Utility.class.getResource("images/info-32x32.png"));
          showMessageDialog(this, panel, "Device Info", JOptionPane.PLAIN_MESSAGE, icon);
        } catch (Exception ex) {
          showErrorDialog(ex.getMessage());
        } finally {
          if (edbg != null) {
            edbg.close();
          }
        }
      } else {
        showErrorDialog("Programmer not available");
      }
    });
    /*
     *    Reinstall Toolchain Menu Item
     */
    actions.addSeparator();
    actions.add(mItem = new JMenuItem("Reinstall Toolchain"));
    mItem.setToolTipText("Copies AVR Toolchain into Java Temporary Disk Space where it can be Executed");
    mItem.addActionListener(e -> reloadToolchain());
    menuBar.add(actions);
    /*
     *    Settings menu
     */
    JMenu settings = new JMenu("Settings");
    menuBar.add(settings);
    // Add "Tabs" Menu with submenu
    settings.add(codePane.getTabSizeMenu());
    settings.addSeparator();
    // Add "Font Size" Menu with submenu
    settings.add(codePane.getFontSizeMenu());
    settings.addSeparator();
    // Add "Programmer" Menu
    progMenu = new JMenu("Programmer");
    progVidPid = prefs.get("progVidPid", "");
    progMenu.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected (MenuEvent e) {
        // Populate menu on demand
        progMenu.removeAll();
        ButtonGroup progGroup = new ButtonGroup();
        for (EDBG.Programmer prog : EDBG.getProgrammers()) {
          JRadioButtonMenuItem item = new JRadioButtonMenuItem(prog.name, prog.key.equals(progVidPid));
          String tTip = String.format("<html><b>Product</b>: %s <br><b>VID</b>: 0x%04X<br><b>PID</b>: 0x%02X<br><b>Seria</b>l: %s " +
                                          "<br><b>Release:</b> %d </html>",
                                      prog.product, prog.vid, prog.pid, prog.serial, prog.release);
          item.setToolTipText(tTip);
          progMenu.add(item);
          progGroup.add(item);
          item.addActionListener((ev) -> {
            prefs.put("progVidPid", progVidPid = prog.key);
          });
        }
      }
      @Override
      public void menuDeselected (MenuEvent e) {
      }
      @Override
      public void menuCanceled (MenuEvent e) {
      }
    });
    settings.add(progMenu);
    // Add Serial Port Menu, if "decode_updi" preference enabled
    jPort.setParameters (EDBG.UPDIClock * 1000, 8, 2, SerialPort.PARITY_EVEN);
    JMenu serialPort = new JMenu("Serial Port");
    serialPort.add(jPort.getPortMenu());
    serialPort.setVisible(prefs.getBoolean("decode_updi", false));
    prefs.addPreferenceChangeListener(evt -> {
      serialPort.setVisible(prefs.getBoolean("decode_updi", false));
      jPort.setPort(prefs.get("serial.port", ""));
    });
    settings.add(serialPort);
    settings.addSeparator();
    // Add Debugger Menu Item
    JMenuItem debugger = new JCheckBoxMenuItem("Show Debugger");
    debugger.setMnemonic(KeyEvent.VK_D);
    debugger.setEnabled(tabPane.getSelectedIndex() == Tab.LIST.num);
    debugger.addItemListener(ev -> {
      showDebugger = !showDebugger;
      debugger.setText(showDebugger ? "Hide Debugger" : "Show Debugger");
      listPane.showStatusPane(showDebugger);
    });
    settings.add(debugger);
    debugger.setAccelerator(DEBUG_KEY);
    tabPane.addChangeListener(ev -> debugger.setEnabled(tabPane.getSelectedIndex() == Tab.LIST.num));
    /*
     *    Target Menu
     */
    targetMenu = new RadioMenu("Target");
    avrChip = prefs.get("programmer.target", "attiny212");
    ButtonGroup targetGroup = new ButtonGroup();
    menuBar.add(targetMenu);
    String libType = null;
    int pinCount = 0;
    for (String type : chipTypes.keySet()) {
      ChipInfo info = chipTypes.get(type);
      if ((libType != null && !libType.equals(info.variant)) || (libType == null && info.variant != null)) {
        targetMenu.addSeparator();
      }
      libType = info.variant;
      if (pinCount != info.pins) {
        JMenuItem pinLabel = new JMenuItem(info.pins + " pins");
        pinLabel.setEnabled(false);
        targetMenu.add(pinLabel);
        pinCount = info.pins;
      }
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(type);
      item.setToolTipText(info.getInfo());
      if (avrChip.equals(type)) {
        item.setSelected(true);
      }
      targetMenu.add(item);
      targetGroup.add(item);
      item.addActionListener( ex -> {
        avrChip = type;
        targetMenu.setText("Target->" + avrChip);
        prefs.put("programmer.target", avrChip);
      });
    }
    targetMenu.setText("Target->" + avrChip);
    setJMenuBar(menuBar);
    // Add window close handler
    addWindowListener(new WindowAdapter() {
      public void windowClosing (WindowEvent ev) {
        listPane.statusPane.setActive(false);
        if (!codeDirty  ||  discardChanges()) {
          System.exit(0);
        }
      }
    });
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setSize(prefs.getInt("window.width", 930), prefs.getInt("window.height", 900));
    setMinimumSize(new Dimension(930, 400));
    setLocation(prefs.getInt("window.x", 10), prefs.getInt("window.y", 10));
    // Track window resize/move events and save in prefs
    addComponentListener(new ComponentAdapter() {
      public void componentMoved (ComponentEvent ev)  {
        Rectangle bounds = ev.getComponent().getBounds();
        prefs.putInt("window.x", bounds.x);
        prefs.putInt("window.y", bounds.y);
      }
      public void componentResized (ComponentEvent ev)  {
        Rectangle bounds = ev.getComponent().getBounds();
        prefs.putInt("window.width", bounds.width);
        prefs.putInt("window.height", bounds.height);
      }
    });
    setVisible(true);
    verifyToolchain();
  }

  private void updateChip (String src) {
    int idx = src.lastIndexOf("#pragma");
    int end = src.indexOf('\n', idx);
    String pragArea = src.substring(0, end + 1);
    for (String line : pragArea.split("\n")) {
      idx = line.indexOf("//");
      if (idx >= 0) {
        line = line.substring(0, idx).trim();
      }
      if (line.startsWith("#pragma")) {
        line = line.substring(7).trim();
        String[] parts = Utility.condenseWhitespace(line).split(" ");
        if (parts.length > 1 && "chip".equals(parts[0])) {
          if (chipTypes.containsKey(parts[1])) {
            avrChip = parts[1];
            targetMenu.setSelected(avrChip);
            targetMenu.setText("Target->" + avrChip);
            targetMenu.setEnabled(false);
            return;
          }
        }
      }
    }
    // If "#pragma chip" not found, or value is invalid set avrChip to selected item in "Target" menu, if any
    targetMenu.setEnabled(true);
    avrChip = targetMenu.getSelected();
    targetMenu.setText("Target->" + avrChip);
  }

  private boolean canProgram () {
    if (compiled || directHex) {
      return true;
    } else {
      showErrorDialog("Code not built!");
    }
    return false;
  }

  void appendToInfoPane (String text) {
    if (infoPane != null) {
      infoPane.append(text);
    }
  }

  static class ProgressBar extends JFrame {
    private final JDialog       frame;
    private final JProgressBar  progress;

    ProgressBar (JFrame comp, String msg) {
      frame = new JDialog(comp);
      frame.setUndecorated(true);
      JPanel pnl = new JPanel(new BorderLayout());
      pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      frame.add(pnl, BorderLayout.CENTER);
      pnl.add(progress = new JProgressBar(), BorderLayout.NORTH);
      JTextArea txt = new JTextArea(msg);
      txt.setEditable(false);
      pnl.add(txt, BorderLayout.SOUTH);
      Rectangle loc = comp.getBounds();
      frame.pack();
      frame.setLocation(loc.x + loc.width / 2 - 150, loc.y + loc.height / 2 - 150);
      frame.setVisible(true);
    }

    void setValue (int value) {
      SwingUtilities.invokeLater(() -> progress.setValue(value));
    }

    void setMaximum (int value) {
      progress.setMaximum(value);
    }

    void close () {
      frame.setVisible(false);
      frame.dispose();
    }
  }

  /**
   * Very all the files in the toolchain are intact by computing a CRC2 value from the tree
   * of directory and file names.  Note: the CRC is not based on the content of the files.
   */
  private void verifyToolchain () {
    boolean reloadTools = prefs.getBoolean("reload_toolchain", false);
    if (!reloadTools) {
      long oldCrc = prefs.getLong("toolchain-crc", 0);
      long newCrc = Utility.crcTree(tmpExe);
      reloadTools = newCrc != oldCrc;
    }
    if (!reloadTools) {
      // Check for new toolchain
      String zipFile = "toolchains/combined.zip";
      long oldCrc = prefs.getLong("toolzip-crc", 0);
      long newCrc = Utility.crcZipfile(zipFile);
      reloadTools = newCrc != oldCrc;
      prefs.putLong("toolzip-crc", newCrc);
    }
    if (reloadTools) {
      reloadToolchain();
    }
  }

  private void reloadToolchain () {
    try {
      new ToolchainLoader(this, "toolchains/combined.zip", tmpExe);
      prefs.remove("reload_toolchain");
      prefs.putLong("toolchain-crc", Utility.crcTree(tmpExe));
    } catch (Exception ex) {
      ex.printStackTrace();
      selectTab(Tab.LIST);
      listPane.setText("Unable to Install Toolchain:\n" + ex.toString());
    }
  }

  class ToolchainLoader implements Runnable  {
    private final String        srcZip;
    private final String        tmpExe;
    private final ProgressBar   progress;

    ToolchainLoader (JFrame comp, String srcZip, String tmpExe) {
      progress = new ProgressBar(comp, "Installing AVR Toolchain");
      this.srcZip = srcZip;
      this.tmpExe = tmpExe;
      new Thread(this).start();
    }

    public void run () {
      try {
        File dst = new File(tmpExe);
        //Utility.removeFiles(dst);           // causes comppile to fail? (need to investigate)
        if (!dst.exists() && !dst.mkdirs()) {
          throw new IllegalStateException("Unable to create directory: " + dst);
        }
        ZipInputStream zipStream = null;
        try {
          InputStream in = MegaTinyIDE.class.getResourceAsStream(srcZip);
          int fileSize = in.available();
          zipStream = new ZipInputStream(in);
          byte[] buffer = new byte[2048];
          Path outDir = Paths.get(dst.getPath());
          int bytesRead = 0;
          progress.setMaximum(100);
          ZipEntry entry;
          while ((entry = zipStream.getNextEntry()) != null) {
            String src = entry.getName();
            int idx = src.indexOf(":");
            if (idx >= 0) {
              int pIdx = src.lastIndexOf("/");
              // Only copy files prefixed
              String code = src.substring(pIdx + 1, idx);
              src = src.substring(0, pIdx + 1) + src.substring(idx + 1);
              if (!code.contains(osCode)) {
                continue;
              }
            }
            Path filePath = outDir.resolve(src);
            File dstDir = filePath.toFile().getParentFile();
            if (!dstDir.exists() && !dstDir.mkdirs()) {
              throw new IllegalStateException("Unable to create directory: " + dstDir);
            }
            File dstFile = filePath.toFile();
            FileOutputStream fos = new FileOutputStream(dstFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
            int len;
            while ((len = zipStream.read(buffer)) > 0) {
              bos.write(buffer, 0, len);
              bytesRead += len;
            }
            bos.close();
            fos.close();
            // Must set permissions after file is written or it doesn't take...
            String file = dstFile.getName();
            if (!file.contains(".") || file.toLowerCase().endsWith(".exe")) {
              if (!dstFile.setExecutable(true)) {
                showErrorDialog("Unable to set permissions for " + dstFile);
              }
            }
            float percent = ((float) bytesRead / fileSize) * 100;
            progress.setValue((int) percent);
          }
        } finally {
          if (zipStream != null) {
            zipStream.close();
          }
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        showErrorDialog("ToolchainLoader.run() exception " + ex.getMessage());
      }
      progress.close();
    }
  }

  private boolean discardChanges () {
    return doWarningDialog("Discard Changes?");
  }

  public void showErrorDialog (String msg) {
    ImageIcon icon = new ImageIcon(Utility.class.getResource("images/warning-32x32.png"));
    showMessageDialog(this, msg, "Error", JOptionPane.PLAIN_MESSAGE, icon);
  }

  public boolean doWarningDialog (String question) {
    ImageIcon icon = new ImageIcon(Utility.class.getResource("images/warning-32x32.png"));
    return JOptionPane.showConfirmDialog(this, question, "Warning", JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE, icon) == JOptionPane.OK_OPTION;
  }

  public static void main (String[] args) {
    java.awt.EventQueue.invokeLater(MegaTinyIDE::new);
  }
}
