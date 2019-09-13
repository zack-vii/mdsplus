package mds;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import debug.DEBUG;
import jscope.jScopeFacade;
import jscope.data.DataProvider;
import jscope.data.DataServerItem;
import jscope.data.ProviderEventListener;
import jscope.data.UpdateEventListener;
import jscope.data.framedata.FrameData;
import jscope.data.framedata.Frames;
import jscope.data.wavedata.WaveData;
import jscope.data.wavedata.WaveDataListener;
import jscope.data.wavedata.XYData;
import mds.data.CTX;
import mds.data.DTYPE;
import mds.data.TREE;
import mds.data.descriptor.Descriptor;
import mds.data.descriptor.Descriptor_A;
import mds.data.descriptor.Descriptor_S;
import mds.data.descriptor_a.Int64Array;
import mds.data.descriptor_a.Int8Array;
import mds.data.descriptor_a.NUMBERArray;
import mds.data.descriptor_a.Uint32Array;
import mds.data.descriptor_a.Uint64Array;
import mds.data.descriptor_a.Uint8Array;
import mds.data.descriptor_apd.List;
import mds.data.descriptor_r.function.X_OF;
import mds.data.descriptor_s.Float64;
import mds.data.descriptor_s.Int64;
import mds.data.descriptor_s.Missing;
import mds.data.descriptor_s.NODE;
import mds.data.descriptor_s.Pointer;
import mds.mdsip.MdsIp;
import mds.mdsip.MdsIp.Provider;
import mds.mdslib.MdsLib;

public class MdsDataProvider implements DataProvider{
	private class mdsUpdateEventListener implements mds.UpdateEventListener {
		private final ArrayList<String> events = new ArrayList<>();
		private final UpdateEventListener uel;
		private mdsUpdateEventListener(final UpdateEventListener uel) {
			this.uel = uel;
		}

		@Override
		public void handleUpdateEvent(Mds source, String name) {
			this.uel.handleUpdateEvent(name);
		}
	}
	private class mdsProviderEventListener implements mds.ContextEventListener, mds.TransferEventListener {
		private final ProviderEventListener pel;
		private mdsProviderEventListener(final ProviderEventListener pel) {
			this.pel = pel;
		}

		@Override
		public void handleTransferEvent(InputStream source, String msg, int read, int to_read) {
			this.pel.handleProviderEvent(MdsDataProvider.this, msg, read, to_read);
		}

		@Override
		public void handleContextEvent(Mds source, String msg, boolean ok) {
			this.pel.handleProviderEvent(MdsDataProvider.this, msg, 0, ok ? 0 : -1);
		}
	}
	class SegmentedFrameData implements FrameData{
		int								bytesPerPixel;
		Dimension						dim;
		int								framesPerSegment;
		int								mode;
		final MdsDataProvider.Signal	sig;
		int								startSegment, endSegment, actSegments;
		double							time_max, time_min;
		double							times[];

		public SegmentedFrameData(final MdsDataProvider.Signal sig, final float time_min, final float time_max) throws IOException{
			if(DEBUG.M) System.out.println("MdsDataProvider.SegmentedFrameData(\"" + sig + "\", " + time_min + ", " + time_max + ")");
			this.sig = sig;
			this.time_min = time_min;
			this.time_max = time_max;
			this.findSegments();
			this.actSegments = this.endSegment - this.startSegment + 1;
			if(DEBUG.D) System.out.println(this.actSegments + " : form " + this.startSegment + " upto " + this.endSegment);
			// Get Frame Dimension and frames per segment
			final Signal.Segment seg = sig.getSegment(0);
			if(seg.seg_shape.length != 3 && seg.seg_shape.length != 1 && (seg.seg_shape.length != 4 || seg.seg_shape[2] != 3)) throw new IOException("Invalid number of segment dimensions: " + Arrays.toString(seg.seg_shape));
			if(seg.seg_shape.length >= 3){
				this.dim = new Dimension(seg.seg_shape[0], seg.seg_shape[1]);
				this.framesPerSegment = seg.seg_dim.length;
				this.bytesPerPixel = seg.seg_shape.length == 4 ? Integer.BYTES : sig.segs[0].seg_dat.length();
				this.mode = sig.frameType;
			}else{
				this.framesPerSegment = 1;
				this.mode = FrameData.AWT_IMAGE;
				final BufferedImage img = ImageIO.read(new ByteArrayInputStream(sig.getSegment(0).seg_dat.asByteArray()));
				this.dim = new Dimension(img.getWidth(), img.getHeight());
			}
			// Get Frame times
			try{
				if(this.framesPerSegment == 1){ // We assume in this case that start time is the same of the frame time
					this.times = new double[this.actSegments];
					for(int i = 0; i < this.actSegments; i++)
						this.times[i] = sig.seglimits[(this.startSegment + i) * 2];
				}else{// Get segment times. We assume that the same number of frames is contained in every segment
					this.times = new double[this.actSegments * this.framesPerSegment];
					final double segt[] = new double[seg.seg_dim.length];
					System.arraycopy(seg.seg_dim, 0, segt, 0, seg.seg_dim.length);
					for(int i = 0; i < segt.length; i++)
						segt[i] = segt[i] - sig.seglimits[0];
					for(int i = 0; i < this.actSegments; i++)
						for(int j = 0; j < seg.seg_dim.length; j++)
							this.times[i * seg.seg_dim.length + j] = segt[j] + sig.seglimits[(this.startSegment + i) * 2];
				}
			}catch(final Exception e){
				e.printStackTrace();
			}
			if(DEBUG.D) System.out.println(this.times[0] + "," + this.times[1] + ", ... ," + this.times[this.times.length - 1]);
		}

