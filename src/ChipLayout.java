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
   *  This class is used to generate pinout images for the various members of the 0-series and 1-series ATtiny chips
   */

public class ChipLayout {
  private static final Color    BACK = new Color(240, 240, 240);
  private static final Color    CHIP = new Color(51, 51, 51);
  private static final Color    PINS = new Color(204, 204, 204);
  private static final Color    PORT = new Color(90, 127, 169);
  private static final Color    BDR = new Color(0, 3, 12);
  private static final Color    DBG = new Color(250, 207, 114);
  private static final Color    VCC = new Color(168, 47, 17);
  private static final Color    SER = new Color(159, 146, 254);
  private static final Color    ISP = new Color(252, 255, 81);
  private static final Color    ANA = new Color(99, 123, 80);
  private static final Color    BLU = new Color(151, 169, 193);
  private static final Color    GRN = new Color(73, 140, 28);
  private static final Color    RST = new Color(247, 153, 66);
  private static final Color    WHT = Color.white;
  private static final Color    BLK = Color.black;
  private static final Font     BFNT = new Font("Helvetica", Font.PLAIN, 32);
  private static final Font     PFNT = new Font("Helvetica", Font.PLAIN, 22);
  private static final String[] SO8 = {"VCC", "PA6/TXD/DAC", "PA7/RXD", "PA1/SDA/MOSI", "PA2/SCL/MISO", "PA0/RST/UPDI",
                                       "PA3//SCK/CLKI", "GND"};
  private static final String[] SO14 = {"VCC", "PA4/SS", "PA5/VREF", "PA6/DAC", "PA7", "PB3/RXD", "PB2/TXD",
                                        "PB1/SDA", "PB0/SCL", "PA0/RST/UPDI", "PA1//MOSI", "PA2//MISO", "PA3//SCK/CLKI", "GND"};
  private static final String[] SO20 = {"VCC", "PA4/SS", "PA5/VREF", "PA6/DAC", "PA7", "PB5", "PB4", "PB3/RXD", "PB2/TXD", "PB1/SDA",
                                        "PB0/SCL", "PC0", "PC1", "PC2", "PC3", "PA0/RST/UPDI", "PA1/MOSI", "PA2/MISO", "PA3/SCK/CLKI", "GND"};
  private static final String[] QF20 = {"PA2/MISO", "PA3/SCK/CLKI", "GND", "VCC", "PA4/SS", "PA5", "PA6/DAC", "PA7", "PB5", "PB4",
                                        "PB3", "PB2", "PB1/SDA", "PB0/SCL", "PC0", "PC1", "PC2", "PC3", "PA0/RST/UPDI", "PA1/MOSI"};
  private static final String[] QF24 = {"PA2/MISO", "PA3/SCK/CLKI", "GND", "VCC", "PA4/SS", "PA5/VREF", "PA6/DAC", "PA7", "PB7", "PB6", "PB5", "PB4",
                                        "PB3/RXD", "PB2/TXD", "PB1/SDA", "PB0/SCL", "PC0", "PC1", "PC2", "PC3", "PC4", "PC5", "PA0/RST/UPDI", "PA1/MOSI",};
  private static final Map<String,ColorSet> colors = new HashMap<>();
  private static final int[]    COLS = new int[] {35, 60, 60, 60, 60};                    // Width of lbl by column
  private static final String[] DCLR = new String[] {"PIN", "PORT", "BLU", "BLU", "BLU"}; // Default lbl color by column

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
    colors.put("SER",  new ColorSet(BLK, SER));
    colors.put("RXD",  new ColorSet(BLK, BLU));
    colors.put("TXD",  new ColorSet(BLK, BLU));
    colors.put("DAC",  new ColorSet(BLK, SER));
    colors.put("SDA",  new ColorSet(BLK, BLU));
    colors.put("SCL",  new ColorSet(BLK, BLU));
    colors.put("SCK",  new ColorSet(BLK, BLU));
    colors.put("MISO", new ColorSet(BLK, BLU));
    colors.put("MOSI", new ColorSet(BLK, BLU));
    colors.put("VREF", new ColorSet(BLK, BLU));
    colors.put("SS",   new ColorSet(BLK, BLU));
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

