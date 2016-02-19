package jScope;

/* $Id$ */
import static java.lang.System.err;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;
import javax.swing.JFrame;
import de.mpg.ipp.codac.signalaccess.IndexIterator;
import de.mpg.ipp.codac.signalaccess.Signal;
import de.mpg.ipp.codac.signalaccess.SignalAddressBuilder;
import de.mpg.ipp.codac.signalaccess.SignalReader;
import de.mpg.ipp.codac.signalaccess.SignalToolsFactory;
import de.mpg.ipp.codac.signalaccess.objylike.ArchieToolsFactory;
import de.mpg.ipp.codac.signalaccess.readoptions.ReadOptions;
import de.mpg.ipp.codac.w7xtime.TimeInterval;

final class W7XDataProvider implements DataProvider{
    MdsDataProvider mds;
    String          error;

    public W7XDataProvider(){
        mds = new MdsDataProvider();
    }

    public W7XDataProvider(String provider) throws IOException{
        mds = new MdsDataProvider(provider);
    }

    private static SignalReader getReader(String path) {
        if(path.toUpperCase().startsWith("/TEST/")){
            final SignalToolsFactory stf_test = ArchieToolsFactory.remoteArchive("Test");
            final SignalAddressBuilder sab_test = stf_test.makeSignalAddressBuilder(new String[0]);
            return stf_test.makeSignalReader(sab_test.newBuilder().path(path.substring(5)).build());
        }
        if(path.toUpperCase().startsWith("/ARCHIVEDB/")) path = path.substring(10);
        final SignalToolsFactory stf = ArchieToolsFactory.remoteArchive();
        final SignalAddressBuilder sab = stf.makeSignalAddressBuilder(new String[0]);
        return stf.makeSignalReader(sab.newBuilder().path(path).build());
    }

    private static TimeInterval getTimeInterval(long from, long upto) {
        return TimeInterval.ALL.withStart(from).withEnd(upto);
    }

    private static Signal getSignal(String path, TimeInterval ti, ReadOptions ro) {
        Signal sig;
        SignalReader sr = getReader(path);
        try{
            sig = sr.readSignal(ti, ro);
        }finally{
            sr.close();
        }
        return sig;
    }

    public static long[] getLong(Signal signal) {
        int count = signal.getSampleCount();
        long[] data = new long[count];
        if(count == 0) return data;
        IndexIterator iter = IndexIterator.of(signal);
        for(int i = 0; i < count; i++)
            data[i] = signal.getValue(Long.class, iter.next());
        return data;
    }

    public static float[] getFloat(Signal signal) {
        int count = signal.getSampleCount();
        float[] data = new float[count];
        if(count == 0) return data;
        IndexIterator iter = IndexIterator.of(signal);
        for(int i = 0; i < count; i++)
            data[i] = signal.getValue(Double.class, iter.next()).floatValue();
        return data;
    }

    public static double[] getDouble(Signal signal) {
        int count = signal.getSampleCount();
        double[] data = new double[count];
        if(count == 0) return data;
        IndexIterator iter = IndexIterator.of(signal);
        for(int i = 0; i < count; i++)
            data[i] = signal.getValue(Double.class, iter.next());
        return data;
    }

    public static byte[] getByteAt(Signal signal, int index, int frameType) throws IOException {
        int w = signal.getDimensionSize(1);
        int h = signal.getDimensionSize(2);
        if(frameType == FrameData.BITMAP_IMAGE_8){
            byte[] data = new byte[w * h];
            for(int iw = 0; iw < w; iw++)
                for(int ih = 0; ih < h; ih++)
                    signal.getValue(Byte.class, new int[]{index, iw, ih});
            return data;
        }
        ByteArrayOutputStream dosb = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(dosb);
        try{
            if(frameType == FrameData.BITMAP_IMAGE_16) for(int iw = 0; iw < w; iw++)
                for(int ih = 0; ih < h; ih++)
                    dos.writeShort(signal.getValue(Integer.class, new int[]{index, iw, ih}));
            else if(frameType == FrameData.BITMAP_IMAGE_32) for(int iw = 0; iw < w; iw++)
                for(int ih = 0; ih < h; ih++)
                    dos.writeInt(signal.getValue(Integer.class, new int[]{index, iw, ih}));
            else if(frameType == FrameData.BITMAP_IMAGE_FLOAT) for(int iw = 0; iw < w; iw++)
                for(int ih = 0; ih < h; ih++)
                    dos.writeFloat(signal.getValue(Float.class, new int[]{index, iw, ih}));
            dos.close();
            return dosb.toByteArray();
        }catch(IOException e){}
        return null;
    }

