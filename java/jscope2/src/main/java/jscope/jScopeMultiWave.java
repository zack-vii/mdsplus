package jscope;

/* $Id$ */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import debug.DEBUG;
import jscope.data.DataProvider;
import jscope.data.UpdateEventListener;
import jscope.data.framedata.FrameData;
import jscope.data.signal.Signal;
import jscope.data.signal.Signal.Marker;
import jscope.dialog.ColorMap.ColorProfile;
import jscope.waveform.MultiWaveform;
import jscope.waveform.WaveInterface;
import jscope.waveform.Waveform;
import jscope.waveform.WaveformEvent;

/**
 * Class MultiWaveform extends the capability of class Waveform to deal with multiple
 * waveforms.
 */
final public class jScopeMultiWave extends MultiWaveform implements UpdateEventListener{
	private static final long serialVersionUID = 1L;
	// Inner class ToTransferHandler to receive jTraverser info
	class ToTransferHandler extends TransferHandler{
		private static final long serialVersionUID = 1L;

		@Override
		public boolean canImport(final TransferHandler.TransferSupport support) {
			if(!support.isDrop()) return false;
			if(!support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
			if((support.getSourceDropActions() & TransferHandler.COPY_OR_MOVE) == 0) return false;
			return true;
		}

		@Override
		public boolean importData(final TransferHandler.TransferSupport support) {
			if(!this.canImport(support)) return false;
			final Pattern pattern = Pattern.compile("(([a-zA-Z]+)#|)(\\\\([^:]+)::((TOP)[.:]|)|)(.*)");
			try{
				final String input = ((String)support.getTransferable().getTransferData(DataFlavor.stringFlavor));
				final Matcher match = pattern.matcher(input);
				if(match.find()){
					final boolean isimage = match.group(1).length() > 0 && match.group(2).equalsIgnoreCase("IMAGE");
					final String experiment = match.group(4);
					final String tag = match.group(5);
					final boolean istoptag = tag.length() > 0 && match.group(6).equalsIgnoreCase("TOP");
					final String path = istoptag ? match.group(7) : new StringBuilder("\\").append(tag).append(match.group(7)).toString();
					if(isimage){
						jScopeMultiWave.this.wi.erase();
						jScopeMultiWave.this.wi.addFrames(path);
					}else{
						if(support.getDropAction() == TransferHandler.MOVE) jScopeMultiWave.this.wi.erase();
						jScopeMultiWave.this.wi.addSignal(path);
					}
					jScopeMultiWave.this.wi.setExperiment(experiment);
				}
				final WaveformEvent we = new WaveformEvent(jScopeMultiWave.this, WaveformEvent.EVENT_UPDATE, "Update on Drop event ");
				jScopeMultiWave.this.dispatchWaveformEvent(we);
			}catch(final Exception exc){
				return false;
			}
			return true;
		}
	}

	public static String getBriefError(final String er, final boolean brief) {
		if(brief) return er.substring(0, (er.indexOf('\n') == -1 ? er.length() : er.indexOf('\n')));
		return er;
	}
	String eventName;

