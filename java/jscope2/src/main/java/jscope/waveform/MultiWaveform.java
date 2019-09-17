package jscope.waveform;

/* $Id$ */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.util.Vector;
import debug.DEBUG;
import jscope.data.framedata.Frames;
import jscope.data.signal.Signal;
import jscope.data.signal.Signal.FreezeMode;
import jscope.data.signal.Signal.Marker;
import jscope.data.signal.Signal.Mode1D;
import jscope.data.signal.Signal.Mode2D;
import jscope.data.signal.SignalListener;

/**
 * Class MultiWaveform extends the capability of class Waveform to deal with multiple waveforms.
 */
public class MultiWaveform extends Waveform{
	private static final long	serialVersionUID	= 1L;
	public static final int		HORIZONTAL			= 0;
	public static final int		LEGEND_BOTTOM		= 1;
	public static final int		LEGEND_IN_GRAPHICS	= 0;
	public static final int		LEGEND_RIGHT		= 2;
	public static final int		PRINT_LEGEND		= 8;
	public static final int		VERTICAL			= 1;
	private int					bottom_size			= 0;
	protected int				curr_point_sig_idx	= -1;
	protected boolean			fixed_legend		= false;
	private int					legend_mode			= 0;
	private Point				legend_point;
	protected double			legend_x;
	protected double			legend_y;
	protected Vector<Signal>	orig_signals		= null;
	protected double			orig_xmax			= Double.NEGATIVE_INFINITY;
	protected double			orig_xmin			= Double.POSITIVE_INFINITY;
	protected int				right_size			= 0;
	protected boolean			show_legend			= false;
	protected Vector<Signal>	signals				= new Vector<Signal>();
	public WaveInterface		wi;

	public MultiWaveform(){
		super();
		if(this.signals.size() != 0) this.signals.removeAllElements();
		this.orig_signals = null;
	}

	public void addSignal(final Signal s) {
		if(!this.exists(s)){
			this.signals.addElement(s);
			this.setLimits();
			this.curr_point_sig_idx = this.signals.size() - 1;
			s.registerSignalListener(this);
		}
	}

	public void addSignals(final Signal s[]) {
		if(s == null || s.length == 0) return;
		for(final Signal element : s){
			this.addSignal(element);
			if(element != null) element.registerSignalListener(this);
		}
		this.setLimits();
		if(this.waveform_signal != null){
			this.curr_point_sig_idx = 0;
			super.update(this.waveform_signal);
		}
	}

	@Override
	public void appendUpdate() {
		Signal s;
		for(int i = 0; i < this.signals.size(); i++){
			s = this.signals.elementAt(i);
			if(s.fullPaint()){
				this.update();
				return;
			}
		}
		this.appendPaint(this.getGraphics(), this.getSize());
	}

	@Override
	public void autoscale() {
		if(DEBUG.M) System.out.println("MultiWaveform.Autoscale()");
		if(this.is_image && this.frames != null){
			super.autoscale();
			return;
		}
		if(this.waveform_signal == null) return;
		this.update_timestamp++;
		if(this.signals == null) return;
		if(this.orig_signals != null){ // Previous zoom
			this.signals = this.orig_signals;
			this.orig_signals = null;
		}
		if(this.undoZoomPendig()) this.addZoomRegion();
		boolean any = false;
		for(final Signal signal : this.signals){
			if(signal == null) continue;
			signal.Autoscale();
			if(any) continue;
			this.waveform_signal.setMode1D(signal.getMode1D());
			this.waveform_signal.setMode2D(signal.getMode2D());
			any = true;
		}
		this.waveform_signal.setFreezeMode(FreezeMode.FREE);
		if(!any) return;
		this.setLimits(Signal.SIMPLE);
		this.reportChanges();
	}

	@Override
	public void autoscaleY() {
		if(this.waveform_signal == null || this.signals == null) return;
		double ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
		for(final Signal signal : this.signals){
			if(signal == null) continue;
			signal.AutoscaleY();
			if(signal.getYmin() < ymin) ymin = signal.getYmin();
			if(signal.getYmax() > ymax) ymax = signal.getYmax();
		}
		this.waveform_signal.setYmin(ymin, Signal.SIMPLE);
		this.waveform_signal.setYmax(ymax, Signal.SIMPLE);
		this.reportChanges();
	}

