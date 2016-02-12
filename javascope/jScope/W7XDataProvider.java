package jScope;

/* $Id$ */
import static java.lang.System.out;
import static java.lang.System.err;
import java.util.*;
import java.io.*;
import java.awt.Dimension;
import javax.swing.JFrame;
import de.mpg.ipp.codac.signalaccess.*;
import de.mpg.ipp.codac.signalaccess.Signal;
import de.mpg.ipp.codac.signalaccess.objylike.ArchieToolsFactory;
import de.mpg.ipp.codac.signalaccess.readoptions.ReadOptions;
import de.mpg.ipp.codac.w7xtime.TimeInterval;

class W7XDataProvider implements DataProvider
{
    MdsDataProvider mds;
    String error;
    public W7XDataProvider(){mds = new MdsDataProvider();}
    public W7XDataProvider(String provider) throws IOException{mds = new MdsDataProvider(provider);}

    private static final SignalToolsFactory stf = ArchieToolsFactory.remoteArchive();
    protected void finalize()
    {
        stf.close();
    }
    private static SignalReader getReader(String path)
    {
		final SignalAddressBuilder sab = stf.makeSignalAddressBuilder(new String[0]);
		SignalAddress adr = sab.newBuilder().path(path).build();
		return stf.makeSignalReader(adr);
    }
    private static TimeInterval getTimeInterval(long from, long upto)
    {
        return TimeInterval.ALL.withStart(from).withEnd(upto);
    }
    private static Signal getSignal(String path,TimeInterval ti,ReadOptions ro)
    {
        Signal sig;
        SignalReader sr = getReader(path);
        try{
            sig = sr.readSignal(ti,ro);
        }finally{sr.close();}
        return sig;
    }
    public static long[] getLong(Signal signal)
    {
		int count = signal.getSampleCount();
		long[] data = new long[count];
		if(count==0) return data;
		IndexIterator iter = IndexIterator.of(signal);
		for (int i=0 ; i<count ; i++)
			data[i] = signal.getValue(Long.class,iter.next());
		return data;
    }
    public static float[] getFloat(Signal signal)
    {
		int count = signal.getSampleCount();
		float[] data = new float[count];
		if(count==0) return data;
		IndexIterator iter = IndexIterator.of(signal);
		for (int i=0 ; i<count ; i++)
			data[i] = signal.getValue(Double.class,iter.next()).floatValue();
		return data;
    }
    public static double[] getDouble(Signal signal)
    {
		int count = signal.getSampleCount();
		double[] data = new double[count];
		if(count==0) return data;
		IndexIterator iter = IndexIterator.of(signal);
		for (int i=0 ; i<count ; i++)
			data[i] = signal.getValue(Double.class,iter.next());
		return data;
    }
	public static byte[] getByteAt(Signal signal, int index, int frameType) throws IOException
    {
		int w=signal.getDimensionSize(1);
		int h=signal.getDimensionSize(2);
		if(frameType==FrameData.BITMAP_IMAGE_8)
		{
		    byte[] data = new byte[w*h];
			for (int iw=0 ; iw<w ; iw++)
				for (int ih=0 ; ih<h ; ih++)
					signal.getValue(Byte.class,new int[]{index,iw,ih});
			return data;
		}		    
		try (ByteArrayOutputStream dosb = new ByteArrayOutputStream()){
          try(DataOutputStream dos = new DataOutputStream(dosb)){
			if(frameType==FrameData.BITMAP_IMAGE_16)
			{
				for (int iw=0 ; iw<w ; iw++)
					for (int ih=0 ; ih<h ; ih++)
						dos.writeShort(signal.getValue(Short.class,new int[]{index,iw,ih}));
			}		    
			else if(frameType==FrameData.BITMAP_IMAGE_32)
			{
				for (int iw=0 ; iw<w ; iw++)
					for (int ih=0 ; ih<h ; ih++)
						dos.writeInt(signal.getValue(Integer.class,new int[]{index,iw,ih}));
			}		    	    
			else if(frameType==FrameData.BITMAP_IMAGE_FLOAT)
			{
				for (int iw=0 ; iw<w ; iw++)
					for (int ih=0 ; ih<h ; ih++)
						dos.writeFloat(signal.getValue(Float.class,new int[]{index,iw,ih}));
			}		    
			return dosb.toByteArray();
		  }
        }
    }
    private boolean isW7X(String in){return in.contains("_DATASTREAM");}
    public int []  GetNumDimensions(String in) {return new int[] {1};}
    public synchronized String ErrorString(){
        String outerror = mds.ErrorString();
        if(error==null)
            return outerror;
        outerror = error;
        error = null;
        return error;
    }
    public FrameData GetFrameData(String in_y, String in_x, float time_min,float time_max) throws IOException
    {
        if(isW7X(in_y))
            return new SimpleFrameData(in_y,in_x,time_min,time_max);
        else
            return mds.GetFrameData(in_y,in_x,time_min,time_max);
    }
    /** mds forwarding **/
    public boolean SupportsCompression(){return mds.SupportsCompression();}
    public void    SetCompression(boolean state){mds.SetCompression(state);}
    public int     InquireCredentials(JFrame f, DataServerItem si){return mds.InquireCredentials(f,si);}
    public boolean SupportsFastNetwork(){return mds.SupportsFastNetwork();}
    public boolean SupportsTunneling(){return mds.SupportsTunneling();}
    public void    SetArgument(String arg) throws IOException{mds.SetArgument(arg);}
    public long[]  GetShots(String in) throws IOException{return mds.GetShots(in);}
    public synchronized void AddConnectionListener(ConnectionListener l){mds.AddConnectionListener(l);}
    public synchronized void RemoveConnectionListener(ConnectionListener l){mds.RemoveConnectionListener(l);}
    public synchronized void Dispose(){mds.Dispose();}
    public synchronized void AddUpdateEventListener(UpdateEventListener l,String event_name) throws IOException{mds.AddUpdateEventListener(l,event_name);}
    public synchronized void RemoveUpdateEventListener(UpdateEventListener l,String event_name) throws IOException{mds.RemoveUpdateEventListener(l,event_name);}
    public synchronized double GetFloat(String in) throws IOException{return mds.GetFloat(in);}
    public synchronized String GetString(String in) throws IOException{return mds.GetString(in);}
    public synchronized void Update(String expt, long shot){mds.Update(expt,shot);}
    public synchronized void SetEnvironment(String in) throws IOException{mds.SetEnvironment(in);}
    /** end mds forwarding **/

