package org.bzewdu.util;

import org.bzewdu.stats.DataSet;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Subresults {
    Map<String, DataSet> data = new HashMap<String, DataSet>();

    /**
     * Parses all subresults files in the given directory. Equivalent to Subresults(directory, true).
     */
    public Subresults(File directory) throws IOException {
        this(directory, true);
    }

    /**
     * Parses all subresults files in the given directory.
     * parseComposite indicates whether to parse the overall score as
     * one of the sub-benchmarks.
     */
    public Subresults(File directory, boolean parseComposite) throws IOException {
        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname.getName().startsWith("subresults."));
            }
        });
         for (File file : files) {
            Properties props = new Properties();
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            props.load(in);
            in.close();
            for (Object result : props.keySet()) {
                String resultName = (String) result;
                DataSet set = data.get(resultName);
                if (set == null) {
                    set = new DataSet();
                    data.put(resultName, set);
                }
                try {
                    set.add(Double.parseDouble((String) props.get(resultName)));
                } catch (NumberFormatException e) {
                    // Skip invalid data points
                }
            }
        }
        if (parseComposite) {
            // Parse the composite score as well out of the results.[benchmarkName] file
            files = directory.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return (pathname.getName().startsWith("results."));
                }
            });
            if (files != null && files.length > 0) {
                Properties props = new Properties();
                InputStream in = new BufferedInputStream(new FileInputStream(files[0]));
                props.load(in);
                in.close();
                DataSet set = new DataSet();
                try {
                    set.add(Double.parseDouble((String) props.get("score")));
                    data.put(files[0].getName().substring("results.".length()) + " composite", set);
                } catch (Exception e) {
                    // Skip any kind of error with this
                }
            }
        }
    }

    public Set<String> benchmarkNames() {
        return data.keySet();
    }

    public DataSet get(String benchmarkName) {
        return data.get(benchmarkName);
    }

    public static void main(String[] args) {
        for (String arg : args) {
            try {
                Subresults sub = new Subresults(new File(arg));
                System.out.println(arg);
                System.out.println("---");
                for (String benchmarkName : sub.benchmarkNames()) {
                    DataSet data = sub.get(benchmarkName);
                    System.out.println(benchmarkName + ": mean " + data.mean() + ", stddev " + data.stddev());
                }
            } catch (IOException e) {
            }
        }
    }
}
