package w7x;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import de.mpg.ipp.codac.signalaccess.IndexIterator;
import de.mpg.ipp.codac.signalaccess.Signal;
import de.mpg.ipp.codac.signalaccess.SignalReader;
import de.mpg.ipp.codac.signalaccess.readoptions.ReadOptions;
import de.mpg.ipp.codac.w7xtime.TimeInterval;
import debug.DEBUG;
import jscope.data.DataServerItem;
import jscope.data.framedata.FrameData;
import jscope.data.wavedata.WaveData;
import jscope.data.wavedata.WaveDataListener;
import jscope.data.wavedata.XYData;
import mds.MdsDataProvider;
import mds.MdsException;
import mds.data.descriptor.Descriptor;
import mds.data.descriptor_a.Int64Array;
import mds.data.descriptor_a.NUMBERArray;
import mds.data.descriptor_a.Uint64Array;
import mds.data.descriptor_s.Int64;
import mds.data.descriptor_s.Path;

public final class W7XDataProvider extends MdsDataProvider{
	public static class JsonReader{
		public static JsonReader fromURL(final URL url, final long orig, final float y_scale, final float y_offset) throws IOException {
			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/json");
			return new JsonReader(connection.getInputStream(), orig, y_scale, y_offset);
		}

		private static int getInteger(final BufferedInputStream is) {
			final StringBuilder sb = new StringBuilder(32);
			try{
				char c;
				for(c = (char)is.read(); c != '-' && c < '0' || c > '9'; c = (char)is.read()){/*scan [0-9]*/}
				sb.append(c);
				for(c = (char)is.read(); c >= '0' && c <= '9'; c = (char)is.read())
					sb.append(c);
			}catch(final IOException e){/**/}
			return Integer.parseInt(sb.toString());
		}

		private static String getKey(final BufferedInputStream is) {
			final StringBuilder sb = new StringBuilder(32);
			try{
				while(is.available() > 0 && is.read() != '\"'){/*scan for "*/}
				if(is.available() > 0){
					for(;;){
						final char c = (char)is.read();
						if(c != '\"') sb.append(c);
						else break;
					}
					while(is.read() != ':'){/*scan for "*/}
				}
				return sb.toString();
			}catch(final IOException e){
				e.printStackTrace();
				return null;
			}
		}

		private static String getString(final BufferedInputStream is) {
			final StringBuilder sb = new StringBuilder(1024);
			try{
				while((char)is.read() != '\"'){/*scan for "*/}
				for(char c = (char)is.read();; c = (char)is.read())
					if(c == '\"'){
						final int len = sb.length();
						if(len > 1 && sb.charAt(len - 1) == '\\') sb.setCharAt(sb.length(), '\"');
						else break;
					}else sb.append(c);
			}catch(final IOException e){/**/}
			return sb.toString();
		}
		public long[]	dimensions	= null;
		public float[]	values		= null;
		public String	label		= null;
		public String	unit		= null;
		public int		sampleCount	= 0;

		public JsonReader(final InputStream inputstream, final long orig, final float y_scale, final float y_offset) throws IOException{
			final BufferedInputStream bis = new BufferedInputStream(inputstream);
			for(;;){
				final String key = JsonReader.getKey(bis);
				if(key == null || key.isEmpty()) break;
				else if(key.equals("values")) this.values = this.getFloatArray(bis, y_scale, y_offset);
				else if(key.equals("dimensions")) this.dimensions = this.getLongArray(bis, -orig);
				else if(key.equals("label")) this.label = JsonReader.getString(bis);
				else if(key.equals("unit")) this.unit = JsonReader.getString(bis);
				else if(key.equals("sampleCount")) this.sampleCount = JsonReader.getInteger(bis);
				else{
					char c = (char)bis.read();
					if(c == '\"') for(char n = (char)bis.read();; n = (char)bis.read()){
						if(n == '\"' && c != '\\') break;
						c = n;
					}
				}
			}
		}

		private float[] getFloatArray(final BufferedInputStream is, final float scale, final float offset) throws IOException {
			final float[] val = new float[this.sampleCount];
			while((char)is.read() != '['){/**/}
			for(int i = 0; i < this.sampleCount; i++){
				final StringBuilder sb = new StringBuilder(32);
				for(char c = (char)is.read(); c != ',' && c != ']'; c = (char)is.read())
					sb.append(c);
				val[i] = Float.parseFloat(sb.toString());
			}
			return val;
		}

