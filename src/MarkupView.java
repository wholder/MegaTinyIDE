import com.github.rjeschke.txtmark.Processor;

// Setup some basic markdown styles (Note: limited to HTML 3.2)
// see: https://stackoverflow.com/questions/25147141/why-isnt-my-css-working-right-in-java
    /*
      Supported CSS:
        background
        background-color (with the exception of transparent)
        background-image
        background-position
        background-repeat
        border-bottom-color
        border-bottom-style
        border-color
        border-left-color
        border-left-style
        border-right-color
        border-right-style
        border-style (only supports inset, outset and none)
        border-top-color
        border-top-style
        color
        font
        font-family
        font-size (supports relative units)
        font-style
        font-weight
        list-style-image
        list-style-position
        list-style-type
        margin
        margin-bottom
        margin-left
        margin-right
        margin-top
        padding
        padding-bottom
        padding-left
        padding-right
        padding-top
        text-align (justify is treated as center)
        text-decoration (with the exception of blink and overline)
        vertical-align (only sup and super)
     */

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MarkupView extends JPanel {
  private final JEditorPane           jEditorPane;
  private final JScrollPane           scrollPane;
  private final ArrayList<StackItem>  stack = new ArrayList<>();
  static final Font                   codeFont = Utility.getCodeFont(12);
  private static final Pattern        BracesMatch = Pattern.compile("\\[(.*)]");
  private String                      basePath;
  private HtmlTable                   regTable;
  private int                         regIndex;
  Map<String,String>                  parmMap;

  private static class StackItem {
    private final String  location;
    private final Point   position;

    private StackItem (String location, Point position) {
      this.location = location;
      this.position = position;
    }
  }

  public static class HTMLButton extends JButton {
    public HTMLButton () {
      super("JButton");
    }
  }

  class MyImageView extends View {
    private String                loc;
    private Image                 img;
    private ChipLayout.DrawSpace  ds;

    private MyImageView (Element elem) {
      super(elem);
      try {
        AttributeSet attributes = elem.getAttributes();
        loc = URLDecoder.decode((String) attributes.getAttribute(HTML.Attribute.SRC), "UTF-8");
      } catch (UnsupportedEncodingException ex) {
        ex.printStackTrace();
      }
    }

    /**
     * This is needed to draw the BufferedImage to the screen, otherwise only images loaded by the
     * superclass will be displayed...
     * @param g Graphics context
     * @param allocation bounds for drawing the image on the JEditorPane
     */
    @Override
    public void paint (Graphics g, Shape allocation) {
      Rectangle2D bnds = allocation.getBounds2D();
      g.drawImage(img, (int) bnds.getX(), (int) bnds.getY(), null);
    }

    public URL getImageURL () {
      if (loc.startsWith("/")) {
        return getClass().getResource(loc);
      }
      return getClass().getResource(basePath + loc);
    }

    public Image getImage (URL url) {
      // Check if image was already loaded
      if (img != null) {
        return img;
      } else if (loc.startsWith("chiplayout:")) {                                   // chiplayout
        this.ds = ChipLayout.getLayout(parmMap.get("CHIP"), parmMap.get("PKG"));
        return img = ds != null ? ds.img : null;
      } else if (loc.startsWith("bitfield:")) {                                     // bitfield
        return img = Diagrams.drawBitfield(loc.substring(9), parmMap);
      } else {
        Image image = Toolkit.getDefaultToolkit().createImage(url);
        if (image != null) {
          // Force the image to be loaded by using an ImageIcon.
          ImageIcon ii = new ImageIcon();
          ii.setImage(image);
        }
        return img = image;
      }
    }

    @Override
    public String getToolTipText (float x, float y, Shape shape) {
      if (ds != null && ds.hoverList.size() > 0 && shape instanceof Rectangle) {
        Rectangle rect = (Rectangle) shape;
        int xLoc = (int) x - rect.x;
        int yLoc = (int) y - rect.y;
        for (ChipLayout.HoverText ht : ds.hoverList) {
          if (ht.rect.contains(xLoc, yLoc)) {
            return ht.text;
          }
        }
      }
      return super.getToolTipText(x, y, shape);
    }

    /**
     * Determines the preferred span for this view along anc axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the span the view would like to be rendered into;
     * typically the view is told to render into the span
     * that is returned, although there is no guarantee;
     * the parent may choose to resize or break the view
     */
    public float getPreferredSpan (int axis) {
      Image newImage = getImage(getImageURL());
      switch (axis) {
      case View.X_AXIS:
        return newImage.getWidth(null);
      case View.Y_AXIS:
        return newImage.getHeight(null);
      default:
        throw new IllegalArgumentException("Invalid axis: " + axis);
      }
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert
     * @param a   the allocated region to render into
     * @return the bounding box of the given position
     * @see View#modelToView
     */
    public Shape modelToView (int pos, Shape a, Position.Bias b) {
      int p0 = getStartOffset();
      int p1 = getEndOffset();
      if ((pos >= p0) && (pos <= p1)) {
        Rectangle r = a.getBounds();
        if (pos == p1) {
          r.x += r.width;
        }
        r.width = 0;
        return r;
      }
      return null;
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     * given point of view
     * @see View#viewToModel
     */
    public int viewToModel (float x, float y, Shape a, Position.Bias[] bias) {
      Rectangle alloc = (Rectangle) a;
      if (x < alloc.x + alloc.width) {
        bias[0] = Position.Bias.Forward;
        return getStartOffset();
      }
      bias[0] = Position.Bias.Backward;
      return getEndOffset();
    }
  }

  class MyViewFactory implements ViewFactory {
    ViewFactory view;

    private MyViewFactory (ViewFactory view) {
      this.view = view;
    }

    public View create (Element elem) {
      AttributeSet attrs = elem.getAttributes();
      Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
      Object obj = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
      if (obj instanceof HTML.Tag && obj == HTML.Tag.IMG) {
        return new MyImageView(elem);
      }
      return view.create(elem);
    }
  }

  class MyEditorKit extends HTMLEditorKit {
    @Override
    public ViewFactory getViewFactory () {
      return new MyViewFactory(super.getViewFactory());
    }
  }

  MarkupView (MegaTinyIDE ide, String loc) {
    this(ide);
    loadMarkup(loc);
  }

  static class HtmlTable {
    private static final String   grid = "black";
    private static final String   blank = "#C0C0C0";
    private static final String   color = "#606060";
    private static final String   header = "#808080";
    List<List<String>>            rows = new ArrayList<>();
    private final String          font;
    private final int             fontSize;
    private final String[]        widths;
    private int                   maxCol;

    HtmlTable (String font, int fontSize, String[] widths) {
      this.font = font;
      this.fontSize = fontSize;
      this.widths = widths;
    }

    void addItem (int row, int col, String text) {
      maxCol = Math.max(maxCol, col + 1);
      while (rows.size() <= row) {
        rows.add(new ArrayList<>());
      }
      for (int ii = 0; ii <= row; ii++) {
        List<String> line = rows.get(ii);
        while (line.size() <= col) {
          line.add("");
        }
      }
      rows.get(row).set(col, text);
    }

    private String decodeHover (String text) {
      // If present, convert hover text into unmarked <a> tag with "title" attribute
      Matcher mat = BracesMatch.matcher(text);
      if (mat.find()) {
        String hover = mat.group(1);
        text = mat.replaceAll("").trim();
        text = "<a style=\"color:" + color + ";text-decoration:none\" title=\"" + hover + "\" href=\"\">" + text + "</a>";
      }
      return text;
    }

    String getHtmlTable () {
      // pad out all rows to same size
      for (java.util.List<String> row : rows) {
        while (row.size() < maxCol) {
          row.add("");
        }
      }
      // generate html table
      StringBuilder buf = new StringBuilder("<table style=\"background-color:" + grid + ";padding:0;white-space:nowrap;width:95%;\">\n");
      String prefix1 = "  <td style=\"background-color:white;color:" + color + ";\"";
      String prefix2 = "  <td style=\"background-color:" + blank + ";color:" + blank + ";\"";
      for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
        List<String> row = rows.get(rowIdx);
        buf.append(" <tr style=\"font-family:" + font + ";font-size:" + fontSize + ";text-align:center;\">\n");
        int cols = 0;
        for (int colIdx = 0; colIdx < row.size(); colIdx++) {
          String text = row.get(colIdx);
          text = text.trim();
          if (text.length() == 0) {
            text = "&nbsp;";
          }
          if (rowIdx == 0) {
            // Draw header row
            if (widths != null) {
              String width = widths[colIdx];
              buf.append("  <th style=\"background-color:" + header + ";color:white;\" width=\"" + width + "\">" + text + "</th>\n");
            } else {
              buf.append("  <th style=\"background-color:" + header + ";color:white;\">" + text + "</th>\n");
            }
          } else {
            // Draw data rows
            int idx = text.indexOf("|");
            if (idx > 0) {
              // Draw spanning cell
              String span = text.substring(0, idx);
              text = text.substring(idx + 1);
              text = decodeHover(text);
              buf.append(prefix1 + " colspan=\"" + span + "\">" + text + "</td>\n");
              cols += Integer.parseInt(span);
            } else {
              // Draw single cell
              if ("-".equals(text)) {
                buf.append(prefix2 + ">&nbsp;</td>\n");
              } else {
                text = decodeHover(text);
                buf.append(prefix1 + ">" + text + "</td>\n");
              }
              cols++;
            }
            if (cols >= row.size()) {
              break;
            }
          }
        }
        buf.append(" </tr>\n");
      }
      buf.append("</table>\n");
      return buf.toString();
    }
  }

  MarkupView (MegaTinyIDE ide) {
    setLayout(new BorderLayout());
    jEditorPane = new JEditorPane();
    scrollPane = new JScrollPane(jEditorPane);
    JButton back = new JButton("<<BACK");
    jEditorPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent ev) {
        if (ev instanceof FormSubmitEvent) {
          // Process form submit GET
          FormSubmitEvent fEvent = (FormSubmitEvent) ev;
          String file = fEvent.getData();
          try {
            file = java.net.URLDecoder.decode(file, "UTF-8");
            int idx = file.indexOf("=");
            if (idx > 0) {
              String type = file.substring(0, idx);
              file = file.substring(idx + 1);
              if ("file".equals(type)) {
                String src = new String(Utility.getResource(file));
                ide.setSource(src);
              } else if ("block".equals(type)) {
                ide.setSource(file);
              }
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        } else {
          HyperlinkEvent.EventType eventType = ev.getEventType();
          String link = ev.getDescription();
          JEditorPane editor = (JEditorPane) ev.getSource();
          if (eventType == HyperlinkEvent.EventType.ACTIVATED) {
            if (link.length() == 0) {
              return;
            }
            if (link.startsWith("http://") || link.startsWith("https://")) {
              // Handle link using external browser
              if (Desktop.isDesktopSupported()) {
                try {
                  Desktop.getDesktop().browse(new URI(link));
                } catch (Exception ex) {
                  ex.printStackTrace();
                }
              }
            } else {
              // Handle link in MarkupView
              loadMarkup(link);
              SwingUtilities.invokeLater(() -> back.setVisible(stack.size() > 1));
            }
          } else if (eventType == HyperlinkEvent.EventType.ENTERED) {
            Element source = ev.getSourceElement();
            if (source instanceof HTMLDocument.RunElement) {
              HTMLDocument.RunElement elem = (HTMLDocument.RunElement) source;
              AttributeSet set = elem.getAttributes();
              Enumeration<?> ee1 = set.getAttributeNames();
              while (ee1.hasMoreElements()) {
                Object name = ee1.nextElement();
                Object attr = set.getAttribute(name);
                if (attr instanceof SimpleAttributeSet) {
                  SimpleAttributeSet tagAttrs = (SimpleAttributeSet) attr;
                  Enumeration<?> ee2 = tagAttrs.getAttributeNames();
                  while (ee2.hasMoreElements()) {
                    Object tagElem = ee2.nextElement();
                    // If tag has "title" attribute, display value as hover text
                    if ("title".equals(tagElem.toString())) {
                      Object tagVal = tagAttrs.getAttribute(tagElem);
                      editor.setToolTipText((String) tagVal);
                      return;
                    }
                  }
                }
              }
            }
          } else if (eventType == HyperlinkEvent.EventType.EXITED) {
            // Turn off tooltip
            editor.setToolTipText(null);
          }
        }
      }
    });
    jEditorPane.setEditable(false);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    add(scrollPane, BorderLayout.CENTER);
    back.addActionListener(e -> {
      if (stack.size() > 0) {
        // Process "BACK" button
        stack.remove(stack.size() - 1);
        StackItem item = stack.get(stack.size() - 1);
        loadMarkup(item.location);
        stack.remove(stack.size() - 1);
        SwingUtilities.invokeLater(() -> {
          scrollPane.getViewport().setViewPosition(item.position);
          back.setVisible(stack.size() > 1);
        });
      }
    });
    add(back, BorderLayout.NORTH);
    back.setVisible(false);
    HTMLEditorKit kit = new MyEditorKit();
    kit.setAutoFormSubmission(false);
    jEditorPane.setEditorKit(kit);
    StyleSheet styleSheet = kit.getStyleSheet();
    styleSheet.addRule("body {color:#000; font-family: Arial, DejaVu Sans, Helvetica; margin: 4px;}");
    styleSheet.addRule("h1 {font-size: 24px; font-weight: 500;}");
    styleSheet.addRule("h2 {font-size: 20px; font-weight: 500;}");
    styleSheet.addRule("h3 {font-size: 16px; font-weight: 500;}");
    styleSheet.addRule("h4 {font-size: 14px; font-weight: 500;}");
    styleSheet.addRule("h5 {font-size: 12px; font-weight: 500;}");
    styleSheet.addRule("h6 {font-size: 10px; font-weight: 500;}");
    styleSheet.addRule("pre {margin-left: 0.5cm;}");
    styleSheet.addRule("ol {margin-left: 1cm;}");
    styleSheet.addRule("ol, li {font-size: 12px; padding-bottom: 6px;}");
    styleSheet.addRule("ul, li {font-size: 12px; padding-bottom: 6px;}");
    //styleSheet.addRule("code {font-family: " + codeFont + "; font-size: 12px; margin-bottom: 3px;}"); // doesn't work?
    styleSheet.addRule("p {font-size: 12px; margin-top: 5px; margin-bottom: 5px;}");
  }

  public void setText (String markup) {
    String html = Processor.process(markup);
    jEditorPane.setText(html);
  }

  public void loadMarkup (String loc) {
    if (loc != null) {
      String link = loc;
      // Check for anchor in link
      String anchor = null;
      int off = link.indexOf("#");
      if (off >= 0) {
        anchor = link.substring(off + 1);
        link = link.substring(0, off);
      }
      // Check for parameters on link
      String parms = null;
      off = link.indexOf("?");
      if (off >= 0) {
        parms = link.substring(off + 1);
        link = link.substring(0, off);
      }
      Point scrollPosition = scrollPane.getViewport().getViewPosition();
      stack.add(new StackItem(loc, scrollPosition));
      if (basePath == null || link.lastIndexOf("/") > 0) {
        int idx = link.lastIndexOf("/");
        if (idx >= 0) {
          basePath = link.substring(0, idx + 1);
          link = link.substring(idx + 1);
        } else {
          basePath = "";
        }
      }
      try {
        String markup = new String(Utility.getResource(basePath + link));
        parmMap = Utility.parseParms(parms);
        markup = Utility.replaceTags(markup, parmMap, (name, parm, tags) -> {
          switch (name) {
          case "PARM":
            return parm;
          case "TAG":
            return tags.get(parm);
          case "CODE_BLOCK":
            String block = Utility.escapeHTML(parm.trim());
            StringBuilder buf1 = new StringBuilder();
            buf1.append("<form action=\"submit\"><input type=\"submit\" value=\"Open Code in Editor\"/>\n");
            buf1.append("<input type=\"hidden\" name=\"block\" value=\"");
            buf1.append(block);
            buf1.append("\"\"></form>\n");
            buf1.append("<pre style=\"background-color:#E0E0E0;overflow:auto;tab-size:2;\">\n");
            buf1.append(block);
            buf1.append("</pre>\n");
            return buf1.toString();
          case "CODE_FILE":
            // Based on: https://www.sitepoint.com/everything-need-know-html-pre-element/
            StringBuilder buf2 = new StringBuilder();
            try {
              String file = new String(Utility.getResource(parm));
              buf2.append("<form action=\"submit\"><input type=\"submit\" value=\"Open Code in Editor\"/>\n");
              buf2.append("<input type=\"hidden\" name=\"file\" value=\"");
              buf2.append(parm);
              buf2.append("\"\"></form>\n");
              buf2.append("<pre style=\"background-color:#E0E0E0;overflow:auto;tab-size:2;\">\n");
              file = Utility.escapeHTML(file);
              buf2.append(file);
            } catch (IOException ex) {
              buf2.append("Unable to load CODE file: " + parm + "\n");
            }
            buf2.append("</pre>\n");
            return buf2.toString();
          case "DEV":
            // Conditional tag for developer (me) use only.  Usage: *[DEV:dev feature markup]*
            if (MegaTinyIDE.isDeveloper()) {
              return parm;
            }
            return "";
          case "INT_VECS":
            // Generate Interrupt Vector Mapping Table
            MegaTinyIDE.ChipInfo info2 = MegaTinyIDE.getChipInfo(parm);
            String vecSet = info2.get("vecs");
            int baseAdd = info2.getInt("fbase");
            try {
              Map<String, String> vecs = Utility.getResourceMap("interrupts/vec" + vecSet + ".props");
              String[] widths = {"5%", "15%", "15%", "65%",};
              HtmlTable vecTable = new HtmlTable(codeFont.getName(), 13, widths);
              vecTable.addItem(0, 0, "Vec #");
              vecTable.addItem(0, 1, "Offset");
              vecTable.addItem(0, 2, "Address");
              vecTable.addItem(0, 3, "Peripheral Source");
              int idx = 0;
              for (String key : vecs.keySet()) {
                String val = vecs.get(key).trim();
                if (val.length() == 0) {
                  val = "--";
                }
                vecTable.addItem(idx + 1, 0, String.format("%d", idx));
                vecTable.addItem(idx + 1, 1, String.format("0x%04X", (idx * 2)));
                vecTable.addItem(idx + 1, 2, String.format("0x%04X", (idx * 2 + baseAdd)));
                vecTable.addItem(idx + 1, 3, val);
                idx++;
              }
              return vecTable.getHtmlTable();
            } catch (IOException ex) {
              return "INT_VECS tag parameter: unable to generate table";
            }
          case "BEGIN_REGS":
            String[] widths = {"10%", "15%", "3%", "9%", "9%", "9%", "9%", "9%", "9%", "9%", "9%"};
            regTable = new HtmlTable(codeFont.getName(), 13, widths);
            regTable.addItem(0, 0, "Address");
            regTable.addItem(0, 1, "Register");
            regTable.addItem(0, 2, "");
            for (int ii = 0; ii < 8; ii++) {
              regTable.addItem(0, ii + 3, "Bit " + (7 - ii));
              regTable.addItem(1, ii + 3, "");
            }
            regIndex = 1;
            return "";
          case "END_REGS":
            if (regTable != null) {
              return regTable.getHtmlTable();
            }
            return "";
          case "REG_ITEM":
            if (parm != null) {
              String[] items = parm.split(",");
              for (int ii = 0; ii < items.length; ii++) {
                String text = items[ii];
                regTable.addItem(regIndex, ii, text);
              }
            }
            regIndex++;
            return "";
          case "MUX_TABLE":
            // Generate table of multiplexed pins
            try {
              if (parm != null && parm.trim().length() > 0) {
                String pkg = tags.get("PKG").toLowerCase();
                HtmlTable tbl = new HtmlTable(codeFont.getName(), 12, null);
                Map<String,String> mux_pins = Utility.getResourceMap("pins/chip_features.props");
                Map<String,String> peripherals = Utility.getResourceMap("pins/peripherals.props");
                String avrChip = mux_pins.get(tags.get("CHIP"));
                String[] features = avrChip.split(",");
                Map<String,Integer> colNames = new HashMap<>();
                for (int ii = 0; ii < features.length; ii++) {
                  String colName = features[ii];
                  colNames.put(colName, ii);
                  if (peripherals.containsKey(colName)) {
                    String url = peripherals.get(colName);
                    colName = "<a style=\"color:white\" href=\"" + url + "\">" + colName + "</a>";
                  }
                  tbl.addItem(0, ii, colName);
                }
                String[] pins = Utility.arrayFromText("pins/" + pkg + ".props");
                Map<String,String> ports = Utility.getResourceMap("pins/mux_pins.props");
                int row = 1;
                for (int ii = 0; ii < pins.length; ii++) {
                  String[] pin = pins[ii].split("/");
                  if (pin.length > 1) {
                    String pinNum = Integer.toString(ii + 1);
                    String pinName = pin[0];
                    tbl.addItem(row, 0, pinNum);
                    tbl.addItem(row, 1, pinName);
                    String altPins = ports.get(pinName);
                    if (altPins != null) {
                      String[] pairs = altPins.split(",");
                      for (String str : pairs) {
                        String[] pair = str.split("\\.");
                        String colName = pair[0];
                        if (colNames.containsKey(colName)) {
                          int colIndex = colNames.get(colName);
                          tbl.addItem(row, colIndex, pair[1]);
                        }
                      }
                    }
                    row++;
                  }
                }
                return tbl.getHtmlTable();
              } else {
                return "Alternate pin configurations available using Port Multiplexer not shown.  See datasheet for details.";
              }
            } catch (IOException ex) {
              return "MUX_TABLE tag parameter: unable to generate table";
            }
          case "INFO":
            String[] parts = parm.split("-");
            if (parts.length > 1) {
              String type = parts[1];
              MegaTinyIDE.ChipInfo info = MegaTinyIDE.getChipInfo(parts[0]);
              if (info != null) {
                String val = info.get(type);
                if (val != null) {
                  if ("sig".equals(type) && val.length() == 6) {
                    val = "0x" + val.substring(0, 2) + ", 0x" + val.substring(2, 4) + ", 0x" + val.substring(4);
                  }
                  return val;
                }
                return "INFO tag parameter \"" + type + "\" undefined";
              }
            }
            return "malformed INFO tag parameter \"" + parm + "\"";
          }
          return "callback tag \"" + name + "\" undefined";
        });
        setText(markup);
        jEditorPane.setCaretPosition(0);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      // Position to anchor on page (if defined)
      if (anchor != null) {
        jEditorPane.scrollToReference(anchor);
      }
    }
  }

  /*
   *  Test code for MarkupView Pane
   */
  public static void main (String[] args) {
    Preferences prefs = Preferences.userRoot().node(MarkupView.class.getName());
    JFrame frame = new JFrame();
    MarkupView mView = new MarkupView(null, "documentation/index.md");
    frame.add(mView, BorderLayout.CENTER);
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.setSize(prefs.getInt("window.width", 800), prefs.getInt("window.height", 900));
    frame.setLocation(prefs.getInt("window.x", 10), prefs.getInt("window.y", 10));
    // Add window close handler
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing (WindowEvent ev) {
        System.exit(0);
      }
    });
    // Track window resize/move events and save in prefs
    frame.addComponentListener(new ComponentAdapter() {
      public void componentMoved (ComponentEvent ev) {
        Rectangle bounds = ev.getComponent().getBounds();
        prefs.putInt("window.x", bounds.x);
        prefs.putInt("window.y", bounds.y);
      }

      public void componentResized (ComponentEvent ev) {
        Rectangle bounds = ev.getComponent().getBounds();
        prefs.putInt("window.width", bounds.width);
        prefs.putInt("window.height", bounds.height);
      }
    });
    frame.setVisible(true);
  }
}