  static class MyLabel {
    String          text;
    List<DrawShape> drawShapes = new ArrayList<>();
    Rectangle2D     bounds = new Rectangle2D.Double();

    MyLabel (String text, Font font, Color fontColor, Color background, double forceWid, double forceHyt) {
      this.text = text;
      BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2 = img.createGraphics();
      GlyphVector gv = font.createGlyphVector(g2.getFontRenderContext(), text);
      Shape glyph = gv.getOutline();
      Rectangle2D bounds = glyph.getBounds2D();
      double padWid = bounds.getWidth() * .4;
      double padHyt = bounds.getHeight() * .4;
      double rounding = Math.min(bounds.getHeight() * .2, 20);
      double wid = forceWid > 0 ? forceWid : bounds.getWidth() + padWid;
      double hyt = forceHyt > 0 ? forceHyt : bounds.getHeight() + padHyt;
      Shape rRect = new RoundRectangle2D.Double(0, 0, wid, hyt, rounding, rounding);
      add(rRect, background, true);
      add(rRect, BDR, false);
      add(glyph, fontColor, true);
    }

    public void add (Shape shape, Color color, boolean fill) {
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

  private static void drawLabel (DrawSpace ds, MyLabel label, double angle, double xLoc, double yLoc) {
    Graphics2D g2 = ds.g2;
    for (DrawShape drawShape : label.drawShapes) {
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
        ds.addHoverText(rect, label.text);
      }
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

  private static DrawSpace getSoic (String label, int imgWid, int pins, String[] pn, boolean hasDacs) {
    int spacing = 35;
    int bodyHyt = pins * spacing / 2 + spacing / 2;
    int bodyWid = 180;
    int imgHyt = bodyHyt + spacing;
    int cx = imgWid / 2;
    int cy = imgHyt / 2;
    DrawSpace ds = new DrawSpace(imgWid, imgHyt);
    int gap = 4;
    int bHyt = 30;
    int sOff = (pins * spacing) / 4 - spacing / 2;
    drawLabel(ds, new MyLabel(label, BFNT, WHT, CHIP, bodyHyt, bodyWid), -90, cx, cy);
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
          drawLabel(ds, new MyLabel(pLbl, PFNT, cSet.txtClr, cSet.lblClr, wid, bHyt), 0, xOff, yOff);
        }
      }
    }
    return ds;
  }

  private static DrawSpace getVqfn (String label, int imgWid, int pins, String[] pn, boolean hasDacs) {
    int spacing = 35;
    int bodyHyt = pins * spacing / 4 + spacing / 2;
    int bodyWid = bodyHyt;
    int imgHyt = imgWid;
    int cx = imgWid / 2;
    int cy = imgHyt / 2;
    DrawSpace ds = new DrawSpace(imgWid, imgHyt);
    int gap = 4;
    int bHyt = 30;
    int sOff = (pins * spacing) / 8 - spacing / 2;
    int xOff, yOff, rotate;
    // Draw VQFN-20 package body
    drawLabel(ds, new MyLabel(label, BFNT, WHT, CHIP, bodyHyt, bodyWid), 0, cx, cy);
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
          ColorSet cSet = getColorSet(pairs, DCLR[ii]);
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
          drawLabel(ds, new MyLabel(pLbl, PFNT, cSet.txtClr, cSet.lblClr, wid, bHyt), rotate, cx + xOff, cy + yOff);
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
        return getVqfn(chipLabel, 700, 20, QF20, hasDacs);
      }
    case 24:
      return getVqfn(chipLabel, 700, 24, QF24, hasDacs);
    }
    return null;
  }
}