		private long[] getLongArray(final BufferedInputStream is, final long offset) throws IOException {
			final long[] val = new long[this.sampleCount];
			while((char)is.read() != '['){/**/}
			for(int i = 0; i < this.sampleCount; i++){
				final StringBuilder sb = new StringBuilder(32);
				for(char c = (char)is.read(); c != ',' && c != ']'; c = (char)is.read())
					sb.append(c);
				val[i] = Long.parseLong(sb.toString().trim()) + offset;
			}
			return val;
		}
	}
	private class SimpleData{
		protected final long	from, upto, orig;
		protected final String	in_x, in_y;
		protected final String	xstream, ystream;
		protected String		y_label;
		protected int			num_dims	= 1;
		protected String		x_label;
		protected String		y_units;
		protected String		x_units;
		protected final float	y_scale;
		protected final float	y_offset;
		protected final float	x_scale;
		protected final float	x_offset;

		protected SimpleData(final String in_y, final String in_x){
			this.in_x = in_x;
			this.in_y = in_y;
			{// y
				final Matcher match = W7XDataProvider.pattern.matcher(in_y);
				match.find();
				this.ystream = match.group(1).trim();
				this.y_scale = match.group(3) != null && match.group(3).length() > 0 ? Float.valueOf(match.group(3)).floatValue() : 1;
				this.y_offset = match.group(6) != null && match.group(6).length() > 0 ? Float.valueOf(match.group(6)).floatValue() : 0;
			}
			if(this.in_x == null){
				this.xstream = "";
				this.x_scale = 1;
				this.x_offset = 0;
			}else{
				final Matcher match = W7XDataProvider.pattern.matcher(in_x);
				match.find();
				this.xstream = match.group(1).trim();
				this.x_scale = match.group(3) != null && match.group(3).length() > 0 ? Float.valueOf(match.group(3)).floatValue() : 1;
				this.x_offset = match.group(6) != null && match.group(6).length() > 0 ? Float.valueOf(match.group(6)).floatValue() : 0;
			}
			final long xoff = (long)(this.x_offset * 1e9);
			if(W7XDataProvider.this.timing == null){
				this.orig = 0;
				this.from = 0;
				this.upto = 0;
			}else{
				this.orig = W7XDataProvider.this.timing.length > 2 ? W7XDataProvider.this.timing[2] - xoff : -xoff;
				this.from = W7XDataProvider.this.timing[0] - xoff;
				this.upto = W7XDataProvider.this.timing[1] - xoff;
			}
		}
	}
	private final class SimpleFrameData extends SimpleData implements FrameData{
		private final class UpdateWorker extends Thread{
			private final int prev_abort;

			public UpdateWorker(){
				super();
				this.prev_abort = MdsDataProvider.abort;
			}

			@Override
			public final synchronized void run() {
				try{
					this.setName(SimpleFrameData.this.in_y);
					@SuppressWarnings("resource")
					final SignalReader sr = W7XSignalAccess.getReader(SimpleFrameData.this.ystream);
					try{
						while(this.prev_abort == MdsDataProvider.abort){
							int idx = -1;
							synchronized(SimpleFrameData.this.frameQueue){
								if(!SimpleFrameData.this.frameQueue.isEmpty()) //
								    idx = SimpleFrameData.this.frameQueue.remove().intValue();
							}
							if(idx < 0) try{
								this.wait(1000);
								continue;
							}catch(final InterruptedException exc){
								break;
							}
							SimpleFrameData.this.readSignal(sr, idx);
						}
					}finally{
						sr.close();
					}
				}catch(final Exception exc){
					if(DEBUG.E){
						final Date d = new Date();
						System.err.println(d + " Error in UpdateWorker.run: " + exc);
						exc.printStackTrace();
					}
				}finally{
					SimpleFrameData.this.update_worker = null;
					W7XDataProvider.this.threads.remove(this);
					synchronized(SimpleFrameData.this.sig_y){
						SimpleFrameData.this.sig_y.notifyAll();
					}
				}
			}
		}
		private final PriorityQueue<Integer>	frameQueue;
		int										frameType	= FrameData.UNDEFINED;
		private Signal							sig_x;
		private final List<Signal>				sig_y;
		private final TimeInterval				TI;
		private final List<TimeInterval>		todoList;
		private UpdateWorker					update_worker;

