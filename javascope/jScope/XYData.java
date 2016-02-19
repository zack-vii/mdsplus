/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package jScope;

/**
 * @author manduchi
 */
final public class XYData{
    double   resolution;         // Number of points/interval
    boolean  increasingX = false;
    int      nSamples    = 0;
    double[] x;
    long[]   xLong;
    float[]  y;
    double   xMin, xMax;

    XYData(double x[], float y[], double resolution, boolean increasingX, double xMin, double xMax){
        this.x = x;
        this.y = y;
        this.resolution = resolution;
        nSamples = (x.length < y.length) ? x.length : y.length;
        this.increasingX = increasingX;
        this.xMin = xMin;
        this.xMax = xMax;
    }

    XYData(double[] x, float[] y, double resolution){
        this(x, y, resolution, false);
    }

    XYData(double[] x, float[] y, double resolution, boolean increasingX){
        this.x = x;
        this.y = y;
        this.resolution = resolution;
        nSamples = (x.length < y.length) ? x.length : y.length;
        MinMax(increasingX);
    }

    XYData(long[] x, float[] y, double resolution){
        this(x, y, resolution, false);
    }

    XYData(long[] x, float[] y, double resolution, boolean increasingX){
        this.xLong = x;
        this.y = y;
        this.resolution = resolution;
        this.x = new double[x.length];
        for(int i = 0; i < x.length; i++)
            this.x[i] = x[i];
        nSamples = (x.length < y.length) ? x.length : y.length;
        MinMax(increasingX);
    }

    private void MinMax(boolean incrX) {
        increasingX = true;
        if(nSamples == 0) return;
        if(incrX){
            xMin = x[0];
            xMax = x[x.length - 1];
            return;
        }
        xMin = xMax = x[0];
        for(int i = 1; i < x.length; i++){
            if(x[i - 1] > x[i]) increasingX = false;
            if(x[i] > xMax) xMax = x[i];
            if(x[i] < xMin) xMin = x[i];
        }
    }
}