	@Override
	public void copy(final Waveform wave) {
		super.copy(wave);
		if(!wave.is_image){
			int i;
			final MultiWaveform w = (MultiWaveform)wave;
			if(this.signals.size() != 0) this.signals.removeAllElements();
			final Vector<Signal> s = w.getSignals();
			for(i = 0; i < s.size(); i++){
				this.signals.addElement(new Signal(s.elementAt(i)));
				if(DEBUG.D) System.out.println("Copy: " + this.signals.elementAt(i));
				this.signals.elementAt(i).registerSignalListener(this);
			}
			this.show_legend = w.show_legend;
			if(w.show_legend){
				this.legend_x = w.legend_x;
				this.legend_y = w.legend_y;
				this.legend_point = new Point(w.legend_point);
			}
			this.updateLimits();
			if(this.waveform_signal != null) super.update(this.waveform_signal);
		}
	}

	@Override
	public void erase() {
		if(this.signals.size() != 0) this.signals.removeAllElements();
		this.orig_signals = null;
		this.show_legend = false;
		this.legend_point = null;
		super.erase();
	}

	public boolean exists(final Signal s) {
		if(s == null) return true;
		if(s.getName() == null || s.getName().length() == 0){
			s.setName("Signal_" + this.signals.size());
			return false;
		}
		for(int i = 0; i < this.signals.size(); i++){
			final Signal s1 = this.signals.elementAt(i);
			if(s1.getName() != null && s.getName() != null && s1.getName().equals(s.getName())) return true;
		}
		return false;
	}

	public int getColorIdx(final int idx) {
		if(this.is_image) return super.getColorIdx();
		if(idx < this.signals.size()) return this.signals.elementAt(idx).getColorIdx();
		return 0;
	}

	public boolean getInterpolate(final int idx) {
		if(idx < this.signals.size()) return this.signals.elementAt(idx).getInterpolate();
		return false;
	}

	public int getLegendMode() {
		return this.legend_mode;
	}

	public double getLegendXPosition() {
		return this.legend_x;
	}

	public double getLegendYPosition() {
		return this.legend_y;
	}

	public Marker getMarker(final int idx) {
		if(idx < this.signals.size()) return this.signals.elementAt(idx).getMarker();
		return Marker.DEFAULT;
	}

	public int getMarkerStep(final int idx) {
		if(idx < this.signals.size() && this.signals.elementAt(idx) != null) return this.signals.elementAt(idx).getMarkerStep();
		return 0;
	}

	@Override
	public int getSelectedSignal() {
		return this.curr_point_sig_idx;
	}

	@Override
	public int getShowSignalCount() {
		if(this.signals != null) return this.signals.size();
		return 0;
	}

	@Override
	public Signal getSignal() {
		if(this.signals != null && this.signals.size() > 0) return this.signals.elementAt(this.curr_point_sig_idx);
		return null;
	}

	public int getSignalCount() {
		return this.getShowSignalCount();
	}

	@Override
	public Mode1D getSignalMode1D() {
		return this.getSignalMode1D(this.curr_point_sig_idx);
	}

	public Mode1D getSignalMode1D(final int idx) {
		if(idx >= 0 && idx < this.signals.size()) return this.signals.elementAt(idx).getMode1D();
		return Mode1D.DEFAULT;
	}

	@Override
	public Mode2D getSignalMode2D() {
		return this.getSignalMode2D(this.curr_point_sig_idx);
	}

	public Mode2D getSignalMode2D(final int idx) {
		if(idx >= 0 && idx < this.signals.size()) return this.signals.elementAt(idx).getMode2D();
		return Mode2D.DEFAULT;
	}

	public String getSignalName(final int idx) {
		if(idx < this.signals.size() && this.signals.elementAt(idx) != null){
			final Signal s = this.signals.elementAt(idx);
			if(s.getName() == null) s.setName(new String("Signal_" + idx));
			return s.getName();
		}else if(this.is_image && this.frames != null) return this.frames.getName();
		return null;
	}

	public Vector<Signal> getSignals() {
		return this.signals;
	}

	public String[] getSignalsName() {
		try{
			final String names[] = new String[this.signals.size()];
			String n;
			Signal s;
			for(int i = 0; i < this.signals.size(); i++){
				s = this.signals.elementAt(i);
				n = s.getName();
				if(n != null) names[i] = n;
				else{
					names[i] = new String("Signal_" + i);
					s.setName(names[i]);
				}
			}
			return names;
		}catch(final Exception e){
			return null;
		}
	}

	public boolean[] getSignalsState() {
		boolean s_state[] = null;
		if(this.signals != null){
			s_state = new boolean[this.signals.size()];
			for(int i = 0; i < this.signals.size(); i++)
				s_state[i] = this.getSignalState(i);
		}
		return s_state;
	}

