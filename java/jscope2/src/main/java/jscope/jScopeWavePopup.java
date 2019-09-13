package jscope;

/* $Id$ */
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.Timer;
import jscope.dialog.ColorMapDialog;
import jscope.dialog.SetupDataDialog;
import jscope.waveform.MultiWavePopup;
import jscope.waveform.Waveform;
import jscope.waveform.WaveformManager;

final class jScopeWavePopup extends MultiWavePopup{
	private static final long		serialVersionUID	= 1L;
	protected JMenuItem				refresh, setup, selectWave;
	private final SetupDataDialog	setup_dialog;

	public jScopeWavePopup(final SetupDataDialog setup_dialog, final ColorMapDialog colorMapDialog){
		super();
		this.setColorMapDialog(colorMapDialog);
		this.setup = new JMenuItem("Setup data source...");
		this.setup.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				jScopeWavePopup.this.showDialog();
			}
		});
		this.setup_dialog = setup_dialog;
		this.selectWave = new JMenuItem("Select wave panel");
		this.selectWave.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				if(jScopeWavePopup.this.wave != ((WaveformManager)jScopeWavePopup.this.parent).getSelected()) ((WaveformManager)jScopeWavePopup.this.parent).select(jScopeWavePopup.this.wave);
				else((WaveformManager)jScopeWavePopup.this.parent).deselect();
			}
		});
		this.refresh = new JMenuItem("Refresh");
		this.refresh.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((jScopeWaveContainer)jScopeWavePopup.this.parent).refresh(((jScopeMultiWave)jScopeWavePopup.this.wave), "Refresh ");
			}
		});
	}

	@Override
	public void setDeselectPoint(final Waveform w) {
		final String f_name = System.getProperty("jScope.save_selected_points");
		if(w.showMeasure() && f_name != null && f_name.length() != 0){
			long shot = 0;
			final jScopeMultiWave mw = (jScopeMultiWave)w;
			if(mw.wi.shots != null) shot = mw.wi.shots[mw.getSelectedSignal()];
			try{
				boolean exist = false;
				final File f = new File(f_name);
				if(f.exists()) exist = true;
				@SuppressWarnings("resource")
				final BufferedWriter out = new BufferedWriter(new FileWriter(f_name, true));
				try{
					if(!exist){
						out.write(" Shot X1 Y1 X2 Y2");
						out.newLine();
					}
					out.write(" " + shot + w.getIntervalPoints());
					out.newLine();
				}finally{
					out.close();
				}
			}catch(final IOException e){
				/**/}
		}
		super.setDeselectPoint(w);
	}

	@Override
	protected void setMenu() {
		super.setMenu();
		this.wave = super.wave;
		// remove_panel.setEnabled(((WaveformManager)parent).getWaveformCount()
		// > 1);
		jScopeFacade.jScopeSetUI(this);
	}

	@Override
	protected void setMenuItem(final boolean is_image) {
		super.setMenuItem(is_image);
		this.insert(this.setup, 0);
		this.insert(new JSeparator(), 1);
		this.insert(this.refresh, this.getComponentCount() - 2);
		this.setup.setEnabled((this.setup_dialog != null));
		if(is_image) this.insert(this.profile_dialog, 1);
		else{
			this.insert(this.selectWave, 2);
			this.addSeparator();
			this.add(this.saveAsText);
		}
		if(this.wave != null && ((jScopeMultiWave)this.wave).wi.num_waves == 1) ((jScopeMultiWave)this.wave).wi.setSelectedSignal(0);
	}

	@Override
	protected void setMenuLabel() {
		super.setMenuLabel();
		if(!this.wave.isImage()){
			if(this.wave.isSelected()) this.selectWave.setText("Deselect wave panel");
			else this.selectWave.setText("Select wave panel");
		}else this.profile_dialog.setEnabled(!this.wave.isSendProfile());
	}

	private void showDialog() {
		final jScopeMultiWave w = (jScopeMultiWave)this.wave;
		if(w.getMode() == Waveform.MODE_POINT) this.setup_dialog.selectSignal(w.getSelectedSignal());
		else if(w.getShowSignalCount() > 0 || w.isImage() && w.wi.num_waves != 0) this.setup_dialog.selectSignal(1);
		final Timer t = new Timer(20, new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent ae) {
				final Point p = ((WaveformManager)jScopeWavePopup.this.parent).getWavePosition(jScopeWavePopup.this.wave);
				jScopeWavePopup.this.setup_dialog.show(jScopeWavePopup.this.wave, p.x, p.y);
			}
		});
		t.setRepeats(false);
		t.start();
	}
}
