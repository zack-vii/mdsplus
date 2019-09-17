package jscope.waveform;

/* $Id$ */
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Locale;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import debug.DEBUG;
import jscope.data.framedata.FrameData;
import jscope.data.framedata.Frames;
import jscope.data.signal.Signal;
import jscope.data.signal.Signal.Marker;
import jscope.data.signal.Signal.Mode1D;
import jscope.data.signal.Signal.Mode2D;
import jscope.data.signal.Signal.Type;
import jscope.data.signal.SignalListener;
import jscope.dialog.ColorMap;
import jscope.dialog.ColorMap.ColorProfile;
import jscope.dialog.ProfileDialog;

public class Waveform extends JComponent implements SignalListener{
	private static final long serialVersionUID = 1L;
	final class ZoomRegion{
		public final double	end_xs;
		public final double	end_ys;
		public final double	start_xs;
		public final double	start_ys;

		ZoomRegion(final double start_xs, final double end_xs, final double start_ys, final double end_ys){
			if(DEBUG.M) System.out.println("new Waveform.ZoomRegion(" + end_xs + ", " + start_ys + ", " + end_ys + ")");
			this.start_xs = start_xs;
			this.end_xs = end_xs;
			this.start_ys = start_ys;
			this.end_ys = end_ys;
		}
	}
	public static final String[]	COLOR_NAME			= {"Black", "Blue", "Cyan", "DarkGray", "Gray", "Green", "LightGray", "Magenta", "Orange", "Pink", "Red", "Yellow"};
	public static final Color[]		COLOR_SET			= {Color.black, Color.blue, Color.cyan, Color.darkGray, Color.gray, Color.green, Color.lightGray, Color.magenta, Color.orange, Color.pink, Color.red, Color.yellow};
	public static Color[]			colors;
	public static int				ixxxx				= 0;
	public static double			mark_point_x, mark_point_y;
	public static final int			MARKER_WIDTH		= 8;
	public static final int			MAX_POINTS			= 5000;
	public static final int			MAX_DRAG_POINT		= 200;
	public static final int			MODE_COPY			= 4;
	public static final int			MODE_PAN			= 3;
	public static final int			MODE_POINT			= 2;
	public static final int			MODE_WAIT			= 5;
	public static final int			MODE_ZOOM			= 1;
	public static final int			NO_PRINT			= 0;
	public static final int			PRINT				= 2;
	public static final int			PRINT_BW			= 4;
	public static boolean			zoom_on_mb1			= true;
	protected static boolean		colors_changed		= true;
	protected static String[]		colors_name;
	protected static Font			font				= null;
	protected static int			horizontal_offset	= 0;
	protected static int			vertical_offset		= 0;
	private static boolean			bug_image			= true;
	private static final int		MIN_H				= 10;
	private static final int		MIN_W				= 10;

	public static final int colorNameToIndex(final String name) {
		if(DEBUG.M) System.out.println("Waveform.colorNameToIndex(\"" + name + "\")");
		if(name != null) for(int i = 0; i < Waveform.COLOR_NAME.length; i++)
			if(name.toLowerCase().equals(Waveform.COLOR_NAME[i].toLowerCase())) return i;
		return 0;
	}

	public static final String convertToString(final double value, final boolean is_log) {
		if(DEBUG.M) System.out.println("Waveform.convertToString(" + value + ", " + is_log + ")");
		if(value == 0) return "0";
		final double val, absval = Math.abs(value);
		final int E;
		if(absval >= 1e6 || absval < .1){
			E = (int)Math.log10(absval);
			val = value / Math.pow(10, E);
		}else{
			val = value;
			E = 0;
		}
		final String tmp = String.format(Locale.US, "%f", new Double(val));
		if(E == 0) return tmp.replaceAll("0*$", "");
		return new StringBuilder(16).append(tmp.replaceAll("\\.?0*$", "")).append('e').append(E).toString();
	}

	public static final Color[] getColors() {
		Waveform.colors_changed = false;
		return Waveform.colors;
	}

	public static final String[] getColorsName() {
		Waveform.colors_changed = false;
		return Waveform.colors_name;
	}

	public static final void getFont(final Graphics g) {
		if(DEBUG.M) System.out.println("Waveform.setFont(" + g + ")");
		final Font f = g.getFont();
		if(Waveform.font == null) Waveform.font = f;
		else if(!Waveform.font.equals(f)) g.setFont(Waveform.font);
	}

	public static final int getHorizontalOffset() {
		return Waveform.horizontal_offset;
	}

	public static final int getVerticalOffset() {
		return Waveform.vertical_offset;
	}

	public static final boolean isColorsChanged() {
		return Waveform.colors_changed;
	}

	public final static boolean isHMS(final Signal s) {
		return s != null && s.isLongXForLabel() && s.getXmin() > 1e12;
	}

	public static final void setColors(final Color _colors[], final String _colors_name[]) {
		Waveform.colors_changed = true;
		Waveform.colors = _colors;
		Waveform.colors_name = _colors_name;
	}

	public static final void setDefaultColors() {
		if(DEBUG.M) System.out.println("Waveform.setDefaultColors()");
		if(Waveform.colors != null && Waveform.colors_name != null && (Waveform.colors != Waveform.COLOR_SET || Waveform.colors_name != Waveform.COLOR_NAME)) return;
		Waveform.colors = Waveform.COLOR_SET;
		Waveform.colors_name = Waveform.COLOR_NAME;
	}

	public static final void setHorizontalOffset(final int h_offset) {
		Waveform.horizontal_offset = h_offset;
	}

	public static final void setStaticFont(final Font f) {
		Waveform.font = f;
	}

	public static final void setVerticalOffset(final int v_offset) {
		Waveform.vertical_offset = v_offset;
	}
	public Frames							frames;
	public double							lx_max				= Double.POSITIVE_INFINITY;
	public double							lx_min				= Double.NEGATIVE_INFINITY;
	public float							ly_max				= Float.POSITIVE_INFINITY;
	public float							ly_min				= Float.NEGATIVE_INFINITY;
	protected boolean						appendDrawMode		= false;
	protected boolean						appendPaintFlag		= false;
	protected boolean						border_changed		= false;
	protected boolean						change_limits		= false;
	protected ColorProfile					colorProfile;
	protected Color							crosshair_color;
	protected Rectangle						curr_display_limits;
	protected double						curr_point;
	protected double						curr_point_y;
	protected Rectangle						curr_rect			= null;
	protected boolean						dragging, copy_selected = false, resizing = true;
	protected int							end_x;
	protected int							end_y;
	protected boolean						first_set_point;
	protected int							frame				= 0;
	protected double						frame_time			= 0;
	protected Rectangle						frame_zoom			= null;
	protected Grid							grid;
	protected int							grid_step_x			= 3, grid_step_y = 3;
	protected boolean						int_xlabel			= true, int_ylabel = true;
	protected boolean						is_image			= false;
	protected boolean						is_mb2, is_mb3;
	protected boolean						is_min_size;
	protected boolean						is_playing			= false;
	protected boolean						is_select;
	protected boolean						just_deselected;
	protected int							marker_width;
	protected WavePopup						menu;
	protected int							mode_saved, mode, grid_mode;
	protected boolean						not_drawn			= true;
	protected int							num_points;
	protected Graphics						off_graphics;
	protected Image							off_image;
	protected int							orig_x;
	protected int							orig_y;
	protected double						pan_delta_x, pan_delta_y;
	protected int							pan_x, pan_y;
	protected Point[]						points;
	protected Polygon						polygon;
	protected int							prev_frame			= -1;
	protected int							prev_height;
	protected int							prev_point_x;
	protected int							prev_point_y;
	protected int							prev_width;
	protected boolean						reversed			= false;
	protected Border						select_border;
	protected boolean						show_measure		= false;
	protected int							start_x;
	protected int							start_y;
	protected Border						unselect_border;
	protected int							update_timestamp;
	protected Rectangle						wave_b_box;
	protected String						wave_error			= null;
	protected double						wave_point_x		= Double.NaN;
	protected double						wave_point_y		= Double.NaN;
	protected Signal						waveform_signal;
	protected WaveformMetrics				wm;
	protected String						x_label, y_label, z_label, title;
	protected boolean						x_log, y_log;
	private boolean							event_enabled		= true;
	private boolean							execute_print		= false;
	private boolean							pan_enabled			= true;
	private final javax.swing.Timer			play_timer;
	private ProfileDialog					profDialog;
	private String							properties;
	private boolean							restart_play		= false;
	private boolean							send_profile		= false;
	private final Vector<ZoomRegion>		undo_zoom			= new Vector<ZoomRegion>();
	private final Vector<WaveformListener>	waveform_listener	= new Vector<WaveformListener>();
	private double							xmax				= Double.NEGATIVE_INFINITY;
	private double							xmin				= Double.POSITIVE_INFINITY;
	private double							ymax				= Double.NEGATIVE_INFINITY;
	private double							ymin				= Double.POSITIVE_INFINITY;

