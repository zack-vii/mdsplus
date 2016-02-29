package w7x;

/* $Id$ */
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import javax.swing.JFrame;
import de.mpg.ipp.codac.signalaccess.IndexIterator;
import de.mpg.ipp.codac.signalaccess.Signal;
import de.mpg.ipp.codac.signalaccess.SignalAddress;
import de.mpg.ipp.codac.signalaccess.SignalAddressBuilder;
import de.mpg.ipp.codac.signalaccess.SignalReader;
import de.mpg.ipp.codac.signalaccess.SignalToolsFactory;
import de.mpg.ipp.codac.signalaccess.SignalsTreeLister;
import de.mpg.ipp.codac.signalaccess.objylike.ArchieToolsFactory;
import de.mpg.ipp.codac.signalaccess.readoptions.ReadOptions;
import de.mpg.ipp.codac.w7xtime.TimeInterval;
import jScope.ConnectionListener;
import jScope.DEBUG;
import jScope.DataProvider;
import jScope.DataServerItem;
import jScope.Descriptor;
import jScope.FrameData;
import jScope.UpdateEventListener;
import jScope.WaveData;
import jScope.WaveDataListener;
import jScope.XYData;
import mds.mdsDataProvider;

public final class w7xDataProvider implements DataProvider{
    public static class signalaccess{
        public static final signalaccess arch = new signalaccess(false);
        public static final signalaccess test = new signalaccess(true);

        private static final SignalAddress getAddress(String path) {
            if(signalaccess.isTest(path)){
                signalaccess.test.sab.newBuilder().path(path.substring(5)).build();
            }
            if(path.toUpperCase().startsWith("/ARCHIVEDB/")) path = path.substring(10);
            return signalaccess.arch.sab.newBuilder().path(path).build();
        }

        public static final byte[] getByteAt(final Signal signal, final int index, final int frameType) throws IOException {
            if(signal == null) return new byte[]{};
            final int w = signal.getDimensionSize(1);
            final int h = signal.getDimensionSize(2);
            if(frameType == FrameData.BITMAP_IMAGE_8){
                final byte[] data = new byte[w * h];
                for(int iw = 0; iw < w; iw++)
                    for(int ih = 0; ih < h; ih++)
                        signal.getValue(Byte.class, new int[]{index, iw, ih});
                return data;
            }
            final ByteArrayOutputStream dosb = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(dosb);
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
            }catch(final IOException e){}
            return null;
        }

        public static final double[] getDouble(final Signal signal) {
            if(signal == null) return new double[]{};
            final int count = signal.getSampleCount();
            final double[] data = new double[count];
            if(count == 0) return data;
            final IndexIterator iter = IndexIterator.of(signal);
            for(int i = 0; i < count; i++)
                data[i] = signal.getValue(Double.class, iter.next());
            return data;
        }

        public static final float[] getFloat(final Signal signal) {
            if(signal == null) return new float[]{};
            final int count = signal.getSampleCount();
            final float[] data = new float[count];
            if(count == 0) return data;
            final IndexIterator iter = IndexIterator.of(signal);
            for(int i = 0; i < count; i++)
                data[i] = signal.getValue(Double.class, iter.next()).floatValue();
            return data;
        }

        public static final long[] getLong(final Signal signal) {
            if(signal == null) return new long[]{};
            final int count = signal.getSampleCount();
            final long[] data = new long[count];
            if(count == 0) return data;
            final IndexIterator iter = IndexIterator.of(signal);
            for(int i = 0; i < count; i++)
                data[i] = signal.getValue(Long.class, iter.next());
            return data;
        }

        private static final SignalReader getReader(final String path) {
            if(signalaccess.isTest(path)) return signalaccess.test.stf.makeSignalReader(signalaccess.getAddress(path));
            return signalaccess.arch.stf.makeSignalReader(signalaccess.getAddress(path));
        }

