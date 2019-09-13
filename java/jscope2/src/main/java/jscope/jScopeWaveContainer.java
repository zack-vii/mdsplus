package jscope;

/* $Id$ */
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.function.BiConsumer;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.SimpleDoc;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import debug.DEBUG;
import jscope.data.DataProvider;
import jscope.data.DataServerItem;
import jscope.data.NotConnectedDataProvider;
import jscope.data.UpdateEventListener;
import jscope.data.signal.Signal;
import jscope.dialog.ColorMapDialog;
import jscope.waveform.MultiWaveform;
import jscope.waveform.RowColumnLayout;
import jscope.waveform.WaveContainerEvent;
import jscope.waveform.WaveInterface;
import jscope.waveform.Waveform;
import jscope.waveform.WaveformContainer;
import jscope.waveform.WaveformEvent;

public final class jScopeWaveContainer extends WaveformContainer{
	private static final long serialVersionUID = 1L;
	class UpdW extends Thread{
		boolean pending = false;

		@Override
		public void run() {
			WaveContainerEvent wce = null;
			this.setName("Update Thread");
			while(true)
				try{
					while(!this.pending)
						synchronized(this){
							this.wait(3);
						}
					this.pending = false;
					wce = new WaveContainerEvent(this, WaveContainerEvent.START_UPDATE, "Start Update");
					jScopeWaveContainer.this.dispatchWaveContainerEvent(wce);
					try{
						long time = System.nanoTime();
						jScopeWaveContainer.this.updateAllWave();
						time = System.nanoTime() - time;
						final String msg;
						if(!jScopeWaveContainer.this.abort) msg = String.format("All waveforms are up to date < %d ms >", new Long(time / 1000000l));
						else msg = " Aborted ";
						wce = new WaveContainerEvent(this, WaveContainerEvent.END_UPDATE, msg);
						jScopeWaveContainer.this.dispatchWaveContainerEvent(wce);
					}catch(final Throwable e){
						e.printStackTrace();
						wce = new WaveContainerEvent(this, WaveContainerEvent.KILL_UPDATE, e.getMessage());
						jScopeWaveContainer.this.dispatchWaveContainerEvent(wce);
					}
				}catch(final InterruptedException e){/**/}
		}

		synchronized public void startUpdate() {
			this.pending = true;
			this.notify();
		}
	}

	public static DataServerItem buildDataServerItem(final Properties pr, final String prompt) {
		if(pr != null){
			final String root = prompt + ".data_server_";
			final DataServerItem dsi = new DataServerItem();
			dsi.properties = new HashMap<String, String>();
			pr.forEach(new BiConsumer<Object, Object>(){
				@Override
				public void accept(final Object _k, final Object _v) {
					final String k = (String)_k, v = (String)_v;
					if(k.startsWith(root)){
						final String propname = k.substring(root.length());
						if("name".equals(propname)) dsi.name = v;
						else if("server".equals(propname) || "argument".equals(propname)) dsi.server = v;
						else if("user".equals(propname)) dsi.user = v;
						else if("class".equals(propname)) dsi.class_name = v;
						else if("browse_class".equals(propname)) dsi.browse_class = v;
						else dsi.properties.put(propname, v);
					}
				}
			});
			if(dsi.properties.size() == 0) dsi.properties = null;
			return dsi;
		}
		return null;
	}

	public static DataServerItem dataServerFromClient(final DataServerItem dataServerIn) {
		int c = 0;
		final boolean found = false;
		String clientMask;
		DataServerItem out = null;
		String prompt;
		try{
			final InetAddress localaddr = InetAddress.getLocalHost();
			final String localIpAdddress = localaddr.getHostAddress();
			final File f_name = Paths.get(System.getProperty("user.home"), "jScope", "jScope_servers.conf").toFile();
			if(dataServerIn != null){
				final InetAddress dsiInet = InetAddress.getByName(dataServerIn.server);
				final String disIpAdddress = dsiInet.getHostAddress();
				final StringTokenizer st = new StringTokenizer(localIpAdddress, ".");
				clientMask = st.nextToken() + "." + st.nextToken() + ".*.*";
				if(!jScopeWaveContainer.checkIpMask(disIpAdddress, clientMask)) return dataServerIn;
			}
			if(f_name.exists()){
				final Properties srvFromClientProp = new Properties();
				@SuppressWarnings("resource")
				final FileInputStream fis = new FileInputStream(f_name);
				try{
					srvFromClientProp.load(fis);
				}finally{
					fis.close();
				}
				while(!found){
					c++;
					prompt = "jScope.server_from_client_" + c;
					clientMask = srvFromClientProp.getProperty(prompt + ".client_mask");
					if(clientMask == null) break;
					/*
					StringTokenizer tokenLocalIp = new StringTokenizer(localIpAdddress, ".");
					StringTokenizer clientMaskIp = new StringTokenizer(clientMask, ".");
					if(tokenLocalIp.countTokens() != clientMaskIp.countTokens()) continue;
					while(tokenLocalIp.hasMoreElements() && clientMaskIp.hasMoreTokens())
					{
					    String tl = tokenLocalIp.nextToken();
					    String tm = clientMaskIp.nextToken();
					    if(found = (tl.equals(tm) || tm.equals("*"))) continue;
					    found = false;
					    break;
					}
					 */
					if(jScopeWaveContainer.checkIpMask(localIpAdddress, clientMask)) out = jScopeWaveContainer.buildDataServerItem(srvFromClientProp, prompt);
				}
			}
		}catch(final Exception exc){
			out = null;
		}
		return out;
	}

	public static final void freeCache() {
		WaveInterface.freeCache();
	}

	protected static jScopeMultiWave buildjScopeMultiWave(final DataProvider dp_in, final jScopeDefaultValues def_vals_in) {
		return new jScopeMultiWave(dp_in, def_vals_in, false);
	}