    private static boolean isW7X(String in) {
        return in.contains("_DATASTREAM");
    }

    public int[] GetNumDimensions(String in) throws IOException {
        if(isW7X(in)){
            System.err.println("W7XDataProvider.GetNumDimensions not implemented!");
            return new int[]{1};
        }
        return mds.GetNumDimensions(in);
    }

    @Override
    public synchronized String ErrorString() {
        String outerror = mds.ErrorString();
        if(error == null) return outerror;
        outerror = error;
        error = null;
        return error;
    }

    /** mds forwarding **/
    public static boolean SupportsCompression() {
        return MdsDataProvider.SupportsCompression();
    }

    public void SetCompression(boolean state) {
        mds.SetCompression(state);
    }

    @Override
    public int InquireCredentials(JFrame f, DataServerItem si) {
        return mds.InquireCredentials(f, si);
    }

    public static boolean SupportsFastNetwork() {
        return MdsDataProvider.SupportsFastNetwork();
    }

    @Override
    public boolean SupportsTunneling() {
        return mds.SupportsTunneling();
    }

    @Override
    public void SetArgument(String arg) throws IOException {
        mds.SetArgument(arg);
    }

    @Override
    public long[] GetShots(String in) throws IOException {
        return mds.GetShots(in);
    }

    @Override
    public synchronized void Dispose() {
        mds.Dispose();
    }

    @Override
    public synchronized void AddConnectionListener(ConnectionListener l) {
        mds.AddConnectionListener(l);
    }

    @Override
    public synchronized void RemoveConnectionListener(ConnectionListener l) {
        mds.RemoveConnectionListener(l);
    }

    @Override
    public synchronized void AddUpdateEventListener(UpdateEventListener l, String event_name) throws IOException {
        mds.AddUpdateEventListener(l, event_name);
    }

    @Override
    public synchronized void RemoveUpdateEventListener(UpdateEventListener l, String event_name) throws IOException {
        mds.RemoveUpdateEventListener(l, event_name);
    }

    @Override
    public synchronized double GetFloat(String in) throws IOException {
        return mds.GetFloat(in);
    }

    @Override
    public synchronized String GetString(String in) throws IOException {
        return mds.GetString(in);
    }

    @Override
    public synchronized void Update(String expt, long shot) {
        mds.Update(expt, shot);
    }

    @Override
    public synchronized void SetEnvironment(String in) throws IOException {
        mds.SetEnvironment(in);
    }

    /** end mds forwarding **/
    @Override
    public FrameData GetFrameData(String in_y, String in_x, float time_min, float time_max) throws IOException {
        return isW7X(in_y) ? new SimpleFrameData(in_y, in_x, time_min, time_max) : mds.GetFrameData(in_y, in_x, time_min, time_max);
    }
    class SimpleFrameData implements FrameData{
        Signal sig_y, sig_x;
        String in_y, in_x;
        int    frameType = 0;
        long   shot, from = 0, upto = 0, orig = 0;

        public SimpleFrameData(String in_y){
            this(in_y, null);
        }

