package jscope.data.signal;

/* $Id$ */
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import debug.DEBUG;
import jscope.data.wavedata.WaveData;
import jscope.data.wavedata.WaveDataListener;
import jscope.data.wavedata.XYData;
import jscope.data.wavedata.XYWaveData;
import jscope.waveform.MultiWaveform;
import jscope.waveform.Waveform;

/**
 * The DataSignal class encapsulates a description of a are : name, marker, point step of marker, color index from an external color pallet, measure point error, offset and gain values. DataSignal is defined in a rectangular region.
 *
 * @see Waveform
 * @see MultiWaveform
 */
public final class Signal implements WaveDataListener{
	static public enum FreezeMode {
		FREE, BLOCK, SCROLL;
		public static final FreezeMode DEFAULT = FREE;
	}
	static public enum Marker {
		NONE, SQUARE, CIRCLE, CROSS, TRIANGLE, POINT;
		public static final Marker DEFAULT = NONE;

		public static Marker valueOf(int idx) {
			try{
				return Marker.values()[idx];
			}catch(IndexOutOfBoundsException e){
				return DEFAULT;
			}
		}
	}
	static public enum Mode1D {
		LINE("Line"), NOLINE("Noline"), STEP("Step");
		public static final Mode1D DEFAULT = LINE;

		public static Mode1D fromString(final String name) {
			if(name == null) return DEFAULT;
			try{
				return Mode1D.valueOf(name.toUpperCase());
			}catch(IllegalArgumentException e){
				return DEFAULT;
			}
		}
		public final String label;

		private Mode1D(String label){
			this.label = label;
		}

		public String toString() {
			return label;
		}
	}
	static public enum Mode2D {
		YX("y(x)"), ZX("z(x)"), ZY("z(y)"), CONTOUR("Contour"), IMAGE("Image"), PROFILE("Profile");
		public static final Mode2D DEFAULT = YX;

		public static Mode2D fromString(final String name) {
			if(name == null) return DEFAULT;
			try{
				if(name.contains("(")) return Mode2D.valueOf(name.substring(0, 2).toUpperCase());
				return Mode2D.valueOf(name.toUpperCase());
			}catch(IllegalArgumentException e){
				return DEFAULT;
			}
		}
		public final String label;

		private Mode2D(String label){
			this.label = label;
		}

		public String toString() {
			return label;
		}
	}
	public static final class RegionDescriptor{
		double	lowerBound, upperBound;
		double	resolution;

		public RegionDescriptor(final double lowerBound, final double upperBound, final double resolution){
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.resolution = resolution; // Number of points for this region / (upperBound - lowerBound)
		}

		@Override
		public final String toString() {
			return new StringBuilder(256).append("RegionDescriptor(").append(this.lowerBound).append(" - ").append(this.upperBound).append(" @ ").append(this.resolution).append(')').toString();
		}
	}
	public static final class ResolutionManager{
		Vector<RegionDescriptor> lowResRegions = new Vector<RegionDescriptor>();

		public ResolutionManager(){}

		public ResolutionManager(final ResolutionManager rm){
			for(int i = 0; i < rm.lowResRegions.size(); i++)
				this.lowResRegions.addElement(rm.lowResRegions.elementAt(i));
		}

		public final void addRegion(final RegionDescriptor newReg) {
			// New regions can only have increased resolution in case they intersect previous regions
			// Skip disjoint regions with lower bounds
			if(newReg.upperBound < newReg.lowerBound) System.err.println("# INTERNAL ERROR: LOWER BOUND > UPPER BOUND!!!!!");
			int idx;
			RegionDescriptor currRegion;
			for(idx = 0; idx < this.lowResRegions.size(); idx++){
				currRegion = this.lowResRegions.elementAt(idx);
				if(currRegion.upperBound > newReg.lowerBound) break;
			}
			if(idx == this.lowResRegions.size()) // All regions with lower bounds
			{
				if(DEBUG.D) System.out.println("Added region (" + newReg.lowerBound + "," + newReg.upperBound + "," + newReg.resolution + ") at bottom");
				this.lowResRegions.addElement(newReg);
				return;
			}
			// Check if the first region which is not all before this region has any intersection
			currRegion = this.lowResRegions.elementAt(idx);
			if(currRegion.lowerBound < newReg.lowerBound) if(currRegion.upperBound <= newReg.upperBound){
				currRegion.upperBound = newReg.lowerBound;
				if(DEBUG.D) System.out.println("updated region (" + currRegion.lowerBound + "," + currRegion.upperBound + ") ");
				idx++;
			}else // The new region is completely contained in currRegion
			{
				final double prevUpper = currRegion.upperBound;
				currRegion.upperBound = newReg.lowerBound;
				if(DEBUG.D) System.out.println("Updated region (" + currRegion.lowerBound + "," + currRegion.upperBound + ") ");
				idx++;
				this.lowResRegions.insertElementAt(newReg, idx);
				if(DEBUG.D) System.out.println("Added region (" + newReg.lowerBound + "," + newReg.upperBound + "," + newReg.resolution + ")");
				idx++;
				this.lowResRegions.insertElementAt(new RegionDescriptor(newReg.upperBound, prevUpper, currRegion.resolution), idx);
				if(DEBUG.D) System.out.println("Added region (" + newReg.upperBound + "," + prevUpper + "," + currRegion.resolution + ")");
				return; // done in this case
			}
			// Remove regions completely contained in the new one
			while(idx < this.lowResRegions.size() && this.lowResRegions.elementAt(idx).upperBound <= newReg.upperBound){
				if(DEBUG.D) System.out.println("Removed region (" + this.lowResRegions.elementAt(idx).lowerBound + "," + this.lowResRegions.elementAt(idx).upperBound + ")");
				this.lowResRegions.removeElementAt(idx);
			}
			// In case there is a overlapped region, adjust its lower bound
			if(idx < this.lowResRegions.size() && this.lowResRegions.elementAt(idx).lowerBound < newReg.upperBound){
				this.lowResRegions.elementAt(idx).lowerBound = newReg.upperBound;
				if(DEBUG.D) System.out.println("Updated region (" + this.lowResRegions.elementAt(idx).lowerBound + "," + this.lowResRegions.elementAt(idx).upperBound + ")");
			}
			this.lowResRegions.insertElementAt(newReg, idx);
			if(DEBUG.D) System.out.println("Added region (" + newReg.lowerBound + "," + newReg.upperBound + "," + newReg.resolution + ")");
			// Merge adjacent regions with same resolution (may happens due to the interval enlargements which occur in zooms)
			idx = 1;
			while(idx < this.lowResRegions.size()){
				final RegionDescriptor currReg = this.lowResRegions.elementAt(idx);
				final RegionDescriptor prevReg = this.lowResRegions.elementAt(idx - 1);
				if(prevReg.upperBound == currReg.lowerBound && prevReg.resolution == currReg.resolution){
					if(DEBUG.D) System.out.println("Regions at (" + prevReg.lowerBound + "," + prevReg.upperBound + ")  (" + currReg.lowerBound + "," + currReg.upperBound + ") merged");
					prevReg.upperBound = currReg.upperBound;
					this.lowResRegions.removeElementAt(idx);
				}else idx++;
			}
		}

		public final void appendRegion(final RegionDescriptor newReg) {
			if(newReg.upperBound < newReg.lowerBound) System.err.println("# INTERNAL ERROR IN APPEND: LOWER BOUND > UPPER BOUND!!!!!");
			if(this.lowResRegions.size() == 0){
				this.lowResRegions.addElement(newReg);
				return;
			}
			final RegionDescriptor lastReg = this.lowResRegions.lastElement();
			if(lastReg.resolution == newReg.resolution) lastReg.upperBound = newReg.upperBound;
			else{
				if(lastReg.upperBound > newReg.lowerBound) // System.err.println("# Warning: INTERNAL ERROR IN APPEND: NEW.LOWERBOUND < LAST.UPPERBOUND");
				    newReg.lowerBound = lastReg.upperBound;
				this.lowResRegions.addElement(newReg);
			}
		}

		/**
		 * Return a list of regions that need to be updated to target resolution.
		 * The resolution of each element is target resolution
		 *
		 * @param double
		 *            lowerBound
		 * @param double
		 *            upperBound
		 * @param double
		 *            resolution
		 * @return Vector<RegionDescriptor>
		 */
		public final Vector<RegionDescriptor> getLowerResRegions(final double lowerBound, final double upperBound, final double resolution) {
			if(DEBUG.D) System.out.println("getLowerResRegions(" + lowerBound + ", " + upperBound + ", " + resolution + ")");
			final Vector<RegionDescriptor> retRegions = new Vector<RegionDescriptor>();
			for(int i = 0; i < this.lowResRegions.size(); i++){
				// 3 cases to be handled
				final RegionDescriptor currReg = this.lowResRegions.elementAt(i);
				// 1) Lower bound is within interval
				if(currReg.lowerBound < upperBound && currReg.lowerBound > lowerBound){
					if(DEBUG.D) System.out.println("CASE 1: Lower bound is within interval " + i + " " + currReg.toString());
					if(currReg.resolution < resolution){
						// Adjust upper bound
						double currUpper = currReg.upperBound;
						if(currUpper > upperBound) currUpper = upperBound;
						final RegionDescriptor newReg = new RegionDescriptor(currReg.lowerBound, currUpper, resolution);
						if(DEBUG.D) System.out.println(newReg.toString());
						retRegions.addElement(newReg);
					}
				}
				// 2) Upper bound is within interval
				else if(currReg.upperBound < upperBound && currReg.upperBound > lowerBound){
					if(DEBUG.D) System.out.println("CASE 2: Upper bound is within interval " + i + " " + currReg.toString());
					if(currReg.resolution < resolution){
						// Adjust lower bound
						double currLower = currReg.lowerBound;
						if(currLower < lowerBound) currLower = lowerBound;
						final RegionDescriptor newReg = new RegionDescriptor(currLower, currReg.upperBound, resolution);
						if(DEBUG.D) System.out.println(newReg.toString());
						retRegions.addElement(newReg);
					}
				}
				// 3) The interval is fully within the current region
				else if(currReg.lowerBound < lowerBound && currReg.upperBound > upperBound){
					if(DEBUG.D) System.out.println("CASE 3: Fully within " + i + " " + currReg.toString());
					if(currReg.resolution < resolution){
						final RegionDescriptor newReg = new RegionDescriptor(lowerBound, upperBound, resolution);
						if(DEBUG.D) System.out.println(newReg.toString());
						retRegions.addElement(newReg);
					}
				}
			}
			return retRegions;
		}