	private static boolean checkIpMask(final String ip, final String Mask) {
		boolean found = false;
		final StringTokenizer tokenLocalIp = new StringTokenizer(ip, ".");
		final StringTokenizer clientMaskIp = new StringTokenizer(Mask, ".");
		if(tokenLocalIp.countTokens() != clientMaskIp.countTokens()) return false;
		while(tokenLocalIp.hasMoreElements() && clientMaskIp.hasMoreTokens()){
			final String tl = tokenLocalIp.nextToken();
			final String tm = clientMaskIp.nextToken();
			if(tl.equals(tm) || tm.equals("*")){
				found = true;
				continue;
			}
			found = false;
			break;
		}
		return found;
	}

	private static void writeLine(final PrintWriter out, final String prompt, final String value) {
		if(value != null && value.length() != 0) out.println(prompt + value);
	}
	private boolean						abort;
	private boolean						add_sig				= false;
	private SignalBrowser				signalbrowserclass	= null;
	private final jScopeDefaultValues	def_vals;
	DataProvider						dp;
	private String						event				= null;
	private String						main_shot_error		= null;
	private String						main_shot_str		= null;
	private int[]						main_shots			= null;
	private final Object				mainShotLock		= new Object();
	private String						prev_add_signal		= null;
	private String						print_event			= null;
	ProgressMonitor						progressMonitor;
	private DataServerItem				server_item			= null;
	private String						title				= null;
	private UpdW						updateThread;
	jScopeMultiWave						wave_all[];
	private JDialog						signalbrowser;

	public jScopeWaveContainer(final int rows[], final DataProvider dp, final jScopeDefaultValues def_vals){
		super(rows, false);
		this.def_vals = def_vals;
		this.dp = dp;
		final Component c[] = this.createWaveComponents(this.getComponentNumber());
		this.addComponents(c);
		this.updateThread = new UpdW();
		this.updateThread.start();
		this.setBackground(Color.white);
		this.save_as_txt_directory = System.getProperty("jScope.curr_directory");
	}

	public jScopeWaveContainer(final int rows[], final jScopeDefaultValues def_vals){
		this(rows, new NotConnectedDataProvider(), def_vals);
		this.server_item = new DataServerItem("Not Connected", null, null, NotConnectedDataProvider.class.getName(), null, null);
	}

	public void abortUpdate() {
		this.abort = true;
		this.dp.reset();
	}

	public synchronized void addAllEvents(final UpdateEventListener l) throws IOException {
		jScopeMultiWave w;
		if(this.dp == null) return;
		if(this.event != null && this.event.length() != 0) this.dp.addUpdateEventListener(l, this.event);
		if(this.print_event != null && this.print_event.length() != 0) this.dp.addUpdateEventListener(l, this.print_event);
		for(int i = 0, k = 0; i < RowColumnLayout.MAX_COLUMN; i++)
			for(int j = 0; j < this.getComponentsInColumn(i); j++, k++){
				w = (jScopeMultiWave)this.getGridComponent(k);
				w.addEvent();
			}
	}

	public void addSignal(final String expr, final boolean check_prev_signal) {
		if(expr != null && expr.length() != 0) if(!check_prev_signal || (check_prev_signal && (this.prev_add_signal == null || !this.prev_add_signal.equals(expr)))){
			this.prev_add_signal = expr;
			this.addSignal(null, null, "", expr, false, false);
		}
	}

	public void addSignal(final String tree, final String shot, final String x_expr, final String y_expr, final boolean with_error, final boolean is_image) {
		final String x[] = new String[1];
		final String y[] = new String[1];
		x[0] = x_expr;
		y[0] = y_expr;
		this.addSignals(tree, shot, x, y, with_error, is_image);
	}

	// with_error == true => Signals is added also if an error occurs
	// during its evaluations
	public void addSignals(final String tree, final String shot, final String x_expr[], final String y_expr[], final boolean with_error, final boolean is_image) {
		jScopeWaveInterface new_wi = null;
		final jScopeMultiWave sel_wave = (jScopeMultiWave)this.getSelectPanel();
		if(sel_wave.wi == null || is_image){
			sel_wave.wi = new jScopeWaveInterface(sel_wave, this.dp, this.def_vals, false);
			sel_wave.wi.setAsImage(is_image);
			if(!with_error) ((jScopeWaveInterface)sel_wave.wi).prev_wi = new jScopeWaveInterface(sel_wave, this.dp, this.def_vals, false);
		}else{
			new_wi = new jScopeWaveInterface((jScopeWaveInterface)sel_wave.wi);
			new_wi.wave = sel_wave;
			if(!with_error) new_wi.prev_wi = (jScopeWaveInterface)sel_wave.wi;
			sel_wave.wi = new_wi;
		}
		if(tree != null && (((jScopeWaveInterface)sel_wave.wi).cexperiment == null || ((jScopeWaveInterface)sel_wave.wi).cexperiment.trim().length() == 0)){
			((jScopeWaveInterface)sel_wave.wi).cexperiment = new String(tree);
			((jScopeWaveInterface)sel_wave.wi).defaults &= ~(1 << jScopeWaveInterface.B_exp);
		}
		if(shot != null && (((jScopeWaveInterface)sel_wave.wi).cin_shot == null || ((jScopeWaveInterface)sel_wave.wi).cin_shot.trim().length() == 0)){
			((jScopeWaveInterface)sel_wave.wi).cin_shot = new String(shot);
			((jScopeWaveInterface)sel_wave.wi).defaults &= ~(1 << jScopeWaveInterface.B_shot);
		}
		if(sel_wave.wi.addSignals(x_expr, y_expr)){
			this.add_sig = true;
			this.refresh(sel_wave, "Add signal to");
			this.update();
			this.add_sig = false;
		}
	}

	public void changeDataProvider(final DataProvider dp_in) {
		jScopeMultiWave w;
		this.main_shot_str = null;
		for(int i = 0; i < this.getGridComponentCount(); i++){
			w = (jScopeMultiWave)this.getWavePanel(i);
			if(w != null){
				if(w.wi != null) w.wi.setDataProvider(dp_in);
				w.erase();
				w.setTitle(null);
			}
		}
	}