        private static final Signal getSignal(final String path, final TimeInterval ti, final ReadOptions ro) {
            Signal sig;
            final SignalReader sr = w7xDataProvider.signalaccess.getReader(path);
            try{
                sig = sr.readSignal(ti, ro);
            }finally{
                sr.close();
            }
            return sig;
        }

        private static final TimeInterval getTimeInterval(final long from, final long upto) {
            return TimeInterval.ALL.withStart(from).withEnd(upto);
        }

        private static final boolean isTest(final String path) {
            return path.toUpperCase().startsWith("/TEST/");
        }
        public final SignalAddressBuilder sab;
        public final SignalToolsFactory   stf;
        public final SignalsTreeLister    stl;

        private signalaccess(final boolean test){
            if(test) this.stf = ArchieToolsFactory.remoteArchive("Test");
            else this.stf = ArchieToolsFactory.remoteArchive();
            this.sab = this.stf.makeSignalAddressBuilder(new String[0]);
            this.stl = this.stf.makeSignalsTreeLister();
        }

        public final List<SignalAddress> getList(final String path) {
            return this.getList(path, TimeInterval.ALL);
        }

        @SuppressWarnings("unchecked")
        public final List<SignalAddress> getList(final String path, final TimeInterval ti) {
            return (List<SignalAddress>)this.stl.listFor(ti, path);
        }
    }
    class SimpleFrameData implements FrameData{
        int    frameType = 0;
        long   from      = 0, upto = 0, orig = 0;
        String in_y, in_x;
        Signal sig_y, sig_x;

        public SimpleFrameData(final String in_y){
            this(in_y, null);
        }

        public SimpleFrameData(final String in_y, final String in_x){
            this(in_y, in_x, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        public SimpleFrameData(final String in_y, final String in_x, final float time_min, final float time_max){
            this.in_x = in_x;
            this.in_y = in_y;
            try{
                final long[] timing = w7xDataProvider.this.getTiming();
                this.orig = timing.length > 2 ? timing[2] : 0L;
                this.from = timing[0];
                this.upto = timing[1];
                this.getSignals();
            }catch(final IOException e){
                System.err.println(e);
            }
        }

        @Override
        public byte[] GetFrameAt(final int idx) throws IOException {
            this.getSignals();
            return w7xDataProvider.signalaccess.getByteAt(this.sig_y, idx, this.GetFrameType());
        }

        @Override
        public Dimension GetFrameDimension() throws IOException {
            return new Dimension(this.sig_y.getDimensionSize(2), this.sig_y.getDimensionSize(1));
        }

        @Override
        public float[] GetFrameTimes() throws IOException {
            this.getSignals();
            if(this.in_x == null){
                final long[] x = w7xDataProvider.signalaccess.getLong(this.sig_x);
                final float[] xd = new float[x.length];
                for(int i = 0; i < x.length; i++)
                    xd[i] = (x[i] - this.orig) / 1E9f;
                return xd;
            }
            return w7xDataProvider.signalaccess.getFloat(this.sig_x);
        }

        @Override
        public int GetFrameType() throws IOException {
            if(this.frameType == 0){
                final String type = this.sig_y.getComponentType().getSimpleName();
                if(type.equals("Byte")) this.frameType = FrameData.BITMAP_IMAGE_8;
                else if(type.equals("Short")) this.frameType = FrameData.BITMAP_IMAGE_16;
                else if(type.equals("Integer")) this.frameType = FrameData.BITMAP_IMAGE_32;
                else this.frameType = FrameData.BITMAP_IMAGE_FLOAT;
            }
            return this.frameType;
        }

        @Override
        public int GetNumFrames() throws IOException {
            return this.sig_y.getDimensionSize(0);
        }

        private void getSignals() {
            if(this.sig_y != null && this.sig_x != null) return;
            final TimeInterval ti = w7xDataProvider.signalaccess.getTimeInterval(this.from, this.upto);
            final ReadOptions ro = ReadOptions.fetchAll();
            this.sig_y = w7xDataProvider.signalaccess.getSignal(this.in_y, ti, ro);
            this.sig_x = (this.in_x == null) ? this.sig_y.getDimensionSignal(0) : w7xDataProvider.signalaccess.getSignal(this.in_x, ti, ro);
        }
    }
    class SimpleWaveData implements WaveData{
        class UpdateWorker extends Thread{
            boolean        requests = false;
            SimpleWaveData swd;

            public UpdateWorker(final SimpleWaveData swd){
                super();
                this.swd = swd;
            }

            synchronized void intUpdate() {
                this.requests = true;
                this.notify();
            }

            @Override
            public void run() {
                this.setName(this.swd.in_y);
                try{
                    this.swd.getSignals();
                }catch(final Exception e){
                    System.err.println("Unable to get signal: " + this.swd.in_y + "\n" + e);
                    return;
                }
                while(true){
                    if(this.requests){
                        try{
                            final XYData xydata = this.swd.getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Integer.MAX_VALUE);
                            for(int j = 0; j < this.swd.getWaveDataListeners().size(); j++)
                                this.swd.getWaveDataListeners().elementAt(j).sourceUpdated(xydata);
                        }catch(final Exception exc){
                            System.err.println("Error in asynchUpdate: " + exc);
                        }
                        return;
                    }
                    synchronized(this){
                        try{
                            this.wait();
                        }catch(final InterruptedException exc){}
                    }
                }
            }

            void update() {
                this.intUpdate();
            }
        }
        boolean                                continuousUpdate   = false;
        String                                 in_x, in_y;
        long                                   shot, from, upto, orig = 0;
        public Signal                          sig_x, sig_y;
        UpdateWorker                           updateWorker;
        private final Vector<WaveDataListener> waveDataListenersV = new Vector<WaveDataListener>();
        float                                  xmax, xmin;

        public SimpleWaveData(final String in_y){
            this(in_y, null);
        }

        public SimpleWaveData(final String in_y, final String in_x){
            this.shot = w7xDataProvider.this.mds.shot;
            this.in_x = in_x;
            this.in_y = in_y;
            try{
                final long[] timing = w7xDataProvider.this.getTiming();
                this.orig = timing.length > 2 ? timing[2] : 0L;
                this.from = timing[0];
                this.upto = timing[1];
                this.updateWorker = new UpdateWorker(this);
                this.updateWorker.start();
            }catch(final IOException e){
                System.err.println(e);
            }
        }

        @Override
        public void addWaveDataListener(final WaveDataListener listener) {
            this.waveDataListenersV.addElement(listener);
            this.updateWorker.update();
        }

        public XYData getData() throws Exception {
            return this.getData(-1);
        }

        @Override
        public XYData getData(final double xmin, final double xmax, final int numPoints) throws Exception {
            return this.getData(xmin, xmax, numPoints, false);
        }

        public XYData getData(final double xmin, final double xmax, final int numPoints, final boolean isXLong) throws Exception {
            if(this.in_x == null){
                final long[] x = this.getX2DLong();
                if(this.isXLong()) return new XYData(x, this.getZ(), Double.POSITIVE_INFINITY, true);
                final double[] xd = new double[x.length];
                for(int i = 0; i < x.length; i++)
                    xd[i] = (x[i] - this.orig) / 1E9;
                return new XYData(xd, this.getZ(), Double.POSITIVE_INFINITY, true, xmin, xmax);
            }
            if(this.isXLong()) return new XYData(this.getX2DLong(), this.getZ(), Double.POSITIVE_INFINITY);
            return new XYData(this.getX2D(), this.getZ(), Double.POSITIVE_INFINITY, false, xmin, xmax);
        }

        @Override
        public XYData getData(final int numPoints) throws Exception {
            return this.getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, numPoints, false);
        }

        @Override
        public void getDataAsync(final double xmin, final double xmax, final int numPoints) {
            this.updateWorker.update();
        }

        @Override
        public int getNumDimension() throws IOException {
            return (this.sig_y != null) ? this.sig_y.getDimensionCount() : 0;
        }

        private void getSignals() {
            long starttime = 0L;
            if(DEBUG.D) starttime = System.nanoTime();
            if(this.sig_y != null && this.sig_x != null) return;
            final TimeInterval ti = w7xDataProvider.signalaccess.getTimeInterval(this.from, this.upto);
            final ReadOptions ro = ReadOptions.fetchAll();
            this.sig_y = w7xDataProvider.signalaccess.getSignal(this.in_y, ti, ro);
            this.sig_x = (this.in_x == null) ? this.sig_y.getDimensionSignal(0) : w7xDataProvider.signalaccess.getSignal(this.in_x, ti, ro);
            if(DEBUG.D) System.out.println("getSignals took " + (System.nanoTime() - starttime) / 1E9 + "s for " + this.sig_y.getSampleCount() + " samples");
        }

        @Override
        public String GetTitle() throws IOException {
            return (this.sig_y != null) ? this.sig_y.getLabel() : null;
        }

        public Vector<WaveDataListener> getWaveDataListeners() {
            return this.waveDataListenersV;
        }

        @Override
        public double[] getX2D() {
            return w7xDataProvider.signalaccess.getDouble(this.sig_x);
        }

        @Override
        public long[] getX2DLong() {
            return w7xDataProvider.signalaccess.getLong(this.sig_x);
        }

        @Override
        public String GetXLabel() throws IOException {
            return (this.in_x != null) ? ((this.sig_x != null) ? this.sig_x.getUnit() : null) : (this.isXLong() ? "time" : "s");
        }

        @Override
        public float[] getY2D() {
            return w7xDataProvider.signalaccess.getFloat(this.sig_y.getDimensionSignal(1));
        }

        @Override
        public String GetYLabel() throws IOException {
            return (this.sig_y != null) ? ((this.getNumDimension() > 1) ? this.sig_y.getDimensionSignal(1).getUnit() : this.sig_y.getUnit()) : null;
        }

        public Signal getYSignal() {
            return this.sig_y;
        }

        @Override
        public float[] getZ() {
            return w7xDataProvider.signalaccess.getFloat(this.sig_y);
        }

        @Override
        public String GetZLabel() throws IOException {
            return (this.sig_y != null) ? this.sig_y.getUnit() : null;
        }

        @Override
        public boolean isXLong() {
            return this.orig == 0L;
        }

        @Override
        public void setContinuousUpdate(final boolean state) {
            this.continuousUpdate = state;
        }
    }
    public static w7xDataProvider instance;
    private static long[]         Timing = null;

    private static boolean isW7X(final String in) {
        return in.startsWith("/") || in.contains("_DATASTREAM");
    }

    public static final void setTiming() {
        w7xDataProvider.instance.mds.mds.mdsValue("TIME(0)");
    }

    public static final void setTiming(final long from, final long upto) {
        w7xDataProvider.setTiming(from, upto, 0L);
    }

    public static final void setTiming(final long from, final long upto, final long orig) {
        w7xDataProvider.Timing = new long[]{from * 1000000L, upto * 1000000L, orig * 1000000L};
        final Vector<Descriptor> args = new Vector<Descriptor>();
        args.addElement(new Descriptor(null, new long[]{w7xDataProvider.Timing[0]}));
        args.addElement(new Descriptor(null, new long[]{w7xDataProvider.Timing[1]}));
        args.addElement(new Descriptor(null, new long[]{w7xDataProvider.Timing[2]}));
        System.out.println(w7xDataProvider.instance.mds.mds.mdsValue("T2STR(TIME($,$,$))", args).strdata);
    }

    public static boolean SupportsCompression() {
        return mdsDataProvider.SupportsCompression();
    }

    public static boolean SupportsFastNetwork() {
        return mdsDataProvider.SupportsFastNetwork();
    }
    String          error;
    mdsDataProvider mds;

    public w7xDataProvider(){
        w7xDataProvider.instance = this;
        this.mds = new mdsDataProvider();
    }

    public w7xDataProvider(final String provider) throws IOException{
        w7xDataProvider.instance = this;
        this.mds = new mdsDataProvider(provider);
    }

    @Override
    public synchronized void AddConnectionListener(final ConnectionListener l) {
        this.mds.AddConnectionListener(l);
    }

    @Override
    public synchronized void AddUpdateEventListener(final UpdateEventListener l, final String event_name) throws IOException {
        this.mds.AddUpdateEventListener(l, event_name);
    }

    @Override
    public synchronized void Dispose() {
        this.mds.Dispose();
    }

    @Override
    public synchronized String ErrorString() {
        String outerror = this.mds.ErrorString();
        if(this.error == null) return outerror;
        outerror = this.error;
        this.error = null;
        return this.error;
    }

    @Override
    public Class getDefaultBrowser() {
        return w7xBrowseSignals.class;
    }

    @Override
    public synchronized float GetFloat(final String in) throws IOException {
        return this.mds.GetFloat(in);
    }

    @Override
    public FrameData GetFrameData(final String in_y, final String in_x, final float time_min, final float time_max) throws IOException {
        return w7xDataProvider.isW7X(in_y) ? new SimpleFrameData(in_y, in_x, time_min, time_max) : this.mds.GetFrameData(in_y, in_x, time_min, time_max);
    }

    @Override
    public final String GetLegendString(final String s) {
        return s;
    }

    public int[] GetNumDimensions(final String in) throws IOException {
        if(w7xDataProvider.isW7X(in)){
            System.err.println("w7xDataProvider.GetNumDimensions not implemented!");
            return new int[]{1};
        }
        return this.mds.GetNumDimensions(in);
    }

    @Override
    public long[] GetShots(final String in) throws IOException {
        if(DEBUG.M) System.out.println("GetShots(" + in + ")");
        return this.mds.GetShots(in);
    }

    @Override
    public synchronized String GetString(final String in) throws IOException {
        return this.mds.GetString(in);
    }

    public final long[] getTiming() throws IOException {
        try{
            final long shot = w7xDataProvider.this.mds.shot;
            if(shot < 1) return w7xDataProvider.this.mds.GetLongArray("TIME()");
            return w7xDataProvider.this.mds.GetLongArray("TIME(" + shot + ")");
        }catch(final Exception e){}
        if(w7xDataProvider.Timing != null) return w7xDataProvider.Timing;
        this.error = "Time not set! Use TIME(from,upto,origin) or specify a valid shot number.";
        throw new IOException(this.error);
    }

    @Override
    public WaveData GetWaveData(final String in) {
        return w7xDataProvider.isW7X(in) ? new SimpleWaveData(in) : this.mds.GetWaveData(in);
    }

    @Override
    public WaveData GetWaveData(final String in_y, final String in_x) {
        return w7xDataProvider.isW7X(in_y) ? new SimpleWaveData(in_y, in_x) : this.mds.GetWaveData(in_y, in_x);
    }

    @Override
    public int InquireCredentials(final JFrame f, final DataServerItem si) {
        return this.mds.InquireCredentials(f, si);
    }

    @Override
    public synchronized void RemoveConnectionListener(final ConnectionListener l) {
        this.mds.RemoveConnectionListener(l);
    }

    @Override
    public synchronized void RemoveUpdateEventListener(final UpdateEventListener l, final String event_name) throws IOException {
        this.mds.RemoveUpdateEventListener(l, event_name);
    }

    @Override
    public void SetArgument(final String arg) throws IOException {
        this.mds.SetArgument(arg);
    }

    public void SetCompression(final boolean state) {
        this.mds.SetCompression(state);
    }

    @Override
    public synchronized void SetEnvironment(final String in) throws IOException {
        this.mds.SetEnvironment(in);
    }

    @Override
    public boolean SupportsTunneling() {
        return this.mds.SupportsTunneling();
    }

    @Override
    public synchronized void Update(final String expt, final long shot) {
        this.mds.Update(expt, shot);
    }
}