        public SimpleFrameData(String in_y, String in_x){
            this(in_y, in_x, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        public SimpleFrameData(String in_y, String in_x, float time_min, float time_max){
            shot = mds.shot;
            this.in_x = in_x;
            this.in_y = in_y;
            long[] timing;
            try{
                try{
                    if(shot < 1) timing = mds.GetLongArray("TIME()");
                    else timing = mds.GetLongArray("TIME(" + shot + ")");
                }catch(IOException e){
                    error = "Time not set! Use TIME(from,upto,origin) or specify a valid shot number.";
                    throw new IOException(error);
                }
                this.orig = timing.length > 2 ? timing[2] : 0L;
                this.from = timing[0];
                this.upto = timing[1];
            }catch(Exception e){
                err.println(">>> W7XDataProvider.SimpleFrameData: " + e);
            }
            getSignals();
        }

        private void getSignals() {
            if(sig_y != null && sig_x != null) return;
            TimeInterval ti = getTimeInterval(from, upto);
            ReadOptions ro = ReadOptions.fetchAll();
            sig_y = W7XDataProvider.getSignal(in_y, ti, ro);
            sig_x = (in_x == null) ? sig_y.getDimensionSignal(0) : W7XDataProvider.getSignal(in_x, ti, ro);
        }

        @Override
        public int GetFrameType() throws IOException {
            if(frameType == 0){
                String type = sig_y.getComponentType().getSimpleName();
                if(type.equals("Byte")) frameType = FrameData.BITMAP_IMAGE_8;
                else if(type.equals("Short")) frameType = FrameData.BITMAP_IMAGE_16;
                else if(type.equals("Integer")) frameType = FrameData.BITMAP_IMAGE_32;
                else frameType = FrameData.BITMAP_IMAGE_FLOAT;
            }
            return frameType;
        }

        @Override
        public int GetNumFrames() throws IOException {
            return sig_y.getDimensionSize(0);
        }

        @Override
        public Dimension GetFrameDimension() throws IOException {
            return new Dimension(sig_y.getDimensionSize(2), sig_y.getDimensionSize(1));
        }

        @Override
        public double[] GetFrameTimes() throws IOException {
            getSignals();
            if(in_x == null){
                long[] x = W7XDataProvider.getLong(sig_x);
                double[] xd = new double[x.length];
                for(int i = 0; i < x.length; i++)
                    xd[i] = (x[i] - orig) / 1E9;
                return xd;
            }
            return W7XDataProvider.getDouble(sig_x);
        }

        @Override
        public byte[] GetFrameAt(int idx) throws IOException {
            getSignals();
            return getByteAt(sig_y, idx, GetFrameType());
        }
    }

    @Override
    public WaveData GetWaveData(String in) {
        return isW7X(in) ? new SimpleWaveData(in) : mds.GetWaveData(in);
    }

    @Override
    public WaveData GetWaveData(String in_y, String in_x) {
        return isW7X(in_y) ? new SimpleWaveData(in_y, in_x) : mds.GetWaveData(in_y, in_x);
    }
    class SimpleWaveData implements WaveData{
        public Signal sig_x, sig_y;
        String        in_x, in_y;
        long          shot, from, upto, orig = 0;
        float         xmax, xmin;
        int           n_points;
        boolean       isXLong          = false;
        boolean       resample         = false;
        boolean       _jscope_set      = false;
        boolean       continuousUpdate = false;
        class UpdateWorker extends Thread{
            SimpleWaveData swd;

            public UpdateWorker(SimpleWaveData swd){
                super();
                this.swd = swd;
            }
            boolean requests = false;

            void update() {
                intUpdate();
            }

            synchronized void intUpdate() {
                requests = true;
                notify();
            }

            @Override
            public void run() {
                this.setName(swd.in_y);
                try{
                    swd.getSignals();
                }catch(Exception e){
                    System.err.println("Unable to get signal: " + swd.in_y + "\n" + e);
                    return;
                }
                while(true){
                    if(requests){
                        try{
                            XYData xydata = swd.getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Integer.MAX_VALUE);
                            for(int j = 0; j < swd.getWaveDataListeners().size(); j++)
                                swd.getWaveDataListeners().elementAt(j).sourceUpdated(xydata);
                        }catch(Exception exc){
                            System.err.println("Error in asynchUpdate: " + exc);
                        }
                        return;
                    }
                    synchronized(this){
                        try{
                            wait();
                        }catch(InterruptedException exc){}
                    }
                }
            }
        }
        UpdateWorker updateWorker;

        public SimpleWaveData(String in_y){
            this(in_y, null);
        }

        public SimpleWaveData(String in_y, String in_x){
            shot = mds.shot;
            this.in_x = in_x;
            this.in_y = in_y;
            long[] timing;
            try{
                try{
                    timing = mds.GetLongArray((shot < 1) ? "TIME()" : "TIME(" + shot + ")");
                }catch(IOException e){
                    error = "Time not set! Use TIME(from,upto,origin) or specify a valid shot number.";
                    throw new IOException(error);
                }
                this.from = timing[0];
                this.upto = timing[1];
                this.orig = timing.length > 2 ? timing[2] : 0L;
            }catch(Exception e){
                err.println("W7XDataProvider.SimpleWaveData: " + e);
            }
            updateWorker = new UpdateWorker(this);
            updateWorker.start();
        }

        private void getSignals() {
            long starttime = 0L;
            if(DEBUG.D) starttime = System.nanoTime();
            if(sig_y != null && sig_x != null) return;
            TimeInterval ti = getTimeInterval(from, upto);
            ReadOptions ro = ReadOptions.fetchAll();
            sig_y = W7XDataProvider.getSignal(in_y, ti, ro);
            sig_x = (in_x == null) ? sig_y.getDimensionSignal(0) : W7XDataProvider.getSignal(in_x, ti, ro);
            if(DEBUG.D) System.out.println("getSignals took " + (System.nanoTime() - starttime) / 1E9 + "s");
        }

        public Signal getYSignal() {
            return sig_y;
        }

        @Override
        public void setContinuousUpdate(boolean state) {
            continuousUpdate = state;
        }
        private Vector<WaveDataListener> waveDataListenersV = new Vector<WaveDataListener>();

        @Override
        public void addWaveDataListener(WaveDataListener listener) {
            waveDataListenersV.addElement(listener);
            updateWorker.update();
        }

        public Vector<WaveDataListener> getWaveDataListeners() {
            return waveDataListenersV;
        }

        @Override
        public void getDataAsync(double xmin, double xmax, int numPoints) {
            updateWorker.update();
        }

        public XYData getData() throws Exception {
            return getData(-1);
        }

        @Override
        public XYData getData(int numPoints) throws Exception {
            return getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, numPoints, false);
        }

