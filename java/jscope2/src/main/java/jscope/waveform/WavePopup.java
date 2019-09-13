package jscope.waveform;

/* $Id$ */
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import debug.DEBUG;
import jscope.data.signal.Signal;
import jscope.data.signal.Signal.Marker;
import jscope.data.signal.Signal.Mode1D;
import jscope.data.signal.Signal.Mode2D;
import jscope.dialog.ColorMapDialog;

class WavePopup extends JPopupMenu implements ItemListener{
	private static final long serialVersionUID = 1L;

	protected static void selectListItem(final ButtonGroup bg, final int idx) {
		int i;
		JRadioButtonMenuItem b = null;
		Enumeration<AbstractButton> e;
		for(e = bg.getElements(), i = 0; e.hasMoreElements() && i <= idx; i++)
			b = (JRadioButtonMenuItem)e.nextElement();
		if(b != null) bg.setSelected(b.getModel(), true);
	}
	ColorMapDialog					colorMapDialog	= null;
	protected JMenuItem				autoscale, autoscaleY, autoscaleAll, autoscaleAllY, allSameScale, allSameXScale, allSameXScaleAutoY, allSameYScale, resetScales, resetAllScales;
	protected JMenu					markerList, colorList, markerStep, mode_2d, mode_1d;
	protected ButtonGroup			markerList_bg, colorList_bg, markerStep_bg, mode_2d_bg, mode_1d_bg;
	protected Point					mouse_point;
	protected Container				parent;
	protected JMenuItem				playFrame, remove_panel, set_point, undo_zoom, maximize, cb_copy, profile_dialog, colorMap, saveAsText;
	protected JRadioButtonMenuItem	plot_line, plot_no_line, plot_step;
	protected JRadioButtonMenuItem	plot_y_time, plot_x_y, plot_contour, plot_image;
	protected boolean				setmenu;
	protected Waveform				wave			= null;