	public void eraseAllWave() {
		jScopeMultiWave w;
		for(int i = 0; i < this.getComponentNumber(); i++){
			w = (jScopeMultiWave)this.getGridComponent(i);
			if(w != null) w.erase();
		}
	}

	public void evaluateMainShot(final String in_shots) throws IOException {
		int[] newshots = null;
		synchronized(this.mainShotLock){
			this.main_shot_error = null;
			this.main_shots = null;
			this.main_shot_str = null;
			if(in_shots == null || in_shots.trim().length() == 0){
				this.main_shot_error = "Main shot value Undefine";
				return;
			}
			newshots = WaveInterface.getShotArray(in_shots, this.def_vals.experiment_str, this.dp);
			if(this.main_shot_error == null) this.main_shots = newshots;
			this.main_shot_str = in_shots.trim();
		}
	}

	public void fromFile(final Properties pr, final String prompt, final int colorMapping[], final ColorMapDialog cmd) throws IOException {
		String prop;
		final int[] read_rows = new int[RowColumnLayout.MAX_COLUMN];
		this.resetMaximizeComponent();
		prop = pr.getProperty(prompt + ".columns");
		if(prop == null) throw(new IOException("missing columns keyword"));
		final int new_columns = new Integer(prop).intValue();
		final int[] ipw = new_columns > 1 ? new int[new_columns] : new int[]{1};
		if(new_columns == 0) read_rows[0] = 1;
		this.title = pr.getProperty(prompt + ".title");
		this.event = pr.getProperty(prompt + ".update_event");
		this.print_event = pr.getProperty(prompt + ".print_event");
		final DataServerItem serveritem = jScopeWaveContainer.buildDataServerItem(pr, prompt);
		if(serveritem != null){
			final DataServerItem server_item_conf = jScopeWaveContainer.dataServerFromClient(serveritem);
			if(server_item_conf != null) this.server_item = server_item_conf;
			else this.server_item = serveritem;
		}
		for(int c = 0; c < new_columns && c <= RowColumnLayout.MAX_COLUMN; c++){
			prop = pr.getProperty(prompt + ".rows_in_column_" + (c + 1));
			if(prop == null){
				if(c == 0) throw(new IOException("missing rows_in_column_1 keyword"));
				break;
			}
			read_rows[c] = new Integer(prop).intValue();
		}
		if(new_columns > 1) for(int c = 1; c < new_columns; c++){
			prop = pr.getProperty(prompt + ".vpane_" + (c));
			if(prop == null) throw(new IOException("missing vpane_" + c + " keyword"));
			final int w = new Integer(prop).intValue();
			ipw[c - 1] = w;
		}
		prop = pr.getProperty(prompt + ".reversed");
		if(prop != null) this.reversed = Boolean.parseBoolean(prop);
		else this.reversed = false;
		this.def_vals.xmax = pr.getProperty(prompt + ".global_1_1.xmax");
		this.def_vals.xmin = pr.getProperty(prompt + ".global_1_1.xmin");
		this.def_vals.xlabel = pr.getProperty(prompt + ".global_1_1.x_label");
		this.def_vals.ymax = pr.getProperty(prompt + ".global_1_1.ymax");
		this.def_vals.ymin = pr.getProperty(prompt + ".global_1_1.ymin");
		this.def_vals.ylabel = pr.getProperty(prompt + ".global_1_1.y_label");
		this.def_vals.experiment_str = pr.getProperty(prompt + ".global_1_1.experiment");
		this.def_vals.title_str = pr.getProperty(prompt + ".global_1_1.title");
		this.def_vals.upd_event_str = pr.getProperty(prompt + ".global_1_1.event");
		this.def_vals.def_node_str = pr.getProperty(prompt + ".global_1_1.default_node");
		prop = pr.getProperty(prompt + ".global_1_1.horizontal_offset");
		{
			if(prop != null){
				int v = 0;
				try{
					v = Integer.parseInt(prop);
				}catch(final NumberFormatException exc){/**/}
				Waveform.setHorizontalOffset(v);
			}
		}
		prop = pr.getProperty(prompt + ".global_1_1.vertical_offset");
		{
			if(prop != null){
				int v = 0;
				try{
					v = Integer.parseInt(prop);
				}catch(final NumberFormatException exc){/**/}
				Waveform.setVerticalOffset(v);
			}
		}
		prop = pr.getProperty(prompt + ".global_1_1.shot");
		if(prop != null){
			if(prop.indexOf("_shots") != -1) this.def_vals.shot_str = prop.substring(prop.indexOf("[") + 1, prop.indexOf("]"));
			else this.def_vals.shot_str = prop;
			this.def_vals.setIsEvaluated(false);
		}
		if(read_rows[0] == 0) read_rows[0] = 1;
		this.resetDrawPanel(read_rows);
		jScopeMultiWave w;
		for(int c = 0, k = 0; c < read_rows.length; c++)
			for(int r = 0; r < read_rows[c]; r++){
				w = (jScopeMultiWave)this.getGridComponent(k);
				((jScopeWaveInterface)w.wi).fromFile(pr, "Scope.plot_" + (r + 1) + "_" + (c + 1), cmd);
				((jScopeWaveInterface)w.wi).mapColorIndex(colorMapping);
				this.setWaveParams(w);
				k++;
			}
		// Evaluate real number of columns
		int r_columns = 0;
		for(int i = 0; i < read_rows.length; i++){
			if(i < r_columns){
				read_rows[r_columns] = read_rows[i];
				read_rows[i] = 0;
			}
			if(read_rows[r_columns] != 0) r_columns = i + 1;
		}
		// Silent file configuration correction
		// possible define same warning information
		if(new_columns != r_columns){
			this.setRowColumn(read_rows);
			for(int i = 0; i < this.pw.length; i++)
				this.pw[i] = i < r_columns ? (float)1. / r_columns : 0.f;
		}else{
			this.setRowColumn(read_rows);
			if(new_columns == RowColumnLayout.MAX_COLUMN) this.pw[7] = Math.abs((float)((1000 - this.pw[7]) / 1000.));
			this.pw[0] = ipw[0] / 1000.f;
			for(int i = 1; i < new_columns - 1; i++)
				this.pw[i] = ((ipw[i] == 0 ? 1000 : ipw[i]) - ipw[i - 1]) / 1000.f;
			for(int i = new_columns; i < RowColumnLayout.MAX_COLUMN; i++)
				this.pw[i] = 0.f;
		}
		this.updateHeight();
	}

