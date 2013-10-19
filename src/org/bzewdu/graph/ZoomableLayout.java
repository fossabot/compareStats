package org.bzewdu.graph;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// This class manages pushing around of the components it knows about
// within a specified Container. It interacts with its managed
// components only through setLocation() and setSize(). You specify
// each component's location and size in normalized coordinates: (0,
// 0) is the upper left of the parent's container and (1, 1) is the
// lower right. You can zoom the view to a particular normalized width
// and height as well.

public class ZoomableLayout implements LayoutManager {
    private Container parent;
    private double viewX = 0.0f;
    private double viewY = 0.0f;
    private double viewW = 1.0f;
    private double viewH = 1.0f;

    class ComponentInfo {
        private Component c;
        private double nx;
        private double ny;
        private double nw;
        private double nh;

        ComponentInfo(Component c,
                      double nx, double ny, double nw, double nh) {
            this.c = c;
            this.nx = nx;
            this.ny = ny;
            this.nw = nw;
            this.nh = nh;
        }

        public Component c() {
            return c;
        }

        public double nx() {
            return nx;
        }

        public double ny() {
            return ny;
        }

        public double nw() {
            return nw;
        }

        public double nh() {
            return nh;
        }

        public void update(int parentWidth, int parentHeight) {
            int x = (int) Math.ceil(parentWidth * (nx - viewX) / viewW);
            int w = (int) Math.ceil(parentWidth * (nw / viewW));
            int y = (int) Math.ceil(parentHeight * (ny - viewY) / viewH);
            int h = (int) Math.ceil(parentHeight * (nh / viewH));

            c.setLocation(x, y);
            c.setSize(w, h);
        }
    }

    private List<ComponentInfo> componentInfo = new ArrayList<ComponentInfo>();

    public ZoomableLayout(Container parent) {
        this.parent = parent;
    }

    public void add(Component c,
                    double nx, double ny, double nw, double nh) {
        componentInfo.add(new ComponentInfo(c, nx, ny, nw, nh));
    }

    public Rectangle2D.Double getComponentBounds(Component c) {
        for (ComponentInfo info : componentInfo) {
            if (info.c() == c) {
                return new Rectangle2D.Double(info.nx(), info.ny(), info.nw(), info.nh());
            }
        }
        return null;
    }

    // This only sets the view rectangle; the application is responsible
    // for causing the parent's layout to be invalidated
    public void setViewRectangle(double x, double y, double width, double height) {
        viewX = x;
        viewY = y;
        viewW = width;
        viewH = height;
        parent.invalidate();
        parent.validate();
    }

    public void addLayoutComponent(String name, Component comp) {
        //    throw new RuntimeException("Do not call this, call add(Component, double, double, double, double) instead");
    }

    // Should be run on AWT event queue thread
    public void layoutContainer(Container parent) {
        int width = parent.getWidth();
        int height = parent.getHeight();

        for (ComponentInfo info : componentInfo) {
            info.update(width, height);
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        return parent.getMinimumSize();
    }

    public Dimension preferredLayoutSize(Container parent) {
        return parent.getPreferredSize();
    }

    public void removeLayoutComponent(Component comp) {
        for (Iterator<ComponentInfo> iter = componentInfo.iterator(); iter.hasNext();) {
            ComponentInfo info = iter.next();
            if (info.c() == comp) {
                iter.remove();
                return;
            }
        }
    }
}
