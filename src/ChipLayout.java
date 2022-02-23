import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/*
   *  This class is called by MarkupView to dynamically generate pinout images for the various members of the
   *  0-series and 1-series ATtiny chips
   */

public class ChipLayout {
  private static final Color    BACK = new Color(240, 240, 240);  // Background
  private static final Color    CHIP = new Color( 51,  51,  51);  // Chip body
  private static final Color    PINS = new Color(204, 204, 204);  // Pin
  private static final Color    PORT = new Color( 90, 127, 169);  // Port
  private static final Color    BDR  = new Color(  0,   3,  12);  // Border color
  private static final Color    DBG  = new Color(250, 207, 114);  // UPDI
  private static final Color    VCC  = new Color(168,  47,  17);  // Vcc pin
  private static final Color    SER  = new Color(159, 146, 254);  // RX, TX
  private static final Color    I2C  = new Color(128, 238, 238);  // SCL, SDA
  private static final Color    AIN  = new Color(252, 255,  81);  // Analog In (not yet implemented)
  private static final Color    DAC  = new Color( 99, 123,  80);  // Analog DAC
  private static final Color    SPI  = new Color(151, 169, 193);  // MISO, MOSI, SCK
  private static final Color    VRF  = new Color(202, 121, 217);  // VRef
  private static final Color    RST  = new Color(247, 153,  66);  // Reset
  private static final Color    WHT  = Color.white;
  private static final Color    BLK  = Color.black;                // Gnd pin
  private static final Font     BFNT = new Font("Helvetica", Font.PLAIN, 32);
  private static final Font     PFNT = new Font("Helvetica", Font.PLAIN, 22);
  private static final Map<String,ColorSet> colors = new HashMap<>();
  private static final int[]    COLS = new int[] {     35,    55,     45,    60,    60,   60};    // Width of lbl by column
  private static final String[] DCLR = new String[] {"PIN", "PORT", "AIN", "PIN", "PIN", "PIN"};  // Default lbl color by column

  static class ColorSet {
    Color  txtClr, lblClr;

    ColorSet (Color txtClr, Color lblClr) {
      this.txtClr = txtClr;
      this.lblClr = lblClr;
    }
  }

  static {
    colors.put("PIN",  new ColorSet(BLK, PINS));
    colors.put("PORT", new ColorSet(WHT, PORT));
    colors.put("UPDI", new ColorSet(BLK, DBG));
    colors.put("RST",  new ColorSet(BLK, RST));
    colors.put("VCC",  new ColorSet(WHT, VCC));
    colors.put("GND",  new ColorSet(WHT, BLK));
    colors.put("CLKI", new ColorSet(BLK, WHT));
    colors.put("RXD",  new ColorSet(BLK, SER));
    colors.put("TXD",  new ColorSet(BLK, SER));
    colors.put("DAC",  new ColorSet(WHT, DAC));
    colors.put("SDA",  new ColorSet(BLK, I2C));
    colors.put("SCL",  new ColorSet(BLK, I2C));
    colors.put("SCK",  new ColorSet(BLK, SPI));
    colors.put("MISO", new ColorSet(BLK, SPI));
    colors.put("MOSI", new ColorSet(BLK, SPI));
    colors.put("VREF", new ColorSet(BLK, VRF));
    colors.put("SS",   new ColorSet(BLK, SPI));
    colors.put("AIN",  new ColorSet(BLK, AIN));
  }

  static class DrawShape {
    Shape   shape;
    Color   color;
    boolean fill;

    DrawShape(Shape shape, Color color, boolean fill) {
      this.shape = shape;
      this.color = color;
      this.fill = fill;
    }
  }

  static class PadBox {
    String          text;
    List<DrawShape> drawShapes = new ArrayList<>();
    Rectangle2D     bounds = new Rectangle2D.Double();

