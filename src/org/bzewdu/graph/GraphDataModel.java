package org.bzewdu.graph;

/**
 * Represents the data for one graph.
 */

public interface GraphDataModel {
    /**
     * Title of the graph
     */
    public String getTitle();

    /**
     * Number of data points
     */
    public int getNumDataPoints();

    /**
     * Get the i'th data point's value
     */
    public double getDataPoint(int i);

    /**
     * Get the i'th data point's title
     */
    public String getDataPointTitle(int i);
}