		@Override
		public byte[] getFrameAt(final int idx) throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SegmentedFrameData.getFrameAt(" + idx + ")");
			final int segmentIdx = this.startSegment + idx / this.framesPerSegment;
			final int segmentOffset = (idx % this.framesPerSegment) * this.dim.width * this.dim.height * this.bytesPerPixel;
			final Descriptor_A<?> segment = this.sig.getSegment(segmentIdx).seg_dat;
			if(this.framesPerSegment == 1) return segment.asByteArray();
			return segment.asByteArray(segmentOffset, this.dim.width * this.dim.height * this.bytesPerPixel);
		}

		@Override
		public Dimension getFrameDimension() {
			return this.dim;
		}

		@Override
		public double[] getFrameTimes() {
			return this.times;
		}

		@Override
		public int getFrameType() throws IOException {
			return this.mode;
		}

		@Override
		public int getNumFrames() {
			return this.actSegments * this.framesPerSegment;
		}

		private void findSegments() {
			if(DEBUG.M) System.out.println("MdsDataProvider.SegmentedFrameData.findSegments() @ " + this.sig.y);
			try{
				final double[] limits = this.sig.seglimits;
				for(this.startSegment = 0; this.startSegment < this.sig.numSegments - 1; this.startSegment++)
					if(limits[this.startSegment * 2 + 1] > this.time_min) break;
				for(this.endSegment = this.sig.numSegments - 1; this.endSegment > this.startSegment; this.endSegment--)
					if(limits[this.endSegment * 2] < this.time_max) break;
			}catch(final Exception e){
				System.err.println("findSegments " + e);
				e.printStackTrace();
			}
		}
	}
	static class SimpleFrameData implements FrameData{
		static public final int getFrameType(final DTYPE dtype) {
			switch(dtype){
				case BU:
				case B:
					return FrameData.BITMAP_IMAGE_8;
				case WU:
				case W:
					return FrameData.BITMAP_IMAGE_16;
				case LU:
				case L:
					return FrameData.BITMAP_IMAGE_32;
				default:
					return FrameData.BITMAP_IMAGE_FLOAT;
			}
		}
		byte[]				buf;
		private Dimension	dim				= null;
		String				error;
		int					first_frame_idx	= -1;
		int					mode			= -1;
		private int			n_frames		= 0;
		int					pixel_size;
		double				time_max, time_min;
		private double[]	times			= null;

		public SimpleFrameData(final Signal sig, final double time_min, final double time_max) throws IOException{
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleFrameData(" + sig + ", " + time_min + ", " + time_max + ")");
			final double all_times[];
			this.time_min = time_min;
			this.time_max = time_max;
			if((sig.dat == null)) throw new IOException(sig.help_of_dat);
			this.buf = sig.dat.asByteArray();
			this.mode = sig.frameType;
			this.pixel_size = sig.dat.length() * 8;
			this.dim = new Dimension(sig.shape[0], sig.shape[1]);
			if(sig.dim == null){
				this.n_frames = sig.shape.length == 2 ? 1 : sig.shape[sig.shape.length - 1];
				this.times = new double[this.n_frames];
				for(int i = 0; i < this.n_frames; i++)
					this.times[i] = i;
			}else{
				all_times = sig.dim.toDoubleArray();
				int st_idx, end_idx;
				for(st_idx = 0; st_idx < all_times.length; st_idx++)
					if(all_times[st_idx] >= time_min) break;
				for(end_idx = st_idx; end_idx < all_times.length; end_idx++)
					if(all_times[end_idx] > time_max) break;
				this.n_frames = end_idx - st_idx;
				if(this.n_frames == 0) throw(new IOException("No frames found between " + time_min + " - " + time_max));
				this.times = new double[this.n_frames];
				System.arraycopy(all_times, st_idx, this.times, 0, this.n_frames);
			}
		}

		@Override
		public byte[] getFrameAt(final int idx) throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleFrameData.getFrameAt(" + idx + ")");
			byte[] b_img = null;
			if(this.mode == FrameData.BITMAP_IMAGE_8 || this.mode == FrameData.BITMAP_IMAGE_16 || this.mode == FrameData.BITMAP_IMAGE_32 || this.mode == FrameData.BITMAP_IMAGE_FLOAT){
				if(this.buf == null) throw(new IOException("Frames not loaded"));
				final ByteArrayInputStream b = new ByteArrayInputStream(this.buf);
				final DataInputStream d = new DataInputStream(b);
				if(this.buf == null) throw(new IOException("Frames dimension not evaluated"));
				final int img_size = this.dim.width * this.dim.height * this.pixel_size / 8;
				if(d.available() < img_size){
					if(DEBUG.M) System.err.println("# >> insufficient bytes: " + d.available() + "/" + img_size);
					return null;
				}
				d.skip(img_size * idx);
				b_img = new byte[img_size];
				d.readFully(b_img);
				return b_img;
			}
			return this.buf;
		}

		@Override
		public Dimension getFrameDimension() {
			return this.dim;
		}

		@Override
		public double[] getFrameTimes() {
			return this.times;
		}

		@Override
		public int getFrameType() throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleFrameData.getFrameType()");
			if(this.mode != -1) return this.mode;
			int i;
			for(i = 0; i < this.n_frames; i++){
				this.buf = this.getFrameAt(i);
				if(this.buf != null) break;
			}
			this.first_frame_idx = i;
			this.mode = Frames.decodeImageType(this.buf);
			return this.mode;
		}

		@Override
		public int getNumFrames() {
			return this.n_frames;
		}
	} // END Inner Class SimpleFrameData
	class SimpleWaveData implements WaveData{
		public static final int					DIMENSION_UNKNOWN	= -1;
		public static final int					SEGMENTED_NO		= 2;
		public static final int					SEGMENTED_UNKNOWN	= 3;
		public static final int					SEGMENTED_YES		= 1;
		private final boolean					continuousUpdate	= true;
		private boolean							isXLong				= false;
		private int								numDimensions		= SimpleWaveData.DIMENSION_UNKNOWN;
		private int								segmentMode			= SimpleWaveData.SEGMENTED_UNKNOWN;
		private final MdsDataProvider.Signal	signal;
		private String							title				= null;
		private final Vector<WaveDataListener>	waveDataListenersV	= new Vector<WaveDataListener>();
		private long							x2DLong[];
		private String							xLabel				= null;
		private String							yLabel				= null;

		public SimpleWaveData(final String in_y, final String in_x, final TREE tree){
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData(\"" + in_y + "\", \"" + in_x + "\", " + tree + ")");
			this.signal = new MdsDataProvider.Signal(in_y, in_x, false, tree);
			MdsDataProvider.this.error = null;
			this.segmentMode();
		}

		public SimpleWaveData(final String in_y, final TREE tree){
			this(in_y, null, tree);
		}

		@Override
		public void addWaveDataListener(final WaveDataListener listener) {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.addWaveDataListener()");
			synchronized(this.waveDataListenersV){
				this.waveDataListenersV.addElement(listener);
			}
		}

		@Override
		public final XYData getData(final double xmin, final double xmax, final int numPoints) throws IOException {
			return this.getData(xmin, xmax, numPoints, true);
		}

		@Override
		public final XYData getData(final int numPoints) throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getData(" + numPoints + ")");
			return this.getData(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, numPoints);
		}

		@Override
		public final void getDataAsync(final double lowerBound, final double upperBound, final int numPoints) {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getDataAsync(" + lowerBound + ", " + upperBound + ", " + numPoints + ")");
			synchronized(this.waveDataListenersV){
				for(final WaveDataListener wdl : this.waveDataListenersV)
					wdl.updatePending();
			}
			MdsDataProvider.this.updateWorker.updateInfo(lowerBound, upperBound, numPoints, this.waveDataListenersV, this, this.isXLong);
		}

		@Override
		public final int getNumDimension() throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getNumDimension()");
			if(this.numDimensions != SimpleWaveData.DIMENSION_UNKNOWN) return this.numDimensions;
			return this.numDimensions = this.signal.shape == null ? 1 : this.signal.shape.length;
		}

		@Override
		public final String getTitle() throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getTitle()");
			if(this.title == null && this.signal.help_of_dat != null) this.title = this.signal.help_of_dat.toString();
			return this.title;
		}

		@Override
		public final double[] getX2D() {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getX2D()");
			if(this.signal.dim == null) return null;
			try{
				final Descriptor_A<?> dim = this.signal.dim.getDataA();
				if(dim instanceof Uint64Array || dim instanceof Int64Array){
					this.isXLong = true;
					this.x2DLong = dim.toLongArray();
					return null;
				}
				this.x2DLong = null;
				return dim.toDoubleArray();
			}catch(final MdsException exc){
				System.err.println("getX2D");
			}
			return null;
		}

		@Override
		public final long[] getX2DLong() {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getX2DLong()");
			return this.x2DLong;
		}

		@Override
		public final String getXLabel() throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getXLabel()");
			if(this.xLabel == null && this.signal.units_of_dim != null) this.xLabel = this.signal.units_of_dim.toString();
			return this.xLabel;
		}

		@Override
		public final float[] getY2D() {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getY2D()");
			return this.execute("Dim_Of($,1)", this.signal.y).toFloatArray();
		}

		@Override
		public final String getYLabel() {
			return this.yLabel;
		}

		@Override
		public float[] getZ() {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getZ()");
			return this.signal.dat.toFloatArray();
		}

		@Override
		public String getZLabel() {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getZLabel()");
			return this.signal.units_of_dat.toString();
		}

		@Override
		public final boolean isXLong() {
			return this.isXLong;
		}

		private Descriptor<?> execute(final String expr, final Descriptor<?>... args) {
			try{
				return this.signal.tree.getMds().getAPI().tdiExecute(this.signal.tree, expr, args).getData();
			}catch(final MdsException e){
				e.printStackTrace();
				return Missing.NEW;
			}
		}

		private final XYData getData(final double xmin, final double xmax, final int numPoints, final boolean isLong) throws IOException {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getData(" + xmin + ", " + xmax + ", " + numPoints + ", " + isLong + ")");
			// If the requested number of samples is Integer.MAX_VALUE, force the old way of getting data
			Descriptor<?> sig;
			try{
				if(numPoints == Integer.MAX_VALUE) throw new Exception("Use Old Method for getting data");
				if(this.signal.tree != null && !this.signal.tree.is_open()) throw new MdsException(MdsException.TreeNOT_OPEN);
				final Descriptor_S<?> xminD = xmin > Double.NEGATIVE_INFINITY ? (isLong ? new Int64(xmin * 1e9) : new Float64(xmin)) : null;
				final Descriptor_S<?> xmaxD = xmax < Double.POSITIVE_INFINITY ? (isLong ? new Int64(xmax * 1e9) : new Float64(xmax)) : null;
				sig = MdsDataProvider.this.api.miscGetXYSignalXd(this.signal.tree, this.signal.y, this.signal.x, xminD, xmaxD, numPoints);
			}catch(final MdsException exec){
				throw exec;
			}catch(final Exception exec){
				final StackTraceElement se[] = exec.getStackTrace();
				System.err.println(String.format("getData: " + exec.toString()));
				for(final StackTraceElement s : se)
					System.err.println(s.toString());
				final Descriptor<?> dscr = this.execute(this.setTimeContext(xmin, xmax, numPoints, isLong), this.signal.y);
				final Descriptor_A<?> dsca = dscr.getDataA();
				if(dsca == null || !(dsca instanceof NUMBERArray)) return null;
				final Descriptor_A<?> dimd = new X_OF.DimOf(dscr).getDataA();
				if(dimd == null || !(dimd instanceof NUMBERArray)){
					final double[] X = new double[dsca.getLength()];
					for(int i = 0; i < X.length; i++)
						X[i] = i;
					return new XYData(X, dsca.toFloatArray(), Double.POSITIVE_INFINITY, true);
				}
				if(dimd instanceof Uint64Array || dimd instanceof Int64Array){
					final long[] longX = dimd.toLongArray();
					final double actres = longX.length * 1e9 / (longX[longX.length - 1] - longX[0]);
					return new XYData(longX, dsca.toFloatArray(), actres, true, xmin, xmax);
				}
				final double[] X = dimd.toDoubleArray();
				final double actres = X.length / (X[X.length - 1] - X[0]);
				return new XYData(X, dsca.toFloatArray(), actres, true, xmin, xmax);
			}
			Descriptor<?> tmp = sig.getHelp();
			this.title = tmp == null ? null : tmp.toString();
			final Descriptor<?> dim = sig.getDimension();
			tmp = dim.getUnits();
			this.xLabel = tmp == null ? null : tmp.toString();
			tmp = sig.getUnits();
			this.yLabel = tmp == null ? null : tmp.toString();
			final Descriptor<?> dimd = dim.getDataA();
			this.isXLong = (dimd.dtype() == DTYPE.Q || dimd.dtype() == DTYPE.QU);
			final XYData res;
			final double maxX;
			final float y[] = sig.toFloatArray();
			final double actres = sig.getRaw().toDouble();
			if(this.isXLong){
				final long[] lx = dimd.toLongArray();
				res = new XYData(lx, y, actres, true, xmin, xmax);
				maxX = lx.length > 0 ? lx[lx.length - 1] / 1e9 : 0;
			}else{
				final double[] dx = dimd.toDoubleArray();
				res = new XYData(dx, y, actres, true, xmin, xmax);
				maxX = dx.length > 0 ? dx[dx.length - 1] : 0;
			}
			if(this.segmentMode == SimpleWaveData.SEGMENTED_YES && this.continuousUpdate){
				long refreshPeriod = jScopeFacade.getRefreshPeriod();
				if(refreshPeriod <= 0) refreshPeriod = 1000; // default 1 s refresh
				MdsDataProvider.this.updateWorker.updateInfo(/*xmin*/maxX, Double.POSITIVE_INFINITY, 2000, this.waveDataListenersV, this, this.isXLong);
			}
			return res;
		}

		private final boolean segmentMode() {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.SegmentMode()");
			if(this.segmentMode == SimpleWaveData.SEGMENTED_UNKNOWN) this.segmentMode = this.signal.numSegments > 0 ? SimpleWaveData.SEGMENTED_YES : SimpleWaveData.SEGMENTED_NO;
			return this.segmentMode == SimpleWaveData.SEGMENTED_YES;
		}

		private String setTimeContext(final double xmin, final double xmax, final double delta) {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.getTimeContext(" + xmin + ", " + xmax + ")");
			final String xminstr = Double.isFinite(xmin) ? Double.toString(xmin) : "*";
			final String xmaxstr = Double.isFinite(xmax) ? Double.toString(xmax) : "*";
			final String deltastr = delta == 0 ? "*" : Double.toString(delta);
			return new StringBuilder(1024).append("SetTimeContext(").append(xminstr).append(',').append(xmaxstr).append(',').append(deltastr).append(");$").toString();
		}

		private String setTimeContext(final double xmin, final double xmax, final int numPoints, final boolean isLong) {
			if(DEBUG.M) System.out.println("MdsDataProvider.SimpleWaveData.setTimeContext(" + xmin + ", " + xmax + ", " + numPoints + ", " + isLong + ")");
			final double delta = (Double.isFinite(xmin) && Double.isFinite(xmax)) ? (xmax - xmin) / numPoints : 0;
			return this.setTimeContext(xmin, xmax, delta);
		}
	} // END Inner Class SimpleWaveData
	private final class Signal{
		private final class Segment{
			public final Descriptor_A<?>	seg_dat;
			public final float[]			seg_dim;
			private final int				idx;
			private final NODE<?>			node;
			public final int[]				seg_shape;

			public Segment(final NODE<?> node, final int idx) throws IOException{
				final mds.data.descriptor_r.Signal dsc = (mds.data.descriptor_r.Signal)node.getSegment(idx).getLocal();
				this.seg_dim = dsc.getDimension().toFloatArray();
				final Descriptor_A<?> data = dsc.getDataA();
				final int[] ishape = data.getShape();
				if(Signal.this.isframe && ishape.length == 4 && ishape[2] == 3) this.seg_dat = this.getRGB(data);
				else this.seg_dat = data;
				this.seg_shape = this.seg_dat.getShape();
				this.idx = idx;
				this.node = node;
				if(idx == 0){
					Signal.this.units_of_dim = new X_OF.UnitsOf(dsc.getDimension()).evaluate().toString();
					Signal.this.units_of_dat = new X_OF.UnitsOf(dsc).evaluate().toString();
					Signal.this.help_of_dat = new X_OF.HelpOf(dsc).evaluate().toString();
				}
			}

			@SuppressWarnings("unused")
			public final boolean equals(final NODE<?> node_in, final int idx_in) {
				return this.idx == idx_in && this.node.equals(node_in);
			}

			private Uint32Array getRGB(final Descriptor_A<?> data) {
				final int[] shape_in = data.getShape();
				final int[] ibuf = data.toIntArray();
				final int imlen = shape_in[0] * shape_in[1];
				final IntBuffer ib = IntBuffer.allocate(imlen * shape_in[3]);
				final IntBuffer r = IntBuffer.wrap(ibuf);
				final IntBuffer g = IntBuffer.wrap(ibuf);
				final IntBuffer b = IntBuffer.wrap(ibuf);
				for(int ti = 0; ti < shape_in[3]; ti++){
					r.position(imlen * (ti * 3 + 0));
					g.position(imlen * (ti * 3 + 1));
					b.position(imlen * (ti * 3 + 2));
					for(int ip = 0; ip < imlen; ip++)
						ib.put(((r.get() & 0xFF) << 16) | ((g.get() & 0xFF) << 8) | (b.get() & 0xFF));
				}
				return new Uint32Array(ib.array(), new int[]{shape_in[0], shape_in[1], shape_in[3]});
			}
		}
		public final Descriptor_A<?>	dat;
		public final Descriptor_A<?>	dim;
		public final int				frameType;
		public String					help_of_dat		= null;
		private final boolean			isframe;
		public int						numSegments		= 0;
		public final double[]			seglimits;
		public final Segment			segs[];
		public final int[]				shape;
		public String					units_of_dat	= null;
		public String					units_of_dim	= null;
		public final Descriptor<?>		y, x;
		private final TREE				tree;

		public Signal(final String in_y, final String in_x, final boolean isframe, final TREE tree){
			if(DEBUG.M) System.out.println("MdsDataProvider.Signal(\"" + in_y + "\",\"" + in_x + "\"," + isframe + ")");
			MdsDataProvider.this.error = null;
			this.tree = tree;
			this.isframe = isframe;
			this.x = this.getNode(in_x);
			this.y = this.getNode(in_y);
			if(tree == null){
				this.frameType = -1;
				this.dim = this.dat = null;
				this.segs = null;
				this.shape = null;
				this.seglimits = null;
				return;
			}
			if(isframe){
				if(this.y instanceof NODE){
					this.numSegments = this.getNumSegments((NODE<?>)this.y);
					if(this.numSegments > 0){
						this.seglimits = this.getLimits();
						this.segs = new Segment[this.numSegments];
						this.getSegment(0);
						if(this.segs[0].seg_dat != null) this.frameType = SimpleFrameData.getFrameType(this.segs[0].seg_dat.dtype());
						else this.frameType = -1;
						this.dim = this.dat = null;
						this.shape = new int[this.segs[0].seg_shape.length];
						System.arraycopy(this.segs[0].seg_shape, 0, this.shape, 0, this.shape.length);
						this.shape[this.shape.length - 1] = this.shape[this.shape.length - 1] * this.numSegments;
						return;
					}
				}
				Descriptor<?> _dat = null;
				Descriptor<?> _dim = null;
				try{
					_dat = this.y.getLocal();
					if(!(_dat instanceof Descriptor_A<?>)){
						this.units_of_dat = new X_OF.UnitsOf(_dat).getData().toString();
						this.help_of_dat = new X_OF.HelpOf(_dat).getData().toString();
						_dim = new X_OF.DimOf(_dat).getLocal();
						this.units_of_dim = new X_OF.UnitsOf(_dim).getData().toString();
						_dat = _dat.getDataA();
						_dim = _dim.getDataA();
					}
				}catch(final MdsException e){
					_dat = null;
					_dim = null;
					this.help_of_dat = "Signal: " + e.getMessage();
				}
				this.segs = null;
				this.seglimits = null;
				this.dat = (Descriptor_A<?>)_dat;
				this.dim = (Descriptor_A<?>)_dim;
				if(this.dat == null){
					this.shape = new int[0];
					this.frameType = FrameData.UNKNOWN;
				}else{
					this.shape = this.dat.getShape();
					this.frameType = SimpleFrameData.getFrameType(this.dat.dtype());
				}
				return;
			}
			this.frameType = -1;
			this.dim = this.dat = null;
			this.segs = null;
			this.shape = null;
			this.seglimits = null;
		}

		public final Segment getSegment(final int idx) {
			if(this.segs[idx] == null) try{
				this.segs[idx] = new Segment((NODE<?>)this.y, idx);
			}catch(final IOException e){
				e.printStackTrace();
				return null;
			}
			return this.segs[idx];
		}

		@Override
		public final String toString() {
			final StringBuilder sb = new StringBuilder("Signal(").append(this.y);
			if(this.numSegments > 0) sb.append("<").append(this.numSegments).append(">");
			return sb.append(")").toString();
		}

		private final double[] getLimits() {
			if(this.y instanceof NODE) try{
				final NODE<?> node = (NODE<?>)this.y;
				final List answer = node.getSegmentTimes();
				final int nseg = answer.getElement(0).toInt();
				final List start = (List)answer.getElement(1);
				final List end = (List)answer.getElement(1);
				final double[] limits = new double[nseg * 2];
				for(int i = 0; i < nseg; i++){
					limits[i * 2] = start.getElement(i).toDouble();
					limits[i * 2 + 1] = end.getElement(i).toDouble();
				}
				return limits;
			}catch(final Exception e){
				System.err.println("GetLimits: " + e);
				e.printStackTrace();
			}
			return null;
		}

		private final Descriptor<?> getNode(final String expr) {
			if(DEBUG.M) System.out.println("MdsDataProvider.Signal.getNode(\"" + expr + "\")");
			if(expr == null || expr.length() == 0) return null;
			Descriptor<?> descriptor = null;
			try{
				descriptor = MdsDataProvider.this.api.tdiCompile(this.tree, expr).getData();
				if(descriptor instanceof NODE) while((((NODE<?>)descriptor).getNciDType() & 0xFE) == 192)// is nid or path
					descriptor = ((NODE<?>)descriptor).getNciRecord();
			}catch(final MdsException e){
				// if(DEBUG.E)
				System.err.println(expr + " : " + e);
			}
			return descriptor;
		}

		private final int getNumSegments(final NODE<?> node) {
			if(node != null) try{
				return node.getNumSegments();
			}catch(final MdsException e){
				if(DEBUG.E) System.err.println(node + " not segmented : " + e);
			}
			return 0;
		}
	}
	// Inner class UpdateWorker handler asynchronous requests for getting (portions of) data
	private final class UpdateWorker extends Thread{
		private final class UpdateDescriptor{
			final boolean					isXLong;
			final SimpleWaveData			simpleWaveData;
			final double					updateLowerBound;
			final int						updatePoints;
			final double					updateUpperBound;
			final Vector<WaveDataListener>	waveDataListenersV;

			UpdateDescriptor(final double updateLowerBound, final double updateUpperBound, final int updatePoints, final Vector<WaveDataListener> waveDataListenersV, final SimpleWaveData simpleWaveData, final boolean isXLong){
				this.updateLowerBound = updateLowerBound;
				this.updateUpperBound = updateUpperBound;
				this.updatePoints = updatePoints;
				this.waveDataListenersV = waveDataListenersV;
				this.simpleWaveData = simpleWaveData;
				this.isXLong = isXLong;
			}
		}
		private final Vector<UpdateDescriptor>	requestsV	= new Vector<UpdateDescriptor>();
		private final int						prev_abort;

		public UpdateWorker(){
			this.prev_abort = MdsDataProvider.abort;
		}

		@Override
		public final synchronized void run() {
			if(DEBUG.M) System.out.println("run()");
			this.setName("UpdateWorker");
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
				if(DEBUG.D) System.out.println("MdsDataProvider->loop " + this.requestsV.size());
				Object currData = null;
				try{
					currData = currUpdate.simpleWaveData.getData(currUpdate.updateLowerBound, currUpdate.updateUpperBound, currUpdate.updatePoints, currUpdate.isXLong);
				}catch(final Exception exc){
					currData = exc;
					if(DEBUG.E) System.err.println(new Date() + " Error in asynchUpdate: " + exc);
				}
				synchronized(currUpdate.waveDataListenersV){
					for(final WaveDataListener wdl : currUpdate.waveDataListenersV)
						wdl.dataRegionUpdated(currData, Double.isInfinite(currUpdate.updateLowerBound) || Double.isInfinite(currUpdate.updateUpperBound));
				}
			}
		}

		private final synchronized void updateInfo(final double updateLowerBound, final double updateUpperBound, final int updatePoints, final Vector<WaveDataListener> waveDataListenersV, final SimpleWaveData simpleWaveData, final boolean isXLong) {
			synchronized(this.requestsV){
				this.requestsV.add(new UpdateDescriptor(updateLowerBound, updateUpperBound, updatePoints, waveDataListenersV, simpleWaveData, isXLong));
			}
			this.notify();
		}
	} // End Inner class UpdateWorker
	protected static final int	MAX_PIXELS			= 20000;
	protected static final long	RESAMPLE_TRESHOLD	= 1000000;
	protected static int		abort				= 0;

	protected static final boolean notYetNumber(final String in) {
		if(DEBUG.M) System.out.println("MdsDataProvider.NotYetNumber(\"" + in + "\")");
		try{
			Float.parseFloat(in);
		}catch(final NumberFormatException e){
			return true;
		}
		return false;
	}

	protected static final boolean notYetString(final String in) {
		if(DEBUG.M) System.out.println("MdsDataProvider.NotYetString(\"" + in + "\")");
		int i;
		if(in.charAt(0) == '\"' || in.charAt(0) == '\''){
			// for(i = 1; i < in.length() && (in.charAt(i) != '\"' && in.charAt(i) != '\'' || ((in.charAt(i) == '\"' || in.charAt(i) == '\'') && in.charAt(i - 1) == '\\')); i++);
			for(i = 1; i < in.length(); i++)
				if(!(in.charAt(i) == '\"' || in.charAt(i) == '\'') //
				        && !((in.charAt(i) == '\"' || in.charAt(i) == '\'') && in.charAt(i - 1) == '\\')) break;
			if(i == (in.length() - 1)) return false;
		}
		return true;
	}

	private static final double getDate(final String in) throws Exception {
		if(DEBUG.M) System.out.println("MdsDataProvider.getDate(\"" + in + "\")");
		final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
		final Date date = df.parse(in + " UTC");
		cal.setTime(date);
		final long javaTime = cal.getTime().getTime();
		return javaTime;
	}

	private static final double getNow(final String in) throws Exception {
		if(DEBUG.M) System.out.println("MdsDataProvider.getNow(\"" + in + "\")");
		boolean isPlus = true;
		int hours = 0, minutes = 0, seconds = 0;
		String currStr = in.trim().toUpperCase();
		if(!currStr.startsWith("NOW")) throw new Exception();
		currStr = currStr.substring(3).trim();
		if(currStr.length() > 0){ // Not only NOW
			if(currStr.startsWith("+")) isPlus = true;
			else if(currStr.startsWith("-")) isPlus = false;
			else throw new Exception();
			currStr = currStr.substring(1).trim();
			final StringTokenizer st = new StringTokenizer(currStr, ":", true);
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
			if(st.hasMoreTokens()) seconds = Integer.parseInt(st.nextToken());
		}
		if(!isPlus){
			hours = -hours;
			minutes = -minutes;
			seconds = -seconds;
		}
		final Calendar cal = Calendar.getInstance();
		// cal.setTimeZone(TimeZone.getTimeZone("GMT+00"));
		cal.setTime(new Date());
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.SECOND, seconds);
		final long javaTime = cal.getTime().getTime();
		return javaTime;
	}
	protected String		environment_vars;
	protected String		error			= null;
	public Mds				mds				= null;
	private MdsApi			api;
	protected Provider		provider;
	protected UpdateWorker	updateWorker;
	protected HashMap<ProviderEventListener,mdsProviderEventListener> pel = new HashMap<>();
	protected HashMap<UpdateEventListener,mdsUpdateEventListener> uel = new HashMap<>();
	private boolean			use_compression	= false;
	protected CTX			ctx;
	Vector<TREE>			trees			= new Vector<TREE>();
	final private Pointer	dbid			= null;

	@Override
	public void addProviderEventListener(final ProviderEventListener l) {
		if(DEBUG.M) System.out.println("MdsDataProvider.addConnectionListener(" + l + ")");
		final mdsProviderEventListener ll = new mdsProviderEventListener(l);
		this.pel.put(l, ll);
		this.mds.addContextEventListener(ll);
		this.mds.addTransferEventListener(ll);
	}

	@Override
	public void addUpdateEventListener(UpdateEventListener l, String event) throws IOException {
		if(DEBUG.M) System.out.println("MdsDataProvider.addUpdateEventListener(" + l + "," + event + ")");
		if(l==null || event == null || event.trim().length() == 0) return;
		this.checkConnection();
		mdsUpdateEventListener ll = this.uel.get(l);
		if (uel==null) uel.put(l,ll = new mdsUpdateEventListener(l));
		ll.events.add(event);
		this.mds.setEvent(ll, event);
	}

	@Override
	public boolean checkProvider() {
		if(DEBUG.M) System.out.println("MdsDataProvider.checkProvider()");
		if(this.mds instanceof MdsIp) return ((MdsIp)this.mds).isConnected();
		return true;
	}

	@Override
	public void dispose() {
		if(DEBUG.M) System.out.println("MdsDataProvider.dispose()");
		this.disconnect();
	}

	@Override
	public String errorString() {
		return this.error;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class getDefaultBrowser() {
		return MdsSignalBrowser.class;
	}

	@Override
	public float getFloat(final String in) throws IOException {
		if(DEBUG.M) System.out.println("MdsDataProvider.getFloat(\"" + in + "\")");
		this.error = null;
		// First check Whether this is a date
		try{
			return (float)MdsDataProvider.getDate(in);
		}catch(final Exception e1){
			try{
				return (float)MdsDataProvider.getNow(in);
			}catch(final Exception e2){
				if(MdsDataProvider.notYetNumber(in)){
					if(!this.checkOpen()) return 0;
					final Descriptor<?> desc = this.mds.getDescriptor(this.ctx, in);
					return desc.toFloat();
				}
				return Float.parseFloat(in);
			}
		}
	}

	@Override
	public FrameData getFrameData(final String in_y, final String in_x, final float time_min, final float time_max) throws IOException {
		if(DEBUG.M) System.out.println("MdsDataProvider.getFrameData(\"" + in_y + "\", \"" + in_x + "\", " + time_min + ", " + time_max + ")");
		if(!this.checkOpen()) throw new MdsException(MdsException.TreeNOT_OPEN);
		final Signal sig = new MdsDataProvider.Signal(in_y, in_x, true, this.trees.lastElement());
		this.error = null;
		if(sig.numSegments > 0) return new SegmentedFrameData(sig, time_min, time_max);
		return(new SimpleFrameData(sig, time_min, time_max));
	}

	@Override
	public Vector<LabeledProperty> getLabeledProperties() {
		final Vector<LabeledProperty> props = new Vector<LabeledProperty>();
		props.add(new LabeledCheckBox("use_compression", this.use_compression));
		return props;
	}

	@Override
	public final String getLegendString(final String s) {
		return s;
	}

	public final String getProvider() {
		return this.provider.toString();
	}

	public int getShot() {
		return this.trees.isEmpty() ? -1 : this.trees.lastElement().shot;
	}

	@Override
	public int[] getShots(final String in) throws IOException {
		if(DEBUG.M) System.out.println("*MdsDataProvider.getShots(\"" + in + "\")");
		// To shot evaluation don't execute check
		// if a pulse file is open
		this.checkConnection();
		return this.mds.getIntegerArray(this.ctx, in);
	}

	@Override
	public String getString(final String in) throws IOException {
		if(DEBUG.M) System.out.println("MdsDataProvider.getString(\"" + in + "\")");
		if(in == null) return null;
		this.error = null;
		if(MdsDataProvider.notYetString(in)){
			if(!this.checkOpen()) return null;
			final Descriptor<?> desc = this.mds.getDescriptor(this.ctx, in);
			if(desc instanceof Uint8Array || desc instanceof Int8Array) return new String(desc.toByteArray(), "UTF-8");
			return desc.toString();
		}
		return new String(in.getBytes(), 1, in.length() - 2, "UTF-8");
	}

	@Override
	public WaveData getWaveData(final String in) {
		return new SimpleWaveData(in, this.trees.isEmpty() ? null : this.trees.lastElement());
	}

	@Override
	public WaveData getWaveData(final String in_y, final String in_x) {
		return new SimpleWaveData(in_y, in_x, this.trees.isEmpty() ? null : this.trees.lastElement());
	}

	final public boolean is_connected() {
		if(this.mds instanceof MdsLib) return true;
		return this.mds != null && ((MdsIp)this.mds).isConnected();
	}

	@Override
	public final synchronized void removeProviderEventListener(final ProviderEventListener l) {
		if(DEBUG.M) System.out.println("MdsDataProvider.removeProviderEventListener(" + l + ")");
		final mdsProviderEventListener ll = this.pel.remove(l);
		if(l == null || this.mds == null) return;
		this.mds.removeContextEventListener(ll);
		this.mds.removeTransferEventListener(ll);
	}

	@Override
	public final synchronized void removeUpdateEventListener(final UpdateEventListener l, final String event) throws IOException {
		if(DEBUG.M) System.out.println("MdsDataProvider.removeUpdateEventListener(" + l + "," + event + ")");
		if(l == null || event == null || event.trim().length() == 0) return;
		final mdsUpdateEventListener ll = this.uel.get(l);
		ll.events.remove(event);
		if (ll.events.isEmpty()) this.uel.remove(l);
		this.checkConnection();
		if(this.mds instanceof MdsIp) ((MdsIp)this.mds).mdsRemoveEvent(ll, event);
	}

	@Override
	public void reset() {
		this.disconnect();
		this.connect();
	}

	@Override
	public int setArguments(final JFrame f, final DataServerItem si) {
		if(DEBUG.M) System.out.println("MdsDataProvider.setArguments(" + f + ", " + si + ")");
		this.provider = (si.server == null || si.server.isEmpty()) ? null : new Provider(si.server);
		if(this.mds != null){
			this.mds.close();
			this.mds = null;
		}
		if(this.provider == null){
			this.mds = new MdsLib();
			return DataProvider.LOGIN_OK;
		}else{
			if(si.properties != null){
				final String use_compression_str = si.properties.get("use_compression");
				if(use_compression_str != null) this.use_compression = Boolean.toString(true).equals(use_compression_str);
			}
			this.connect();
			return this.is_connected() ? DataProvider.LOGIN_OK : DataProvider.LOGIN_ERROR;
		}
	}

	@Override
	public void setDefault(final String def_node) {
		if(this.checkOpen()) try{
			synchronized(this.trees){
				this.trees.lastElement().setDefault(def_node);
			}
		}catch(final MdsException e){
			e.printStackTrace();
		}
	}

	@Override
	public void setEnvironment(final Map<String, String> in) throws IOException {
		if(DEBUG.M) System.out.println("MdsDataProvider.setEnvironment(\"" + in + "\")");
		if(in == null || in.size() == 0) return;
		for(final Entry<String, String> pair : in.entrySet()){
			final String key = pair.getKey();
			if(key.startsWith("_")) this.mds.execute(String.join("=", pair.getKey(), pair.getValue()));
			else this.api.setenv(pair.toString());
			if(DEBUG.D) System.out.println(this.api.getenv(key));
		}
	}

	@Override
	public void update(final String expt_in, final int shot_in) {
		if(DEBUG.M) System.out.println("MdsDataProvider.Update(\"" + expt_in + "\", " + shot_in + ")");
		this.error = null;
		this.open_tree(expt_in, shot_in);
	}

	protected boolean checkConnection() {
		if(DEBUG.M) System.out.println("MdsDataProvider.checkConnection()");
		if(this.mds == null) return false;
		if(this.mds instanceof MdsLib) return true;
		if(!((MdsIp)this.mds).isConnected()) this.connect();
		return ((MdsIp)this.mds).isConnected();
	}

	protected boolean checkOpen() {
		synchronized(this.trees){
			if(this.trees.isEmpty() || !this.trees.lastElement().is_open()) return false;
		}
		return true;
	}

	final private void close_trees() {
		this.ctx = this.dbid;
		synchronized(this.trees){
			for(final TREE tree : this.trees){
				if(tree == null) continue;
				try{
					if(tree.is_open()) tree.close();
				}catch(final MdsException e){
					e.printStackTrace();
				}
			}
			this.trees.clear();
		}
	}

	private void connect() {
		if(this.mds instanceof MdsIp){
			if(((MdsIp)this.mds).isConnected()) return;
			((MdsIp)this.mds).close();
			this.mds = null;
		} 
		if (this.mds == null) {

			if(this.provider == null){
				this.mds = new MdsLib();
			}else{
				this.mds = new MdsIp(this.provider);
				((MdsIp)this.mds).connect(this.use_compression);
			}
			for (mdsProviderEventListener l : this.pel.values()) {
				this.mds.addTransferEventListener(l);
				this.mds.addContextEventListener(l);
			}
			for (mdsUpdateEventListener l : this.uel.values()) {
				for (String e : l.events)
					this.mds.setEvent(l,e);
			}
			this.api = this.mds.getAPI();
			try{
				this.api.setenv("MDSPLUS_DEFAULT_RESAMPLE_MODE=MinMax");
			}catch(final MdsException e){
				e.printStackTrace();
			}
			MdsDataProvider.abort++;
			this.updateWorker = new UpdateWorker();
			this.updateWorker.start();
		}
	}

	private void disconnect() {
		MdsDataProvider.abort++;
		this.close_trees();
		if(this.is_connected()) this.mds.close();
		this.mds = null;
		if(DEBUG.D) System.out.println(">> disconnected");
	}

	private boolean open_tree(final String expt_in, final int shot_in) {
		if(!this.is_connected() || shot_in == 0) return false;
		try{
			TREE tree = null;
			synchronized(this.trees){
				for(final TREE _tree : this.trees)
					if(_tree.expt.equalsIgnoreCase(expt_in) && _tree.shot == shot_in){
						tree = _tree;
						break;
					}
				if(tree == null) tree = new TREE(this.mds, expt_in, shot_in);
				else this.trees.remove(tree);
				this.trees.add(tree);
			}
			if(!tree.is_open()) tree.open(TREE.READONLY);
			if(tree.is_open()){
				this.ctx = tree;
				if(this.environment_vars != null && this.environment_vars.length() > 0){
					this.mds.execute(this.environment_vars);
					if(this.error != null) this.error = "Public variable evaluation error " + expt_in + " shot " + shot_in + " : " + this.error;
				}
			}else{
				this.error = "error open tree.";
				this.ctx = this.dbid;
				return false;
			}
		}catch(final MdsException e){
			this.error = "open_tree: " + e.getMessage();
			return false;
		}
		return true;
	}
}