	public boolean getSignalState(final int idx) {
		if(idx > this.signals.size()) return false;
		final Signal s = this.signals.elementAt(idx);
		if(s == null) return false;
		return !(!s.getInterpolate() && s.getMarker() == Signal.Marker.NONE);
	}

	public WaveInterface getWaveInterface() {
		return this.wi;
	}

	public boolean isFixedLegend() {
		return this.fixed_legend;
	}

	public boolean isShowLegend() {
		return this.show_legend;
	}

	@Override
	synchronized public void paint(final Graphics g, final Dimension d, final int print_mode) {
		this.bottom_size = this.right_size = 0;
		if(this.fixed_legend && this.show_legend || (print_mode & MultiWaveform.PRINT_LEGEND) == MultiWaveform.PRINT_LEGEND){
			Waveform.getFont(g);
			if(this.legend_mode == MultiWaveform.LEGEND_BOTTOM){
				final Dimension dim = this.getLegendDimension(g, d, MultiWaveform.HORIZONTAL);
				this.bottom_size = dim.height;
				g.drawLine(0, dim.height - 1, d.width, dim.height - 1);
			}
			if(this.legend_mode == MultiWaveform.LEGEND_RIGHT){
				final Dimension dim = this.getLegendDimension(g, d, MultiWaveform.VERTICAL);
				this.right_size = dim.width;
				g.drawLine(dim.width - 1, 0, dim.width - 1, d.height);
			}
		}
		super.paint(g, d, print_mode);
	}

	public void removeLegend() {
		if(this.wi instanceof WaveInterface) this.wi.showLegend(false);
		this.show_legend = false;
		this.not_drawn = true;
		this.repaint();
	}

	public void removeSignal(final int idx) {
		if(idx < this.signals.size()) this.signals.removeElementAt(idx);
	}

	public void replaceSignal(final int idx, final Signal s) {
		if(idx < this.signals.size()){
			this.signals.removeElementAt(idx);
			this.signals.insertElementAt(s, idx);
		}
	}

	@Override
	public void resetScales() {
		if(this.signals == null || this.waveform_signal == null) return;
		if(this.orig_signals != null){
			this.signals = this.orig_signals;
			this.orig_signals = null;
		}
		for(final Signal signal : this.signals)
			if(signal != null) signal.ResetScales();
		this.setLimits(Signal.AT_CREATION);
		this.waveform_signal.ResetScales();
		this.reportChanges();
	}

	public void setColorIdx(final int idx, final int color_idx) {
		if(this.is_image){
			super.setColorIdx(color_idx);
			super.setCrosshairColor(color_idx);
			return;
		}
		if(idx < this.signals.size()){
			this.signals.elementAt(idx).setColorIdx(color_idx);
			if(idx == this.curr_point_sig_idx) this.crosshair_color = Waveform.colors[color_idx % Waveform.colors.length];
		}
	}

	public void setInterpolate(final int idx, final boolean interpolate) {
		if(idx < this.signals.size()) (this.signals.elementAt(idx)).setInterpolate(interpolate);
	}

	public void setLegend(final Point p) {
		final Dimension d = this.getSize();
		this.legend_x = this.wm.toXValue(p.x, d);
		this.legend_y = this.wm.toYValue(p.y, d);
		this.legend_point = new Point(p);
		this.show_legend = true;
		this.not_drawn = true;
		this.repaint();
	}

	public void setLegendMode(final int legend_mode) {
		this.legend_mode = legend_mode;
		if(legend_mode != MultiWaveform.LEGEND_IN_GRAPHICS) this.fixed_legend = true;
		else this.fixed_legend = false;
	}

	public void setMarker(final int idx, final Marker marker) {
		if(idx < this.signals.size()) this.signals.elementAt(idx).setMarker(marker);
	}

	public void setMarkerStep(final int idx, final int marker_step) {
		if(idx < this.signals.size()) this.signals.elementAt(idx).setMarkerStep(marker_step);
	}

	public void setPointSignalIndex(final int idx) {
		if(idx >= 0 && idx < this.signals.size()){
			Signal curr_signal;
			this.curr_point_sig_idx = idx;
			curr_signal = this.signals.elementAt(this.curr_point_sig_idx);
			if(curr_signal == null) return;
			if(curr_signal.getColor() != null) this.crosshair_color = curr_signal.getColor();
			else this.crosshair_color = Waveform.colors[curr_signal.getColorIdx() % Waveform.colors.length];
		}
	}

