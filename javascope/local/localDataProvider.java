package local;

/* $Id$ */
import java.awt.Dimension;
import java.io.IOException;
import java.util.Vector;
import javax.swing.JFrame;
import jScope.Array.RealArray;
import jScope.AsynchDataSource;
import jScope.ConnectionListener;
import jScope.DEBUG;
import jScope.DataProvider;
import jScope.DataServerItem;
import jScope.FrameData;
import jScope.UpdateEvent;
import jScope.UpdateEventListener;
import jScope.WaveData;
import jScope.WaveDataListener;
import jScope.XYData;
import jScope.jScopeFacade;

public class localDataProvider implements DataProvider{
    static class EventDescriptor{
        String              event;
        int                 evId;
        UpdateEventListener listener;

        EventDescriptor(final UpdateEventListener listener, final String event, final int evId){
            this.listener = listener;
            this.event = event;
            this.evId = evId;
        }

        @Override
        public boolean equals(final Object obj) {
            if(!(obj instanceof EventDescriptor)) return false;
            final EventDescriptor evDescr = (EventDescriptor)obj;
            return this.listener == evDescr.getListener() && this.event.equals(evDescr.getEvent());
        }

        String getEvent() {
            return this.event;
        }

        int getEvId() {
            return this.evId;
        }

        UpdateEventListener getListener() {
            return this.listener;
        }

        @Override
        public int hashCode() {
            if(DEBUG.M) System.out.println("# hashCode() is not defined for localDataProvider.EventDescriptor");
            return this.listener.hashCode();
        }
    } // EventDescriptor
    class LocalFrameData implements FrameData{
        byte[]   allFrames;
        byte[][] frames;
        // If the frames are stored in non segmented data, all the frames are read at the same time
        // otherwise they are read when needed
        boolean  isSegmented;
        String   nodeName;
        int      pixelSize;
        int      segIdxs[];
        int      startIdx, endIdx;
        float[]  times;
        int      width, height;

        void configure(final String nodeName, String timeName, final float timeMin, final float timeMax) throws IOException {
            if(DEBUG.M) System.out.println("localDataProvider.LocalFrameData.configure(\"" + this.nodeName + "\", \"" + timeName + "\", " + timeMin + ", " + timeMax + ")");
            this.nodeName = nodeName;
            this.isSegmented = localDataProvider.NativeIsSegmentedNode(this.nodeName);
            if(this.isSegmented){
                this.times = localDataProvider.NativeGetSegmentTimes(this.nodeName, timeName, timeMin, timeMax);
                if(this.times == null) throw new IOException(localDataProvider.this.ErrorString());
                this.frames = new byte[this.times.length][];
                this.segIdxs = localDataProvider.NativeGetSegmentIdxs(this.nodeName, timeMin, timeMax);
                if(this.segIdxs == null) throw new IOException(localDataProvider.this.ErrorString());
            }else{
                try{
                    localDataProvider.this.GetString("_jscope_frames = ( " + this.nodeName + " );\"\""); // Caching
                    this.nodeName = "_jscope_frames";
                }catch(final Exception exc){
                    System.out.println("# error " + exc);
                }
            }
            final localDataProviderInfo info = localDataProvider.NativeGetInfo(this.nodeName, this.isSegmented);
            if(DEBUG.M) System.out.println("localDataProvider.getAllTimes.NativeGetInfo() info=" + info);
            if(info == null) throw new IOException(localDataProvider.this.ErrorString());
            this.width = info.dims[0];
            this.height = info.dims[1];
            this.pixelSize = info.pixelSize;
            if(DEBUG.D) System.out.println(">> pixelSize = " + this.pixelSize);
            if(!this.isSegmented){
                if(timeName == null || timeName.trim().equals("")) timeName = "DIM_OF(" + this.nodeName + ")";
                if(DEBUG.D) System.out.println(">> timeName = " + timeName);
                final int[] segs = localDataProvider.NativeGetSegmentIdxs(this.nodeName, timeMin, timeMax);
                this.startIdx = segs[0];
                this.endIdx = segs[-1];
                this.times = localDataProvider.NativeGetSegmentTimes(this.nodeName, timeName, timeMin, timeMax);
                this.allFrames = localDataProvider.NativeGetAllFrames(this.nodeName, this.startIdx, this.endIdx);
                if(DEBUG.A) DEBUG.printByteArray(this.allFrames, this.pixelSize, this.width, this.height, this.times.length);
            }
        }

