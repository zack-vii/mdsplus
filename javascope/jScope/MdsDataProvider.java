package jScope;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class MdsDataProvider implements DataProvider{
    protected String  provider;
    protected String  experiment;
    String            default_node;
    String            environment_vars;
    private boolean   def_node_changed  = false;
    protected long    shot;
    boolean           open, connected;
    MdsConnection     mds;
    String            error;
    private boolean   use_compression   = false;
    static int        var_idx           = 0;
    boolean           is_tunneling      = false;
    String            tunnel_provider   = "127.0.0.1:8000";
    SshTunneling      ssh_tunneling;
    static final long RESAMPLE_TRESHOLD = 1000000000;
    static final int  MAX_PIXELS        = 20000;
    public class ByteArray{
        byte buf[];
        byte dtype;

        public ByteArray(byte[] buf, byte dtype){
            this.buf = buf;
            this.dtype = dtype;
        }

        public int getDataSize() {
            return Descriptor.getDataSize(dtype, buf);
        }

        public int getFrameType() {
            if(DEBUG.D) System.out.println(">> getFrameType = " + this.dtype);
            switch(this.dtype){
                case Descriptor.DTYPE_UBYTE:
                case Descriptor.DTYPE_BYTE:
                    return FrameData.BITMAP_IMAGE_8;
                case Descriptor.DTYPE_USHORT:
                case Descriptor.DTYPE_SHORT:
                    return FrameData.BITMAP_IMAGE_16;
                case Descriptor.DTYPE_ULONG:
                case Descriptor.DTYPE_LONG:
                    return FrameData.BITMAP_IMAGE_32;
                case Descriptor.DTYPE_ULONGLONG:
                case Descriptor.DTYPE_LONGLONG:
                case Descriptor.DTYPE_FLOAT:
                case Descriptor.DTYPE_DOUBLE:
                    return FrameData.BITMAP_IMAGE_FLOAT;
                default:
                    return FrameData.BITMAP_IMAGE_8;
            }
        }
    }
    public class AllFrames extends ByteArray{
        Dimension dim;
        double    times[];

        public AllFrames(ByteArray byteArray, int width, int height, double times[]){
            super(byteArray.buf, byteArray.dtype);
            this.dim = new Dimension(width, height);
            this.times = times;
        }
    }

    public MdsDataProvider(){
        if(DEBUG.M) System.out.println("MdsDataProvider()");
        experiment = null;
        shot = 0;
        open = connected = false;
        mds = getConnection();
        error = null;
        // updateWorker = new UpdateWorker();
        // updateWorker.start();
    }

    public MdsDataProvider(String provider){
        if(DEBUG.M) System.out.println("MdsDataProvider(\"" + provider + "\")");
        setProvider(provider);
        experiment = null;
        shot = 0;
        open = connected = false;
        mds = new MdsConnection(this.provider);
        error = null;
        // updateWorker = new UpdateWorker();
        // updateWorker.start();
    }

    public MdsDataProvider(String exp, int s){
        if(DEBUG.M) System.out.println("MdsDataProvider(\"" + exp + "\", " + s + ")");
        experiment = exp;
        shot = 0; // what's about s
        open = connected = false;
        mds = new MdsConnection();
        error = null;
        // updateWorker = new UpdateWorker();
        // updateWorker.start();
    }
    class SegmentedFrameData implements FrameData{
        String    in_x, in_y;
        double    time_max, time_min;
        int       framesPerSegment;
        int       numSegments;
        int       startSegment, endSegment, actSegments;
        int       mode;
        Dimension dim;
        double    times[];
        int       bytesPerPixel;

        @Override
        public int GetFrameType() throws IOException {
            return mode;
        }

        @Override
        public int GetNumFrames() {
            return actSegments * framesPerSegment;
        }

        @Override
        public Dimension GetFrameDimension() {
            return dim;
        }

        @Override
        public double[] GetFrameTimes() {
            return times;
        }

        public SegmentedFrameData(String in_y, String in_x, double time_min, double time_max, int numSegments) throws IOException{
            if(DEBUG.M) System.out.println("MdsDataProvider.SegmentedFrameData(\"" + in_y + "\", \"" + in_x + "\", " + time_min + ", " + time_max + ", " + numSegments + ")");
            // Find out frames per segment and frame min and max based on time min and time max
            this.in_x = in_x;
            this.in_y = in_y;
            this.time_min = time_min;
            this.time_max = time_max;
            this.numSegments = numSegments;
            startSegment = -1;
            double startTimes[] = new double[numSegments];
            // Get segment window corresponding to the passed time window
            for(int i = 0; i < numSegments; i++){
                double limits[] = GetDoubleArray("GetSegmentLimits(" + in_y + "," + i + ")");
                startTimes[i] = limits[0];
                if(limits[1] > time_min){
                    startSegment = i;
                    break;
                }
            }
            if(startSegment == -1) throw new IOException("Frames outside defined time window");
            // Check first if endTime is greated than the end of the last segment, to avoid rolling over all segments
            double endLimits[] = GetDoubleArray("GetSegmentLimits(" + in_y + "," + (numSegments - 1) + ")");
            // Throw away spurious frames at the end
            while(endLimits == null || endLimits.length != 2){
                numSegments--;
                if(numSegments == 0) break;
                endLimits = GetDoubleArray("GetSegmentLimits(" + in_y + "," + (numSegments - 1) + ")");
            }
            if(numSegments > 100 && endLimits[0] < time_max){
                endSegment = numSegments - 1;
                for(int i = startSegment; i < numSegments; i++)
                    startTimes[i] = startTimes[0] + i * (endLimits[0] - startTimes[0]) / numSegments;
            }else{
                for(endSegment = startSegment; endSegment < numSegments; endSegment++){
                    try{
                        double limits[] = GetDoubleArray("GetSegmentLimits(" + in_y + "," + endSegment + ")");
                        startTimes[endSegment] = limits[0];
                        if(limits[0] > time_max) break;
                    }catch(Exception exc){
                        break;
                    }
                }
            }
            actSegments = endSegment - startSegment;
            // Get Frame Dimension and frames per segment
            mds.MdsValue("_jscope_seg=*");
            int dims[] = GetIntArray("shape(_jscope_seg=GetSegment(" + in_y + ", 0))");
            if(dims.length != 3){
                mds.MdsValue("DEALLOCATE('_jscope_seg')");
                throw new IOException("Invalid number of segment dimensions: " + dims.length);
            }
            dim = new Dimension(dims[0], dims[1]);
            framesPerSegment = dims[2];
            // Get Frame element length in bytes
            ByteArray data = getByteArray("_jscope_seg[0,0,0]");
            mds.MdsValue("DEALLOCATE('_jscope_seg')");
            if(DEBUG.M) System.out.println(">> data = " + data);
            bytesPerPixel = data.getDataSize();
            mode = data.getFrameType();
            // Get Frame times
            if(framesPerSegment == 1) // We assume in this case that start time is the same of the frame time
            {
                times = new double[actSegments];
                for(int i = 0; i < actSegments; i++)
                    times[i] = startTimes[startSegment + i];
            }else // Get segment times. We assume that the same number of frames is contained in every segment
            {
                times = new double[actSegments * framesPerSegment];
                for(int i = 0; i < actSegments; i++){
                    double segTimes[] = GetDoubleArray("D_Float(Dim_Of(GetSegment(" + in_y + "," + i + ")))");
                    if(segTimes.length != framesPerSegment) throw new IOException("Inconsistent definition of time in frame + " + i + ": read " + segTimes.length + " times, expected " + framesPerSegment);
                    for(int j = 0; j < framesPerSegment; j++)
                        times[i * framesPerSegment + j] = segTimes[j];
                }
            }
        }

        @Override
        public byte[] GetFrameAt(int idx) throws IOException {
            if(DEBUG.M) System.out.println("MdsDataProvider.SegmentedFrameData.GetFrameAt(" + idx + ")");
            int segmentIdx = startSegment + idx / framesPerSegment;
            int segmentOffset = (idx % framesPerSegment) * dim.width * dim.height * bytesPerPixel;
            byte[] segment = GetByteArray("GetSegment(" + in_y + "," + segmentIdx + ")");
            if(framesPerSegment == 1) return segment;
            byte[] outFrame = new byte[dim.width * dim.height * bytesPerPixel];
            System.arraycopy(segment, segmentOffset, outFrame, 0, dim.width * dim.height * bytesPerPixel);
            return outFrame;
        }
    }
    class SimpleFrameData implements FrameData{
        String            in_x, in_y;
        double            time_max, time_min;
        int               mode            = -1;
        int               pixel_size;
        int               first_frame_idx = -1;
        byte              buf[];
        String            error;
        private int       st_idx          = -1, end_idx = -1;
        private int       n_frames        = 0;
        private double    times[]         = null;
        private Dimension dim             = null;

        @Override
        public int GetNumFrames() {
            return n_frames;
        }

        @Override
        public Dimension GetFrameDimension() {
            return dim;
        }

        @Override
        public double[] GetFrameTimes() {
            return times;
        }

        public SimpleFrameData(String in_y, String in_x, double time_min, double time_max) throws Exception{
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleFrameData(\"" + in_y + "\", \"" + in_x + "\", " + time_min + ", " + time_max + ")");
            int i;
            double t;
            double all_times[] = null;
            this.in_y = in_y;
            this.in_x = in_x;
            this.time_min = time_min;
            this.time_max = time_max;
            AllFrames allFrames = GetAllFrames(in_y);
            if(allFrames != null){
                this.buf = allFrames.buf;
                mode = allFrames.getFrameType();
                pixel_size = allFrames.getDataSize() * 8;
                dim = allFrames.dim;
                if(allFrames.times != null) all_times = allFrames.times;
                else{
                    if(DEBUG.D) System.out.println(">> GetWaveData(in_x), " + in_x);
                    all_times = MdsDataProvider.this.GetDoubleArray(in_x);
                    // all_times = MdsDataProvider.this.GetWaveData(in_x).getData(MAX_PIXELS).y;
                }
            }else{
                String mframe_error = ErrorString();
                if(in_x == null || in_x.length() == 0) all_times = MdsDataProvider.this.GetFrameTimes(in_y);
                else all_times = MdsDataProvider.this.GetDoubleArray(in_x);
                // all_times = MdsDataProvider.this.GetWaveData(in_x).getData(MAX_PIXELS).y;
                if(all_times == null){
                    if(mframe_error != null) error = " Pulse file or image file not found\nRead pulse file error\n" + mframe_error + "\nFrame times read error";
                    else error = " Image file not found ";
                    if(ErrorString() != null) error = error + "\n" + ErrorString();
                    throw(new IOException(error));
                }
            }
            for(i = 0; i < all_times.length; i++){
                t = all_times[i];
                if(t > time_max) break;
                if(t >= time_min){
                    if(st_idx == -1) st_idx = i;
                }
            }
            end_idx = i;
            if(st_idx == -1) throw(new IOException("No frames found between " + time_min + " - " + time_max));
            n_frames = end_idx - st_idx;
            times = new double[n_frames];
            int j = 0;
            for(i = st_idx; i < end_idx; i++)
                times[j++] = all_times[i];
        }

        @Override
        public int GetFrameType() throws IOException {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleFrameData.GetFrameType()");
            if(mode != -1) return mode;
            int i;
            for(i = 0; i < n_frames; i++){
                buf = GetFrameAt(i);
                if(buf != null) break;
            }
            first_frame_idx = i;
            mode = Frames.DecodeImageType(buf);
            return mode;
        }

        @Override
        public byte[] GetFrameAt(int idx) throws IOException {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleFrameData.GetFrameAt(" + idx + ")");
            byte[] b_img = null;
            if(mode == BITMAP_IMAGE_8 || mode == BITMAP_IMAGE_16 || mode == BITMAP_IMAGE_32 || mode == BITMAP_IMAGE_FLOAT){
                if(buf == null) throw(new IOException("Frames not loaded"));
                ByteArrayInputStream b = new ByteArrayInputStream(buf);
                DataInputStream d = new DataInputStream(b);
                if(buf == null) throw(new IOException("Frames dimension not evaluated"));
                int img_size = dim.width * dim.height * pixel_size / 8;
                if(d.available() < img_size){
                    if(DEBUG.M){
                        System.err.println("# >> insufficient bytes: " + d.available() + "/" + img_size);
                    }
                    return null;
                }
                d.skip(img_size * idx);
                b_img = new byte[img_size];
                d.readFully(b_img);
                return b_img;
            }
            // we = new WaveformEvent(wave, "Loading frame "+idx+"/"+n_frames);
            // wave.dispatchWaveformEvent(we);
            if(idx == first_frame_idx && buf != null) return buf;
            b_img = MdsDataProvider.this.GetFrameAt(in_y, st_idx + idx);
            return b_img;
        }
    } // END Inner Class SimpleFrameData
      // //////////////////////////////////////GAB JULY 2014
    class tdicache{
        String         var;
        boolean        xdim;
        String         in_x;
        String         in_y;
        String         xc;
        String         yc;
        byte           xkind    = 0;
        byte           ykind    = 0;
        public boolean useCache = false;

        public tdicache(String _in_y, String _in_x, int _var){
            prep(_in_y, _in_x, Integer.toString(_var));
        }

        public tdicache(String _in_y, String _in_x, String _var){
            prep(_in_y, _in_x, _var);
        }

        void prep(String _in_y, String _in_x, String _var) {
            in_y = _in_y;
            var = "_jscope_" + _var;
            xdim = _in_x == null;
            if(xdim) in_x = "DIM_OF(" + in_y + ")";
            else in_x = _in_x;
            if(in_y != "[]") cache();
        }

        void cache() {
            y();
            useCache = ykind == -62; // -61 = Sig, -62 parm -> python script
            if(!useCache){
                yc = in_y;
                xc = in_x;
            }
            if(DEBUG.D) System.out.println(">> tdicache yclass  = " + ykind);
        }

        public String xo() {
            return in_x;
        }

        public String yo() {
            return in_y;
        }

        public String y() {
            if(yc == null){
                yc = var + "y";
                String expr = yc + " = EVALUATE(" + in_y + "); KIND( " + yc + " )";
                try{
                    ykind = GetByteArray(expr)[0];
                    if(DEBUG.D) System.out.println(">> tdicache y ( " + expr + " -> " + ykind + " ) )");
                }catch(Exception exc){
                    System.err.println("# tdicache error: could not cache y ( " + expr + " -> " + ykind + " ) ): " + exc);
                    yc = in_y;
                }
                if(xdim) xc = "DIM_OF(" + yc + ")";
            }
            return yc;
        }

        public String x() {
            if(xc == null){
                if(xdim) y();
                else{
                    xc = var + "x";
                    String expr = xc + " = (" + in_x + "); KIND( " + xc + " )";
                    try{
                        xkind = GetByteArray(expr)[0];
                        if(DEBUG.D) System.out.println(">> tdicache x (" + expr + ")");
                    }catch(Exception exc){
                        System.err.println("# tdicache error: could not cache x (" + expr + ")");
                        xc = in_x;
                    }
                }
            }
            return xc;
        }

        @Override
        protected void finalize() {
            mds.MdsValue("DEALLOCATE(['" + xc + "','" + yc + "'])");
        }
    }
    class SimpleWaveData implements WaveData{
        tdicache         c;
        static final int SEGMENTED_YES    = 1, SEGMENTED_NO = 2, SEGMENTED_UNKNOWN = 3;
        static final int UNKNOWN          = -1;
        int              numDimensions    = UNKNOWN;
        int              segmentMode      = SEGMENTED_UNKNOWN;
        boolean          isXLong          = false;
        String           title            = null;
        String           xLabel           = null;
        String           yLabel           = null;
        boolean          titleEvaluated   = false;
        boolean          xLabelEvaluated  = false;
        boolean          yLabelEvaluated  = false;
        boolean          continuousUpdate = false;
        String           wd_experiment;
        long             wd_shot;
        AsynchDataSource asynchSource     = null;

        public SimpleWaveData(String _in_y, String experiment, long shot){
            this(_in_y, null, experiment, shot);
        }

        public SimpleWaveData(String in_y, String in_x, String experiment, long shot){
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData(\"" + in_y + "\", \"" + in_x + "\", \"" + experiment + "\", " + shot + ")");
            }
            wd_experiment = experiment;
            wd_shot = shot;
            if(checkForAsynchRequest(in_y)) c = new tdicache("[]", "[]", var_idx++);
            else c = new tdicache(in_y, in_x, var_idx++);
            SegmentMode();
        }

        private void SegmentMode() {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.SegmentMode()");
            }
            if(segmentMode == SEGMENTED_UNKNOWN){
                try{// fast using in_y as NumSegments is a node property
                    int[] numSegments = GetIntArray("GetNumSegments(" + c.yo() + ")");
                    if(numSegments == null) segmentMode = SEGMENTED_UNKNOWN;
                    else if(numSegments[0] > 0) segmentMode = SEGMENTED_YES;
                    else segmentMode = SEGMENTED_NO;
                }catch(Exception exc){// numSegments==null should not get here anymore
                    if(DEBUG.M){
                        System.err.println("# MdsDataProvider.SimpleWaveData.SegmentMode: " + exc);
                    }
                    error = null;
                    segmentMode = SEGMENTED_UNKNOWN;
                }
            }
        }

        // Check if the passed Y expression specifies also an asynchronous part (separated by the patern &&&)
        // in case get an implemenation of AsynchDataSource
        boolean checkForAsynchRequest(String expression) {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.checkForAsynchRequest(\"" + expression + "\")");
            }
            if(expression.startsWith("ASYNCH::")){
                asynchSource = getAsynchSource();
                if(asynchSource != null){
                    asynchSource.startGeneration(expression.substring("ASYNCH::".length()));
                }
                return true;
            }
            return false;
        }

        @Override
        public void setContinuousUpdate(boolean continuousUpdate) {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.setContinuousUpdate(" + continuousUpdate + ")");
            }
            this.continuousUpdate = continuousUpdate;
        }

        @Override
        public int getNumDimension() throws IOException {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.getNumDimension()");
            }
            if(numDimensions != UNKNOWN) return numDimensions;
            String expr;
            if(segmentMode == SEGMENTED_YES) expr = "GetSegment(" + c.yo() + ",0)";
            else expr = c.y();
            error = null;
            int shape[] = GetNumDimensions(expr);
            if(DEBUG.D){
                String msg = ">> shape =";
                for(int i = 0; i < shape.length; i++)
                    msg += " " + shape[i];
                System.out.println(msg);
            }
            if(error != null || shape == null){
                error = null;
                return 1;
            }
            numDimensions = shape.length;
            return shape.length;
        }

        @Override
        public String GetTitle() throws IOException {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.GetTitle()");
            }
            if(!titleEvaluated){
                titleEvaluated = true;
                title = GetStringValue("help_of(" + c.y() + ")");
            }
            return title;
        }

        @Override
        public String GetXLabel() throws IOException {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.GetXLabel()");
            }
            if(!xLabelEvaluated){
                xLabelEvaluated = true;
                xLabel = GetStringValue("Units(" + c.x() + ")");
            }
            return xLabel;
        }

        @Override
        public String GetYLabel() throws IOException {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.GetYLabel()");
            }
            if(!yLabelEvaluated){
                yLabelEvaluated = true;
                if(getNumDimension() > 1){
                    if(segmentMode == SEGMENTED_YES) yLabel = GetStringValue("Units(Dim_of(GetSegment(" + c.yo() + ",0),1))");
                    else yLabel = GetStringValue("Units(Dim_of(" + c.y() + ",1))");
                }else{
                    if(segmentMode == SEGMENTED_YES) yLabel = GetStringValue("Units(GetSegment(" + c.yo() + ",0))");
                    else yLabel = GetStringValue("Units(" + c.y() + ")");
                }
            }
            return yLabel;
        }

        @Override
        public String GetZLabel() throws IOException {
            if(DEBUG.M){
                System.out.println("MdsDataProvider.SimpleWaveData.GetZLabel()");
            }
            return GetStringValue("Units(" + c.y() + ")");
        }

        // GAB JULY 2014 NEW WAVEDATA INTERFACE RAFFAZZONATA
        @Override
        public XYData getData(double xmin, double xmax, int numPoints) throws Exception {
            return getData(xmin, xmax, numPoints, false);
        }

        public XYData getData(double xmin, double xmax, int numPoints, boolean isLong) throws Exception {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.XYData(" + xmin + ", " + xmax + ", " + numPoints + ", " + isLong + ")");
            if(!CheckOpen(this.wd_experiment, this.wd_shot)) return null;
            if(segmentMode == SEGMENTED_UNKNOWN){
                Vector<Descriptor> args = new Vector<Descriptor>();
                args.addElement(new Descriptor(null, c.yo()));
                try{
                    byte[] retData = GetByteArray("byte(MdsMisc->IsSegmented($))", args);
                    if(retData[0] > 0) segmentMode = SEGMENTED_YES;
                    else segmentMode = SEGMENTED_NO;
                }catch(Exception exc){// MdsMisc->IsSegmented failed
                    segmentMode = SEGMENTED_NO;
                }
            }
            // String setTimeContext = getTimeContext(xmin,xmax,isLong);
            String setTimeContext = "";
            if(!c.useCache) try{
                return getXYSignal(xmin, xmax, numPoints, isLong, setTimeContext);
            }catch(Exception exc){// causes the next mdsvalue to fail raising: %TDI-E-SYNTAX, Bad punctuation or misspelled word or number
                if(DEBUG.M) System.err.println("# MdsMisc->GetXYSignal() is not available on the server: " + exc);
                mds.MdsValue("1");
            }
            float y[] = GetFloatArray(setTimeContext + c.y());
            if(DEBUG.D) System.out.println(">> y = " + y);
            if(DEBUG.A) DEBUG.printFloatArray(y, y.length, 1, 1);
            RealArray xReal = GetRealArray(c.x());
            if(xReal.isLong){
                isXLong = true;
                return new XYData(xReal.getLongArray(), y, 1E12);
            }
            isXLong = false;
            double x[] = xReal.getDoubleArray();
            if(DEBUG.D) System.out.println(">> x = " + x);
            if(DEBUG.A) DEBUG.printDoubleArray(x, x.length, 1, 1);
            return new XYData(x, y, 1E12);
        }

        /*
        private String getTimeContext(double xmin, double xmax, boolean isLong) throws Exception {
            if(DEBUG.M)
                System.out.println("MdsDataProvider.SimpleWaveData.setTimeContext(" + xmin + ", " + xmax + ", " + isLong + ")");
            
            String res;
            try{
                if(xmin == Double.NEGATIVE_INFINITY && xmax == Double.POSITIVE_INFINITY) res = "SetTimeContext(*,*,*);";
                else if(xmin == Double.NEGATIVE_INFINITY){
                    if(isLong) res = "SetTimeContext(*, QUADWORD(" + (long)xmax + "Q), *);";
                    else res = "SetTimeContext(*, " + xmax + ", *);";
                }else if(xmax == Double.POSITIVE_INFINITY){
                    if(isLong) res = "SetTimeContext(QUADWORD(" + (long)xmin + "Q),*, *);";
                    else res = "SetTimeContext(" + xmin + ",*, *);";
                }else{
                    if(isLong) res = "SetTimeContext(QUADWORD(" + (long)xmin + "Q),QUADWORD(" + (long)xmax + "Q), *);";
                    else res = "SetTimeContext(" + xmin + "," + xmax + ", *);";
                }
            }catch(Exception exc){
                if(DEBUG.M){
                    System.err.println("# MdsDataProvider.SimpleWaveData.setTimeContext: " + exc);
                }
                res = "";
            }
            return res;
        }
        */
        private XYData getXYSignal(double xmin, double xmax, int numPoints, boolean isLong, String setTimeContext) throws Exception {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getXYSignal(" + xmin + ", " + xmax + ", " + numPoints + ", " + isLong + ", \"" + setTimeContext + "\")");
            // If the requeated number of mounts is Integer.MAX_VALUE, force the old way of getting data
            if(numPoints == Integer.MAX_VALUE) throw new Exception("Use Old Method for getting data");
            XYData res = null;
            double maxX = 0;
            Vector<Descriptor> args = new Vector<Descriptor>();
            args.addElement(new Descriptor(null, c.yo()));
            args.addElement(new Descriptor(null, c.xo()));
            if(isLong){
                args.addElement(new Descriptor(null, new long[]{(xmin == Double.NEGATIVE_INFINITY) ? 0 : (long)xmin}));
                args.addElement(new Descriptor(null, new long[]{(xmax == Double.POSITIVE_INFINITY) ? 0 : (long)xmax}));
            }else{
                args.addElement(new Descriptor(null, new float[]{(float)xmin}));
                args.addElement(new Descriptor(null, new float[]{(float)xmax}));
            }
            args.addElement(new Descriptor(null, new int[]{numPoints}));
            byte[] retData;
            int nSamples;
            // all fine if setTimeContext is an empty string
            // if a space is required between ; and further code setTimeContext sould have it
            try{
                if(isLong) retData = GetByteArray(setTimeContext + "MdsMisc->GetXYSignalLongTimes:DSC", args);
                else retData = GetByteArray(setTimeContext + "MdsMisc->GetXYSignal:DSC", args);
            }catch(IOException exc){
                throw(new Exception(exc.getMessage()));
            }
            if(DEBUG.D) System.out.println(">> MdsMisc->GetXYSignal*Long*Times:DSC");
            if(DEBUG.A) DEBUG.printByteArray(retData, 1, 8, 8, 1);
            /*Decode data: Format:
                   -retResolution(float)
                   -number of samples (minumum between X and Y)
                   -type of X xamples (byte: long(1), double(2) or float(3))
                   -y samples
                   -x Samples
            */
            if(DEBUG.D) System.out.println(">> retData " + retData.length + " " + retData);
            ByteArrayInputStream bis = new ByteArrayInputStream(retData);
            DataInputStream dis = new DataInputStream(bis);
            float fRes;
            double dRes;
            fRes = dis.readFloat();
            if(fRes >= 1E10) dRes = Double.POSITIVE_INFINITY;
            else dRes = fRes;
            nSamples = dis.readInt();
            if(nSamples <= 0){
                error = "No Samples returned";
                return null;
            }
            byte type = dis.readByte();
            float y[] = new float[nSamples];
            for(int i = 0; i < nSamples; i++)
                y[i] = dis.readFloat();
            if(type == 1) // Long X (i.e. absolute times
            {
                long[] longX = new long[nSamples];
                for(int i = 0; i < nSamples; i++)
                    longX[i] = dis.readLong();
                isXLong = true;
                res = new XYData(longX, y, dRes);
                if(longX.length > 0) maxX = longX[longX.length - 1];
                else maxX = 0;
            }else if(type == 2) // double X
            {
                double[] x = new double[nSamples];
                for(int i = 0; i < nSamples; i++)
                    x[i] = dis.readDouble();
                res = new XYData(x, y, dRes);
                if(x.length > 0) maxX = x[x.length - 1];
                else maxX = 0;
            }else // float X
            {
                double[] x = new double[nSamples];
                for(int i = 0; i < nSamples; i++)
                    x[i] = dis.readFloat();
                res = new XYData(x, y, dRes);
                if(x.length > 0) maxX = x[x.length - 1];
                else maxX = 0;
            }
            // Get title, xLabel and yLabel
            int titleLen = dis.readInt();
            if(titleLen > 0){
                byte[] titleBuf = new byte[titleLen];
                dis.readFully(titleBuf);
                title = new String(titleBuf, "UTF-8");
            }
            int xLabelLen = dis.readInt();
            if(xLabelLen > 0){
                byte[] xLabelBuf = new byte[xLabelLen];
                dis.readFully(xLabelBuf);
                xLabel = new String(xLabelBuf, "UTF-8");
            }
            int yLabelLen = dis.readInt();
            if(yLabelLen > 0){
                byte[] yLabelBuf = new byte[yLabelLen];
                dis.readFully(yLabelBuf);
                yLabel = new String(yLabelBuf, "UTF-8");
            }
            titleEvaluated = xLabelEvaluated = yLabelEvaluated = true;
            if(type == 1) isLong = true;
            if(segmentMode == SEGMENTED_YES && continuousUpdate){
                long refreshPeriod = jScopeFacade.getRefreshPeriod();
                if(refreshPeriod <= 0) refreshPeriod = 1000; // default 1 s refresh
                updateWorker.updateInfo(/*xmin*/maxX, Double.POSITIVE_INFINITY, 2000, waveDataListenersV, this, isLong, refreshPeriod);
            }
            return res;
        }

        @Override
        public XYData getData(int numPoints) throws Exception {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getData(" + numPoints + ")");
            return getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, numPoints);
        }

        @Override
        public float[] getZ() {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getZ()");
            try{
                return GetFloatArray(c.y());
            }catch(Exception exc){
                return null;
            }
        }
        private long x2DLong[];

        @Override
        public double[] getX2D() {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getX2D()");
            try{
                RealArray realArray = GetRealArray("DIM_OF(" + c.y() + ", 0)");
                if(realArray.isLong){
                    this.isXLong = true;
                    x2DLong = realArray.getLongArray();
                    return null;
                }
                x2DLong = null;
                return realArray.getDoubleArray();
                // return GetFloatArray(in);
            }catch(Exception exc){
                return null;
            }
        }

        @Override
        public long[] getX2DLong() {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getX2DLong()");
            return x2DLong;
        }

        @Override
        public float[] getY2D() {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getY2D()");
            try{
                return GetFloatArray("DIM_OF(" + c.y() + ", 1)");
            }catch(Exception exc){
                return null;
            }
        }

        // Cesare Mar 2015
        public float[] getX_Z() {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getX_Z()");
            try{
                return GetFloatArray("(" + c.x() + ")");
            }catch(Exception exc){
                return null;
            }
        }

        public float[] getX_X2D() {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getX_X2D()");
            try{
                return GetFloatArray("DIM_OF(" + c.x() + ", 0)");
            }catch(Exception exc){
                return null;
            }
        }

        public float[] getX_Y2D() {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getX_Y2D()");
            try{
                return GetFloatArray("DIM_OF(" + c.x() + ", 1)");
            }catch(Exception exc){
                return null;
            }
        }

        // End
        // public double[] getXLimits(){System.out.println("BADABUM!!"); return null;}
        // public long []getXLong(){System.out.println("BADABUM!!"); return null;}
        @Override
        public boolean isXLong() {
            return isXLong;
        }
        // Async update management
        Vector<WaveDataListener> waveDataListenersV = new Vector<WaveDataListener>();

        @Override
        public void addWaveDataListener(WaveDataListener listener) {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.addWaveDataListener()");
            waveDataListenersV.addElement(listener);
            if(asynchSource != null) asynchSource.addDataListener(listener);
        }

        @Override
        public void getDataAsync(double lowerBound, double upperBound, int numPoints) {
            if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getDataAsync(" + lowerBound + ", " + upperBound + ", " + numPoints + ")");
            updateWorker.updateInfo(lowerBound, upperBound, numPoints, waveDataListenersV, this, isXLong);
        }
    } // END Inner Class SimpleWaveData
      // Inner class UpdateWorker handler asynchronous requests for getting (portions of) data
    class UpdateWorker extends Thread{
        class UpdateDescriptor{
            double                   updateLowerBound;
            double                   updateUpperBound;
            int                      updatePoints;
            Vector<WaveDataListener> waveDataListenersV;
            SimpleWaveData           simpleWaveData;
            boolean                  isXLong;
            long                     updateTime;

            UpdateDescriptor(double updateLowerBound, double updateUpperBound, int updatePoints, Vector<WaveDataListener> waveDataListenersV, SimpleWaveData simpleWaveData, boolean isXLong, long updateTime){
                this.updateLowerBound = updateLowerBound;
                this.updateUpperBound = updateUpperBound;
                this.updatePoints = updatePoints;
                this.waveDataListenersV = waveDataListenersV;
                this.simpleWaveData = simpleWaveData;
                this.isXLong = isXLong;
                this.updateTime = updateTime;
            }
        }
        boolean                  enabled   = true;
        Vector<UpdateDescriptor> requestsV = new Vector<UpdateDescriptor>();

        void updateInfo(double updateLowerBound, double updateUpperBound, int updatePoints, Vector<WaveDataListener> waveDataListenersV, SimpleWaveData simpleWaveData, boolean isXLong, long delay) {
            intUpdateInfo(updateLowerBound, updateUpperBound, updatePoints, waveDataListenersV, simpleWaveData, isXLong, Calendar.getInstance().getTimeInMillis() + delay);
        }

        void updateInfo(double updateLowerBound, double updateUpperBound, int updatePoints, Vector<WaveDataListener> waveDataListenersV, SimpleWaveData simpleWaveData, boolean isXLong) {
            // intUpdateInfo(updateLowerBound, updateUpperBound, updatePoints, waveDataListenersV, simpleWaveData, isXLong,
            // Calendar.getInstance().getTimeInMillis());
            intUpdateInfo(updateLowerBound, updateUpperBound, updatePoints, waveDataListenersV, simpleWaveData, isXLong, -1);
        }

        synchronized void intUpdateInfo(double updateLowerBound, double updateUpperBound, int updatePoints, Vector<WaveDataListener> waveDataListenersV, SimpleWaveData simpleWaveData, boolean isXLong, long updateTime) {
            if(updateTime > 0) // If a delayed request for update
            {
                for(int i = 0; i < requestsV.size(); i++){
                    UpdateDescriptor currUpdateDescr = requestsV.elementAt(i);
                    if(currUpdateDescr.updateTime > 0 && currUpdateDescr.simpleWaveData == simpleWaveData) return; // Another delayed update request for that signal is already in the pending list
                }
            }
            requestsV.add(new UpdateDescriptor(updateLowerBound, updateUpperBound, updatePoints, waveDataListenersV, simpleWaveData, isXLong, updateTime));
            notify();
        }

        synchronized void enableAsyncUpdate(boolean enabled) {
            this.enabled = enabled;
            if(enabled) notify();
        }
        boolean stopWorker = false;

        synchronized void stopUpdateWorker() {
            stopWorker = true;
            notify();
        }

        @Override
        public void run() {
            if(DEBUG.M){
                System.out.println("run()");
            }
            this.setName("UpdateWorker");
            while(true){
                synchronized(this){
                    try{
                        wait();
                        if(stopWorker) return;
                    }catch(InterruptedException exc){}
                }
                if(!enabled) continue;
                long currTime = Calendar.getInstance().getTimeInMillis();
                long nextTime = -1;
                int i = 0;
                while(i < requestsV.size()){
                    if(!enabled) break;
                    UpdateDescriptor currUpdate = requestsV.elementAt(i);
                    if(currUpdate.updateTime < currTime){
                        try{
                            requestsV.removeElementAt(i);
                            XYData currData = currUpdate.simpleWaveData.getData(currUpdate.updateLowerBound, currUpdate.updateUpperBound, currUpdate.updatePoints, currUpdate.isXLong);
                            if(currData == null || currData.nSamples == 0) continue;
                            for(int j = 0; j < currUpdate.waveDataListenersV.size(); j++){
                                if(currUpdate.isXLong) currUpdate.waveDataListenersV.elementAt(j).dataRegionUpdated(currData.xLong, currData.y, currData.resolution);
                                else currUpdate.waveDataListenersV.elementAt(j).dataRegionUpdated(currData.x, currData.y, currData.resolution);
                            }
                        }catch(Exception exc){
                            Date d = new Date();
                            if(DEBUG.M){
                                System.err.println(d + " Error in asynchUpdate: " + exc);
                            }
                        }
                    }else{
                        if(nextTime == -1 || nextTime > currUpdate.updateTime) // It will alway be nextTime != -1
                        nextTime = currUpdate.updateTime;
                        i++;
                    }
                }
                if(nextTime != -1) // If a pending request for which time did not expire, schedure a new notification
                {
                    currTime = Calendar.getInstance().getTimeInMillis();
                    java.util.Timer timer = new java.util.Timer();
                    timer.schedule(new TimerTask(){
                        @Override
                        public void run() {
                            synchronized(UpdateWorker.this){
                                UpdateWorker.this.notify();
                            }
                        }
                    }, (nextTime < currTime + 50) ? 50 : (nextTime - currTime + 1));
                }
            }
        }
    } // End Inner class UpdateWorker
    UpdateWorker updateWorker;

    protected MdsConnection getConnection() {
        if(DEBUG.M) System.out.println("MdsDataProvider(\"" + provider + "\")");
        return new MdsConnection();
    }

    @Override
    protected void finalize() {
        if(DEBUG.M) System.out.println("MdsDataProvider.finalize()");
        if(open) mds.MdsValue("JavaClose(\"" + experiment + "\"," + shot + ")");
        if(connected) mds.DisconnectFromMds();
        if(DEBUG.D) System.out.println(">> disconnected");
    }

    // To be overridden by any DataProvider implementation with added dynamic generation
    @SuppressWarnings("static-method")
    public jScope.AsynchDataSource getAsynchSource() {
        return null;
    }

    @Override
    public void SetArgument(String arg) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.SetArgument(" + arg + ")");
        setProvider(arg);
        mds.setProvider(provider);
    }

    private void setProvider(String arg) {
        if(DEBUG.M) System.out.println("MdsDataProvider.setProvider(" + arg + ")");
        if(is_tunneling) provider = tunnel_provider;
        else provider = arg;
    }

    public static boolean SupportsCompression() {
        return true;
    }

    public void SetCompression(boolean state) {
        if(DEBUG.M) System.out.println("MdsDataProvider.SetCompression(" + state + ")");
        if(connected) Dispose();
        use_compression = state;
    }

    protected String GetExperimentName(String in_frame) {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetExperimentName(\"" + in_frame + "\")");
        if(experiment == null){
            if(in_frame.indexOf(".") == -1) return in_frame;
            return in_frame.substring(0, in_frame.indexOf("."));
        }
        return experiment;
    }

    @Override
    public FrameData GetFrameData(String in_y, String in_x, float time_min, float time_max) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetFrameData(\"" + in_y + "\", \"" + in_x + "\", " + time_min + ", " + time_max + ")");
        int[] numSegments = null;
        try{
            numSegments = GetIntArray("GetNumSegments(" + in_y + ")");
        }catch(Exception exc){
            error = null;
        }
        if(numSegments != null && numSegments[0] > 0){
            int numDims[] = GetIntArray("NDims(" + in_y + ")");
            if(numDims != null && numDims[0] > 2) return new SegmentedFrameData(in_y, in_x, time_min, time_max, numSegments[0]);
        }
        try{
            return(new SimpleFrameData(in_y, in_x, time_min, time_max));
        }catch(Exception exc){
            if(DEBUG.D) System.err.println("# MdsDataProvider.SimpleFrameData: " + exc);
            return null;
        }
    }

    public synchronized AllFrames GetAllFrames(String in_frame) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetAllFrames(" + in_frame + ")");
        ByteArray img = null;
        double time[] = null;
        int shape[];
        // int pixel_size = 8;
        // int num_time = 0;
        img = getByteArray("_jScope_img = IF_ERROR(EVALUATE(" + in_frame + "),*)");
        if(img == null) return null;
        if(DEBUG.D) System.out.println(">> MdsDataProvider.getByteArray: " + img.buf.length);
        shape = GetIntArray("SHAPE( _jScope_img )");
        time = GetDoubleArray("D_FLOAT(DIM_OF( _jScope_img ))");
        mds.MdsValue("DEALLOCATE('_jScope_img')");
        if(time == null || shape == null) return null;
        if(DEBUG.D) System.out.println(">> MdsDataProvider.GetDoubleArray: " + time.length);
        /*
        if(shape.length == 3){
            num_time = shape[2];
            pixel_size = img.buf.length / (shape[0] * shape[1] * shape[2]) * 8;
        }else{
            if(shape.length == 2){
                num_time = 1;
                pixel_size = img.buf.length / (shape[0] * shape[1]) * 8;
            }else if(shape.length == 1){ throw(new IOException("The evaluated signal is not an image")); }
        }
        */
        if(shape.length == 1) throw(new IOException("The evaluated signal is not an image"));
        return new AllFrames(img, shape[0], shape[1], time);
    }

    public synchronized double[] GetFrameTimes(String in_frame) {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetFrameTimes(\"" + in_frame + "\")");
        String exp = GetExperimentName(in_frame);
        String in = "JavaGetFrameTimes(\"" + exp + "\",\"" + in_frame + "\"," + shot + " )";
        Descriptor desc = mds.MdsValue(in);
        double out_data[];
        switch(desc.dtype){
            case Descriptor.DTYPE_FLOAT:
                out_data = new double[desc.float_data.length];
                for(int i = 0; i < desc.float_data.length; i++)
                    out_data[i] = desc.float_data[i];
                return out_data;
            case Descriptor.DTYPE_DOUBLE:
                return desc.double_data;
            case Descriptor.DTYPE_LONG:
                out_data = new double[desc.int_data.length];
                for(int i = 0; i < desc.int_data.length; i++)
                    out_data[i] = desc.int_data[i];
                return out_data;
            case Descriptor.DTYPE_BYTE:
                error = "Cannot convert byte array to double array";
                return null;
            case Descriptor.DTYPE_CSTRING:
                if((desc.status & 1) == 0) error = desc.error;
                return null;
        }
        return null;
    }

    public byte[] GetFrameAt(String in_frame, int frame_idx) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetFrameAt(\"" + in_frame + "\", " + frame_idx + ")");
        String exp = GetExperimentName(in_frame);
        String in = "JavaGetFrameAt(\"" + exp + "\",\" " + in_frame + "\"," + shot + ", " + frame_idx + " )";
        return GetByteArray(in);
    }

    public synchronized byte[] GetByteArray(String in) throws IOException {
        return getByteArray(in, null).buf;
    }

    public synchronized byte[] GetByteArray(String in, Vector<Descriptor> args) throws IOException {
        return getByteArray(in, args).buf;
    }

    public synchronized ByteArray getByteArray(String in) throws IOException {
        return getByteArray(in, null);
    }

    public synchronized ByteArray getByteArray(String in, Vector<Descriptor> args) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.getByteArray(\"" + in + "\", " + args + ")");
        if(!CheckOpen()) throw new IOException("TreeNotOpen");
        if(DEBUG.D) System.out.println(">> mds = " + mds);
        Descriptor desc = mds.MdsValue(in, args);
        if(desc == null || desc.dtype == 0) throw new IOException("MdsValue: no result (" + in + ")");
        if(DEBUG.D) System.out.println(">> desc.dtype = " + desc.dtype);
        ByteArrayOutputStream dosb = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(dosb);
        switch(desc.dtype){
            case Descriptor.DTYPE_DOUBLE:
                for(int i = 0; i < desc.double_data.length; i++)
                    dos.writeFloat((float)desc.double_data[i]);
                return new ByteArray(dosb.toByteArray(), Descriptor.DTYPE_FLOAT);
            case Descriptor.DTYPE_FLOAT:
                for(int i = 0; i < desc.float_data.length; i++)
                    dos.writeFloat(desc.float_data[i]);
                dos.close();
                return new ByteArray(dosb.toByteArray(), desc.dtype);
            case Descriptor.DTYPE_USHORT:
            case Descriptor.DTYPE_SHORT: // bdb hacked this to try to make profile dialog read true data values, not normalised
                for(int i = 0; i < desc.short_data.length; i++)
                    dos.writeShort(desc.short_data[i]);
                dos.close();
                return new ByteArray(dosb.toByteArray(), desc.dtype);
            case Descriptor.DTYPE_ULONG:
            case Descriptor.DTYPE_LONG:
                for(int i = 0; i < desc.int_data.length; i++)
                    dos.writeInt(desc.int_data[i]);
                dos.close();
                return new ByteArray(dosb.toByteArray(), desc.dtype);
            case Descriptor.DTYPE_UBYTE:
            case Descriptor.DTYPE_BYTE:
                return new ByteArray(desc.byte_data, desc.dtype);
            case Descriptor.DTYPE_CSTRING:
                if(DEBUG.D) System.out.println("# " + desc.strdata);
                if((desc.status & 1) == 0) error = desc.error;
                throw new IOException(error);
        }
        throw new IOException(error);
    }

    @Override
    public synchronized String ErrorString() {
        return error;
    }

    @Override
    public synchronized void Update(String experiment, long shot) {
        Update(experiment, shot, false);
    }

    public synchronized void Update(String experiment, long shot, boolean resetExperiment) {
        if(DEBUG.M) System.out.println("MdsDataProvider.Update(\"" + experiment + "\", " + shot + ", " + resetExperiment + ")");
        this.error = null;
        if(resetExperiment) this.experiment = null;
        if((shot != this.shot) || (shot == 0L) || (this.experiment == null) || (this.experiment.length() == 0) || (!this.experiment.equalsIgnoreCase(experiment))){
            this.experiment = ((experiment != null) && (experiment.trim().length() > 0) ? experiment : null);
            this.shot = shot;
            this.open = false;
        }
    }

    @Override
    public synchronized String GetString(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetString(\"" + in + "\")");
        if(in == null) return null;
        error = null;
        if(NotYetString(in)){
            if(!CheckOpen()) return null;
            Descriptor desc = mds.MdsValue(in);
            switch(desc.dtype){
                case Descriptor.DTYPE_BYTE:
                case Descriptor.DTYPE_UBYTE:
                    return new String(desc.byte_data, "UTF-8");
                case Descriptor.DTYPE_FLOAT:
                    error = "Cannot convert a float to string";
                    throw new IOException(error);
                case Descriptor.DTYPE_CSTRING:
                    if((desc.status & 1) == 1) return desc.strdata;
                    return(error = desc.error);
            }
            if(desc.error == null) return "Undefined error";
            return(error = desc.error);
        }
        return new String(in.getBytes(), 1, in.length() - 2, "UTF-8");
    }

    @Override
    public synchronized void SetEnvironment(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.SetEnvironment(\"" + in + "\")");
        if(in == null || in.length() == 0) return;
        Properties pr = new Properties();
        pr.load(new ByteArrayInputStream(in.getBytes()));
        String def_node = pr.getProperty("__default_node");
        if(def_node != null){
            def_node = def_node.trim();
            if(!(default_node != null && def_node.equals(default_node)) || (def_node.length() == 0 && default_node != null)){
                default_node = (def_node.length() == 0) ? null : def_node;
                def_node_changed = true;
            }
            return;
        }
        if(in.indexOf("pulseSetVer") >= 0) open = false;
        if(environment_vars == null || !environment_vars.equalsIgnoreCase(in)){
            open = false;
            environment_vars = in;
        }
    }

    void SetEnvironmentSpecific(String in) {
        if(DEBUG.M) System.out.println("MdsDataProvider.SetEnvironmentSpecific(\"" + in + "\")");
        Descriptor desc = mds.MdsValue(in);
        switch(desc.dtype){
            case Descriptor.DTYPE_CSTRING:
                if((desc.status & 1) == 0) error = desc.error;
        }
    }

    public void enableAsyncUpdate(boolean enable) {
        updateWorker.enableAsyncUpdate(enable);
    }

    private static double GetDate(String in) throws Exception {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetDate(\"" + in + "\")");
        Calendar cal = Calendar.getInstance();
        // cal.setTimeZone(TimeZone.getTimeZone("GMT+00"));
        DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm Z");
        // DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm");-
        Date date = df.parse(in + " GMT");
        // Date date = df.parse(in);
        cal.setTime(date);
        long javaTime = cal.getTime().getTime();
        return javaTime;
    }

    private static double GetNow(String in) throws Exception {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetNow(\"" + in + "\")");
        boolean isPlus = true;
        int hours = 0, minutes = 0, seconds = 0;
        String currStr = in.trim().toUpperCase();
        if(!currStr.startsWith("NOW")) throw new Exception();
        currStr = currStr.substring(3).trim();
        if(currStr.length() > 0) // Not only NOW
        {
            if(currStr.startsWith("+")) isPlus = true;
            else if(currStr.startsWith("-")) isPlus = false;
            else throw new Exception();
            currStr = currStr.substring(1).trim();
            StringTokenizer st = new StringTokenizer(currStr, ":", true);
            String currTok = st.nextToken();
            if(currTok.equals(":")) hours = 0;
            else{
                hours = Integer.parseInt(currTok);
                currTok = st.nextToken();
            }
            if(!currTok.equals(":")) throw new Exception();
            currTok = st.nextToken();
            if(currTok.equals(":")) minutes = 0;
            else{
                minutes = Integer.parseInt(currTok);
                currTok = st.nextToken();
            }
            if(!currTok.equals(":")) throw new Exception();
            if(st.hasMoreTokens()){
                seconds = Integer.parseInt(st.nextToken());
            }
        }
        if(!isPlus){
            hours = -hours;
            minutes = -minutes;
            seconds = -seconds;
        }
        Calendar cal = Calendar.getInstance();
        // cal.setTimeZone(TimeZone.getTimeZone("GMT+00"));
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, hours);
        cal.add(Calendar.MINUTE, minutes);
        cal.add(Calendar.SECOND, seconds);
        long javaTime = cal.getTime().getTime();
        return javaTime;
    }

    @Override
    public synchronized double GetFloat(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetFloat(\"" + in + "\")");
        error = null;
        // First check Whether this is a date
        try{
            return GetDate(in);
        }catch(Exception excD){}
        try{
            return GetNow(in);
        }catch(Exception excN){}
        if(NotYetNumber(in)){
            if(!CheckOpen()) return 0;
            Descriptor desc = mds.MdsValue(in);
            if(desc.error != null) error = desc.error;
            switch(desc.dtype){
                case Descriptor.DTYPE_DOUBLE:
                    return desc.double_data[0];
                case Descriptor.DTYPE_FLOAT:
                    return desc.float_data[0];
                case Descriptor.DTYPE_LONG:
                    return desc.int_data[0];
                case Descriptor.DTYPE_BYTE:
                case Descriptor.DTYPE_UBYTE:
                    return desc.byte_data[0];
                case Descriptor.DTYPE_CSTRING:
                    if((desc.status & 1) == 0){
                        error = desc.error;
                        throw(new IOException(error));
                    }
                    return 0;
            }
        }else return new Float(in).floatValue();
        return 0;
    }

    @Override
    public WaveData GetWaveData(String in) {
        return new SimpleWaveData(in, experiment, shot);
    }

    @Override
    public WaveData GetWaveData(String in_y, String in_x) {
        return new SimpleWaveData(in_y, in_x, experiment, shot);
    }

    public float[] GetFloatArray(String in) throws IOException {
        RealArray realArray = GetRealArray(in);
        if(realArray == null) return null;
        return realArray.getFloatArray();
    }

    public double[] GetDoubleArray(String in) throws IOException {
        RealArray realArray = GetRealArray(in);
        if(realArray == null) return null;
        return realArray.getDoubleArray();
    }

    public synchronized RealArray GetRealArray(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetRealArray(\"" + in + "\")");
        ConnectionEvent e = new ConnectionEvent(this, 1, 0);
        DispatchConnectionEvent(e);
        if(!CheckOpen()) return null;
        Descriptor desc = mds.MdsValue(in);
        RealArray out = null;
        if(DEBUG.D) System.out.println(">> MdsDataProvider.GetRealArray: " + desc.dtype);
        switch(desc.dtype){
            case Descriptor.DTYPE_FLOAT:
                out = new RealArray(desc.float_data);
                break;
            case Descriptor.DTYPE_DOUBLE:
                out = new RealArray(desc.double_data);
                break;
            case Descriptor.DTYPE_ULONG:
            case Descriptor.DTYPE_LONG:{
                float[] outF = new float[desc.int_data.length];
                for(int i = 0; i < desc.int_data.length; i++)
                    outF[i] = desc.int_data[i];
                out = new RealArray(outF);
            }
                break;
            case Descriptor.DTYPE_USHORT:
            case Descriptor.DTYPE_SHORT:{
                float[] outF = new float[desc.short_data.length];
                for(int i = 0; i < desc.short_data.length; i++)
                    outF[i] = desc.short_data[i];
                out = new RealArray(outF);
            }
                break;
            case Descriptor.DTYPE_UBYTE:
            case Descriptor.DTYPE_BYTE:{
                float[] outF = new float[desc.byte_data.length];
                for(int i = 0; i < desc.byte_data.length; i++)
                    outF[i] = desc.byte_data[i];
                out = new RealArray(outF);
            }
                break;
            case Descriptor.DTYPE_ULONGLONG:
            case Descriptor.DTYPE_LONGLONG:{
                out = new RealArray(desc.long_data);
            }
                break;
            case Descriptor.DTYPE_CSTRING:
                if(DEBUG.D) System.out.println(">> MdsDataProvider.GetRealArray: " + desc.dtype);
                if((desc.status & 1) == 0) error = desc.error;
                break;
            default:
                error = "Data type code : " + desc.dtype + " not yet supported (RealArray.GetRealArray)";
        }
        return out;
    }

    @Override
    public long[] GetShots(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetShots(\"" + in + "\")");
        // To shot evaluation don't execute check
        // if a pulse file is open
        CheckConnection();
        return GetLongArray(in);
    }

    public int[] GetIntArray(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetIntArray(\"" + in + "\")");
        if(!CheckOpen()) throw new IOException("Tree not open");
        return GetIntegerArray(in);
    }

    public synchronized long[] GetLongArray(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetLongArray(\"" + in + "\")");
        long out_data[];
        Descriptor desc = mds.MdsValue(in);
        switch(desc.dtype){
            case Descriptor.DTYPE_ULONGLONG:
            case Descriptor.DTYPE_LONGLONG:
                return desc.long_data;
            case Descriptor.DTYPE_LONG:
                out_data = new long[desc.int_data.length];
                for(int i = 0; i < desc.int_data.length; i++){
                    out_data[i] = (desc.int_data[i]);
                }
                return out_data;
            case Descriptor.DTYPE_FLOAT:
                out_data = new long[desc.float_data.length];
                for(int i = 0; i < desc.float_data.length; i++)
                    out_data[i] = (long)(desc.float_data[i] + 0.5);
                return out_data;
            case Descriptor.DTYPE_BYTE:
            case Descriptor.DTYPE_UBYTE:
                out_data = new long[desc.byte_data.length];
                for(int i = 0; i < desc.byte_data.length; i++)
                    out_data[i] = (long)(desc.byte_data[i] + 0.5);
                return out_data;
            case Descriptor.DTYPE_CSTRING:
                if((desc.status & 1) == 0) error = desc.error;
                throw new IOException(error);
            default:
                error = "Data type code : " + desc.dtype + " not yet supported (GetLongArray)";
        }
        throw new IOException(error);
    }

    private synchronized int[] GetIntegerArray(String in) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetIntegerArray(\"" + in + "\")");
        int out_data[];
        Descriptor desc = mds.MdsValue(in);
        switch(desc.dtype){
            case Descriptor.DTYPE_LONG:
                return desc.int_data;
            case Descriptor.DTYPE_FLOAT:
                out_data = new int[desc.float_data.length];
                for(int i = 0; i < desc.float_data.length; i++)
                    out_data[i] = (int)(desc.float_data[i] + 0.5);
                return out_data;
            case Descriptor.DTYPE_BYTE:
            case Descriptor.DTYPE_UBYTE:
                out_data = new int[desc.byte_data.length];
                for(int i = 0; i < desc.byte_data.length; i++)
                    out_data[i] = (int)(desc.byte_data[i] + 0.5);
                return out_data;
            case Descriptor.DTYPE_CSTRING:
                if((desc.status & 1) == 0) error = desc.error;
                throw new IOException(error);
            default:
                error = "Data type code : " + desc.dtype + " not yet supported (GetIntegerArray)";
        }
        throw new IOException(error);
    }

    @Override
    public synchronized void Dispose() {
        if(DEBUG.M) System.out.println("MdsDataProvider.Dispose()");
        if(is_tunneling && ssh_tunneling != null) ssh_tunneling.Dispose();
        if(connected){
            connected = false;
            mds.DisconnectFromMds();
            if(DEBUG.D) System.out.println(">> disconnected");
            ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.LOST_CONNECTION, "Lost connection from : " + provider);
            mds.dispatchConnectionEvent(ce);
        }
        if(updateWorker != null && updateWorker.isAlive()){
            updateWorker.stopUpdateWorker();
        }
    }

    protected synchronized void CheckConnection() throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.CheckConnection()");
        if(!connected){
            if(mds.ConnectToMds(use_compression) == 0){
                if(mds.error != null) throw new IOException(mds.error);
                throw new IOException("Could not get IO for " + provider);
            }
            if(DEBUG.M) System.out.println("connected");
            connected = true;
            updateWorker = new UpdateWorker();
            updateWorker.start();
        }
    }

    protected synchronized boolean CheckOpen() throws IOException {
        return CheckOpen(this.experiment, this.shot);
    }

    protected synchronized boolean CheckOpen(String experiment, long shot) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.CheckOpen(\"" + experiment + "\", " + shot + ")");
        int status;
        if(!connected){
            status = mds.ConnectToMds(use_compression);
            if(status == 0){
                if(mds.error != null) throw new IOException("Cannot connect to data server : " + mds.error);
                error = "Cannot connect to data server";
                return false;
            }
            if(DEBUG.D) System.out.println(">> connected");
            connected = true;
            updateWorker = new UpdateWorker();
            updateWorker.start();
        }
        if(!open && experiment != null || this.shot != shot || experiment != null && !experiment.equalsIgnoreCase(this.experiment)){
            // System.out.println("\n-->\nOpen tree "+experiment+ " shot "+ shot +"\n<--\n");
            Descriptor descr = mds.MdsValue("JavaOpen(\"" + experiment + "\"," + shot + ")");
            if(descr.dtype != Descriptor.DTYPE_CSTRING && descr.dtype == Descriptor.DTYPE_LONG && descr.int_data != null && descr.int_data.length > 0 && (descr.int_data[0] % 2 == 1)){
                open = true;
                def_node_changed = true;
                this.shot = shot;
                this.experiment = experiment;
                if(environment_vars != null && environment_vars.length() > 0){
                    this.SetEnvironmentSpecific(environment_vars);
                    if(error != null){
                        error = "Public variable evaluation error " + experiment + " shot " + shot + " : " + error;
                        return false;
                    }
                }
            }else{
                if(mds.error != null) error = "Cannot open experiment " + experiment + " shot " + shot + " : " + mds.error;
                else error = "Cannot open experiment " + experiment + " shot " + shot;
                return false;
            }
        }
        if(open && def_node_changed){
            Descriptor descr;
            if(default_node != null){
                descr = mds.MdsValue("TreeSetDefault(\"\\\\" + default_node + "\")");
                if((descr.int_data[0] & 1) == 0) mds.MdsValue("TreeSetDefault(\"\\\\" + experiment + "::TOP\")");
            }else descr = mds.MdsValue("TreeSetDefault(\"\\\\" + experiment + "::TOP\")");
            def_node_changed = false;
        }
        return true;
    }

    protected static boolean NotYetString(String in) {
        if(DEBUG.M) System.out.println("MdsDataProvider.NotYetString(\"" + in + "\")");
        int i;
        if(in.charAt(0) == '\"'){
            for(i = 1; i < in.length() && (in.charAt(i) != '\"' || (i > 0 && in.charAt(i) == '\"' && in.charAt(i - 1) == '\\')); i++);
            if(i == (in.length() - 1)) return false;
        }
        return true;
    }

    protected static boolean NotYetNumber(String in) {
        if(DEBUG.M) System.out.println("MdsDataProvider.NotYetNumber(\"" + in + "\")");
        try{
            new Float(in);
        }catch(NumberFormatException e){
            return false;
        }
        return true;
    }

    @Override
    public synchronized void AddUpdateEventListener(UpdateEventListener l, String event_name) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.AddUpdateEventListener(" + l + "," + event_name + ")");
        if(event_name == null || event_name.trim().length() == 0) return;
        CheckConnection();
        mds.MdsSetEvent(l, event_name);
    }

    @Override
    public synchronized void RemoveUpdateEventListener(UpdateEventListener l, String event_name) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.RemoveUpdateEventListener(" + l + "," + event_name + ")");
        if(event_name == null || event_name.trim().length() == 0) return;
        CheckConnection();
        mds.MdsRemoveEvent(l, event_name);
    }

    @Override
    public synchronized void AddConnectionListener(ConnectionListener l) {
        if(DEBUG.M) System.out.println("MdsDataProvider.AddConnectionListener(" + l + ")");
        if(mds == null){ return; }
        mds.addConnectionListener(l);
    }

    @Override
    public synchronized void RemoveConnectionListener(ConnectionListener l) {
        if(DEBUG.M) System.out.println("MdsDataProvider.RemoveConnectionListener(" + l + ")");
        if(mds == null){ return; }
        mds.removeConnectionListener(l);
    }

    protected void DispatchConnectionEvent(ConnectionEvent e) {
        if(DEBUG.M) System.out.println("MdsDataProvider.DispatchConnectionEvent(" + e + ")");
        if(mds == null){ return; }
        mds.dispatchConnectionEvent(e);
    }

    @Override
    public boolean SupportsTunneling() {
        return true;
    }

    @Override
    public int InquireCredentials(JFrame f, DataServerItem server_item) {
        if(DEBUG.M) System.out.println("MdsDataProvider.InquireCredentials(" + f + ", " + server_item + ")");
        mds.setUser(server_item.user);
        is_tunneling = false;
        if(server_item.tunnel_port != null && server_item.tunnel_port.trim().length() != 0){
            StringTokenizer st = new StringTokenizer(server_item.argument, ":");
            String ip;
            String remote_port = "" + MdsConnection.DEFAULT_PORT;
            ip = st.nextToken();
            if(st.hasMoreTokens()) remote_port = st.nextToken();
            is_tunneling = true;
            try{
                ssh_tunneling = new SshTunneling(f, this, ip, remote_port, server_item.user, server_item.tunnel_port);
                ssh_tunneling.start();
                tunnel_provider = "127.0.0.1:" + server_item.tunnel_port;
            }catch(Throwable exc){
                if(exc instanceof NoClassDefFoundError) JOptionPane.showMessageDialog(f, "The MindTerm.jar library is required for ssh tunneling.You can download it from \nhttp://www.appgate.com/mindterm/download.php\n" + exc, "alert", JOptionPane.ERROR_MESSAGE);
                return DataProvider.LOGIN_ERROR;
            }
        }
        return DataProvider.LOGIN_OK;
    }

    public static boolean SupportsFastNetwork() {
        return true;
    }

    protected String GetStringValue(String expression) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetStringValue(\"" + expression + "\")");
        String out = GetString(expression);
        if(out == null || out.length() == 0 || error != null){
            error = null;
            return null;
        }
        if(out.indexOf(0) > 0) out = out.substring(0, out.indexOf(0));
        return out;
    }

    protected int[] GetNumDimensions(String expression) throws IOException {
        if(DEBUG.M) System.out.println("MdsDataProvider.GetNumDimensions(\"" + expression + "\")");
        // return GetIntArray(in_y);
        // Gabriele June 2013: reduce dimension if one component is 1
        int[] fullDims = GetIntArray("SHAPE( " + expression + " )");
        if(fullDims == null) return null;
        if(fullDims.length == 1) return fullDims;
        // count dimensions == 1
        int numDimensions = 0;
        for(int i = 0; i < fullDims.length; i++){
            if(fullDims[i] != 1) numDimensions++;
        }
        int[] retDims = new int[numDimensions];
        int j = 0;
        for(int i = 0; i < fullDims.length; i++){
            if(fullDims[i] != 1) retDims[j++] = fullDims[i];
        }
        return retDims;
    }
    static class RealArray{
        double  doubleArray[] = null;
        float   floatArray[]  = null;
        long    longArray[]   = null;
        boolean isDouble;
        boolean isLong;

        RealArray(float[] floatArray){
            if(DEBUG.M) System.out.println("MdsDataProvider.RealArray(" + floatArray + ")");
            this.floatArray = floatArray;
            isDouble = false;
            isLong = false;
        }

        RealArray(double[] doubleArray){
            if(DEBUG.M) System.out.println("MdsDataProvider.RealArray(" + doubleArray + ")");
            this.doubleArray = doubleArray;
            isDouble = true;
            isLong = false;
        }

        RealArray(long[] longArray){
            if(DEBUG.M) System.out.println("MdsDataProvider.RealArray(" + longArray + ")");
            this.longArray = longArray;
            for(int i = 0; i < longArray.length; i++)
                longArray[i] = jScopeFacade.convertFromSpecificTime(longArray[i]);
            isDouble = false;
            isLong = true;
        }

        float[] getFloatArray() {
            if(DEBUG.M) System.out.println("MdsDataProvider.RealArray.getFloatArray()");
            if(isLong) return null;
            if(isDouble && floatArray == null && doubleArray != null){
                floatArray = new float[doubleArray.length];
                for(int i = 0; i < doubleArray.length; i++)
                    floatArray[i] = (float)doubleArray[i];
            }
            return floatArray;
        }

        double[] getDoubleArray() {
            if(DEBUG.M) System.out.println("MdsDataProvider.RealArray.getDoubleArray()");
            if(DEBUG.D) System.out.println(">> " + isLong + isDouble + (floatArray != null) + (doubleArray != null));
            if(isLong) return null;
            if(!isDouble && floatArray != null && doubleArray == null){
                doubleArray = new double[floatArray.length];
                for(int i = 0; i < floatArray.length; i++)
                    doubleArray[i] = floatArray[i];
            }
            return doubleArray;
        }

        long[] getLongArray() {
            if(DEBUG.M) System.out.println("MdsDataProvider.RealArray.getLongArray()");
            if(isDouble) return null;
            return longArray;
        }
    }

    public boolean isPresent(String expression) {
        try{
            String out = GetString("TEXT(PRESENT( " + expression + " ))").trim();
            System.out.println(">> " + expression + " present = " + out);
            return out == "1";
        }catch(IOException exc){
            return false;
        }
    }
}