	@SuppressWarnings("unchecked")
	public Class<SignalBrowser> getBrowserClass() {
		try{
			return((this.server_item != null && this.server_item.browse_class != null) ? (Class<SignalBrowser>)Class.forName(this.server_item.browse_class) : this.dp.getDefaultBrowser());
		}catch(final ClassNotFoundException e){
			return null;
		}
	}

	public String getDefaultExpt() {
		return this.def_vals.experiment_str;
	}

	public String getEvaluatedTitle() {
		if(this.title == null || this.title.length() == 0 || this.dp == null) return "";
		try{
			final String t = this.dp.getString(this.title);
			final String err = this.dp.errorString();
			if(err == null || err.length() == 0) return t;
			return "< evaluation error >";
		}catch(final Exception e){
			return "";
		}
	}

	public String getEvent() {
		return this.event;
	}

	public void getjScopeMultiWave() {
		this.wave_all = new jScopeMultiWave[this.getGridComponentCount()];
		for(int i = 0, k = 0; i < this.rows.length; i++)
			for(int j = 0; j < this.rows[i]; j++, k++)
				this.wave_all[k] = (jScopeMultiWave)this.getGridComponent(k);
	}

	public String getMainShotError(final boolean brief) {
		return this.main_shot_error;
	}

	public int[] getMainShots() {
		return this.main_shots;
	}

	public String getMainShotStr() {
		return this.main_shot_str;
	}

	public String getPrintEvent() {
		return this.print_event;
	}

	// public String GetServerLabel(){return (server_item != null ? server_item.name : "");}
	public String getServer() {
		return(this.server_item != null ? this.server_item.server : "");
	}

	public DataServerItem getServerItem() {
		return this.server_item;
	}

	/*
	remove 28/06/2005
	    public void SetServerItem(DataServerItem dsi)
	    {
	        server_item = dsi;
	    }
	 */
	public String getServerLabel() {
		if(this.dp == null && this.server_item != null && this.server_item.name != null) return "Can't connect to " + this.server_item.name;
		if(this.dp == null && this.server_item == null) return "Not connected";
		return this.server_item.name;
	}

	public String getTitle() {
		return this.title;
	}

	public void invalidateDefaults() {
		jScopeMultiWave w;
		for(int i = 0, k = 0; i < this.rows.length; i++)
			for(int j = 0; j < this.rows[i]; j++, k++){
				w = (jScopeMultiWave)this.getGridComponent(k);
				if(w != null) ((jScopeWaveInterface)w.wi).default_is_update = false;
			}
	}

	@Override
	public boolean isAborted() {
		return this.abort;
	}

	@Override
	public void maximizeComponent(final Waveform w) {
		super.maximizeComponent(w);
		if(w == null) this.startUpdate();
	}

	@Override
	public void notifyChange(final Waveform dest, final Waveform source) {
		final jScopeMultiWave w = ((jScopeMultiWave)source);
		final jScopeWaveInterface mwi = new jScopeWaveInterface(((jScopeWaveInterface)w.wi));
		mwi.setDefaultsValues(this.def_vals);
		((jScopeMultiWave)dest).wi = mwi;
		((jScopeMultiWave)dest).wi.setDataProvider(this.dp);
		((jScopeMultiWave)dest).wi.wave = dest;
		this.refresh((jScopeMultiWave)dest, "Copy in");
	}

	@Override
	public int print(final Graphics g, final PageFormat pf, final int pageIndex) throws PrinterException {
		final int st_x = 0, st_y = 0;
		final double height = pf.getImageableHeight();
		final double width = pf.getImageableWidth();
		final Graphics2D g2 = (Graphics2D)g;
		if(pageIndex == 0){
			g2.translate(pf.getImageableX(), pf.getImageableY());
			this.printAll(g2, st_x, st_y, (int)height, (int)width);
			return Printable.PAGE_EXISTS;
		}
		return Printable.NO_SUCH_PAGE;
	}

	public void printAll(final Graphics g, final int height, final int width) {
		if(this.font == null) this.font = g.getFont();
		else g.setFont(this.font);
		super.printAll(g, 0, 0, height, width);
	}

	@Override
	public void printAll(final Graphics g, final int st_x, int st_y, int height, final int width) {
		final String ltitle = this.getEvaluatedTitle();
		if(ltitle != null && ltitle.length() != 0){
			FontMetrics fm;
			int s_width;
			if(this.font == null){
				this.font = g.getFont();
				this.font = new Font(this.font.getName(), this.font.getStyle(), 18);
				g.setFont(this.font);
			}else{
				this.font = new Font(this.font.getName(), this.font.getStyle(), 18);
				g.setFont(this.font);
			}
			fm = g.getFontMetrics();
			s_width = fm.stringWidth(ltitle);
			st_y += fm.getHeight() / 2 + 2;
			g.drawString(ltitle, st_x + (width - s_width) / 2, st_y);
			st_y += 2;
			height -= st_y;
		}
		super.printAll(g, st_x, st_y, height, width);
	}

	public void printAllWaves(final DocPrintJob prnJob, final PrintRequestAttributeSet attrs) throws PrintException {
		final DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
		final Doc doc = new SimpleDoc(this, flavor, null);
		prnJob.print(doc, attrs);
	}

