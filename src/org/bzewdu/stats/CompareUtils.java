package org.bzewdu.stats;

import org.bzewdu.util.Results;

public class CompareUtils {
    public static double percentDiff(double baseline, double specimen, boolean isHigherBetter) {
        if (baseline == 0) {
            if (specimen > 0 && isHigherBetter) {
                return Double.POSITIVE_INFINITY;
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }

        double diff = ((specimen - baseline) / baseline) * 100.0;
        if (!isHigherBetter) {
            diff *= -1.0;
        }
        return diff;
    }
}