		public final double[] getMinMaxX() {
			final double limits[] = new double[2];
			limits[0] = this.lowResRegions.elementAt(0).lowerBound;
			limits[1] = this.lowResRegions.elementAt(this.lowResRegions.size() - 1).upperBound;
			return limits;
		}

		public final boolean isEmpty() {
			return this.lowResRegions.size() == 0;
		}

		public final void resetRegions() {
			this.lowResRegions.clear();
		}
	} // End inner class ResolutionManager
	public static final int		AT_CREATION				= 1;
	public static final int		DEFAULT_CONTOUR_LEVEL	= 20;
	public static final int		DEFAULT_INC_SIZE		= 10000;
	public static final int		DO_NOT_UPDATE			= 4;
	public static final int		FIXED_LIMIT				= 2;
	public static final int		FUSO					= 0;
	public static final int[]	markerStepList			= new int[]{1, 5, 10, 20, 50, 100};
	public static final int		SIMPLE					= 0;

	private static final double[] appendArray(final double arr1[], final int sizeUsed, final double arr2[], final int incSize) {
		/*
		 * float arr[] = new float[arr1.length]; for(int i = 0; i < arr1.length; i++) arr[i] = (float)arr1[i]; return appendArray(arr, sizeUsed, arr2, incSize);
		 */
		if(arr1 == null) return arr2.clone();
		if(arr2 == null) return arr1.clone();
		double val[];
		if(arr1.length < sizeUsed + arr2.length){
			val = new double[arr1.length + arr2.length + incSize];
			System.arraycopy(arr1, 0, val, 0, sizeUsed);
		}else val = arr1;
		System.arraycopy(arr2, 0, val, sizeUsed, arr2.length);
		return val;
	}

	private static final float[] appendArray(final float arr1[], final int sizeUsed, final float arr2[], final int incSize) {
		if(arr1 == null) return arr2.clone();
		if(arr2 == null) return arr1.clone();
		float val[];
		if(arr1.length < sizeUsed + arr2.length){
			val = new float[arr1.length + arr2.length + incSize];
			System.arraycopy(arr1, 0, val, 0, sizeUsed);
		}else val = arr1;
		System.arraycopy(arr2, 0, val, sizeUsed, arr2.length);
		return val;
	}

	private static final int findIndex(final double d[], final double v, final int pIdx) {
		int i;
		if(v > d[pIdx]){
			for(i = pIdx; i < d.length; i++)
				if(d[i] >= v) break;
			if(i-- > 0) return i;
			return 0;
		}
		if(v < d[pIdx]){
			for(i = pIdx; i > 0; i--)
				if(d[i] <= v) break;
			if(++i < d.length) return i;
			return d.length - 1;
		}
		return pIdx;
	}

	private static final int findIndex(final float d[], final double vd, final int pIdx) {
		final float v = (float)vd;
		int i;
		if(v > d[pIdx]){
			for(i = pIdx; i < d.length; i++)
				if(d[i] >= v) break;
			if(i-- > 0) return i;
			return 0;
		}
		if(v < d[pIdx]){
			for(i = pIdx; i > 0; i--)
				if(d[i] <= v) break;
			if(++i < d.length) return i;
			return d.length - 1;
		}
		return pIdx;
	}

	private static final int getArrayIndex(final double arr[], final double d) {
		int i = -1;
		if(i == -1) for(i = 0; i < arr.length - 1; i++)
			if((d > arr[i] && d < arr[i + 1]) || d == arr[i]) break;
		return i;
	}

	private static final int getArrayIndex(final float arr[], final double d) {
		int i = -1;
		if(i == -1) for(i = 0; i < arr.length - 1; i++)
			if((d > arr[i] && d < arr[i + 1]) || d == arr[i]) break;
		return i;
	}

	private static final String toStringTime(final long time) {
		final DateFormat df = new SimpleDateFormat("HH:mm:sss");
		final Date date = new Date();
		date.setTime(time);
		return df.format(date).toString();
	}
	private int										num_points			= -1;
	private boolean									asym_error;
	private Color									color				= null;
	private int										color_idx			= 0;
	private double									contourLevels[];
	private Vector<Float>							contourLevelValues	= new Vector<Float>();
	private Vector<Vector<Vector<Point2D.Double>>>	contourSignals		= new Vector<Vector<Vector<Point2D.Double>>>();
	private ContourSignal							cs;
	private int										curr_x_yz_idx		= -1;
	private double									curr_x_yz_plot		= Double.NaN;
	private double									curr_xmax;
	private double									curr_xmin;
	private int										curr_y_xz_idx		= -1;
	private float									curr_y_xz_plot		= Float.NaN;
	private WaveData								data;
	private boolean									error;
	private boolean									find_NaN			= false;
	private boolean									fix_xmax			= false;
	private boolean									fix_xmin			= false;
	private boolean									fix_ymax			= false;
	private boolean									fix_ymin			= false;
	private double									freezedXMax;
	private double									freezedXMin;
	private FreezeMode								freezeMode			= Signal.FreezeMode.FREE;
	// private boolean full_load = false; // True if signal is re-sampled on server side to reduce net load
	private float									gain				= 1.0F;
	private int										img_xprev			= 0;
	private int										img_yprev			= 0;
	private boolean									increasing_x		= true;
	private boolean									interpolate			= true;
	private String									legend				= null;
	private WaveData								low_errorData;
	private float									lowError[];
	private Marker									marker				= Signal.Marker.NONE;
	private int										marker_step			= 1;
	private Mode1D									mode1D				= Mode1D.DEFAULT;
	private Mode2D									mode2D				= Mode2D.DEFAULT;
	private int										n_nans				= 0;
	private String									name;
	private int										nans[];
	private float									offset				= 0.0F;
	private final Vector<XYData>					pendingUpdatesV		= new Vector<XYData>();
	private int										prev_idx			= 0;
	private ResolutionManager						resolutionManager	= new ResolutionManager();
	private double									saved_xmax			= Double.POSITIVE_INFINITY;
	private double									saved_xmin			= Double.NEGATIVE_INFINITY;
	private double									saved_ymax			= Double.POSITIVE_INFINITY;
	private double									saved_ymin			= Double.NEGATIVE_INFINITY;
	private Vector<SignalListener>					signalListeners		= new Vector<SignalListener>();
	private double[]								sliceX;
	private float[]									sliceY;
	private int										startIndexToUpdate	= 0;
	private double									t_xmax;
	private double									t_xmin;
	private double									t_ymax;
	private double									t_ymin;
	private String									title;
	private WaveData								up_errorData;
	private int										updSignalSizeInc;
	private float									upError[];
	private double[]								x;
	private WaveData								x_data;
	private double									x2D_max;
	private double									x2D_min;
	private int										x2D_points			= 0;
	private String									xlabel;
	private boolean									xLimitsInitialized	= false;
	private long[]									xLong;
	private double									xmax				= Double.NEGATIVE_INFINITY;
	private double									xmin				= Double.POSITIVE_INFINITY;
	private double[]								xY2D;
	private float[]									y;
	private double									y2D_max;
	private double									y2D_min;
	private int										y2D_points			= 0;
	private String									ylabel;
	private double									ymax				= Double.NEGATIVE_INFINITY;
	private double									ymin				= Double.POSITIVE_INFINITY;
	private float[]									yY2D;
	private float[]									z;
	private double									z_value				= Double.NaN;
	private double									z2D_max;
	private double									z2D_min;
	private int										z2D_points			= 0;
	private String									zlabel;
	private float									zY2D[];
	private int										pending				= 0;

	/**
	 * Constructs a zero Signal with 2 points.
	 */
	public Signal(){
		this.error = this.asym_error = false;
		this.data = new XYWaveData(new double[]{0., 1.}, new float[]{0, 0});
		this.setAxis();
		this.saved_xmin = this.curr_xmin = this.xmin;
		this.saved_xmax = this.curr_xmax = this.xmax;
		this.increasing_x = true;
	}

	public Signal(final double _x[], final float _y[]){
		this.error = this.asym_error = false;
		this.data = new XYWaveData(_x, _y, this.num_points = (_x.length < _y.length) ? _x.length : _y.length);
		this.setAxis();
		this.saved_xmin = this.curr_xmin = this.xmin;
		this.saved_xmax = this.curr_xmax = this.xmax;
		this.saved_ymin = this.ymin;
		this.saved_ymax = this.ymax;
		this.checkIncreasingX();
	}

	/**
	 * Constructs and initialize a Signal with x and y array.
	 *
	 * @param _x
	 *            an array of x coordinates
	 * @param _y
	 *            an array of y coordinates
	 */
	public Signal(final float _x[], final float _y[]){
		this.error = this.asym_error = false;
		this.data = new XYWaveData(_x, _y, this.num_points = (_x.length < _y.length) ? _x.length : _y.length);
		this.setAxis();
		this.saved_xmin = this.curr_xmin = this.xmin;
		this.saved_xmax = this.curr_xmax = this.xmax;
		this.saved_ymin = this.ymin;
		this.saved_ymax = this.ymax;
		this.checkIncreasingX();
	}