	@Override
	public void processWaveformEvent(final WaveformEvent e) {
		super.processWaveformEvent(e);
		final jScopeMultiWave w = (jScopeMultiWave)e.getSource();
		switch(e.getID()){
			case WaveformEvent.END_UPDATE:
				final Point p = this.getComponentPosition(w);
				if(w.wi.isAddSignal()){
					String er;
					if(!w.wi.isSignalAdded()) this.prev_add_signal = null;
					if(w.wi.error != null) er = w.wi.error;
					else er = ((jScopeWaveInterface)w.wi).getErrorString(); // this.brief_error);
					if(er != null) JOptionPane.showMessageDialog(this, er, "alert processWaveformEvent", JOptionPane.ERROR_MESSAGE);
					w.wi.setAddSignal(false);
				}
				final WaveContainerEvent wce = new WaveContainerEvent(this, WaveContainerEvent.END_UPDATE, "Wave column " + p.x + " row " + p.y + " is updated");
				jScopeWaveContainer.this.dispatchWaveContainerEvent(wce);
				break;
		}
	}

	public synchronized void refresh(final jScopeMultiWave w, final String label) {
		Point p = null;
		if(this.add_sig) p = this.getSplitPosition();
		if(p == null) p = this.getComponentPosition(w);
		this.setCacheState(false);
		final WaveContainerEvent wce = new WaveContainerEvent(this, WaveContainerEvent.START_UPDATE, label + " wave column " + p.x + " row " + p.y);
		jScopeWaveContainer.this.dispatchWaveContainerEvent(wce);
		w.refresh();
		if(this.add_sig) this.resetSplitPosition();
	}

	public void removeAllEvents(final UpdateEventListener l) throws IOException {
		jScopeMultiWave w;
		if(this.dp == null) return;
		if(this.event != null && this.event.length() != 0) this.dp.removeUpdateEventListener(l, this.event);
		if(this.print_event != null && this.print_event.length() != 0) this.dp.removeUpdateEventListener(l, this.print_event);
		for(int i = 0, k = 0; i < this.rows.length; i++)
			for(int j = 0; j < this.getComponentsInColumn(i); j++, k++){
				w = (jScopeMultiWave)this.getGridComponent(k);
				w.removeEvent();
			}
	}

	public void reset() {
		final int[] reset_rows = new int[this.rows.length];
		reset_rows[0] = 1;
		this.ph = null;
		this.setTitle(null);
		this.event = null;
		this.print_event = null;
		this.resetDrawPanel(reset_rows);
		this.update();
		final jScopeMultiWave w = (jScopeMultiWave)this.getWavePanel(0);
		w.jScopeErase();
		this.def_vals.reset();
	}

	public void saveAsText(final jScopeMultiWave w, final boolean all) {
		final Vector<jScopeMultiWave> panel = new Vector<jScopeMultiWave>();
		jScopeWaveInterface wi;
		jScopeMultiWave wave;
		if(!all && (w == null || w.wi == null || w.wi.signals == null || w.wi.signals.length == 0)) return;
		final String ltitle;
		if(all) ltitle = "Save all signals in text format";
		else{
			final Point p = this.getWavePosition(w);
			if(p == null) ltitle = "Save";
			else ltitle = "Save signals on panel (" + p.x + ", " + p.y + ") in text format";
		}
		JFileChooser file_diag = new JFileChooser();
		if(this.save_as_txt_directory != null && this.save_as_txt_directory.trim().length() != 0) file_diag.setCurrentDirectory(new File(this.save_as_txt_directory));
		file_diag.setDialogTitle(ltitle);
		int returnVal = JFileChooser.CANCEL_OPTION;
		String txtsig_file = null;
		while(true){
			returnVal = file_diag.showSaveDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION){
				final File file = file_diag.getSelectedFile();
				txtsig_file = file.getAbsolutePath();
				if(file.exists()){
					final Object[] options = {"Yes", "No"};
					final int val = JOptionPane.showOptionDialog(this, txtsig_file + " already exists.\nDo you want to replace it?", "Save as", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
					if(val == JOptionPane.YES_OPTION) break;
				}else break;
			}else break;
		}
		if(returnVal == JFileChooser.APPROVE_OPTION) if(txtsig_file != null){
			this.save_as_txt_directory = new String(txtsig_file);
			if(all) for(int i = 0; i < this.getWaveformCount(); i++)
				panel.addElement((jScopeMultiWave)this.getWavePanel(i));
			else panel.addElement(w);
			try{
				final StringBuffer space = new StringBuffer();
				String s = "", s1 = "", s2 = "";
				@SuppressWarnings("resource")
				final BufferedWriter out = new BufferedWriter(new FileWriter(txtsig_file));
				try{
					for(int l = 0; l < 3; l++){
						s = "%";
						for(int k = 0; k < panel.size(); k++){
							wave = panel.elementAt(k);
							wi = (jScopeWaveInterface)wave.wi;
							if(wi == null || wi.signals == null) continue;
							for(int i = 0; i < wi.signals.length; i++){
								switch(l){
									case 0:
										s += "x : " + ((wi.in_x != null && wi.in_x.length > 0) ? wi.in_x[i] : "None");
										break;
									case 1:
										s += "y : " + ((wi.in_y != null && wi.in_y.length > 0) ? wi.in_y[i] : "None");
										break;
									case 2:
										s += "Shot : " + ((wi.shots != null && wi.shots.length > 0) ? "" + wi.shots[i] : "None");
										break;
								}
								out.write(s, 0, (s.length() < 50) ? s.length() : 50);
								space.setLength(0);
								for(int u = 0; u < 52 - s.length(); u++)
									space.append(' ');
								out.write(space.toString());
								s = "";
							}
						}
						out.newLine();
					}
				}finally{
					out.close();
				}
				int n_max_sig = 0;
				final boolean more_point[] = new boolean[panel.size()];
				for(int k = 0; k < panel.size(); k++){
					more_point[k] = true;
					wave = panel.elementAt(k);
					wi = (jScopeWaveInterface)wave.wi;
					if(wi == null || wi.signals == null) continue;
					if(wi.signals.length > n_max_sig) n_max_sig = wi.signals.length;
				}
				boolean g_more_point = true;
				final int start_idx[][] = new int[panel.size()][n_max_sig];
				while(g_more_point){
					g_more_point = false;
					for(int k = 0; k < panel.size(); k++){
						wave = panel.elementAt(k);
						wi = (jScopeWaveInterface)wave.wi;
						if(wi == null || wi.signals == null) continue;
						if(!more_point[k]){
							for(@SuppressWarnings("unused")
							final Signal signal : wi.signals)
								out.write("                                   ");
							continue;
						}
						g_more_point = true;
						int j = 0;
						final double xmax = wave.getWaveformMetrics().xmax;
						final double xmin = wave.getWaveformMetrics().xmin;
						more_point[k] = false;
						for(int i = 0; i < wi.signals.length; i++){
							s1 = "";
							s2 = "";
							if(wi.signals[i] != null && wi.signals[i].hasX()) for(j = start_idx[k][i]; j < wi.signals[i].getNumPoints(); j++)
								if(wi.signals[i].getX(j) > xmin && wi.signals[i].getX(j) < xmax){
									more_point[k] = true;
									s1 = "" + wi.signals[i].getX(j);
									s2 = "" + wi.signals[i].getY(j);
									start_idx[k][i] = j + 1;
									break;
								}
							out.write(s1);
							space.setLength(0);
							for(int u = 0; u < 25 - s1.length(); u++)
								space.append(' ');
							space.append(' ');
							out.write(space.toString());
							out.write(" ");
							out.write(s2);
							space.setLength(0);
							for(int u = 0; u < 25 - s2.length(); u++)
								space.append(' ');
							out.write(space.toString());
						}
					}
					out.newLine();
				}
				out.close();
			}catch(final IOException e){
				e.getStackTrace();
			}
		}
		file_diag = null;
	}

