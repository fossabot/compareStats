package org.bzewdu.graph;


import org.bzewdu.stats.DataSet;
import org.bzewdu.util.Subresults;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GraphRW {
    private static void usage() {
        System.out.println("Usage: java GraphRW [JDK identifier string] [subresults directory] ...");
        System.out.println("Graphs multiple benchmarks' subresults from multiple JDKs.");
        System.out.println("JDKs are displayed in graphs in the order they are specified");
        System.out.println("on the command line.");
        System.out.println("Example invocation: ");
        System.out.println("java GraphRW \"JDK 1.4.2\" .../results-142/results \\");
        System.out.println("                 \"JDK 5\" .../results-15/results \\");
        System.out.println("                 \"JDK 6\" .../results-16/results \\");
        System.out.println("Once window is visible, left-click on a particular benchmark");
        System.out.println("to zoom in, and right-click to zoom back out.");
        System.exit(1);
    }

    private static List<GraphDataModel> buildDataModel(final String[] experimentNames,
                                                       final Subresults[] subresults) {
        List<GraphDataModel> data = new ArrayList<GraphDataModel>();
        Subresults base = subresults[0];
        for (final String benchmarkName : base.benchmarkNames()) {
            // Find benchmark in all JDKs
            data.add(new GraphDataModel() {
                public String getTitle() {
                    return benchmarkName;
                }

                public int getNumDataPoints() {
                    return experimentNames.length;
                }

                public double getDataPoint(int i) {
                    DataSet datum = subresults[i].get(benchmarkName);
                    if (datum == null) return 0;
                    return datum.mean();
                }

                public String getDataPointTitle(int i) {
                    return experimentNames[i];
                }
            });
        }
        return data;
    }

    public static void main(String[] args) throws IOException {
        if ((args.length == 0) || ((args.length % 2) != 0))
            usage();

        String[] experimentNames = new String[args.length / 2];
        Subresults[] subresults = new Subresults[args.length / 2];
        int i = 0;
        int j = 0;
        while (i < args.length) {
            experimentNames[j] = args[i++];
            subresults[j] = new Subresults(new File(args[i++]));
            j++;
        }
        Toolkit.getDefaultToolkit().setDynamicLayout(true);
        final JFrame frame = new JFrame("Benchmark results");
        frame.getContentPane().setBackground(Color.BLACK);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GraphMulti multi = new GraphMulti(frame.getContentPane(), buildDataModel(experimentNames, subresults));
        frame.setSize(640, 480);
        frame.setVisible(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.invalidate();
                frame.validate();
            }
        });
    }
}