	/**
	 * Constructs a Signal with x and y array, with n_points in a defined two-dimensional region.
	 *
	 * @param _x
	 *            an array of x coordinates
	 * @param _y
	 *            an array of y coordinates
	 * @param _n_points
	 *            number of Signal points
	 * @param _xmin
	 *            x minimum of region space
	 * @param _xmax
	 *            x maximum of region space
	 * @param _ymin
	 *            y minimum of region space
	 * @param _ymax
	 *            y maximum of region space
	 */
	public Signal(final float _x[], final float _y[], final int _n_points, final double _xmin, final double _xmax, double _ymin, final double _ymax){
		this.error = this.asym_error = false;
		this.data = new XYWaveData(_x, _y, _n_points);
		this.xLimitsInitialized = true;
		this.xmin = _xmin;
		this.xmax = _xmax;
		if(this.xmax - this.xmin < _x[1] - _x[0]) this.xmax = this.xmin + _x[1] - _x[0];
		this.saved_xmin = this.curr_xmin = this.xmin;
		this.saved_xmax = this.curr_xmax = this.xmax;
		if(this.xmax <= this.xmin) this.saved_xmax = this.xmax = this.xmin + (float)1E-6;
		if(_ymin > _ymax) _ymin = _ymax;
		this.saved_ymin = this.ymin = _ymin;
		this.saved_ymax = this.ymax = _ymax;
		this.curr_xmax = this.xmax;
		this.curr_xmin = this.xmin;
		this.setAxis(); // Here xmin and xmax have been passed, so override values computed by setAxis()
		this.ymin = this.saved_ymin;
		this.ymax = this.saved_ymax;
		this.saved_xmin = this.curr_xmin = this.xmin;
		this.saved_xmax = this.curr_xmax = this.xmax;
		this.checkIncreasingX();
		this.setAxis();
	}

	/**
	 * Constructs a Signal equal to argument Signal
	 *
	 * @param s
	 *            a Signal
	 */
	public Signal(final Signal s){
		this.error = s.error;
		if(this.error) this.upError = s.upError;
		this.asym_error = s.asym_error;
		if(this.asym_error) this.lowError = s.lowError;
		this.nans = s.nans;
		this.n_nans = s.n_nans;
		this.gain = s.gain;
		this.offset = s.offset;
		this.cs = s.cs;
		this.contourLevels = s.contourLevels;
		this.contourSignals = s.contourSignals;
		this.contourLevelValues = s.contourLevelValues;
		this.data = s.data; // WaveData is state less!!
		this.resolutionManager = new ResolutionManager(s.resolutionManager);
		this.xLimitsInitialized = s.xLimitsInitialized;
		this.saved_ymax = s.saved_ymax;
		this.ymax = s.ymax;
		this.saved_ymin = s.saved_ymin;
		this.ymin = s.ymin;
		this.saved_xmin = s.saved_xmin;
		this.curr_xmin = s.curr_xmin;
		this.xmin = s.xmin;
		this.saved_xmax = s.saved_xmax;
		this.curr_xmax = s.curr_xmax;
		this.xmax = s.xmax;
		this.fix_xmin = s.fix_xmin;
		this.fix_xmax = s.fix_xmax;
		this.fix_ymin = s.fix_ymin;
		this.fix_ymax = s.fix_ymax;
		this.x2D_max = s.x2D_max;
		this.x2D_min = s.x2D_min;
		this.y2D_max = s.y2D_max;
		this.y2D_min = s.y2D_min;
		this.z2D_max = s.z2D_max;
		this.z2D_min = s.z2D_min;
		if(this.xmax <= this.xmin) this.saved_xmax = this.xmax = this.xmin + 1E-6;
		this.increasing_x = s.increasing_x;
		this.marker = s.marker;
		this.marker_step = s.marker_step;
		this.color_idx = s.color_idx;
		this.color = s.color;
		this.interpolate = s.interpolate;
		this.name = s.name;
		this.mode1D = s.mode1D;
		this.mode2D = s.mode2D;
		this.xlabel = s.xlabel;
		this.ylabel = s.ylabel;
		this.zlabel = s.zlabel;
		this.title = s.title;
		// Deep copy buffered signals
		if(s.x != null){
			this.x = new double[s.x.length];
			System.arraycopy(s.x, 0, this.x, 0, this.x.length);
		}
		if(s.y != null){
			this.y = new float[s.y.length];
			System.arraycopy(s.y, 0, this.y, 0, this.y.length);
		}
		if(s.xLong != null){
			this.xLong = new long[s.xLong.length];
			System.arraycopy(s.xLong, 0, this.xLong, 0, this.xLong.length);
		}
		this.x_data = s.x_data;
		if(s.z != null){
			this.z = new float[s.z.length];
			System.arraycopy(s.z, 0, this.z, 0, this.z.length);
		}
		if(s.xY2D != null){
			this.xY2D = new double[s.xY2D.length];
			System.arraycopy(s.xY2D, 0, this.xY2D, 0, this.xY2D.length);
		}
		if(s.yY2D != null){
			this.yY2D = new float[s.yY2D.length];
			System.arraycopy(s.yY2D, 0, this.yY2D, 0, this.yY2D.length);
		}
		if(s.zY2D != null){
			this.zY2D = new float[s.zY2D.length];
			System.arraycopy(s.zY2D, 0, this.zY2D, 0, this.zY2D.length);
		}
		this.startIndexToUpdate = s.startIndexToUpdate;
		this.signalListeners = s.signalListeners;
		this.freezeMode = s.freezeMode;
	}