		public SimpleFrameData(final String in_y, final String in_x, final float time_min, final float time_max){
			super(in_y, in_x);
			this.TI = W7XSignalAccess.getTimeInterval(this.from, this.upto);
			@SuppressWarnings("resource")
			final SignalReader sr_y = W7XSignalAccess.getReader(this.ystream);
			try{
				this.todoList = sr_y.availableIntervals(this.TI);
				this.sig_y = Arrays.asList(new Signal[this.todoList.size()]);
			}finally{
				sr_y.close();
			}
			this.frameQueue = new PriorityQueue<Integer>();
			this.requestFrame(0);
			this.update_worker = new UpdateWorker();
			W7XDataProvider.this.threads.add(this.update_worker);
			this.update_worker.start();
		}

		@Override
		public final byte[] getFrameAt(final int idx) throws IOException {
			this.getSignal(idx);
			return this.getByteAt(this.sig_y.get(idx), 0, this.getFrameType());
		}

		@Override
		public final Dimension getFrameDimension() throws IOException {
			final Signal sig = this.getSignal(0);
			return new Dimension(sig.getDimensionSize(2), sig.getDimensionSize(1));
		}

		@Override
		public final double[] getFrameTimes() throws IOException {
			if(this.in_x == null){
				final long[] x = new long[this.todoList.size()];
				final double[] xd = new double[this.todoList.size()];
				for(int i = 0; i < x.length; i++){
					x[i] = this.todoList.get(i).upto();
					xd[i] = (x[i] - this.orig) / 1E9f;
				}
				return xd;
			}
			return W7XSignalAccess.getDouble(this.sig_x, this.x_scale, this.x_scale);
		}

		@Override
		public final int getFrameType() throws IOException {
			if(this.sig_y == null) return FrameData.UNKNOWN;
			if(this.frameType == FrameData.UNDEFINED){
				final Signal sig = this.getSignal(0);
				if(sig == null) return FrameData.UNKNOWN;
				final Class<?> type = sig.getComponentType();
				if(type.equals(byte.class)) this.frameType = FrameData.BITMAP_IMAGE_8;
				else if(type.equals(short.class)) this.frameType = FrameData.BITMAP_IMAGE_16;
				else if(type.equals(int.class)) this.frameType = FrameData.BITMAP_IMAGE_32;
				else this.frameType = FrameData.BITMAP_IMAGE_FLOAT;
			}
			return this.frameType;
		}

		@Override
		public final int getNumFrames() throws IOException {
			return this.todoList.size();
		}

		public final void requestFrame(final int idx) {
			synchronized(this.sig_y){
				if(this.sig_y.get(idx) == null) synchronized(this.frameQueue){
					final Integer Idx = Integer.valueOf(idx);
					this.frameQueue.remove(Idx);
					this.frameQueue.add(Idx);
					this.frameQueue.notifyAll();
				}
			}
		}

		private final byte[] getByteAt(final Signal signal, final int index, final int frameType_in) {
			if(signal == null) return new byte[]{};
			final int w = signal.getDimensionSize(1);
			final int h = signal.getDimensionSize(2);
			if(frameType_in == FrameData.BITMAP_IMAGE_8){
				final byte[] data = new byte[w * h];
				for(int iw = 0; iw < w; iw++)
					for(int ih = 0; ih < h; ih++)
						signal.getValue(Byte.class, new int[]{index, iw, ih});
				return data;
			}
			final ByteArrayOutputStream dosb = new ByteArrayOutputStream();
			@SuppressWarnings("resource")
			final DataOutputStream dos = new DataOutputStream(dosb);
			try{
				if(frameType_in == FrameData.BITMAP_IMAGE_16) for(int iw = 0; iw < w; iw++)
					for(int ih = 0; ih < h; ih++)
						dos.writeShort(signal.getValue(Integer.class, new int[]{index, iw, ih}).intValue());
				else if(frameType_in == FrameData.BITMAP_IMAGE_32) for(int iw = 0; iw < w; iw++)
					for(int ih = 0; ih < h; ih++)
						dos.writeInt(signal.getValue(Integer.class, new int[]{index, iw, ih}).intValue());
				else if(frameType_in == FrameData.BITMAP_IMAGE_FLOAT) for(int iw = 0; iw < w; iw++)
					for(int ih = 0; ih < h; ih++)
						dos.writeFloat(signal.getValue(Float.class, new int[]{index, iw, ih}).intValue());
				dos.close();
				return dosb.toByteArray();
			}catch(final IOException e){/**/}
			return null;
		}

