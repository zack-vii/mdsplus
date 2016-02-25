/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package jScope;

/**
 * @author manduchi Defines the methods called by WaveData to report regions of increased resolution or new data available
 */
public interface WaveDataListener{
    public void dataRegionUpdated(double[] x, float[] y, double resolution);

    public void dataRegionUpdated(long[] x, float[] y, double resolution);

    public void legendUpdated(String name);

    public void sourceUpdated(XYData xydata);
}
