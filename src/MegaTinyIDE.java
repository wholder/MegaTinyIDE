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
import javax.swing.filechooser.FileView;
import javax.swing.text.Document;

import jssc.SerialNativeInterface;
import jssc.SerialPort;

import static javax.swing.JOptionPane.*;

/**
   *  An IDE for tinyAVR® 1-series and 0-series Microcontrollers
   *  Author: Wayne Holder, 2011-2021
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
  private static final KeyStroke  LOAD_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_L, cmdMask) ;
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
  public Programmer               programmer = null;
  private ListingPane             listPane;
  private MyTextPane              hexPane;
  private final MyTextPane        infoPane;
  private final JMenuItem         openMenu = new JMenuItem("Open");
  private final JMenuItem         debugMenu = new JCheckBoxMenuItem("Show Debugger");
  private final JMenuItem         saveMenu = new JMenuItem("Save");
  private final JMenuItem         saveAsMenu = new JMenuItem("Save As...");
  private final JComponent        newMenu;
  private final RadioMenu         targetMenu;
  private final JMenuItem         build = new JMenuItem("Build");
  private final JMenuItem         loadElf = new JMenuItem("Load ELF");
  private final JMenuItem         readFlash = new JMenuItem("Read Flash");
  private final JMenuItem         disasmFlash = new JMenuItem("Disassemble Flash");
  private final JMenuItem         progFlash = new JMenuItem("Program Flash");
  private final JMenuItem         readFuses = new JMenuItem("Read/Modify Fuses");
  private final JMenu             progMenu;
  private String                  tmpDir, tmpExe;
  private String                  progVidPid;
  private String                  avrChip;
  private String                  editFile;
  private boolean                 compiled, codeDirty, showDebugger;
  final Preferences               prefs = Preferences.userRoot().node(this.getClass().getName());
  final JSSCPort                  jPort = new JSSCPort(prefs);
  boolean                         directHex;
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
    if (build != null) {
      build.setEnabled(!active);
      loadElf.setEnabled(!active);
    }
    readFlash.setEnabled(!active);
    disasmFlash.setEnabled(!active);
    progFlash.setEnabled(!active);
    readFuses.setEnabled(!active);
    progMenu.setEnabled(!active);
  }

  void setSource (String src) {
    if (codePane.getText().length() == 0 || discardChanges()) {
      codePane.setCode(src);
      setDirtyIndicator(false);
      directHex = false;
      selectTab(MegaTinyIDE.Tab.SRC);
      saveMenu.setEnabled(true);
    }
  }

  public static boolean isDeveloper () {
    return "wholder".equals(System.getProperty("user.name"));
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

  public String getProgPidVid () {
    return progVidPid;
  }

  public Programmer getProgrammer (boolean program) {
    if (programmer != null) {
      return programmer;
    }
    if (progVidPid != null && progVidPid.length() > 0) {
      return programmer = new EDBG(this, program);

    } else if (jPort != null && jPort.postSelected()) {
      return programmer = new SDBG(this, jPort);
    }
    throw new IllegalStateException("Unvalid Progerammerr");
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
      return "series: " + series + ", signature: " + signature + ", flash: " + flash + "K, sram: " + sram +
        " bytes, eeprom: " + eeprom + " bytes";
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
    prefs.putInt("sdbg_baud", prefs.getInt("sdbg_baud", 57600));
    prefs.putBoolean("enable_preprocessing", prefs.getBoolean("enable_preprocessing", false));
    prefs.putBoolean("developer_features", prefs.getBoolean("developer_features", false));
    prefs.putBoolean("decode_updi", prefs.getBoolean("decode_updi", false));
    prefs.putBoolean("show_dependencies", prefs.getBoolean("show_dependencies", false));
    prefs.putBoolean("enable_projects", prefs.getBoolean("enable_projects", false));
  }

  public static class ProjectFolderFilter extends FileFilter {
    /**
     * Whether the given file is accepted by this filter.
     */
    public boolean accept (File file) {
      if (file.isDirectory()) {
        String[] fParts = file.getName().split("\\.");
        File[] files =  file.listFiles();
        if (files != null) {
          for (File tmp : files) {
            if (tmp.isFile()) {
              String[] tParts = tmp.getName().split("\\.");
              if (fParts[0].equals(tParts[0])) {
                return true;
              }
            }
          }
        }
      }
      return false;
    }

    /**
     * The description of this filter. For example: "JPG and GIF Images"
     * @see FileView#getName
     */
    public String getDescription() {
      return "Project Folder";
    }
  }

  private JFileChooser getFileChooser () {
    JFileChooser fc = new JFileChooser();
    String fPath = prefs.get("default.dir", "/");
    int selIndex = tabPane.getSelectedIndex();
    if (selIndex == Tab.HEX.num) {
      FileFilter filter = new FileNameExtensionFilter(".hex files", "hex");
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter);
      int idx = fPath.lastIndexOf('.');
      if (idx > 0) {
        fPath = fPath.substring(0, idx + 1) + "hex";
      }
    } else if (selIndex == Tab.LIST.num) {
      FileFilter filter = new FileNameExtensionFilter(".lst files", "lst");
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter);
      int idx = fPath.lastIndexOf('.');
      if (idx > 0) {
        fPath = fPath.substring(0, idx + 1) + "lst";
      }
    } else {
      List<FileFilter> filterList = new ArrayList<>();
      filterList.add(new FileNameExtensionFilter("AVR .c, .cpp or .ino files", "c", "cpp", "ino"));
      filterList.add(new FileNameExtensionFilter("AVR .asm or .s files", "asm", "s"));
      if (prefs.getBoolean("enable_projects", false)) {
        filterList.add(new ProjectFolderFilter());
      }
      FileFilter[] filters = filterList.toArray(new FileFilter[0]);
      String ext = prefs.get("default.extension", "c");
      for (FileFilter filter : filters) {
        fc.addChoosableFileFilter(filter);
        if (filter instanceof FileNameExtensionFilter && ((FileNameExtensionFilter) filter).getExtensions()[0].equals(ext)) {
          fc.setFileFilter(filter);
        }
      }
    }
    if (!fPath.endsWith("/")) {
      fPath += "/";
    }
    fc.setCurrentDirectory(new File(fPath));
    fc.setAcceptAllFileFilterUsed(true);
    fc.setMultiSelectionEnabled(false);
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    return fc;
  }

  private JFileChooser getElfChooser () {
    JFileChooser fc = new JFileChooser();
    String fPath = prefs.get("default.dir", "/");
    FileNameExtensionFilter filter = new FileNameExtensionFilter("AVR .elf files", "elf");
    String ext = "elf";
    fc.addChoosableFileFilter(filter);
    if (filter.getExtensions()[0].equals(ext)) {
      fc.setFileFilter(filter);
    }
    if (!fPath.endsWith("/")) {
      fPath += "/";
    }
    fc.setCurrentDirectory(new File(fPath));
    fc.setAcceptAllFileFilterUsed(true);
    fc.setMultiSelectionEnabled(false);
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    return fc;
  }

  public void selectTab (Tab tab) {
    tabPane.setSelectedIndex(tab.num);
  }

  void setDirtyIndicator (boolean dirty) {
    codeDirty = dirty;
    this.setTitle("MegaTinyIDE: " + (editFile != null ? editFile : "") + (dirty ? " [unsaved]" : ""));
  }

  private void showAboutBox () {
    ImageIcon icon = null;
    try {
      icon = getImageIcon("images/MegaTinyIDE.png");
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
    items.add(new ParmDialog.Item("Serial Programmer Baud Rate:19200:38400:57600:115200", "*[PROGBAUD]*", "sdbg_baud", 57600));
    if ((modifiers & InputEvent.CTRL_MASK) != 0) {
      // Developer features
      items.add(new ParmDialog.Item("Enable Preprocessing (Developer)", "*[PREPROCESS]*", "enable_preprocessing", false));
      items.add(new ParmDialog.Item("Enable Developer Features", "*[DEV_ONLY]*", "developer_features", false));
      items.add(new ParmDialog.Item("Decode UPDI Commands", "*[UPDI_DECODE]*", "decode_updi", false));
      items.add(new ParmDialog.Item("Show Dependencies", "*[SHOW_DEPENDENCIES]*", "show_dependencies", false));
      items.add(new ParmDialog.Item("Enable Project Folders", "*[ENABLE_PROJECTS]*", "enable_projects", false));
    }
    ParmDialog.Item[] parmSet = items.toArray(new ParmDialog.Item[0]);
    ParmDialog dialog = (new ParmDialog("Edit Preferences", parmSet, new String[] {"Save", "Cancel"}));
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);              // Note: this call invokes dialog
  }

  private MegaTinyIDE (String[] args) {
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
      setDirtyIndicator(true);
      updateChip(codePane.getText());
      compiled = false;
      listPane.setForeground(Color.red);
      hexPane.setForeground(Color.red);
    });
    MarkupView howToPane = new MarkupView(this, "documentation/index.md");
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
    JMenuItem mItem;
    // Add "File" Menu
    JMenu fileMenu = new JMenu("File");
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
        Map<String, String> latest = Utility.getResourceMap(new URL(VERSION_URL));
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
                  ImageIcon icon = getImageIcon("images/info-32x32.png");
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
              ImageIcon icon = getImageIcon("images/info-32x32.png");
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
    if (prefs.getBoolean("enable_projects", false)) {
      fileMenu.add(newMenu = new JMenu("New"));
      JMenuItem newProject = new JMenuItem("Project Folder");         // New->Project Folder
      newProject.addActionListener(e -> {
        if (codePane.getText().length() == 0 || discardChanges()) {
          codePane.setForeground(Color.black);
          codePane.setCode("");
          directHex = false;
          compiled = false;
          editFile = null;
          setDirtyIndicator(false);
          selectTab(Tab.SRC);
          cFile = null;
        }
      });
      newMenu.add(newProject);
      JMenuItem newFile = new JMenuItem("Project File");              // New->Project File
      newMenu.add(newFile);
      newFile.addActionListener(e -> {
        if (codePane.getText().length() == 0 || discardChanges()) {
          codePane.setForeground(Color.black);
          codePane.setCode("");
          directHex = false;
          compiled = false;
          editFile = null;
          setDirtyIndicator(false);
          selectTab(Tab.SRC);
          cFile = null;
        }
      });
    } else {
      fileMenu.add(newMenu = new JMenuItem("New"));                   // New (file)
      ((JMenuItem) newMenu).addActionListener(e -> {
        if (codePane.getText().length() == 0 || discardChanges()) {
          codePane.setForeground(Color.black);
          codePane.setCode("");
          directHex = false;
          compiled = false;
          editFile = null;
          setDirtyIndicator(false);
          selectTab(Tab.SRC);
          cFile = null;
        }
      });
    }
    fileMenu.add(openMenu);                                           // Open
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
              prefs.put("default.extension", exts[0]);
            }
            String src = Utility.getFile(oFile);
            cFile = oFile;
            codePane.setForeground(Color.black);
            codePane.setCode(src);
            editFile = oFile.getAbsolutePath();
            prefs.put("default.dir", oFile.getParent());
            setDirtyIndicator(false);
            directHex = false;
            selectTab(Tab.SRC);
            saveMenu.setEnabled(true);
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    });
    fileMenu.add(saveMenu);                                           // Save
    saveMenu.setAccelerator(SAVE_KEY);
    saveMenu.setEnabled(false);
    saveMenu.addActionListener(e -> {
      Utility.saveFile(cFile, codePane.getText());
      setDirtyIndicator(false);
    });
    fileMenu.add(saveAsMenu);                                         // Save As
    saveAsMenu.addActionListener(e -> {
      JFileChooser fc = getFileChooser();
      if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File sFile = fc.getSelectedFile();
        FileFilter filter = fc.getFileFilter();
        if (filter instanceof FileNameExtensionFilter) {
          String[] exts = ((FileNameExtensionFilter) filter).getExtensions();
          prefs.put("default.extension", exts[0]);
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
        editFile = sFile.getAbsolutePath();
        prefs.put("default.dir", sFile.getParent());
        setDirtyIndicator(codeDirty = false);
        saveMenu.setEnabled(true);
      }
    });
    fileMenu.addSeparator();
    fileMenu.add(mItem = new JMenuItem(""));                          // Quit MegaTinyIDE
    mItem.setAccelerator(QUIT_KEY);
    mItem.addActionListener(e -> {
      if (!codeDirty || discardChanges()) {
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
      openMenu.setEnabled(idx == Tab.SRC.num || idx == Tab.DOC.num || idx == Tab.HEX.num);
      saveMenu.setEnabled(idx == Tab.SRC.num);
      saveAsMenu.setEnabled(idx == Tab.SRC.num || idx == Tab.HEX.num || idx == Tab.LIST.num);
      newMenu.setEnabled(idx == Tab.SRC.num);
    });
    // Add "Actions" Menu
    JMenu actions = new JMenu("Actions");
    // Add "Build" Menu Item
    actions.add(build);
    build.setToolTipText("Compile Code in Source Code Pane and Display Result in Listing and Hex Output Panes");
    build.setAccelerator(BUILD_KEY);
    build.addActionListener(e -> compileCode());
    // Add "Load ELF" Menu Item
    actions.add(loadElf);
    loadElf.setAccelerator(LOAD_KEY);
    loadElf.addActionListener(e -> {
      JFileChooser fc = getElfChooser();
      if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          File elfFile = fc.getSelectedFile();
          String fName = elfFile.getName();
          verifyToolchain(new Thread(() -> {
          try {
            Utility.copyFile(elfFile, tmpDir + fName);
            String target = Utility.getTargetFromElf(elfFile);
            if (avrChip != null) {
              setTarget(avrChip = target);
            }
            Map<String, String> tags = new HashMap<>();
            tags.put("TDIR", tmpDir);
            tags.put("TEXE", tmpExe);
            tags.put("INTLV", prefs.getBoolean("interleave", true) ? "-S" : "");
            String[] srcParts = fName.split("\\.");
            String srcBase = srcParts[0];
            tags.put("BASE", srcBase);
            compileMap = MegaTinyCompiler.debugBuild(tags, prefs);
          } catch (Exception ex) {
            showErrorDialog("Error loading: " + fName);
          }
          listPane.setForeground(Color.black);
          StringBuilder tmp = new StringBuilder();
          if (compileMap.containsKey("INFO")) {
            tmp.append(compileMap.get("INFO"));
            tmp.append("\n\n");
          }
          if (compileMap.containsKey("WARN")) {
            tmp.append(compileMap.get("WARN"));
            tmp.append("\n\n");
          }
          tmp.append(compileMap.get("SIZE"));
          tmp.append(compileMap.get("LST"));
          String listing = tmp.toString();
          listPane.setText(listing.replace(tmpDir, ""));
          hexPane.setForeground(Color.black);
          hexPane.setText(compileMap.get("HEX"));
          compiled = true;
          listPane.statusPane.setActive(false);
          showDebugger = true;
          debugMenu.setText("Hide Debugger");
          listPane.showStatusPane(showDebugger);
          selectTab(Tab.LIST);
        }));
      }
    });
    // Add "Run Preprocessor" Menu Item
    JMenuItem preprocess = new JMenuItem("Run Preprocessor");
    preprocess.setToolTipText("Run GCC Preprocessor and Display Result in Listing Pane");
    actions.add(preprocess);
    prefs.addPreferenceChangeListener(evt -> preprocess.setVisible(prefs.getBoolean("enable_preprocessing", false)));
    preprocess.addActionListener(e -> {
      if (cFile != null) {
        String fName = cFile.getName().toLowerCase();
        if (fName.endsWith(".cpp") || fName.endsWith(".c")) {
          // Reinstall toolchain if there was an error last time we tried to build
          verifyToolchain(new Thread(() -> {
            try {
              listPane.setForeground(Color.black);
              listPane.setText("");
              Map<String,String> tags = new HashMap<>();
              tags.put("TDIR", tmpDir);
              tags.put("TEXE", tmpExe);
              tags.put("IDIR", tmpExe + "avr" + fileSep + "include" + fileSep);
              tags.put("FNAME", fName);
              tags.put("PREPROCESS", "PREONLY");
              compileMap = MegaTinyCompiler.compileBuild(codePane.getText(), tags, prefs, this);
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
              infoPane.append(bOut + "\n");
              selectTab(Tab.INFO);
            }
          }));
        } else {
          listPane.setText("Must be .c or .cpp file");
        }
      } else {
        showErrorDialog("Please save file first!");
      }
    });
    actions.addSeparator();
    /*
     *    "Read Flash" Menu Item
     */
    actions.add(readFlash);
    readFlash.setToolTipText("Used to Read Program Code from Device");
    readFlash.addActionListener(e -> {
      if (avrChip != null) {
        try {
          Programmer edbg = getProgrammer(true);
          new Thread(() -> {
            try {
              byte[] sig = edbg.getDeviceSignature();         // 3 bytes
              String code = String.format("%02X%02X%02X", sig[0], sig[1], sig[2]);
              ChipInfo chip = chipSignatures.get(code);
              int flashSize = chip.getInt("flash") * 1024;
              edbg.setProgressMessage("Reading Flash");
              byte[] data1 = edbg.readFlash(0, flashSize);
              edbg.setProgressMessage("Verifying...");
              byte[] data2 = edbg.readFlash(0, flashSize);
              edbg.closeProgressBar();
              if (Arrays.equals(data1, data2)) {
                HexEditPane flashPane = new HexEditPane(MegaTinyIDE.this, 16, 16);
                flashPane.showDialog("Flash Code", "Flash Code", 0, data1, null);
              } else {
                showErrorDialog("Verify failed");
              }
            } finally {
              edbg.close();
            }
          }).start();
        } catch (EDBG.EDBGException ex) {
          showErrorDialog(ex.getMessage());
        }
      }
    });
    /*
     *    "Disassemble Flash" Menu Item
     */
    actions.add(disasmFlash);
    disasmFlash.setToolTipText("Used to Disassemble Program Code from Device");
    disasmFlash.addActionListener(e -> {
      if (avrChip != null) {
        try {
          Programmer edbg = getProgrammer(true);
          new Thread(() -> {
            try {
              byte[] sig = edbg.getDeviceSignature();         // 3 bytes
              String code = String.format("%02X%02X%02X", sig[0], sig[1], sig[2]);
              ChipInfo chip = chipSignatures.get(code);
              int flashSize = chip.getInt("flash") * 1024;
              edbg.setProgressMessage("Reading Flash");
              byte[] data1 = edbg.readFlash(0, flashSize);
              edbg.setProgressMessage("Verifying...");
              byte[] data2 = edbg.readFlash(0, flashSize);
              edbg.closeProgressBar();
              if (Arrays.equals(data1, data2)) {
                disassemble(chip, listPane, data1);
                tabPane.setSelectedIndex(Tab.LIST.num);
              } else {
                showErrorDialog("Verify failed");
              }
            } finally {
              edbg.close();
            }
          }).start();
        } catch (EDBG.EDBGException ex) {
          showErrorDialog(ex.getMessage());
        }
      }
    });
    /*
     *    "Program Flash" Menu  Item
     */
    actions.add(progFlash);
    progFlash.setToolTipText("Used to Upload Compiled Program Code to Device");
    progFlash.addActionListener(e -> {
      if (avrChip != null && canProgram()) {
        try {
          Utility.CodeImage codeImg = Utility.parseIntelHex(hexPane.getText());
          Programmer edbg = getProgrammer(true);
          new Thread(() -> {
            try {
              edbg.setProgressMessage("Reading Flash");
              edbg.eraseTarget(0, 0);         //
              edbg.writeFlash(0, codeImg.data);
              edbg.setProgressMessage("Verifying...");
              byte[] data2 = edbg.readFlash(0, codeImg.data.length);
              edbg.closeProgressBar();
              if (!Arrays.equals(codeImg.data, data2)) {
                showErrorDialog("Verify failed");
              }
            } finally {
              edbg.close();
            }
          }).start();
          showMessageDialog(this, "Done");
        } catch (EDBG.EDBGException ex) {
          showErrorDialog(ex.getMessage());
        }
      }
    });
    /*
     *    Read/Modify Fuses Menu Item
     */
    actions.add(readFuses);
    readFuses.setToolTipText("Used to Read and optionally Modify Device's Fuse Bytes");
    readFuses.addActionListener(e -> {
      // Use chip type, if selected, else use attiny212 as proxy
      ChipInfo info = chipTypes.get(avrChip != null ? avrChip : "attiny212");
      Programmer edbg = null;
      try {
        edbg = getProgrammer(true);
        FusePane fusePane = new FusePane(info);
        int[] offsets = new int[] {0, 1, 2, 4, 5, 6, 7, 8 /*, 10*/};
        Map<Integer,Integer> reverse = new HashMap<>();
        byte[] fuses = edbg.readFuses(offsets);
        for (int ii = 0; ii < offsets.length; ii++) {
          fusePane.setFuse(offsets[ii], fuses[ii]);
          reverse.put(offsets[ii], ii);
        }
        if (JOptionPane.showConfirmDialog(this, fusePane, "FUSES for " + info.name, OK_CANCEL_OPTION, PLAIN_MESSAGE) == 0) {
          List<Integer> changedOffsets = new ArrayList<>();
          for (int offset : offsets) {
            if (fusePane.hasChanged(offset)) {
              changedOffsets.add(offset);
            }
          }
          if (!changedOffsets.isEmpty()) {
            int[] cOffs = changedOffsets.stream().mapToInt(Integer::intValue).toArray();
            byte[] cFuses = new byte[cOffs.length];
            for (int ii = 0; ii < cOffs.length; ii++) {
              cFuses[ii] = fusePane.getFuse(changedOffsets.get(ii));
            }
            StringBuilder msg = new StringBuilder("<html>Confirm update to changed fuses?<br><br>");
            msg.append("<p style=\"font-family:Courier;font-size:12\">");
            for (int ii = 0; ii < cOffs.length; ii++) {
              int offset = cOffs[ii];
              int revOff = reverse.get(offset);
              int oldVal = fuses[revOff] & 0xFF;
              int newVal = cFuses[ii] & 0xFF;
              msg.append(String.format("Fuse 0x%02X: 0x%02X -> 0x%02X<br>", offset, oldVal, newVal));
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
    });
    /*
     *    View/Modify EEPROM Contents
     */
    JMenuItem readEeprom;
    actions.add(readEeprom = new JMenuItem("Read/Modify EEPROM"));
    readEeprom.setToolTipText("Used to Read & Modify Device's EEPROM Bytes");
    readEeprom.addActionListener(e -> {
      Programmer edbg = null;
      boolean attached = false;
      try {
        attached = programmer != null;
        edbg = getProgrammer(false);
        byte[] sig = edbg.getDeviceSignature();         // 3 bytes
        String code = String.format("%02X%02X%02X", sig[0], sig[1], sig[2]);
        ChipInfo chip = chipSignatures.get(code);
        int eBytes = chip.getInt("eeprom");
        byte[] data = edbg.readEeprom(0, eBytes);
        HexEditPane hexPane = new HexEditPane(this, 8, 8);
        Programmer debugger = edbg;
        boolean[] changed = new boolean[] {false};
        hexPane.showDialog("EEPROM", null, 0, data, (offset, value) -> {
          data[offset] = (byte) value;
          changed[0] = true;
        });
        if (changed[0]) {
          debugger.writeEeprom(0, data);
        }
      } catch (EDBG.EDBGException ex) {
        showErrorDialog(ex.getMessage());
      } finally {
        // Don't close connection if it was open before
        if (!attached && edbg != null) {
          edbg.close();
        }
      }
    });
    /*
     *    View/Modify USERROW Contents
     */
    JMenuItem readUserRow;
    actions.add(readUserRow = new JMenuItem("Read/Modify USERROW"));
    readUserRow.setToolTipText("Used to Read & Modify Device's USERROW Bytes");
    readUserRow.addActionListener(e -> {
      Programmer edbg = null;
      boolean attached = false;
      try {
        attached = programmer != null;
        edbg = getProgrammer(false);
        byte[] data = edbg.readUserRow(0, 32);
        HexEditPane hexPane = new HexEditPane(this, 8, 8);
        Programmer debugger = edbg;
        byte[] userRpw = debugger.readUserRow(0, 16);
        boolean[] changed = new boolean[] {false};
        hexPane.showDialog("USERROW", null, 0, data, (offset, value) -> {
          userRpw[offset] = (byte) value;
          changed[0] = true;
        });
        if (changed[0]) {
          debugger.writeUserRow(0, userRpw);
        }
      } catch (EDBG.EDBGException ex) {
        showErrorDialog(ex.getMessage());
      } finally {
        // Don't close connection if it was open before
        if (!attached && edbg != null) {
          edbg.close();
        }
      }
    });
    /*
     *    Read Device Info (Signature and Serial Number)
     */
    JMenuItem idTarget;
    actions.add(idTarget = new JMenuItem("Identify Device"));
    idTarget.setToolTipText("Used to Read and Send Back Device's Signature & Serial Number");
    idTarget.addActionListener(e -> {
      Programmer edbg = null;
      boolean attached = false;
      try {
        attached = programmer != null;
        edbg = getProgrammer(true);
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
        ImageIcon icon = getImageIcon("images/info-32x32.png");
        showMessageDialog(this, panel, "Device Info", JOptionPane.PLAIN_MESSAGE, icon);
      } catch (Exception ex) {
        showErrorDialog(ex.getMessage());
      } finally {
        // Don't close connection if it was open before
        if (!attached && edbg != null) {
          edbg.close();
        }
      }
    });
    /*
     *    Reinstall Toolchain Menu Item
     */
    actions.addSeparator();
    actions.add(mItem = new JMenuItem("Reinstall Toolchain"));
    mItem.setToolTipText("Copies AVR Toolchain into Java Temporary Disk Space where it can be Executed");
    mItem.addActionListener(e -> new Thread(() -> loadToolchain(null)).start());
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
        for (EDBG.ProgDevice prog : EDBG.getProgrammers(decodeUpdi())) {
          boolean selected = prog.key.equals(progVidPid);
          JRadioButtonMenuItem item = new JRadioButtonMenuItem(prog.name, selected);
          item.setToolTipText(prog.getInfo());
          progMenu.add(item);
          progGroup.add(item);
          item.addActionListener((ActionEvent ev) -> {
            prefs.put("progVidPid", progVidPid = prog.key);
            debugMenu.setEnabled(true);
            programmer = null;
          });
        }
        if (!decodeUpdi()){
          List<JRadioButtonMenuItem> items = jPort.getPortMenuItems();
          for (JRadioButtonMenuItem item : items) {
            progMenu.add(item);
            progGroup.add(item);
            item.addActionListener(ev -> {
              prefs.put("progVidPid", progVidPid = "");
              debugMenu.setEnabled(false);
              programmer = null;
            });
          }
        }
      }
      @Override
      public void menuDeselected (MenuEvent e) {}
      @Override
      public void menuCanceled (MenuEvent e) {}
    });
    settings.add(progMenu);
    // Add Serial Port Menu, if "decode_updi" preference enabled
    jPort.setParameters (EDBG.UPDIClock * 1000, 8, 2, SerialPort.PARITY_EVEN);
    JMenu serialPort = jPort.getPortMenu("Serial Port", null);
    serialPort.setVisible(decodeUpdi());
    prefs.addPreferenceChangeListener(evt -> {
      serialPort.setVisible(decodeUpdi());
      jPort.setPort(prefs.get("serial.port", ""));
    });
    settings.add(serialPort);
    settings.addSeparator();
    // Add Debugger Menu Item
    debugMenu.setMnemonic(KeyEvent.VK_D);
    debugMenu.addItemListener(ev -> {
      showDebugger = !showDebugger;
      debugMenu.setText(showDebugger ? "Hide Debugger" : "Show Debugger");
      listPane.showStatusPane(showDebugger);
      tabPane.setSelectedIndex(Tab.LIST.num);
    });
    debugMenu.setEnabled(progVidPid != null && progVidPid.length() > 0);
    settings.add(debugMenu);
    debugMenu.setAccelerator(DEBUG_KEY);
    tabPane.addChangeListener(ev -> debugMenu.setEnabled(tabPane.getSelectedIndex() == Tab.LIST.num));
    /*
     *    Target Menu
     */
    targetMenu = new RadioMenu("Target");
    avrChip = prefs.get("programmer.target", "attiny212");
    ButtonGroup targetGroup = new ButtonGroup();
    menuBar.add(targetMenu);
    int pinCount = 0;
    for (String type : chipTypes.keySet()) {
      ChipInfo info = chipTypes.get(type);
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
    verifyToolchain(null);
  }

  private void disassemble (ChipInfo chip, ListingPane listPane, byte[] data) {
    data = Utility.trimAvrCode(data);
    String intelHex = Utility.toIntelHex(data);
    verifyToolchain(new Thread(() -> {
      try {
        Utility.removeFiles(new File(tmpDir));
        // Write intel hex to temp dir
        File file = new File(tmpDir + "intel.hex");
        FileOutputStream out = new FileOutputStream(file);
        PrintStream print = new PrintStream(out);
        print.print(intelHex);
        print.close();
        // decompile intel hex
        Map<String, String> tags = new HashMap<>();
        tags.put("TDIR", tmpDir);
        tags.put("TEXE", tmpExe);
        String arch = "avr" +  chip.variant.substring(3);
        String cmd = "*[TEXE]*bin/avr-objdump -m " + arch + " -D *[TDIR]*intel.hex";
        cmd = Utility.replaceTags(cmd, tags);
        Process proc = Runtime.getRuntime().exec(cmd);
        String disasm =  Utility.runCmd(proc);
        String pat = "00000000 <.sec1>:\n";
        int idx = disasm.indexOf(pat);
        if (idx >= 0) {
          disasm = disasm.substring(idx + pat.length());
        }
        avrChip = chip.name;
        targetMenu.setSelected(avrChip);
        targetMenu.setText("Target->" + avrChip);
        listPane.setText(disasm);
        hexPane.setText(intelHex);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }));
  }

  private void setTarget (String target) {
    int items = targetMenu.getItemCount();
    for (int ii = 0; ii < items; ii++) {
      JMenuItem item = targetMenu.getItem(ii);
      if (item instanceof JRadioButtonMenuItem) {
        String label = item.getText();
        if (label.equals(target)) {
          item.setSelected(true);
          targetMenu.setText("Target->" + target);
        }
      }
    }
  }

  private void compileCode () {
    if (cFile != null) {
      String fName = cFile.getName().toLowerCase();
      // Reinstall toolchain if there was an error last time we tried to build
      verifyToolchain(new Thread(() -> {
        try {
          listPane.setForeground(Color.black);
          listPane.setText("");
          Map<String, String> tags = new HashMap<>();
          tags.put("TDIR", tmpDir);
          tags.put("TEXE", tmpExe);
          tags.put("IDIR", tmpExe + "avr" + fileSep + "include" + fileSep);
          tags.put("FNAME", fName);
          tags.put("EFILE", editFile);
          if (prefs.getBoolean("gen_prototypes", false)) {
            tags.put("PREPROCESS", "GENPROTOS");
          }
          compileMap = MegaTinyCompiler.compileBuild(codePane.getText(), tags, prefs, this);
          if (compileMap == null) {
            return;
          }
          String trueName = cFile.getName();
          if (compileMap.containsKey("ERR")) {
            listPane.setForeground(Color.red);
            String errText = compileMap.get("ERR");
            // Escape HTML <> symbols
            errText = errText.replaceAll("<", "&lt;");
            errText = errText.replaceAll(">", "&gt;");
            // Remove path to tmpDir from error messages
            errText = errText.replaceAll(tmpDir, "");
            Pattern lineRef = Pattern.compile("(" + trueName + ":([0-9]+?):(([0-9]+?):)*)", Pattern.CASE_INSENSITIVE);
            Matcher mat = lineRef.matcher(errText);
            Font font = Utility.getCodeFont(12);
            StringBuffer buf = new StringBuffer("<html><pre " + Utility.getFontStyle(font) + ">");
            while (mat.find()) {
              String seq = mat.group(1);
              String line = mat.group(2);
              String col = mat.group(4);
              if (col == null) {
                col = "0";
              }
              if (seq != null) {
                mat.appendReplacement(buf, Matcher.quoteReplacement("<a href=\"err:" + line + ":" + col + "\">" + seq + "</a>"));
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
            tmp.append(compileMap.get("SIZE"));
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
          listPane.setText("Compile error (see Error Info pane for details)\n" + ex);
          infoPane.append("Stack Trace:\n");
          ByteArrayOutputStream bOut = new ByteArrayOutputStream();
          PrintStream pOut = new PrintStream(bOut);
          ex.printStackTrace(pOut);
          pOut.close();
          infoPane.append(bOut + "\n");
          selectTab(Tab.INFO);
        }
      }));
    } else {
      showErrorDialog("Please save file first!");
    }
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
      String hex = hexPane.getText();
      if (hex != null && hex.length() > 0) {
        Utility.CodeImage code = Utility.parseIntelHex(hex);
        if (code.data.length > 0) {
          return true;
        }
      }
      showErrorDialog("Code not built!");
    }
    return false;
  }

  void appendToInfoPane (String text) {
    if (infoPane != null) {
      infoPane.append(text);
    }
  }

  /**
   * Verify all the files in the toolchain are intact by computing a CRC2 value from the tree
   * of directory and file names.  Note: the CRC is not based on the content of the files.
   */
  private void verifyToolchain (Thread thread) {
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
      new Thread(() -> loadToolchain(thread)).start();
      prefs.putBoolean("reload_toolchain", false);
    } else {
      if (thread != null) {
        thread.start();
      }
    }
  }

  public void loadToolchain (Thread thread) {
    Utility.ProgressBar progress = new Utility.ProgressBar(MegaTinyIDE.this, "Installing AVR Toolchain");
    String srcZip = "toolchains/combined.zip";
    try {
      File dst = new File(tmpExe);
      Utility.removeFiles(dst);           // causes compile to fail? (need to investigate)
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
          try {
            if (!file.contains(".") || file.toLowerCase().endsWith(".exe")) {
              if (!dstFile.setExecutable(true)) {
                showErrorDialog("Unable to set permissions for " + dstFile);
              }
            }
          } catch (Exception ex) {
            showErrorDialog("Error calling setExecutable(true) for file: " + file);
            prefs.putBoolean("reload_toolchain", true);
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
    prefs.putLong("toolchain-crc", Utility.crcTree(tmpExe));
    if (thread != null) {
      thread.start();
    }
  }

  private boolean discardChanges () {
    return doWarningDialog("Discard Changes?");
  }

  public void showErrorDialog (String msg) {
    ImageIcon icon = getImageIcon("images/warning-32x32.png");
    showMessageDialog(this, msg, "Error", JOptionPane.PLAIN_MESSAGE, icon);
  }

  public boolean doWarningDialog (String question) {
    ImageIcon icon = getImageIcon("images/warning-32x32.png");
    return JOptionPane.showConfirmDialog(this, question, "Warning", JOptionPane.YES_NO_OPTION,
      JOptionPane.WARNING_MESSAGE, icon) == JOptionPane.OK_OPTION;
  }

  private ImageIcon getImageIcon (String name) {
    URL loc = getClass().getResource(name);
    if (loc != null) {
      return new ImageIcon(loc);
    }
    return null;
  }

  public static void main (String[] args) {
    java.awt.EventQueue.invokeLater(() -> new MegaTinyIDE(args));
  }
}