	public void setShowLegend(final boolean show_legend) {
		this.show_legend = show_legend;
	}

	public void setSignalMode2D(final Mode2D mode) {
		this.setSignalMode2D(this.curr_point_sig_idx, mode);
	}

	public void setSignalMode2D(final int idx, final Mode2D mode) {
		if(idx >= 0 && idx < this.signals.size()){
			final Signal s = this.signals.elementAt(idx);
			if(s != null)
				switch(mode){
					case YX:
						break;
					case ZX:
						s.setMode2D(mode, (float)this.wave_point_y);
						break;
					case ZY:
						s.setMode2D(mode, (float)this.wave_point_x);
						break;
					case IMAGE:
						s.setMode2D(mode, (float)this.wave_point_x);
						break;
					case CONTOUR:
						s.setMode2D(mode, (float)this.wave_point_x);
						break;
					default:
						break;
				}
				this.sendUpdateEvent();
				this.autoscale();
				/*
				 * if (mode == Signal.Mode2D.XZ && s.getMode2D() ==
				 * Signal.Mode2D.YZ) s.setMode2D(mode, (float)
				 * wave_point_y); else s.setMode2D(mode, (float)
				 * wave_point_x);
				 */
			}
		this.not_drawn = true;
		this.repaint();
	}

	@Override
	public void setSignalMode1D(final Mode1D mode) {
		super.setSignalMode1D(mode);
		this.setSignalMode1D(this.curr_point_sig_idx, mode);
	}

	public void setSignalMode1D(final int idx, final Mode1D mode) {
		if(idx >= 0 && idx < this.signals.size()) this.signals.elementAt(idx).setMode1D(mode);
		this.not_drawn = true;
		this.repaint();
	}

	public void setSignalState(final String label, final boolean state) {
		Signal sig;
		if(this.signals != null){
			for(int i = 0; i < this.signals.size(); i++){
				sig = this.signals.elementAt(i);
				if(sig == null) continue;
				if(sig.getName().equals(label)){
					sig.setInterpolate(state);
					sig.setMarker(Signal.Marker.NONE);
				}
			}
			if(this.mode == Waveform.MODE_POINT){
				final Dimension d = this.getSize();
				final double curr_x = this.wm.toXValue(this.end_x, d), curr_y = this.wm.toYValue(this.end_y, d);
				this.findPoint(curr_x, curr_y, true);
			}
		}
	}

	public void setWaveInterface(final WaveInterface wi) {
		this.wi = wi;
	}

	@Override
	public void setXlimits(final double xmin, final double xmax) {
		if(this.signals == null) return;
		Signal s;
		for(int i = 0; i < this.signals.size(); i++){
			s = this.signals.elementAt(i);
			if(s != null) s.setXLimits(xmin, xmax, Signal.SIMPLE);
		}
	}

	@Override
	public void setXScale(final Waveform w) {
		if(this.waveform_signal == null) return;
		this.waveform_signal.setXLimits(w.waveform_signal.getXmin(), w.waveform_signal.getXmax(), Signal.SIMPLE);
		for(int i = 0; i < this.signals.size(); i++){
			if(this.signals.elementAt(i) == null) continue;
			this.signals.elementAt(i).setXLimits(w.waveform_signal.getXmin(), w.waveform_signal.getXmax(), Signal.SIMPLE);
		}
		this.reportChanges();
	}

	@Override
	public void setXScaleAutoY(final Waveform w) {
		if(this.waveform_signal == null || this.signals == null) return;
		if(w != this && this.orig_signals != null) // Previous zoom for
		                                           // different windows
		{
			this.signals = this.orig_signals;
			// operation on signals must not affect original signals
			this.orig_signals = new Vector<Signal>();
			for(int i = 0; i < this.signals.size(); i++)
				this.orig_signals.addElement(this.signals.elementAt(i));
		}
		this.waveform_signal.setXLimits(w.waveform_signal.getXmin(), w.waveform_signal.getXmax(), Signal.SIMPLE);
		for(int i = 0; i < this.signals.size(); i++){
			if(this.signals.elementAt(i) == null) continue;
			this.signals.elementAt(i).setXLimits(w.waveform_signal.getXmin(), w.waveform_signal.getXmax(), Signal.SIMPLE);
		}
		this.autoscaleY();
		this.update_timestamp++;
		this.notifyZoom(this.waveform_signal.getXmin(), this.waveform_signal.getXmax(), this.waveform_signal.getYmin(), this.waveform_signal.getYmax(), this.update_timestamp);
	}