		private final Signal getSignal(final int idx) {
			if(this.sig_x == null && this.xstream != null && !this.xstream.isEmpty())//
			    this.sig_x = W7XSignalAccess.getSignal(this.xstream, SimpleFrameData.this.from, SimpleFrameData.this.upto);
			if(idx >= this.todoList.size()) return null;
			this.requestFrame(idx);
			if(this.update_worker != null){
				Signal sig;
				synchronized(this.sig_y){
					if((sig = this.sig_y.get(idx)) == null) try{
						this.sig_y.wait();
						sig = this.sig_y.get(idx);
					}catch(final InterruptedException e){
						return null;
					}
				}
				return sig;
			}
			@SuppressWarnings("resource")
			final SignalReader sr = W7XSignalAccess.getReader(this.ystream);
			try{
				return this.readSignal(sr, idx);
			}finally{
				sr.close();
			}
		}

		private final Signal readSignal(final SignalReader sr, final int idx) {
			final TimeInterval ti = this.todoList.get(idx);
			final Signal sig = sr.readSignal(ti, ReadOptions.fetchAll());
			synchronized(this.sig_y){
				this.sig_y.set(idx, sig);
				this.sig_y.notifyAll();
			}
			return sig;
		}
	}
	private final class SimpleWaveData extends SimpleData implements WaveData{
		private final class UpdateWorker extends Thread{
			private final class UpdateDescriptor{
				double	updateLowerBound;
				int		updatePoints;
				double	updateUpperBound;

				UpdateDescriptor(final double updateLowerBound, final double updateUpperBound, final int updatePoints){
					this.updateLowerBound = updateLowerBound;
					this.updateUpperBound = updateUpperBound;
					this.updatePoints = updatePoints;
				}
			}
			private final int			prev_abort;
			Vector<UpdateDescriptor>	requestsV	= new Vector<UpdateDescriptor>();

			public UpdateWorker(){
				this.prev_abort = MdsDataProvider.abort;
			}

			@Override
			public final synchronized void run() {
				try{
					this.setName(SimpleWaveData.this.in_y);
					while(this.prev_abort == MdsDataProvider.abort){
						UpdateDescriptor currUpdate = null;
						synchronized(this.requestsV){
							if(this.requestsV.size() > 0) currUpdate = this.requestsV.remove(0);
						}
						if(currUpdate == null) try{
							this.wait(1000);
							continue;
						}catch(final InterruptedException exc){
							break;
						}
						Object currData;
						try{
							currData = SimpleWaveData.this.getData(currUpdate.updateLowerBound, currUpdate.updateUpperBound, currUpdate.updatePoints);
						}catch(final Exception exc){
							currData = exc;
							if(DEBUG.E) System.err.println(new Date() + " Error in asynchUpdate: " + exc);
						}
						synchronized(SimpleWaveData.this.waveDataListenersV){
							for(final WaveDataListener wdl : SimpleWaveData.this.waveDataListenersV)
								wdl.dataRegionUpdated(currData, true);
						}
						return; // for now there is nothing left to do
					}
				}finally{
					SimpleWaveData.this.update_worker = null;
					synchronized(W7XDataProvider.this.threads){
						W7XDataProvider.this.threads.remove(this);
					}
				}
			}

			synchronized private final void updateInfo(final double updateLowerBound, final double updateUpperBound, final int updatePoints) {
				synchronized(this.requestsV){
					for(int i = this.requestsV.size(); i-- > 0;){
						final UpdateDescriptor ud = this.requestsV.get(i);
						if(ud.updateLowerBound >= updateLowerBound //
						        && ud.updateUpperBound <= updateUpperBound //
						        && ud.updatePoints <= updatePoints) this.requestsV.remove(i);
					}
					this.requestsV.add(new UpdateDescriptor(updateLowerBound, updateUpperBound, updatePoints));
				}
				this.notify();
			}
		}
		private UpdateWorker					update_worker;
		private final Vector<WaveDataListener>	waveDataListenersV	= new Vector<WaveDataListener>();

		public SimpleWaveData(final String in_y){
			this(in_y, null);
		}