        @Override
        public byte[] GetFrameAt(final int idx) throws IOException {
            if(DEBUG.M) System.out.println("localDataProvider.LocalFrameData.GetFrameAt(" + idx + ")");
            if(this.isSegmented){
                if(this.frames[idx] == null){
                    final int segIdx = this.segIdxs[idx];
                    int segOffset = 0;
                    for(int i = idx - 1; i >= 0 && this.segIdxs[i] == segIdx; i--, segOffset++);
                    this.frames[idx] = localDataProvider.NativeGetSegment(this.nodeName, segIdx, segOffset);
                }
                return this.frames[idx];
            }
            final int img_size = this.pixelSize * this.width * this.height;
            final byte[] outFrame = new byte[img_size];
            System.arraycopy(this.allFrames, idx * img_size, outFrame, 0, img_size);
            return outFrame;
        }

        @Override
        public Dimension GetFrameDimension() throws IOException {
            return new Dimension(this.width, this.height);
        }

        @Override
        public float[] GetFrameTimes() throws IOException {
            return this.times;
        }

        @Override
        public int GetFrameType() throws IOException {
            if(DEBUG.M) System.out.println("localDataProvider.LocalFrameData.GetFrameType()");
            switch(this.pixelSize){
                case 1:
                    return FrameData.BITMAP_IMAGE_8;
                case 2:
                    return FrameData.BITMAP_IMAGE_16;
                case 4:
                    return FrameData.BITMAP_IMAGE_32;
                default:// 8
                    return FrameData.BITMAP_IMAGE_FLOAT;
            }
        }

        @Override
        public int GetNumFrames() throws IOException {
            if(DEBUG.M) System.out.println("localDataProvider.LocalFrameData.GetNumFrames()");
            if(DEBUG.D) System.out.println(">> NumFrames = " + this.times.length);
            return this.times.length;
        }
    } // LocalFrameData
    class SimpleWaveData implements WaveData{
        public static final int                SEGMENTED_NO       = 2;
        public static final int                SEGMENTED_UNKNOWN  = 3;
        public static final int                SEGMENTED_YES      = 1;
        public static final int                UNKNOWN            = -1;
        private final AsynchDataSource         asynchSource       = null;
        private final tdicache                 c;
        DataProvider                           dp;
        private boolean                        isXLong            = false;
        private int                            numDimensions      = SimpleWaveData.UNKNOWN;
        private int                            segmentMode        = SimpleWaveData.SEGMENTED_UNKNOWN;
        private String                         title              = null;
        private boolean                        titleEvaluated     = false;
        private final Vector<WaveDataListener> waveDataListenersV = new Vector<WaveDataListener>();
        private long                           x2DLong[];
        private String                         xLabel             = null;
        private boolean                        xLabelEvaluated    = false;
        private String                         yLabel             = null;
        private boolean                        yLabelEvaluated    = false;

        public SimpleWaveData(final String _in_y, final String experiment, final long shot){
            this(_in_y, null, experiment, shot);
        }

        public SimpleWaveData(String in_y, final String in_x, final String experiment, final long shot){
            if(DEBUG.M) System.out.println("SimpleWaveData(\"" + in_y + "\", \"" + in_x + "\", \"" + experiment + "\", " + shot + ")");
            in_y = this.checkForAsynchRequest(in_y);
            this.c = new tdicache(in_y, in_x, localDataProvider.var_idx++);
            this.SegmentMode();
        }

        @Override
        public void addWaveDataListener(final WaveDataListener listener) {
            if(DEBUG.M) System.out.println("SimpleWaveData.addWaveDataListener()");
            this.waveDataListenersV.addElement(listener);
            if(this.asynchSource != null) this.asynchSource.addDataListener(listener);
        }

        // Check if the passed Y expression specifies also an asynchronous part (separated by the pattern &&&)
        // in case get an implementation of AsynchDataSource
        String checkForAsynchRequest(final String expression) {
            if(DEBUG.M) System.out.println("SimpleWaveData.checkForAsynchRequest(\"" + expression + "\")");
            return expression.startsWith("ASYNCH::") ? expression.substring(8) : expression;
        }

