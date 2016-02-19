/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package jScope;

/**
 * @author manduchi
 */
final public class XYData{
    boolean  increasingX = false;
    int      nSamples    = 0;
    double   resolution;         // Number of points/interval
    double[] x;
    long[]   xLong;
    double   xMin, xMax;
    float[]  y;

    XYData(final double x[], final float y[], final double resolution, final boolean increasingX, final double xMin, final double xMax){
        this.x = x;
        this.y = y;
        this.resolution = resolution;
        this.nSamples = (x.length < y.length) ? x.length : y.length;
        this.increasingX = increasingX;
        this.xMin = xMin;
        this.xMax = xMax;
    }

    XYData(final double[] x, final float[] y, final double resolution){
        this(x, y, resolution, false);
    }

    XYData(final double[] x, final float[] y, final double resolution, final boolean increasingX){
        this.x = x;
        this.y = y;
        this.resolution = resolution;
        this.nSamples = (x.length < y.length) ? x.length : y.length;
        this.MinMax(increasingX);
    }

    XYData(final long[] x, final float[] y, final double resolution){
        this(x, y, resolution, false);
    }

    XYData(final long[] x, final float[] y, final double resolution, final boolean increasingX){
        this.xLong = x;
        this.y = y;
        this.resolution = resolution;
        this.x = new double[x.length];
        for(int i = 0; i < x.length; i++)
            this.x[i] = x[i];
        this.nSamples = (x.length < y.length) ? x.length : y.length;
        this.MinMax(increasingX);
    }

    private void MinMax(final boolean incrX) {
        this.increasingX = true;
        if(this.nSamples == 0) return;
        if(incrX){
            this.xMin = this.x[0];
            this.xMax = this.x[this.x.length - 1];
            return;
        }
        this.xMin = this.xMax = this.x[0];
        for(int i = 1; i < this.x.length; i++){
            if(this.x[i - 1] > this.x[i]) this.increasingX = false;
            if(this.x[i] > this.xMax) this.xMax = this.x[i];
            if(this.x[i] < this.xMin) this.xMin = this.x[i];
        }
    }
}