    PadBox (String text, Font font, Color fontColor, Color background, double wid, double hyt, double angle, double xLoc, double yLoc) {
      try {
        boolean overbar = text.startsWith("~");
        this.text = text = overbar ? text.substring(1) : text;
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        GlyphVector gv = font.createGlyphVector(g2.getFontRenderContext(), text);
        Shape glyph = gv.getOutline();
        Rectangle2D bounds = glyph.getBounds2D();
        double rounding = Math.min(bounds.getHeight() * .2, 20);
        Shape rRect = new RoundRectangle2D.Double(0, 0, wid, hyt, rounding, rounding);
        addShape(rRect, background, true, angle, xLoc, yLoc);
        addShape(rRect, BDR, false, angle, xLoc, yLoc);
        if (overbar) {
          double width = bounds.getWidth();
          Area temp = new Area(glyph);
          temp.add(new Area(new Rectangle2D.Double(1, -bounds.getHeight() - 3, width + 1, 1.5)));
          glyph = temp;
        }
        addShape(glyph, fontColor, true, angle, xLoc, yLoc);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    public void addShape (Shape shape, Color color, boolean fill, double angle, double xLoc, double yLoc) {
      Rectangle2D bounds = shape.getBounds2D();
      double xShift = (-bounds.getWidth() / 2) - bounds.getX();
      double yShift = (-bounds.getHeight() / 2) - bounds.getY();
      AffineTransform center = AffineTransform.getTranslateInstance(xShift, yShift);
      Shape centered = center.createTransformedShape(shape);
      AffineTransform rotate = AffineTransform.getRotateInstance(Math.toRadians(angle));
      Shape rotated = rotate.createTransformedShape(centered);
      AffineTransform position = AffineTransform.getTranslateInstance(xLoc, yLoc);
      Shape positioned = position.createTransformedShape(rotated);
      this.bounds.add(positioned.getBounds2D());
      DrawShape  drawShape = new DrawShape(positioned, color, fill);
      drawShapes.add(drawShape);
    }

    public Rectangle2D getBounds () {
      return bounds;
    }

    private void drawPad (DrawSpace ds, int cx, int cy) {
      Graphics2D g2 = ds.g2;
      for (DrawShape drawShape : drawShapes) {
        g2.setColor(drawShape.color);
        AffineTransform position = AffineTransform.getTranslateInstance(cx, cy);
        Shape positioned = position.createTransformedShape(drawShape.shape);
        if (drawShape.fill) {
          g2.fill(positioned);
        } else {
          g2.draw(positioned);
          Rectangle rect = positioned.getBounds();
          ds.addHoverText(rect, text);
        }
      }
    }
  }

  static class HoverText {
    Rectangle rect;
    String    text;

    HoverText (Rectangle rect,  String text) {
      this.rect = rect;
      this.text = text;
    }
  }

  static class DrawSpace {
     BufferedImage    img;
     Graphics2D       g2;
     List<HoverText>  hoverList = new ArrayList<>();
     static HashMap<String,String> pins = new HashMap<>();

     static {
       pins.put("VCC",  "Operating Voltage");
       pins.put("GND",  "Ground");
       pins.put("DAC",  "Digital to Analog Converter Output");
       pins.put("MISO", "SPI Master In Slave Out");
       pins.put("MOSI", "SPI Master Out Slave In");
       pins.put("SCK",  "SPI Clock In/Out");
       pins.put("SS",   "SPI Slave Select (active low)");
       pins.put("RXD",  "USART Serial Input");
       pins.put("TXD",  "USART Serial Output");
       pins.put("SCL",  "TWI Clock");
       pins.put("SDA",  "TWI Data In/Out");
       pins.put("UPDI", "Unified Program and Debug Interface");
       pins.put("CLKI", "External Clock Input");
       pins.put("RST",  "Reset Input (active low)");
       pins.put("VREF", "External Voltage Reference");
     }

     DrawSpace (int wid, int hyt) {
      img = new BufferedImage(wid, hyt, BufferedImage.TYPE_INT_RGB);
      g2 = img.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(BACK);
      g2.fillRect(0, 0, wid, hyt);
    }

    void addHoverText (Rectangle rect, String text) {
       if (text.matches("P[ABC][0-7]")) {
         text = text + ": Port " + text.charAt(1) + ", Bit " + text.charAt(2);
       } else if (text.matches("A[0-9]")) {
         text = text + ": Analog Input, Channel " + text.substring(1);
       } else if (text.matches("^\\d+$")) {
         text = "Pin " + text;
       } else if (pins.containsKey(text)) {
         text = text + ": " + pins.get(text);
       }
       String html = "<html><body><p style=\"font-family:Courier;font-size:24\">" + text + "</p></body></html>";
      hoverList.add(new HoverText(rect, html));
    }
  }

  private static ColorSet getColorSet (String[] pairs, String def) {
    if (pairs.length > 1) {
      return colors.get(pairs[1]);
    } else if (colors.containsKey(pairs[0])) {
      return colors.get(pairs[0]);
    }
    return colors.get(def);
  }

  private static DrawSpace getSoic (String label, int pins, String[] pn, boolean hasDacs) {
    List<PadBox> pads = new ArrayList<>();
    int spacing = 35;
    int bodyHyt = (pins / 2) * spacing + spacing / 2;
    int bodyWid = 180;
    int gap = 4;
    int bHyt = 30;
    int sOff = (pins * spacing) / 4 - spacing / 2;
    pads.add(new PadBox(label, BFNT, WHT, CHIP, bodyHyt, bodyWid, -90, 0, 0));
    int[] cols = new int[COLS.length];
    int base = bodyWid / 2;
    for (int ii = 0; ii < COLS.length; ii++) {
      cols[ii] = base + COLS[ii] / 2;
      base += COLS[ii] + gap;
    }
    for (int pin = 1, rp = pins; pin <= pins; pin++, rp--) {
      boolean left = pin <= pins / 2;
      int yOff = left ? (pin - 1) * spacing - sOff : sOff - (pin - pins / 2 - 1) * spacing;
      String[] tmp = pn[pin - 1].split("/");
      String[] parts = new String[tmp.length + 1];
      parts[0] = pin + ":PIN";
      System.arraycopy(tmp, 0, parts, 1, tmp.length);
      // Draw pin labels (physical, logical, special)
      for (int ii = 0; ii < parts.length; ii++) {
        int col = cols[ii];
        int wid = COLS[ii];
        int xOff = left ? -col : col;
        String[] pairs = parts[ii].split(":");
        String pLbl = pairs[0];
        if (pLbl.length() > 0 && (!"DAC".equals(pLbl) || hasDacs)) {
          ColorSet cSet = getColorSet(pairs, DCLR[ii]);
          pads.add(new PadBox(pLbl, PFNT, cSet.txtClr, cSet.lblClr, wid, bHyt, 0, xOff, yOff));
        }
      }
    }
    return getDrawSpace(pads);
  }

  private static DrawSpace getVqfn (String label, int pins, String[] pn, boolean hasDacs) {
    List<PadBox> pads = new ArrayList<>();
    int spacing = 35;
    int bodyHyt = (pins / 4) * spacing + spacing / 2;
    int bodyWid = bodyHyt;
    int gap = 4;
    int bHyt = 30;
    int sOff = (pins * spacing) / 8 - spacing / 2;
    int xOff, yOff, rotate;
    // Draw VQFN-20 package body
    pads.add(new PadBox(label, BFNT, WHT, CHIP, bodyHyt, bodyWid, 0, 0, 0));
    int[] cols = new int[COLS.length];
    int base = bodyWid / 2;
    for (int ii = 0; ii < COLS.length; ii++) {
      cols[ii] = base + COLS[ii] / 2;
      base += COLS[ii] + gap;
    }
    for (int pin = 1; pin <= pins; pin++) {
      boolean left = pin <= pins / 4;
      boolean bottom = !left && pin <= pins / 2;
      boolean right = !bottom && pin <= pins - pins / 4;
      boolean top = !left && !right && !bottom;
      rotate = top || bottom ? -90 : 0;
      String[] tmp = pn[pin - 1].split("/");
      String[] parts = new String[tmp.length + 1];
      parts[0] = pin + ":PIN";
      System.arraycopy(tmp, 0, parts, 1, tmp.length);
      // Draw pin labels (physical, logical, special)
      for (int ii = 0; ii < parts.length; ii++) {
        int col = cols[ii];
        int wid = COLS[ii];
        String[] pairs = parts[ii].split(":");
        String pLbl = pairs[0];
        if (pLbl.length() > 0) {
          if (left) {
            xOff = -col;
            yOff = (pin - 1) * spacing - sOff;
          } else if (bottom) {
            xOff = (pin - pins / 4 - 1) * spacing - sOff;
            yOff = col;
          } else if (right) {
            xOff = col;
            yOff = sOff - (pin - pins / 2 - 1) * spacing;
          } else {
            xOff = sOff - (pin - pins / 2 - pins / 4 - 1) * spacing;
            yOff = -col;
          }
          if (!"DAC".equals(pLbl) || hasDacs) {
            ColorSet cSet = getColorSet(pairs, DCLR[ii]);
            pads.add(new PadBox(pLbl, PFNT, cSet.txtClr, cSet.lblClr, wid, bHyt, rotate, xOff, yOff));
          }
        }
      }
    }
    return getDrawSpace(pads);
  }

  private static DrawSpace getDrawSpace (List<PadBox> pads) {
    Rectangle2D outline = new Rectangle2D.Double(0, 0, 0, 0);
    for (PadBox pad : pads) {
      Rectangle2D bnds = pad.getBounds();
      outline.add(bnds);
    }
    int border = 10;
    int dx = (int) -outline.getX() + border;
    int dy = (int) -outline.getY() + border;
    int wid = (int) outline.getWidth() + border * 2;
    int hyt = (int) outline.getHeight() + border * 2;
    DrawSpace ds = new DrawSpace(wid, hyt);
    for (PadBox pad : pads) {
      pad.drawPad(ds, dx, dy);
    }
    return ds;
  }

  public static DrawSpace getLayout (String avrChip, String pkg) {
    MegaTinyIDE.ChipInfo info = MegaTinyIDE.getChipInfo(avrChip);
    String chipLabel = "AT" + avrChip.substring(2);
    boolean hasDacs = info.getInt("dacs") > 0;
    String[] pinNames = Utility.arrayFromText("pins/" + pkg + ".props");
    int pinCount = info.getInt("pins");
    if (pkg.toLowerCase().startsWith("soic")) {
      return getSoic(chipLabel, pinCount, pinNames, hasDacs);
    } else if (pkg.toLowerCase().startsWith("vqfn")) {
      return getVqfn(chipLabel, pinCount, pinNames, hasDacs);
    } else {
      return null;
    }
  }
}
