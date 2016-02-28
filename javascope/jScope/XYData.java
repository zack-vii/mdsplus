/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package jScope;

/**
 * @author manduchi
 */
final public class XYData{
    public final boolean   increasingX;
    private final int      nSamples;
    public final double    resolution; // Number of points/interval
    private final double[] x;
    private final long[]   xLong;
    private final double   xMin, xMax;
    private final float[]  y;

    public XYData(final double x[], final float y[], final double resolution, final boolean increasingX, final double xMin, final double xMax){
        this.x = x;
        this.xLong = null;
        this.y = y;
        this.resolution = resolution;
        this.nSamples = (x.length < y.length) ? x.length : y.length;
        this.increasingX = increasingX;
        this.xMin = xMin;
        this.xMax = xMax;
    }

    public XYData(final double[] x, final float[] y, final double resolution){
        this(x, null, y, resolution, false);
    }

    XYData(final double[] x, final float[] y, final double resolution, final boolean increasingX){
        this(x, null, y, resolution, increasingX);
    }

    private XYData(final double[] x, final long[] xLong, final float[] y, final double resolution, boolean increasingX){
        if(xLong == null) this.x = x;
        else{
            this.x = new double[xLong.length];
            for(int i = 0; i < xLong.length; i++)
                x[i] = xLong[i];
        }
        this.xLong = xLong;
        this.y = y;
        this.resolution = resolution;
        this.nSamples = (x.length < y.length) ? x.length : y.length;
        if(this.isEmpty()){
            this.xMax = Double.NEGATIVE_INFINITY;
            this.xMin = Double.POSITIVE_INFINITY;
        }else if(increasingX){
            int i;
            for(i = 0; !Double.isFinite(this.x[i]); i++);
            this.xMin = this.x[i];
            for(i = this.nSamples - 1; !Double.isFinite(this.x[i]); i--);
            this.xMax = this.x[i];
        }else{
            double xmax = Double.NEGATIVE_INFINITY;
            double xmin = Double.POSITIVE_INFINITY;
            increasingX = false;
            for(final double element : this.x){
                if(Double.isNaN(element)) continue;
                increasingX &= (xmax <= element);
                if(element < xmin) xmin = element;
                if(element > xmax) xmax = element;
            }
            this.xMin = xmin;
            this.xMax = xmax;
        }
        this.increasingX = increasingX;
    }

    public XYData(final long[] xLong, final float[] y, final double resolution){
        this(null, xLong, y, resolution, false);
    }

    public XYData(final long[] xLong, final float[] y, final double resolution, final boolean increasingX){
        this(null, xLong, y, resolution, increasingX);
    }

    public final double[] getX() {
        return this.x;
    }

    public final long[] getXLong() {
        return this.xLong;
    }

    public final double getXMax() {
        return this.xMax;
    }

    public final double getXMin() {
        return this.xMin;
    }

    public final float[] getY() {
        return this.y;
    }

    public final boolean isEmpty() {
        return this.nSamples == 0;
    }
}
