/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package jScope;

import java.util.Vector;

/**
 * @author manduchi Basic version of WaveData which keeps arrays for X and Y (old style)
 */
final public class XYWaveData implements WaveData{
    // Inner class AsyncUpdater
    class AsyncUpdater extends Thread{
        double lowerBound;
        double resolution;
        double upperBound;

        AsyncUpdater(final double lowerBound, final double upperBound, final double resolution){
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.resolution = resolution;
        }

        @Override
        public void run() {
            XYWaveData.this.fireListeners(XYWaveData.this.getData(this.lowerBound, this.upperBound, 1000));
        }
    }
    // Inner class LiveUpdater just for test
    class LiveUpdater extends Thread{
        @Override
        public void run() {
            // if(true) return; //REMOVE THIS FOR LIVE UPDATE
            final double[] newX = new double[1];
            final float[] newY = new float[1];
            for(int i = 0; i < XYWaveData.this.x.length; i++){
                try{
                    Thread.sleep(100);
                }catch(final InterruptedException exc){}
                newX[0] = XYWaveData.this.x[XYWaveData.this.x.length - 1] + i + 1;
                newY[0] = XYWaveData.this.y[XYWaveData.this.x.length - 1 - i];
                XYWaveData.this.fireListeners(newX, newY, Double.POSITIVE_INFINITY);
            }
        }
    }
    boolean                  increasingX       = true;
    Vector<WaveDataListener> listeners         = new Vector<WaveDataListener>();
    // For test
    boolean                  liveUpdateStarted = false;
    private final int        type;
    private final double[]   x;
    private final long[]     xLong;
    private final float[]    y;
    private final float[]    z;

    XYWaveData(final double x[], final float y[]){
        this(x, y, x.length);
    }