        @Override
        public XYData getData(double xmin, double xmax, int numPoints) throws Exception {
            return getData(xmin, xmax, numPoints, false);
        }

        public XYData getData(double xmin, double xmax, int numPoints, boolean isXLong) throws Exception {
            if(in_x == null){
                long[] x = getX2DLong();
                if(shot < 0) return new XYData(x, getZ(), Long.MAX_VALUE, true);
                double[] xd = new double[x.length];
                for(int i = 0; i < x.length; i++)
                    xd[i] = (x[i] - orig) / 1E9;
                return new XYData(xd, getZ(), Double.MAX_VALUE, true);
            }
            if(isXLong()) return new XYData(getX2DLong(), getZ(), Double.POSITIVE_INFINITY);
            return new XYData(getX2D(), getZ(), Double.POSITIVE_INFINITY);
        }

        @Override
        public float[] getZ() {
            return W7XDataProvider.getFloat(sig_y);
        }

        @Override
        public float[] getY2D() {
            return W7XDataProvider.getFloat(sig_y.getDimensionSignal(1));
        }

        @Override
        public double[] getX2D() {
            return W7XDataProvider.getDouble(sig_x);
        }

        @Override
        public long[] getX2DLong() {
            return W7XDataProvider.getLong(sig_x);
        }

        @Override
        public boolean isXLong() {
            return orig == 0;
        }

        @Override
        public String GetTitle() throws IOException {
            return (sig_y != null) ? sig_y.getLabel() : null;
        }

        @Override
        public String GetXLabel() throws IOException {
            return (in_x != null) ? ((sig_x != null) ? sig_x.getUnit() : null) : (shot < 0 ? "time" : "s");
        }

        @Override
        public String GetYLabel() throws IOException {
            return (sig_y != null) ? ((getNumDimension() > 1) ? sig_y.getDimensionSignal(1).getUnit() : sig_y.getUnit()) : null;
        }

        @Override
        public String GetZLabel() throws IOException {
            return (sig_y != null) ? sig_y.getUnit() : null;
        }

        @Override
        public int getNumDimension() throws IOException {
            return (sig_y != null) ? sig_y.getDimensionCount() : 0;
        }
    }
}