	@Override
	public void setYlimits(final float ymin, final float ymax) {
		if(this.signals == null) return;
		Signal s;
		for(int i = 0; i < this.signals.size(); i++){
			s = this.signals.elementAt(i);
			s.setYlimits(ymin, ymax);
		}
	}

	@Override
	public void signalUpdated(final int updatemode) {
		this.checkPending();
		if(updatemode >= SignalListener.UPDATE_REPAINT){
			this.change_limits |= updatemode >= SignalListener.UPDATE_LIMITS;
			this.setLimits(Signal.SIMPLE);
			this.not_drawn = true;
			this.repaint();
		}
	}

	@Override
	public void update() {
		if(!this.is_image){
			this.updateLimits();
			if(this.waveform_signal != null){
				this.curr_point_sig_idx = 0;
				super.update(this.waveform_signal);
			}else{
				this.not_drawn = true;
				this.repaint();
			}
		}else if(this.frames != null) super.update();
	}

	public void update(final Frames frames_in) {
		this.frames = frames_in;
		this.is_image = true;
		this.update();
	}

	public void update(final Signal signals_in[]) {
		if(signals_in == null) return;
		if(this.signals.size() != 0) this.signals.removeAllElements();
		for(final Signal sig : signals_in)
			if(sig != null){
				this.signals.addElement(sig);
				sig.registerSignalListener(this);
			}
		this.signalUpdated(SignalListener.UPDATE_LIMITS);
		MultiWaveform.this.update();
	}

	@Override
	public synchronized void updateCrosshair(final double curr_x, final double curr_y) {
		if(!this.is_image){
			// if(wm == null) { System.out.println("wm == null"); return;}
			// if(dragging || mode != MODE_POINT || signals == null ||
			// signals.size() == 0)
			if(this.mode != Waveform.MODE_POINT || this.signals == null || this.signals.size() == 0) return;
			Signal s;
			for(int i = 0; i < this.signals.size(); i++){
				s = this.signals.elementAt(i);
				if(s == null) continue;
				if(s.getMode2D() == Signal.Mode2D.ZY){
					s.showYZ(curr_x);
					this.not_drawn = true;
				}
				/*
				 * if ( s.getType() == Signal.Type.IMAGE && s.getMode2D() ==
				 * Signal.MODE_PROFILE ) { s.showProfile(s.getMode2D(), (float)
				 * curr_x); not_drawn = true; }
				 */
			}
		}
		super.updateCrosshair(curr_x, curr_y);
	}

	void updateLimits() {
		if(DEBUG.D) System.out.println("MultiWaveform.updateLimits()");
		if(this.signals == null || this.signals.size() == 0) return;
		int i;
		this.waveform_signal = null;
		if(this.curr_point_sig_idx == -1 || this.curr_point_sig_idx >= this.signals.size() || this.signals.elementAt(this.curr_point_sig_idx) == null){
			for(i = 0; i < this.signals.size(); i++)
				if(this.signals.elementAt(i) != null) break;
			if(i == this.signals.size()) return;
		}else i = this.curr_point_sig_idx;
		this.waveform_signal = this.signals.size() > i ? new Signal(this.signals.elementAt(i)) : new Signal();
		// Check if any of the elements of signals vector refers to absolute
		// time.
		// In this case set minimum and maximum X value of reference
		// waveform_signal to its limits
		this.setLimits(Signal.AT_CREATION);
	}

