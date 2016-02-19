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
    Vector<EventDescriptor> listeners  = new Vector<EventDescriptor>();
    Vector<String>          eventNames = new Vector<String>();
    static class EventDescriptor{
        UpdateEventListener listener;
        String              event;
        int                 evId;

        EventDescriptor(UpdateEventListener listener, String event, int evId){
            this.listener = listener;
            this.event = event;
            this.evId = evId;
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof EventDescriptor)) return false;
            EventDescriptor evDescr = (EventDescriptor)obj;
            return listener == evDescr.getListener() && event.equals(evDescr.getEvent());
        }

        UpdateEventListener getListener() {
            return listener;
        }

        String getEvent() {
            return event;
        }

        int getEvId() {
            return evId;
        }

        @Override
        public int hashCode() {
            if(DEBUG.M) System.out.println("# hashCode() is not defined for LocalDataProvider.EventDescriptor");
            return listener.hashCode();
        }
    } // EventDescriptor

    @Override
    native public byte[] GetByteArray(String in);

    @Override
    native public String ErrorString();

    native public long[] GetLongArrayNative(String in);

    native public float[] GetFloatArrayNative(String in);

    native public double[] GetDoubleArrayNative(String in);

    @Override
    native public int[] GetIntArray(String in);

    native public void SetEnvironmentSpecific(String in, String defaultNode);

    native public void UpdateNative(String exp, long s);

    @Override
    native public String GetString(String in);

    native public double GetFloatNative(String in);

    static native boolean isSegmentedNode(String nodeName);

    static native byte[] getSegment(String nodeName, int segIdx, int segOffset);

    static native byte[] getAllFrames(String nodeName, int startIdx, int endIdx);

    static native LocalDataProviderInfo getInfo(String nodeName, boolean isSegmented); // returned: width, height, bytesPerPixel

    static native double[] getSegmentTimes(String nodeName, String timeNames, double timeMin, double timeMax);

    static native double[] getAllTimes(String nodeName, String timeNames);

    static native int[] getSegmentIdxs(String nodeName, double timeMin, double timeMax);
    class LocalFrameData implements FrameData{
        // If the frames are stored in non segmented data, all the frames are read at the same time
        // otherwise they are read when needed
        boolean  isSegmented;
        String   nodeName;
        double[] times;
        int      segIdxs[];
        int      width, height;
        byte[][] frames;
        byte[]   allFrames;
        int      pixelSize;
        int      startIdx, endIdx;

        @SuppressWarnings("fallthrough")
        private byte[] mygetAllFrames(String nodeName, int startIdx, int endIdx) throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.mygetAllFrames(\"" + nodeName + "\", " + startIdx + ", " + endIdx + ")");
            if(DEBUG.M) System.out.println(">> " + width + "x" + height + "x" + pixelSize);
            int size = width * height;
            int maxpages;
            if(pixelSize == 1){
                if(DEBUG.D) System.out.println(">> Bytes");
                byte buf[] = GetByteArray(nodeName);
                if(buf == null) throw new IOException(LocalDataProvider.this.ErrorString());
                maxpages = buf.length / size;
                if(DEBUG.D) System.out.println(">> maxpages = " + maxpages);
                if(startIdx > 0 || endIdx < maxpages) return Arrays.copyOfRange(buf, size * startIdx, size * endIdx);
                return buf;
            }
            ByteArrayOutputStream dosb = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(dosb);
            switch(pixelSize){
                case 2: // int16
                    if(DEBUG.D) System.out.println(">> Short");
                case 4: // int32
                {
                    if(DEBUG.D && pixelSize == 4) System.out.println(">> Integer");
                    int buf[] = GetIntArray(nodeName);
                    if(buf == null) throw new IOException(LocalDataProvider.this.ErrorString());
                    maxpages = buf.length / size;
                    if(DEBUG.D) System.out.println(">> maxpages = " + maxpages);
                    if(startIdx > 0 || endIdx < maxpages){
                        if(DEBUG.D) System.out.println(">> from " + size * startIdx + " to " + size * endIdx);
                        buf = Arrays.copyOfRange(buf, size * startIdx, size * endIdx);
                    }
                    if(pixelSize == 2) for(int i = 0; i < buf.length; i++)
                        dos.writeShort(0xFFFF & buf[i]);
                    else for(int i = 0; i < buf.length; i++)
                        dos.writeInt(buf[i]);
                    break;
                }
                case 8: // Double & (double)Float
                {
                    if(DEBUG.D && pixelSize == 8) System.out.println(">> Double");
                    float buf[] = GetFloatArrayNative(nodeName);
                    if(buf == null) throw new IOException(LocalDataProvider.this.ErrorString());
                    maxpages = buf.length / size;
                    if(DEBUG.D) System.out.println(">> maxpages = " + maxpages);
                    if(startIdx > 0 || endIdx < maxpages){
                        if(DEBUG.D) System.out.println(">> from " + size * startIdx + " to " + size * endIdx);
                        buf = Arrays.copyOfRange(buf, size * startIdx, size * endIdx);
                    }
                    for(int i = 0; i < buf.length; i++)
                        dos.writeFloat(buf[i]);
                    break;
                }
                default:
                    throw new IOException("Unexpected pixelSize = " + pixelSize);
            }
            dos.close();
            return dosb.toByteArray();
        }

        void configure(String _nodeName, String timeName, float timeMin, float timeMax) throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.configure(\"" + nodeName + "\", \"" + timeName + "\", " + timeMin + ", " + timeMax + ")");
            nodeName = _nodeName;
            isSegmented = isSegmentedNode(nodeName);
            if(isSegmented){
                times = getSegmentTimes(nodeName, timeName, timeMin, timeMax);
                if(times == null) throw new IOException(LocalDataProvider.this.ErrorString());
                frames = new byte[times.length][];
                segIdxs = getSegmentIdxs(nodeName, timeMin, timeMax);
                if(segIdxs == null) throw new IOException(LocalDataProvider.this.ErrorString());
            }else{
                try{
                    GetString("_jscope_frames = ( " + nodeName + " );\"\""); // Caching
                    nodeName = "_jscope_frames";
                }catch(Exception exc){
                    System.out.println("# error " + exc);
                }
            }
            LocalDataProviderInfo info = getInfo(nodeName, isSegmented);
            if(DEBUG.M) System.out.println("LocalDataProvider.getAllTimes.getInfo() info=" + info);
            if(info == null) throw new IOException(LocalDataProvider.this.ErrorString());
            width = info.dims[0];
            height = info.dims[1];
            pixelSize = info.pixelSize;
            if(DEBUG.D) System.out.println(">> pixelSize = " + pixelSize);
            if(!isSegmented){
                if(timeName == null || timeName.trim().equals("")) timeName = "DIM_OF(" + nodeName + ")";
                if(DEBUG.D) System.out.println(">> timeName = " + timeName);
                double[] allTimes = GetDoubleArrayNative(timeName);
                if(allTimes == null) throw new IOException(LocalDataProvider.this.ErrorString());
                if(DEBUG.D) System.out.println(">> allTimes.length = " + allTimes.length);
                for(startIdx = 0; startIdx < allTimes.length && allTimes[startIdx] < timeMin; startIdx++);
                for(endIdx = startIdx; endIdx < allTimes.length && allTimes[endIdx] < timeMax; endIdx++);
                if(DEBUG.D) System.out.println(">> startIdx = " + startIdx + ", endIdx = " + endIdx);
                times = new double[endIdx - startIdx];
                for(int i = 0; i < endIdx - startIdx; i++)
                    times[i] = allTimes[startIdx + i];
                allFrames = mygetAllFrames(nodeName, startIdx, endIdx);
                if(DEBUG.A) DEBUG.printByteArray(allFrames, pixelSize, width, height, times.length);
            }
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
            switch(pixelSize){
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
            if(DEBUG.D) System.out.println(">> NumFrames = " + times.length);
            return times.length;
        }

        /**
         * Return the dimension of a frame. All the frames in the sequence must have the same dimension.
         *
         * @return The frame dimension.
         * @exception java.io.IOException
         */
        @Override
        public Dimension GetFrameDimension() throws IOException {
            return new Dimension(width, height);
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
            return times;
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
        public byte[] GetFrameAt(int idx) throws IOException {
            if(DEBUG.M) System.out.println("LocalDataProvider.LocalFrameData.GetFrameAt(" + idx + ")");
            if(isSegmented){
                if(frames[idx] == null){
                    int segIdx = segIdxs[idx];
                    int segOffset = 0;
                    for(int i = idx - 1; i >= 0 && segIdxs[i] == segIdx; i--, segOffset++);
                    frames[idx] = getSegment(nodeName, segIdx, segOffset);
                }
                return frames[idx];
            }
            int img_size = pixelSize * width * height;
            byte[] outFrame = new byte[img_size];
            System.arraycopy(allFrames, idx * img_size, outFrame, 0, img_size);
            return outFrame;
        }
    } // LocalFrameData
    static{
        try{
            System.loadLibrary("JavaMds");
        }catch(UnsatisfiedLinkError e){
            // System.out.println("Load library "+e);
            javax.swing.JOptionPane.showMessageDialog(null, "Can't load data provider class LocalDataProvider : " + e, "Alert LocalDataProvider", javax.swing.JOptionPane.ERROR_MESSAGE);
            // e.printStackTrace();
        }
    }

    @Override
    public void Update(String exp, long s) {
        if(DEBUG.M) System.out.println("LocalDataProvider.Update(\"" + exp + "\", " + s + ")");
        var_idx = 0;
        UpdateNative(exp, s);
    }

    @Override
    public synchronized double GetFloat(String in) throws IOException {
        if(DEBUG.M) System.out.println("LocalDataProvider.GetFloat(\"" + in + "\")");
        error = null;
        try{
            Calendar cal = Calendar.getInstance();
            // cal.setTimeZone(TimeZone.getTimeZone("GMT+00"));
            DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm Z");
            // DateFormat df = new SimpleDateFormat("d-MMM-yyyy HH:mm");-
            Date date = df.parse(in + " GMT");
            // Date date = df.parse(in);
            cal.setTime(date);
            long javaTime = cal.getTime().getTime();
            return javaTime;
        }catch(Exception exc){} // If exception occurs this is not a date
        return GetFloatNative(in);
    }

    @Override
    public synchronized float[] GetFloatArray(String in) throws IOException {
        return GetFloatArrayNative(in);
    }

    @Override
    public synchronized double[] GetDoubleArray(String in) throws IOException {
        return GetDoubleArrayNative(in);
    }

    @Override
    public synchronized RealArray GetRealArray(String in) throws IOException {
        long longArray[] = GetLongArrayNative(in);
        if(longArray != null) return new RealArray(longArray);
        return new RealArray(GetDoubleArray(in));
    }

    @Override
    public long[] GetShots(String in) {
        if(DEBUG.M) System.out.println("LocalDataProvider.GetShots(\"" + in + "\")");
        try{
            int shots[] = GetIntArray(in.trim());
            long lshots[] = new long[shots.length];
            for(int i = 0; i < shots.length; i++)
                lshots[i] = shots[i];
            return lshots;
        }catch(UnsatisfiedLinkError e){
            System.err.println("# Error in GetIntArray: " + e);
            return null;
        }catch(Exception exc){
            System.err.println("# Error in GetIntArray: " + exc);
            return null;
        }
    }

    @Override
    public void AddUpdateEventListener(UpdateEventListener l, String event) {
        if(DEBUG.M) System.out.println("LocalDataProvider.AddUpdateEventListener(" + l + ", \"" + event + "\")");
        int evId;
        int idx;
        try{
            evId = getEventId(event);
            idx = eventNames.indexOf(event);
        }catch(Exception exc){
            idx = eventNames.size();
            eventNames.addElement(event);
            evId = registerEvent(event, idx);
        }
        listeners.addElement(new EventDescriptor(l, event, evId));
    }

    @Override
    public void RemoveUpdateEventListener(UpdateEventListener l, String event) {
        if(DEBUG.M) System.out.println("LocalDataProvider.RemoveUpdateEventListener(" + l + ", \"" + event + "\")");
        int idx = listeners.indexOf(new EventDescriptor(l, event, 0));
        if(idx != -1){
            int evId = listeners.elementAt(idx).getEvId();
            listeners.removeElementAt(idx);
            try{
                getEventId(event);
            }catch(Exception exc){
                unregisterEvent(evId);
            }
        }
    }

    @Override
    public void AddConnectionListener(ConnectionListener l) {}

    @Override
    public void RemoveConnectionListener(ConnectionListener l) {}

    // public float[] GetFrameTimes(String in_frame) {return null; }
    // public byte[] GetFrameAt(String in_frame, int frame_idx) {return null; }
    @Override
    public FrameData GetFrameData(String in_y, String in_x, float time_min, float time_max) throws IOException {
        if(DEBUG.M) System.out.println("LocalDataProvider.GetFrameData(\"" + in_y + "\", \"" + in_x + "\", " + time_min + ", " + time_max + ")");
        LocalFrameData frameData = new LocalFrameData();
        frameData.configure(in_y, in_x, time_min, time_max);
        return frameData;
    }

    @Override
    protected synchronized boolean CheckOpen(String experiment, long shot) {
        return true;
    }

    public static boolean SupportsCompression() {
        return false;
    }

    @Override
    public void SetCompression(boolean state) {}

    public static boolean SupportsContinuous() {
        return false;
    }

    public static boolean DataPending() {
        return false;
    }

    @Override
    public int InquireCredentials(JFrame f, DataServerItem server_item) {
        return DataProvider.LOGIN_OK;
    }

    public static boolean SupportsFastNetwork() {
        return true;
    }

    @Override
    public void SetArgument(String arg) {};

    @Override
    public boolean SupportsTunneling() {
        return false;
    }

    int getEventId(String event) throws Exception {
        if(DEBUG.M) System.out.println("LocalDataProvider.getEventId(\"" + event + "\")");
        for(int idx = 0; idx < listeners.size(); idx++){
            EventDescriptor evDescr = listeners.elementAt(idx);
            if(event.equals(evDescr.getEvent())) return evDescr.getEvId();
        }
        throw(new Exception());
    }

    public void fireEvent(int nameIdx) {
        if(DEBUG.M) System.out.println("LocalDataProvider.fireEvent(" + nameIdx + ")");
        String event = eventNames.elementAt(nameIdx);
        for(int idx = 0; idx < listeners.size(); idx++){
            EventDescriptor evDescr = listeners.elementAt(idx);
            if(evDescr.getEvent().equals(event)) evDescr.getListener().processUpdateEvent(new UpdateEvent(this, event));
        }
    }

    native public int registerEvent(String event, int idx);

    native public void unregisterEvent(int evId);

    void setResampleLimits(double min, double max) {
        if(DEBUG.M) System.out.println("LocalDataProvider.setResampleLimits(" + min + ", " + max + ")");
        String limitsExpr;
        if(Math.abs(min) > RESAMPLE_TRESHOLD || Math.abs(max) > RESAMPLE_TRESHOLD){
            long maxSpecific = jScopeFacade.convertToSpecificTime((long)max);
            long minSpecific = jScopeFacade.convertToSpecificTime((long)min);
            long dt = (maxSpecific - minSpecific) / MAX_PIXELS;
            limitsExpr = "JavaSetResampleLimits(" + minSpecific + "UQ," + maxSpecific + "UQ," + dt + "UQ)";
        }else{
            double dt = (max - min) / MAX_PIXELS;
            limitsExpr = "JavaSetResampleLimits(" + min + "," + max + "," + dt + ")";
        }
        GetFloatNative(limitsExpr);
    }

    static boolean supportsLargeSignals() {
        return false;
    } // Subclass LocalDataProvider will return false
}