		public SimpleWaveData(final String in_y, final String in_x){
			super(in_y, in_x);
			this.update_worker = new UpdateWorker();
			synchronized(W7XDataProvider.this.threads){
				W7XDataProvider.this.threads.add(this.update_worker);
			}
			this.update_worker.start();
		}

		@Override
		public void addWaveDataListener(final WaveDataListener listener) {
			synchronized(this.waveDataListenersV){
				this.waveDataListenersV.add(listener);
			}
		}

		@Override
		public final XYData getData(final double xmin, final double xmax, final int numPoints) throws IOException {
			return this.getData(xmin, xmax, numPoints, false);
		}

		public final XYData getData(final double xmin, final double xmax, final int numPoints, final boolean isLong) {
			if(!this.xstream.isEmpty()) return this.getData_xy(xmin, xmax, numPoints);
			if(W7XDataProvider.this.use_json) return this.getData_y_json(xmin, xmax, numPoints, isLong);
			return this.getData_y(xmin, xmax, numPoints, isLong);
		}

		@Override
		public final XYData getData(final int numPoints) throws IOException {
			return this.getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, numPoints, true);
		}

		@Override
		public final void getDataAsync(final double lowerBound, final double upperBound, final int numPoints) {
			if(DEBUG.M) System.out.println("W7XDataProvider.SimpleWaveData.getDataAsync(" + lowerBound + ", " + upperBound + ", " + numPoints + ")");
			synchronized(this.waveDataListenersV){
				for(final WaveDataListener wdl : this.waveDataListenersV)
					wdl.updatePending();
			}
			this.update_worker.updateInfo(lowerBound, upperBound, numPoints);
		}

		@Override
		public final int getNumDimension() throws IOException {
			return this.num_dims;
		}

		public final long[] getTime(final Signal signal) {
			if(signal == null) return new long[]{};
			return this.getTime(signal, LongBuffer.allocate(signal.getSampleCount())).array();
		}

		public final LongBuffer getTime(final Signal signal, final LongBuffer buf) {
			if(signal == null) return buf;
			for(final int[] i : IndexIterator.of(signal))
				buf.put((signal.getValue(Long.class, i).longValue() - this.orig));
			return buf;
		}

		@Override
		public final String getTitle() throws IOException {
			return this.y_label;
		}

		@Override
		public double[] getX2D() {
			return null;
		}

		@Override
		public final long[] getX2DLong() {
			return null;
		}

		@Override
		public final String getXLabel() throws IOException {
			return this.x_label == null ? this.x_units : this.x_units == null ? this.x_label : this.x_label + "[" + this.x_units + "]";
		}

		@Override
		public final float[] getY2D() {
			return null;
		}

		@Override
		public final String getYLabel() throws IOException {
			return this.y_units;
		}

		@Override
		public final float[] getZ() {
			return null;
		}

		@Override
		public final String getZLabel() throws IOException {
			return this.y_units;
		}

		@Override
		public final boolean isXLong() {
			return this.orig == 0L;
		}