	protected void drawLegend(final Graphics g, final Point p, final int print_mode, final int orientation) {
		final Dimension d = this.getSize();
		final int h = g.getFont().getSize() + 2;
		final Color prev_col = g.getColor();
		final Point pts[] = new Point[1];
		final FontMetrics fm = this.getFontMetrics(g.getFont());
		String s;
		pts[0] = new Point();
		int curr_width = 0, sum_width = p.x;
		final Marker markers[] = Marker.values();
		int curr_marker = 0;
		g.setColor(Color.black);
		if(orientation == MultiWaveform.VERTICAL) g.translate(-this.marker_width, 0);
		for(int i = 0, py = p.y + h, px = p.x; i < this.getSignalCount(); i++){
			if(!this.isSignalShow(i)) continue;
			if((print_mode & MultiWaveform.PRINT_BW) != MultiWaveform.PRINT_BW) g.setColor(this.getSignalColor(i));
			s = this.getSignalInfo(i);
			if(orientation == MultiWaveform.HORIZONTAL){
				final char s_ar[] = s.toCharArray();
				curr_width = fm.charsWidth(s_ar, 0, s_ar.length) + 3 * this.marker_width;
				if(sum_width + curr_width < d.width){
					px = sum_width;
					sum_width += curr_width;
				}else{
					py += h;
					px = p.x;
					sum_width = p.x + curr_width;
				}
			}
			pts[0].x = px + 2 * this.marker_width;
			pts[0].y = py - this.marker_width / 2;
			this.drawMarkers(g, pts, 1, this.getMarker(i), 1, Mode1D.DEFAULT);
			if((this.getMarker(i) == Signal.Marker.NONE) && ((print_mode & MultiWaveform.PRINT_BW) == MultiWaveform.PRINT_BW)){
				this.drawMarkers(g, pts, 1, markers[curr_marker + 1], 1, Mode1D.DEFAULT);
				curr_marker = (curr_marker + 1) % (markers.length - 1);
			}
			g.drawString(s, px + 3 * this.marker_width, py);
			if(orientation == MultiWaveform.VERTICAL) py += h;
		}
		if(orientation == MultiWaveform.VERTICAL) g.translate(this.marker_width, 0);
		g.setColor(prev_col);
	}

	@Override
	protected void drawMarkers(final Graphics g, final Vector<Polygon> segments, final Marker mark_type, final int step, final Mode1D mode_in) {
		int lnum_points = 0;
		Point lpoints[];
		Polygon curr_polygon;
		if(segments == null) return;
		final int num_segments = segments.size();
		for(int i = lnum_points = 0; i < num_segments; i++)
			lnum_points += segments.elementAt(i).npoints;
		lpoints = new Point[lnum_points];
		for(int i = lnum_points = 0; i < num_segments; i++){
			curr_polygon = segments.elementAt(i);
			for(int j = 0; j < curr_polygon.npoints; j += step)
				lpoints[lnum_points++] = new Point(curr_polygon.xpoints[j], curr_polygon.ypoints[j]);
		}
		super.drawMarkers(g, lpoints, lnum_points, mark_type, 1, mode_in);
	}

	protected void drawMarkers(final Graphics g, final Vector<Polygon> segments, final Signal s) {
		this.drawMarkers(g, segments, s.getMarker(), s.getMarkerStep(), s.getMode1D());
	}

	@Override
	protected void drawSignal(final Graphics g) {
		this.drawSignal(g, this.getSize(), Waveform.NO_PRINT);
	}

	@Override
	protected void drawSignal(final Graphics g, final Dimension d, final int print_mode) {
		if(this.wm == null) return;
		final int num_marker = Marker.values().length - 1;
		if((print_mode & MultiWaveform.PRINT_BW) == MultiWaveform.PRINT_BW){
			int curr_marker = 0;
			for(final Signal s : this.signals){
				if(s == null) continue;
				final Vector<Polygon> segments = this.wm.toPolygons(s, d, this.appendDrawMode);
				int marker_step = (int)(((s.getNumPoints() > 1000) ? 100 : s.getNumPoints() / 10.) + 0.5);
				this.drawMarkers(g, segments, Marker.values()[curr_marker + 1], marker_step, s.getMode1D());
				curr_marker = (curr_marker + 1) % num_marker;
			}
		} else for(final Signal s : this.signals){
			if(s == null) continue;
			this.drawSignal(s,g,d,print_mode);
		}
	}

	@Override
	protected Point findPoint(final double curr_x, final double curr_y, final boolean is_first) {
		return this.findPoint(curr_x, curr_y, this.getWaveSize(), is_first);
	}

