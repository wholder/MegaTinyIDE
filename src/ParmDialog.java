import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static javax.swing.JOptionPane.showMessageDialog;

class ParmDialog extends JDialog {
  private static final double         lblWeight = 0.5;
  private static Map<String,String>   dialogInfo;
  private boolean                     cancelled = true;

  static {
    try {
      dialogInfo = Utility.getResourceMap("dialoginfo.props");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  interface ParmListener {
    void parmEvent (Item parm);
  }

  static class Item {
    JLabel          label;
    JComponent      field;
    ParmListener    parmListener;
    String          name, info, pref;
    Object          value, valueType;
    boolean         readOnly, lblValue, enabled = true;

    Item (String line) {
      int idx1 = line.indexOf("(");
      int idx2 = line.indexOf(")");
      String parm = idx1 >= 0 && idx2 > idx1 ? line.substring(idx1 + 1, idx2).trim() : "";
      idx1 = line.indexOf("{");
      idx2 = line.indexOf("}");
      String info = idx1 > 0 && idx2 > idx1 ? line.substring(idx1 + 1, idx2).trim() : "";
      String[] items = parm.split(",");
      if (items.length == 2) {
        setup(items[0].trim(), info, null, items[1].trim());
      } else {
        throw new IllegalStateException();
      }
    }

    Item (String name, String info, String parm,  Object value) {
      setup(name, info, parm, value);
    }

    private void setup (String name, String info, String parm,  Object value) {
      this.name = Utility.replaceTags(name, dialogInfo);
      this.info = info != null ? Utility.replaceTags(info, dialogInfo) : null;
      this.pref = parm;
      if (parm != null) {
        Preferences prefs = Preferences.userRoot().node(MegaTinyIDE.class.getName());
        if (value instanceof Boolean) {
          value = prefs.getBoolean(parm, (boolean) value);
        } else if (value instanceof String) {
          value = prefs.get(parm, (String) value);
        } else if (value instanceof Integer) {
          value = prefs.getInt(parm, (int) value);
        } else if (value instanceof Double) {
          value = prefs.getDouble(parm, (double) value);
        }
      }
      if (name.startsWith("*")) {
        name = name.substring(1);
        readOnly = true;
      } else if (name.startsWith("@")) {
        name = name.substring(1);
        lblValue = true;
      }
      if (name.contains(":")) {
        String[] parts = name.split(":");
        name = parts[0];
        for (int ii = 1; ii < parts.length; ii++) {
          parts[ii] = parts[ii].replace(';', ':');
        }
        valueType = Arrays.copyOfRange(parts, 1, parts.length);
      } else if (name.contains("[")  &&  name.contains("]")) {
        int ii = name.indexOf("[");
        int jj = name.indexOf("]");
        String range = name.substring(ii + 1, jj);
        String[] parts = range.split("-");
        name = name.substring(0, ii);
        if (parts.length == 2) {
          int first = Integer.parseInt(parts[0]);
          int last = Integer.parseInt(parts[1]);
          int[] vals = new int[last - first + 1];
          for (int kk = 0; kk < vals.length; kk++) {
            vals[kk] = first + kk;
          }
          valueType = vals;
          this.value = value;
        } else {
          throw new IllegalArgumentException("ParmItem: " + name);
        }
      } else {
        this.valueType = value;
      }
      String[] tmp = name.split("\\|");
      if (tmp.length == 1) {
        this.name = name;
      } else {
        this.name = tmp[0];
      }
      this.value = value;
    }

    void setInvalidData (boolean invalid) {
      JTextField tf = (JTextField) field;
      if (invalid)  {
        tf.setBackground(Color.pink);
      } else {
        tf.setBackground(Color.white);
      }
    }

    void setEnabled (boolean enabled) {
      if (field != null) {
        field.setEnabled(enabled);
        if (field instanceof JComboBox){
          ((JComboBox<?>) field).setSelectedIndex(0);
        }
      }
      this.enabled = enabled;
    }

    private void hookActionListener () {
      if (parmListener != null) {
        if (field instanceof JComboBox){
          ((JComboBox<?>) field).addActionListener(ev -> parmListener.parmEvent(this));
        } else if (field instanceof JTextField) {
          ((JTextField) field).getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate (DocumentEvent e) {
              parmListener.parmEvent(Item.this);
            }

            @Override
            public void removeUpdate (DocumentEvent e) {
              parmListener.parmEvent(Item.this);
            }

            @Override
            public void changedUpdate (DocumentEvent e) { }
          });
        }
        setEnabled(enabled);
      }
    }

    // Return true if invalid value
    private boolean setValueAndValidate (String newValue) {
      if (valueType instanceof Integer || valueType instanceof int[]) {
        try {
          this.value = Integer.parseInt(newValue);
        } catch (NumberFormatException ex) {
          return true;
        }
      } else if (valueType instanceof String[]) {
        value = newValue;
      } else if (valueType instanceof String) {
        value = newValue;
      }
      return false;
    }
  }

