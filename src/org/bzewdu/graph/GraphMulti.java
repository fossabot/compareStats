package org.bzewdu.graph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Lays out multiple GraphPanels using a ZoomableLayout in a given
// Container.

public class GraphMulti {
    private ZoomableLayout zoom;
    private Container parent;
    private java.util.List<GraphPanel> panels = new ArrayList<GraphPanel>();

    public GraphMulti(Container parent,
                      List<GraphDataModel> data) {
        this.parent = parent;
        zoom = new ZoomableLayout(parent);
        parent.setLayout(zoom);
        for (GraphDataModel aData : data) {
            GraphPanel panel = new GraphPanel();
            panel.setData(aData);
            panel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        zoomIn(e.getComponent());
                    } else {
                        zoomOut();
                    }
                }
            });
            panels.add(panel);
            parent.add(panel);
        }
        layout();
        parent.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });
        Thread t = new Thread(new Worker(), "Zoom Worker");
        t.start();
    }

    private void relayout() {
        for (GraphPanel panel : panels) {
            zoom.removeLayoutComponent(panel);
        }
        layout();
    }

    private void layout() {
        if (parent.getWidth() == 0 || parent.getHeight() == 0) {
            return;
        }

        float aspectRatio = (float) parent.getWidth() / (float) parent.getHeight();
        int numPerLine = (int) Math.ceil(Math.sqrt(aspectRatio * panels.size()));
        int numLines = (int) Math.ceil((float) panels.size() / (float) numPerLine);
        int numLeft = panels.size();
        double w = 0.85 / (float) (1 + numPerLine);
        double h = 0.85 / (float) (1 + numLines);
        double yStep = 1.2f / (float) (1 + numLines);
        double y = (1.0 - (numLines - 1) * yStep) / 2;
        Iterator<GraphPanel> iter = panels.iterator();
        for (int i = 0; i < numLines; i++) {
            int numOnThisLine = (numLeft >= numPerLine) ? numPerLine : numLeft;
            numLeft -= numOnThisLine;
            double xStep = 1.2f / (float) (1 + numOnThisLine);
            double x = (1.0 - (numOnThisLine - 1) * xStep) / 2;
            for (int j = 0; j < numOnThisLine; j++) {
                zoom.add(iter.next(), x - w / 2, y - h / 2, w, h);
                x += xStep;
            }
            y += yStep;
        }
    }

    private void zoomIn(Component c) {
        Rectangle2D.Double rect = zoom.getComponentBounds(c);
        if (curTask == null) {
            synchronized (taskLock) {
                curTask = new ZoomTask(curRect, rect);
                taskLock.notifyAll();
            }
        }
    }

    private void zoomOut() {
        if (curTask == null) {
            synchronized (taskLock) {
                curTask = new ZoomTask(curRect, initRect);
                taskLock.notifyAll();
            }
        }
    }

    class ZoomTask {
        private Rectangle2D.Double startRect;
        private Rectangle2D.Double destRect;
        private long startTime;
        private long endTime;

        ZoomTask(Rectangle2D.Double startRect,
                 Rectangle2D.Double destRect) {
            this.startRect = startRect;
            this.destRect = destRect;
            startTime = System.currentTimeMillis();
            endTime = startTime + 500;
        }

        public void run() {
            long curTime = System.currentTimeMillis();
            double alpha = (double) (curTime - startTime) / (double) (endTime - startTime);
            if (alpha >= 1.0) {
                curTask = null;
                curRect = destRect;
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            zoom.setViewRectangle(curRect.getX(), curRect.getY(), curRect.getWidth(), curRect.getHeight());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                alpha = sigmoid(alpha, 10);
                final double x = destRect.getX() * alpha + startRect.getX() * (1 - alpha);
                final double y = destRect.getY() * alpha + startRect.getY() * (1 - alpha);
                final double w = destRect.getWidth() * alpha + startRect.getWidth() * (1 - alpha);
                final double h = destRect.getHeight() * alpha + startRect.getHeight() * (1 - alpha);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            zoom.setViewRectangle(x, y, w, h);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private double sigmoid(double alpha, double weight) {
        // Clamp incoming alpha
        alpha = Math.max(0.0, Math.min(1.0, alpha));

        // sigmoid function.
        // S(x) = 1/(1+e^(-w*x))  : where w is the specified weight, and x
        // is the distance from the middle of the duration.
        double middle = alpha - 0.5;
        double exponent = weight * middle;
        return (1.0 - (1.0 / (1.0 + Math.exp(exponent))));
    }

    private ZoomTask curTask;
    private final Object taskLock = new Object();
    private Rectangle2D.Double initRect = new Rectangle2D.Double(0, 0, 1, 1);
    
    private Rectangle2D.Double curRect = initRect;

    class Worker implements Runnable {
        public void run() {
            while (true) {
                synchronized (taskLock) {
                    while (curTask == null) {
                        try {
                            taskLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                while (curTask != null) {
                    curTask.run();
                }
            }
        }
    }
}