	public void setCacheState(final boolean state) {
		jScopeMultiWave w;
		for(int i = 0; i < this.getComponentNumber(); i++){
			w = (jScopeMultiWave)this.getGridComponent(i);
			if(w != null && w.wi != null){
				w.wi.enableCache(state);
				w.wi.setModified(true);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void setDataServer(DataServerItem server_item, final UpdateEventListener l) throws Exception {
		DataProvider new_dp = null;
		if(server_item == null || server_item.name.trim().length() == 0) throw(new Exception("Defined null or empty data server name"));
		if(DEBUG.D) System.out.println("server_item.name " + server_item.name + ", server_item.class_name " + server_item.class_name);
		if(server_item.class_name != null) try{
			final Class cl = Class.forName(server_item.class_name);
			new_dp = (DataProvider)cl.newInstance();
		}catch(final NoClassDefFoundError e){
			throw(new Exception("Can't load data provider class : " + server_item.class_name + "\n" + e));
		}
		else throw(new Exception("Undefined data provider class for " + server_item.name));
		if(this.signalbrowser != null && this.signalbrowser.isShowing()){
			this.signalbrowser.dispose();
			this.signalbrowser = null;
		}
		try{
			// Current data server Disconnection
			this.removeAllEvents(l);
			this.setDataProvider(new_dp);
			final int option = this.dp.setArguments(this.getFrameParent(), server_item);
			switch(option){
				case DataProvider.LOGIN_ERROR:
				case DataProvider.LOGIN_CANCEL:
					server_item = new DataServerItem("Not Connected", null, null, NotConnectedDataProvider.class.getName(), null, null);
					this.setDataProvider(new NotConnectedDataProvider());
			}
			if(!server_item.class_name.equals(NotConnectedDataProvider.class.getName())) // Check data server connection
			    if(!this.dp.checkProvider()) throw(new Exception("Cannot connect to " + server_item.class_name + " data server"));
			// create browse panel if defined
			final Class cl;
			if(server_item.browse_class != null) cl = Class.forName(server_item.browse_class);
			else cl = this.dp.getDefaultBrowser();
			try{
				this.signalbrowserclass = (SignalBrowser)cl.newInstance();
			}catch(final Exception e){
				this.signalbrowserclass = null;
			}
			this.server_item = server_item;
		}catch(final IOException e){
			this.server_item = new DataServerItem("Not Connected", null, null, NotConnectedDataProvider.class.getName(), null, null);
			this.setDataProvider(new NotConnectedDataProvider());
			this.changeDataProvider(this.dp);
			this.addAllEvents(l);
			throw(e);
		}
		this.changeDataProvider(this.dp);
		this.addAllEvents(l);
	}

	public void setEvent(final UpdateEventListener l, final String event) throws IOException {
		this.event = this.addRemoveEvent(l, this.event, event);
	}

	public void setMainShot(final String shot_str) {
		if(shot_str != null) try{
			this.evaluateMainShot(shot_str.trim());
		}catch(final IOException exc){
			this.main_shot_str = null;
			this.main_shot_error = "Main Shots evaluations error : \n" + exc.getMessage();
			JOptionPane.showMessageDialog(this, this.main_shot_error, "alert SetMainShot", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void setModifiedState(final boolean state) {
		jScopeMultiWave w;
		for(int i = 0; i < this.getComponentNumber(); i++){
			w = (jScopeMultiWave)this.getGridComponent(i);
			if(w != null && w.wi != null) w.wi.setModified(state);
		}
	}

	public void setPrintEvent(final UpdateEventListener l, final String print_event) throws IOException {
		this.print_event = this.addRemoveEvent(l, this.print_event, print_event);
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public void showBrowseSignals() {
		if(this.signalbrowserclass != null){
			if(this.signalbrowser == null || !this.signalbrowser.isVisible()){
				this.signalbrowser = new JDialog();
				this.signalbrowser.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}
			final JComponent cont = this.signalbrowserclass.getComponent(this, this.dp);
			cont.setBorder(new BevelBorder(2));
			this.signalbrowser.setContentPane(cont);
			this.signalbrowser.setTitle(this.signalbrowserclass.getTitle());
			this.signalbrowser.setPreferredSize(this.signalbrowserclass.getPreferredSize());
			this.signalbrowser.pack();
			final Point loc = this.getLocationOnScreen();
			loc.x = Math.max(0, loc.x - this.signalbrowser.getWidth());
			loc.y -= (this.signalbrowser.getHeight() - this.getHeight()) / 2;
			this.signalbrowser.setLocation(loc);
			this.signalbrowser.setVisible(true);
		}else JOptionPane.showMessageDialog(this, "Signal browser not yet implemented on this data server", "alert", JOptionPane.ERROR_MESSAGE);
	}

	public void startPrint(final DocPrintJob prnJob, final PrintRequestAttributeSet attrs) {
		try{
			this.getjScopeMultiWave();
			this.updateAllWave();
			this.printAllWaves(prnJob, attrs);
		}catch(final Exception e){/**/}
	}

	public void startUpdate() {
		if(!this.updateThread.isAlive()){
			this.updateThread = new UpdW();
			this.updateThread.start();
		}
		this.getjScopeMultiWave();
		this.updateThread.startUpdate();
	}

	public void startUpdatingPanel() {
		this.progressMonitor = new ProgressMonitor(this, "Running a Long Task", "", 0, 0);
	}

	public void stopUpdatingPanel() {
		this.progressMonitor.close();
	}

	public void toFile(final PrintWriter out, final String prompt) {
		jScopeMultiWave w;
		jScopeWaveInterface wi;
		jScopeWaveContainer.writeLine(out, prompt + "title: ", this.title);
		if(this.server_item != null){
			jScopeWaveContainer.writeLine(out, prompt + "data_server_name: ", this.server_item.name);
			jScopeWaveContainer.writeLine(out, prompt + "data_server_class: ", this.server_item.class_name);
			if(this.server_item.server != null) jScopeWaveContainer.writeLine(out, prompt + "data_server_server: ", this.server_item.server);
			if(this.server_item.user != null) jScopeWaveContainer.writeLine(out, prompt + "data_server_user: ", this.server_item.user);
			if(this.server_item.browse_class != null) jScopeWaveContainer.writeLine(out, prompt + "data_server_browse_class: ", this.server_item.browse_class);
			if(this.server_item.properties != null) for(final Entry<String, String> pair : this.server_item.properties.entrySet())
				jScopeWaveContainer.writeLine(out, prompt + "data_server_" + pair.getKey() + ": ", pair.getValue());
		}
		jScopeWaveContainer.writeLine(out, prompt + "update_event: ", this.event);
		jScopeWaveContainer.writeLine(out, prompt + "print_event: ", this.print_event);
		jScopeWaveContainer.writeLine(out, prompt + "reversed: ", "" + this.reversed);
		out.println();
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.experiment: ", this.def_vals.experiment_str);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.event: ", this.def_vals.upd_event_str);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.default_node: ", this.def_vals.def_node_str);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.shot: ", this.def_vals.shot_str);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.title: ", this.def_vals.title_str);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.xmax: ", this.def_vals.xmax);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.xmin: ", this.def_vals.xmin);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.x_label: ", this.def_vals.xlabel);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.ymax: ", this.def_vals.ymax);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.ymin: ", this.def_vals.ymin);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.y_label: ", this.def_vals.ylabel);
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.horizontal_offset: ", String.valueOf(Waveform.getHorizontalOffset()));
		jScopeWaveContainer.writeLine(out, prompt + "global_1_1.vertical_offset: ", String.valueOf(Waveform.getVerticalOffset()));
		out.println();
		out.println("Scope.columns: " + this.getColumns());
		final float normHeight[] = this.getNormalizedHeight();
		final float normWidth[] = this.getNormalizedWidth();
		final Dimension dim = this.getSize();
		for(int i = 0, c = 1, k = 0; i < this.getColumns(); i++, c++){
			out.println("\n");
			jScopeWaveContainer.writeLine(out, prompt + String.format("rows_in_column_%d: ", Integer.valueOf(c)), String.valueOf(this.getComponentsInColumn(i)));
			for(int j = 0, r = 1; j < this.getComponentsInColumn(i); j++, r++){
				w = (jScopeMultiWave)this.getGridComponent(k);
				wi = (jScopeWaveInterface)w.wi;
				out.println();
				// writeLine(out, prompt + "plot_" + r + "_" + c + ".height: " , ""+w.getSize().height );
				jScopeWaveContainer.writeLine(out, prompt + String.format("plot_%d_%d.height: ", Integer.valueOf(r), Integer.valueOf(c)), String.valueOf((int)(dim.height * normHeight[k])));
				jScopeWaveContainer.writeLine(out, prompt + String.format("plot_%d_%d.grid_mode: ", Integer.valueOf(r), Integer.valueOf(c)), String.valueOf(w.getGridMode()));
				if(wi != null) wi.toFile(out, prompt + String.format("plot_%d_%d.", Integer.valueOf(r), Integer.valueOf(c)));
				k++;
			}
		}
		out.println();
		for(int i = 1, pos = 0; i < this.getColumns(); i++){ // , k = 0
			pos += (int)(normWidth[i - 1] * 1000.);
			jScopeWaveContainer.writeLine(out, prompt + "vpane_" + i + ": ", "" + pos);
		}
	}

	public final void updateAllWave() throws Exception {// synchronized
		WaveContainerEvent wce;
		try{
			this.dp.reset();
			if(this.wave_all == null) this.abort = true;
			else this.abort = false;
			if(this.def_vals != null && !this.def_vals.getIsEvaluated()){
				this.dp.setEnvironment(this.def_vals.getPublicVariables());
				this.def_vals.setIsEvaluated(true);
			}
			for(int i = 0, k = 0; i < RowColumnLayout.MAX_COLUMN && !this.abort; i++)
				for(int j = 0; j < this.rows[i]; j++, k++)
					if(this.wave_all[k].wi != null && this.wave_all[k].isWaveformVisible()) ((jScopeWaveInterface)this.wave_all[k].wi).update();
			// Initialize wave evaluation
			for(int i = 0, k = 0; i < RowColumnLayout.MAX_COLUMN && !this.abort; i++)
				for(int j = 0; j < this.rows[i] && !this.abort; j++, k++)
					if(this.wave_all[k].wi != null && this.wave_all[k].wi.error == null && this.wave_all[k].isWaveformVisible()){
						wce = new WaveContainerEvent(this, WaveContainerEvent.START_UPDATE, "Start Evaluate column " + (i + 1) + " row " + (j + 1));
						try{
							this.dispatchWaveContainerEvent(wce);
							((jScopeWaveInterface)this.wave_all[k].wi).startEvaluate();
						}catch(final Exception exc){
							exc.printStackTrace();
						}
					}
			synchronized(this.mainShotLock){
				if(this.main_shots != null) for(int l = 0; l < this.main_shots.length && !this.abort; l++)
					for(int i = 0, k = 0; i < RowColumnLayout.MAX_COLUMN && !this.abort; i++)
						for(int j = 0; j < this.rows[i] && !this.abort; j++, k++)
							if(this.wave_all[k].wi != null && this.wave_all[k].wi.error == null && this.wave_all[k].isWaveformVisible() && this.wave_all[k].wi.num_waves != 0 && ((jScopeWaveInterface)this.wave_all[k].wi).useDefaultShot()){
								wce = new WaveContainerEvent(this, WaveContainerEvent.START_UPDATE, "Update signal column " + (i + 1) + " row " + (j + 1) + " main shot " + this.main_shots[l]);
								this.dispatchWaveContainerEvent(wce);
								((jScopeWaveInterface)this.wave_all[k].wi).evaluateShot(this.main_shots[l], this);
								if(((jScopeWaveInterface)this.wave_all[k].wi).allEvaluated()) if(this.wave_all[k].wi != null) this.wave_all[k].update(this.wave_all[k].wi);
							}
			}
			// Evaluate evaluate other shot
			if(this.wave_all != null) for(int i = 0, k = 0; i < RowColumnLayout.MAX_COLUMN && k < this.wave_all.length && !this.abort; i++)
				for(int j = 0; j < this.rows[i] && k < this.wave_all.length && !this.abort; j++, k++)
					if(this.wave_all[k] != null && this.wave_all[k].isWaveformVisible()){
						if(this.wave_all[k].wi.error == null && this.wave_all[k].wi.num_waves != 0){
							if(((jScopeWaveInterface)this.wave_all[k].wi).allEvaluated()) continue;
							wce = new WaveContainerEvent(this, WaveContainerEvent.START_UPDATE, "Evaluate wave column " + (i + 1) + " row " + (j + 1));
							this.dispatchWaveContainerEvent(wce);
							((jScopeWaveInterface)this.wave_all[k].wi).evaluateOthers();
						}
						this.wave_all[k].update(this.wave_all[k].wi);
					}
			for(int i = 0, k = 0; i < RowColumnLayout.MAX_COLUMN; i++)
				for(int j = 0; j < this.rows[i]; j++, k++)
					if(this.wave_all != null && this.wave_all[k] != null && this.wave_all[k].wi != null) ((jScopeWaveInterface)this.wave_all[k].wi).allEvaluated();
			this.wave_all = null;
		}catch(final Exception exc){
			exc.printStackTrace();
			this.repaintAllWave();
		}
	}

	public void updateHeight() {
		float height = 0;
		jScopeMultiWave w;
		this.ph = new float[this.getComponentNumber()];
		for(int j = 0, k = 0; j < this.getColumns(); j++){
			height = 0;
			for(int i = 0; i < this.rows[j]; i++){
				w = (jScopeMultiWave)this.getGridComponent(k + i);
				height += w.wi.height;
			}
			for(int i = 0; i < this.rows[j]; i++, k++){
				w = (jScopeMultiWave)this.getGridComponent(k);
				if(height == 0){
					k -= i;
					for(i = 0; i < this.rows[j]; i++)
						this.ph[k++] = (float)1. / this.rows[j];
					break;
				}
				this.ph[k] = (w.wi.height / height);
			}
		}
		this.invalidate();
	}

	@Override
	protected MultiWaveform createWaveComponent() {
		final jScopeMultiWave wave = jScopeWaveContainer.buildjScopeMultiWave(this.dp, this.def_vals);
		wave.addWaveformListener(this);
		this.setWaveParams(wave);
		return wave;
	}

	private String addRemoveEvent(final UpdateEventListener l, final String curr_event, final String event_in) throws IOException {
		if(curr_event != null && curr_event.length() != 0){
			if(event_in == null || event_in.length() == 0){
				this.dp.removeUpdateEventListener(l, curr_event);
				return null;
			}
			if(!curr_event.equals(event_in)){
				this.dp.removeUpdateEventListener(l, curr_event);
				this.dp.addUpdateEventListener(l, event_in);
			}
			return event_in;
		}
		if(event_in != null && event_in.length() != 0) this.dp.addUpdateEventListener(l, event_in);
		return event_in;
	}

	/*
	private static boolean IsIpAddress(final String addr) {
	    return(addr.trim().indexOf(".") != -1 && addr.trim().indexOf(" ") == -1);
	}
	 */
	private JFrame getFrameParent() {
		Container c = this;
		while(c != null && !(c instanceof JFrame))
			c = c.getParent();
		return (JFrame)c;
	}

	private void repaintAllWave() {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				jScopeMultiWave wx;
				for(int i = 0, k = 0; i < jScopeWaveContainer.this.rows.length; i++)
					for(int j = 0; j < jScopeWaveContainer.this.rows[i]; j++, k++){
						wx = (jScopeMultiWave)jScopeWaveContainer.this.getGridComponent(k);
						if(wx.wi != null) wx.update(wx.wi);
					}
			}
		});
	}

	private void setDataProvider(final DataProvider new_dp) {
		if(this.dp != null) this.dp.dispose();
		this.dp = new_dp;
	}
}