	@Override
	protected Point findPoint(final double curr_x, final double curr_y, final Dimension d, final boolean is_first) {
		Signal curr_signal;
		int curr_idx = -1, i, img_idx = -1;
		double curr_dist = 0, min_dist = Double.POSITIVE_INFINITY;
		if(this.signals == null || this.signals.size() == 0) return null;
		// if(signals[curr_point_sig_idx] == null) return 0;
		if(!is_first) return this.findPoint(this.signals.elementAt(this.curr_point_sig_idx), curr_x, curr_y, d);
		for(this.curr_point_sig_idx = i = 0; i < this.signals.size(); i++){
			curr_signal = this.signals.elementAt(i);
			if(curr_signal == null || !this.getSignalState(i)) continue;
			curr_idx = curr_signal.FindClosestIdx(curr_x, curr_y);
			if(curr_signal.getMode2D() == Signal.Mode2D.IMAGE || curr_signal.getMode2D() == Signal.Mode2D.CONTOUR){
				final double x2D[] = curr_signal.getX2D();
				int inc = (int)(x2D.length / 10.) + 1;
				inc = (curr_idx + inc > x2D.length) ? x2D.length - curr_idx - 1 : inc;
				// if(curr_idx >= 0 && curr_idx < x2D.length) img_dist =
				// (x2D[curr_idx] - x2D[curr_idx + inc]) * (x2D[curr_idx] -
				// x2D[curr_idx + inc]);
				img_idx = i;
			}else{
				if(curr_signal.hasX()) curr_dist = (curr_signal.getY(curr_idx) - curr_y) * (curr_signal.getY(curr_idx) - curr_y) + (curr_signal.getX(curr_idx) - curr_x) * (curr_signal.getX(curr_idx) - curr_x);
				if(i == 0 || curr_dist < min_dist){
					min_dist = curr_dist;
					this.curr_point_sig_idx = i;
				}
			}
		}
		try{
			if(img_idx != -1) if(curr_idx != -1){
				curr_signal = this.signals.elementAt(this.curr_point_sig_idx);
				if(min_dist > 10 * (curr_signal.getY(0) - curr_signal.getY(1)) * (curr_signal.getY(0) - curr_signal.getY(1))) this.curr_point_sig_idx = img_idx;
			}else this.curr_point_sig_idx = img_idx;
		}catch(final Exception exc){
			/**/}
		this.setPointSignalIndex(this.curr_point_sig_idx);
		curr_signal = this.signals.elementAt(this.curr_point_sig_idx);
		this.not_drawn = true;
		final Point p = this.findPoint(curr_signal, curr_x, curr_y, d);
		return p;
	}

	@Override
	protected int getBottomSize() {
		return this.bottom_size;
	}

	protected Dimension getLegendDimension(final Graphics g, final Dimension d, final int orientation) {
		final Dimension dim = new Dimension(0, 0);
		int curr_width = 0, sum_width = 0;
		final Font f = g.getFont();
		final int h = f.getSize() + 2;
		final FontMetrics fm = this.getFontMetrics(f);
		if(this.getSignalCount() == 0) return dim;
		for(int i = 0; i < this.getSignalCount(); i++){
			if(!this.isSignalShow(i)) continue;
			final String lab = this.getSignalInfo(i);
			final char[] lab_ar = lab.toCharArray();
			curr_width = fm.charsWidth(lab_ar, 0, lab_ar.length);
			if(orientation == MultiWaveform.VERTICAL){
				curr_width += 2 * this.marker_width;
				dim.height += h;
				if(curr_width > dim.width) dim.width = curr_width;
			}
			if(orientation == MultiWaveform.HORIZONTAL){
				curr_width += 3 * this.marker_width;
				if(sum_width + curr_width < d.width) sum_width += curr_width;
				else{
					if(sum_width > dim.width) dim.width = sum_width;
					sum_width = curr_width;
					dim.height += h;
				}
			}
		}
		dim.height += (orientation == MultiWaveform.HORIZONTAL) ? (int)(3. / 2 * h + 0.5) : h / 2;
		return dim;
	}

	@Override
	protected int getRightSize() {
		return this.right_size;
	}

	protected Color getSignalColor(final int i) {
		if(i > this.signals.size()) return Color.black;
		final Signal sign = this.signals.elementAt(i);
		if(sign.getColor() != null) return sign.getColor();
		return Waveform.colors[sign.getColorIdx() % Waveform.colors.length];
	}

	protected String getSignalInfo(final int i) {
		final Signal sign = this.signals.elementAt(i);
		String lab = sign.getName();
		switch(sign.getMode2D()){
			case ZX:
				lab = lab + " [X-Z Y = " + Waveform.convertToString(sign.getYinXZplot(), false) + " ]";
				break;
			case ZY:
				lab = lab + " [Y-Z X = " + sign.getStringOfXinYZplot() +
				// Waveform.ConvertToString(sign.getTime(), false) +
				        " ]";
				break;
			/*
			 * case Signal.MODE_YX: lab = lab + " [Y-X T = " +
			 * sign.getStringTime() + //
			 * Waveform.ConvertToString(sign.getTime(), false) " ]"; break;
			 */
			default:
				break;
		}
		return lab;
	}

	@Override
	protected void handleCopy() {
		/*
		 * if(IsCopySelected()) return; if(signals != null && signals.length !=
		 * 0 && controller.GetCopySource() == null || is_image && frames != null
		 * && controller.GetCopySource() == null ) {
		 * controller.SetCopySource(this); SetCopySelected(true); }
		 */
	}