		private final XYData getData_xy(final double xmin, final double xmax, final int numPoints) {
			final long _from = Double.isFinite(xmin) ? (long)(xmin * 1e9) + this.orig : this.from;
			final long _upto = Double.isFinite(xmax) ? (long)(xmax * 1e9) + this.orig : this.upto;
			final ReadOptions ro = ReadOptions.fetchAll();
			final TimeInterval tti = W7XSignalAccess.getTimeInterval(_from, _upto);
			final double[] x;
			{
				int len = 0;
				final LinkedList<double[]> L = new LinkedList<double[]>();
				@SuppressWarnings("resource")
				final SignalReader sr = W7XSignalAccess.getReader(SimpleWaveData.this.xstream);
				long starttime;
				if(DEBUG.D) starttime = System.nanoTime();
				try{
					boolean readmeta = false;
					for(final TimeInterval ti : sr.availableIntervals(tti)){
						final Signal sig = sr.readSignal(ti, ro);
						L.add(W7XSignalAccess.getDouble(sig, SimpleWaveData.this.x_scale, SimpleWaveData.this.x_offset));
						len += sig.getSampleCount();
						if(!readmeta && len > 0){
							readmeta = true;
							SimpleWaveData.this.x_label = sig.getLabel();
							SimpleWaveData.this.x_units = sig.getUnit();
						}
					}
				}finally{
					if(DEBUG.D) System.out.println("getSignals X took " + (System.nanoTime() - starttime) / 1E9 + "s for " + len + " samples");
					sr.close();
				}
				if(len == 0) return null;
				x = new double[len];
				final DoubleBuffer B = DoubleBuffer.wrap(x);
				while(!L.isEmpty())
					B.put(L.removeFirst());
			}
			final float[] y;
			{
				int len = 0;
				final LinkedList<float[]> L = new LinkedList<float[]>();
				@SuppressWarnings("resource")
				final SignalReader sr = W7XSignalAccess.getReader(SimpleWaveData.this.ystream);
				long starttime;
				if(DEBUG.D) starttime = System.nanoTime();
				try{
					boolean readmeta = false;
					for(final TimeInterval ti : sr.availableIntervals(tti)){
						final Signal sig = sr.readSignal(ti, ro);
						L.add(W7XSignalAccess.getFloat(sig, SimpleWaveData.this.y_scale, SimpleWaveData.this.y_offset));
						len += sig.getSampleCount();
						if(!readmeta && len > 0){
							readmeta = true;
							SimpleWaveData.this.y_label = sig.getLabel();
							SimpleWaveData.this.y_units = sig.getUnit();
							SimpleWaveData.this.num_dims = sig.getDimensionCount();
						}
					}
				}finally{
					if(DEBUG.D) System.out.println("getSignals Y took " + (System.nanoTime() - starttime) / 1E9 + "s for " + len + " samples");
					sr.close();
				}
				if(len == 0) return null;
				y = new float[len];
				final FloatBuffer B = FloatBuffer.wrap(y);
				while(!L.isEmpty())
					B.put(L.removeFirst());
			}
			return new XYData(x, y, Double.POSITIVE_INFINITY, false);
		}

		private final XYData getData_y(final double xmin, final double xmax, final int numPoints, final boolean isLong) {
			final long _from = Double.isFinite(xmin) ? (long)(xmin * 1e9) + this.orig : this.from;
			final long _upto = Double.isFinite(xmax) ? (long)(xmax * 1e9) + this.orig : this.upto;
			final TimeInterval tti = W7XSignalAccess.getTimeInterval(_from, _upto);
			final ReadOptions ro = ReadOptions.fetchAll();
			Signal sig_x = null, sig_y = null;
			final LinkedList<long[]> Lx = new LinkedList<long[]>();
			final LinkedList<float[]> Ly = new LinkedList<float[]>();
			int len = 0;
			boolean readmeta = false;
			@SuppressWarnings("resource")
			final SignalReader sr = W7XSignalAccess.getReader(this.ystream);
			long starttime;
			if(DEBUG.D) starttime = System.nanoTime();
			try{
				for(final TimeInterval ti : sr.availableIntervals(tti)){
					sig_y = sr.readSignal(ti, ro);
					if(sig_y.isEmpty()) continue;
					sig_x = sig_y.getDimensionSignal(0);
					Ly.add(W7XSignalAccess.getFloat(sig_y, this.y_scale, this.y_offset));
					Lx.add(this.getTime(sig_x));
					len = len + sig_y.getSampleCount();
					if(!readmeta && len > 0){
						readmeta = true;
						this.y_label = sig_y.getLabel();
						this.y_units = sig_y.getUnit();
						this.num_dims = sig_y.getDimensionCount();
						this.x_label = sig_x.getLabel();
						this.x_units = sig_x.getUnit();
					}
				}
			}finally{
				if(DEBUG.D) System.out.println("getSignals took " + (System.nanoTime() - starttime) / 1E9 + "s for " + len + " samples");
				sr.close();
			}
			final long[] x = new long[len];
			final LongBuffer Bx = LongBuffer.wrap(x);
			while(!Lx.isEmpty())
				Bx.put(Lx.removeFirst());
			final float[] y = new float[len];
			final FloatBuffer By = FloatBuffer.wrap(y);
			while(!Ly.isEmpty())
				By.put(Ly.removeFirst());
			return new XYData(x, y, Double.POSITIVE_INFINITY, true, xmin, xmax);
		}