	public jScopeMultiWave(final DataProvider dp, final jScopeDefaultValues def_values, final boolean cache_enabled){
		super();
		this.wi = new jScopeWaveInterface(this, dp, def_values, cache_enabled, this.getWidth() * 2);
		this.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent e) {
				final jScopeMultiWave that = ((jScopeMultiWave)e.getSource());
				if(that != null && that.wi != null){
					that.wi.setNumPoints(that.getWidth() * 2);
					try{
						that.wi.setLimits();
					}catch(final Exception e1){/**/
					}
				}
			}
		});
		this.setTransferHandler(new ToTransferHandler());
	}

	public void addEvent() throws IOException {
		((jScopeWaveInterface)this.wi).addEvent(this);
	}

	public void addEvent(final String event) throws IOException {
		((jScopeWaveInterface)this.wi).addEvent(this, event);
	}

	@Override
	public ColorProfile getColorProfile() {
		return this.wi.getColorProfile();
	}

	@Override
	public Marker getMarker(final int idx) {
		if(idx < this.wi.num_waves) return this.wi.markers[idx];
		return Marker.DEFAULT;
	}

	@Override
	public int getSignalCount() {
		return this.wi.num_waves;
	}

	@Override
	public String[] getSignalsName() {
		return this.wi.getSignalsName();
	}

	@Override
	public boolean[] getSignalsState() {
		return this.wi.getSignalsState();
	}

	public void jScopeErase() {
		this.erase();
		this.wi.erase();
	}

	public synchronized void jScopeWaveUpdate() {
		if(this.wi.isAddSignal()){
			// reset to previous configuration if signal/s are not added
			if(((jScopeWaveInterface)this.wi).prev_wi != null && ((jScopeWaveInterface)this.wi).prev_wi.getNumEvaluatedSignal() == ((jScopeWaveInterface)this.wi).getNumEvaluatedSignal()){
				((jScopeWaveInterface)this.wi).prev_wi.error = (this.wi).error;
				((jScopeWaveInterface)this.wi).prev_wi.w_error = ((jScopeWaveInterface)this.wi).w_error;
				((jScopeWaveInterface)this.wi).prev_wi.setAddSignal(this.wi.isAddSignal());
				this.wi = ((jScopeWaveInterface)this.wi).prev_wi;
				this.wi.setIsSignalAdded(false);
			}else this.wi.setIsSignalAdded(true);
			((jScopeWaveInterface)this.wi).prev_wi = null;
		}
		this.update(this.wi);
		final WaveformEvent e = new WaveformEvent(this, WaveformEvent.END_UPDATE);
		this.dispatchWaveformEvent(e);
	}

	@Override
	public void handleUpdateEvent(final String name) {
		this.eventName = name;
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				if(DEBUG.M) System.out.println("Event at waveform " + jScopeMultiWave.this.eventName);
				final WaveformEvent we = new WaveformEvent(jScopeMultiWave.this, WaveformEvent.EVENT_UPDATE, "Update on event " + jScopeMultiWave.this.eventName);
				jScopeMultiWave.this.dispatchWaveformEvent(we);
			}
		});
	}

	public void refresh() {
		try{
			this.addEvent();
		}catch(final IOException e1){/**/}
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				final WaveInterface mwi = jScopeMultiWave.this.wi;
				try{
					mwi.refresh();
				}catch(final Exception e){/**/}
				jScopeMultiWave.this.jScopeWaveUpdate();
			}
		});
	}

	public void refreshOnEvent() {
		final jScopeWaveInterface mwi = (jScopeWaveInterface)this.wi;
		final boolean cache_state = mwi.cache_enabled;
		mwi.cache_enabled = false;
		try{
			mwi.refresh();
		}catch(final Exception e){
			e.getStackTrace();
		}
		mwi.cache_enabled = cache_state;
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				jScopeMultiWave.this.jScopeWaveUpdate();
			}
		});
	}

	public void removeEvent() throws IOException {
		((jScopeWaveInterface)this.wi).removeEvent(this);
	}

	public void removeEvent(final String event) throws IOException {
		((jScopeWaveInterface)this.wi).addEvent(this, event);
	}

	@Override
	public void removeNotify() {
		try{
			this.removeEvent();
		}catch(final IOException e){/**/}
		this.wi = null;
		this.signals = null;
		this.orig_signals = null;
		final Graphics g = this.getGraphics();
		g.dispose();
		super.removeNotify();
	}

	@Override
	public void setColorProfile(final ColorProfile colorProfile) {
		super.setColorProfile(colorProfile);
		this.wi.setColorProfile(colorProfile);
	}

	@Override
	public void setSignalState(final String label, final boolean state) {
		this.wi.setSignalState(label, state);
		super.setSignalState(label, state);
	}

	public void update(final WaveInterface wi_in) {
		this.wi = wi_in;
		this.resetMode();
		this.orig_signals = null;
		this.x_label = this.wi.xlabel;
		this.y_label = this.wi.ylabel;
		this.z_label = this.wi.zlabel;
		this.x_log = this.wi.x_log;
		this.y_log = this.wi.y_log;
		// String error = null;
		// if(!wi.isAddSignal())
		this.wave_error = this.wi.getErrorTitle(true);
		this.title = (this.wi.title != null) ? this.wi.title : "";
		this.setColorProfile(this.wi.getColorProfile());
		this.show_legend = this.wi.show_legend;
		this.legend_x = this.wi.legend_x;
		this.legend_y = this.wi.legend_y;
		this.is_image = this.wi.is_image;
		this.setFrames(this.wi.getFrames());
		if(this.wi.signals != null){
			boolean all_null = true;
			for(int i = 0; i < this.wi.signals.length; i++)
				if(this.wi.signals[i] != null){
					all_null = false;
					if(this.wi.in_label[i] != null && this.wi.in_label[i].length() != 0) this.wi.signals[i].setName(this.wi.in_label[i]);
					else this.wi.signals[i].setName(this.wi.in_y[i]);
					this.wi.signals[i].setMarker(this.wi.markers[i]);
					this.wi.signals[i].setMarkerStep(this.wi.markers_step[i]);
					this.wi.signals[i].setInterpolate(this.wi.interpolates[i]);
					this.wi.signals[i].setColorIdx(this.wi.colors_idx[i]);
					this.wi.signals[i].setMode1D(this.wi.mode1D[i]);
					this.wi.signals[i].setMode2D(this.wi.mode2D[i]);
				}
			if(!all_null){
				this.update(this.wi.signals);
				return;
			}
		}
		if(this.wi.is_image && this.wi.getFrames() != null){
			this.frames.setAspectRatio(this.wi.keep_ratio);
			this.frames.setHorizontalFlip(this.wi.horizontal_flip);
			this.frames.setVerticalFlip(this.wi.vertical_flip);
			this.curr_point_sig_idx = 0;
			if(this.signals.size() != 0) this.signals.removeAllElements();
			if(this.wi.getModified()) this.frame = 0;
			this.not_drawn = true;
			super.update();
			return;
		}
		this.erase();
	}

	@Override
	protected void drawImage(final Graphics g, final Object img, final Dimension dim, final int type) {
		if(type != FrameData.JAI_IMAGE) super.drawImage(g, img, dim, type);
		else{
			((Graphics2D)g).clearRect(0, 0, dim.width, dim.height);
			((Graphics2D)g).drawRenderedImage((RenderedImage)img, new AffineTransform(1f, 0f, 0f, 1f, 0F, 0F));
		}
	}

	@Override
	protected Color getSignalColor(final int i) {
		if(i > this.wi.num_waves) return Color.black;
		return Waveform.colors[this.wi.colors_idx[i] % Waveform.colors.length];
	}

	@Override
	protected String getSignalInfo(final int i) {
		String s;
		final String name = (this.wi.in_label != null && this.wi.in_label[i] != null && this.wi.in_label[i].length() > 0) ? this.wi.in_label[i] : this.wi.in_y[i];
		final String er = (this.wi.w_error != null && this.wi.w_error[i] != null) ? " ERROR " : "";
		// If the legend is defined in the signal, override it
		if(this.signals.size() > i && (this.signals.elementAt(i)).getLegend() != null) return (this.signals.elementAt(i)).getLegend();
		if(this.wi.shots != null) s = name + " " + this.wi.shots[i] + er;
		else s = name + er;
		if(this.signals.size() > i){
			final Signal sign = this.signals.elementAt(i);
			// s += sign.getName(); #1034
			switch(sign.getMode2D()){
				case ZX:
					s = s + " [X-Z Y = " + Waveform.convertToString(sign.getYinXZplot(), false) + " ]";
					break;
				case ZY:
					s = s + " [Y-Z X = " + sign.getStringOfXinYZplot() +
					// Waveform.ConvertToString(sign.getTime(), false) +
					        " ]";
					break;
				default:
					break;
			}
		}
		return this.wi.dp.getLegendString(s);
	}

	@Override
	protected boolean isSignalShow(final int i) {
		return this.wi.getSignalState(i);
	}
}