        // GAB JULY 2014 NEW WAVEDATA INTERFACE RAFFAZZONATA
        @Override
        public final XYData getData(final double xmin, final double xmax, final int numPoints) throws Exception {
            return this.getData(xmin, xmax, numPoints, false);
        }

        public final XYData getData(final double xmin, final double xmax, final int numPoints, final boolean isLong) throws Exception {
            if(DEBUG.M) System.out.println("SimpleWaveData.XYData(" + xmin + ", " + xmax + ", " + numPoints + ", " + isLong + ")");
            if(this.segmentMode == SimpleWaveData.SEGMENTED_UNKNOWN){
                try{
                    final byte[] retData = localDataProvider.NativeGetByteArray("byte(mdsMisc->IsSegmented(" + this.c.yo() + "))");
                    if(retData[0] > 0) this.segmentMode = SimpleWaveData.SEGMENTED_YES;
                    else this.segmentMode = SimpleWaveData.SEGMENTED_NO;
                }catch(final Exception exc){// mdsMisc->IsSegmented failed
                    this.segmentMode = SimpleWaveData.SEGMENTED_NO;
                }
            }
            final String setTimeContext = "";
            final float y[] = localDataProvider.GetFloatArray(setTimeContext + this.c.y());
            if(DEBUG.D) System.out.println(">> y = " + y);
            if(DEBUG.A) DEBUG.printFloatArray(y, y.length, 1, 1);
            final RealArray xReal = localDataProvider.GetRealArray(this.c.x());
            if(xReal.isLong){
                this.isXLong = true;
                return new XYData(xReal.getLongArray(), y, 1E12);
            }
            this.isXLong = false;
            final double x[] = xReal.getDoubleArray();
            if(DEBUG.D) System.out.println(">> x = " + x);
            if(DEBUG.A) DEBUG.printDoubleArray(x, x.length, 1, 1);
            return new XYData(x, y, 1E12);
        }

        @Override
        public final XYData getData(final int numPoints) throws Exception {
            if(DEBUG.M) System.out.println("SimpleWaveData.getData(" + numPoints + ")");
            return this.getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, numPoints);
        }

        @Override
        public final void getDataAsync(final double lowerBound, final double upperBound, final int numPoints) {}

        @Override
        public final int getNumDimension() throws IOException {
            if(DEBUG.M) System.out.println("SimpleWaveData.getNumDimension()");
            if(this.numDimensions != SimpleWaveData.UNKNOWN) return this.numDimensions;
            String expr;
            if(this.segmentMode == SimpleWaveData.SEGMENTED_YES) expr = "GetSegment(" + this.c.yo() + ",0)";
            else expr = this.c.y();
            final int shape[] = localDataProvider.GetNumDimensions(expr);
            if(DEBUG.D){
                String msg = ">> shape =";
                for(final int element : shape)
                    msg += " " + element;
                System.out.println(msg);
            }
            this.numDimensions = shape.length;
            return shape.length;
        }

        @Override
        public final String GetTitle() throws IOException {
            if(DEBUG.M) System.out.println("SimpleWaveData.GetTitle()");
            if(!this.titleEvaluated){
                this.titleEvaluated = true;
                this.title = localDataProvider.this.GetString("help_of(" + this.c.y() + ")");
            }
            return this.title;
        }

        public final float[] getX_X2D() {
            if(DEBUG.M) System.out.println("SimpleWaveData.getX_X2D()");
            try{
                return localDataProvider.GetFloatArray("DIM_OF(" + this.c.x() + ", 0)");
            }catch(final Exception exc){
                return null;
            }
        }

        public final float[] getX_Y2D() {
            if(DEBUG.M) System.out.println("SimpleWaveData.getX_Y2D()");
            try{
                return localDataProvider.GetFloatArray("DIM_OF(" + this.c.x() + ", 1)");
            }catch(final Exception exc){
                return null;
            }
        }