		private final XYData getData_y_json(final double xmin, final double xmax, final int numPoints, final boolean isLong) {
			final long _from = Double.isFinite(xmin) ? (long)(xmin * 1e9) + this.orig : this.from;
			final long _upto = Double.isFinite(xmax) ? (long)(xmax * 1e9) + this.orig : this.upto;
			final StringBuilder path = new StringBuilder(1024);
			if(!this.in_y.startsWith("/")) path.append("/ArchiveDB/");
			path.append(this.in_y).append("/_signal.json");
			final StringBuilder args = new StringBuilder(1024);
			args.append("from=").append(_from);
			args.append("&upto=").append(_upto);
			args.append("&reduction=minmax&nSamples=").append(numPoints * 10); // TODO: remove safety factor 10 once web-api minmax is working
			URL url;
			final JsonReader jr;
			try{
				url = new URI("http", "archive-webapi.ipp-hgw.mpg.de", path.toString(), args.toString(), null).toURL();
				jr = JsonReader.fromURL(url, this.orig, this.y_scale, this.y_offset);
			}catch(final Exception e){
				e.printStackTrace();
				return null;
			}
			if(jr.dimensions == null) return null;
			final double res = (1e9 * jr.dimensions.length) / (jr.dimensions[jr.dimensions.length - 1] - jr.dimensions[0]);
			this.y_label = jr.label;
			this.y_units = jr.unit;
			this.num_dims = 1;
			this.x_label = "time";
			this.x_units = "s";
			return new XYData(jr.dimensions, jr.values, res, true, xmin, xmax);
		}

