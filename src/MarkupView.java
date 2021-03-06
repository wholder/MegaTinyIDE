import com.github.rjeschke.txtmark.Processor;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.prefs.Preferences;

class MarkupView extends JPanel {
  private final JEditorPane           jEditorPane;
  private final ArrayList<StackItem>  stack = new ArrayList<>();
  private static final Font           codeFont = Utility.getCodeFont(12);
  private String                      basePath, currentPage;
  Map<String,String>                  parmMap;

  private static class StackItem {
    private final String  location, parms;
    private final Point   position;

    private StackItem (String location, Point position, String parms) {
      this.location = location;
      this.position = position;
      this.parms = parms;
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

  MarkupView (String loc, String parms) {
    this();
    loadMarkup(loc, parms);
  }

  MarkupView () {
    setLayout(new BorderLayout());
    jEditorPane = new JEditorPane();
    JScrollPane scrollPane = new JScrollPane(jEditorPane);
    JButton back = new JButton("<<BACK");
    jEditorPane.addHyperlinkListener(new HyperlinkListener() {
      private String tooltip;
      @Override
      public void hyperlinkUpdate(HyperlinkEvent ev) {
        JEditorPane editor = (JEditorPane) ev.getSource();
        if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          String link = ev.getDescription();
          if (link.startsWith("http://") || link.startsWith("https://")) {
            if (Desktop.isDesktopSupported()) {
              try {
                Desktop.getDesktop().browse(new URI(link));
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            }
          } else {
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
            stack.add(new StackItem(currentPage, scrollPosition, parms));
            loadMarkup(link, parms);
            if (anchor != null) {
              jEditorPane.scrollToReference(anchor);
            }
            SwingUtilities.invokeLater(() -> back.setVisible(stack.size() > 0));
          }
        } else if (ev.getEventType() == HyperlinkEvent.EventType.ENTERED) {
          tooltip = editor.getToolTipText();
          String text = ev.getDescription();
          editor.setToolTipText(text);
        } else if (ev.getEventType() == HyperlinkEvent.EventType.EXITED) {
          editor.setToolTipText(tooltip);
        }
      }
    });
    jEditorPane.setEditable(false);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    add(scrollPane, BorderLayout.CENTER);
    back.addActionListener(e -> {
      if (stack.size() > 0) {
        StackItem item = stack.remove(stack.size() - 1);
        loadMarkup(item.location, item.parms);
        parmMap = Utility.parseParms(item.parms);
        SwingUtilities.invokeLater(() -> {
          scrollPane.getViewport().setViewPosition(item.position);
          back.setVisible(stack.size() > 0);
        });
      }
    });
    add(back, BorderLayout.NORTH);
    back.setVisible(false);
    HTMLEditorKit kit = new MyEditorKit();
    jEditorPane.setEditorKit(kit);
    // Setup some basic markdown styles (Note: limited to HTML 3.2)
    // see: https://stackoverflow.com/questions/25147141/why-isnt-my-css-working-right-in-java
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
    styleSheet.addRule("code {font-family: " + codeFont + "; font-size: 12px; margin-bottom: 3px;}");
    styleSheet.addRule("p {font-size: 12px; margin-top: 5px; margin-bottom: 5px;}");
  }

  public void setText (String markup) {
    String html = Processor.process(markup);
    jEditorPane.setText(html);
  }

  public void loadMarkup (String loc, String parms) {
    if (loc != null) {
      if (basePath == null) {
        int idx = loc.lastIndexOf("/");
        if (idx >= 0) {
          basePath = loc.substring(0, idx + 1);
          loc = loc.substring(idx + 1);
        } else {
          basePath = "";
        }
      }
      try {
        String markup = new String(getResource(basePath + loc));
        parmMap = Utility.parseParms(parms);
        markup = Utility.replaceTags(markup, parmMap, (name, parm, tags) -> {
          switch (name) {
          case "PARM":
            return parm;
          case "TAG":
            return tags.get(parm);
          case "INFO":
            String[] parts = parm.split("-");
            if (parts.length > 1) {
              MegaTinyIDE.ChipInfo info = MegaTinyIDE.getChipInfo(parts[0]);
              if (info != null) {
                String tmp = info.get(parts[1]);
                if (tmp != null) {
                  return tmp;
                }
                return "INFO tag parameter \"" + parts[1] + "\" undefined";
              }
            }
            return "malformed INFO tag parameter \"" + parm + "\"";
          }
          return "callback tag \"" + name + "\" undefined";
        });
        setText(markup);
        currentPage = loc;
        jEditorPane.setCaretPosition(0);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  private byte[] getResource (String file) throws IOException {
    InputStream fis = MarkupView.class.getClassLoader().getResourceAsStream(file);
    if (fis != null) {
      byte[] data = new byte[fis.available()];
      if (fis.read(data) != data.length) {
        throw new IOException("getResource() not all bytes read from file: " + file);
      }
      fis.close();
      return data;
    }
    throw new IllegalStateException("MarkupView.getResource() " + file + " not found");
  }

  /*
   *  Test code for MarkupView Pane
   */
  public static void main (String[] args) {
    Preferences prefs = Preferences.userRoot().node(MarkupView.class.getName());
    JFrame frame = new JFrame();
    MarkupView mView = new MarkupView("documentation/index.md", null);
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