	@Override
	protected void handlePaste() {
		/*
		 * if(IsCopySelected()) { SetCopySelected(false);
		 * controller.SetCopySource(null); } else {
		 * if(controller.GetCopySource() != null) controller.NotifyChange(this,
		 * controller.GetCopySource()); }
		 */
	}

	@Override
	protected boolean hasPending() {
		synchronized(this.signals){
			for(final Signal sig : this.signals)
				if(sig != null && sig.hasPending()) return true;
		}
		return false;
	}

	protected boolean isSignalShow(final int i) {
		final Signal sign = this.signals.elementAt(i);
		return(sign != null && (sign.getInterpolate() || sign.getMarker() != Signal.Marker.NONE));
	}

	@Override
	protected void notifyZoom(final double start_xs, final double end_xs, final double start_ys, final double end_ys, final int timestamp) {
		if(this.orig_signals == null){
			this.orig_signals = new Vector<Signal>();
			for(int i = 0; i < this.signals.size(); i++)
				this.orig_signals.addElement(this.signals.elementAt(i));
			this.orig_xmin = this.waveform_signal.getXmin();
			this.orig_xmax = this.waveform_signal.getXmax();
		}
	}

	@Override
	synchronized protected void paintSignal(final Graphics g, final Dimension d, final int print_mode) {
		Dimension dim;
		if(print_mode == Waveform.NO_PRINT) dim = this.getWaveSize();
		else dim = this.getPrintWaveSize(d);
		super.paintSignal(g, d, print_mode);
		if(this.show_legend && !this.fixed_legend && !this.is_min_size){
			Point p = new Point();
			if(this.legend_point == null || this.prev_width != d.width || this.prev_height != d.height){
				p.x = this.wm.toXPixel(this.legend_x, dim);
				p.y = this.wm.toYPixel(this.legend_y, dim);
				this.legend_point = p;
			}else p = this.legend_point;
			this.drawLegend(g, p, print_mode, MultiWaveform.VERTICAL);
		}
		if(this.fixed_legend && this.show_legend || (print_mode & MultiWaveform.PRINT_LEGEND) == MultiWaveform.PRINT_LEGEND){
			g.setClip(0, 0, d.width, d.height);
			if(this.legend_mode == MultiWaveform.LEGEND_BOTTOM && this.bottom_size != 0) this.drawLegend(g, new Point(0, dim.height), print_mode, MultiWaveform.HORIZONTAL);
			if(this.legend_mode == MultiWaveform.LEGEND_RIGHT && this.right_size != 0) this.drawLegend(g, new Point(dim.width, 0), print_mode, MultiWaveform.VERTICAL);
		}
	}

	@Override
	protected void reportLimits(final ZoomRegion r, final boolean add_undo) {
		if(!add_undo){
			if(this.waveform_signal == null) return;
			this.update_timestamp++;
			if(this.signals == null) return;
			if(this.orig_signals != null){// Previous zoom
				this.signals = this.orig_signals;
				this.orig_signals = null;
			}
		}
		super.reportLimits(r, add_undo);
		if(add_undo) this.notifyZoom(r.start_xs, r.end_xs, r.start_ys, r.end_ys, this.update_timestamp);
	}

	protected void setLimits() {
		this.setXlimits(this.lx_min, this.lx_max);
		this.setYlimits(this.ly_min, this.ly_max);
		this.updateLimits();
		this.change_limits = true;
	}

	private void setLimits(final int mode) {
		if(this.waveform_signal == null || this.signals == null) return;
		double xmin = Double.POSITIVE_INFINITY, ymin = Double.POSITIVE_INFINITY, xmax = Double.NEGATIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
		for(final Signal signal : this.signals){
			if(signal == null) continue;
			if(Double.isFinite(signal.getXmin()) && signal.getXmin() < xmin) xmin = signal.getXmin();
			if(Double.isFinite(signal.getXmax()) && signal.getXmax() > xmax) xmax = signal.getXmax();
			if(Double.isFinite(signal.getYmin()) && signal.getYmin() < ymin) ymin = signal.getYmin();
			if(Double.isFinite(signal.getYmax()) && signal.getYmax() > ymax) ymax = signal.getYmax();
		}
		this.waveform_signal.setXLimits(xmin, xmax, mode);
		this.waveform_signal.setYmax(ymax, mode);
		this.waveform_signal.setYmin(ymin, mode);
	}
}
