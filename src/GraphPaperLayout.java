import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>GraphPaperLayout</code> class is a layout manager that lays out a container's components in a rectangular grid,
 * similar to GridLayout.  Unlike GridLayout, however, components can take up multiple rows and/or columns.  The layout manager
 * acts as a sheet of graph paper.  When a component is added to the layout manager, the location and relative size of the
 * component are simply supplied by the constraints as a Rectangle.
 *
 * @author Original code by Michael Martak, updated and revised Sept 26, 2020 by Wayne Holder
 */

public class GraphPaperLayout implements LayoutManager2 {
  private final Map<Component, Rectangle> compTable = new HashMap<>();  // constraints (Rectangles)
  private final int hgap, vgap;                                         // horizontal, vertical gap
  private int gridWid, gridHyt;                                         // grid size in logical units (n x m)

  /**
   * Creates a graph paper layout with an expanding grid, and no vertical or horizontal padding.
   */
  public GraphPaperLayout () {
    this(0, 0);
  }

  /**
   * Creates a graph paper layout with an expanding grid with padding
   * @param hgap  horizontal padding
   * @param vgap  vertical padding
   */
  public GraphPaperLayout (int hgap, int vgap) {
    this.hgap = hgap;
    this.vgap = vgap;
  }

  /**
   * @return the current size of the graph paper in logical units (n x m)
   */
  public Dimension getGridSize () {
    return new Dimension(gridWid, gridHyt);
  }

  public void setConstraints (Component comp, Rectangle constraints) {
    // Increase grid size, if needed
    gridWid = Math.max(gridWid, constraints.x + constraints.width);
    gridHyt = Math.max(gridHyt, constraints.y + constraints.height);
    compTable.put(comp, new Rectangle(constraints));
  }

  /**
   * Adds the specified component with the specified name to
   * the layout.  This does nothing in GraphPaperLayout, since constraints
   * are required.
   */
  public void addLayoutComponent (String name, Component comp) {
  }

  /**
   * Removes the specified component from the layout.
   * @param comp the component to be removed
   */
  public void removeLayoutComponent (Component comp) {
    compTable.remove(comp);
  }

  /**
   * Calculates the preferred size dimensions for the specified
   * panel given the components in the specified parent container.
   * @param parent the component to be laid out
   * @see #minimumLayoutSize
   */
  public Dimension preferredLayoutSize (Container parent) {
    return getLayoutSize(parent, true);
  }

  /**
   * Calculates the minimum size dimensions for the specified
   * panel given the components in the specified parent container.
   * @param parent the component to be laid out
   * @see #preferredLayoutSize
   */
  public Dimension minimumLayoutSize (Container parent) {
    return getLayoutSize(parent, false);
  }

  /**
   * Algorithm for calculating layout size (minimum or preferred).
   * <p>
   * The width of a graph paper layout is the largest cell width (calculated in <code>getLargestCellSize()</code>
   * times the number of columns, plus the horizontal padding times the number of columns plus one, plus the left
   * and right insets of the target container.
   * <p>
   * The height of a graph paper layout is the largest cell height (calculated in <code>getLargestCellSize()</code>
   * times the number of rows, plus the vertical padding times the number of rows plus one, plus the top and bottom
   * insets of the target container.
   * @param parent      the container in which to do the layout.
   * @param isPreferred true for calculating preferred size, false for calculating minimum size.
   * @return the dimensions to lay out the subcomponents of the specified container.
   */
  protected Dimension getLayoutSize (Container parent, boolean isPreferred) {
    Dimension largestSize = getLargestCellSize(parent, isPreferred);
    Insets insets = parent.getInsets();
    largestSize.width = (largestSize.width * gridWid) + (hgap * (gridWid + 1)) + insets.left + insets.right;
    largestSize.height = (largestSize.height * gridHyt) + (vgap * (gridHyt + 1)) + insets.top + insets.bottom;
    return largestSize;
  }

  /**
   * Algorithm for calculating the largest minimum or preferred cell size.
   * <p>
   * Largest cell size is calculated by getting the applicable size of each  component and keeping the maximum value,
   * dividing the component's width by the number of columns it is specified to occupy and dividing the component's height
   * by the number of rows it is specified to occupy.
   * @param parent      the container in which to do the layout.
   * @param isPreferred true for calculating preferred size, false for calculating minimum size.
   * @return the largest cell size required.
   */
  protected Dimension getLargestCellSize (Container parent, boolean isPreferred) {
    int ncomponents = parent.getComponentCount();
    Dimension maxCellSize = new Dimension(0, 0);
    for (int i = 0; i < ncomponents; i++) {
      Component c = parent.getComponent(i);
      Rectangle rect = compTable.get(c);
      if (c != null && rect != null) {
        Dimension componentSize;
        if (isPreferred) {
          componentSize = c.getPreferredSize();
        } else {
          componentSize = c.getMinimumSize();
        }
        // Note: rect dimensions are already asserted to be > 0 when the
        // component is added with constraints
        maxCellSize.width = Math.max(maxCellSize.width, componentSize.width / rect.width);
        maxCellSize.height = Math.max(maxCellSize.height, componentSize.height / rect.height);
      }
    }
    return maxCellSize;
  }