	public Signal(final WaveData data, final WaveData x_data, final double xminVal, final double xmaxVal, final WaveData lowErrData, final WaveData upErrData, final int num_points, Mode1D mode1d, Mode2D mode2d){
		if(DEBUG.M) System.out.println("Signal(" + data + ", " + x_data + ", " + xminVal + ", " + xmaxVal + ", " + lowErrData + ", " + upErrData + ")");
		this.error = (lowErrData != null || upErrData != null);
		this.asym_error = (lowErrData != null && upErrData != null);
		this.low_errorData = lowErrData;
		this.up_errorData = upErrData;
		this.num_points = num_points;
		if(xminVal != Double.NEGATIVE_INFINITY && xmaxVal != Double.POSITIVE_INFINITY){
			this.xLimitsInitialized = true;
			this.saved_xmin = this.xmin = this.curr_xmin = xminVal;
			this.saved_xmax = this.xmax = this.curr_xmax = xmaxVal;
		}
		this.data = data;
		this.x_data = x_data;
		this.data.addWaveDataListener(this);
		try {
			this.checkData(xminVal, xmaxVal, mode2d);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void AddAsymError(final WaveData up_error, final WaveData low_error) {
		this.error = this.asym_error = true;
		this.up_errorData = up_error;
		this.low_errorData = low_error;
	}

	public Vector<Vector<Point2D.Double>> addContourLevel(final double level) {
		Vector<Vector<Point2D.Double>> v;
		if(this.cs == null) this.cs = new ContourSignal(this);
		v = this.cs.contour(level);
		if(v.size() != 0){
			this.contourSignals.addElement(v);
			this.contourLevelValues.addElement(new Float(level));
		}
		return v;
	}

	/**
	 * Add a symmetric error bar.
	 *
	 * @param _error
	 *            an array of y measure error
	 */
	public void AddError(final WaveData in_error) {
		this.error = true;
		this.up_errorData = this.low_errorData = in_error;
	}

	public void adjustArraySizes() {
		if(this.x.length < this.y.length){
			final float[] newY = new float[this.x.length];
			System.arraycopy(this.y, 0, newY, 0, this.x.length);
			this.y = newY;
		}
		if(this.y.length < this.x.length){
			final double[] newX = new double[this.y.length];
			System.arraycopy(this.x, 0, newX, 0, this.y.length);
			this.x = newX;
		}
	}

	public void appendValues(final double x_in[], final float y_in[], final int numPoints[], final float time[]) {
		if(this.mode2D == Mode2D.YX || x_in.length != this.y.length || time == null || numPoints == null) return;
		int numProfile = 0;
		int xIdx, zIdx, yIdx;
		double x2D[] = this.data.getX2D();
		float y2D[] = this.data.getY2D();
		float z2D[] = this.data.getZ();
		xIdx = (x2D == null) ? 0 : this.x2D_points;
		yIdx = (y2D == null) ? 0 : this.y2D_points;
		zIdx = (z2D == null) ? 0 : this.z2D_points;
		if(numPoints.length == time.length) numProfile = time.length * 2;
		else if(numPoints.length > time.length) numProfile = numPoints.length * 2;
		else if(numPoints.length < time.length) numProfile = time.length * 2;
		final float t[] = new float[numProfile];
		for(int i = 0, j = 0; i < numProfile; i += 2, j++){
			t[i] = (time.length == 1) ? time[0] : time[j];
			t[i + 1] = (numPoints.length == 1) ? numPoints[0] : numPoints[j];
		}
		x2D = Signal.appendArray(x2D, this.x2D_points, x_in, this.updSignalSizeInc);
		this.x2D_points += x_in.length;
		y2D = Signal.appendArray(y2D, this.y2D_points, this.y, this.updSignalSizeInc);
		this.y2D_points += this.y.length;
		z2D = Signal.appendArray(z2D, this.z2D_points, t, this.updSignalSizeInc);
		this.z2D_points += t.length;
		this.data = new XYWaveData(x2D, y2D, z2D);
		this.setAxis(x2D, z2D, y2D, xIdx, zIdx, yIdx);
		if(this.xmin > this.x2D_min) this.xmin = this.x2D_min;
		if(this.ymin > this.y2D_min) this.ymin = this.y2D_min;
		if(this.xmax < this.x2D_max) this.xmax = this.x2D_max;
		if(this.ymax < this.y2D_max) this.ymax = this.y2D_max;
		this.curr_x_yz_plot = t[t.length - 2];
	}

	// NOTE this is called only by CompositeWaveDisplay and not by jScope
	public void appendValues(final float inX[], final float inY[]) {
		if(this.x == null || this.y == null) return;
		if(this.mode2D == Mode2D.YX){
			final int len = (inX.length < inY.length) ? inX.length : inY.length;
			final double newX[] = new double[this.x.length + len];
			final float newY[] = new float[this.x.length + len];
			for(int i = 0; i < this.x.length; i++){
				newX[i] = this.x[i];
				newY[i] = this.y[i];
			}
			for(int i = 0; i < len; i++){
				newX[this.x.length + i] = inX[i];
				newY[this.x.length + i] = inY[i];
			}
			this.data = new XYWaveData(newX, newY);
			try{
				final XYData xyData = this.data.getData(this.num_points);
				this.x = xyData.getX();
				this.y = xyData.getY();
				this.adjustArraySizes();
				this.xmax = this.x[this.x.length - 1];
			}catch(final Exception exc){/**/}
		}
	}

	/**
	 * Autoscale Signal.
	 */
	public final void Autoscale() {
		if(DEBUG.M) System.out.println("Signal.Autoscale()");
		this.AutoscaleX();
		this.AutoscaleY();
	}

	/**
	 * Autoscale x coordinates.
	 */
	public final void AutoscaleX() {
		if(DEBUG.M) System.out.println("Signal.AutoscaleX()");
		this.xmin = Double.POSITIVE_INFINITY;
		this.xmax = Double.NEGATIVE_INFINITY;
		this.unfreeze();
		switch (this.mode2D) {
			case IMAGE:
			case CONTOUR:
				this.xmax = this.x2D_max;
				this.xmin = this.x2D_min;
				break;
			case ZX:
			case ZY:
				this.AutoscaleX1D(this.sliceX);
				break;
			default:
				this.AutoscaleX1D(this.x);
		}
	}

	/**
	 * Autoscale y coordinates.
	 */
	public final void AutoscaleY() {
		this.AutoscaleY(this.xmin, this.xmax);
	}

	public final void AutoscaleY(final double min, final double max) {
		if(DEBUG.M) System.out.println("Signal.AutoscaleY(" + min + ", " + max + ")");
		this.ymin = Double.POSITIVE_INFINITY;
		this.ymax = Double.NEGATIVE_INFINITY;
		switch (this.mode2D) {
			case IMAGE:
			case CONTOUR:
				this.ymin = this.y2D_min;
				this.ymax = this.y2D_max;
				break;
			case ZX:
			case ZY:
				this.AutoscaleY1D(this.sliceX, this.sliceY, min, max);
				break;
			default:
				this.AutoscaleY1D(this.x, this.y, min, max);
		}
	}

	@Override
	public final void dataRegionUpdated(final Object obj_in, final boolean update_limits) {
		if(!(obj_in instanceof XYData)){
			if(obj_in instanceof Exception) this.title = ((Exception)obj_in).getMessage();
			else this.title = "ERROR READING DATA";
			this.fireSignalUpdated(SignalListener.UPDATE_REPAINT);
			return;
		}
		final XYData data_in = (XYData)obj_in;
		if(this.freezeMode == Signal.FreezeMode.BLOCK) // If zooming in ANY part of the signal
		{
			this.pendingUpdatesV.addElement(data_in);
			this.fireSignalUpdated(SignalListener.UPDATE_REPAINT);
			return;
		}
		if(this.x == null){
			this.x = data_in.getX();
			this.xLong = data_in.getXLong();
			this.y = data_in.getY();
			if(!this.fix_xmin) this.xmin = this.saved_xmin = data_in.getXMin();
			if(!this.fix_xmax) this.xmax = this.saved_xmax = data_in.getXMax();
		}else{
			int samplesBefore, offsetAfter;
			// find index of first element >= data.x_min; zero if none, this.x.length if all
			for(samplesBefore = 0; samplesBefore < this.x.length && this.x[samplesBefore] < data_in.getXMin(); samplesBefore++){/**/}
			// find index of last element <= data.x_max; zero if none, this.x.length if all
			for(offsetAfter = samplesBefore; offsetAfter < this.x.length && this.x[offsetAfter] <= data_in.getXMax(); offsetAfter++){/**/}
			final double[] newX = new double[samplesBefore + data_in.getX().length + this.x.length - offsetAfter];
			System.arraycopy(this.x, 0, newX, 0, samplesBefore);
			System.arraycopy(data_in.getX(), 0, newX, samplesBefore, data_in.getX().length);
			System.arraycopy(this.x, offsetAfter, newX, samplesBefore + data_in.getX().length, this.x.length - offsetAfter);
			final float[] newY = new float[newX.length];
			System.arraycopy(this.y, 0, newY, 0, samplesBefore);
			System.arraycopy(data_in.getY(), 0, newY, samplesBefore, data_in.getY().length);
			System.arraycopy(this.y, offsetAfter, newY, samplesBefore + data_in.getY().length, this.y.length - offsetAfter);
			this.x = newX;
			this.y = newY;
			if(this.xLong != null && data_in.getXLong() != null){
				final long[] newXLong = new long[newX.length];
				System.arraycopy(this.xLong, 0, newXLong, 0, samplesBefore);
				System.arraycopy(data_in.getXLong(), 0, newXLong, samplesBefore, data_in.getXLong().length);
				System.arraycopy(this.xLong, offsetAfter, newXLong, samplesBefore + data_in.getXLong().length, this.xLong.length - offsetAfter);
				this.xLong = newXLong;
			}
		}
		this.resolutionManager.addRegion(new RegionDescriptor(data_in.getXMin(), data_in.getXMax(), data_in.resolution));
		if(this.x.length == 0){
			this.title = "No DATA";
			this.fireSignalUpdated(SignalListener.UPDATE_REPAINT);
			return;
		}
		final boolean dataattached = this.x[0] > data_in.getXMin() || this.x[this.x.length - 1] < data_in.getXMax();
		if(dataattached) if(this.freezeMode == Signal.FreezeMode.SCROLL){
			final double delta = data_in.getXMax() - data_in.getXMin();
			this.xmin += delta;
			this.saved_xmax = this.xmax += delta;
		}else if(this.freezeMode == Signal.FreezeMode.FREE){
			if(!this.fix_xmin && this.saved_xmin > data_in.getXMin()) this.xmin = this.saved_xmin = data_in.getXMin();
			if(!this.fix_xmax && this.saved_xmax < data_in.getXMax()) this.xmax = this.saved_xmax = data_in.getXMax();
		}
		final double old_ymin = this.ymin, old_ymax = this.ymax;
		if(!this.fix_ymin || !this.fix_ymax){
			this.AutoscaleY();
			if(this.fix_ymin) this.ymin = old_ymin;
			else if(this.saved_ymin == Double.NEGATIVE_INFINITY || this.saved_ymin > this.ymin) this.saved_ymin = this.ymin;
			if(this.fix_ymax) this.ymax = old_ymax;
			else if(this.saved_ymax == Double.POSITIVE_INFINITY || this.saved_ymax < this.ymax) this.saved_ymax = this.ymax;
		}
		this.fireSignalUpdated(dataattached || update_limits ? SignalListener.UPDATE_LIMITS : SignalListener.UPDATE_REPAINT);
	}

	public final void decShow() {
		switch(this.mode2D){
			case ZX:
				this.decShowZX();
				break;
			case ZY:
				this.decShowZY();
				break;
			case PROFILE:
				// decShowProfile();
				break;
			default:
				break;
		}
	}

	/**
	 * Return index of nearest signal point to argument (curr_x, curr_y) point.
	 *
	 * @param curr_x
	 *            value
	 * @param curr_y
	 *            value
	 * @return index of signal point
	 */
	public final int FindClosestIdx(final double curr_x, final double curr_y) {
		if(DEBUG.M) System.out.println("FindClosestIdx(" + curr_x + ", " + curr_y + ")");
		if(this.x == null || this.x.length < 1) return -1;
		try{
			double min_dist, curr_dist;
			int min_idx;
			int i = 0;
			if(this.mode2D == Signal.Mode2D.IMAGE || this.mode2D == Signal.Mode2D.CONTOUR){
				this.img_xprev = Signal.findIndex(this.x, curr_x, this.img_xprev);
				this.img_yprev = Signal.findIndex(this.y, curr_y, this.img_yprev);
				if(this.img_xprev > this.y.length) return this.img_xprev - 6;
				return this.img_xprev;
			}
			double currX[];
			if (this.mode2D == Signal.Mode2D.YX) currX = this.x;
			else if(this.mode2D == Signal.Mode2D.ZX || this.mode2D == Signal.Mode2D.ZY) currX = this.sliceX;
			else{
				final double xf[] = this.x;
				currX = new double[xf.length];
				for(int idx = 0; idx < xf.length; idx++)
					currX[idx] = xf[idx];
			}
			if(this.increasing_x || this.mode2D != Signal.Mode2D.YX){
				if(currX == null) return -1;
				if(this.prev_idx >= currX.length) this.prev_idx = currX.length - 1;
				if(curr_x > currX[this.prev_idx]){
					for(i = this.prev_idx; i < currX.length; i++)
						if(currX[i] >= curr_x) break;
					if(i > 0) i--;
					this.prev_idx = i;
					return i;
				}
				if(curr_x < currX[this.prev_idx]){
					for(i = this.prev_idx; i > 0; i--)
						if(currX[i] <= curr_x) break;
					this.prev_idx = i;
					return i;
				}
				return this.prev_idx;
			}
			// Handle below x values not in ascending order
			if(curr_x > this.curr_xmax){
				for(min_idx = 0; min_idx < currX.length; min_idx++)
					if(currX[min_idx] == this.curr_xmax) break;
				if(min_idx == currX.length) min_idx--;
				return min_idx;
			}
			if(curr_x < this.curr_xmin){
				for(min_idx = 0; min_idx < currX.length; min_idx++)
					if(currX[min_idx] == this.curr_xmin) break;
				if(min_idx == currX.length) min_idx--;
				return min_idx;
			}
			min_idx = 0;
			min_dist = Double.POSITIVE_INFINITY;
			this.find_NaN = false;
			for(i = 0; i < this.x.length - 1; i++){
				if(Float.isNaN(this.y[i])){
					this.find_NaN = true;
					continue;
				}
				if(curr_x > currX[i] && curr_x < currX[i + 1] || curr_x < currX[i] && curr_x > currX[i + 1] || currX[i] == currX[i + 1]){
					curr_dist = (curr_x - currX[i]) * (curr_x - currX[i]) + (curr_y - this.y[i]) * (curr_y - this.y[i]);
					// Patch to elaborate strange RFX signal (roprand bar error signal)
					if(currX[i] != currX[i + 1] && !Float.isNaN(this.y[i + 1])) curr_dist += (curr_x - currX[i + 1]) * (curr_x - currX[i + 1]) + (curr_y - this.y[i + 1]) * (curr_y - this.y[i + 1]);
					if(curr_dist < min_dist){
						min_dist = curr_dist;
						min_idx = i;
					}
				}
			}
			return min_idx;
		}catch(final Exception exc){
			System.err.println("Signal.FindClosestIdx: " + exc);
			exc.getStackTrace();
		}
		return -1;
	}

	public final boolean findNaN() {
		return this.find_NaN;
	}

	public final void freeze() {
		if(this.isLongX() && (this.xLong.length == 0 || this.xmax > this.xLong[this.xLong.length - 1])) this.freezeMode = Signal.FreezeMode.SCROLL;
		else this.freezeMode = Signal.FreezeMode.BLOCK;
		this.freezedXMin = this.xmin;
		this.freezedXMax = this.xmax;
	}

	@SuppressWarnings("static-method")
	public final boolean fullPaint() {
		return true;
	}

	public final float getClosestX(final double x_in) {
		if(this.mode2D == Signal.Mode2D.IMAGE || this.mode2D == Signal.Mode2D.CONTOUR){
			this.img_xprev = Signal.findIndex(this.x, x_in, this.img_xprev);
			return (float)this.x[this.img_xprev];
		}
		return Float.NaN;
	}

	public final float getClosestY(final double y_in) {
		if(this.mode2D == Signal.Mode2D.IMAGE || this.mode2D == Signal.Mode2D.CONTOUR){
			this.img_yprev = Signal.findIndex(this.y, y_in, this.img_yprev);
			return this.y[this.img_yprev];
		}
		return Float.NaN;
	}

	public final Color getColor() {
		return this.color;
	}

	public final int getColorIdx() {
		return this.color_idx;
	}

	public final Vector<Float> getContourLevelValues() {
		return this.contourLevelValues;
	}

	public final Vector<Vector<Vector<Point2D.Double>>> getContourSignals() {
		return this.contourSignals;
	}

	public final double getCurrentXmax() {
		return this.curr_xmax;
	}

	public final double getCurrentXmin() {
		return this.curr_xmin;
	}

	public FreezeMode getFreezeMode() {
		return this.freezeMode;
	}

	public final float getGain() {
		return this.gain;
	}

	public final boolean getInterpolate() {
		return this.interpolate;
	}

	public final String getLegend() {
		return this.legend;
	}

	public final float[] getLowError() {
		return this.lowError;
	}

	public final Marker getMarker() {
		return this.marker;
	}

	public final int getMarkerStep() {
		return (this.marker == Signal.Marker.POINT) ? 1 : this.marker_step;
	}

	public final Mode1D getMode1D() {
		return this.mode1D;
	}

	public final Mode2D getMode2D() {
		return this.mode2D;
	}

	public final String getName() {
		return this.name;
	}

	public final int[] getNaNs() {
		return this.nans;
	}

	public final int getNumNaNs() {
		return this.n_nans;
	}

	public final int getNumPoints() {
		try{
			if(this.mode2D == Signal.Mode2D.ZY || this.mode2D == Signal.Mode2D.ZX) return (this.sliceX == null) ? 0 : this.sliceX.length;
			if(this.x != null && this.y != null) return (this.x.length < this.y.length) ? this.x.length : this.y.length;
		}catch(final Exception exc){
			System.err.println("getNumPoints(): " + exc);
			exc.printStackTrace();
		}
		return 0;
	}

	public final float getOffset() {
		return this.offset;
	}

	public final double getOriginalYmax() {
		return this.saved_ymax;
	}

	public final double getOriginalYmin() {
		return this.saved_ymin;
	}

	public final String getStringOfXinYZplot() {
		if(this.isLongX()) return Signal.toStringTime((long)this.curr_x_yz_plot);
		return "" + this.curr_x_yz_plot;
	}

	public final String getTitlelabel() {
		return this.title;
	}

	public final int getUpdSignalSizeInc() {
		return this.updSignalSizeInc;
	}

	public final float[] getUpError() {
		return this.upError;
	}

	public final double[] getX() {
		if(this.mode2D == Signal.Mode2D.ZX || this.mode2D == Signal.Mode2D.ZY) return this.sliceX;
		return this.x;
	}

	public final double getX(final int idx) {
		final double x[] = this.getX();  
		if (idx<0 || idx>=x.length) return Double.NaN;
		return x[idx];
	}

	public final double[] getX2D() {
		if(this.x == null) this.x = this.data.getX2D();
		return this.x;
	}

	public final double getX2Dmax() {
		return this.x2D_max;
	}

	public final double getX2Dmin() {
		return this.x2D_min;
	}

	public final double getXinYZplot() {
		return this.curr_x_yz_plot;
	}

	public final String getXlabel() {
		return this.xlabel;
	}

	public final double getXmax() {
		return this.xmax;
	}

	public final double getXmin() {
		return this.xmin;
	}

	public final float[] getY() {
		if(this.mode2D == Signal.Mode2D.ZX || this.mode2D == Signal.Mode2D.ZY) return this.sliceY;
		return this.y;
	}

	public final float getY(final int idx) {
		final float y[] = this.getY();
		if (y == null || idx<0 || idx>=y.length) return Float.NaN;
		return y[idx];
	}

	public final float[] getY2D() {
		if(this.y == null) this.y = this.data.getY2D();
		return this.y;
	}

	public final double getY2Dmax() {
		return this.y2D_max;
	}

	public final double getY2Dmin() {
		return this.y2D_min;
	}

	public final float getYinXZplot() {
		return this.curr_y_xz_plot;
	}

	public final String getYlabel() {
		return this.ylabel;
	}

	public final double getYmax() {
		return this.ymax;
	}

	public final double getYmin() {
		return this.ymin;
	}

	public final float[] getZ() {
		if(this.z == null) this.z = this.data.getZ();
		return this.z;
	}

	public final float getZ(final int idx) {
		if(this.z == null) this.z = this.data.getZ();
		return this.z[idx];
	}

	public final float[][] getZ2D() {
		final float zOut[][] = new float[this.x.length][this.y.length];
		int k;
		for(int i = 0; i < this.x.length; i++)
			for(int j = 0; j < this.y.length; j++){
				k = j * this.x.length + i;
				if(k < this.z.length) zOut[i][j] = this.z[k];
			}
		return zOut;
	}

	public final double getZ2Dmax() {
		return this.z2D_max;
	}

	public final double getZ2Dmin() {
		return this.z2D_min;
	}

	public final String getZlabel() {
		return this.zlabel;
	}

	public final double getZValue() {
		if(this.mode2D == Signal.Mode2D.IMAGE){
			final int idx = this.img_xprev * this.y.length + this.img_yprev;
			if(this.z != null && idx < this.z.length) return this.z[idx];
		}else if(this.mode2D == Signal.Mode2D.CONTOUR) return this.z_value;
		return Float.NaN;
	}

	public final boolean hasAsymError() {
		return this.asym_error;
	}

	public final boolean hasError() {
		return this.error;
	}

	public boolean hasPending() {
		synchronized(this){
			return this.pending > 0;
		}
	}

	@SuppressWarnings("static-method")
	public final boolean hasX() {
		return true;
	}

	public final void incShow() {
		switch(this.mode2D){
			case ZX:
				this.incShowXZ();
				break;
			case ZY:
				this.incShowYZ();
				break;
			case PROFILE:
				// incShowProfile();
				break;
			default:
				break;
		}
	}

	/*
	private final boolean isFullLoad() {
	    return this.full_load;
	}
	 */
	public final boolean isIncreasingX() {
		return this.increasing_x;
	}

	public final boolean isLongX() {
		return ((this.mode2D == Signal.Mode2D.YX || this.mode2D == Signal.Mode2D.ZX || this.mode2D == Signal.Mode2D.IMAGE) && this.xLong != null);
	}

	public final boolean isLongXForLabel() {
		return this.data.isXLong();
	}

	@Override
	public final void legendUpdated(final String name_in) {
		this.setLegend(name_in);
	}

	public final void registerSignalListener(final SignalListener listener) {
		synchronized(this.signalListeners){
			if(!this.signalListeners.contains(listener)) this.signalListeners.addElement(listener);
		}
	}

	/**
	 * Reset scale, return to the initial two dimensional region
	 */
	public final void ResetScales() {
		this.unfreeze();
		this.xmax = this.saved_xmax;
		this.xmin = this.saved_xmin;
		this.ymax = this.saved_ymax;
		this.ymin = this.saved_ymin;
	}

	public final void resetSignalData() {
		this.x2D_points = 0;
		this.y2D_points = 0;
		this.z2D_points = 0;
		final double new_x[] = new double[]{0., 1.};
		final float new_y[] = new float[]{0, 0};
		this.data = new XYWaveData(new_x, new_y);
		this.low_errorData = null;
		this.up_errorData = null;
		this.startIndexToUpdate = 0;
	}

	/**
	 * Reset x scale, return to original x range two dimensional region
	 */
	public final void ResetXScale() {
		this.unfreeze();
		this.xmax = this.saved_xmax;
		this.xmin = this.saved_xmin;
	}

	/**
	 * Reset x scale, return to the initial y range two dimensional region
	 */
	public final void ResetYScale() {
		this.ymax = this.saved_ymax;
		this.ymin = this.saved_ymin;
	}

	public final void setAttributes(final Signal s) {
		this.color = s.getColor();
		this.color_idx = s.getColorIdx();
		this.gain = s.getGain();
		this.interpolate = s.getInterpolate();
		this.marker = s.getMarker();
		this.marker_step = s.getMarkerStep();
		this.offset = s.getOffset();
		this.name = s.getName();
	}

	public final void setAttributes(final String name, final int color_idx, final Marker marker, final int marker_step, final boolean interpolate) {
		this.setMarker(marker);
		this.setMarkerStep(marker_step);
		this.setInterpolate(interpolate);
		this.setColorIdx(color_idx);
		this.setName(name);
	}

	public final boolean setAxis() {
		try{
			int i;
			// If the signal dimension is 2 or the x axis are not increasing, the signal is assumed to be completely in memory
			// and no further readout from data is performed
			if(this.mode2D != Mode2D.YX || !this.increasing_x) return true;
			// Check if the signal is fully available (i.e. has already been read without X limits)
			if(!this.resolutionManager.isEmpty()){
				final double minMax[] = this.resolutionManager.getMinMaxX();
				if(minMax[0] == Double.NEGATIVE_INFINITY && minMax[1] == Double.POSITIVE_INFINITY){
					this.xLimitsInitialized = true;
					this.xmin = this.x[0];
					this.xmax = this.x[this.x.length - 1];
					return true;
				}
			}
			// resolutionManager.resetRegions();
			final XYData xyData = this.data.getData(this.num_points);
			if(xyData == null) return false;
			this.x = xyData.getX();
			this.y = xyData.getY();
			this.adjustArraySizes();
			this.increasing_x = xyData.increasingX;
			if(this.increasing_x) this.resolutionManager.addRegion(new RegionDescriptor(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, this.x.length / (this.x[this.x.length - 1] - this.x[0])));
			if(this.data.isXLong()) this.xLong = xyData.getXLong();
			this.curr_xmin = this.xmin = xyData.getXMin();
			this.curr_xmax = this.xmax = xyData.getXMax();
			this.ymax = this.ymin = this.y[0];
			for(i = 0; i < this.x.length; i++){
				if(Float.isNaN(this.y[i]) && this.n_nans < 100) this.nans[this.n_nans++] = i;
				if(this.y[i] > this.ymax) this.ymax = this.y[i];
				if(this.ymin > this.y[i]) this.ymin = this.y[i];
			}
			return true;
		}catch(final Exception e){
			if(DEBUG.E) System.err.println("Signal.setAxis Exception: " + e);
			this.xmax = Double.NEGATIVE_INFINITY;
			this.xmin = Double.POSITIVE_INFINITY;
			this.ymax = Double.NEGATIVE_INFINITY;
			this.ymin = Double.POSITIVE_INFINITY;
			return false;
		}
	}

	public final void setAxis(final double x2D[], final float z2D[], final float y2D[]) {
		this.x2D_max = this.x2D_min = x2D[0];
		this.z2D_max = this.z2D_min = z2D[0];
		this.y2D_max = this.y2D_min = y2D[0];
		this.setAxis(x2D, z2D, y2D, 0, 0, 0);
	}

	public final void setAxis(final double x2D[], final float z2D[], final float y2D[], final int xIdx, final int zIdx, final int yIdx) {
		int i;
		for(i = xIdx; i < x2D.length; i++){
			if(x2D[i] > this.x2D_max) this.x2D_max = x2D[i];
			if(this.x2D_min > x2D[i]) this.x2D_min = x2D[i];
		}
		for(i = zIdx; i < z2D.length; i++){
			if(z2D[i] > this.z2D_max) this.z2D_max = z2D[i];
			if(this.z2D_min > z2D[i]) this.z2D_min = z2D[i];
		}
		for(i = yIdx; i < y2D.length; i++){
			if(y2D[i] > this.y2D_max) this.y2D_max = y2D[i];
			if(this.y2D_min > y2D[i]) this.y2D_min = y2D[i];
		}
	}

	public final void setCalibrate(final float gain, final float offset) {
		this.gain = gain;
		this.offset = offset;
		this.setAxis();
	}

	public final void setColor(final Color value) {
		this.color = value;
	}

	public final void setColorIdx(final int value) {
		this.color = null;
		this.color_idx = value;
	}

	public void setFreezeMode(final FreezeMode freezeMode) {
		this.freezeMode = freezeMode;
	}

	/*
	public final void setFullLoad(final boolean full_load) {
	    this.full_load = full_load;
	}
	 */
	public final void setInterpolate(final boolean value) {
		this.interpolate = value;
	}

	public final void setLabels(final String title, final String xlabel, final String ylabel, final String zlabel) {
		this.title = title;
		this.xlabel = xlabel;
		this.ylabel = ylabel;
		this.zlabel = zlabel;
	}

	public final void setLegend(final String legend) {
		this.legend = legend;
	}

	public final void setMarker(final Marker value) {
		this.marker = value;
	}

	public final void setMarker(final String name) {
		if(name == null) this.setMarker(Marker.NONE);
		else try{
			final Marker mark = Signal.Marker.valueOf(name.toUpperCase());
			this.setMarker(mark);
		}catch(IllegalArgumentException e){
			this.setMarker(Marker.NONE);
		}
	}

	public final void setMarkerStep(final int value) {
		this.marker_step = value;
	}

	public final void setMode1D(final Mode1D mode) {
		if(mode == null) this.mode1D = Mode1D.DEFAULT;
		else this.mode1D = mode;
		switch(this.mode1D){
			case LINE:
				this.interpolate = true;
				break;
			case NOLINE:
				this.interpolate = false;
				break;
			case STEP:
				this.interpolate = true;
				break;
		}
	}

	public final void setMode2D(final Mode2D mode) {
		switch(mode){
			case YX:
				break;
			case IMAGE:
				this.setMode2D(mode, 0);
				break;
			case ZX:
				this.setMode2D(mode, this.y[0]);
				break;
			case ZY:
				double v = this.x[0];
				if(!Double.isNaN(this.curr_x_yz_plot)) v = this.curr_x_yz_plot;
				this.setMode2D(mode, v);
				break;
			case CONTOUR:
				this.setMode2D(mode, 0);
				break;
			case PROFILE:
				/*
				 * if(z2D != null && z2D.length > 0) { float v1 = z2D[0]; if (!Float.isNaN(curr_x_yz_plot)) v1 = curr_x_yz_plot; setMode2D(mode, v1); }
				 */
				break;
		}
	}

	public final void setMode2D(final Mode2D mode, final double value) {
		this.mode2D = mode;
		switch(mode){
			case YX:
				return;
			case IMAGE:
				if(this.saved_ymin == Double.NEGATIVE_INFINITY) this.saved_ymin = this.ymin = this.y2D_min;
				else this.ymin = this.saved_ymin;
				if(this.saved_ymax == Double.POSITIVE_INFINITY) this.saved_ymax = this.ymax = this.y2D_max;
				else this.ymax = this.saved_ymax;
				if(this.saved_xmin == Double.NEGATIVE_INFINITY) this.saved_xmin = this.xmin = this.x2D_min;
				else this.xmin = this.saved_xmin;
				if(this.saved_xmax == Double.POSITIVE_INFINITY) this.saved_xmax = this.xmax = this.x2D_max;
				else this.xmax = this.saved_xmax;
				break;
			case ZX:
				this.showXZ((float)value);
				break;
			case ZY:
				this.prev_idx = 0;
				this.showYZ((float)value);
				break;
			case CONTOUR:
				this.initContour();
				break;
			case PROFILE:
				// prev_idx = 0; showProfile(mode, value);
				break;
		}
		this.curr_x_yz_plot = Float.NaN;
		this.curr_y_xz_plot = Float.NaN;
		this.curr_x_yz_idx = -1;
		this.curr_y_xz_idx = -1;
	}

	public final void setName(final String value) {
		if(value != null && value.length() != 0) this.name = new String(value);
	}

	public final void setNumPoints(final int num_points) {
		this.num_points = num_points;
	}

	public final void setStartIndexToUpdate() {
		if(this.x != null) this.startIndexToUpdate = this.x.length;
	}

	public final void setUpdSignalSizeInc(int updSignalSizeInc) {
		if(updSignalSizeInc <= 0) updSignalSizeInc = Signal.DEFAULT_INC_SIZE;
		this.updSignalSizeInc = updSignalSizeInc;
	}

	public final void setXinYZplot(final float curr_x_yz_plot) {
		this.curr_x_yz_plot = curr_x_yz_plot;
	}

	public final void setXLimits(final double xmin, final double xmax, final int mode) {
		if(DEBUG.M) System.out.println("setXLimits(" + xmin + ", " + xmax + ", " + mode + ")");
		if(this.freezeMode != Signal.FreezeMode.FREE){// If adding samples when freeze
			if(DEBUG.D) System.out.println("unfreezed mode");
			this.xmin = xmin;
			this.xmax = xmax;
			return;
		}
		this.xLimitsInitialized = true;
		if(xmin != Double.NEGATIVE_INFINITY){
			this.xmin = xmin;
			if((mode & (Signal.AT_CREATION | Signal.FIXED_LIMIT)) != 0) this.saved_xmin = xmin;
			if((mode & Signal.FIXED_LIMIT) != 0) this.fix_xmin = true;
		}
		if(xmax != Double.POSITIVE_INFINITY){
			this.xmax = xmax;
			if((mode & (Signal.AT_CREATION | Signal.FIXED_LIMIT)) != 0) this.saved_xmax = xmax;
			if((mode & Signal.FIXED_LIMIT) != 0) this.fix_xmax = true;
		}
		double actXMin = xmin;
		if(actXMin == Double.NEGATIVE_INFINITY) actXMin = this.xmin;
		double actXMax = xmax;
		if(actXMax == Double.POSITIVE_INFINITY) actXMax = this.xmax;
		/* Enlarge by 1/20 */
		final double enlargeFactor = 40;
		actXMax += (actXMax - actXMin) / enlargeFactor;
		actXMin -= (actXMax - actXMin) / enlargeFactor;
		final double actResolution = this.num_points / (actXMax - actXMin);
		if(!this.increasing_x) return; // Dynamic re-sampling only for "classical" signals
		if(this.up_errorData != null || this.low_errorData != null) return; // Dynamic re-sampling only without error bars
		final Vector<RegionDescriptor> lowResRegions = this.resolutionManager.getLowerResRegions(actXMin, actXMax, actResolution);
		for(int i = 0; i < lowResRegions.size(); i++){
			final RegionDescriptor currReg = lowResRegions.elementAt(i);
			final double currLower = currReg.lowerBound;
			final double currUpper = currReg.upperBound;
			// Error bars are assumed to be used only for small signals and should not arrive here. In case make it not asynchronous
			/*
			 * if(up_errorData != null) { try { XYData currError = up_errorData.getData(xmin, xmax, Integer.MAX_VALUE); upError = currError.y; }catch(Exception exc) { System.out.println("Cannot evaluate error: "+ exc); } } if(low_errorData != null) { try {
			 * XYData currError = low_errorData.getData(xmin, xmax, Integer.MAX_VALUE); lowError = currError.y; }catch(Exception exc) { System.out.println("Cannot evaluate error: "+ exc); } }
			 */
			if(((mode & Signal.DO_NOT_UPDATE) == 0) && (currLower != this.saved_xmin || currUpper != this.saved_xmax || (mode & Signal.AT_CREATION) == 0)) this.data.getDataAsync(currLower, currUpper, this.num_points);
		}
	}

	public final void setYinXZplot(final float curr_y_xz_plot) {
		this.curr_y_xz_plot = curr_y_xz_plot;
	}

	public final void setYlimits(final double ymin, final double ymax) {
		if(Double.isInfinite(ymax)){
			this.ymax = ymax;
			this.fix_ymax = true;
		}else this.fix_ymax = false;
		if(Double.isInfinite(ymin)){
			this.ymin = ymin;
			this.fix_ymin = true;
		}else this.fix_ymin = false;
	}

	public final void setYmax(final double ymax, final int mode) {
		if(ymax == Double.POSITIVE_INFINITY) return;
		this.ymax = ymax;
		if((mode & (Signal.AT_CREATION | Signal.FIXED_LIMIT)) != 0) this.saved_ymax = ymax;
		if((mode & Signal.FIXED_LIMIT) != 0) this.fix_ymax = true;
	}

	public final void setYmin(final double ymin, final int mode) {
		if(ymin == Double.NEGATIVE_INFINITY) return;
		this.ymin = ymin;
		if((mode & (Signal.AT_CREATION | Signal.FIXED_LIMIT)) != 0) this.saved_ymin = ymin;
		if((mode & Signal.FIXED_LIMIT) != 0) this.fix_ymin = true;
	}

	public final void showXZ(final double xd) {
		if(this.curr_y_xz_plot == xd) return;
		final int i = Signal.getArrayIndex(this.y, xd);
		this.showXZ(i);
	}

	public final void showXZ(final int idx) {
		final float[] y2d = this.y;
		double[] x2d = this.x;
		// if ( (idx >= x2d.length || idx == curr_y_xz_idx) &&
		if((idx >= y2d.length || idx == this.curr_y_xz_idx) && this.mode2D == Signal.Mode2D.ZX) return;
		this.prev_idx = 0;
		this.curr_y_xz_plot = y2d[idx];
		this.curr_y_xz_idx = idx;
		this.curr_x_yz_plot = Float.NaN;
		this.curr_x_yz_idx = -1;
		if(this.zY2D != null){
			x2d = new double[this.x.length];
			this.curr_xmin = this.curr_xmax = this.zY2D[this.x.length * idx];
			for(int j = 0; j < this.x.length; j++){
				x2d[j] = this.zY2D[this.x.length * idx + j];
				if(x2d[j] > this.curr_xmax) this.curr_xmax = x2d[j];
				else if(x2d[j] < this.curr_xmin) this.curr_xmin = x2d[j];
			}
		}
		this.sliceX = new double[x2d.length];
		this.sliceY = new float[x2d.length];
		float sliceMin, sliceMax;
		if (this.z == null) {
			sliceMin = sliceMax = 0;
		} else {
			final int zLen = this.z == null ? 0 : this.z.length;
			// sliceMin = sliceMax = z[ y2d.length * idx];
			sliceMin = sliceMax = this.z == null ? 0 : this.z[x2d.length * idx];
			for(int j = 0; j < x2d.length; j++){
				this.sliceX[j] = x2d[j];
				// int k = y2d.length * idx + j;
				final int k = x2d.length * idx + j;
				if(k >= zLen) break;
				this.sliceY[j] = this.z[k];
				if(sliceMin > this.z[k]) sliceMin = this.z[k];
				if(sliceMax < this.z[k]) sliceMax = this.z[k];
			}
		}
		this.error = this.asym_error = false;
		this.mode2D = Signal.Mode2D.ZX;
		if(!this.fix_xmin) this.saved_xmin = this.curr_xmin = this.xmin = this.x2D_min;
		// saved_xmin = curr_xmin;
		if(!this.fix_xmax) this.saved_xmax = this.curr_xmax = this.xmax = this.x2D_max;
		// saved_xmax = curr_xmax;
		if(!this.fix_ymin) this.saved_ymin = this.ymin = sliceMin;
		if(!this.fix_ymax) this.saved_ymax = this.ymax = sliceMax;
		// Assumed that for 2D data, dimensions are increasing
		this.increasing_x = true;
	}

	public final void showYZ(final double t) {
		if(this.curr_x_yz_plot == t && this.mode2D == Signal.Mode2D.ZY) return;
		final int i = Signal.getArrayIndex(this.x, t);
		this.showYZ(i);
	}

	public final void showYZ(final int idx) {
		final float[] y2d = this.y;
		final double[] x2d = this.x;
		if((idx >= x2d.length || idx == this.curr_x_yz_idx) && this.mode2D == Signal.Mode2D.ZY) return;
		this.prev_idx = 0;
		this.curr_x_yz_plot = x2d[idx];
		this.curr_x_yz_idx = idx;
		this.curr_y_xz_plot = Float.NaN;
		this.curr_y_xz_idx = -1;
		if(this.zY2D != null){
			this.ymin = this.ymax = this.zY2D[idx];
			for(int j = 0; j < y2d.length; j++){
				final int k = x2d.length * j + idx;
				y2d[j] = this.zY2D[k];
				if(this.ymin > y2d[j]) this.ymin = y2d[j];
				if(this.ymax < y2d[j]) this.ymax = y2d[j];
			}
		}
		this.sliceX = new double[y2d.length];
		this.sliceY = new float[y2d.length];
		final int zLen = this.z.length;
		float sliceMin, sliceMax;
		sliceMin = sliceMax = this.z[idx];
		for(int j = 0; j < y2d.length; j++){
			final int k = x2d.length * j + idx;
			this.sliceX[j] = y2d[j];
			if(k >= zLen) break;
			this.sliceY[j] = this.z[k];
			if(sliceMin > this.z[k]) sliceMin = this.z[k];
			if(sliceMax < this.z[k]) sliceMax = this.z[k];
		}
		this.error = this.asym_error = false;
		this.mode2D = Signal.Mode2D.ZY;
		if(!this.fix_xmin) this.saved_xmin = this.curr_xmin = this.xmin = this.y2D_min;
		// saved_xmin = curr_xmin = ymin;
		if(!this.fix_xmax) this.saved_xmax = this.curr_xmax = this.xmax = this.y2D_max;
		// saved_xmax = curr_xmax = ymax;
		if(!this.fix_ymin) this.saved_ymin = this.ymin = sliceMin;
		if(!this.fix_ymax) this.saved_ymax = this.ymax = sliceMax;
		// Assumed that for 2D data, dimensions are increasing
		this.increasing_x = true;
	}

	@Override
	public final void sourceUpdated(final XYData xydata) {
		long startTime = 0L;
		if(DEBUG.D) startTime = System.nanoTime();
		if(this.title == null) try{
			this.title = this.data.getTitle();
		}catch(final IOException e){
			this.title = e.getMessage();
		}
		if(this.xlabel == null) try{
			this.xlabel = (this.x_data == null) ? this.data.getXLabel() : this.x_data.getYLabel();
		}catch(final IOException e){
			this.xlabel = e.getMessage();
		}
		if(this.ylabel == null) try{
			this.ylabel = this.data.getYLabel();
		}catch(final IOException e){
			this.ylabel = e.getMessage();
		}
		if(this.zlabel == null && this.mode2D != Mode2D.YX) try{
			this.zlabel = this.data.getZLabel();
		}catch(final IOException e){
			this.zlabel = e.getMessage();
		}
		this.x = xydata.getX();
		this.xLong = xydata.getXLong();
		this.y = xydata.getY();
		this.Autoscale();
		this.saved_xmin = this.curr_xmin = this.xmin;
		this.saved_xmax = this.curr_xmax = this.xmax;
		this.saved_ymin = this.ymin;
		this.saved_ymax = this.ymax;
		this.fireSignalUpdated(SignalListener.UPDATE_LIMITS);
		if(DEBUG.D) System.out.println("sourceUpdated took " + (System.nanoTime() - startTime) / 1E9);
	}

	/**
	 * Method to call before execute a translate method.
	 */
	public final void StartTranslate() {
		this.t_xmax = this.xmax;
		this.t_xmin = this.xmin;
		this.t_ymax = this.ymax;
		this.t_ymin = this.ymin;
	}

	public final double surfaceValue(final double x0, final double y0) {
		try{
			if(this.mode2D == Signal.Mode2D.IMAGE || this.mode2D == Signal.Mode2D.CONTOUR){
				this.img_yprev = Signal.findIndex(this.y, y0, this.img_yprev);
				this.img_xprev = Signal.findIndex(this.x, x0, this.img_xprev);
				System.out.println(this.y.length + "," + this.img_yprev + "," + this.x.length + "," + this.img_xprev);
				final int ixl = (this.x.length - 1 == this.img_xprev || x0 < this.x[this.img_xprev]) && this.img_xprev != 0 ? this.img_xprev - 1 : this.img_xprev, ixh = ixl + 1;
				final int iyl = (this.y.length - 1 == this.img_yprev || y0 < this.y[this.img_yprev]) && this.img_yprev != 0 ? this.img_yprev - 1 : this.img_yprev, iyh = iyl + 1;
				final double xl = this.x[ixl], xh = this.x[ixh], dx = (x0 - xl) / (xh - xl);
				final double yl = this.y[iyl], yh = this.y[iyh], dy = (y0 - yl) / (yh - yl);
				final double z00 = this.z[ixl * this.y.length + iyl];
				final double z10 = this.z[ixh * this.y.length + iyl];
				final double z01 = this.z[ixl * this.y.length + iyh];
				final double z11 = this.z[ixh * this.y.length + iyh];
				this.z_value = z00 * (1 - dx) * (1 - dy) + z10 * dx * (1 - dy) + z01 * (1 - dx) * dy + z11 * dx * dy;
				System.out.println(x0 + "," + y0 + ": " + z00 + "," + z10 + "," + z01 + "," + z11 + " -> " + this.z_value);
			}
		}catch(final Exception exc){
			System.err.println("jscope.Signal.surfaceValue: " + exc);
			exc.printStackTrace();
			final int idx = this.img_xprev * this.x.length + this.img_yprev;
			this.z_value = this.z[idx < this.z.length ? idx : this.z.length - 1];
		}
		return this.z_value;
	}

	/**
	 * Translate signal of delta_x and delta_y
	 *
	 * @param delta_x
	 *            x translation factor
	 * @param delta_y
	 *            y translation factor
	 * @param x_log
	 *            logaritm scale flag, if is logarithm scale true
	 * @param y_log
	 *            logaritm scale flag, if is logarithm scale true
	 */
	public final void Traslate(final double delta_x, final double delta_y, final boolean x_log, final boolean y_log) {
		if(x_log){
			this.xmax = this.t_xmax * delta_x;
			this.xmin = this.t_xmin * delta_x;
		}else{
			this.xmax = this.t_xmax + delta_x;
			this.xmin = this.t_xmin + delta_x;
		}
		if(y_log){
			this.ymax = this.t_ymax * delta_y;
			this.ymin = this.t_ymin * delta_y;
		}else{
			this.ymax = this.t_ymax + delta_y;
			this.ymin = this.t_ymin + delta_y;
		}
	}

	public final void unfreeze() {
		if(this.freezeMode == Signal.FreezeMode.FREE) return;
		this.freezeMode = Signal.FreezeMode.FREE;
		this.xmin = this.freezedXMin;
		this.xmax = this.freezedXMax;
		for(int i = 0; i < this.pendingUpdatesV.size(); i++)
			this.dataRegionUpdated(this.pendingUpdatesV.elementAt(i), false);
		this.pendingUpdatesV.clear();
	}

	@Override
	public void updatePending() {
		boolean newpending;
		synchronized(this){
			newpending = this.pending == 0;
			this.pending++;
		}
		if(newpending) synchronized(this.signalListeners){
			for(final SignalListener sl : this.signalListeners)
				sl.signalUpdated(SignalListener.UPDATE_PENDING);
		}
	}

	public final boolean xLimitsInitialized() {
		return this.xLimitsInitialized;
	}

	void fireSignalUpdated(final int mode) {
		if(DEBUG.M) System.out.println("fireSignalUpdated(" + mode + ") for " + this.signalListeners.size());
		synchronized(this){
			this.pending--;
			if(this.pending < 0) this.pending = 0;
		}
		synchronized(this.signalListeners){
			for(final SignalListener sl : this.signalListeners)
				sl.signalUpdated(mode);
		}
	}

	private final void AutoscaleX1D(final double[] X) {
		this.xmin = Double.POSITIVE_INFINITY;
		this.xmax = Double.NEGATIVE_INFINITY;
		if(X == null) return;
		for(final double xi : X){
			if(Double.isNaN(xi)) continue;
			if(xi < this.xmin) this.xmin = xi;
			if(xi > this.xmax) this.xmax = xi;
		}
		// if(this.fix_xmin && this.saved_xmin > this.xmin) this.xmin = this.saved_xmin;
		// if(this.fix_xmax && this.saved_xmax < this.xmax) this.xmax = this.saved_xmax;
		if(this.xmin != this.xmax) return;
		this.xmax = this.xmin + 1E-3;
		this.xmin = this.xmin - 1E-3;
	}

	private final void AutoscaleY1D(final double[] X, final float[] Y, final double min, final double max) {
		if(DEBUG.M) System.out.println("Signal.AutoscaleY1D(" + X + "," + Y + "," + min + "," + max + ")");
		if(X == null || Y == null) return;
		final int len = (X.length < Y.length) ? X.length : Y.length;
		final boolean All = (min == Double.NEGATIVE_INFINITY && max == Double.POSITIVE_INFINITY);
		for(int i = 0; i < len; i++){
			if(Float.isNaN(Y[i])) continue;
			if(!All && (X[i] < min || X[i] > max)) continue;
			if(Y[i] < this.ymin) this.ymin = Y[i];
			if(Y[i] > this.ymax) this.ymax = Y[i];
		}
		// if(this.fix_ymin && this.saved_ymin > this.ymin) this.ymin = this.saved_ymin;
		// if(this.fix_ymax && this.saved_ymax < this.ymax) this.ymax = this.saved_ymax;
		if(this.ymin != this.ymax) return;
		this.ymax = this.ymin + 1E-3f;
		this.ymin = this.ymin - 1E-3f;
	}

	private final void checkData(final double xMin, final double xMax, Mode2D mode2d) throws Exception {
		if(DEBUG.M) System.out.println("Signal.checkData(" + xMin + ", " + xMax + ")");
		if(mode2d == Mode2D.YX){
			this.mode2D = Mode2D.YX;
			this.data.getDataAsync(xMin, xMax, this.num_points);
			if(this.up_errorData != null && this.upError == null){
				// XYData xyData = up_errorData.getData(xMin, xMax, NUM_POINTS);
				final XYData xyData = this.up_errorData.getData(xMin, xMax, Integer.MAX_VALUE);
				this.upError = xyData.getY();
			}
			if(this.low_errorData != null && this.lowError == null){
				// XYData xyData = low_errorData.getData(xMin, xMax, NUM_POINTS);
				final XYData xyData = this.low_errorData.getData(xMin, xMax, Integer.MAX_VALUE);
				this.lowError = xyData.getY();
			}
			if(this.saved_ymin == Double.NEGATIVE_INFINITY) this.saved_ymin = this.ymin;
			if(this.saved_ymax == Double.POSITIVE_INFINITY) this.saved_ymax = this.ymax;
		} else {
			this.mode2D = mode2d;
			this.x = this.data.getX2D();
			if(this.x == null && this.data.isXLong()){
				this.xLong = this.data.getX2DLong();
				this.x = new double[this.xLong.length];
				for(int i = 0; i < this.xLong.length; i++)
					this.x[i] = this.xLong[i];
			}
			this.y = this.data.getY2D();
			this.z = this.data.getZ();
			if(this.x_data != null){
				this.xY2D = this.x_data.getX2D();
				this.yY2D = this.x_data.getY2D();
				this.zY2D = this.x_data.getZ();
				if((this.x != null && this.x.length != this.xY2D.length) || (this.xLong != null && this.xLong.length != this.xY2D.length) && this.y.length != this.yY2D.length && this.z.length != this.zY2D.length){
					this.xY2D = null;
					this.yY2D = null;
					this.zY2D = null;
					this.x_data = null;
				}
			}
			this.x2D_min = Double.POSITIVE_INFINITY;
			this.x2D_max = Double.NEGATIVE_INFINITY;
			this.y2D_min = Double.POSITIVE_INFINITY;
			this.y2D_max = Double.NEGATIVE_INFINITY;
			this.z2D_min = Double.POSITIVE_INFINITY;
			this.z2D_max = Double.NEGATIVE_INFINITY;
			for(final double element : this.x){
				if(Double.isNaN(element)) continue;
				if(element < this.x2D_min) this.x2D_min = element;
				if(element > this.x2D_max) this.x2D_max = element;
			}
			for(final float element : this.y){
				if(Float.isNaN(element)) continue;
				if(element < this.y2D_min) this.y2D_min = element;
				if(element > this.y2D_max) this.y2D_max = element;
			}
			for(final float element : this.z){
				if(Float.isNaN(element)) continue;
				if(element < this.z2D_min) this.z2D_min = element;
				if(element > this.z2D_max) this.z2D_max = element;
			}
			if(xMin == Double.NEGATIVE_INFINITY) this.xmin = this.curr_xmin = this.x2D_min;
			else this.xmin = this.curr_xmin = xMin;
			if(xMax == Double.POSITIVE_INFINITY) this.xmax = this.curr_xmax = this.x2D_max;
			else this.xmax = this.curr_xmax = xMax;
		}
		if(this.saved_xmin == Double.NEGATIVE_INFINITY) this.saved_xmin = this.xmin;
		if(this.saved_xmax == Double.POSITIVE_INFINITY) this.saved_xmax = this.xmax;
	}

	/**
	 * Check if x array coordinates are increasing.
	 */
	private final void checkIncreasingX() {
		this.increasing_x = false;
		for(int i = 1; i < this.x.length; i++)
			if(this.x[i] < this.x[i - 1]) return;
		this.increasing_x = true;
	}

	private final void decShowZX() {
		if(this.mode2D == Signal.Mode2D.ZX){
			int idx = this.curr_y_xz_idx - 1;
			if(idx < 0) idx = this.y.length - 1;
			this.showXZ(idx);
		}
	}

	private final void decShowZY() {
		if(this.mode2D == Signal.Mode2D.ZY){
			int idx = this.curr_x_yz_idx - 1;
			if(idx < 0) idx = this.x.length - 1;
			this.showYZ(idx);
		}
	}

	private final void incShowXZ() {
		if(this.mode2D == Signal.Mode2D.ZX){
			int idx = this.curr_y_xz_idx;
			idx = (idx + 1) % this.y.length;
			this.showXZ(idx);
		}
	}

	private final void incShowYZ() {
		if(this.mode2D == Signal.Mode2D.ZY){
			int idx = this.curr_x_yz_idx;
			idx = (idx + 1) % this.x.length;
			this.showYZ(idx);
		}
	}

	private final void initContour() {
		this.saved_ymin = this.ymin = this.y2D_min;
		this.saved_ymax = this.ymax = this.y2D_max;
		this.saved_xmin = this.xmin = this.x2D_min;
		this.saved_xmax = this.xmax = this.x2D_max;
		// x = x2D;
		// y = y2D;
		this.cs = new ContourSignal(this);
		if(this.contourLevels == null || this.contourLevels.length == 0){
			this.contourLevels = new double[Signal.DEFAULT_CONTOUR_LEVEL];
			final double dz = (this.z2D_max - this.z2D_min) / (Signal.DEFAULT_CONTOUR_LEVEL + 1);
			for(int i = 0; i < this.contourLevels.length; i++)
				this.contourLevels[i] = this.z2D_min + dz * (i + 1);
		}
		for(final double contourLevel : this.contourLevels)
			this.addContourLevel(contourLevel);
	}
}
