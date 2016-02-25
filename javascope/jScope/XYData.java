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
        this.xMax = Double.NEGATIVE_INFINITY;
        this.xMin = Double.POSITIVE_INFINITY;
        if(incrX){
            for(int i = 0; !Double.isFinite(this.xMin); i++)
                this.xMin = this.x[i];
            for(int i = this.nSamples - 1; !Double.isFinite(this.xMax); i--)
                this.xMax = this.x[i];
            return;
        }
        for(final double element : this.x){
            if(Double.isNaN(element)) continue;
            if(this.xMax > element) this.increasingX = false;
            if(element > this.xMax) this.xMax = element;
            if(element < this.xMin) this.xMin = element;
        }
    }
}
