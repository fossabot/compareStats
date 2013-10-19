package org.bzewdu.stats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataSet implements Iterable<Number> {
    private List<Number> data = new ArrayList<Number>();
    private boolean dirty = true;
    private double mean;
    private double variance;
    private double stddev;

    public void add(double dataPoint) {
        dirty = true;
        data.add(dataPoint);
    }

    public boolean remove(double dataPoint) {
        dirty = true;
        return data.remove(dataPoint);
    }

    /**
     * Returns an Iterator over java.lang.Double objects in the data
     * set.
     */
    public Iterator<Number> iterator() {
        return data.iterator();
    }

    public double mean() {
        if (dirty)
            recompute();
        return mean;
    }

    public double stddev() {
        if (dirty)
            recompute();
        return stddev;
    }

    public double variance() {
        if (dirty)
            recompute();
        return variance;
    }

    public int numSamples() {
        return data.size();
    }

    private void recompute() {
        mean = 0;
        stddev = 0;
        variance = 0;

        // Note: we use the same Numerical Recipes-derived algorithm for
        // reducing the rounding error in the variance computation as is
        // used in stats.awk.

        int numPts = 0;
        for (Number value : this) {
            if (value.doubleValue() >= 0) {
                ++numPts;
                mean += value.doubleValue();
            }
        }

        if (numPts > 1) {
            mean /= (double) numPts;

            double err = 0;

            for (Number value : this) {
                if (value.doubleValue() >= 0) {
                    double dev = value.doubleValue() - mean;
                    err += dev;
                    variance = variance + dev * dev;
                }
            }
            variance = (variance - (err * err) / numPts) / (numPts - 1);
            stddev = Math.sqrt(variance);
        }

        dirty = false;
    }
    
    public double[] toDoubleArray() {
        int i = 0;
        double[] d = new double[data.size()];
        for (Number num : this) {
            d[i++] = num.doubleValue();
        }
        return d;
    }
}
