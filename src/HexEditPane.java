import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static javax.swing.JOptionPane.showMessageDialog;

public class HexEditPane extends JTextPane {
  DefaultHighlightPainter     hp = new DefaultHighlightPainter(Color.lightGray);
  private static final Font   codeFont = Utility.getCodeFont(12);
  private final Component     parent;
  private final int           rowHeight;
  private final int           rows, cols;

  interface Update {
    void setValue (int offset, int value);
  }

  HexEditPane (Component parent, int rows, int cols) {
    this.parent = parent;
    this.rows = rows;
    this.cols = cols;
    setFont(codeFont);
    setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 4));
    FontMetrics fontMetrics = getFontMetrics(codeFont);
    rowHeight = fontMetrics.getHeight();
    setEditable(false);
    setCaret(new DefaultCaret() {
      @Override
      public void mousePressed (MouseEvent e) {
      }

      @Override
      public void mouseDragged (MouseEvent e) {
      }
    });
    setToolTipText("Right clock, or double click on a value to edit");
  }

  private boolean isPrintable (int val) {
    return val <= 0x7F && val >= 0x20;
  }

  public void showDialog (String frameLabel, String boxLabel, int add, byte[] data, Update updater) {
    int[] posHex = new int[data.length];
    int[] posChr = new int[data.length];
    int rowWidth = 8 + cols * 6 + 2 + 1;
    JScrollPane scroll = new JScrollPane(this);
    scroll.getVerticalScrollBar().setUnitIncrement(rowHeight);
    StringBuilder buf = new StringBuilder();
    int count = cols;
    int base = 0;
    for (int ii = 0; ii < data.length; ii++) {
      if ((ii % cols) == 0) {
        if (ii != 0) {
          buf.append("\n");
          count = cols;
          base = ii;
        }
        buf.append(String.format("0x%04X: ", add + ii));
      }
      posHex[ii] = buf.length();
      buf.append(String.format("0x%02X ", data[ii]));
      count--;
      if (count == 0 || ii == data.length - 1) {
        while (count-- > 0 && ii >= cols) {
          buf.append("     ");
        }
        buf.append("| ");
        for (int jj = 0; jj < cols && (base + jj) < data.length; jj++) {
          int val = (int) data[base + jj] & 0xFF;
          posChr[base + jj] = buf.length();
          buf.append(String.format("%c", (isPrintable(val) ? val : '.')));
        }
      }
    }
    if (updater != null) {
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed (MouseEvent ev) {
          int clickCount = ev.getClickCount();
          int pos = getUI().viewToModel(HexEditPane.this, ev.getPoint());
          int row = pos / rowWidth;
          int col = pos % rowWidth;
          getHighlighter().removeAllHighlights();
          int off;
          if (col >= 8 && col < cols * 5 + 8) {
            off = row * cols + (col - 8) / 5;
            if (off < data.length) {
              try {
                Highlighter highlighter = getHighlighter();
                highlighter.addHighlight(posHex[off], posHex[off] + 4, hp);
                highlighter.addHighlight(posChr[off], posChr[off] + 1, hp);
                if (SwingUtilities.isRightMouseButton(ev) || clickCount == 2) {
                  setToolTipText(null);
                  JPanel panel = new JPanel();
                  panel.setLayout(new FlowLayout());
                  JLabel lbl = new JLabel("New Value:");
                  panel.add(lbl);
                  JTextField field = new JTextField(2);
                  field.setText(String.format("%02X", 0));
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
                  JDialog popup = new JDialog();
                  okBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed (MouseEvent ev) {
                      String val = field.getText();
                      try {
                        int nVal = Integer.parseInt(val, 16);
                        try {
                          updater.setValue(off, nVal);
                        } catch (Exception ex) {
                          ex.printStackTrace();
                          ImageIcon icon = new ImageIcon(HexEditPane.class.getResource("images/warning-32x32.png"));
                          showMessageDialog(popup, "Unknown Error", ex.getMessage(), JOptionPane.PLAIN_MESSAGE, icon);
                          return;
                        }
                        setSelectionStart(posHex[off]);
                        setSelectionEnd(posHex[off] + 4);
                        setEditable(true);
                        replaceSelection(String.format("0x%02X", nVal));
                        if (isPrintable(nVal)) {
                          setSelectionStart(posChr[off]);
                          setSelectionEnd(posChr[off] + 1);
                          replaceSelection(Character.toString((char) nVal));
                        }
                        setEditable(false);
                        repaint();
                        popup.dispose();
                      } catch (Exception ex) {
                        ImageIcon icon = new ImageIcon(HexEditPane.class.getResource("images/warning-32x32.png"));
                        showMessageDialog(popup, "Must be hexadecimal", "Invalid value", JOptionPane.PLAIN_MESSAGE, icon);
                        field.requestFocusInWindow();
                        field.setText("00");
                        field.selectAll();
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
                  Point temp = ev.getLocationOnScreen();
                  popup.setLocation(temp.x + 20, temp.y);
                  field.requestFocusInWindow();
                  field.selectAll();
                  popup.pack();
                  popup.setAlwaysOnTop(true);
                  popup.setVisible(true);
                }
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            }
          }
        }
      });
    }
    setText(buf.toString());
    JPanel panel = new JPanel(new BorderLayout());
    if (boxLabel != null) {
      JLabel label = new JLabel(boxLabel + " (" + data.length + " bytes)");
      label.setHorizontalAlignment(SwingConstants.LEFT);
      panel.add(label, BorderLayout.NORTH);
    }
    panel.add(scroll, BorderLayout.CENTER);
    setCaretPosition(0);
    JOptionPane.showConfirmDialog(parent, panel, frameLabel, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);

/*
    // Buttons in reversd order
    Object[] buttons = {"OK", "Get IntelHex"};
    int val = JOptionPane.showOptionDialog(parent, panel, frameLabel, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
      null, buttons, buttons[0]);
    System.out.println(val);
*/

  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    Dimension base = super.getPreferredSize();
    Insets insets = getInsets();
    int width = base.width + insets.left + insets.right;
    int estimatedRows = Math.min(rows, (int) (base.getHeight() / rowHeight));
    int height = estimatedRows * rowHeight + insets.top + insets.bottom;
    return new Dimension(width, height);
  }

  // Allow horizontal scrollbar to appear
  @Override
  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  // Test code
  public static void main (String[] args) {
    HexEditPane varPane = new HexEditPane(null, 16, 16);
    byte[] data = new byte[256];
    for (int ii = 0; ii < data.length; ii++) {
      data[ii] = (byte) ii;
    }
    varPane.showDialog("Test", "var1", 0, data, new Update() {
      public void setValue (int offset, int value) {
        System.out.printf("setValue(0x%02X, 0x%02X)\n", offset, value);
      }
    });
  }
}