	public Waveform(){
		if(DEBUG.M) System.out.println("Waveform()");
		this.setName("Waveform_" + (Waveform.ixxxx++));
		this.setBorder(BorderFactory.createLoweredBevelBorder());
		this.setSelectBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.red, Color.red));
		this.play_timer = new javax.swing.Timer(100, new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent evt) {
				if(Waveform.this.frames == null){
					Waveform.this.play_timer.stop();
					return;
				}
				Waveform.this.frame = Waveform.this.frames.getNextFrameIdx();
				if(Waveform.this.frame == Waveform.this.frames.getNumFrame() - 1) Waveform.this.frame = 0;
				Waveform.this.repaint();
				if(Waveform.this.mode == Waveform.MODE_POINT){
					Waveform.this.sendUpdateEvent();
					if(Waveform.this.send_profile) Waveform.this.sendProfileEvent();
				}
			}
		});
		this.prev_point_x = this.prev_point_y = -1;
		this.update_timestamp = 0;
		this.waveform_signal = null;
		this.mode = Waveform.MODE_ZOOM;
		this.dragging = false;
		this.grid_mode = Grid.IS_DOTTED;
		this.first_set_point = true;
		this.marker_width = Waveform.MARKER_WIDTH;
		this.x_log = this.y_log = false;
		this.setMouse();
		this.setKeys();
		Waveform.setDefaultColors();
	}

	public Waveform(final Signal s){
		this();
		this.waveform_signal = s;
		this.waveform_signal.registerSignalListener(this);
		this.not_drawn = true;
	}

	public final void addWaveformListener(final WaveformListener l) {
		if(l != null) synchronized(this.waveform_listener){
			this.waveform_listener.addElement(l);
		}
	}

	public final synchronized void appendPaint(final Graphics g, final Dimension d) {
		if(DEBUG.M) System.out.println("Waveform.appendPaint(" + g + ", " + d + ")");
//		Waveform.getFont(g);
		g.setColor(Color.black);
		if(!this.is_image){
			if(this.wm != null){
				this.appendDrawMode = true;
				this.appendPaintFlag = true;
				//this.drawSignal(g, d, Waveform.NO_PRINT);
				this.appendDrawMode = false;
			}else this.paintSignal(g, d, Waveform.NO_PRINT);
		}else this.paintImage(g, d, Waveform.NO_PRINT);
	}

	public void appendUpdate() {
		this.appendPaint(this.getGraphics(), this.getSize());
	}

	public final void applyColorMap(final ColorMap cm) {
		if(cm == null) return;
		if(this.frames != null) this.frames.applyColorMap(cm);
		else this.colorProfile.colorMap = cm;
		this.not_drawn = true;
		this.repaint();
	}

	public void autoscale() {
		if(DEBUG.M) System.out.println("Waveform.autoscale()");
		if(this.undoZoomPendig()) this.addZoomRegion();
		if(this.is_image && this.frames != null && this.frames.getNumFrame() != 0) this.frames.resize();
		else{
			if(this.waveform_signal == null) return;
			this.waveform_signal.Autoscale();
		}
		this.reportChanges();
	}

	public void autoscaleY() {
		if(this.waveform_signal == null) return;
		this.waveform_signal.AutoscaleY();
		this.reportChanges();
	}

	public void checkPending() {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				if(Waveform.this.hasPending()) Waveform.this.setMode(Waveform.MODE_WAIT);
				else Waveform.this.resetMode();
			}
		});
	}

	public void copy(final Waveform wave) {
		if(DEBUG.M) System.out.println("Waveform.copy(" + wave + ")");
		if(wave.is_image){
			this.frames = new Frames(wave.frames);
			this.frame = wave.frame;
			this.prev_frame = wave.prev_frame;
			this.frame_time = wave.frame_time;
			this.is_image = true;
		}else this.is_image = false;
		this.not_drawn = true;
		this.repaint();
	}

	public final void deselectWave() {
		if(DEBUG.M) System.out.println("Waveform.deselectWave()");
		this.is_select = false;
		this.border_changed = true;
		if(!this.unselect_border.getBorderInsets(this).equals(this.select_border.getBorderInsets(this))) this.not_drawn = true;
		this.setBorder(this.unselect_border);
	}

	public final void dispatchWaveformEvent(final WaveformEvent e) {
		if(e == null || !this.event_enabled) return;
		if(e.getID() == WaveformEvent.COPY_PASTE){
			final Thread thread = new Thread(){
				@Override
				public void run() {
					Waveform.this.dispatchWaveformEvent_(e);
				}
			};
			thread.setName("dispatchWaveformEvent: " + e.getStatusInfo());
			thread.start();
		}else this.dispatchWaveformEvent_(e);
	}

	public final void drawContourLevel(final Vector<Vector<Point2D.Double>> cOnLevel, final Graphics g, final Dimension d) {
		if(DEBUG.A) System.out.println("Waveform.drawContourLevel(" + cOnLevel + ", " + g + ", " + d + ")");
		Vector<Point2D.Double> c;
		Point2D.Double p;
		this.wm.computeFactors(d);
		for(int i = 0; i < cOnLevel.size(); i++){
			c = cOnLevel.elementAt(i);
			final int cx[] = new int[c.size()];
			final int cy[] = new int[c.size()];
			for(int j = 0; j < c.size(); j++){
				p = c.elementAt(j);
				cx[j] = this.wm.toXPixel(p.x);
				cy[j] = this.wm.toYPixel(p.y);
			}
			g.drawPolyline(cx, cy, c.size());
		}
	}

	public final void drawError(final Signal sig, final Graphics g, final Dimension d) {
		if(DEBUG.A) System.out.println("Waveform.drawError(" + sig + ", " + g + ", " + d + ")");
		if(!sig.hasError()) return;
		int up, low, x;
		final float up_error[] = sig.getUpError();
		final float low_error[] = sig.getLowError();
		for(int i = 0; i < sig.getNumPoints(); i++){
			up = this.wm.toYPixel(up_error[i] + sig.getY(i), d);
			if(!sig.hasAsymError()) low = this.wm.toYPixel(sig.getY(i) - up_error[i], d);
			else low = this.wm.toYPixel(sig.getY(i) - low_error[i], d);
			x = this.wm.toXPixel(sig.getX(i), d);
			g.drawLine(x, up, x, low);
			g.drawLine(x - 2, up, x + 2, up);
			g.drawLine(x - 2, low, x + 2, low);
		}
	}

	private final void drawWave(final Signal s, final Graphics g, final Dimension d) {
		if(DEBUG.M) System.out.println("Waveform.drawWave(" + s + ", " + g + ", " + d + ")");
		final Vector<Polygon> segments = this.wm.toPolygons(s, d, this.appendDrawMode);
		Polygon curr_polygon;
		if(s.getColor() != null) g.setColor(s.getColor());
		else g.setColor(Waveform.colors[s.getColorIdx() % Waveform.colors.length]);
		for(int k = 0; k < segments.size(); k++){
			curr_polygon = segments.elementAt(k);
			if(s.getInterpolate()) g.drawPolyline(curr_polygon.xpoints, curr_polygon.ypoints, curr_polygon.npoints);
		}
	}

	public void erase() {
		if(DEBUG.M) System.out.println("Waveform.erase()");
		this.update_timestamp = 0;
		this.waveform_signal = null;
		this.dragging = false;
		this.first_set_point = true;
		this.marker_width = Waveform.MARKER_WIDTH;
		this.x_log = this.y_log = false;
		this.off_image = null;
		this.not_drawn = true;
		this.frames = null;
		this.prev_frame = -1;
		this.grid = null;
		this.x_label = null;
		this.y_label = null;
		this.not_drawn = true;
		if(this.profDialog != null) this.profDialog.dispose();
		this.repaint();
	}

	public final int getColorIdx() {
		if(DEBUG.M) System.out.println("Waveform.getColorIdx()");
		if(this.waveform_signal != null) return this.waveform_signal.getColorIdx();
		else if(this.frames != null) return this.frames.getColorIdx();
		return 0;
	}

	public ColorProfile getColorProfile() {
		return this.frames.getColorProfile();
	}

	public final int getGridMode() {
		return this.grid_mode;
	}

	public final int getGridStepX() {
		return this.grid_step_x;
	}

	public final int getGridStepY() {
		return this.grid_step_y;
	}

	public final Image getImage() {
		return this.off_image;
	}

	public final boolean getInterpolate() {
		return(this.waveform_signal != null ? this.waveform_signal.getInterpolate() : true);
	}

	public final String getIntervalPoints() {
		if(DEBUG.M) System.out.println("Waveform.getIntervalPoints()");
		final Dimension d = this.getWaveSize();
		double curr_x = this.wm.toXValue(this.end_x, d);
		double curr_y = this.wm.toYValue(this.end_y, d);
		curr_x = this.wave_point_x;
		curr_y = this.wave_point_y;
		return " " + Waveform.mark_point_x + " " + Waveform.mark_point_y + " " + curr_x + " " + curr_y;
	}

	public final Marker getMarker() {
		if(this.waveform_signal == null) return Marker.DEFAULT;
		return this.waveform_signal.getMarker();
	}

	public final int getMarkerStep() {
		return(this.waveform_signal != null ? this.waveform_signal.getMarkerStep() : 0);
	}

	@Override
	public final Dimension getMinimumSize() {
		if(DEBUG.M) System.out.println("Waveform.getMinimumSize()");
		final Insets i = this.getInsets();
		return new Dimension(Waveform.MIN_W + i.right + i.left, Waveform.MIN_H + i.top + i.bottom);
	}

	public final int getMode() {
		return this.mode;
	}

	public final String getProperties() {
		return this.properties;
	}

	public int getShowSignalCount() {
		return (this.waveform_signal != null) ? 1 : 0;
	}

	public Signal getSignal() {
		return this.waveform_signal;
	}

	public Mode1D getSignalMode1D() {
		if(this.waveform_signal == null) return Mode1D.DEFAULT;
		return this.waveform_signal.getMode1D();
	}

	public Mode2D getSignalMode2D() {
		if(this.waveform_signal == null) return Mode2D.DEFAULT;
		return this.waveform_signal.getMode2D();
	}

	public final String getSignalName() {
		if(DEBUG.M) System.out.println("Waveform.getSignalName()");
		if(this.is_image && this.frames != null) return this.frames.getName();
		return(this.waveform_signal != null ? this.waveform_signal.getName() : "");
	}

	public Type getSignalType() {
		if(this.waveform_signal == null) return Type.DEFAULT;
		return this.waveform_signal.getType();
	}

	public final String getTitle() {
		return this.title;
	}

	public final WaveformMetrics getWaveformMetrics() {
		return this.wm;
	}

	public final String getXLabel() {
		return this.x_label;
	}

	public final String getYLabel() {
		return this.y_label;
	}

	public final boolean isCopySelected() {
		return this.copy_selected;
	}

	public final boolean isImage() {
		return this.is_image;
	}

	public final boolean isReversed() {
		return this.reversed;
	}

	public final boolean isSelected() {
		return this.is_select;
	}

	public final boolean isSendProfile() {
		return this.send_profile;
	}

	public final boolean isShowSigImage() {
		return this.getSignalMode2D() == Signal.Mode2D.IMAGE;
	}

	public boolean isWaveformVisible() {
		final Dimension d = this.getWaveSize();
		return !(d.width <= 0 || d.height <= 0);
	}

	public synchronized void paint(final Graphics g, Dimension d, final int print_mode) {
		if(DEBUG.M) System.out.println("Waveform.paint(" + g + ", " + d + ", " + print_mode + ")");
		this.execute_print = (print_mode != Waveform.NO_PRINT);
		final Insets i = this.getInsets();
		Waveform.font = g.getFont();
		Dimension dim;
		if(this.not_drawn || this.prev_width != d.width || this.prev_height != d.height || this.execute_print || (this.is_image && this.prev_frame != this.frame) || this.appendPaintFlag){
			this.appendPaintFlag = false;
			this.not_drawn = false;
			if(print_mode == Waveform.NO_PRINT){
				this.resizing = this.prev_width != d.width || this.prev_height != d.height;
				if(this.resizing) if(this.is_image && this.frames != null){
					Point p = this.frames.getFramePoint(new Point(this.end_x, this.end_y), new Dimension(this.prev_width, this.prev_height));
					p = this.frames.getImagePoint(p, d);
					this.end_x = p.x;
					this.end_y = p.y;
				}
				dim = this.getWaveSize();
				this.is_min_size = (dim.width < Waveform.MIN_W || dim.height < Waveform.MIN_H);
				if(Waveform.bug_image){
					Waveform.bug_image = false;
					this.off_image = this.createImage(1, 1);
					this.off_graphics = this.off_image.getGraphics();
					this.off_graphics.drawString("", 0, 0);
					this.off_graphics.dispose();
				}
				this.off_image = this.createImage(d.width, d.height);
				this.off_graphics = this.off_image.getGraphics();
				Waveform.getFont(this.off_graphics);
				this.paintBorder(this.off_graphics);
				this.border_changed = false;
				this.off_graphics.translate(i.right, i.top);
			}else{
				this.resizing = true;
				dim = d;
				this.off_graphics = g;
				g.setColor(Color.BLUE);
				this.off_graphics.drawRect(0, 0, d.width - 1, d.height - 1);
			}
			this.off_graphics.setClip(0, 0, dim.width, dim.height);
			if(this.is_image) this.paintImage(this.off_graphics, d, print_mode);
			else this.paintSignal(this.off_graphics, d, print_mode);
			if(print_mode == Waveform.NO_PRINT){
				// this.off_graphics.translate(-i.right, -i.top);
				// this.off_graphics.setClip(0, 0, d.width, d.height);
				this.prev_width = d.width;
				this.prev_height = d.height;
			}
		}else if(this.border_changed){
			this.paintBorder(this.off_graphics);
			this.border_changed = false;
		}
		if(this.execute_print){
			this.execute_print = false;
			System.gc();
			return;
		}
		if(!(this.mode == Waveform.MODE_PAN && this.dragging && this.waveform_signal != null) || this.is_image) if(this.off_image != null) g.drawImage(this.off_image, 0, 0, this);
		g.translate(i.right, i.top);
		if(this.mode == Waveform.MODE_ZOOM) if(this.curr_rect != null){
			if(this.is_image && this.crosshair_color != null) g.setColor(this.crosshair_color);
			else g.setColor(this.reversed ? Color.white : Color.black);
			g.drawRect(this.curr_rect.x, this.curr_rect.y, this.curr_rect.width, this.curr_rect.height);
		}
		if(this.is_image) this.imageActions(g, d = this.getWaveSize());
		else this.signalActions(g, d = this.getWaveSize());
		if(this.show_measure && this.mode == Waveform.MODE_POINT){
			int mark_px, mark_py;
			final Color c = g.getColor();
			g.setColor(Color.red);
			if(this.is_image){
				mark_px = (int)Waveform.mark_point_x;
				mark_py = (int)Waveform.mark_point_y;
				final Point mp = this.frames.getMeasurePoint(d);
				mark_px = mp.x;
				mark_py = mp.y;
			}else{
				mark_px = this.wm.toXPixel(Waveform.mark_point_x, d);
				mark_py = this.wm.toYPixel(Waveform.mark_point_y, d);
			}
			g.drawLine(mark_px, 0, mark_px, d.height);
			g.drawLine(0, mark_py, d.width, mark_py);
			g.setColor(c);
		}
		g.translate(-i.right, -i.top);
	}

	@Override
	public final void paintComponent(final Graphics g) {
		if(this.execute_print) return;
		final Dimension d = this.getSize();
		this.paint(g, d, Waveform.NO_PRINT);
		if(this.mode == Waveform.MODE_POINT && this.send_profile) this.sendProfileEvent();
	}

	public final synchronized void paintImage(final Graphics g, final Dimension d, final int print_mode) {
		if(DEBUG.M) System.out.println("Waveform.paintImage(" + g + ", " + d + ", " + print_mode + ")");
		if(this.frames != null){
			this.drawFrame(g, d, this.frame);
			this.prev_frame = this.frames.getFrameIdx();
		}else{
			this.prev_frame = -1;
			this.curr_rect = null;
		}
		this.grid = new Grid(this.xmax, this.ymax, this.xmin, this.ymin, this.x_log, this.y_log, this.grid_mode, this.x_label, this.y_label, this.title, this.wave_error, this.grid_step_x, this.grid_step_y, this.int_xlabel, this.int_ylabel, true);
		if(!this.is_min_size) this.grid.paint(g, d, this, null);
	}

	public final void performZoom() {
		if(this.wm == null) return;
		final Dimension d = this.getWaveSize();
		final double start_xs = this.wm.toXValue(this.start_x, d), end_xs = this.wm.toXValue(this.end_x, d);
		final double start_ys = this.wm.toYValue(this.start_y, d), end_ys = this.wm.toYValue(this.end_y, d);
		this.reportLimits(new ZoomRegion(start_xs, end_xs, start_ys, end_ys), true);
	}

	public final void playFrame() {
		if(DEBUG.M) System.out.println("Waveform.playFrame()");
		if(!this.is_playing){
			this.is_playing = true;
			this.play_timer.start();
		}
	}

	public final boolean playing() {
		return this.is_playing;
	}

	public final void removeWaveformListener(final WaveformListener l) {
		if(l != null) synchronized(this.waveform_listener){
			this.waveform_listener.removeElement(l);
		}
	}

	public final void repaint(final boolean state) {
		this.not_drawn = state;
		this.repaint();
	}

	public final void reportChanges() {
		this.wm = null;
		this.not_drawn = true;
		if(this.mode == Waveform.MODE_POINT && this.is_image && this.frames != null){
			final Point p = this.frames.getFramePoint(this.getWaveSize());
			this.end_x = p.x;
			this.end_y = p.y;
		}
		this.repaint();
		if(this.mode == Waveform.MODE_POINT){
			this.sendUpdateEvent();
			if(this.is_image && this.send_profile) this.sendProfileEvent();
		}
		if(this.waveform_signal != null) this.notifyZoom(this.waveform_signal.getXmin(), this.waveform_signal.getXmax(), this.waveform_signal.getYmin(), this.waveform_signal.getYmax(), this.update_timestamp);
	}

	public void resetScales() {
		if(this.waveform_signal == null) return;
		this.waveform_signal.ResetScales();
		this.undo_zoom.clear();
		this.waveform_signal.unfreeze();
		this.reportChanges();
	}

	public final void selectWave() {
		if(DEBUG.M) System.out.println("Waveform.selectWave()");
		if(!this.is_select){
			this.is_select = true;
			this.border_changed = true;
			this.unselect_border = this.getBorder();
			if(!this.unselect_border.getBorderInsets(this).equals(this.select_border.getBorderInsets(this))) this.not_drawn = true;
			this.setBorder(this.select_border);
		}
	}

	public final void sendCutEvent() {
		final WaveformEvent we = new WaveformEvent(this, WaveformEvent.COPY_CUT);
		this.dispatchWaveformEvent(we);
	}

	// Send Waveform event routines
	public final void sendPasteEvent() {
		final WaveformEvent we = new WaveformEvent(this, WaveformEvent.COPY_PASTE);
		this.dispatchWaveformEvent(we);
	}

	public final void sendProfileEvent() {
		if(DEBUG.M) System.out.println("Waveform.sendProfileEvent()");
		if(this.frames != null && this.frames.getNumFrame() != 0){
			WaveformEvent we;
			final Dimension d = this.getWaveSize();
			final Point p = this.frames.getFramePoint(new Point(this.end_x, this.end_y), d);
			final int frame_type = this.frames.getFrameType();
			if(frame_type == FrameData.BITMAP_IMAGE_32 || frame_type == FrameData.BITMAP_IMAGE_16){
				we = new WaveformEvent(this, p.x, p.y, (float)(Math.round(this.frames.getFrameTime() * 10000) / 10000.), this.frames.getName(), this.frames.getValuesX(p.y), this.frames.getStartPixelX(), this.frames.getValuesY(p.x), this.frames.getStartPixelY());
				if(this.show_measure){
					final Point p_pos = new Point((int)Waveform.mark_point_x, (int)Waveform.mark_point_y);
					final Point mp = this.frames.getFramePoint(p_pos, d);
					we.setValuesLine(this.frames.getValuesLine(mp.x, mp.y, p.x, p.y));
				}
				we.setFrameType(frame_type);
			}else{
				we = new WaveformEvent(this, p.x, p.y, (float)(Math.round(this.frames.getFrameTime() * 10000) / 10000.), this.frames.getName(), this.frames.getPixelsX(p.y), this.frames.getStartPixelX(), this.frames.getPixelsY(p.x), this.frames.getStartPixelY());
				if(this.show_measure){
					final Point p_pos = new Point((int)Waveform.mark_point_x, (int)Waveform.mark_point_y);
					final Point mp = this.frames.getFramePoint(p_pos, d);
					we.setPixelsLine(this.frames.getPixelsLine(mp.x, mp.y, p.x, p.y));
				}
				we.setFrameType(frame_type);
			}
			this.dispatchWaveformEvent(we);
		}
	}

	public final void sendUpdateEvent() {
		if(DEBUG.M) System.out.println("Waveform.sendUpdateEvent()");
		double curr_x, curr_y;
		WaveformEvent we;
		final Dimension d = this.getWaveSize();
		if(this.is_image && this.frames != null && this.frames.getNumFrame() != 0){
			final Point p = this.frames.getFramePoint(new Point(this.end_x, this.end_y), d);
			final int frame_type = this.frames.getFrameType();
			we = new WaveformEvent(this, WaveformEvent.POINT_UPDATE, p.x, p.y, this.frames.getFrameTime(), 0, this.frames.getPixel(this.frames.getFrameIdx(), p.x, p.y), 0);
			if(frame_type == FrameData.BITMAP_IMAGE_32 || frame_type == FrameData.BITMAP_IMAGE_16 || frame_type == FrameData.BITMAP_IMAGE_8) we.setPointValue(this.frames.getPointValue(this.frames.getFrameIdx(), p.x, p.y));
			we.setFrameType(frame_type);
			this.dispatchWaveformEvent(we);
		}else if(this.waveform_signal != null && this.wm != null){
			curr_x = this.wm.toXValue(this.end_x, d);
			curr_y = this.wm.toYValue(this.end_y, d);
			this.findPoint(curr_x, curr_y, this.first_set_point);
			this.first_set_point = false;
			curr_x = this.curr_point = this.wave_point_x;
			curr_y = this.curr_point_y = this.wave_point_y;
			double dx = 0, dy = 0;
			int event_id;
			dx = curr_x - Waveform.mark_point_x;
			dy = curr_y - Waveform.mark_point_y;
			event_id = this.show_measure ? WaveformEvent.MEASURE_UPDATE : WaveformEvent.POINT_UPDATE;
			we = new WaveformEvent(this, event_id, curr_x, curr_y, dx, dy, 0, this.getSelectedSignal());
			final Signal s = this.getSignal();
			we.setTimeValue(s.getXinYZplot());
			we.setXValue(s.getYinXZplot());
			we.setDataValue(s.getZValue());
			we.setIsMB2(this.is_mb2);
			if(Waveform.isHMS(s)) we.setDateValue(0);
			this.dispatchWaveformEvent(we);
		}
	}

	public final void setColorIdx(int idx) {
		if(DEBUG.M) System.out.println("Waveform.setColorIdx("+ idx +")");
		idx = idx % Waveform.colors.length;
		if(this.waveform_signal != null) this.waveform_signal.setColorIdx(idx);
		else if(this.frames != null){
			idx = (idx == 0 ? 1 : idx);
			this.frames.setColorIdx(idx);
			this.setCrosshairColor(idx);
		}
		this.setCrosshairColor(idx);
	}

	public void setColorProfile(final ColorProfile cp) {
		this.colorProfile = cp;
		if(this.frames != null) this.frames.setColorProfile(cp);
		this.not_drawn = true;
		this.repaint();
	}

	public final void setCopySelected(final boolean selec) {
		this.copy_selected = selec;
		this.not_drawn = true;
		this.repaint();
	}

	public final void setCrosshairColor(final Color crosshair_color) {
		this.crosshair_color = crosshair_color;
	}

	public final void setCrosshairColor(final int idx) {
		this.crosshair_color = Waveform.colors[idx % Waveform.colors.length];
	}

	public final void setEnabledDispatchEvents(final boolean event_enabled) {
		this.event_enabled = event_enabled;
	}

	public final void setEnabledPan(final boolean pan_enabled) {
		this.pan_enabled = pan_enabled;
	}

	public final void setFixedLimits() {
		if(DEBUG.M) System.out.println("Waveform.setFixedLimits()");
		try{
			this.waveform_signal.setXLimits(this.lx_min, this.lx_max, Signal.SIMPLE);
		}catch(final Exception exc){
			exc.printStackTrace();
		}
		this.setYlimits(this.ly_min, this.ly_max);
		this.change_limits = true;
	}

	public final void setFixedLimits(final double xmin, final double xmax, final float ymin, final float ymax) {
		if(DEBUG.M) System.out.println("Waveform.setFixedLimits(" + xmin + ", " + xmax + ", " + ymin + ", " + ymax + ")");
		this.lx_max = xmax;
		this.lx_min = xmin;
		this.ly_max = ymax;
		this.ly_min = ymin;
		this.setFixedLimits();
	}

	public final void setFrameBitShift(final int bitShift, final boolean bitClip) {
		if(this.frames == null) return;
		this.frames.setBitShift(bitShift, bitClip);
		this.not_drawn = true;
		this.repaint();
	}

	public final void setFrameRGB(final boolean useRGB) {
		if(this.frames == null) return;
		this.frames.setRGB(useRGB);
		this.not_drawn = true;
		this.repaint();
	}

	public final void setFrames(final Frames frames) {
		this.frames = frames;
	}

	public final void setGridMode(final int grid_mode, final boolean int_xlabel, final boolean int_ylabel) {
		if(DEBUG.M) System.out.println("Waveform.setGridMode(" + grid_mode + ", " + int_xlabel + ", " + int_ylabel + ")");
		this.grid_mode = grid_mode;
		this.int_xlabel = int_xlabel;
		this.int_ylabel = int_ylabel;
		this.wm = null;
		this.grid = null;
		this.not_drawn = true;
		// repaint();
	}

	public final void setGridSteps(final int _grid_step_x, final int _grid_step_y) {
		this.grid_step_x = _grid_step_x;
		if(this.grid_step_x <= 1) this.grid_step_x = 2;
		this.grid_step_y = _grid_step_y;
		if(this.grid_step_y <= 1) this.grid_step_y = 2;
		this.wm = null;
		this.grid = null;
		this.not_drawn = true;
	}

	public final void setInterpolate(final boolean interpolate) {
		if(this.waveform_signal != null) this.waveform_signal.setInterpolate(interpolate);
	}

	public final void setMarker(final Marker marker) {
		if(this.waveform_signal != null) this.waveform_signal.setMarker(marker);
	}

	public final void setMarkerStep(int step) {
		if(DEBUG.M) System.out.println("Waveform.setMarkerStep(" + step + ")");
		if(this.waveform_signal != null){
			if(step == 0 || step < 0) step = 1;
			this.waveform_signal.setMarkerStep(step);
		}
	}

	public final void setMarkerWidth(final int marker_width) {
		this.marker_width = marker_width;
	}

	public void setMode(final int mode) {
		if(DEBUG.M) System.out.println("Waveform.setMode(" + mode + ")");
		switch(mode){
			case MODE_PAN:
				if(this.pan_enabled){
					this.setCursor(new Cursor(Cursor.MOVE_CURSOR));
					this.mode_saved = this.mode = mode;
				}
				break;
			case MODE_COPY:
				this.setCursor(new Cursor(Cursor.HAND_CURSOR));
				this.mode_saved = this.mode = mode;
				break;
			case MODE_WAIT:
				this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				this.mode = mode;
				break;
			case MODE_POINT:
				this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
				this.mode_saved = this.mode = mode;
				break;
			case MODE_ZOOM:
				this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				this.mode_saved = this.mode = mode;
				break;
		}
		if(this.waveform_signal != null || this.is_image){
			if(mode == Waveform.MODE_POINT && this.is_image && this.frames != null){
				final Point p = this.frames.getFramePoint(this.getWaveSize());
				this.end_x = p.x;
				this.end_y = p.y;
			}
			this.repaint();
			if(mode == Waveform.MODE_POINT) if(this.is_image && this.send_profile) this.sendProfileEvent();
		}
	}

	public final void setPointMeasure() {
		if(DEBUG.M) System.out.println("Waveform.setPointMeasure()");
		final Dimension d = this.getWaveSize();
		if(this.is_image){
			Waveform.mark_point_x = this.end_x;
			Waveform.mark_point_y = this.end_y;
			this.frames.setMeasurePoint(this.end_x, this.end_y, d);
		}else{
			double curr_x = this.wm.toXValue(this.end_x, d);
			double curr_y = this.wm.toYValue(this.end_y, d);
			this.findPoint(curr_x, curr_y, d, true);
			Waveform.mark_point_x = curr_x = this.wave_point_x;
			Waveform.mark_point_y = curr_y = this.wave_point_y;
		}
	}

	public final void setProperties(final String properties) {
		this.properties = properties;
	}

	public final void setReversed(final boolean reversed) {
		if(this.profDialog != null) this.profDialog.setReversed(reversed);
		if((this.is_image && !reversed) || this.reversed == reversed) return;
		this.reversed = reversed;
		if(this.grid != null) this.grid.setReversed(reversed);
		this.update();
	}

	public final void setScale(final Waveform w) {
		if(DEBUG.M) System.out.println("Waveform.setScale(" + w + ")");
		if(this.waveform_signal == null) return;
		this.waveform_signal.setXLimits(w.waveform_signal.getXmin(), w.waveform_signal.getXmax(), Signal.SIMPLE);
		this.waveform_signal.setYmin(w.waveform_signal.getYmin(), Signal.SIMPLE);
		this.waveform_signal.setYmax(w.waveform_signal.getYmax(), Signal.SIMPLE);
		this.reportChanges();
	}

	public void setSelectBorder(final Border border) {
		this.select_border = border;
	}

	public final void setSendProfile(final boolean state) {
		this.send_profile = state;
	}

	public final void setShowMeasure(final boolean state) {
		this.show_measure = state;
	}

	public void setSignalMode1D(final Mode1D mode) {
		if(DEBUG.M) System.out.println("Waveform.setSignalMode1D(" + mode + ")");
		if(this.waveform_signal != null){
			this.waveform_signal.setMode1D(mode);
			this.not_drawn = true;
			this.repaint();
		}
	}

	public void setSignalMode2D(final Mode2D mode) {
		if(DEBUG.M) System.out.println("Waveform.setSignalMode2D(" + mode + ")");
		if(this.waveform_signal != null){
			this.waveform_signal.setMode2D(mode);
			if(this.waveform_signal.getType() == Signal.Type.IMAGE){
				this.autoscale();
				this.sendUpdateEvent();
			}
			this.not_drawn = true;
			this.repaint();
		}
	}

	public final void setSignalState(final boolean state) {
		if(DEBUG.M) System.out.println("Waveform.setSignalState(" + this.mode + ")");
		if(this.waveform_signal != null){
			this.waveform_signal.setInterpolate(state);
			this.waveform_signal.setMarker(Marker.NONE);
		}
	}

	public final void setTitle(final String title) {
		this.title = title;
	}

	public final void setXLabel(final String x_label) {
		this.x_label = x_label;
	}

	public void setXlimits(final double xmin, final double xmax) {
		if(DEBUG.M) System.out.println("Waveform.setXlimits(" + xmin + ", " + xmax + ")");
		if(this.waveform_signal != null) try{
			this.waveform_signal.setXLimits(xmin, xmax, Signal.SIMPLE);
		}catch(final Exception exc){
			exc.printStackTrace();
		}
	}

	public final void setXLog(final boolean x_log) {
		this.x_log = x_log;
	}

	public void setXScale(final Waveform w) {
		if(this.waveform_signal == null) return;
		this.waveform_signal.setXLimits(w.waveform_signal.getXmin(), w.waveform_signal.getXmax(), Signal.SIMPLE);
		this.reportChanges();
	}

	public void setXScaleAutoY(final Waveform w) {
		if(this.waveform_signal == null) return;
		this.waveform_signal.setXLimits(w.waveform_signal.getXmin(), w.waveform_signal.getXmax(), Signal.SIMPLE);
		this.waveform_signal.AutoscaleY();
		this.reportChanges();
	}

	public void setYLabel(final String y_label) {
		this.y_label = y_label;
	}

	public void setYlimits(final float ymin, final float ymax) {
		if(DEBUG.M) System.out.println("Waveform.setYlimits(" + ymin + ", " + ymax + ")");
		if(this.waveform_signal != null) this.waveform_signal.setYlimits(ymin, ymax);
	}

	public void setYLog(final boolean y_log) {
		this.y_log = y_log;
	}

	public void setYScale(final Waveform w) {
		if(this.waveform_signal == null) return;
		this.waveform_signal.setYmin(w.waveform_signal.getYmin(), Signal.SIMPLE);
		this.waveform_signal.setYmax(w.waveform_signal.getYmax(), Signal.SIMPLE);
		this.reportChanges();
	}

	public boolean showMeasure() {
		return this.show_measure;
	}

	public final void showProfileDialog() {
		if(this.profDialog != null) this.profDialog.dispose();
		this.profDialog = new ProfileDialog(this, this, this.reversed);
		this.profDialog.pack();
		this.profDialog.setSize(200, 250);
		this.setSendProfile(true);
		this.profDialog.setLocationRelativeTo(this);
		this.profDialog.setVisible(true);
		this.sendProfileEvent();
	}

	@Override
	public void signalUpdated(final int updatemode) {
		this.checkPending();
		if(updatemode >= SignalListener.UPDATE_REPAINT){
			final boolean created = this.minXSignal() == Double.POSITIVE_INFINITY || this.maxXSignal() == Double.NEGATIVE_INFINITY;
			if(created) this.update();
			else{
				this.change_limits |= updatemode >= SignalListener.UPDATE_LIMITS;
				this.not_drawn = true;
				this.repaint();
			}
		}
	}

	public final void stopFrame() {
		if(DEBUG.M) System.out.println("Waveform.StopFrame()");
		if(this.is_image && this.is_playing){
			this.is_playing = false;
			this.play_timer.stop();
		}
	}

	public final void undoZoom() {
		if(this.undo_zoom.size() == 0) return;
		this.reportLimits(this.undo_zoom.lastElement(), false);
		this.not_drawn = true;
		this.repaint();
	}

	public final boolean undoZoomPendig() {
		return this.undo_zoom.size() > 0;
	}

	public void update() {
		if(DEBUG.M) System.out.println("Waveform.Update()");
		if(this.waveform_signal != null) this.waveform_signal.setNumPoints(this.getWidth() * 2);
		this.update_();
	}

	public final void update(final float x[], final float y[]) {
		if(DEBUG.M) System.out.println("Waveform.Update(" + x + ", " + y + ")");
		this.wave_error = null;
		if(x.length <= 1 || y.length <= 1){
			this.wave_error = " Less than two points";
			this.update((Signal)null);
		}else this.update(new Signal(x, y));
	}

	public final synchronized void update(final Signal s) {
		if(DEBUG.M) System.out.println("Waveform.Update(" + s + ")");
		this.waveform_signal = s;
		this.waveform_signal.registerSignalListener(this);
		this.update_();
	}

	public final void updateImage(final Frames frames_in) {
		if(DEBUG.M) System.out.println("Waveform.UpdateImage(" + frames_in + ")");
		this.setFrames(frames_in);
		this.is_image = true;
		this.curr_rect = null;
		this.prev_point_x = this.prev_point_y = -1;
		this.not_drawn = true;
		this.repaint();
	}

	synchronized void updateCrosshair(final double curr_x, final double curr_y) {
		if(DEBUG.M) System.out.println("Waveform.UpdatePoint(" + curr_x + ", " + curr_y + ")");
		final Dimension d = this.getWaveSize();
		if(curr_x == this.curr_point && !this.dragging) return;
		this.curr_point = curr_x;
		this.curr_point_y = curr_y;
		if(!this.is_image){
			if(this.mode != Waveform.MODE_POINT || this.waveform_signal == null) return;
			if(this.waveform_signal.getType() == Signal.Type.IMAGE && this.waveform_signal.getMode2D() == Signal.Mode2D.YZ){
				this.waveform_signal.showYZ((float)curr_x);
				this.not_drawn = true;
			}
			this.paintImmediately(0, 0, d.width, d.height);
		}else if(this.frames != null && !this.is_playing){
			this.frame = this.frames.getFrameIdxAtTime((float)curr_x);
			this.not_drawn = true;
			this.repaint();
			if(this.send_profile) this.sendProfileEvent();
		}
	}

	protected final void addZoomRegion() {
		final ZoomRegion r_prev = new ZoomRegion(this.waveform_signal.getXmin(), this.waveform_signal.getXmax(), this.waveform_signal.getYmax(), this.waveform_signal.getYmin());
		this.undo_zoom.addElement(r_prev);
	}

	protected final void dispose() {
		if(this.profDialog != null) this.profDialog.dispose();
	}

	protected final boolean drawFrame(final Graphics g, final Dimension d, int frame_idx) {
		if(DEBUG.M) System.out.println("Waveform.DrawFrame(" + g + ", " + d + ", " + frame_idx + ")");
		this.wave_error = null;
		Object img;
		if(this.mode == Waveform.MODE_ZOOM && this.curr_rect != null){
			final Rectangle rect = new Rectangle(this.start_x, this.start_y, Math.abs(this.start_x - this.end_x), Math.abs(this.start_y - this.end_y));
			frame_idx = this.frames.getFrameIdx();
			this.frames.setZoomRegion(frame_idx, d, rect);
			this.curr_rect = null;
		}
		img = this.frames.getFrame(frame_idx, d);
		if(img == null){
			this.wave_error = " No frame at time " + this.curr_point;// frames.getTime(frame_idx);
			return false;
		}
		final Dimension dim = this.frames.getFrameSize(frame_idx, this.getWaveSize());
		// int type = frames.frame_type[frame_idx];
		final int type = this.frames.getFrameType();
		this.drawImage(g, img, dim, type);
		return true;
	}

	protected void drawImage(final Graphics g, final Object img, final Dimension dim, final int type) {
		if(DEBUG.M) System.out.println("Waveform.DrawImage(" + g + ", " + img + ", " + dim + ", " + type + ")");
		final Rectangle r = this.frames.getZoomRect();
		final Graphics2D g2 = (Graphics2D)g;
		final Dimension imgDim = new Dimension(((BufferedImage)img).getWidth(), ((BufferedImage)img).getHeight());
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if(type != FrameData.JAI_IMAGE){
			final int dx1;// the x coordinate of the first corner of the
			              // destination rectangle.
			final int dy1;// the y coordinate of the first corner of the
			              // destination rectangle.
			final int dx2;// the x coordinate of the second corner of the
			              // destination rectangle.
			final int dy2;// the y coordinate of the second corner of the
			              // destination rectangle.
			final int sx1;// the x coordinate of the first corner of the source
			              // rectangle.
			final int sy1;// the y coordinate of the first corner of the source
			              // rectangle.
			final int sx2;// the x coordinate of the second corner of the source
			              // rectangle.
			final int sy2;// the y coordinate of the second corner of the source
			              // rectangle.
			sx1 = r == null ? 0 : r.x;
			sy1 = r == null ? 0 : r.y;
			sx2 = r == null ? imgDim.width : r.x + r.width;
			sy2 = r == null ? imgDim.height : r.y + r.height;
			if(this.frames.getHorizontalFlip()){
				dx1 = dim.width;
				dx2 = 1;
			}else{
				dx1 = 1;
				dx2 = dim.width;
			}
			if(this.frames.getVerticalFlip()){
				dy1 = dim.height;
				dy2 = 1;
			}else{
				dy1 = 1;
				dy2 = dim.height;
			}
			g2.drawImage((Image)img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, this);
		}else{
			g2.clearRect(0, 0, dim.width, dim.height);
			g2.drawRenderedImage((RenderedImage)img, new AffineTransform(1f, 0f, 0f, 1f, 0F, 0F));
		}
	}

	protected final void drawMarkers(final Graphics g, final Point pnt[], final int n_pnt, final Marker marker, final int step, final Mode1D mode_in) {
		if(DEBUG.A) System.out.println("Waveform.drawMarkers(" + g + ", " + pnt + ", " + n_pnt + ", " + marker + ", " + step + ", " + mode_in + ")");
		for(int i = 0; i < n_pnt; i += step){
			if(mode_in == Mode1D.STEP && i % 2 == 1) continue;
			switch(marker){
				case CIRCLE:
					g.drawOval(pnt[i].x - this.marker_width / 2, pnt[i].y - this.marker_width / 2, this.marker_width, this.marker_width);
					break;
				case SQUARE:
					g.drawRect(pnt[i].x - this.marker_width / 2, pnt[i].y - this.marker_width / 2, this.marker_width, this.marker_width);
					break;
				case TRIANGLE:
					g.drawLine(pnt[i].x - this.marker_width / 2, pnt[i].y + this.marker_width / 2, pnt[i].x, pnt[i].y - this.marker_width / 2);
					g.drawLine(pnt[i].x + this.marker_width / 2, pnt[i].y + this.marker_width / 2, pnt[i].x, pnt[i].y - this.marker_width / 2);
					g.drawLine(pnt[i].x - this.marker_width / 2, pnt[i].y + this.marker_width / 2, pnt[i].x + this.marker_width / 2, pnt[i].y + this.marker_width / 2);
					break;
				case CROSS:
					g.drawLine(pnt[i].x, pnt[i].y - this.marker_width / 2, pnt[i].x, pnt[i].y + this.marker_width / 2);
					g.drawLine(pnt[i].x - this.marker_width / 2, pnt[i].y, pnt[i].x + this.marker_width / 2, pnt[i].y);
					break;
				case POINT:
					g.fillRect(pnt[i].x - 1, pnt[i].y - 1, 3, 3);
					break;
				case NONE:
					break;
			}
		}
	}

	protected void drawMarkers(final Graphics g, final Vector<Polygon> segments, final Marker marker, final int step, final Mode1D mode_in) {
		if(DEBUG.A) System.out.println("Waveform.drawMarkers(" + g + ", " + segments + ", " + marker + ", " + step + ", " + mode_in + ")");
		Polygon currPolygon;
		int pntX[];
		int pntY[];
		for(int k = 0; k < segments.size(); k++){
			currPolygon = segments.elementAt(k);
			pntX = currPolygon.xpoints;
			pntY = currPolygon.ypoints;
			for(int i = 0; i < currPolygon.npoints; i += step){
				if(mode_in == Mode1D.STEP && i % 2 == 1) continue;
				switch(marker){
					case CIRCLE:
						g.drawOval(pntX[i] - this.marker_width / 2, pntY[i] - this.marker_width / 2, this.marker_width, this.marker_width);
						break;
					case SQUARE:
						g.drawRect(pntX[i] - this.marker_width / 2, pntY[i] - this.marker_width / 2, this.marker_width, this.marker_width);
						break;
					case TRIANGLE:
						g.drawLine(pntX[i] - this.marker_width / 2, pntY[i] + this.marker_width / 2, pntX[i], pntY[i] - this.marker_width / 2);
						g.drawLine(pntX[i] + this.marker_width / 2, pntY[i] + this.marker_width / 2, pntX[i], pntY[i] - this.marker_width / 2);
						g.drawLine(pntX[i] - this.marker_width / 2, pntY[i] + this.marker_width / 2, pntX[i] + this.marker_width / 2, pntY[i] + this.marker_width / 2);
						break;
					case CROSS:
						g.drawLine(pntX[i], pntY[i] - this.marker_width / 2, pntX[i], pntY[i] + this.marker_width / 2);
						g.drawLine(pntX[i] - this.marker_width / 2, pntY[i], pntX[i] + this.marker_width / 2, pntY[i]);
						break;
					case POINT:
						g.fillRect(pntX[i] - 1, pntY[i] - 1, 3, 3);
						break;
					case NONE:
						break;
				}
			}
		}
	}

	protected final void drawMarkers(final Signal s, final Graphics g, final Dimension d) {
		if(DEBUG.D) System.out.println("Waveform.drawMarkers(" + s + ", " + g + ", " + d + ")");
		if(s.getMarker() == Marker.NONE) return;
		final Vector<Polygon> segments = this.wm.toPolygons(s, d, this.appendDrawMode);
		this.drawMarkers(g, segments, s.getMarker(), s.getMarkerStep(), s.getMode1D());
	}

	protected void drawSignal(final Graphics g) {
		this.drawSignal(g, this.getWaveSize(), Waveform.NO_PRINT);
	}

	protected void drawSignal(final Graphics g, final Dimension d, final int print_mode) {
		this.drawSignal(this.waveform_signal, g, d, print_mode);
	}

	protected void drawSignal(final Signal s, final Graphics g, final Dimension d, final int print_mode) {
		if(DEBUG.M) System.out.println("Waveform.drawSignal(" + s + ", " + g + ", " + d + ")");
		if((print_mode & Waveform.PRINT_BW) != Waveform.PRINT_BW){
			if(s.getColor() != null) g.setColor(s.getColor());
			else g.setColor(Waveform.colors[s.getColorIdx() % Waveform.colors.length]);
		} else g.setColor(Color.black);
		final boolean DRAGGING = this.mode == Waveform.MODE_PAN && this.dragging;
		if(DRAGGING && s.getNumPoints() > Waveform.MAX_DRAG_POINT) {
			int drag_point = Waveform.MAX_DRAG_POINT;
			int x[] = new int[s.getNumPoints()];
			int y[] = new int[s.getNumPoints()];
			Point curr_points[] = new Point[s.getNumPoints()];
			float step = (float)s.getNumPoints() / drag_point;
			int num_steps = drag_point;
			for(int j = 0; j < num_steps; j++){
				x[j] = this.wm.toXPixel(s.getX((int)(step * j)), d);
				y[j] = this.wm.toYPixel(s.getY((int)(step * j)), d);
				curr_points[j] = new Point(x[j], y[j]);
			}
			for(int jj = 0; jj < num_steps - 1; jj++)
				if(!Double.isNaN(s.getY((int)(step * jj))) && !Double.isNaN(s.getY((int)(step * (jj + 1))))) g.drawLine(x[jj], y[jj], x[jj + 1], y[jj + 1]);
		} else if(s.getType() == Signal.Type.IMAGE && (s.getMode2D() == Signal.Mode2D.IMAGE || s.getMode2D() == Signal.Mode2D.CONTOUR)){
			if(!DRAGGING) {
				switch(s.getMode2D()){
					case IMAGE:
						final Image img = this.createImage(d.width, d.height);
						this.wm.toImage(s, img, d, this.colorProfile.colorMap);
						g.drawImage(img, 0, 0, d.width, d.height, this);
						break;
					case CONTOUR:
						this.drawSignalContour(s, g, d);
						break;
					default:
						break;
				}
			}
		}else{
			if (DRAGGING || s.getInterpolate()) {
				final Vector<Polygon> segments = this.wm.toPolygons(s, d, this.appendDrawMode);
				if(segments != null) {
					for (final Polygon seg : segments) {
						g.drawPolyline(seg.xpoints, seg.ypoints, seg.npoints);
					}
				}
			}
		}
		if(DRAGGING) return;
		this.drawWave(s, g, d);
		this.drawMarkers(s, g, d);
		this.drawError(s, g, d);
	}

	protected final void drawSignalContour(final Signal s, final Graphics g, final Dimension d) {
		if(DEBUG.M) System.out.println("Waveform.drawSignalContour(" + s + ", " + g + ", " + d + ")");
		final Vector<Vector<Vector<Point2D.Double>>> cs = s.getContourSignals();
		final Vector<Float> ls = s.getContourLevelValues();
		final int numLevel = cs.size();
		Vector<Vector<Point2D.Double>> cOnLevel;
		float level;
		for(int l = 0; l < numLevel; l++){
			cOnLevel = cs.elementAt(l);
			level = ls.elementAt(l).floatValue();
			final float z[] = s.getZ();
			float zMin, zMax;
			zMin = zMax = z[0];
			for(final float element : z){
				if(element < zMin) zMin = element;
				if(element > zMax) zMax = element;
			}
			g.setColor(this.colorProfile.colorMap.getColor(level, zMin, zMax));
			this.drawContourLevel(cOnLevel, g, d);
		}
	}

	protected Point findPoint(final double curr_x, final double curr_y, final boolean is_first) {
		if(!this.findPoint(this.waveform_signal, curr_x, curr_y)) return null;
		final Dimension d = this.getWaveSize();
		return new Point(this.wm.toXPixel(this.wave_point_x, d), this.wm.toYPixel(this.wave_point_y, d));
	}

	protected Point findPoint(final double curr_x, final double curr_y, Dimension d, final boolean is_first) {
		if(!this.findPoint(this.waveform_signal, curr_x, curr_y)) return null;
		if(this.waveform_signal.getType() == Type.IMAGE && !(this.waveform_signal.getMode2D() == Mode2D.PROFILE)) d = this.getWaveSize();
		return new Point(this.wm.toXPixel(this.wave_point_x, d), this.wm.toYPixel(this.wave_point_y, d));
	}

	protected final boolean findPoint(final Signal s, final double curr_x, final double curr_y) {
		if(DEBUG.M) System.out.println("Waveform.FindPoint(" + s + ", " + curr_x + ", " + curr_y + ")");
		this.wave_point_x = this.wave_point_y = Double.NaN;
		if(s == null) return false;
		if(s.getType() == Type.IMAGE) switch(s.getMode2D()){
			case IMAGE:
				this.wave_point_x = s.getClosestX(curr_x);
				this.wave_point_y = s.getClosestY(curr_y);
				return true;
			case CONTOUR:
				this.wave_point_x = curr_x;
				this.wave_point_y = curr_y;
				s.surfaceValue(this.wave_point_x, this.wave_point_y);
				return true;
			case PROFILE:
				System.out.println("MODE_PROFILE");
				break;
			default:
				break;
		}
		if(!s.hasX()) return false;
		final int idx = s.FindClosestIdx(curr_x, curr_y);
		if(curr_x > s.getCurrentXmax() || curr_x < s.getCurrentXmin() || idx == s.getNumPoints() - 1){
			this.wave_point_x = s.getX(idx);
			this.wave_point_y = s.getY(idx);
		}else if(s.getMarker() != Marker.NONE && !s.getInterpolate() && s.getMode1D() != Mode1D.STEP || s.findNaN()){
			double val;
			final boolean increase = s.getX(idx) < s.getX(idx + 1);
			if(increase) val = s.getX(idx) + s.getX(idx + 1) - s.getX(idx) / 2;
			else val = s.getX(idx + 1) + s.getX(idx) - s.getX(idx + 1) / 2;
			// Patch to elaborate strange RFX signal (roprand bar error
			// signal)
			if(s.getX(idx) == s.getX(idx + 1) && !Double.isNaN(s.getY(idx + 1))) val += curr_x;
			if(curr_x < val){
				this.wave_point_y = s.getY(idx + (increase ? 0 : 1));
				this.wave_point_x = s.getX(idx + (increase ? 0 : 1));
			}else{
				this.wave_point_y = s.getY(idx + (increase ? 1 : 0));
				this.wave_point_x = s.getX(idx + (increase ? 1 : 0));
			}
		}else{
			this.wave_point_x = curr_x;
			try{
				if(s.getMode1D() == Mode1D.STEP) this.wave_point_y = s.getY(idx);
				else this.wave_point_y = s.getY(idx) + (s.getY(idx + 1) - s.getY(idx)) * (this.wave_point_x - s.getX(idx)) / (s.getX(idx + 1) - s.getX(idx));
			}catch(final ArrayIndexOutOfBoundsException e){
				this.wave_point_x = this.wave_point_y = Double.NaN;
				return false;
			}
		}
		return true;
	}

	protected final Point findPoint(final Signal signal, final double curr_x, final double curr_y, Dimension d) {
		if(!this.findPoint(signal, curr_x, curr_y)) return null;
		if(signal.getType() == Signal.Type.IMAGE && !(signal.getMode2D() == Mode2D.PROFILE)) d = this.getWaveSize();
		return new Point(this.wm.toXPixel(this.wave_point_x, d), this.wm.toYPixel(this.wave_point_y, d));
	}

	@SuppressWarnings("static-method")
	protected int getBottomSize() {
		return 0;
	}

	@SuppressWarnings("static-method")
	protected final Dimension getLegendDimension(final Graphics g) {
		return new Dimension(0, 0);
	}

	protected final Dimension getPrintWaveSize(final Dimension dim) {
		if(DEBUG.M) System.out.println("Waveform.getPrintWaveSize(" + dim + ")");
		return new Dimension(dim.width - this.getRightSize(), dim.height - this.getBottomSize());
	}

	@SuppressWarnings("static-method")
	protected int getRightSize() {
		return 0;
	}

	@SuppressWarnings("static-method")
	protected int getSelectedSignal() {
		return 0;
	}

	protected final Dimension getWaveSize() {
		if(DEBUG.G) System.out.println("Waveform.getWaveSize()");
		final Dimension dim = this.getSize();
		final Insets i = this.getInsets();
		return new Dimension(dim.width - this.getRightSize() - i.top - i.bottom, dim.height - this.getBottomSize() - i.right - i.left);
	}

	protected void handleCopy() {
		/* stub */}

	protected void handlePaste() {
		/* stub */}

	protected boolean hasPending() {
		return this.waveform_signal.hasPending();
	}

	protected final double maxXSignal() {
		if(this.waveform_signal == null) return 1.;
		return this.waveform_signal.getXmax();
	}

	protected final double maxYSignal() {
		if(this.waveform_signal == null) return 1.;
		return this.waveform_signal.getYmax();
	}

	protected final double minXSignal() {
		if(this.waveform_signal == null) return -1.;
		return this.waveform_signal.getXmin();
	}

	protected final double minYSignal() {
		if(this.waveform_signal == null) return -1.;
		return this.waveform_signal.getYmin();
	}

	protected void notifyZoom(final double start_xs, final double end_xs, final double start_ys, final double end_ys, final int timestamp) {
		if(DEBUG.M) System.out.println("Waveform.NotifyZoom(" + start_xs + ", " + end_xs + ", " + start_ys + ", " + end_ys + ", " + timestamp + ")");
	}

	protected synchronized void paintSignal(final Graphics g, final Dimension dim, final int print_mode) {
		if(DEBUG.M) System.out.println("Waveform.PaintSignal(" + g + ", " + dim + ", " + print_mode + ")");
		Dimension d;
		String orizLabel = this.x_label;
		String vertLabel = this.y_label;
		String sigTitle = this.title;
		if(print_mode == Waveform.NO_PRINT) d = this.getWaveSize();
		else d = this.getPrintWaveSize(dim);
		if(this.mode != Waveform.MODE_PAN || this.dragging == false) if(this.waveform_signal != null){// TACON MOSTRUOSO per gestire il
			// fatto che jScope vede solo
			// gli offsets nei times!!!!
			this.xmax = this.maxXSignal();
			this.xmin = this.minXSignal();
			this.ymax = this.maxYSignal();
			this.ymin = this.minYSignal();
			if(Double.isFinite(this.xmax) && Double.isFinite(this.xmin)){
				final double xrange = this.xmax - this.xmin;
				this.xmax += xrange * Waveform.horizontal_offset / 200.;
				this.xmin -= xrange * Waveform.horizontal_offset / 200.;
			}
			final double yrange = this.ymax - this.ymin;
			this.ymax += yrange * Waveform.vertical_offset / 200.;
			this.ymin -= yrange * Waveform.vertical_offset / 200.;
			if(this.waveform_signal.getType() == Signal.Type.IMAGE){
				switch(this.waveform_signal.getMode2D()){
					case IMAGE:
					case XZ:
						orizLabel = (this.x_label == null) ? this.waveform_signal.getXlabel() : this.x_label;
						break;
					case YZ:
						orizLabel = (this.y_label == null) ? this.waveform_signal.getYlabel() : this.y_label;
						break;
					default:
						break;
				}
				vertLabel = this.waveform_signal.getZlabel();
			}else{
				orizLabel = (this.x_label == null) ? this.waveform_signal.getXlabel() : this.x_label;
				vertLabel = (this.y_label == null) ? this.waveform_signal.getYlabel() : this.y_label;
			}
			sigTitle = (this.title == null || this.title.length() == 0) ? this.waveform_signal.getTitlelabel() : this.title;
		}
		if(this.resizing || this.grid == null || this.wm == null || this.change_limits){
			this.change_limits = false;
			this.grid = new Grid(this.xmax, this.ymax, this.xmin, this.ymin, this.x_log, this.y_log, this.grid_mode, orizLabel, vertLabel, sigTitle, this.wave_error, this.grid_step_x, this.grid_step_y, this.int_xlabel, this.int_ylabel, this.reversed);
			this.curr_display_limits = new Rectangle();
			this.grid.getLimits(g, this.curr_display_limits, this.y_log);
			this.wm = new WaveformMetrics(this.xmax, this.xmin, this.ymax, this.ymin, this.curr_display_limits, d, this.x_log, this.y_log, 0, 0);
		}else{
			this.grid.updateValues(orizLabel, vertLabel, sigTitle, this.wave_error, this.grid_step_x, this.grid_step_y, this.int_xlabel, this.int_ylabel, this.reversed);
			this.grid.setLabels(sigTitle, orizLabel, vertLabel);
		}
		this.grid.setXaxisHMS(Waveform.isHMS(this.waveform_signal));
		if(!this.copy_selected || print_mode != Waveform.NO_PRINT){
			if(this.reversed && print_mode == Waveform.NO_PRINT) g.setColor(Color.black);
			else g.setColor(Color.white);
		}else g.setColor(Color.lightGray);
		g.fillRect(1, 1, d.width - 2, d.height - 2);
		if(this.waveform_signal != null){
			this.wave_b_box = new Rectangle(this.wm.toXPixel(this.minXSignal(), d), this.wm.toYPixel(this.maxYSignal(), d), this.wm.toXPixel(this.maxXSignal(), d) - this.wm.toXPixel(this.minXSignal(), d) + 1, this.wm.toYPixel(this.minYSignal(), d) - this.wm.toYPixel(this.maxYSignal(), d) + 1);
			if(print_mode == Waveform.NO_PRINT) g.clipRect(this.curr_display_limits.width, 0, d.width - this.curr_display_limits.width, d.height - this.curr_display_limits.height);
			this.drawSignal(g, d, print_mode);
		}
		if(print_mode == Waveform.PRINT && this.mode == Waveform.MODE_POINT){
			/* Needed to print cross hair */
			final double curr_x = this.wm.toXValue(this.end_x, d);
			final double curr_y = this.wm.toYValue(this.end_y, d);
			final Point p = this.findPoint(curr_x, curr_y, d, true);
			if(p != null){
				final Color prev_color = g.getColor();
				if(this.crosshair_color != null) g.setColor(this.crosshair_color);
				g.drawLine(0, p.y, d.width, p.y);
				g.drawLine(p.x, 0, p.x, d.height);
				g.setColor(prev_color);
			}
		}
		if(!this.is_min_size && this.grid != null) this.grid.paint(g, d, this, this.wm);
	}

	protected void reportLimits(final ZoomRegion r, final boolean add_undo) {
		if(DEBUG.M) System.out.println("Waveform.ReportLimits(" + r + ", " + add_undo + ")");
		if(add_undo) this.addZoomRegion();
		else{
			this.undo_zoom.removeElement(r);
			if(this.undo_zoom.size() == 0) this.waveform_signal.unfreeze();
		}
		this.setXlimits((float)r.start_xs, (float)r.end_xs);
		this.waveform_signal.setXLimits(r.start_xs, r.end_xs, Signal.SIMPLE);
		this.waveform_signal.setYmin(r.end_ys, Signal.SIMPLE);
		this.waveform_signal.setYmax(r.start_ys, Signal.SIMPLE);
		this.change_limits = true;
		if(this.undo_zoom.size() > 0) this.waveform_signal.freeze();
	}

	protected final void resetMode() {
		this.setMode(this.mode_saved);
	}

	protected final void resize(final int x, final int y, final boolean enlarge) {
		if(DEBUG.M) System.out.println("Waveform.Resize(" + x + ", " + y + ", " + enlarge + ")");
		final Dimension d = this.getWaveSize();
		final double curr_x = this.wm.toXValue(x, d), curr_y = this.wm.toYValue(y, d);
		double new_xmax, new_xmin, new_ymax, new_ymin;
		final double factor = enlarge ? .5 : 2.;
		new_xmin = curr_x - (curr_x - this.wm.xmin) * factor;
		new_xmax = curr_x + (this.wm.xmax - curr_x) * factor;
		new_ymin = curr_y - (curr_y - this.wm.ymin) * factor;
		new_ymax = curr_y + (this.wm.ymax - curr_y) * factor;
		this.reportLimits(new ZoomRegion(new_xmin, new_xmax, new_ymax, new_ymin), true);
		this.not_drawn = true;
	}

	protected final void setKeys() {
		if(DEBUG.M) System.out.println("Waveform.setKeys()");
		this.addKeyListener(new KeyAdapter(){
			@Override
			public final void keyPressed(final KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_PAGE_DOWN || e.getKeyCode() == KeyEvent.VK_PAGE_UP){
					if(e.getKeyCode() == KeyEvent.VK_PAGE_DOWN){
						if(Waveform.this.is_image){
							if(Waveform.this.frames != null && Waveform.this.frames.getFrameIdx() > 0){
								Waveform.this.frame = Waveform.this.frames.getLastFrameIdx();
								Waveform.this.not_drawn = false;
							}
						}else{
							final Signal s = Waveform.this.getSignal();
							if(s.getType() == Signal.Type.IMAGE){
								s.decShow();
								Waveform.this.not_drawn = true;
							}
						}
					}else if(Waveform.this.is_image){
						if(Waveform.this.frames != null){
							Waveform.this.frame = Waveform.this.frames.getNextFrameIdx();
							Waveform.this.not_drawn = false;
						}
					}else{
						final Signal s = Waveform.this.getSignal();
						if(s.getType() == Signal.Type.IMAGE){
							s.incShow();
							Waveform.this.not_drawn = true;
						}
					}
					// ReportChanges();
					Waveform.this.repaint();
					Waveform.this.sendUpdateEvent();
				}
			}

			@Override
			public final void keyReleased(final KeyEvent e) {
				/* stub */}

			@Override
			public final void keyTyped(final KeyEvent e) {
				/* stub */}
		});
	}

	protected void setMouse() {
		if(DEBUG.M) System.out.println("Waveform.setMouse()");
		this.addMouseListener(new MouseAdapter(){
			@Override
			public final void mouseClicked(final MouseEvent e) {
				if((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) Waveform.this.getParent().dispatchEvent(e);
			}

			@Override
			public final void mousePressed(final MouseEvent e) {
				if(Waveform.this.getCursor().getType() == Cursor.WAIT_CURSOR) return;
				final Insets i = Waveform.this.getInsets();
				Waveform.this.just_deselected = false;
				Waveform.this.requestFocus();
				Waveform.this.is_mb2 = Waveform.this.is_mb3 = false;
				if((e.getModifiers() & Event.ALT_MASK) != 0) Waveform.this.is_mb2 = true;
				else if((e.getModifiers() & Event.META_MASK) != 0) Waveform.this.is_mb3 = true;
				if(Waveform.this.mode == Waveform.MODE_COPY && !Waveform.this.is_mb3){
					if(Waveform.this.is_mb2){
						if(!Waveform.this.isCopySelected()) Waveform.this.sendPasteEvent();
					}else Waveform.this.sendCutEvent();
					return;
				}
				final int x = e.getX() - i.right;
				final int y = e.getY() - i.top;
				if(Waveform.this.mode != Waveform.MODE_POINT) Waveform.this.update_timestamp++;
				Waveform.this.start_x = Waveform.this.end_x = Waveform.this.prev_point_x = Waveform.this.orig_x = x;
				Waveform.this.start_y = Waveform.this.end_y = Waveform.this.prev_point_y = Waveform.this.orig_y = y;
				Waveform.this.dragging = true;
				Waveform.this.first_set_point = true;
				Waveform.this.setMousePoint();
				if(Waveform.this.mode == Waveform.MODE_PAN && Waveform.this.waveform_signal != null) Waveform.this.waveform_signal.StartTranslate();
				if(Waveform.this.mode == Waveform.MODE_POINT && Waveform.this.waveform_signal != null) Waveform.this.repaint();
				if((e.getModifiers() & Event.CTRL_MASK) != 0) if(Waveform.this.is_image){
					if(Waveform.this.frames != null && Waveform.this.frames.getFrameIdx() > 0) Waveform.this.frame = Waveform.this.frames.getLastFrameIdx();
				}else{
					final Signal s = Waveform.this.getSignal();
					if(s.getType() == Signal.Type.IMAGE){
						s.decShow();
						Waveform.this.not_drawn = true;
						Waveform.this.repaint();
					}
				}
				if((e.getModifiers() & Event.SHIFT_MASK) != 0) if(Waveform.this.is_image){
					if(Waveform.this.frames != null) Waveform.this.frame = Waveform.this.frames.getNextFrameIdx();
				}else{
					final Signal s = Waveform.this.getSignal();
					if(s.getType() == Signal.Type.IMAGE){
						s.incShow();
						Waveform.this.not_drawn = true;
						Waveform.this.repaint();
					}
				}
				if(Waveform.this.mode == Waveform.MODE_POINT){
					if(Waveform.this.is_image && Waveform.this.frames != null){
						// if(!frames.contain(new Point(start_x, start_y), d))
						// return;
						Waveform.this.not_drawn = false;
						Waveform.this.repaint();
					}else{
						final Signal s = Waveform.this.getSignal();
						if(Waveform.this.is_mb2 && s.getType() == Signal.Type.IMAGE && s.getMode2D() == Signal.Mode2D.CONTOUR){
							s.addContourLevel(s.getZValue());
							Waveform.this.not_drawn = true;
							Waveform.this.repaint();
						}
					}
					Waveform.this.sendUpdateEvent();
				}else{
					Waveform.this.end_x = Waveform.this.end_y = 0;
					Waveform.this.show_measure = false;
				}
			}

			@Override
			public final void mouseReleased(final MouseEvent e) {
				if(Waveform.this.getCursor().getType() == Cursor.WAIT_CURSOR) return;
				final Insets i = Waveform.this.getInsets();
				final Dimension d = Waveform.this.getWaveSize();
				Waveform.this.dragging = false;
				final int x = e.getX() - i.right;
				final int y = e.getY() - i.top;
				if(Waveform.this.mode == Waveform.MODE_POINT && Waveform.this.is_image && Waveform.this.frames != null)
				    /* Save current point position */
				    Waveform.this.frames.setFramePoint(new Point(Waveform.this.end_x, Waveform.this.end_y), d);
				if(Waveform.this.is_mb3) // e.isPopupTrigger()) //Se e' MB3
				    return; // Waveform.this.getParent().dispatchEvent(e);
				if((Waveform.this.waveform_signal == null && !Waveform.this.is_image) || (Waveform.this.frames == null && Waveform.this.is_image)) return;
				if(Waveform.this.mode == Waveform.MODE_ZOOM && x != Waveform.this.orig_x && y != Waveform.this.orig_y){
					if(!Waveform.this.is_image) Waveform.this.performZoom();
					Waveform.this.not_drawn = true;
				}
				if(Waveform.this.mode == Waveform.MODE_ZOOM && Waveform.zoom_on_mb1 && x == Waveform.this.orig_x && y == Waveform.this.orig_y && !Waveform.this.is_image) if((e.getModifiers() & Event.ALT_MASK) != 0) Waveform.this.resize(x, y, false);
				else Waveform.this.resize(x, y, true);
				if(Waveform.this.mode == Waveform.MODE_PAN && !Waveform.this.is_image){
					Waveform.this.notifyZoom(Waveform.this.minXSignal(), Waveform.this.maxXSignal(), Waveform.this.minYSignal(), Waveform.this.maxYSignal(), Waveform.this.update_timestamp);
					Waveform.this.grid = null;
					Waveform.this.not_drawn = true;
					// July 2014 in order to force resolution adjustment
					try{
						Waveform.this.waveform_signal.setXLimits(Waveform.this.minXSignal(), Waveform.this.maxXSignal(), Signal.SIMPLE);
						Waveform.this.setXlimits((float)Waveform.this.minXSignal(), (float)Waveform.this.maxXSignal());
					}catch(final Exception exc){
						exc.printStackTrace();
					}
				}
				Waveform.this.prev_point_x = Waveform.this.prev_point_y = -1;
				if(!Waveform.this.is_image) Waveform.this.curr_rect = null;
				Waveform.this.dragging = false;
				Waveform.this.repaint();
				if(Waveform.this.is_image && Waveform.this.restart_play){
					Waveform.this.play_timer.start();
					Waveform.this.restart_play = false;
				}
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseDragged(final MouseEvent e) {
				if(Waveform.this.getCursor().getType() == Cursor.WAIT_CURSOR) return;
				if(Waveform.this.waveform_signal == null && !Waveform.this.is_image || Waveform.this.is_mb2) return;
				// if(is_image && is_playing) return;
				final Insets i = Waveform.this.getInsets();
				final Dimension d = Waveform.this.getWaveSize();
				final int x = e.getX() - i.right;
				final int y = e.getY() - i.top;
				if((e.getModifiers() & Event.META_MASK) != 0 || Waveform.this.is_mb3 || e.isPopupTrigger()) // Se e'
				                                                                                            // MB3
				    return;
				if(Waveform.this.mode == Waveform.MODE_ZOOM && x < Waveform.this.orig_x){
					Waveform.this.start_x = x;
					Waveform.this.end_x = Waveform.this.orig_x;
				}else{
					Waveform.this.end_x = x;
					Waveform.this.start_x = Waveform.this.orig_x;
				}
				if(Waveform.this.mode == Waveform.MODE_ZOOM && y < Waveform.this.orig_y){
					Waveform.this.end_y = Waveform.this.orig_y;
					Waveform.this.start_y = y;
				}else{
					Waveform.this.end_y = y;
					Waveform.this.start_y = Waveform.this.orig_y;
				}
				if(Waveform.this.is_image && Waveform.this.frames != null){
					/* Check if point is out of the image */
					if(Waveform.this.mode == Waveform.MODE_ZOOM && !Waveform.this.frames.contain(new Point(Waveform.this.start_x, Waveform.this.start_y), d)) return;
					Waveform.this.setMousePoint();
				}
				if(Waveform.this.mode == Waveform.MODE_ZOOM){
					if(Waveform.this.is_image && Waveform.this.is_playing){
						Waveform.this.restart_play = true;
						Waveform.this.play_timer.stop();
					}
					if(Waveform.this.curr_rect == null) Waveform.this.curr_rect = new Rectangle(Waveform.this.start_x, Waveform.this.start_y, Waveform.this.end_x - Waveform.this.start_x, Waveform.this.end_y - Waveform.this.start_y);
					else Waveform.this.curr_rect.setBounds(Waveform.this.start_x, Waveform.this.start_y, Waveform.this.end_x - Waveform.this.start_x, Waveform.this.end_y - Waveform.this.start_y);
					Waveform.this.not_drawn = false;
					Waveform.this.repaint();
				}else Waveform.this.curr_rect = null;
				if(Waveform.this.mode == Waveform.MODE_POINT){
					Waveform.this.not_drawn = false;
					Waveform.this.sendUpdateEvent();
					Waveform.this.paintImmediately(0, 0, d.width, d.height);
					if(Waveform.this.is_image && Waveform.this.send_profile) Waveform.this.sendProfileEvent();
				}
				if(Waveform.this.mode == Waveform.MODE_PAN && !Waveform.this.is_image){
					if(Waveform.this.wm.x_log) Waveform.this.pan_delta_x = Waveform.this.wm.toXValue(Waveform.this.start_x, d) / Waveform.this.wm.toXValue(Waveform.this.end_x, d);
					else Waveform.this.pan_delta_x = Waveform.this.wm.toXValue(Waveform.this.start_x, d) - Waveform.this.wm.toXValue(Waveform.this.end_x, d);
					if(Waveform.this.wm.y_log) Waveform.this.pan_delta_y = Waveform.this.wm.toYValue(Waveform.this.start_y, d) / Waveform.this.wm.toYValue(Waveform.this.end_y, d);
					else Waveform.this.pan_delta_y = Waveform.this.wm.toYValue(Waveform.this.start_y, d) - Waveform.this.wm.toYValue(Waveform.this.end_y, d);
					Waveform.this.not_drawn = false;
					Waveform.this.repaint();
				}
			}
		});
	}

	protected final float toXValue(final int x) {
		if(DEBUG.M) System.out.println("Waveform.convertX(" + x + ")");
		final Dimension d = this.getWaveSize();
		return (float)this.wm.toXValue(x, d);
	}

	protected final float toYValue(final int y) {
		if(DEBUG.M) System.out.println("Waveform.convertY(" + y + ")");
		final Dimension d = this.getWaveSize();
		return (float)this.wm.toYValue(y, d);
	}

	private final void dispatchWaveformEvent_(final WaveformEvent e) {
		final WaveformListener[] list;
		synchronized(Waveform.this.waveform_listener){
			list = new WaveformListener[this.waveform_listener.size()];
			this.waveform_listener.copyInto(list);
		}
		for(final WaveformListener wfl : list)
			wfl.processWaveformEvent(e);
	}

	private final void imageActions(final Graphics g, final Dimension d) {
		if(DEBUG.M) System.out.println("Waveform.ImageActions(" + g + ", " + d + ")");
		if(this.frames != null && this.frames.getNumFrame() != 0 && (this.mode == Waveform.MODE_POINT || this.mode == Waveform.MODE_ZOOM) && !this.not_drawn && !this.is_min_size || (this.frames != null && this.send_profile && this.prev_frame != this.frame)) if(this.mode == Waveform.MODE_POINT){
			final Color prev_color = g.getColor();
			if(this.crosshair_color != null) g.setColor(this.crosshair_color);
			g.drawLine(0, this.end_y, d.width, this.end_y);
			g.drawLine(this.end_x, 0, this.end_x, d.height);
			if(this.show_measure){
				g.setColor(Color.green);
				final Point mp = this.frames.getMeasurePoint(d);
				g.drawLine(mp.x, mp.y, this.end_x, this.end_y);
			}
			g.setColor(prev_color);
		}
	}

	private final void setMousePoint() {
		if(DEBUG.M) System.out.println("Waveform.setMousePoint()");
		if(this.is_image && this.frames != null){
			final Dimension d = this.getWaveSize();
			Point p_pos;
			p_pos = new Point(this.end_x, this.end_y);
			this.frames.getFramePoint(p_pos, d);
			this.end_x = p_pos.x;
			this.end_y = p_pos.y;
		}
	}

	private void signalActions(final Graphics g, final Dimension d) {
		if(DEBUG.M) System.out.println("Waveform.SignalActions(" + g + ", " + d + ")");
		double curr_x, curr_y;
		if(this.waveform_signal != null && this.mode == Waveform.MODE_POINT && !this.not_drawn && !this.is_min_size && this.wm != null){
			curr_x = this.curr_point;
			curr_y = this.wm.toYValue(this.end_y, d);
			final Point p = this.findPoint(curr_x, curr_y, this.first_set_point);
			this.first_set_point = false;
			if(p != null){
				if(this.curr_point_y != Double.NaN) p.y = this.wm.toYPixel(this.curr_point_y, d);
				curr_x = this.wave_point_x;
				curr_y = this.wave_point_y;
				final Color prev_color = g.getColor();
				if(this.crosshair_color != null) g.setColor(this.crosshair_color);
				g.drawLine(0, p.y, d.width, p.y);
				g.drawLine(p.x, 0, p.x, d.height);
				g.setColor(prev_color);
				this.prev_point_x = p.x;
				this.prev_point_y = p.y;
			}
		}
		if(this.mode == Waveform.MODE_PAN && this.dragging && this.waveform_signal != null){
			this.waveform_signal.Traslate(this.pan_delta_x, this.pan_delta_y, this.wm.x_log, this.wm.y_log);
			this.wm = new WaveformMetrics(this.maxXSignal(), this.minXSignal(), this.maxYSignal(), this.minYSignal(), this.curr_display_limits, d, this.wm.x_log, this.wm.y_log, 0, 0);
			g.setColor(this.reversed ? Color.black : Color.white);
			g.fillRect(1, 1, d.width - 2, d.height - 2);
			g.setColor(Color.black);
			this.drawSignal(g);
		}
	}

	private final void update_() {
		if(DEBUG.M) System.out.println("Waveform.Update()");
		this.change_limits = true;
		this.wm = null;
		this.curr_rect = null;
		this.prev_point_x = this.prev_point_y = -1;
		this.not_drawn = true;
		this.repaint();
	}
}