    class SimpleFrameData implements FrameData
    {
        Signal sig_y, sig_x;
        String in_y,in_x;
		int frameType = 0;
        long shot,from,upto,orig = 0;
        public SimpleFrameData(String in_y){this(in_y, null);}
        public SimpleFrameData(String in_y, String in_x){this(in_y,in_x,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY);}
        public SimpleFrameData(String in_y, String in_x, float time_min, float time_max)
        {
            shot = mds.shot;
            this.in_x = in_x;
            this.in_y = in_y;
            long[] timing;
        try{
                try{
                    if(shot<1) timing = mds.GetLongArray("TIME()");
                    else       timing = mds.GetLongArray("TIME("+shot+")");
                }catch(IOException e){error = "Time not set! Use TIME(from,upto,origin) or specify a valid shot number.";throw new IOException(error);}
        this.orig = timing.length>2 ? timing[2] : 0L;
        this.from = timing[0];
        this.upto = timing[1];
        }catch(Exception e){err.println(">>> W7XDataProvider.SimpleFrameData: "+e);}
            TimeInterval ti = getTimeInterval(from,upto);
            ReadOptions ro = ReadOptions.fetchAll();
            sig_y = W7XDataProvider.getSignal(in_y,ti,ro);
            if(in_x==null)
                sig_x = sig_y.getDimensionSignal(0);
            else
                sig_x = W7XDataProvider.getSignal(in_x,ti,ro);
        }
        public int GetFrameType() throws IOException
		{	if(frameType==0)
			{
				String type = sig_y.getComponentType().getSimpleName();
				     if(type.equals("Byte"))    frameType = FrameData.BITMAP_IMAGE_8;
				else if(type.equals("Short"))   frameType = FrameData.BITMAP_IMAGE_16;
				else if(type.equals("Integer")) frameType = FrameData.BITMAP_IMAGE_32;
				else                            frameType = FrameData.BITMAP_IMAGE_FLOAT;
			}
			return frameType;
		}
        public int GetNumFrames() throws IOException
        {
           return sig_y.getDimensionSize(0);
        }
        public Dimension GetFrameDimension()throws IOException
        {
            return new Dimension(sig_y.getDimensionSize(2),sig_y.getDimensionSize(1));
        }
        public double[] GetFrameTimes() throws IOException
        {
            if(in_x==null)
            {
                long[] x = W7XDataProvider.getLong(sig_x);
                double [] xd = new double[x.length];
                for (int i=0 ; i<x.length ; i++)
                    xd[i] = (x[i]-orig)/1E9;
                return xd;
            }
            else
                return W7XDataProvider.getDouble(sig_x);
        }
        public byte[] GetFrameAt(int idx) throws IOException
        {
			return getByteAt(sig_y,idx,GetFrameType());
        }
    }
    public WaveData GetWaveData(String in)
    {
        if(isW7X(in))
        return new SimpleWaveData(in);
        else
            return mds.GetWaveData(in);
    }
    public WaveData GetWaveData(String in_y, String in_x)
    {
        if(isW7X(in_y))
        return new SimpleWaveData(in_y,in_x);
        else
            return mds.GetWaveData(in_y,in_x);
    }
    class SimpleWaveData implements WaveData
    {
        Signal sig_x, sig_y;
        String in_x, in_y;
        long shot,from,upto,orig = 0;
        float xmax, xmin;
        int n_points;
        boolean isXLong = false;
        boolean resample = false;
        boolean _jscope_set = false;
        boolean continuousUpdate = false;
        AsynchDataSource asynchSource = null;
        Vector<WaveDataListener> waveDataListenersV = new Vector<WaveDataListener>();