        public final float[] getX_Z() {
            if(DEBUG.M) System.out.println("SimpleWaveData.getX_Z()");
            try{
                return localDataProvider.GetFloatArray("(" + this.c.x() + ")");
            }catch(final Exception exc){
                return null;
            }
        }

        @Override
        public final double[] getX2D() {
            if(DEBUG.M) System.out.println("SimpleWaveData.getX2D()");
            try{
                final RealArray realArray = localDataProvider.GetRealArray("DIM_OF(" + this.c.y() + ", 0)");
                if(realArray.isLong){
                    this.isXLong = true;
                    this.x2DLong = realArray.getLongArray();
                    return null;
                }
                this.x2DLong = null;
                return realArray.getDoubleArray();
                // return GetFloatArray(in);
            }catch(final Exception exc){
                return null;
            }
        }

        @Override
        public final long[] getX2DLong() {
            if(DEBUG.M) System.out.println("SimpleWaveData.getX2DLong()");
            return this.x2DLong;
        }

        @Override
        public final String GetXLabel() throws IOException {
            if(DEBUG.M){
                System.out.println("SimpleWaveData.GetXLabel()");
            }
            if(!this.xLabelEvaluated){
                this.xLabelEvaluated = true;
                this.xLabel = localDataProvider.this.GetString("Units(" + this.c.x() + ")");
            }
            return this.xLabel;
        }

        @Override
        public final float[] getY2D() {
            if(DEBUG.M) System.out.println("SimpleWaveData.getY2D()");
            try{
                return localDataProvider.NativeGetFloatArray("DIM_OF(" + this.c.y() + ", 1)");
            }catch(final Exception exc){
                return null;
            }
        }

        @Override
        public final String GetYLabel() throws IOException {
            if(DEBUG.M) System.out.println("SimpleWaveData.GetYLabel()");
            if(!this.yLabelEvaluated){
                this.yLabelEvaluated = true;
                if(this.getNumDimension() > 1){
                    if(this.segmentMode == SimpleWaveData.SEGMENTED_YES) this.yLabel = localDataProvider.this.GetString("Units(Dim_of(GetSegment(" + this.c.yo() + ",0),1))");
                    else this.yLabel = localDataProvider.this.GetString("Units(Dim_of(" + this.c.y() + ",1))");
                }else{
                    if(this.segmentMode == SimpleWaveData.SEGMENTED_YES) this.yLabel = localDataProvider.this.GetString("Units(GetSegment(" + this.c.yo() + ",0))");
                    else this.yLabel = localDataProvider.this.GetString("Units(" + this.c.y() + ")");
                }
            }
            return this.yLabel;
        }

        @Override
        public float[] getZ() {
            if(DEBUG.M) System.out.println("SimpleWaveData.getZ()");
            try{
                return localDataProvider.GetFloatArray(this.c.y());
            }catch(final Exception exc){
                return null;
            }
        }

        @Override
        public String GetZLabel() throws IOException {
            if(DEBUG.M) System.out.println("SimpleWaveData.GetZLabel()");
            return localDataProvider.this.GetString("Units(" + this.c.y() + ")");
        }

        @Override
        public final boolean isXLong() {
            return this.isXLong;
        }

        private final void SegmentMode() {
            if(DEBUG.M) System.out.println("SimpleWaveData.SegmentMode()");
            if(this.segmentMode == SimpleWaveData.SEGMENTED_UNKNOWN){
                final String expr = "[GetNumSegments(" + this.c.yo() + ")]";
                try{// fast using in_y as NumSegments is a node property
                    final int[] numSegments = localDataProvider.NativeGetIntArray(expr);
                    if(numSegments == null) this.segmentMode = SimpleWaveData.SEGMENTED_UNKNOWN;
                    else if(numSegments[0] > 0) this.segmentMode = SimpleWaveData.SEGMENTED_YES;
                    else this.segmentMode = SimpleWaveData.SEGMENTED_NO;
                }catch(final Exception exc){// happens if expression is not a plain node path
                    if(DEBUG.M) System.err.println("# SimpleWaveData.SegmentMode, \"" + expr + "\": " + exc);
                    this.segmentMode = SimpleWaveData.SEGMENTED_UNKNOWN;
                }
            }
        }

