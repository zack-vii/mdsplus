package w7x;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.mpg.ipp.codac.archive.XPtools;
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

public final class W7XSignalAccess{
	/** Helper class to asynchronously read Signals **/
	private final static class SignalFetcher extends Thread{
		private final String		path;
		private final ReadOptions	options;
		private final TimeInterval	interval;
		private Signal				signal;

		public SignalFetcher(final String path, final TimeInterval interval, final ReadOptions options){
			this.path = path;
			this.interval = interval;
			this.options = options;
		}

		/** Returns the Signal object **/
		public final Signal getSignal() {
			try{
				this.join();
				return this.signal;
			}catch(final InterruptedException e){
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public final void run() {
			@SuppressWarnings("resource")
			final SignalReader reader = W7XSignalAccess.getReader(this.path);
			try{
				this.signal = reader.readSignal(this.interval, this.options);
			}finally{
				reader.close();
			}
			System.out.println(this.signal.getLastSampleTime() + " : " + this.signal.getDimensionSize(0));
		}
	}
	public static final String							help			= "usage:\nString   path = \"/ArchiveDB/codac/W7X/CoDaStationDesc.111/DataModuleDesc.250_DATASTREAM/15/L1_ECA63/scaled\";\nString     xp = \"XP:20160310.7\";\nSignal signal = W7XSignalAccess.getSignal(path,xp);\ndouble[] data = W7XSignalAccess.getDouble(signal);\nint[]   shape = W7XSignalAccess.getShape(signal);\nlong[]   time = W7XSignalAccess.getDimension(signal);";
	private static final Map<String, W7XSignalAccess>	access			= new HashMap<String, W7XSignalAccess>(2);
	private static final int							MAX_SAMPLES		= 0x01000000;
	private static final String[]						databaselist	= new String[]{"ArchiveDB", "Test", "Sandbox"};

	/** Returns the data vector of the given Signal object as byte[] **/
	public static final byte[] getByte(final Signal signal) {
		if(signal == null) return new byte[]{};
		return W7XSignalAccess.getByte(signal, ByteBuffer.allocate(signal.getSampleCount())).array();
	}

	public static final ByteBuffer getByte(final Signal signal, final ByteBuffer buf) {
		if(signal == null) return buf;
		for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).byteValue());
		return buf;
	}

	/** Returns the list of known data bases **/
	public static final List<String> getDataBaseList() {
		return Arrays.asList(W7XSignalAccess.databaselist);
	}

	/** Returns the time vector of the given Signal object as long[] **/
	public static final long[] getDimension(final Signal signal) {
		return W7XSignalAccess.getLong(signal.getDimensionSignal(0));
	}

	/** Returns the data vector of the given Signal object as double[] **/
	public static final double[] getDouble(final Signal signal) {
		if(signal == null) return new double[]{};
		return W7XSignalAccess.getDouble(signal, DoubleBuffer.allocate(signal.getSampleCount())).array();
	}

	/** Returns the data vector of the given Signal object as double[] **/
	public static final double[] getDouble(final Signal signal, final double scale, final double offset) {
		if(signal == null) return new double[]{};
		return W7XSignalAccess.getDouble(signal, scale, offset, DoubleBuffer.allocate(signal.getSampleCount())).array();
	}

