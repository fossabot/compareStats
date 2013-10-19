package org.bzewdu.compare;


import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.TTestImpl;
import org.bzewdu.stats.CompareUtils;
import org.bzewdu.stats.DataSet;
import org.bzewdu.util.Results;
import org.bzewdu.util.Subresults;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Compare {

    static boolean recursive = false;

    private static void usage() {
        System.out.println("Usage: java Compare [-v] [-r] [results dir 1] [results dir 2] ...");
        System.out.println("Prints statistical comparison of two or more benchmark results.");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        ArrayList<String> dirs = new ArrayList<String>();

        if (args.length < 2) {
            usage();
        }

        for (String arg : args) {
            if (arg.equals("-v")) {
                System.err.println("compareStats 0.13");
                System.exit(0);
            }
            if (arg.equals("-r")) {
                recursive = true;
                continue;
            }
            dirs.add(arg);
        }
        ArrayList<Results[]> results = readResultFiles(dirs);

        if (results.size() < 2) {
            System.err.println("2 or more results are required to proceed...");
            System.exit(0);
        }

        Results workload = null;
        for (Results[] res_ : results) {
            for (Results res : res_) {
                if (res.isWorkload()) {
                    workload = res;
                    //weights = res.getWeights();
                    break;
                }
            }
        }
        
        // Print output
        printSeparator();

        printBaselineResult(results.get(0), workload, recursive);

        for (Results[] result : results) {
            if (!results.get(0).equals(result)) {
                printSpecimenResults(workload, results.get(0), result);
            }
        }

        printSeparator();
    }

    private static ArrayList<Results[]> readResultFiles(ArrayList<String> dirs) throws IOException {
        ArrayList<Results[]> results = new ArrayList<Results[]>();
        for (String dir : dirs) {
            results.add(readResults(listResultDirectories(new File(dir))));
        }
        return results;
    }

    private static void printSpecimenResults(Results workload, Results[] results1, Results[] results2) throws IOException {
        printSeparator();
        System.out.print(results2[0].getDirectory());
        		 
        if (workload != null) {
            System.out.println(": " + workload.getName());
        } else {
            System.out.println();
        }
        printSecondHeading();
        printSpecimenResults(results1, results2, workload, recursive);
        if (workload != null) {
            printLine();
            for (Results res : results2) {
                if (res.isWorkload()) {
                    System.out.printf("  Weighted Geomean%23.2f%10.2f",
                            res.getMean(),
                            CompareUtils.percentDiff(workload.getMean(), res.getMean(), workload.isHigherBetter()));
                    System.out.println();
                }
            }
        }
    }

    private static void printBaselineResult(Results[] results1, Results workload, boolean recursive) throws IOException {
        System.out.print(results1[0].getDirectory());

        if (workload != null) {
            System.out.println(": " + workload.getName());
        } else {
            System.out.println();
        }
        printFirstHeading();


        printBaselineResults(results1, (workload != null), recursive);
        if (workload != null) {
            printLine();
            System.out.printf("  Weighted Geomean%22.2f", workload.getMean());
            System.out.println();
        }
    }

    private static void printSeparator() {
        for (int i = 0; i < 80; i++) System.out.print("=");
        System.out.println();
    }

    private static void printLine() {
        System.out.print("  ");
        for (int i = 0; i < 78; i++) System.out.print("-");
        System.out.println();
    }

    private static void printFirstHeading() {
        // Column number      2                   22             37       46                65
        System.out.println("  Benchmark           Samples        Mean     Stdev");//        Geomean Weight
    }

    private static void printSecondHeading() {
        // Column number      2                   22             37       46        54         63   67
        System.out.println("  Benchmark           Samples        Mean     Stdev     %Diff     P  Significant");
    }

    private static void printBaselineResults(Results[] baselineResults,
                                             boolean haveBaselineWorkload,
                                             boolean recursive) throws IOException {
        for (Results res : baselineResults) {
            if (!res.isWorkload()) {
                System.out.printf("  %-24s%3d%12.2f%10.2f",
                        res.getName(),
                        res.getSuccesses(),
                        res.getMean(),
                        res.getStddev());
                System.out.println();
            }
            if (recursive) {
                // Extract and print subresults from this directory
                Subresults subres = new Subresults(res.getDirectory(), false);
                for (String subbenchmark : subres.benchmarkNames()) {
                    DataSet data = subres.get(subbenchmark);
                    // FIXME:  deduce which geomean
                    // weight goes with which benchmark
                    System.out.printf("    %-22s%3d%12.2f%10.2f",
                            subbenchmark,
                            data.numSamples(),
                            data.mean(),
                            data.stddev());
                    System.out.println();
                }
            }
        }

         if (!haveBaselineWorkload &&
                recursive &&
                (baselineResults.length > 0) &&
                (new File(baselineResults[0].getDirectory(), "results." + baselineResults[0].getRandomSubresultName()).exists())) {
            for (Results res : baselineResults) {
                File[] dirs = listResultDirectories(res.getDirectory());
                // Convert to Results
                Results[] results = readResults(dirs);
                // Recur
                printBaselineResults(results, haveBaselineWorkload, recursive);
            }
        }
    }

    private static void printSpecimenResults(Results[] baselineResults,
                                             Results[] specimenResults,
                                             Results baselineWorkload,
                                             boolean recursive) throws IOException {
        for (int i = 0; i < baselineResults.length; i++) {
            Results res1 = baselineResults[i];
            Results res2 = specimenResults[i];
            if (!res2.isWorkload()) {
                /*
                 * See API documentation for apache-commons-math 
                 * http://jakarta.apache.org/commons/math/apidocs/org/apache/commons/math/stat/inference/TTest.html
                 */
                TTestImpl ttest = new TTestImpl();
                double tValue = ttest.t(res2.getScores().toDoubleArray(), res1.getScores().toDoubleArray());
                double pValue = 0;

                try {
                      pValue = ttest.tTest(res2.getScores().toDoubleArray(), res1.getScores().toDoubleArray());
                } catch (MathException e) {
                    System.err.println("Encountered MathException: " + e.getMessage());
                    System.err.println("Exiting.");
                    e.printStackTrace();
                    System.exit(-1);
                }

                boolean isHigherBetter =
                        (baselineWorkload != null) ? baselineWorkload.isHigherBetter() : res2.isHigherBetter();

                System.out.printf("  %-24s%3d%12.2f%10.2f%10.2f%6.3f%13s",
                        res2.getName(),
                        res2.getSuccesses(),
                        res2.getMean(),
                        res2.getStddev(),
                        CompareUtils.percentDiff(res1.getMean(), res2.getMean(), isHigherBetter),
                        pValue,
                        (pValue < 0.01) ? "Yes" : "*");
                System.out.println();
                if (recursive) {
                    Subresults subres1 = new Subresults(res1.getDirectory(), false);
                    Subresults subres2 = new Subresults(res2.getDirectory(), false);
                    for (String subbenchmark : subres1.benchmarkNames()) {
                        DataSet data1 = subres1.get(subbenchmark);
                        DataSet data2 = subres2.get(subbenchmark);
                        try {
                              pValue = ttest.tTest(data2.toDoubleArray(), data1.toDoubleArray());
                        } catch (MathException e) {
                              System.err.println("Encountered MathException: " + e.getMessage());
                              System.err.println("Exiting.");
                              e.printStackTrace();
                              System.exit(-1);
                        }

                        System.out.printf("    %-22s%3d%12.2f%10.2f%10.2f%6.3f%13s",
                                subbenchmark,
                                data2.numSamples(),
                                data2.mean(),
                                data2.stddev(),
                                CompareUtils.percentDiff(data1.mean(), data2.mean(), res2.isSubresultHigher(subbenchmark)), pValue, (pValue < 0.01) ? "Yes" : "*");
                                System.out.println();
                    }
                }
            }
        }

         if ((baselineWorkload == null) &&
                recursive &&
                (baselineResults.length > 0) &&
                (new File(baselineResults[0].getDirectory(), "results." + baselineResults[0].getRandomSubresultName()).exists())) {
            for (int i = 0; i < baselineResults.length; i++) {
                File[] dirs1 = listResultDirectories(baselineResults[i].getDirectory());
                File[] dirs2 = listResultDirectories(specimenResults[i].getDirectory());
                // Convert to Results
                Results[] results1 = readResults(dirs1);
                Results[] results2 = readResults(dirs2);
                // Recur
                printSpecimenResults(results1, results2, baselineWorkload, recursive);
            }
        }
    }

    private static File[] listResultDirectories(File baseDir) throws IOException {
        File[] dirs = baseDir.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return (f.getName().startsWith("results.") &&
                        f.isDirectory());
            }
        });
        if (dirs == null) {
            throw new IOException("Directory not found: " + baseDir);
        }
        Arrays.sort(dirs);
        return dirs;
    }

    private static Results[] readResults(File[] resultDirs) throws IOException {
        Results[] results = new Results[resultDirs.length];
        for (int i = 0; i < resultDirs.length; i++) {
            results[i] = new Results(resultDirs[i]);
        }
        return results;
    }

}
