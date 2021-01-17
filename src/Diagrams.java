import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.prefs.Preferences;

/*
 *  This class is called by MarkupView to dynamically generate various kinds of diagrams images
 */

public class Diagrams {

  public static BufferedImage drawBitfield (String pattern, Map<String,String> parmMap) {
    int cellWid = 90;
    int cellHyt = 21;
    int cellHalf = cellHyt / 2;
    int lrMargin = 10;
    int wid = cellWid * 8 + lrMargin * 2;
    int hyt = cellHyt * 3;
    BufferedImage img = new BufferedImage(wid, hyt, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = img.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(new Color(245, 245, 245));
    g2.fillRect(0, 0, wid, hyt);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(1.2f));
    Font font = Utility.getCodeFont(12);
    FontMetrics fm = (new JPanel()).getFontMetrics(font);
    int fHyt = fm.getAscent();
    String[] items = pattern.split(",");
    boolean[] drawn = new boolean[9];
    for (String item : items) {
      String[] parts = item.split(":");
      drawn[7 - Integer.parseInt(parts[0])] = true;
      drawn[7 - Integer.parseInt(parts[1]) + 1] = true;
    }
    // Draw vertical lines that separate bitfields and bits
    for (int ii = 0; ii < 9; ii++) {
      int x = lrMargin + ii * cellWid;
      g2.setColor(drawn[ii] ? Color.BLACK : Color.lightGray);
      g2.drawLine(x, hyt / 2 - cellHalf, x, hyt / 2 + cellHalf);
      if (ii < 8) {
        // Draw bit number
        g2.setColor(Color.darkGray);
        int xBit = x + cellWid / 2 - fm.stringWidth("0") / 2;
        g2.drawString(Integer.toString(7 - ii), xBit, cellHyt - 5);
      }
    }
    // Draw text labels inside bit fields
    g2.setColor(Color.BLACK);
    for (String item : items) {
      String[] parts = item.split(":");
      if (parts.length >= 3) {
        int lVert = lrMargin + (7 - Integer.parseInt(parts[0])) * cellWid;
        int rVert = lrMargin + (7 - Integer.parseInt(parts[1]) + 1) * cellWid;
        String cellName = parts[2];
        int cWid = rVert - lVert;
        int tWid = fm.stringWidth(cellName);
        int x = lVert + cWid / 2 - tWid / 2;
        g2.drawString(cellName, x, hyt / 2 + fHyt / 2);
        if (parts.length > 3) {
          // Draw access rights (RW, RO, WO)
          g2.setColor(Color.darkGray);
          int xBit = lVert + cWid / 2 - fm.stringWidth(parts[3]) / 2;
          g2.drawString(parts[3], xBit, hyt - 5);
        }
      }
    }
    // Draw top and bottom horizontal lines
    g2.setColor(Color.BLACK);
    int rght = lrMargin + 8 * cellWid;
    g2.drawLine(lrMargin, hyt / 2 - cellHalf, rght, hyt / 2 - cellHalf);
    g2.drawLine(lrMargin, hyt / 2 + cellHalf, rght, hyt / 2 + cellHalf);
    return img;
  }

  /*
   *  Test code for MarkupView Pane and Diagrams
   */
  public static void main (String[] args) {
    Preferences prefs = Preferences.userRoot().node(MarkupView.class.getName());
    JFrame frame = new JFrame();
    MarkupView mView = new MarkupView();
    mView.setText("<p align=\"center\"><img src=\"bitfield:7:4:FIFO_CTRL:RW,3:3:XXXX:RO,2:2,1:0:YYYYY:WO\"></p>");
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