	public static final DoubleBuffer getDouble(final Signal signal, final double scale, final double offset, final DoubleBuffer buf) {
		if(signal == null) return buf;
		if(scale == 1){
			if(offset == 0) for(final int[] i : IndexIterator.of(signal))
				buf.put(signal.getValue(Number.class, i).doubleValue());
			else for(final int[] i : IndexIterator.of(signal))
				buf.put(signal.getValue(Number.class, i).doubleValue() + offset);
		}else if(offset == 0) for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).doubleValue() * scale);
		else for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).doubleValue() * scale + offset);
		return buf;
	}

	public static final DoubleBuffer getDouble(final Signal signal, final DoubleBuffer buf) {
		if(signal == null) return buf;
		for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).doubleValue());
		return buf;
	}

	/** Returns the data vector of the given Signal object as float[] **/
	public static final float[] getFloat(final Signal signal) {
		if(signal == null) return new float[]{};
		return W7XSignalAccess.getFloat(signal, FloatBuffer.allocate(signal.getSampleCount())).array();
	}

	/** Returns the data vector of the given Signal object as float[] **/
	public static final float[] getFloat(final Signal signal, final float scale, final float offset) {
		if(signal == null) return new float[]{};
		return W7XSignalAccess.getFloat(signal, scale, offset, FloatBuffer.allocate(signal.getSampleCount())).array();
	}

	public static final FloatBuffer getFloat(final Signal signal, final float scale, final float offset, final FloatBuffer buf) {
		if(signal == null) return buf;
		if(scale == 1){
			if(offset == 0) for(final int[] i : IndexIterator.of(signal))
				buf.put(signal.getValue(Number.class, i).floatValue());
			else for(final int[] i : IndexIterator.of(signal))
				buf.put(signal.getValue(Number.class, i).floatValue() + offset);
		}else if(offset == 0) for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).floatValue() * scale);
		else for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).floatValue() * scale + offset);
		return buf;
	}

	public static final FloatBuffer getFloat(final Signal signal, final FloatBuffer buf) {
		if(signal == null) return buf;
		for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).floatValue());
		return buf;
	}

	/** Returns the data vector of the given Signal object as int[] **/
	public static final int[] getInteger(final Signal signal) {
		if(signal == null) return new int[]{};
		return W7XSignalAccess.getInteger(signal, IntBuffer.allocate(signal.getSampleCount())).array();
	}

	public static final IntBuffer getInteger(final Signal signal, final IntBuffer buf) {
		if(signal == null) return buf;
		for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).intValue());
		return buf;
	}

	/** Returns the addresses of available children of the given path String in the given TimeInterval **/
	public static final List<SignalAddress> getList(final String database, final String path, final TimeInterval interval) {
		return W7XSignalAccess.getAccess(database).getList_(path, interval);
	}

	/** Returns the data vector of the given Signal object as long[] **/
	public static final long[] getLong(final Signal signal) {
		if(signal == null) return new long[]{};
		return W7XSignalAccess.getLong(signal, LongBuffer.allocate(signal.getSampleCount())).array();
	}

	public static final LongBuffer getLong(final Signal signal, final LongBuffer buf) {
		if(signal == null) return buf;
		for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).longValue());
		return buf;
	}

	/** Returns the SignalReader object of the given path String **/
	public static final SignalReader getReader(final String path) {
		return W7XSignalAccess.getAccessByPath(path).getReader_(path);
	}

	/** Returns the shape of the given Signal object **/
	public static final int[] getShape(final Signal signal) {
		final int ndims = signal.getDimensionCount();
		final int[] shape = new int[ndims];
		for(int i = 0; i < ndims; i++)
			shape[i] = signal.getDimensionSize(i);
		return shape;
	}

	/** Returns the data vector of the given Signal object as short[] **/
	public static final short[] getShort(final Signal signal) {
		if(signal == null) return new short[]{};
		return W7XSignalAccess.getShort(signal, ShortBuffer.allocate(signal.getSampleCount())).array();
	}

	public static final ShortBuffer getShort(final Signal signal, final ShortBuffer buf) {
		if(signal == null) return buf;
		for(final int[] i : IndexIterator.of(signal))
			buf.put(signal.getValue(Number.class, i).shortValue());
		return buf;
	}

	/** Returns the Signal object based on path String and from- and upto time stamps **/
	public static final Signal getSignal(final String path, final long from, final long upto) {
		return W7XSignalAccess.getSignal(path, from, upto, W7XSignalAccess.MAX_SAMPLES);
	}

	/** Returns the Signal object based on path String and from- and upto time stamps limited to a number of samples **/
	public static final Signal getSignal(final String path, final long from, final long upto, final int samples) {
		final TimeInterval ti = W7XSignalAccess.getTimeInterval(from, upto);
		final ReadOptions options = ReadOptions.firstNSamples(samples);
		return W7XSignalAccess.getSignal(path, ti, options);
	}

	/** Returns the Signal object based on path String and XP number **/
	public static final Signal getSignal(final String path, final String xp) {
		return W7XSignalAccess.getSignal(path, W7XSignalAccess.getTimeInterval(xp));
	}

	/** Returns the Signal object based on path String and TimeInterval **/
	public static final Signal getSignal(final String path, final TimeInterval interval) {
		final ReadOptions options = ReadOptions.firstNSamples(W7XSignalAccess.MAX_SAMPLES);
		return W7XSignalAccess.getSignal(path, interval, options);
	}

	/** Returns the Signal object based on path String and TimeInterval with ReadOptions **/
	public static final Signal getSignal(final String path, final TimeInterval interval, final ReadOptions options) {
		return W7XSignalAccess.getAccessByPath(path).getSignal_(path, interval, options);
	}

	/** Returns the data vector of the given Signal object as String[] **/
	public static final String[] getString(final Signal signal) {
		if(signal == null) return new String[]{};
		final int count = signal.getSampleCount();
		final String[] data = new String[count];
		if(count == 0) return data;
		final IndexIterator iter = IndexIterator.of(signal);
		for(int i = 0; i < count; i++)
			data[i] = signal.getValue(String.class, iter.next());
		return data;
	}

	/** Returns the TimeInterval defined by the XP number **/
	public static final TimeInterval getTimeInterval(final int shot) {
		if(shot <= 0) return TimeInterval.ALL;
		final StringBuilder xp = new StringBuilder(16).append("XP:20").append(shot / 1000).append('.').append(shot % 1000);
		return XPtools.xp2TimeInterval(xp.toString());
	}

	/** Returns the TimeInterval defined by from and upto **/
	public static final TimeInterval getTimeInterval(final long from, final long upto) {
		return TimeInterval.ALL.withStart(from).withEnd(upto);
	}

	/** Returns the TimeInterval defined by the XP number **/
	public static final TimeInterval getTimeInterval(final String xp) {
		return XPtools.xp2TimeInterval(xp);
	}

	/** Prints the 'help' example **/
	public static final String help() {
		System.out.println(W7XSignalAccess.help);
		return W7XSignalAccess.help;
	}

	/** Returns true if W7XSignalAccess is properly connected to the W7X-Archive **/
	public static final boolean isConnected() {
		return W7XSignalAccess.getAccess(W7XSignalAccess.databaselist[0]) != null;
	}

	/** Returns a list of Signal chunks multi-threaded read based on path String and TimeInterval **/
	public static final Signal[] readBoxes(final String path, final TimeInterval interval) {
		final ReadOptions options = ReadOptions.firstNSamples(W7XSignalAccess.MAX_SAMPLES);
		final TimeInterval[] interval_array;
		@SuppressWarnings("resource")
		final SignalReader reader = W7XSignalAccess.getReader(path);
		try{
			interval_array = reader.availableIntervals(interval).toArray(new TimeInterval[0]);
		}finally{
			reader.close();
		}
		final SignalFetcher[] fetchers = new SignalFetcher[interval_array.length];
		final Signal[] signals = new Signal[interval_array.length];
		for(int i = 0; i < fetchers.length; i++)
			fetchers[i] = new SignalFetcher(path, interval_array[i], options);
		for(final SignalFetcher fetcher : fetchers)
			fetcher.start();
		for(int i = 0; i < fetchers.length; i++)
			signals[i] = fetchers[i].getSignal();
		return signals;
	}

	/** Returns an instance of W7XSignalAccess based the given path String **/
	/*package*/static final W7XSignalAccess getAccessByPath(final String path) {
		String name;
		if(path.startsWith("/")){
			name = path.split("/", 3)[1];
			for(String known : W7XSignalAccess.databaselist)
				if(known.equalsIgnoreCase(name)){
					name = known;
					break;
				}
		}else name = W7XSignalAccess.databaselist[0];
		return W7XSignalAccess.getAccess(name);
	}

	/** Returns an instance of W7XSignalAccess based the given name String **/
	/*package*/static final W7XSignalAccess getAccess(final String name) {
		if(!W7XSignalAccess.access.containsKey(name)) W7XSignalAccess.access.put(name, W7XSignalAccess.newInstance(name));
		return W7XSignalAccess.access.get(name);
	}

	/** Returns a new instance of W7XSignalAccess linked to the given data base **/
	private static final W7XSignalAccess newInstance(final String database) {
		try{
			return new W7XSignalAccess(database);
		}catch(final Exception e){
			return null;
		}
	}
	public final String					database;
	private final SignalAddressBuilder	sab;
	private final SignalToolsFactory	stf;

	/** Constructs an instance of W7XSignalAccess linked to the given database **/
	public W7XSignalAccess(final String database){
		this.database = database;
		this.stf = ArchieToolsFactory.remoteArchive(database);
		this.sab = this.stf.makeSignalAddressBuilder(new String[0]);
	}

	public final SignalAddress getAddress(String path) {
		if(this.checkPath(path)) path = path.substring(this.database.length() + 1);
		return this.sab.newBuilder().path(path).build();
	}

	protected final void close() {
		this.stf.close();
	}

	private final boolean checkPath(final String path) {
		return path.startsWith(String.format("/%s/", this.database));
	}

	@SuppressWarnings("unchecked")
	private final List<SignalAddress> getList_(final String path, final TimeInterval interval) {
		@SuppressWarnings("resource")
		final SignalsTreeLister stl = this.stf.makeSignalsTreeLister();
		try{
			return (List<SignalAddress>)stl.listFor(interval, path);
		}finally{
			stl.close();
		}
	}

	private final SignalReader getReader_(final String path) {
		return this.stf.makeSignalReader(this.getAddress(path));
	}

	private final Signal getSignal_(final String path, final TimeInterval interval, final ReadOptions options) {
		@SuppressWarnings("resource")
		final SignalReader sr = this.getReader_(path);
		try{
			return sr.readSignal(interval, options);
		}finally{
			sr.close();
		}
	}
}