        public SimpleWaveData(String in_y){this(in_y, null);}
        public SimpleWaveData(String in_y, String in_x)
        {
            shot = mds.shot;
            this.in_x = in_x;
            this.in_y = in_y;
            long[] timing;
        try{
                try{
                    if(shot<1) timing = mds.GetLongArray("TIME()");
                    else       timing = mds.GetLongArray("TIME("+shot+")");
                }catch(IOException e){error = "Time not set! Use TIME(from,upto,origin) or specify a valid shot number.";throw new IOException(error);}
        this.from = timing[0];
        this.upto = timing[1];
        this.orig = timing.length>2 ? timing[2] : 0L;
        }catch(Exception e){err.println("W7XDataProvider.SimpleWaveData: "+e);}
            TimeInterval ti = getTimeInterval(from,upto);
            ReadOptions ro = ReadOptions.fetchAll();
            sig_y = W7XDataProvider.getSignal(in_y,ti,ro);
            if(in_x==null)
                sig_x = sig_y.getDimensionSignal(0);
            else
                sig_x = W7XDataProvider.getSignal(in_x,ti,ro);
        }
        public void setContinuousUpdate(boolean state){continuousUpdate = state;}
        public void addWaveDataListener(WaveDataListener listener)
        {
            waveDataListenersV.addElement(listener);
            if(asynchSource != null)
                asynchSource.addDataListener(listener);
        }
        public void getDataAsync(double lowerBound, double upperBound, int numPoints)
        {
            //updateWorker.updateInfo(lowerBound, upperBound, numPoints, waveDataListenersV, this, isXLong);
        }
        public XYData getData(int numPoints) throws Exception
        {
            return getData(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,numPoints,false);
        }
        public XYData getData(double xmin, double xmax, int numPoints) throws Exception
        {
            return getData(xmin,xmax,numPoints,false);
        }
        public XYData getData(double xmin, double xmax, int numPoints, boolean isXLong) throws Exception
        {
            if(in_x==null)
            {
				long[] x = getX2DLong();
				if(shot<0)
					return new XYData(x,getZ(),(double)Long.MAX_VALUE,true);
				else
				{
					double [] xd = new double[x.length];
					for (int i=0 ; i<x.length ; i++)
						xd[i] = (x[i]-orig)/1E9;
					return new XYData(xd,getZ(),Double.MAX_VALUE,true);
				}
            }
            if(isXLong())
                return new XYData(getX2DLong(),getZ(),(double)Long.MAX_VALUE);
            else
                return new XYData(getX2D(),getZ(),Double.MAX_VALUE);
        }
        public float[] getZ()
        {
            return W7XDataProvider.getFloat(sig_y);
        }
        public float[] getY2D()
        {
            return W7XDataProvider.getFloat(sig_y.getDimensionSignal(1));
        }
        public double[] getX2D()
        {
        return W7XDataProvider.getDouble(sig_x);
        }
        public long[] getX2DLong()
        {
            return W7XDataProvider.getLong(sig_x);
        }

        public boolean isXLong(){return orig==0;}

        public String GetTitle()   throws IOException
        {
            return sig_y.getLabel();
        }
        public String GetXLabel()  throws IOException
        {
            if(in_x!=null)
                return sig_x.getUnit();
            if(shot<0)
        return "time";
        else
        return "s";

        }
        public String GetYLabel()  throws IOException
        {
            if( getNumDimension() > 1)
                return sig_y.getDimensionSignal(1).getUnit();
            else
                return sig_y.getUnit();
        }
        public String GetZLabel()  throws IOException
        {
            return sig_y.getUnit();
        }
        public int getNumDimension() throws IOException
        {
            return sig_y.getDimensionCount();
        }
    }
}
