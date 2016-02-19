package jScope;

/* $Id$ */
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;// import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import javax.swing.JFrame;

public class LocalDataProvider extends MdsDataProvider /* implements DataProvider */
{
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
            if(DEBUG.M) System.out.println("# hashCode() is not defined for LocalDataProvider.EventDescriptor");
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
        double[] times;
        int      width, height;

        void configure(final String _nodeName, String timeName, final float timeMin, final float timeMax) throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.configure(\"" + this.nodeName + "\", \"" + timeName + "\", " + timeMin + ", " + timeMax + ")");
            this.nodeName = _nodeName;
            this.isSegmented = LocalDataProvider.isSegmentedNode(this.nodeName);
            if(this.isSegmented){
                this.times = LocalDataProvider.getSegmentTimes(this.nodeName, timeName, timeMin, timeMax);
                if(this.times == null) throw new IOException(LocalDataProvider.this.ErrorString());
                this.frames = new byte[this.times.length][];
                this.segIdxs = LocalDataProvider.getSegmentIdxs(this.nodeName, timeMin, timeMax);
                if(this.segIdxs == null) throw new IOException(LocalDataProvider.this.ErrorString());
            }else{
                try{
                    LocalDataProvider.this.GetString("_jscope_frames = ( " + this.nodeName + " );\"\""); // Caching
                    this.nodeName = "_jscope_frames";
                }catch(final Exception exc){
                    System.out.println("# error " + exc);
                }
            }
            final LocalDataProviderInfo info = LocalDataProvider.getInfo(this.nodeName, this.isSegmented);
            if(DEBUG.M) System.out.println("LocalDataProvider.getAllTimes.getInfo() info=" + info);
            if(info == null) throw new IOException(LocalDataProvider.this.ErrorString());
            this.width = info.dims[0];
            this.height = info.dims[1];
            this.pixelSize = info.pixelSize;
            if(DEBUG.D) System.out.println(">> pixelSize = " + this.pixelSize);
            if(!this.isSegmented){
                if(timeName == null || timeName.trim().equals("")) timeName = "DIM_OF(" + this.nodeName + ")";
                if(DEBUG.D) System.out.println(">> timeName = " + timeName);
                final double[] allTimes = LocalDataProvider.this.GetDoubleArrayNative(timeName);
                if(allTimes == null) throw new IOException(LocalDataProvider.this.ErrorString());
                if(DEBUG.D) System.out.println(">> allTimes.length = " + allTimes.length);
                for(this.startIdx = 0; this.startIdx < allTimes.length && allTimes[this.startIdx] < timeMin; this.startIdx++);
                for(this.endIdx = this.startIdx; this.endIdx < allTimes.length && allTimes[this.endIdx] < timeMax; this.endIdx++);
                if(DEBUG.D) System.out.println(">> startIdx = " + this.startIdx + ", endIdx = " + this.endIdx);
                this.times = new double[this.endIdx - this.startIdx];
                for(int i = 0; i < this.endIdx - this.startIdx; i++)
                    this.times[i] = allTimes[this.startIdx + i];
                this.allFrames = this.mygetAllFrames(this.nodeName, this.startIdx, this.endIdx);
                if(DEBUG.A) DEBUG.printByteArray(this.allFrames, this.pixelSize, this.width, this.height, this.times.length);
            }
        }

        /**
         * Return the frame at the given position.
         *
         * @param idx
         *            The index of the requested frame in the frame sequence.
         * @return The frame as a byte array. If the frame type is FrameData.BITMAP_IMAGE, the matrix uses row major
         *         ordering.
         * @exception java.io.IOException
         */
        @Override
        public byte[] GetFrameAt(final int idx) throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.GetFrameAt(" + idx + ")");
            if(this.isSegmented){
                if(this.frames[idx] == null){
                    final int segIdx = this.segIdxs[idx];
                    int segOffset = 0;
                    for(int i = idx - 1; i >= 0 && this.segIdxs[i] == segIdx; i--, segOffset++);
                    this.frames[idx] = LocalDataProvider.getSegment(this.nodeName, segIdx, segOffset);
                }
                return this.frames[idx];
            }
            final int img_size = this.pixelSize * this.width * this.height;
            final byte[] outFrame = new byte[img_size];
            System.arraycopy(this.allFrames, idx * img_size, outFrame, 0, img_size);
            return outFrame;
        }

        /**
         * Return the dimension of a frame. All the frames in the sequence must have the same dimension.
         *
         * @return The frame dimension.
         * @exception java.io.IOException
         */
        @Override
        public Dimension GetFrameDimension() throws IOException {
            return new Dimension(this.width, this.height);
        }

        /**
         * Return the times associated with every frame of the sequence. This information is required to correlate
         * the frame sequence with the other signals displayed by jScope.
         *
         * @return The time array for the frame sequence.
         * @exception java.io.IOException
         */
        @Override
        public double[] GetFrameTimes() throws IOException {
            return this.times;
        }

        /**
         * Returns the type of the corresponding frames. Returned frames can have either of the following types: <br>
         * -FrameData.BITMAP_IMAGE meaning that method GetFrameAt will return a byte matrix. <br>
         * -FrameData.AWT_IMAGE meaning that method GetFrameAt will return a byte vector representing the binary
         * content of a gif or jpeg file. <br>
         * -FramDeata.JAI_IMAGE meaning that method GetFrameAt will return a byte vector representing the binary
         * content of every image file supported by the JAI (Java Advanced Imaging) package. The JAI package needs not
         * to be installed unless file formats other than gif or jpeg are used.
         *
         * @return The type of the corresponding frame.
         * @exception java.io.IOException
         */
        @Override
        public int GetFrameType() throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.GetFrameType()");
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

        /**
         * Returns thenumber of frames in the sequence.
         *
         * @return The number of frames in the sequence.
         * @exception java.io.IOException
         */
        @Override
        public int GetNumFrames() throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.GetNumFrames()");
            if(DEBUG.D) System.out.println(">> NumFrames = " + this.times.length);
            return this.times.length;
        }

        @SuppressWarnings("fallthrough")
        private byte[] mygetAllFrames(final String nodeName, final int startIdx, final int endIdx) throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.mygetAllFrames(\"" + nodeName + "\", " + startIdx + ", " + endIdx + ")");
            if(DEBUG.M) System.out.println(">> " + this.width + "x" + this.height + "x" + this.pixelSize);
            final int size = this.width * this.height;
            int maxpages;
            if(this.pixelSize == 1){
                if(DEBUG.D) System.out.println(">> Bytes");
                final byte buf[] = LocalDataProvider.this.GetByteArray(nodeName);
                if(buf == null) throw new IOException(LocalDataProvider.this.ErrorString());
                maxpages = buf.length / size;
                if(DEBUG.D) System.out.println(">> maxpages = " + maxpages);
                if(startIdx > 0 || endIdx < maxpages) return Arrays.copyOfRange(buf, size * startIdx, size * endIdx);
                return buf;
            }
            final ByteArrayOutputStream dosb = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(dosb);
            switch(this.pixelSize){
                case 2: // int16
                    if(DEBUG.D) System.out.println(">> Short");
                case 4: // int32
                {
                    if(DEBUG.D && this.pixelSize == 4) System.out.println(">> Integer");
                    int buf[] = LocalDataProvider.this.GetIntArray(nodeName);
                    if(buf == null) throw new IOException(LocalDataProvider.this.ErrorString());
                    maxpages = buf.length / size;
                    if(DEBUG.D) System.out.println(">> maxpages = " + maxpages);
                    if(startIdx > 0 || endIdx < maxpages){
                        if(DEBUG.D) System.out.println(">> from " + size * startIdx + " to " + size * endIdx);
                        buf = Arrays.copyOfRange(buf, size * startIdx, size * endIdx);
                    }
                    if(this.pixelSize == 2) for(final int element : buf)
                        dos.writeShort(0xFFFF & element);
                    else for(final int element : buf)
                        dos.writeInt(element);
                    break;
                }
                case 8: // Double & (double)Float
                {
                    if(DEBUG.D && this.pixelSize == 8) System.out.println(">> Double");
                    float buf[] = LocalDataProvider.this.GetFloatArrayNative(nodeName);
                    if(buf == null) throw new IOException(LocalDataProvider.this.ErrorString());
                    maxpages = buf.length / size;
                    if(DEBUG.D) System.out.println(">> maxpages = " + maxpages);
                    if(startIdx > 0 || endIdx < maxpages){
                        if(DEBUG.D) System.out.println(">> from " + size * startIdx + " to " + size * endIdx);
                        buf = Arrays.copyOfRange(buf, size * startIdx, size * endIdx);
                    }
                    for(final float element : buf)
                        dos.writeFloat(element);
                    break;
                }
                default:
                    throw new IOException("Unexpected pixelSize = " + this.pixelSize);
            }
            dos.close();
            return dosb.toByteArray();
        }
    } // LocalFrameData
    static{
        try{
            System.loadLibrary("JavaMds");
        }catch(final UnsatisfiedLinkError e){
            // System.out.println("Load library "+e);
            javax.swing.JOptionPane.showMessageDialog(null, "Can't load data provider class LocalDataProvider : " + e, "Alert LocalDataProvider", javax.swing.JOptionPane.ERROR_MESSAGE);
            // e.printStackTrace();
        }
    }

    public static boolean DataPending() {
        return false;
    }

    static native byte[] getAllFrames(String nodeName, int startIdx, int endIdx);

    static native double[] getAllTimes(String nodeName, String timeNames);

    static native LocalDataProviderInfo getInfo(String nodeName, boolean isSegmented); // returned: width, height, bytesPerPixel

    static native byte[] getSegment(String nodeName, int segIdx, int segOffset);

    static native int[] getSegmentIdxs(String nodeName, double timeMin, double timeMax);

    static native double[] getSegmentTimes(String nodeName, String timeNames, double timeMin, double timeMax);

    static native boolean isSegmentedNode(String nodeName);

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
    } // Subclass LocalDataProvider will return false
    Vector<String>          eventNames = new Vector<String>();
    Vector<EventDescriptor> listeners  = new Vector<EventDescriptor>();

    @Override
    public void AddConnectionListener(final ConnectionListener l) {}

    @Override
    public void AddUpdateEventListener(final UpdateEventListener l, final String event) {
        if(DEBUG.M) System.out.println("LocalDataProvider.AddUpdateEventListener(" + l + ", \"" + event + "\")");
        int evId;
        int idx;
        try{
            evId = this.getEventId(event);
            idx = this.eventNames.indexOf(event);
        }catch(final Exception exc){
            idx = this.eventNames.size();
            this.eventNames.addElement(event);
            evId = this.registerEvent(event, idx);
        }
        this.listeners.addElement(new EventDescriptor(l, event, evId));
    }

    @Override
    protected synchronized boolean CheckOpen(final String experiment, final long shot) {
        return true;
    }

    @Override
    native public String ErrorString();

    public void fireEvent(final int nameIdx) {
        if(DEBUG.M) System.out.println("LocalDataProvider.fireEvent(" + nameIdx + ")");
        final String event = this.eventNames.elementAt(nameIdx);
        for(int idx = 0; idx < this.listeners.size(); idx++){
            final EventDescriptor evDescr = this.listeners.elementAt(idx);
            if(evDescr.getEvent().equals(event)) evDescr.getListener().processUpdateEvent(new UpdateEvent(this, event));
        }
    }

    @Override
    native public byte[] GetByteArray(String in);

    @Override
    public synchronized double[] GetDoubleArray(final String in) throws IOException {
        return this.GetDoubleArrayNative(in);
    }

    native public double[] GetDoubleArrayNative(String in);

    int getEventId(final String event) throws Exception {
        if(DEBUG.M) System.out.println("LocalDataProvider.getEventId(\"" + event + "\")");
        for(int idx = 0; idx < this.listeners.size(); idx++){
            final EventDescriptor evDescr = this.listeners.elementAt(idx);
            if(event.equals(evDescr.getEvent())) return evDescr.getEvId();
        }
        throw(new Exception());
    }

    @Override
    public synchronized double GetFloat(final String in) throws IOException {
        if(DEBUG.M) System.out.println("LocalDataProvider.GetFloat(\"" + in + "\")");
        this.error = null;
        try{
            final Calendar cal = Calendar.getInstance();
            // cal.setTimeZone(TimeZone.getTimeZone("GMT+00"));
            final DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm Z");
            // DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm");-
            final Date date = df.parse(in + " GMT");
            // Date date = df.parse(in);
            cal.setTime(date);
            final long javaTime = cal.getTime().getTime();
            return javaTime;
        }catch(final Exception exc){} // If exception occurs this is not a date
        return this.GetFloatNative(in);
    }

    @Override
    public synchronized float[] GetFloatArray(final String in) throws IOException {
        return this.GetFloatArrayNative(in);
    }

    native public float[] GetFloatArrayNative(String in);

    native public double GetFloatNative(String in);

    // public float[] GetFrameTimes(String in_frame) {return null; }
    // public byte[] GetFrameAt(String in_frame, int frame_idx) {return null; }
    @Override
    public FrameData GetFrameData(final String in_y, final String in_x, final float time_min, final float time_max) throws IOException {
        if(DEBUG.M) System.out.println("LocalDataProvider.GetFrameData(\"" + in_y + "\", \"" + in_x + "\", " + time_min + ", " + time_max + ")");
        final LocalFrameData frameData = new LocalFrameData();
        frameData.configure(in_y, in_x, time_min, time_max);
        return frameData;
    }

    @Override
    native public int[] GetIntArray(String in);

    native public long[] GetLongArrayNative(String in);

    @Override
    public synchronized RealArray GetRealArray(final String in) throws IOException {
        final long longArray[] = this.GetLongArrayNative(in);
        if(longArray != null) return new RealArray(longArray);
        return new RealArray(this.GetDoubleArray(in));
    }

    @Override
    public long[] GetShots(final String in) {
        if(DEBUG.M) System.out.println("LocalDataProvider.GetShots(\"" + in + "\")");
        try{
            final int shots[] = this.GetIntArray(in.trim());
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
    native public String GetString(String in);

    @Override
    public int InquireCredentials(final JFrame f, final DataServerItem server_item) {
        return DataProvider.LOGIN_OK;
    }

    native public int registerEvent(String event, int idx);

    @Override
    public void RemoveConnectionListener(final ConnectionListener l) {}

    @Override
    public void RemoveUpdateEventListener(final UpdateEventListener l, final String event) {
        if(DEBUG.M) System.out.println("LocalDataProvider.RemoveUpdateEventListener(" + l + ", \"" + event + "\")");
        final int idx = this.listeners.indexOf(new EventDescriptor(l, event, 0));
        if(idx != -1){
            final int evId = this.listeners.elementAt(idx).getEvId();
            this.listeners.removeElementAt(idx);
            try{
                this.getEventId(event);
            }catch(final Exception exc){
                this.unregisterEvent(evId);
            }
        }
    }

    @Override
    public void SetArgument(final String arg) {};

    @Override
    public void SetCompression(final boolean state) {}

    native public void SetEnvironmentSpecific(String in, String defaultNode);

    void setResampleLimits(final double min, final double max) {
        if(DEBUG.M) System.out.println("LocalDataProvider.setResampleLimits(" + min + ", " + max + ")");
        String limitsExpr;
        if(Math.abs(min) > MdsDataProvider.RESAMPLE_TRESHOLD || Math.abs(max) > MdsDataProvider.RESAMPLE_TRESHOLD){
            final long maxSpecific = jScopeFacade.convertToSpecificTime((long)max);
            final long minSpecific = jScopeFacade.convertToSpecificTime((long)min);
            final long dt = (maxSpecific - minSpecific) / MdsDataProvider.MAX_PIXELS;
            limitsExpr = "JavaSetResampleLimits(" + minSpecific + "UQ," + maxSpecific + "UQ," + dt + "UQ)";
        }else{
            final double dt = (max - min) / MdsDataProvider.MAX_PIXELS;
            limitsExpr = "JavaSetResampleLimits(" + min + "," + max + "," + dt + ")";
        }
        this.GetFloatNative(limitsExpr);
    }

    @Override
    public boolean SupportsTunneling() {
        return false;
    }

    native public void unregisterEvent(int evId);

    @Override
    public void Update(final String exp, final long s) {
        if(DEBUG.M) System.out.println("LocalDataProvider.Update(\"" + exp + "\", " + s + ")");
        MdsDataProvider.var_idx = 0;
        this.UpdateNative(exp, s);
    }

    native public void UpdateNative(String exp, long s);
}
