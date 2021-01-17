import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private static final String[] SO8 = {"VCC",                     // Pin 1
                                       "PA6/A6/TXD/DAC",          // Pin 2 - A6
                                       "PA7/A7/RXD",              // Pin 3 - A7
                                       "PA1/A1/SDA/MOSI",         // Pin 4 - A1
                                       "PA2/A2/SCL/MISO",         // Pin 5 - A2
                                       "PA0/A0/RST/UPDI",         // Pin 6 - A0
                                       "PA3/A3/CLKI/SCK",         // Pin 7 - A3
                                       "GND"};                    // Pin 8
  private static final String[] SO14 = {"VCC",                    // Pin 1
                                        "PA4/A4/SS",              // Pin 2 - A4
                                        "PA5/A5/VREF",            // Pin 3 - A5
                                        "PA6/A6/DAC",             // Pin 4 - A6
                                        "PA7/A7",                 // Pin 5 - A7
                                        "PB3//RXD",               // Pin 6
                                        "PB2//TXD",               // Pin 7
                                        "PB1/A10/SDA",            // Pin 8 - A10
                                        "PB0/A11/SCL",            // Pin 9 - A11
                                        "PA0/A0/RST/UPDI",        // Pin 10 - A0
                                        "PA1/A1/MOSI",            // Pin 11 - A1
                                        "PA2/A2/MISO",            // Pin 12 - A2
                                        "PA3/A3/SCK/CLKI",        // Pin 13 - A3
                                        "GND"};                   // Pin 14
  private static final String[] SO20 = {"VCC",                    // Pin 1
                                        "PA4/A4/SS",              // Pin 2 - A4
                                        "PA5/A5/VREF",            // Pin 3 - A5
                                        "PA6/A6/DAC",             // Pin 4 - A6
                                        "PA7/A7",                 // Pin 5 - A7
                                        "PB5/A8",                 // Pin 6 - A8
                                        "PB4/A9",                 // Pin 7 - A9
                                        "PB3//RXD",               // Pin 8
                                        "PB2//TXD",               // Pin 9
                                        "PB1/A10/SDA",            // Pin 10 - A10
                                        "PB0/A11/SCL",            // Pin 11 - A11
                                        "PC0",                    // Pin 12
                                        "PC1",                    // Pin 13
                                        "PC2",                    // Pin 14
                                        "PC3",                    // Pin 15
                                        "PA0/A0/RST/UPDI",        // Pin 16 - A0
                                        "PA1/A1/MOSI",            // Pin 17 - A1
                                        "PA2/A2/MISO",            // Pin 18 - A2
                                        "PA3/A3/SCK/CLKI",        // Pin 19 - A3
                                        "GND"};                   // Pin 20
  private static final String[] QF20 = {"PA2/A2/MISO",            // Pin 1 - A2
                                        "PA3/A3/SCK/CLKI",        // Pin 2 - A3
                                        "GND",                    // Pin 3
                                        "VCC",                    // Pin 4
                                        "PA4/A4/SS",              // Pin 5 - A4
                                        "PA5/A5",                 // Pin 6 - A5
                                        "PA6/A6/DAC",             // Pin 7 - A6
                                        "PA7/A7",                 // Pin 8 - A7
                                        "PB5/A8",                 // Pin 9 - A8
                                        "PB4/A9",                 // Pin 10 - A9
                                        "PB3",                    // Pin 11
                                        "PB2",                    // Pin 12
                                        "PB1/A10/SDA",            // Pin 13 - A10
                                        "PB0/A11/SCL",            // Pin 14 - A11
                                        "PC0",                    // Pin 15
                                        "PC1",                    // Pin 16
                                        "PC2",                    // Pin 17
                                        "PC3",                    // Pin 18
                                        "PA0/A0/RST/UPDI",        // Pin 19 - A0
                                        "PA1/A1/MOSI"};           // Pin 20 - A1
  private static final String[] QF24 = {"PA2/A2/MISO",            // Pin 1 - A2
                                        "PA3/A3/SCK/CLKI",        // Pin 2 - A3
                                        "GND",                    // Pin 3
                                        "VCC",                    // Pin 4
                                        "PA4/A4/SS",              // Pin 5 - A4
                                        "PA5/A5/VREF",            // Pin 6 - A5
                                        "PA6/A6/DAC",             // Pin 7 - A6
                                        "PA7/A7",                 // Pin 8 - A7
                                        "PB7",                    // Pin 9 - A4 (ADC1)
                                        "PB6",                    // Pin 10 - A5 (ADC1)
                                        "PB5/A8",                 // Pin 11 - A8
                                        "PB4/A9",                 // Pin 12 - A9
                                        "PB3/RXD",                // Pin 13
                                        "PB2/TXD",                // Pin 14
                                        "PB1/A10/SDA",            // Pin 15 - A10
                                        "PB0/A11/SCL",            // Pin 16 - A11
                                        "PC0",                    // Pin 17
                                        "PC1",                    // Pin 18
                                        "PC2",                    // Pin 19
                                        "PC3",                    // Pin 20
                                        "PC4",                    // Pin 21
                                        "PC5",                    // Pin 22
                                        "PA0/A0/RST/UPDI",        // Pin 23 - A0
                                        "PA1/A1/MOSI",};          // Pin 24 - A1
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

    PadBox (String text, Font font, Color fontColor, Color background, double wid, double hyt) {
      try {
        this.text = text;
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        GlyphVector gv = font.createGlyphVector(g2.getFontRenderContext(), text);
        Shape glyph = gv.getOutline();
        Rectangle2D bounds = glyph.getBounds2D();
        double rounding = Math.min(bounds.getHeight() * .2, 20);
        Shape rRect = new RoundRectangle2D.Double(0, 0, wid, hyt, rounding, rounding);
        addShape(rRect, background, true);
        addShape(rRect, BDR, false);
        addShape(glyph, fontColor, true);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    public void addShape (Shape shape, Color color, boolean fill) {
      Rectangle2D bounds = shape.getBounds2D();
      double xShift = (-bounds.getWidth() / 2) - bounds.getX();
      double yShift = (-bounds.getHeight() / 2) - bounds.getY();
      this.bounds.add(bounds);
      AffineTransform center = AffineTransform.getTranslateInstance(xShift, yShift);
      DrawShape  drawShape = new DrawShape(center.createTransformedShape(shape), color, fill);
      drawShapes.add(drawShape);
    }

    public Rectangle2D getBounds () {
      return bounds;
    }

    private void drawPad (DrawSpace ds, double angle, double xLoc, double yLoc) {
      Graphics2D g2 = ds.g2;
      for (DrawShape drawShape : drawShapes) {
        AffineTransform rotate = AffineTransform.getRotateInstance(Math.toRadians(angle));
        Shape rotated = rotate.createTransformedShape(drawShape.shape);
        AffineTransform position = AffineTransform.getTranslateInstance(xLoc, yLoc);
        Shape positioned = position.createTransformedShape(rotated);
        g2.setColor(drawShape.color);
        if (drawShape.fill) {
          g2.fill(positioned);
        } else{
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

     DrawSpace (int wid, int hyt) {
      img = new BufferedImage(wid, hyt, BufferedImage.TYPE_INT_RGB);
      g2 = img.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(BACK);
      g2.fillRect(0, 0, wid, hyt);
    }

    void addHoverText (Rectangle rect, String text) {
       if (text.matches("P[ABC][0-7]")) {
         text = text + ": Port " + text.charAt(1) + ", Pin " + text.charAt(2);
       } else if (text.matches("A[0-9]")) {
           text = text + ": Analog Input, Channel " + text.substring(1);
       } else if (text.matches("^\\d+$")) {
           text = "Pin " + text;
       } else {
         switch (text) {
         case "VCC":
           text = text + ": Operating Voltage";
           break;
         case "GND":
           text = text + ": Ground";
           break;
         case "DAC":
           text = text + ": Digital to Analog Converter Output";
           break;
         case "MISO":
           text = text + ": SPI Master In Slave Out";
           break;
         case "MOSI":
           text = text + ": SPI Master Out Slave In";
           break;
         case "SCK":
           text = text + ": SPI Clock In/Out";
           break;
         case "SS":
           text = text + ": SPI Slave Select (active low)";
           break;
         case "RXD":
           text = text + ": USART Serial Input";
           break;
         case "TXD":
           text = text + ": USART Serial Output";
           break;
         case "SCL":
           text = text + ": TWI Clock";
           break;
         case "SDA":
           text = text + ": TWI Data In/Out";
           break;
         case "UPDI":
           text = text + ": Unified Program and Debug Interface";
           break;
         case "CLKI":
           text = text + ": External Clock Input";
           break;
         case "RST":
           text = text + ": Reset Input";
           break;
         case "VREF":
           text = text + ": External Voltage Reference";
           break;
         }
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

  private static DrawSpace getSoic (String label, int padWid, int pins, String[] pn, boolean hasDacs) {
    int spacing = 35;
    int bodyHyt = pins * spacing / 2 + spacing / 2;
    int bodyWid = 180;
    int padHyt = bodyHyt + spacing;
    int cx = padWid / 2;
    int cy = padHyt / 2;
    DrawSpace ds = new DrawSpace(padWid, padHyt);
    int gap = 4;
    int bHyt = 30;
    int sOff = (pins * spacing) / 4 - spacing / 2;
    new PadBox(label, BFNT, WHT, CHIP, bodyHyt, bodyWid).drawPad(ds, -90, cx, cy);
    int[] cols = new int[COLS.length];
    int base = bodyWid / 2;
    for (int ii = 0; ii < COLS.length; ii++) {
      cols[ii] = base + COLS[ii] / 2;
      base += COLS[ii] + gap;
    }
    for (int pin = 1, rp = pins; pin <= pins; pin++, rp--) {
      boolean left = pin <= pins / 2;
      int yOff = left ? cy + (pin - 1) * spacing - sOff : cy + sOff - (pin - pins / 2 - 1) * spacing;
      String[] tmp = pn[pin - 1].split("/");
      String[] parts = new String[tmp.length + 1];
      parts[0] = pin + ":PIN";
      System.arraycopy(tmp, 0, parts, 1, tmp.length);
      // Draw pin labels (physical, logical, special)
      for (int ii = 0; ii < parts.length; ii++) {
        int col = cols[ii];
        int wid = COLS[ii];
        int xOff = left ? cx - col : cx + col;
        String[] pairs = parts[ii].split(":");
        String pLbl = pairs[0];
        if (pLbl.length() > 0 && (!"DAC".equals(pLbl) || hasDacs)) {
          ColorSet cSet = getColorSet(pairs, DCLR[ii]);
          new PadBox(pLbl, PFNT, cSet.txtClr, cSet.lblClr, wid, bHyt).drawPad(ds, 0, xOff, yOff);
        }
      }
    }
    return ds;
  }

  private static DrawSpace getVqfn (String label, int padWid, int pins, String[] pn) {
    int spacing = 35;
    int bodyHyt = pins * spacing / 4 + spacing / 2;
    int bodyWid = bodyHyt;
    int padHyt = padWid;
    int cx = padWid / 2;
    int cy = padHyt / 2;
    DrawSpace ds = new DrawSpace(padWid, padHyt);
    int gap = 4;
    int bHyt = 30;
    int sOff = (pins * spacing) / 8 - spacing / 2;
    int xOff, yOff, rotate;
    // Draw VQFN-20 package body
    new PadBox(label, BFNT, WHT, CHIP, bodyHyt, bodyWid).drawPad(ds, 0, cx, cy);
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
          ColorSet cSet = getColorSet(pairs, DCLR[ii]);
          new PadBox(pLbl, PFNT, cSet.txtClr, cSet.lblClr, wid, bHyt).drawPad(ds, rotate, cx + xOff, cy + yOff);
        }
      }
    }
    return ds;
  }

  public static DrawSpace getLayout (String avrChip, String pkg) {
    MegaTinyIDE.ChipInfo info = MegaTinyIDE.getChipInfo(avrChip);
    String chipLabel = "AT" + avrChip.substring(2);
    boolean hasDacs = info.getInt("dacs") > 0;
    switch (info.pins) {
    case 8:
      return getSoic(chipLabel, 800, 8, SO8, hasDacs);
    case 14:
      return getSoic(chipLabel, 800, 14, SO14, hasDacs);
    case 20:
      if ("SOIC-20".equals(pkg)) {
        return getSoic(chipLabel, 800, 20, SO20, hasDacs);
      } else {
        return getVqfn(chipLabel, 800, 20, QF20);
      }
    case 24:
      return getVqfn(chipLabel, 800, 24, QF24);
    }
    return null;
  }
}