        @Override
        public final void setContinuousUpdate(final boolean continuousUpdate) {}
    } // END Inner Class SimpleWaveData
    private final class tdicache{
        private final String  in_x;
        private final String  in_y;
        private boolean       useCache = true;
        private final String  var;
        private String        xc;
        private final boolean xdim;
        private String        yc;
        private byte          ykind;

        public tdicache(final String _in_y, final String _in_x, final int _var){
            this(_in_y, _in_x, Integer.toString(_var));
        }

        public tdicache(final String _in_y, final String _in_x, final String _var){
            this.in_y = _in_y;
            this.var = "_jscope_" + _var;
            this.xdim = _in_x == null;
            if(this.xdim) this.in_x = "DIM_OF(" + this.in_y + ")";
            else this.in_x = _in_x;
            if(this.in_y != "[]"){
                this.y();
                this.useCache = this.useCache & (this.ykind == -62); // only use on "EXT_FUNCTIONS" such as the archive interface
                if(!this.useCache){
                    this.yc = this.in_y;
                    this.xc = this.in_x;
                }else System.out.println("Caching " + _in_y);
                return;
            }
            this.ykind = -1;
            this.useCache = false;
        }

        @Override
        protected final void finalize() {
            localDataProvider.this.mdsValue("DEALLOCATE(['" + this.xc + "','" + this.yc + "'])");
        }

        public final String x() {
            if(this.xc == null){
                if(this.xdim) this.y();
                else{
                    this.xc = this.var + "x";
                    final String expr = this.xc + " = (" + this.in_x + "); KIND( " + this.xc + " )";
                    try{
                        localDataProvider.GetRealArray(expr);
                        if(DEBUG.D) System.out.println(">> tdicache x (" + expr + ")");
                    }catch(final Exception exc){
                        System.err.println("# tdicache error: could not cache x (" + expr + ")");
                        this.xc = this.in_x;
                    }
                }
            }
            return this.xc;
        }

        @SuppressWarnings("unused")
        public final String xo() {
            return this.in_x;
        }

        public final String y() {
            if(!this.useCache) return this.in_y;
            if(this.yc == null){
                this.yc = this.var + "y";
                final String expr = this.yc + "=EVALUATE(" + this.in_y + ");[KIND(" + this.yc + ")]";
                try{
                    this.ykind = (byte)localDataProvider.NativeGetIntArray(expr)[0];
                    if(DEBUG.D) System.out.println(">> tdicache y ( " + expr + " -> " + this.ykind + " ) )");
                }catch(final Exception exc){
                    System.err.println("# tdicache error: could not cache y ( " + expr + " -> " + this.ykind + " ) ): " + exc);
                    this.yc = this.in_y;
                }
                if(this.xdim) this.xc = "DIM_OF(" + this.yc + ")";
            }
            return this.yc;
        }

        public final String yo() {
            return this.in_y;
        }
    }
    static final int MAX_PIXELS        = 2000;
    static final int RESAMPLE_TRESHOLD = 1000000000;
    static int       var_idx           = 0;

    static{
        try{
            System.loadLibrary("localDataProvider");
        }catch(final UnsatisfiedLinkError e){
            javax.swing.JOptionPane.showMessageDialog(null, "Can't load data provider class localDataProvider : " + e, "Alert localDataProvider", javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(System.err);
        }
    }

    public static boolean DataPending() {
        return false;
    }

    public synchronized static final float[] GetFloatArray(final String in) throws IOException {
        return localDataProvider.NativeGetFloatArray(in);
    }

    public static final int[] GetNumDimensions(final String expression) {
        final int[] fullDims = localDataProvider.NativeGetIntArray("SHAPE( " + expression + " )");
        if(fullDims == null) return null;
        if(fullDims.length == 1) return fullDims;
        // count dimensions == 1
        int numDimensions = 0;
        for(final int fullDim : fullDims){
            if(fullDim != 1) numDimensions++;
        }
        final int[] retDims = new int[numDimensions];
        int j = 0;
        for(final int fullDim : fullDims){
            if(fullDim != 1) retDims[j++] = fullDim;
        }
        return retDims;
    }

    public synchronized static final RealArray GetRealArray(final String in) throws IOException {
        final long longArray[] = localDataProvider.NativeGetLongArray(in);
        if(longArray != null) return new RealArray(longArray);
        return new RealArray(localDataProvider.NativeGetDoubleArray(in));
    }

    public static final void main(final String[] args) {
        final localDataProvider dp = new localDataProvider();
        try{
            /* user code */
            System.out.println(dp.GetString("TCL('DIR',_out);_out"));
        }catch(final Exception e){
            System.err.println(e);
        }
    }

    private static native String NativeErrorString();

    private static native byte[] NativeGetAllFrames(String nodeName, int startIdx, int endIdx);

    private static native float[] NativeGetAllTimes(String nodeName, String timeNames);

    private static native byte[] NativeGetByteArray(String in);

    private static native double[] NativeGetDoubleArray(String in);

    public static native double NativeGetFloat(String in);

    public static native float[] NativeGetFloatArray(String in);

    private static native localDataProviderInfo NativeGetInfo(String nodeName, boolean isSegmented); // returned: width, height, bytesPerPixel

    public static native int[] NativeGetIntArray(String in);

    public static native long[] NativeGetLongArray(String in);

    private static native byte[] NativeGetSegment(String nodeName, int segIdx, int segOffset);

    private static native int[] NativeGetSegmentIdxs(String nodeName, float timeMin, float timeMax);

    private static native float[] NativeGetSegmentTimes(String nodeName, String timeNames, float timeMin, float timeMax);

    public static native String NativeGetString(String in);

    private static native boolean NativeIsSegmentedNode(String nodeName);

    private static native int NativeRegisterEvent(String event, int idx);

    private static native void NativeSetEnvironmentSpecific(String name, String value);

    private static native void NativeUnregisterEvent(int evId);

    private static native void NativeUpdate(String exp, long s);

    static void setResampleLimits(final double min, final double max) {
        if(DEBUG.M) System.out.println("localDataProvider.setResampleLimits(" + min + ", " + max + ")");
        String limitsExpr;
        if(Math.abs(min) > localDataProvider.RESAMPLE_TRESHOLD || Math.abs(max) > localDataProvider.RESAMPLE_TRESHOLD){
            final long maxSpecific = jScopeFacade.convertToSpecificTime((long)max);
            final long minSpecific = jScopeFacade.convertToSpecificTime((long)min);
            final long dt = (maxSpecific - minSpecific) / localDataProvider.MAX_PIXELS;
            limitsExpr = "JavaSetResampleLimits(" + minSpecific + "UQ," + maxSpecific + "UQ," + dt + "UQ)";
        }else{
            final double dt = (max - min) / localDataProvider.MAX_PIXELS;
            limitsExpr = "JavaSetResampleLimits(" + min + "," + max + "," + dt + ")";
        }
        localDataProvider.NativeGetFloat(limitsExpr);
    }

    public static boolean SupportsCompression() {
        return false;
    }

    public static boolean SupportsContinuous() {
        return false;
    }

    public static boolean SupportsFastNetwork() {
        return true;
    }

    static boolean supportsLargeSignals() {
        return false;
    } // Subclass localDataProvider will return false
    String                  error      = null;
    Vector<String>          eventNames = new Vector<String>();
    String                  experiment;
    Vector<EventDescriptor> listeners  = new Vector<EventDescriptor>();
    long                    shot;
    public Object           updateWorker;

    @Override
    public void AddConnectionListener(final ConnectionListener l) {}

    @Override
    public void AddUpdateEventListener(final UpdateEventListener l, final String event) {
        if(DEBUG.M) System.out.println("localDataProvider.AddUpdateEventListener(" + l + ", \"" + event + "\")");
        int evId;
        int idx;
        try{
            evId = localDataProvider.this.getEventId(event);
            idx = this.eventNames.indexOf(event);
        }catch(final Exception exc){
            idx = this.eventNames.size();
            this.eventNames.addElement(event);
            evId = localDataProvider.NativeRegisterEvent(event, idx);
        }
        this.listeners.addElement(new EventDescriptor(l, event, evId));
    }

    @Override
    public void Dispose() {}

    @Override
    public String ErrorString() {
        return localDataProvider.NativeErrorString();
    }

    public void fireEvent(final int nameIdx) {
        if(DEBUG.M) System.out.println("localDataProvider.fireEvent(" + nameIdx + ")");
        final String event = this.eventNames.elementAt(nameIdx);
        for(int idx = 0; idx < this.listeners.size(); idx++){
            final EventDescriptor evDescr = this.listeners.elementAt(idx);
            if(evDescr.getEvent().equals(event)) evDescr.getListener().processUpdateEvent(new UpdateEvent(this, event));
        }
    }

    int getEventId(final String event) throws Exception {
        if(DEBUG.M) System.out.println("localDataProvider.getEventId(\"" + event + "\")");
        for(int idx = 0; idx < this.listeners.size(); idx++){
            final EventDescriptor evDescr = this.listeners.elementAt(idx);
            if(event.equals(evDescr.getEvent())) return evDescr.getEvId();
        }
        throw(new Exception());
    }

    @Override
    public synchronized float GetFloat(final String in) throws IOException {
        if(DEBUG.M) System.out.println("localDataProvider.GetFloat(\"" + in + "\")");
        return (float)localDataProvider.NativeGetFloat(in);
    }

    @Override
    public FrameData GetFrameData(final String in_y, final String in_x, final float time_min, final float time_max) throws IOException {
        if(DEBUG.M) System.out.println("localDataProvider.GetFrameData(\"" + in_y + "\", \"" + in_x + "\", " + time_min + ", " + time_max + ")");
        final LocalFrameData frameData = new LocalFrameData();
        frameData.configure(in_y, in_x, time_min, time_max);
        return frameData;
    }

    @Override
    public final String GetLegendString(final String s) {
        return s;
    }

    @Override
    public long[] GetShots(final String in) {
        if(DEBUG.M) System.out.println("localDataProvider.GetShots(\"" + in + "\")");
        try{
            final int shots[] = localDataProvider.NativeGetIntArray(in.trim());
            final long lshots[] = new long[shots.length];
            for(int i = 0; i < shots.length; i++)
                lshots[i] = shots[i];
            return lshots;
        }catch(final UnsatisfiedLinkError e){
            System.err.println("# Error in GetIntArray: " + e);
            return null;
        }catch(final Exception exc){
            System.err.println("# Error in GetIntArray: " + exc);
            return null;
        }
    }

    @Override
    public String GetString(final String in) throws IOException {
        return localDataProvider.NativeGetString(in);
    }

    @Override
    public WaveData GetWaveData(final String in) {
        return new SimpleWaveData(in, this.experiment, this.shot);
    }

    @Override
    public WaveData GetWaveData(final String in_y, final String in_x) {
        return new SimpleWaveData(in_y, in_x, this.experiment, this.shot);
    }

    @Override
    public int InquireCredentials(final JFrame f, final DataServerItem server_item) {
        return DataProvider.LOGIN_OK;
    }

    public void mdsValue(final String exp) {
        try{
            this.GetFloat(exp + ";1.");
        }catch(final Exception e){}
    }

    @Override
    public void RemoveConnectionListener(final ConnectionListener l) {}

    @Override
    public void RemoveUpdateEventListener(final UpdateEventListener l, final String event) {
        if(DEBUG.M) System.out.println("localDataProvider.RemoveUpdateEventListener(" + l + ", \"" + event + "\")");
        final int idx = this.listeners.indexOf(new EventDescriptor(l, event, 0));
        if(idx != -1){
            final int evId = this.listeners.elementAt(idx).getEvId();
            this.listeners.removeElementAt(idx);
            try{
                this.getEventId(event);
            }catch(final Exception exc){
                localDataProvider.NativeUnregisterEvent(evId);
            }
        }
    }

    @Override
    public void SetArgument(final String arg) {}

    @Override
    public void SetEnvironment(final String exp) throws IOException {
        this.mdsValue(exp);
    }

    native public void SetEnvironmentSpecific(String in, String defaultNode);

    @Override
    public boolean SupportsTunneling() {
        return false;
    }

    @Override
    public void Update(final String exp, final long s) {
        if(DEBUG.M) System.out.println("localDataProvider.Update(\"" + exp + "\", " + s + ")");
        localDataProvider.var_idx = 0;
        localDataProvider.NativeUpdate(exp, s);
    }
}