	public WavePopup(){
		this.remove_panel = new JMenuItem("Remove panel");
		this.remove_panel.setEnabled(false);
		this.remove_panel.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Object[] options = {"Yes", "No"};
				final int opt = JOptionPane.showOptionDialog(WavePopup.this.parent, "Are you sure you want to remove this wave panel?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if(opt == JOptionPane.YES_OPTION) ((WaveformManager)WavePopup.this.parent).removePanel(WavePopup.this.wave);
			}
		});
		this.maximize = new JMenuItem("Maximize Panel");
		this.maximize.setEnabled(false);
		this.maximize.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				if(((WaveformManager)WavePopup.this.parent).isMaximize()) ((WaveformManager)WavePopup.this.parent).maximizeComponent(null);
				else((WaveformManager)WavePopup.this.parent).maximizeComponent(WavePopup.this.wave);
			}
		});
		this.set_point = new JMenuItem("Set Point");
		this.set_point.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				WavePopup.this.setDeselectPoint(WavePopup.this.wave);
			}
		});
		this.markerList = new JMenu("Markers");
		JRadioButtonMenuItem ob;
		this.markerList_bg = new ButtonGroup();
		for(Marker marker : Marker.values()){
			this.markerList_bg.add(ob = new JRadioButtonMenuItem(marker.toString()));
			ob.getModel().setActionCommand("MARKER " + marker.name());
			this.markerList.add(ob);
			ob.addItemListener(this);
		}
		this.markerList.setEnabled(false);
		this.markerStep_bg = new ButtonGroup();
		this.markerStep = new JMenu("Marker step");
		for(int i = 0; i < Signal.markerStepList.length; i++){
			this.markerStep_bg.add(ob = new JRadioButtonMenuItem("" + Signal.markerStepList[i]));
			ob.getModel().setActionCommand("MARKER_STEP " + i);
			this.markerStep.add(ob);
			ob.addItemListener(this);
		}
		this.markerStep.setEnabled(false);
		this.colorList = new JMenu("Colors");
		this.mode_1d_bg = new ButtonGroup();
		this.mode_1d = new JMenu("Mode Plot 1D");
		this.mode_1d.add(this.plot_line = new JRadioButtonMenuItem("Line"));
		this.mode_1d_bg.add(this.plot_line);
		this.plot_line.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if(!WavePopup.this.setmenu && e.getStateChange() == ItemEvent.SELECTED) WavePopup.this.setMode1D(Mode1D.LINE);
			}
		});
		this.mode_1d.add(this.plot_no_line = new JRadioButtonMenuItem("No Line"));
		this.mode_1d_bg.add(this.plot_no_line);
		this.plot_no_line.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if(!WavePopup.this.setmenu && e.getStateChange() == ItemEvent.SELECTED) WavePopup.this.setMode1D(Mode1D.NOLINE);
			}
		});
		this.mode_1d.add(this.plot_step = new JRadioButtonMenuItem("Step Plot"));
		this.mode_1d_bg.add(this.plot_step);
		this.plot_step.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if(!WavePopup.this.setmenu && e.getStateChange() == ItemEvent.SELECTED) WavePopup.this.setMode1D(Mode1D.STEP);
			}
		});
		this.mode_2d_bg = new ButtonGroup();
		this.mode_2d = new JMenu("signal 2D");
		this.mode_2d.add(this.plot_y_time = new JRadioButtonMenuItem("Plot xz(y)"));
		this.mode_2d_bg.add(this.plot_y_time);
		this.plot_y_time.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if(WavePopup.this.setmenu) return;
				if(e.getStateChange() == ItemEvent.SELECTED) WavePopup.this.setMode2D(Mode2D.XZ);
			}
		});
		this.mode_2d.add(this.plot_x_y = new JRadioButtonMenuItem("Plot yz(x)"));
		this.mode_2d_bg.add(this.plot_x_y);
		this.plot_x_y.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if(WavePopup.this.setmenu) return;
				if(e.getStateChange() == ItemEvent.SELECTED) WavePopup.this.setMode2D(Mode2D.YZ);
			}
		});
		this.mode_2d.add(this.plot_contour = new JRadioButtonMenuItem("Plot Contour"));
		this.mode_2d_bg.add(this.plot_contour);
		this.plot_contour.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if(WavePopup.this.setmenu) return;
				if(e.getStateChange() == ItemEvent.SELECTED) WavePopup.this.setMode2D(Mode2D.CONTOUR);
			}
		});
		this.mode_2d.add(this.plot_image = new JRadioButtonMenuItem("Plot Image"));
		this.mode_2d_bg.add(this.plot_image);
		this.plot_image.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if(WavePopup.this.setmenu) return;
				if(e.getStateChange() == ItemEvent.SELECTED) WavePopup.this.setMode2D(Mode2D.IMAGE);
			}
		});
		this.autoscale = new JMenuItem("Autoscale");
		this.autoscale.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				WavePopup.this.wave.autoscale();
			}
		});
		this.autoscaleY = new JMenuItem("Autoscale Y");
		this.autoscaleY.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				WavePopup.this.wave.autoscaleY();
			}
		});
		this.autoscaleAll = new JMenuItem("Autoscale all");
		this.autoscaleAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
		this.autoscaleAll.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				if(WavePopup.this.wave.isImage()) ((WaveformManager)WavePopup.this.parent).autoscaleAllImages();
				else((WaveformManager)WavePopup.this.parent).autoscaleAll();
			}
		});
		this.autoscaleAllY = new JMenuItem("Autoscale all Y");
		this.autoscaleAllY.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
		this.autoscaleAllY.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((WaveformManager)WavePopup.this.parent).autoscaleAllY();
			}
		});
		this.allSameScale = new JMenuItem("All same scale");
		this.allSameScale.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((WaveformManager)WavePopup.this.parent).allSameScale(WavePopup.this.wave);
			}
		});
		this.allSameXScale = new JMenuItem("All same X scale");
		this.allSameXScale.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((WaveformManager)WavePopup.this.parent).allSameXScale(WavePopup.this.wave);
			}
		});
		this.allSameXScaleAutoY = new JMenuItem("All same X scale (auto Y)");
		this.allSameXScaleAutoY.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((WaveformManager)WavePopup.this.parent).allSameXScaleAutoY(WavePopup.this.wave);
			}
		});
		this.allSameYScale = new JMenuItem("All same Y scale");
		this.allSameYScale.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((WaveformManager)WavePopup.this.parent).allSameYScale(WavePopup.this.wave);
			}
		});
		this.resetScales = new JMenuItem("Reset scales");
		this.resetScales.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				WavePopup.this.wave.resetScales();
			}
		});
		this.resetAllScales = new JMenuItem("Reset all scales");
		this.resetAllScales.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((WaveformManager)WavePopup.this.parent).resetAllScales();
			}
		});
		this.undo_zoom = new JMenuItem("Undo Zoom");
		this.undo_zoom.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				WavePopup.this.wave.undoZoom();
			}
		});
		this.cb_copy = new JMenuItem("Copy to Clipboard");
		this.cb_copy.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				if(DEBUG.D) System.out.println("actionPerformed" + e);
				final Dimension dim = WavePopup.this.wave.getSize();
				final BufferedImage ri = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
				final Graphics2D g2d = (Graphics2D)ri.getGraphics();
				g2d.setFont(wave.getFont());
				g2d.setBackground(Color.white);
				WavePopup.this.wave.paint(g2d, dim, Waveform.PRINT);
				try{
					final ImageTransferable imageTransferable = new ImageTransferable(ri);
					final Clipboard cli = Toolkit.getDefaultToolkit().getSystemClipboard();
					cli.setContents(imageTransferable, imageTransferable);
				}catch(final Exception exc){
					exc.printStackTrace();
				}
			}
		});
		this.playFrame = new JMenuItem("Start play");
		this.playFrame.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				if(WavePopup.this.wave.playing()) WavePopup.this.wave.stopFrame();
				else WavePopup.this.wave.playFrame();
			}
		});
		this.profile_dialog = new JMenuItem("Show profile dialog");
		this.profile_dialog.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				WavePopup.this.wave.showProfileDialog();
			}
		});
		this.colorMap = new JMenuItem("Color Palette");
		this.colorMap.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				WavePopup.this.showColorMapDialog(WavePopup.this.wave);
			}
		});
		this.saveAsText = new JMenuItem("Save as text ...");
		this.saveAsText.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(final ActionEvent e) {
				((WaveformContainer)WavePopup.this.parent).saveAsText(WavePopup.this.wave, false);
			}
		});
	}

	@Override
	public void itemStateChanged(final ItemEvent e) {
		final Object target = e.getSource();
		if(target instanceof JRadioButtonMenuItem && e.getStateChange() == ItemEvent.SELECTED){
			final JRadioButtonMenuItem cb = (JRadioButtonMenuItem)target;
			final String action_cmd = cb.getModel().getActionCommand();
			if(action_cmd == null) return;
			final StringTokenizer act = new StringTokenizer(action_cmd);
			final String action = act.nextToken();
			if(action.equals("MARKER")){
				final Marker mark = Marker.valueOf(act.nextToken());
				this.setMarker(mark);
				this.markerStep.setEnabled(!(this.wave.getMarker() == Marker.NONE));
				// this.wave.repaint(true);
				this.wave.reportChanges();
				return;
			}
			final int idx = Integer.parseInt(act.nextToken());
			if(action.equals("MARKER_STEP")){
				this.setMarkerStep(Signal.markerStepList[idx]);
				this.wave.reportChanges();
				return;
			}
			if(action.equals("COLOR_LIST")){
				this.setColor(idx);
				this.wave.reportChanges();
				return;
			}
		}
	}

	public void setColorMapDialog(final ColorMapDialog colorMapDialog) {
		this.colorMapDialog = colorMapDialog;
	}

	public void setDeselectPoint(final Waveform w) {
		if(w.showMeasure()){
			if(this.parent != null && this.parent instanceof WaveformManager) ((WaveformManager)this.parent).setShowMeasure(false);
			w.setShowMeasure(false);
		}else{
			if(this.parent != null && this.parent instanceof WaveformManager) ((WaveformManager)this.parent).setShowMeasure(true);
			w.setShowMeasure(true);
			w.setPointMeasure();
		}
		w.repaint();
	}

	public void setParent(final Container parent) {
		this.parent = parent;
	}

	public void setWave(final Waveform wave_in) {
		this.wave = wave_in;
	}

	public void show(final Waveform wave_in, final Point p) {
		if(wave_in.playing()) wave_in.stopFrame();
		this.wave = wave_in;
		this.mouse_point = p;
		this.setMenu();
		this.setMenuLabel();
		this.show(wave_in, p.x, p.y);
	}

	public void showColorMapDialog(final Waveform wave_in) {
		if(this.colorMapDialog == null) this.colorMapDialog = new ColorMapDialog(null, null);
		else this.colorMapDialog.setWave(wave_in);
		this.colorMapDialog.setLocationRelativeTo(wave_in);
		this.colorMapDialog.setVisible(true);
	}

	protected void initColorMenu() {
		if(!Waveform.isColorsChanged() && this.colorList_bg != null) return;
		if(this.colorList.getItemCount() != 0) this.colorList.removeAll();
		final String[] colors_name = Waveform.getColorsName();
		JRadioButtonMenuItem ob = null;
		this.colorList_bg = new ButtonGroup();
		if(colors_name != null) for(int i = 0; i < colors_name.length; i++){
			this.colorList.add(ob = new JRadioButtonMenuItem(colors_name[i]));
			ob.getModel().setActionCommand("COLOR_LIST " + i);
			this.colorList_bg.add(ob);
			ob.addItemListener(this);
		}
	}

	protected void initOptionMenu() {
		final boolean state = (this.wave.getShowSignalCount() == 1);
		this.markerList.setEnabled(state);
		this.colorList.setEnabled(state);
		this.set_point.setEnabled(true);
		if(state){
			final boolean state_m = (this.wave.getMarker() != Signal.Marker.NONE);
			this.markerStep.setEnabled(state_m);
			WavePopup.selectListItem(this.markerList_bg, this.wave.getMarker().ordinal());
			int st;
			for(st = 0; st < Signal.markerStepList.length; st++)
				if(Signal.markerStepList[st] == this.wave.getMarkerStep()) break;
			WavePopup.selectListItem(this.markerStep_bg, st);
			WavePopup.selectListItem(this.colorList_bg, this.wave.getColorIdx());
		}else this.markerStep.setEnabled(false);
	}

	protected void setColor(final int idx) {
		if(this.wave.getColorIdx() != idx) this.wave.setColorIdx(idx);
	}

	protected void setImageMenu() {
		this.setMenuItem(true);
		final boolean state = (this.wave.frames != null && this.wave.frames.getNumFrame() != 0);
		this.colorList.setEnabled(state);
		WavePopup.selectListItem(this.colorList_bg, this.wave.getColorIdx());
		this.playFrame.setEnabled(state);
		this.set_point.setEnabled(state && ((this.wave.mode == Waveform.MODE_POINT)));
		this.profile_dialog.setEnabled(!this.wave.isSendProfile());
	}

	protected void setMarker(final Marker mark) {
		if(this.wave.getMarker() != mark) this.wave.setMarker(mark);
	}

	protected void setMarkerStep(final int step) {
		if(this.wave.getMarkerStep() != step) this.wave.setMarkerStep(step);
	}

	protected void setMenu() {
		this.setmenu = true;
		this.initColorMenu();
		if(this.wave.is_image) this.setImageMenu();
		else this.setSignalMenu();
		if(this.parent != null && this.parent instanceof WaveformManager) this.remove_panel.setEnabled(((WaveformManager)this.parent).getWaveformCount() > 1);
		this.setmenu = false;
	}

	protected void setMenuItem(final boolean is_image) {
		if(this.getComponentCount() != 0) this.removeAll();
		if(this.is_multi_panel()){
			if(((WaveformManager)this.parent).isMaximize()) this.maximize.setText("Show All Panels");
			else this.maximize.setText("Maximize Panel");
			this.add(this.maximize);
			this.add(this.remove_panel);
		}
		if(is_image){
			this.add(this.colorList);
			this.add(this.colorMap);
			this.add(this.playFrame);
			this.add(this.set_point);
			this.addSeparator();
			this.add(this.autoscale);
			this.add(this.undo_zoom);
			if(this.is_multi_panel()){
				this.addSeparator();
				this.autoscaleAll.setText("Autoscale all images");
				this.add(this.autoscaleAll);
			}
			this.set_point.setEnabled((this.wave.mode == Waveform.MODE_POINT));
		}else{
			this.add(this.set_point);
			this.set_point.setEnabled((this.wave.mode == Waveform.MODE_POINT));
			this.addSeparator();
			this.add(this.markerList);
			this.add(this.markerStep);
			this.add(this.colorList);
			if(this.wave.mode == Waveform.MODE_POINT || this.wave.getShowSignalCount() == 1){
				if(this.wave.getSignalType() == Signal.Type.VECTOR || (this.wave.getSignalType() == Signal.Type.IMAGE && (this.wave.getSignalMode2D() == Mode2D.XZ || this.wave.getSignalMode2D() == Mode2D.YZ))){
					this.add(this.mode_1d);
					switch(this.wave.getSignalMode1D()){
						case LINE:
							this.mode_1d_bg.setSelected(this.plot_line.getModel(), true);
							break;
						case NOLINE:
							this.mode_1d_bg.setSelected(this.plot_no_line.getModel(), true);
							break;
						case STEP:
							this.mode_1d_bg.setSelected(this.plot_step.getModel(), true);
							break;
					}
				}
				if(this.wave.getSignalType() == Signal.Type.IMAGE){
					this.add(this.colorMap);
					this.add(this.mode_2d);
					this.mode_2d.setEnabled(this.wave.getSignalMode2D() != Mode2D.PROFILE);
					switch(this.wave.getSignalMode2D()){
						case XZ:
							this.mode_2d_bg.setSelected(this.plot_y_time.getModel(), true);
							break;
						case YZ:
							this.mode_2d_bg.setSelected(this.plot_x_y.getModel(), true);
							break;
						case CONTOUR:
							this.mode_2d_bg.setSelected(this.plot_contour.getModel(), true);
							break;
						case IMAGE:
							this.mode_2d_bg.setSelected(this.plot_image.getModel(), true);
							break;
						default:
							break;
					}
					this.plot_image.setEnabled(!this.wave.isShowSigImage());
				}
			}
			this.addSeparator();
			this.add(this.autoscale);
			this.add(this.autoscaleY);
			this.add(this.resetScales);
			this.add(this.undo_zoom);
			if(this.is_multi_panel()){
				this.addSeparator();
				this.autoscaleAll.setText("Autoscale all");
				this.add(this.autoscaleAll);
				this.add(this.autoscaleAllY);
				this.add(this.allSameScale);
				this.add(this.allSameXScale);
				this.add(this.allSameXScaleAutoY);
				this.add(this.allSameYScale);
				this.add(this.resetAllScales);
			}
			this.addSeparator();
			this.add(this.cb_copy);
			this.add(this.saveAsText);
		}
	}

	protected void setMenuLabel() {
		if(!this.wave.isImage()){
			if(this.wave.showMeasure()) this.set_point.setText("Deselect Point");
			else this.set_point.setText("Set Point");
		}else if(this.wave.showMeasure()) // && wave.sendProfile())
		    this.set_point.setText("Deselect Point");
		else this.set_point.setText("Set Point");
	}

	protected void setMode1D(final Mode1D mode) {
		this.wave.setSignalMode1D(mode);
	}

	protected void setMode2D(final Mode2D mode) {
		this.wave.setSignalMode2D(mode);
	}

	protected void setSignalMenu() {
		this.setMenuItem(false);
		if(this.wave.getShowSignalCount() != 0) this.initOptionMenu();
		else{
			this.markerList.setEnabled(false);
			this.colorList.setEnabled(false);
			this.markerStep.setEnabled(false);
			this.set_point.setEnabled(false);
		}
		this.undo_zoom.setEnabled(this.wave.undoZoomPendig());
	}

	final private boolean is_multi_panel() {
		return this.parent instanceof WaveformManager && ((WaveformManager)this.parent).getWaveformCount() > 1;
	}
}