    XYWaveData(final double x[], final float y[], final float z[]){
        if(z.length != x.length * y.length) System.out.println("INTERNAL ERROR: WRONG DIMENSIONS FOR 2D SIGNAL");
        this.type = Signal.TYPE_2D;
        this.xLong = null;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    XYWaveData(final double x[], final float y[], final int numPoints){
        int len = numPoints;
        if(x.length < len) len = x.length;
        if(y.length < len) len = y.length;
        this.type = Signal.TYPE_1D;
        this.xLong = null;
        this.x = new double[len];
        this.y = new float[len];
        this.z = null;
        System.arraycopy(x, 0, this.x, 0, len);
        System.arraycopy(y, 0, this.y, 0, len);
        this.checkIncreasingX();
    }

    XYWaveData(final float x[], final float y[]){
        this(x, y, x.length);
    }

    XYWaveData(final float x[], final float y[], final float z[]){
        if(z.length != x.length * y.length) System.err.println("INTERNAL ERROR: WRONG DIMENSIONS FOR 2D SIGNAL");
        this.type = Signal.TYPE_2D;
        this.xLong = null;
        this.x = new double[x.length];
        this.y = y;
        this.z = z;
        for(int i = 0; i < this.x.length; i++)
            this.x[i] = x[i];
    }

    XYWaveData(final float x[], final float y[], final int numPoints){
        int len = numPoints;
        if(x.length < len) len = x.length;
        if(y.length < len) len = y.length;
        this.type = Signal.TYPE_1D;
        this.x = new double[len];
        this.y = new float[len];
        this.xLong = null;
        this.z = null;
        System.arraycopy(x, 0, this.x, 0, len);
        System.arraycopy(y, 0, this.y, 0, len);
        this.checkIncreasingX();
    }

    XYWaveData(final long xLong[], final float y[]){
        this.type = Signal.TYPE_1D;
        int len = xLong.length;
        if(y.length < len) len = y.length;
        this.x = new double[len];
        this.y = new float[len];
        this.z = null;
        this.xLong = new long[len];
        System.arraycopy(xLong, 0, this.xLong, 0, len);
        System.arraycopy(y, 0, this.y, 0, len);
        for(int i = 0; i < len; i++)
            this.x[i] = xLong[i];
        this.checkIncreasingX();
    }

    XYWaveData(final long xLong[], final float y[], final float z[]){
        if(z.length != xLong.length * y.length) System.err.println("INTERNAL ERROR: WRONG DIMENSIONS FOR 2D SIGNAL");
        this.x = null;
        this.type = Signal.TYPE_2D;
        this.xLong = xLong;
        this.y = y;
        this.z = z;
    }

    @Override
    public void addWaveDataListener(final WaveDataListener listener) {
        this.listeners.addElement(listener);
    }

    void checkIncreasingX() {
        this.increasingX = true;
        for(int i = 1; i < this.x.length; i++)
            if(this.x[i - 1] > this.x[i]){
                this.increasingX = false;
                return;
            }
    }

    void fireListeners(final double[] x, final float[] y, final double resolution) {
        for(int i = 0; i < this.listeners.size(); i++)
            this.listeners.elementAt(i).dataRegionUpdated(x, y, resolution);
    }

    void fireListeners(final long[] x, final float[] y, final double resolution) {
        for(int i = 0; i < this.listeners.size(); i++)
            this.listeners.elementAt(i).dataRegionUpdated(x, y, resolution);
    }

    void fireListeners(final XYData xydata) {
        if(XYWaveData.this.isXLong()) this.fireListeners(xydata.getXLong(), xydata.getY(), xydata.resolution);
        else this.fireListeners(xydata.getX(), xydata.getY(), xydata.resolution);
    }

    /*
     * Read data within specified interval. either Xmin or xmax can specify no limit (-Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
     */
    @Override
    public XYData getData(double xmin, double xmax, final int numPoints) {
        int minIdx, maxIdx;
        if(!this.increasingX) // If not increasing return bare data
        { return new XYData(this.x, this.y, Double.POSITIVE_INFINITY, false); }
        if(xmin == Double.NEGATIVE_INFINITY){
            xmin = this.x[0];
            minIdx = 0;
        }else{
            for(minIdx = 0; minIdx < this.x.length - 2 && this.x[minIdx] < xmin; minIdx++);
            if(minIdx > 0 && this.x[minIdx] > xmin) minIdx--;
        }
        if(xmax == Double.POSITIVE_INFINITY){
            xmax = this.x[this.x.length - 1];
            maxIdx = this.x.length - 1;
        }else{
            for(maxIdx = minIdx + 1; maxIdx < this.x.length - 1 && this.x[maxIdx] < xmax; maxIdx++);
        }
        // OK, trovato l'intervallo tra minIdx e maxIdx
        final double delta = (xmax - xmin) / numPoints;
        // double retResolution;
        boolean showMinMax = false;
        int actPoints;
        // Forces re-sampling only if there is a significant number of points
        if((maxIdx - minIdx) > 1000 && delta > 4 * (maxIdx - minIdx + 1) / (xmax - xmin)) // If at least there are four times real points
        {
            actPoints = 2 * (int)((xmax - xmin) / delta + 0.5);
            showMinMax = true;
            // retResolution = 1. / delta;
        }else{
            actPoints = maxIdx - minIdx + 1; // No re-sampling at all
            showMinMax = false;
            // retResolution = Double.POSITIVE_INFINITY; // Maximum resolution
        }
        final float retY[] = new float[actPoints];
        final double retX[] = new double[actPoints];
        long retXLong[] = null;
        if(this.isXLong()) retXLong = new long[actPoints];
        if(showMinMax){
            int currIdx = minIdx;
            for(int i = 0; i < actPoints / 2; i++){
                float currMin = this.y[currIdx];
                float currMax = this.y[currIdx];
                final double currStart = this.x[currIdx];
                while(currIdx < this.x.length - 1 && (this.x[currIdx] - currStart) < delta){
                    if(this.y[currIdx] < currMin) currMin = this.y[currIdx];
                    if(this.y[currIdx] > currMax) currMax = this.y[currIdx];
                    currIdx++;
                }
                retX[2 * i] = retX[2 * i + 1] = (currStart + this.x[(currIdx == 0) ? 0 : currIdx - 1]) / 2.;
                if(retXLong != null) retXLong[2 * i] = retXLong[2 * i + 1] = (long)((currStart + this.x[(currIdx == 0) ? 0 : currIdx - 1]) / 2.);
                retY[2 * i] = currMin;
                retY[2 * i + 1] = currMax;
            }
            this.startLiveUpdate();
            if(retXLong != null) return new XYData(retXLong, retY, actPoints / (xmax - xmin), true);
            return new XYData(retX, retY, actPoints / (xmax - xmin), true);
        }
        for(int i = 0; i < maxIdx - minIdx + 1; i++){
            retY[i] = this.y[minIdx + i];
            retX[i] = this.x[minIdx + i];
            if(retXLong != null) retXLong[i] = this.xLong[minIdx + i];
        }
        this.startLiveUpdate();
        if(retXLong != null) return new XYData(retXLong, retY, Double.POSITIVE_INFINITY, true);
        return new XYData(retX, retY, Double.POSITIVE_INFINITY, true);
    }

    @Override
    public XYData getData(final int numPoints) {
        if(this.type != Signal.TYPE_1D){
            System.out.println("INTERNAL ERROR getData called for non 1 D signal");
            return null;
        }
        if(numPoints >= this.x.length) return new XYData(this.x, this.y, Double.POSITIVE_INFINITY, true);
        return this.getData(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, numPoints);
    }

    @Override
    public void getDataAsync(final double lowerBound, final double upperBound, final int numPoints) {
        // (new AsyncUpdater(lowerBound, upperBound, numPoints/(upperBound - lowerBound))).start();
    }

    @Override
    public int getNumDimension() {
        return 1;
    }

    @Override
    public String GetTitle() {
        return "Title";
    }

    @Override
    public double[] getX2D() {
        if(this.type == Signal.TYPE_2D) return this.x;
        System.out.println("INTERNAL ERROR SimpleWave.getZ for 1D signal");
        return null;
    }

    @Override
    public long[] getX2DLong() {
        if(this.type == Signal.TYPE_2D) return this.xLong;
        System.out.println("INTERNAL ERROR SimpleWave.getZ2dLong for 1D signal");
        return null;
    }

    /**
     * Get the associated label for X axis. It is displayed if no X axis label is defined in the setup data definition.
     *
     * @return The X label string.
     * @exception java.io.IOException
     */
    @Override
    public String GetXLabel() {
        return "XLabel";
    }

    public double[] getXLimits() {
        double xmin, xmax;
        xmin = xmax = this.x[0];
        for(final double element : this.x){
            if(element > xmax) xmax = element;
            if(element < xmin) xmin = element;
        }
        return new double[]{xmin, xmax};
    }

    public long[] getXLong() {
        if(this.xLong == null) System.out.println("INTERNAL ERROR: getLong called for non long X");
        return this.xLong;
    }

    @Override
    public float[] getY2D() {
        if(this.type == Signal.TYPE_2D) return this.y;
        System.out.println("INTERNAL ERROR SimpleWave.getZ for 1D signal");
        return null;
    }

    /**
     * Get the associated label for Y axis. It is displayed if no Y axis label is defined in the setup data definition.
     *
     * @return The Y label string.
     * @exception java.io.IOException
     */
    @Override
    public String GetYLabel() {
        return "YLabel";
    }

    @Override
    public float[] getZ() {
        if(this.type == Signal.TYPE_2D) return this.z;
        System.out.println("INTERNAL ERROR SimpleWave.getZ for 1D signal");
        return null;
    }

    /**
     * Get the associated label for Z axis (for 2D signals only). It is displayed if no X axis label is defined in the setup data definition.
     *
     * @return The Z label string.
     * @exception java.io.IOException
     */
    @Override
    public String GetZLabel() {
        return "ZLabel";
    }

    @Override
    public final boolean isXLong() {
        return this.xLong != null;
    }

    @Override
    public void setContinuousUpdate(final boolean continuopusUpdate) {}

    void startLiveUpdate() {
        if(!this.liveUpdateStarted){
            this.liveUpdateStarted = true;
            new LiveUpdater().start();
        }
    }
}
