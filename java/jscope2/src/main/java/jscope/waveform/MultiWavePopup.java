package jscope.waveform;

/* $Id$ */
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import jscope.data.signal.Signal;
import jscope.data.signal.Signal.Marker;
import jscope.data.signal.Signal.Mode1D;
import jscope.data.signal.Signal.Mode2D;

public class MultiWavePopup extends WavePopup{
	private static final long	serialVersionUID	= 1L;
	protected JMenuItem			legend, remove_legend;
	protected JMenu				signalList;

	public MultiWavePopup(){
		super();
		this.legend = new JMenuItem("Position legend");
		this.legend.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				if(!MultiWavePopup.this.getWave().isFixedLegend() || !MultiWavePopup.this.getWave().isShowLegend()) MultiWavePopup.this.positionLegend(MultiWavePopup.this.mouse_point);
				else if(MultiWavePopup.this.getWave().isFixedLegend()) MultiWavePopup.this.removeLegend();
			}
		});
		this.legend.setEnabled(false);
		this.remove_legend = new JMenuItem("Remove legend");
		this.remove_legend.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				MultiWavePopup.this.removeLegend();
			}
		});
		this.remove_legend.setEnabled(false);
		this.signalList = new JMenu("Signals");
		this.signalList.setEnabled(false);
	}

	@Override
	public void setColor(final int idx) {
		final MultiWaveform w = this.getWave();
		if(w.getColorIdx(w.getSelectedSignal()) != idx) w.setColorIdx(w.getSelectedSignal(), idx);
		final int sigIdx = w.getSelectedSignal();
		if(w.wi != null && sigIdx != -1 && w.wi.colors_idx[sigIdx] != idx){
			w.wi.colors_idx[w.getSelectedSignal()] = idx % Waveform.colors.length;
			w.setCrosshairColor(idx);
		}
	}

	@Override
	public void setMarker(final Marker mark) {
		final MultiWaveform w = this.getWave();
		if(w.getMarker(w.getSelectedSignal()) != mark) w.setMarker(w.getSelectedSignal(), mark);
		if(w.wi != null && w.wi.markers[w.getSelectedSignal()] != mark) w.wi.markers[w.getSelectedSignal()] = mark;
	}

	@Override
	public void setMarkerStep(final int step) {
		final MultiWaveform w = (MultiWaveform)this.wave;
		if(w.getMarkerStep(w.getSelectedSignal()) != step) w.setMarkerStep(w.getSelectedSignal(), step);
		if(w.wi != null && w.wi.markers_step[w.getSelectedSignal()] != step) w.wi.markers_step[w.getSelectedSignal()] = step;
	}

	public void setSignalState(final String label, final boolean state) {
		this.getWave().setSignalState(label, state);
	}

	final protected MultiWaveform getWave() {
		return (MultiWaveform)this.wave;
	}

	@Override
	protected void initOptionMenu() {
		int sig_idx;
		final String s_name[] = this.getWave().getSignalsName();
		final boolean s_state[] = this.getWave().getSignalsState();
		if(!(s_name != null && s_state != null && s_name.length > 0 && s_name.length > 0 && s_name.length == s_state.length)) return;
		final boolean state = (this.getWave().mode == Waveform.MODE_POINT || this.getWave().getShowSignalCount() == 1);
		this.markerList.setEnabled(state);
		this.colorList.setEnabled(state);
		this.set_point.setEnabled(this.getWave().mode == Waveform.MODE_POINT);
		if(state){
			if(this.getWave().getShowSignalCount() == 1) sig_idx = 0;
			else sig_idx = this.getWave().getSelectedSignal();
			final boolean state_m = state && (this.getWave().getMarker(sig_idx) != Signal.Marker.NONE);
			this.markerStep.setEnabled(state_m);
			WavePopup.selectListItem(this.markerList_bg, this.getWave().getMarker(sig_idx).ordinal());
			int st;
			for(st = 0; st < Signal.markerStepList.length; st++)
				if(Signal.markerStepList[st] == this.getWave().getMarkerStep(sig_idx)) break;
			WavePopup.selectListItem(this.markerStep_bg, st);
			WavePopup.selectListItem(this.colorList_bg, this.getWave().getColorIdx(sig_idx));
		}else this.markerStep.setEnabled(false);
		JCheckBoxMenuItem ob;
		if(this.signalList.getItemCount() != 0) this.signalList.removeAll();
		this.signalList.setEnabled(s_name.length != 0);
		this.legend.setEnabled(s_name.length != 0);
		for(int i = 0; i < s_name.length; i++){
			ob = new JCheckBoxMenuItem(s_name[i]);
			this.signalList.add(ob);
			ob.setState(s_state[i]);
			ob.addItemListener(new ItemListener(){
				@Override
				public void itemStateChanged(final ItemEvent e) {
					final Object target = e.getSource();
					MultiWavePopup.this.setSignalState(((JCheckBoxMenuItem)target).getText(), ((JCheckBoxMenuItem)target).getState());
					MultiWavePopup.this.getWave().repaint(true);
				}
			});
		}
		if(this.getWave().isFixedLegend()){
			if(this.getWave().isShowLegend()) this.legend.setText("Hide Legend");
			else this.legend.setText("Show Legend");
		}else{
			this.legend.setText("Position Legend");
			if(this.getWave().isShowLegend()) this.remove_legend.setEnabled(true);
			else this.remove_legend.setEnabled(false);
		}
	}

	protected void positionLegend(final Point p) {
		final MultiWaveform w = this.getWave();
		w.setLegend(p);
		if(w.wi == null) return;
		w.wi.showLegend(true);
		w.wi.setLegendPosition(w.getLegendXPosition(), w.getLegendYPosition());
	}

	protected void removeLegend() {
		final MultiWaveform w = this.getWave();
		w.removeLegend();
	}

	protected void setInterpolate(final boolean state) {
		final MultiWaveform w = this.getWave();
		w.setInterpolate(w.getSelectedSignal(), state);
		w.wi.interpolates[w.getSelectedSignal()] = state;
	}

	@Override
	protected void setMenuItem(final boolean is_image) {
		int start = 0;
		super.setMenuItem(is_image);
		if(!is_image){
			if(this.parent instanceof WaveformManager) start += 2;
			this.insert(this.legend, start + 0);
			if(this.getWave().isFixedLegend()){
				this.insert(this.signalList, start + 4);
				this.legend.setText("Show Legend");
			}else{
				this.insert(this.remove_legend, start + 1);
				this.insert(this.signalList, start + 5);
			}
		}
	}

	@Override
	protected void setMode1D(final Mode1D mode) {
		final MultiWaveform w = this.getWave();
		if(w.wi != null) w.wi.mode1D[w.getSelectedSignal()] = mode;
		super.setMode1D(mode);
	}

	@Override
	protected void setMode2D(final Mode2D mode) {
		final MultiWaveform w = this.getWave();
		w.setSignalMode2D(w.getSelectedSignal(), mode);
		if(w.wi != null) w.wi.mode2D[w.getSelectedSignal()] = mode;
	}

	@Override
	protected void setSignalMenu() {
		super.setSignalMenu();
		if(this.getWave().getShowSignalCount() == 0){
			this.legend.setEnabled(false);
			this.remove_legend.setEnabled(false);
			this.signalList.setEnabled(false);
		}
	}
}
