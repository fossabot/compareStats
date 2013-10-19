package org.bzewdu.util;

import org.bzewdu.stats.DataSet;
import org.bzewdu.stats.CompareUtils;
import org.bzewdu.stats.IncomparableResultsException;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class Results {
    private String name;
    private File directory;
    private boolean isWorkload;
    private boolean isHigherBetter;
    private boolean failed;
    private double score;
    private double mean;
    private double stddev;
    private double var;
    private DataSet scores;
    private DataSet weights;
    private int attempts;
    private int successes;
    private int failures;
    private Set<String> higherSubresults;
    private Set<String> lowerSubresults;

    /**
     * Parses the results.* file in the specified directory, figuring
     * out from that file which benchmark is being scored.
     */
    public Results(File directory) throws IOException {
        this.directory = directory;
        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File path) {
                return (path.getName().startsWith("results.") &&
                        (!path.isDirectory()));
            }
        });
        if (files.length == 0) {
            throw new IOException("No results file found in directory " + directory);
        }
        if (files.length != 1) {
            throw new RuntimeException("Expected 1 results file, found " + files.length +
                    " in directory " + directory);
        }
        name = files[0].getName().substring("results.".length());
        Properties props = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(files[0]));
        props.load(in);
        in.close();
        // Parse
        try {
            isWorkload = (Integer.parseInt(props.getProperty("is_workload")) == 1);
            isHigherBetter = (Integer.parseInt(props.getProperty("is_higher_better")) == 1);
            score = Float.parseFloat(props.getProperty("score"));
            scores = new DataSet();
            for (String s : props.getProperty("scores").split(" ")) {
                scores.add(Double.parseDouble(s));
            }
            if (isWorkload()) {
                weights = new DataSet();
                for (String s : props.getProperty("weights").split(" ")) {
                    weights.add(Double.parseDouble(s));
                }
                failed = (Integer.parseInt(props.getProperty("failed")) == 1);
            } else {
                mean = Float.parseFloat(props.getProperty("mean"));
                stddev = Float.parseFloat(props.getProperty("stdev"));
                var = Float.parseFloat(props.getProperty("var"));
                attempts = Integer.parseInt(props.getProperty("attempts"));
                successes = Integer.parseInt(props.getProperty("successes"));
                failures = Integer.parseInt(props.getProperty("failures"));
                higherSubresults = new HashSet<String>();
                higherSubresults.addAll(Arrays.asList(props.getProperty("subresults_higher").split(" ")));
                // Handle case where string was empty
                higherSubresults.remove("");
                lowerSubresults = new HashSet<String>();
                lowerSubresults.addAll(Arrays.asList(props.getProperty("subresults_lower").split(" ")));
                // Handle case where string was empty
                lowerSubresults.remove("");
            }
        } catch (Exception e) {
            throw (IOException) new IOException("Error parsing results file " + files[0].getAbsolutePath()).initCause(e);
        }
    }

    public File getDirectory() {
        return directory;
    }

    public String getName() {
        return name;
    }

    public boolean isWorkload() {
        return isWorkload;
    }

    public boolean isHigherBetter() {
        return isHigherBetter;
    }

    public double getMean() {
        return mean;
    }

    public double getStddev() {
        return stddev;
    }

    public double getVar() {
        return var;
    }

    public DataSet getScores() {
        return scores;
    }

    public DataSet getWeights() {
        return weights;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getSuccesses() {
        return successes;
    }

    public int getFailures() {
        return failures;
    }

    public boolean isSubresultHigher(String subresultName) throws IllegalArgumentException {
        checkSubresult(subresultName);
        return higherSubresults.contains(subresultName);
    }

    public boolean isSubresultLower(String subresultName) throws IllegalArgumentException {
        checkSubresult(subresultName);
        return !higherSubresults.contains(subresultName);
    }

    // Used for checking to see whether we need to recur in the Compare script
    public String getRandomSubresultName() {
        for (String higherSubresult : higherSubresults) {
            return higherSubresult;
        }
        for (String lowerSubresult : lowerSubresults) {
            return lowerSubresult;
        }
        return null;
    }

    private void checkSubresult(String subresultName) {
        if (higherSubresults.contains(subresultName) || lowerSubresults.contains(subresultName)) {
            return;
        }
        throw new IllegalArgumentException("Unknown sub-benchmark name " + subresultName);
    }
    
    public static double percentDiff(Results baseline, Results specimen) throws IncomparableResultsException {
        if (baseline.isHigherBetter() != specimen.isHigherBetter()) {
             throw new IncomparableResultsException("Specimen and baseline disposition don't match");
        }
        return CompareUtils.percentDiff(baseline.getMean(), specimen.getMean(), specimen.isHigherBetter);
    }
}
