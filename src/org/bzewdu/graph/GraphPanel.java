package org.bzewdu.graph;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class GraphPanel extends JButton {
    private Color borderColor = Color.YELLOW;
    private Color axisColor = Color.WHITE;
    private Color dataBGColor = Color.DARK_GRAY;
    private float h = 0.8f;
    private float l = 0.2f;
    private Color[] dataColors = new Color[] {
            new Color(h, l, l),
            new Color(l, h, l),
            new Color(l, l, h),
            new Color(h, h, l),
            new Color(l, h, h),
            new Color(h, l, h)
    };
    private static Font baseFont = new Font("SansSerif", Font.PLAIN, 24);
    private Font titleFont;
    private Font dataPointFont;
    private boolean borderEnabled = false;
    private GraphDataModel data;

    public GraphPanel() {
        setBorder(new GraphBorder());
        setBackground(Color.BLACK);
        //    setBackground(new Color(0, 0, 0, 0));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                borderEnabled = true;
                repaint();
            }

            public void mouseExited(MouseEvent e) {
                borderEnabled = false;
                repaint();
            }
        });
    }

    public void setBorderColor(Color c) {
        borderColor = c;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setData(GraphDataModel data) {
        this.data = data;
    }

    public GraphDataModel getData() {
        return data;
    }

    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        g.setColor(getBackground());
        g.fillRect(0, 0, width, height);

        if (data != null) {
            // Title
            g.setColor(axisColor);
            g.setFont(titleFont);
            drawRelativeCenteredString(g, 0.5, 0.05, data.getTitle());

            // Data points
            g.setFont(dataPointFont);
            double rectWidth = 0.8 / (3 * data.getNumDataPoints());
            double rectSpacing = 0.8 / (1 + data.getNumDataPoints());
            // Normalize all points to largest
            double max = 0;
            for (int i = 0; i < data.getNumDataPoints(); i++) {
                max = Math.max(max, data.getDataPoint(i));
            }
            for (int i = 0; i < data.getNumDataPoints(); i++) {
                // Bar
                double pt = data.getDataPoint(i);
                double x = (0.1 + ((i + 1) * rectSpacing)) - (rectWidth / 2);
                // Go up from bottom
                double y = 1 - (0.1 + (0.8 * (pt / max)));
                double w = rectWidth;
                double h = 0.8 * (pt / max);
                g.setColor(dataBGColor);
                double spacer = 0.005;
                fillRelativeRect(g, x - spacer, y - spacer, w + 2 * spacer, h + 2 * spacer);
                g.setColor(dataColors[i % dataColors.length]);
                fillRelativeRect(g, x, y, w, h);

                // Title
                g.setColor(axisColor);
                drawRelativeCenteredString(g, 0.1 + ((i + 1) * rectSpacing), 0.95, data.getDataPointTitle(i));
            }
        }

        // Axes
        g.setColor(axisColor);
        fillRelativeRect(g, 0.1, 0.1, 0.01, 0.8);
        fillRelativeRect(g, 0.1, 0.9, 0.8, 0.01);
    }

    public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
        titleFont = baseFont.deriveFont((float) height / 20);
        dataPointFont = baseFont.deriveFont((float) height / 26);
    }

    private void fillRelativeRect(Graphics g,
                                  double x,
                                  double y,
                                  double width,
                                  double height) {
        int rx = (int) Math.ceil((x * getWidth()));
        int ry = (int) Math.ceil((y * getHeight()));
        int rw = (int) Math.ceil((width * getWidth()));
        int rh = (int) Math.ceil((height * getHeight()));
        g.fillRect(rx, ry, rw, rh);
    }

    private void drawRelativeCenteredString(Graphics g,
                                            double centerX,
                                            double centerY,
                                            String str) {
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D bounds = fm.getStringBounds(str, g);
        int startX = (int) (Math.ceil(centerX * getWidth()) - (bounds.getWidth() / 2));
        int startY = (int) (Math.ceil(centerY * getHeight()) + (bounds.getHeight() / 2));
        g.drawString(str, startX, startY);
    }


    class GraphBorder implements Border {
        public Insets getBorderInsets(Component c) {
            int val = computeBorderWidth(c);
            return new Insets(val, val, val, val);
        }

        public boolean isBorderOpaque() {
            return borderEnabled;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (borderEnabled) {
                int val = computeBorderWidth(c);
                g.setColor(borderColor);
                g.fillRect(x, y, val, height);
                g.fillRect(x, y, width, val);
                g.fillRect(x + width - val, y, val, height);
                g.fillRect(x, y + height - val, width, val);
            }
        }

        private int computeBorderWidth(Component c) {
            // Make border a percentage of the component's size
            int min = Math.min(c.getWidth(), c.getHeight());
            return Math.max(1, min / 200);
        }
    }
}