		@SuppressWarnings("unused") // decimation is client side still, no speedup
		private final XYData getData_y_minmax(final double xmin, final double xmax, final int numPoints, final boolean isLong) {
			try{
				long starttime;
				final long _from = Double.isFinite(xmin) ? (long)(xmin * 1e9) + this.orig : this.from;
				final long _upto = Double.isFinite(xmax) ? (long)(xmax * 1e9) + this.orig : this.upto;
				final TimeInterval ti = W7XSignalAccess.getTimeInterval(_from, _upto);
				final ReadOptions ro_mm = ReadOptions.minMax(numPoints);
				if(DEBUG.D) starttime = System.nanoTime();
				final Signal sig_y = W7XSignalAccess.getSignal(this.ystream, ti, ro_mm);
				if(sig_y.isEmpty()) return null;
				if(DEBUG.D) System.out.println("getSignals took " + (System.nanoTime() - starttime) / 1E9 + "s for " + sig_y.getSampleCount() + " samples");
				final Signal sig_x = sig_y.getDimensionSignal(0);
				this.y_label = sig_y.getLabel();
				this.y_units = sig_y.getUnit();
				this.num_dims = sig_y.getDimensionCount();
				this.x_label = sig_x.getLabel();
				this.x_units = sig_x.getUnit();
				final float[] y = W7XSignalAccess.getFloat(sig_y, this.y_scale, this.y_offset);
				final long[] x = this.getTime(sig_x);
				final double res = 1.9 * x.length / (x[x.length - 1] - x[0]);// res> 0 ? res : Double.POSITIVE_INFINITY
				return new XYData(x, y, res, true, xmin, xmax);
			}catch(final Exception exc){
				exc.printStackTrace();
				return null;
			}
		}
	}
	final static Pattern			pattern	= Pattern.compile("([^\\+\\*]+|)\\s*(\\*\\s*([-\\.0-9eE]+)|())\\s*(\\+\\s*([-\\.0-9eE]+)|())");
	public static final DateFormat	format	= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	// public static W7XDataProvider instance;

	private static boolean isW7X(final String expr) {
		return expr.startsWith("/");
	}
	public boolean					use_json	= false;
	private final Vector<Thread>	threads		= new Vector<Thread>();
	private long[]					timing		= null;
	private String					shot_cache_in;
	private int[]					shot_cache_out;

	@Override
	public final boolean checkProvider() {
		if(!super.checkProvider()) System.err.println(super.errorString());
		return W7XSignalAccess.isConnected();
	}

	@Override
	public final Class<W7XSignalBrowser> getDefaultBrowser() {
		return W7XSignalBrowser.class;
	}

	@Override
	public final FrameData getFrameData(final String in_y, final String in_x, final float time_min, final float time_max) throws IOException {
		if(W7XDataProvider.isW7X(in_y)) return new SimpleFrameData(in_y, in_x, time_min, time_max);
		if(this.mds != null) return super.getFrameData(in_y, in_x, time_min, time_max);
		return null;
	}

	@Override
	public Vector<LabeledProperty> getLabeledProperties() {
		final Vector<LabeledProperty> props = super.getLabeledProperties();
		props.add(new LabeledCheckBox("use_json", this.use_json));
		return props;
	}

	@Override
	public final int[] getShots(final String in) throws IOException {
		if(DEBUG.M) System.out.println("getShots(" + in + ")");
		if(in == null) return new int[]{-1};
		if(this.shot_cache_in != null && this.shot_cache_in == in) return this.shot_cache_out;
		if(in.startsWith("XP:")){
			final TimeInterval ti = W7XSignalAccess.getTimeInterval(in);
			if(ti.isNull()) return this.shot_cache_out = null;
			this.timing = new long[]{ti.from(), ti.upto(), ti.from()};
			return this.shot_cache_out = new int[]{-1};
		}
		Descriptor<?> tmp = this.mds.getDescriptor("DATA(" + in + ")");
		if(tmp instanceof Int64Array || tmp instanceof Uint64Array){
			this.timing = new long[]{0, 0, 0};
			NUMBERArray<?> tim = (NUMBERArray<?>)tmp;
			for(int i = 0; i < tim.getLength(); i++)
				this.timing[i] = tim.get(i).longValue();
			return this.shot_cache_out = new int[]{-1};
		}
		return this.shot_cache_out = super.getShots(in);
	}

	@Override
	public final String getString(final String in) throws IOException {
		if(in == "TimeInterval") return String.format("from %s upto %s t0 %s", //
		        W7XDataProvider.format.format(new Date(this.timing[0])), //
		        W7XDataProvider.format.format(new Date(this.timing[1])), //
		        W7XDataProvider.format.format(new Date(this.timing[2])));
		return super.getString(in);
	}

	public final long[] getTiming() {
		if(this.timing == null) this.update("w7x", 0);
		return this.timing;
	}

	@Override
	public final WaveData getWaveData(final String in) {
		return W7XDataProvider.isW7X(in) ? new SimpleWaveData(in) : super.getWaveData(in);
	}

	@Override
	public final WaveData getWaveData(final String in_y, final String in_x) {
		return W7XDataProvider.isW7X(in_y) ? new SimpleWaveData(in_y, in_x) : super.getWaveData(in_y, in_x);
	}

	@Override
	public final void reset() {
		super.reset();
		synchronized(W7XDataProvider.this.threads){
			while(this.threads.size() > 0){
				final Thread th = this.threads.remove(0);
				if(th != null) th.interrupt();
			}
		}
	}

	@Override
	public final int setArguments(final JFrame f, final DataServerItem si) {
		if(si.properties != null){
			final String use_json_str = si.properties.get("use_json");
			if(use_json_str != null) this.use_json = Boolean.toString(true).equals(use_json_str);
		}
		return super.setArguments(f, si);
	}

	public final void setTiming() {
		if(super.is_connected()) try{
			this.mds.execute("TIME(0)");
			this.timing = null;
		}catch(final MdsException e){
			e.printStackTrace();
		}
		else this.timing = null;
	}

	public final void setTiming(final long from, final long upto) {
		this.setTiming(from, upto, 0L);
	}

	public final void setTiming(final long from, final long upto, final long orig) {
		if(super.is_connected()) try{
			System.out.println(super.mds.getString("T2STR(TIME($,$,$))", new Int64(from), new Int64(upto), new Int64(orig)));
			this.timing = new long[]{from, upto, orig};
		}catch(final MdsException e){
			e.printStackTrace();
		}
		else this.timing = new long[]{from, upto, orig};
	}

	@Override
	public final void update(String expt, final int shot_in) {
		if(expt == null || expt.length() == 0) expt = "w7x";
		super.update(expt, shot_in);
		if(this.error != null) return;
		if(shot_in == 0){
			this.error = "Invalid Shot";
			return;
		}
		final TimeInterval _timing = shot_in > 0 ? W7XSignalAccess.getTimeInterval(shot_in) : null;
		if(_timing == null || _timing.isEmpty()) try{
			if(shot_in <= 0) this.timing = super.mds.getLongArray("TIME()");
			else this.timing = super.mds.getLongArray(this.ctx, "$", new Path("\\TIME"));
		}catch(final IOException e){
			if(this.timing == null) this.error = "update: " + e.getMessage();
			return;
		}
		else this.timing = new long[]{_timing.from(), _timing.upto(), _timing.from() + 61000000000l};
	}
}