  private GridBagConstraints getGbc (int x, int y) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
    gbc.fill = (x == 0) ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
    gbc.weightx = (x == 0) ? lblWeight : 1.0 - lblWeight;
    gbc.ipady = 2;
    return gbc;
  }

  static String[] getLabels (String[] vals) {
    String[] tmp = new String[vals.length];
    for (int ii = 0; ii < vals.length; ii++) {
      tmp[ii] = vals[ii].contains("=") ? vals[ii].substring(0, vals[ii].indexOf("=")) : vals[ii];
    }
    return tmp;
  }

  static String[] getValues (String[] vals) {
    String[] tmp = new String[vals.length];
    for (int ii = 0; ii < vals.length; ii++) {
      tmp[ii] = vals[ii].contains("=") ? vals[ii].substring(vals[ii].indexOf("=") + 1) : vals[ii];
    }
    return tmp;
  }

  private static String[] intToString (int[] vals) {
    String[] strs = new String[vals.length];
    for (int ii = 0; ii < vals.length; ii++) {
      strs[ii] = Integer.toString(vals[ii]);
    }
    return strs;
  }

  boolean wasPressed () {
    return !cancelled;
  }

  /**
   * Constructor for Pop Up Parameters Dialog with error checking
   * @param title text for parameter dialog window
   * @param parms array of ParmItem objects that describe each parameter
   * @param buttons String] array of button names (first name in array is action button)
   */
  ParmDialog (String title, Item[] parms, String[] buttons) {
    super((Frame) null, true);
    setTitle(title);
    JPanel fields = new JPanel();
    fields.setLayout(new GridBagLayout());
    int jj = 0;
    for (Item parm : parms) {
      if (parm.value instanceof JComponent) {
        if (parm.name != null) {
          fields.add(parm.label = new JLabel(parm.name + ": "), getGbc(0, jj));
          fields.add((JComponent) parm.value, getGbc(1, jj));
        } else {
          GridBagConstraints gbc = new GridBagConstraints();
          gbc.gridx = 0;
          gbc.weightx = 1;
          gbc.fill = GridBagConstraints.HORIZONTAL;
          gbc.gridwidth = GridBagConstraints.REMAINDER;
          fields.add((JComponent) parm.value, gbc);
        }
      } else {
        fields.add(parm.label = new JLabel(parm.name + ": "), getGbc(0, jj));
        if (parm.valueType instanceof Boolean) {
          JCheckBox select = new JCheckBox();
          select.setBorderPainted(false);
          select.setFocusable(false);
          select.setBorderPaintedFlat(true);
          select.setSelected((Boolean) parm.value);
          select.setHorizontalAlignment(JCheckBox.RIGHT);
          fields.add(parm.field = select, getGbc(1, jj));
        } else if (parm.valueType instanceof String[]) {
          String[] labels = getLabels((String[]) parm.valueType);
          JComboBox<String> select = new JComboBox<>(labels);
          ((JLabel)select.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
          String[] values = getValues((String[]) parm.valueType);
          if (parm.value instanceof Integer) {
            select.setSelectedIndex(Arrays.asList(values).indexOf(Integer.toString((Integer) parm.value)));
          } else {
            select.setSelectedIndex(Arrays.asList(values).indexOf(parm.value.toString()));
          }
          fields.add(parm.field = select, getGbc(1, jj));
        } else if (parm.valueType instanceof int[]) {
          String[] vals = intToString((int[]) parm.valueType);
          JComboBox<String> select = new JComboBox<>(vals);
          ((JLabel)select.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
          select.setSelectedItem(Integer.toString((Integer) parm.value));
          fields.add(parm.field = select, getGbc(1, jj));
        } else if (parm.value instanceof JComponent) {
          fields.add(parm.field = (JComponent) parm.value, getGbc(1, jj));
        } else {
          String val = parm.value.toString();
          if (parm.lblValue) {
            // If label name starts with "@" display value as JLabel
            JLabel lbl = new JLabel(val, SwingConstants.CENTER);
            fields.add(parm.field = lbl, getGbc(1, jj));
          } else {
            JTextField tf = new JTextField(val, 8);
            Dimension dim = tf.getPreferredSize();
            tf.setPreferredSize(new Dimension(dim.width, dim.height - 4));
            tf.setEditable(!parm.readOnly);
            if (parm.readOnly) {
              tf.setForeground(Color.gray);
            }
            tf.setHorizontalAlignment(JTextField.CENTER);
            fields.add(parm.field = tf, getGbc(1, jj));
            parm.field.addFocusListener(new FocusAdapter() {
              @Override
              public void focusGained (FocusEvent ev) {
                super.focusGained(ev);
                parm.setInvalidData(false);
              }
            });
          }
        }
        int col = 2;
        if (parm.info != null && parm.info.length() > 0) {
          // Add popup Info dialog
          try {
            final String[] tmp = parm.info.split("--");
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/info.png"));
            JButton iBut = new JButton(icon);
            Dimension dim = iBut.getPreferredSize();
            iBut.setPreferredSize(new Dimension(dim.width - 4, dim.height - 4));
            fields.add(iBut, getGbc(col, jj));
            iBut.addActionListener(ev -> {
              JEditorPane textArea = new JEditorPane();
              textArea.setContentType("text/html");
              textArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
              textArea.setFont(new Font("Arial", Font.PLAIN, 14));
              JScrollPane scrollPane = new JScrollPane(textArea);
              scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
              String iMsg = tmp.length > 1 ? tmp[1] : tmp[0];
              textArea.setText(iMsg);
              textArea.setEditable(false);
              textArea.setCaretPosition(0);
              scrollPane.setPreferredSize(new Dimension(350, 150));
              showMessageDialog(this, scrollPane, tmp.length > 1 ? tmp[0] : parm.name, JOptionPane.PLAIN_MESSAGE);
            });
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        } else {
          fields.add(new JLabel(""), getGbc(col, jj));
        }
        if (parm.info != null) {
          parm.field.setToolTipText(parm.info);
        }
      }
      jj++;
      parm.hookActionListener();
    }
    // Define a custion action button so we can catch and save the screen coordinates where the "Place" button was clicked...
    // Yeah, it's a lot of weird code but it avoids having the placed object not show up until the mouse is moved.
    JButton button = new JButton(buttons[0]);
    button.addActionListener(actionEvent -> {
      JButton but = ((JButton) actionEvent.getSource());
      JOptionPane pane = (JOptionPane) but.getParent().getParent();
      pane.setValue(buttons[0]);
    });
    Object[] buts = new Object[] {button, buttons[1]};
    JComponent parmPanel = fields;
    Dimension dim = parmPanel.getPreferredSize();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    if (dim.height > screenSize.height - (int) (screenSize.height * 0.2)) {
      JScrollPane sPane = new JScrollPane(fields);
      dim = sPane.getPreferredSize();
      sPane.setPreferredSize(new Dimension(dim.width + 50, screenSize.height - (int) (screenSize.height * 0.2)));
      parmPanel = sPane;
    }
    JOptionPane optionPane = new JOptionPane(parmPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, buts, buts[0]);
    setContentPane(optionPane);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    optionPane.addPropertyChangeListener(ev -> {
      String prop = ev.getPropertyName();
      if (isVisible() && (ev.getSource() == optionPane) && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
          JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
        Object value = optionPane.getValue();
        Preferences prefs = Preferences.userRoot().node(MegaTinyIDE.class.getName());
        if (value != JOptionPane.UNINITIALIZED_VALUE) {
          // Reset the JOptionPane's value.  If you don't do this, then if the user
          // presses the same button next time, no property change event will be fired.
          optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
          if (buttons[0].equals(value)) {
            boolean invalid = false;
            for (Item parm : parms) {
              Component comp = parm.field;
              if (comp instanceof JTextField) {
                JTextField tf = (JTextField) comp;
                if (parm.setValueAndValidate(tf.getText())) {
                  parm.setInvalidData(invalid = true);
                } else {
                  parm.setInvalidData(false);
                }
              } else if (comp instanceof JCheckBox) {
                parm.value = (((JCheckBox) comp).isSelected());
              } else if (comp instanceof JComboBox) {
                JComboBox<?> sel = (JComboBox<?>) comp;
                if (parm.valueType instanceof String[]) {
                  String[] values = getValues((String[]) parm.valueType);
                  parm.setValueAndValidate(values[sel.getSelectedIndex()]);
                } else if (parm.valueType instanceof int[]) {
                  String val = (String) sel.getSelectedItem();
                  parm.setValueAndValidate(val);
                }
              }
              if (parm.pref != null) {
                if (parm.value instanceof Boolean) {
                  prefs.putBoolean(parm.pref, (boolean) parm.value);
                } else if (parm.value instanceof String) {
                  prefs.put(parm.pref, (String) parm.value);
                } else if (parm.value instanceof Integer) {
                  prefs.putInt(parm.pref, (Integer) parm.value);
                } else if (parm.value instanceof Double) {
                  prefs.putDouble(parm.pref, (Double) parm.value);
                }
              }
            }
            if (!invalid) {
              dispose();
              cancelled = false;
            }
          } else {
            // User closed dialog or clicked cancel
            dispose();
          }
        }
      }
    });
    pack();
    setResizable(false);
  }

  public static void main (String... args) {
    Item[] parmSet = {
        new Item("(CYCLES, 1)"),
        new Item("(PATTERN:Random=0:Chaser=1:Disco=2:Cylon=3:Stripes=4, 1)"),
    };
    ParmDialog dialog = (new ParmDialog("Edit Parameters", parmSet, new String[] {"Save", "Cancel"}));
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);              // Note: this call invokes dialog
    if (dialog.wasPressed()) {
      for (Item parm : parmSet) {
        System.out.println(parm.name + ": " + parm.value);
      }
    } else {
      System.out.println("Cancel");
    }
  }
}