  /**
   * Lays out the container in the specified container.
   * @param parent the component which needs to be laid out
   */
  public void layoutContainer (Container parent) {
    synchronized (parent.getTreeLock()) {
      Insets insets = parent.getInsets();
      int ncomponents = parent.getComponentCount();
      if (ncomponents == 0) {
        return;
      }
      // Total parent dimensions
      Dimension size = parent.getSize();
      int totalW = size.width - (insets.left + insets.right);
      int totalH = size.height - (insets.top + insets.bottom);
      // Cell dimensions, including padding
      int totalCellW = totalW / gridWid;
      int totalCellH = totalH / gridHyt;
      // Cell dimensions, without padding
      int cellW = (totalW - ((gridWid + 1) * hgap)) / gridWid;
      int cellH = (totalH - ((gridHyt + 1) * vgap)) / gridHyt;
      for (int i = 0; i < ncomponents; i++) {
        Component c = parent.getComponent(i);
        Rectangle rect = compTable.get(c);
        if (rect != null) {
          int x = insets.left + (totalCellW * rect.x) + hgap;
          int y = insets.top + (totalCellH * rect.y) + vgap;
          int w = (cellW * rect.width) - hgap;
          int h = (cellH * rect.height) - vgap;
          c.setBounds(x, y, w, h);
        }
      }
    }
  }

  // LayoutManager2 /////////////////////////////////////////////////////////

  /**
   * Adds the specified component to the layout, using the specified constraint object.  The size of the containing
   * grid (gridWid, gridHyt) is automatically expanded to fit components that extend beyond the current bounds.
   * @param comp        the component to be added
   * @param constraints where/how the component is added to the layout.
   */
  public void addLayoutComponent (Component comp, Object constraints) {
    if (constraints instanceof Rectangle) {
      Rectangle rect = (Rectangle) constraints;
      if (rect.width <= 0 || rect.height <= 0) {
        throw new IllegalArgumentException("cannot add to layout: rectangle must have positive width and height");
      }
      if (rect.x < 0 || rect.y < 0) {
        throw new IllegalArgumentException("cannot add to layout: rectangle x and y must be >= 0");
      }
      setConstraints(comp, rect);
    } else if (constraints != null) {
      throw new IllegalArgumentException("cannot add to layout: constraint must be a Rectangle");
    }
  }

  /**
   * Returns the maximum size of this component.
   * @see Component#getMinimumSize()
   * @see Component#getPreferredSize()
   * @see LayoutManager
   */
  public Dimension maximumLayoutSize (Container target) {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Returns the alignment along the x axis.  This specifies how the component would like to be aligned relative to other
   * components.  The value should be a number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned
   * the furthest away from the origin, 0.5 is centered, etc.
   */
  public float getLayoutAlignmentX (Container target) {
    return 0.5f;
  }

  /**
   * Returns the alignment along the y axis.  This specifies how the component would like to be aligned relative to other
   * components.  The value should be a number between 0 and 1 where 0 represents alignment along the origin, 1 is aligned
   * the furthest away from the origin, 0.5 is centered, etc.
   */
  public float getLayoutAlignmentY (Container target) {
    return 0.5f;
  }

  /**
   * Invalidates the layout, indicating that if the layout manager has cached information it should be discarded.
   */
  public void invalidateLayout (Container target) {
    // Do nothing
  }

  /*
   * Test and demo code
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("GraphPaperTest");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      JPanel panel = new JPanel(new GraphPaperLayout(4, 4));
      panel.add(new JButton("1"), new Rectangle(0, 0, 1, 1));     // Add a 1x1 Rect at (0, 0)   Box 1
      panel.add(new JButton("2"), new Rectangle(2, 0, 2, 1));     // Add a 2x1 Rect at (2, 0)   Box 2
      panel.add(new JButton("3"), new Rectangle(1, 1, 1, 2));     // Add a 1x2 Rect at (1, 1)   Box 3
      panel.add(new JButton("4"), new Rectangle(2, 1, 2, 2));     // Add a 2x2 Rect at (3, 2)   Box 4
      panel.add(new JButton("5"), new Rectangle(0, 4, 1, 1));     // Add a 1x1 Rect at (0, 4)   Box 5
      panel.add(new JButton("6"), new Rectangle(2, 3, 1, 2));     // Add a 1x2 Rect at (2, 3)   Box 6
      frame.setContentPane(panel);
      frame.setLocationRelativeTo(null);
      frame.pack();
      frame.setVisible(true);
    });
  }
}