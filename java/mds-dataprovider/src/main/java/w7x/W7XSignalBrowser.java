package w7x;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTree;
import javax.swing.SpinnerDateModel;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.calendar.SingleDaySelectionModel;
import de.mpg.ipp.codac.signalaccess.SignalAddress;
import de.mpg.ipp.codac.w7xtime.TimeInterval;
import de.mpg.ipp.codac.w7xtime.TimeIntervals;
import jscope.SignalBrowser;
import jscope.jScopeFacade;
import jscope.jScopeWaveContainer;
import jscope.data.DataProvider;
import jscope.waveform.Grid;
import mds.MdsSignalBrowser;

public final class W7XSignalBrowser extends MdsSignalBrowser{
	public class CalendarEdit extends JPanel{
		private static final long serialVersionUID = 1L;
		private final class DateTimePicker extends JXDatePicker{
			private static final long	serialVersionUID	= 1L;
			private JSpinner			timeSpinner;

			public DateTimePicker(){
				super();
				this.updateTextFieldFormat();
				SingleDaySelectionModel sdsm = new SingleDaySelectionModel();
				this.getMonthView().setSelectionModel(sdsm);
				this.setTimeZone(W7XSignalBrowser.UTC);
				this.setFormats(W7XSignalBrowser.format);
				super.setLinkPanel(this.createTimePanel());
			}

			private final Calendar getCalendar() {
				return this.getMonthView().getSelectionModel().getCalendar();
			}

			@Override
			public void cancelEdit() {
				super.cancelEdit();
				this.setDate(getDate());
			}

			@Override
			public void commitEdit() throws ParseException {
				super.commitEdit();
				this.commitTime();
			}

			private void commitTime() {
				final Calendar timeCalendar = Calendar.getInstance(W7XSignalBrowser.UTC);
				timeCalendar.setTimeInMillis(((Date)this.timeSpinner.getValue()).getTime() % Grid.dayMilliSeconds);
				final Calendar calendar = this.getCalendar();
				calendar.setTime(getDate());
				for(final int entry : W7XSignalBrowser.time)
					calendar.set(entry, timeCalendar.get(entry));
				super.setDate(calendar.getTime());
			}

			public final JPanel getLinkPanel() {
				final Date date = getDate();
				if(date instanceof Date) this.timeSpinner.setValue(date);
				return super.getLinkPanel();
			}

			private JPanel createTimePanel() {
				final JPanel newPanel = new JPanel();
				newPanel.setLayout(new FlowLayout());
				final SpinnerDateModel dateModel = new SpinnerDateModel(new Date(0), null, null, Calendar.HOUR);
				this.timeSpinner = new JSpinner(dateModel);
				this.updateTextFieldFormat();
				newPanel.add(new JLabel("Time:"));
				newPanel.add(this.timeSpinner);
				this.timeSpinner.addChangeListener(new ChangeListener(){
					@Override
					public void stateChanged(final ChangeEvent e) {
						DateTimePicker.this.commitTime();
					}
				});
				newPanel.setBackground(Color.WHITE);
				return newPanel;
			}

			private void updateTextFieldFormat() {
				if(this.timeSpinner == null) return;
				final JFormattedTextField tf = ((JSpinner.DefaultEditor)this.timeSpinner.getEditor()).getTextField();
				final DefaultFormatterFactory factory = (DefaultFormatterFactory)tf.getFormatterFactory();
				final DateFormatter formatter = (DateFormatter)factory.getDefaultFormatter();
				W7XSignalBrowser.timeFormat.getCalendar().setTimeZone(UTC);
				formatter.setFormat(W7XSignalBrowser.timeFormat);
			}
		}
		private final JXMonthView		jxmv;
		private final DateTimePicker	from, upto;

		public CalendarEdit(){
			this(W7XSignalBrowser.UTC);
		}

		public CalendarEdit(final TimeZone tz){
			super(new BorderLayout(3, 3));
			this.jxmv = new JXMonthView();
			this.jxmv.setBorder(BorderFactory.createLoweredBevelBorder());
			this.jxmv.setTimeZone(tz);
			this.from = new DateTimePicker();
			this.upto = new DateTimePicker();
			this.jxmv.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					CalendarEdit.this.setDay(CalendarEdit.this.jxmv.getSelectionDate());
				}
			});
			JButton jb;
			JPanel cal_bar, cal_bar_east, cal_bar_west, calendar, timepick;
			this.add(calendar = new JPanel(new BorderLayout()), BorderLayout.CENTER);
			this.add(timepick = new JPanel(new GridLayout(7, 1)), BorderLayout.EAST);
			calendar.add(this.jxmv, BorderLayout.CENTER);
			calendar.setBorder(BorderFactory.createLoweredBevelBorder());
			calendar.add(cal_bar = new JPanel(new BorderLayout()), BorderLayout.NORTH);
			cal_bar.add(cal_bar_west = new JPanel(new GridLayout(1, 2)), BorderLayout.WEST);
			cal_bar.add(cal_bar_east = new JPanel(new GridLayout(1, 2)), BorderLayout.EAST);
			cal_bar_west.add(jb = new JButton("<<"));
			jb.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					CalendarEdit.this.modCalendar(Calendar.YEAR, -1);
				}
			});
			cal_bar_west.add(jb = new JButton("<"));
			jb.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					CalendarEdit.this.modCalendar(Calendar.MONTH, -1);
				}
			});
			cal_bar.add(jb = new JButton(), BorderLayout.CENTER);
			jb.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					CalendarEdit.this.setToday();
				}
			});
			cal_bar_east.add(jb = new JButton(">"));
			jb.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					CalendarEdit.this.modCalendar(Calendar.MONTH, 1);
				}
			});
			cal_bar_east.add(jb = new JButton(">>"));
			jb.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					CalendarEdit.this.modCalendar(Calendar.YEAR, 1);
				}
			});
			final JButton settime_b = new JButton("set Time");
			timepick.add(settime_b);
			settime_b.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					CalendarEdit.this.setDPTiming();
				}
			});
			final JButton cleartime_b = new JButton("clear Time");
			timepick.add(cleartime_b);
			cleartime_b.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					W7XSignalBrowser.this.wdp.setTiming();
				}
			});
			final JCheckBox asimage = new JCheckBox("as image");
			timepick.add(this.from);
			timepick.add(this.upto);
			timepick.add(settime_b);
			timepick.add(cleartime_b);
			timepick.add(asimage);
			asimage.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
					W7XSignalBrowser.this.as_image = ((JCheckBox)e.getSource()).isSelected();
				}
			});
		}

		private final void setDay(Date date) {
			this.from.setDate(date);
			date.setTime(date.getTime() + 24 * 60 * 60 * 1000L - 1L);
			this.upto.setDate(date);
		}

		public final long getDate() {
			final Calendar cal = this.jxmv.getCalendar();
			for(final int entry : W7XSignalBrowser.time)
				cal.set(entry, 0);
			return cal.getTimeInMillis() * 1000_000L;
		}

		public final long getTime() {
			final Calendar cal = this.jxmv.getCalendar();
			return cal.getTimeInMillis() * 1000_000L;
		}

		public final void setCalendar(final Calendar cal, final boolean setselection) {
			final Date date = cal.getTime();
			if(!this.jxmv.isSelectionEmpty() || setselection) this.jxmv.setSelectionDate(date);
			this.jxmv.setFirstDisplayedDay(date);
			this.jxmv.updateUI();
		}

		public void setToday() {
			this.setDay(this.getToday().getTime());
		}

		private Calendar getToday() {
			final Calendar cal = Calendar.getInstance(CalendarEdit.this.jxmv.getTimeZone());
			cal.setTime(CalendarEdit.this.jxmv.getToday());
			CalendarEdit.this.setCalendar(cal, true);
			return cal;
		}

		protected void setDPTiming() {
			W7XSignalBrowser.this.wdp.setTiming(CalendarEdit.this.from.getDate().getTime() * 1000_000L, CalendarEdit.this.upto.getDate().getTime() * 1000_000L);
			if(jScopeFacade.instance != null) jScopeFacade.instance.updateAllWaves();
		}

		protected void setTiming(final long from, final long upto) {
			final Date date = new Date(from / 1000_000L);
			CalendarEdit.this.from.setDate(date);
			CalendarEdit.this.upto.setDate(new Date((upto + 999_999L) / 1000_000L));
			this.jxmv.setSelectionDate(date);
			this.jxmv.setFirstDisplayedDay(date);
			this.jxmv.updateUI();
		}

		private final Calendar getCalendar() {
			final Date date = this.jxmv.isSelectionEmpty() ? this.jxmv.getFirstDisplayedDay() : this.jxmv.getSelectionDate();
			final Calendar cal = Calendar.getInstance(this.jxmv.getTimeZone());
			cal.setTime(date);
			return cal;
		}

		private final void modCalendar(final int mode, final int increment) {
			final Calendar cal = CalendarEdit.this.getCalendar();
			cal.set(mode, cal.get(mode) + increment);
			CalendarEdit.this.setCalendar(cal, false);
		}
	}
	public final class W7XDataBase extends w7xNode{
		private static final long		serialVersionUID	= 1L;
		private final String			name;
		public final W7XSignalAccess	sa;

		public W7XDataBase(final String name, final W7XSignalAccess sa){
			super(sa.getAddress(""));
			this.name = name;
			this.sa = sa;
		}

		@Override
		public final String toString() {
			return this.name;
		}
	}
	public class w7xNode extends DefaultMutableTreeNode{
		private static final long	serialVersionUID	= 1L;
		private boolean				loaded				= false;

		public w7xNode(final SignalAddress userObject){
			super(userObject);
			this.setAllowsChildren(!this.isLeaf());
		}

		public final List<SignalAddress> getChildren() {
			final TreeNode[] path = this.getPath();
			if(path == null || path.length < 2) return null;
			TimeInterval interval = W7XSignalBrowser.this.getTimeInterval();
			if(interval.isNull()) interval = TimeIntervals.today();
			final String sigpath = this.getSignalPath();
			final String database = this.getDataBase().name;
			return W7XSignalAccess.getList(database, sigpath, interval);
		}

		@Override
		public boolean isLeaf() {
			return this.getSignalAddress().complete();
		}

		@Override
		public String toString() {
			return ((SignalAddress)this.getUserObject()).tail();
		}

		private W7XDataBase getDataBase() {
			MutableTreeNode db = this;
			while(!(db instanceof W7XDataBase || db == null))
				db = ((w7xNode)db).parent;
			return((W7XDataBase)db);
		}

		private SignalAddress getSignalAddress() {
			return (SignalAddress)this.getUserObject();
		}

		private String getSignalPath() {
			return this.getSignalAddress().address().replaceFirst("/No_Box$", "");
		}

		private void loadChildren() {
			this.setChildren(this.getChildren());
		}

		private void setChildren(final List<SignalAddress> children) {
			if(children == null) return;
			this.removeAllChildren();
			for(final SignalAddress node : children)
				if(!node.tail().equals("No_Box")) this.add(new w7xNode(node));
			this.loaded = true;
		}
	}
	private final class FromTransferHandler extends TransferHandler{
		private static final long serialVersionUID = 1L;

		@Override
		public final Transferable createTransferable(final JComponent comp) {
			try{
				final w7xNode node = (w7xNode)((JTree)comp).getLastSelectedPathComponent();
				final StringBuilder sb = new StringBuilder();
				if(W7XSignalBrowser.this.as_image) sb.append("IMAGE#");
				sb.append("\\W7X::TOP./");
				sb.append(node.getDataBase()); // add database e.g. "Test"
				sb.append(node.getSignalPath()); // add path e.g. "/raw/"
				return new StringSelection(sb.toString());
			}catch(final Exception exc){
				return null;
			}
		}

		@Override
		public final int getSourceActions(final JComponent comp) {
			final Point ml = MouseInfo.getPointerInfo().getLocation();
			final Point tl = ((JTree)comp).getLocationOnScreen();
			final int selRow = ((JTree)comp).getRowForLocation(ml.x - tl.x, ml.y - tl.y);
			if(selRow < 0) return TransferHandler.NONE;
			final w7xNode node = (w7xNode)(((JTree)comp).getPathForRow(selRow)).getLastPathComponent();
			return node != null && node.isLeaf() ? TransferHandler.COPY_OR_MOVE : TransferHandler.NONE;
		}
	}
	public static final DateFormat	format		= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static final DateFormat	timeFormat	= new SimpleDateFormat("HH:mm:ss.SSS");
	static{
		W7XSignalBrowser.timeFormat.setTimeZone(W7XSignalBrowser.UTC);
		W7XSignalBrowser.format.setTimeZone(W7XSignalBrowser.UTC);
	}
	public static final TimeZone	UTC		= TimeZone.getTimeZone("UTC");
	private static final int[]		time	= new int[]{Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND};

	public static void main(final String... args) {
		final JFrame signalbrowser = new JFrame();
		signalbrowser.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final SignalBrowser sb = new W7XSignalBrowser();
		final DataProvider dp = new W7XDataProvider();
		final JComponent cont = sb.getComponent(null, dp);
		cont.setBorder(new BevelBorder(2));
		signalbrowser.setContentPane(cont);
		signalbrowser.setTitle(sb.getTitle());
		signalbrowser.setPreferredSize(sb.getPreferredSize());
		signalbrowser.pack();
		signalbrowser.setVisible(true);
	}
	private W7XDataProvider	wdp;
	protected boolean		as_image	= false;

	/**
	 * Create the frame.
	 */
	public W7XSignalBrowser(){
		super();
	}

	@Override
	public JComponent getComponent(final jScopeWaveContainer wc, final DataProvider dp) {
		this.wdp = (W7XDataProvider)dp;
		final JPanel panel, trees;
		final DefaultMutableTreeNode top;
		panel = new JPanel(new BorderLayout(3, 3));
		final CalendarEdit calendarEdit = new CalendarEdit();
		panel.add(calendarEdit, BorderLayout.PAGE_START);
		final JTree tree = new JTree(top = new DefaultMutableTreeNode("DataBase"));
		final JComponent mds = super.getComponent(wc, this.wdp);
		if(mds == null) panel.add(new JScrollPane(tree));
		else{
			panel.add(trees = new JPanel(new GridLayout(1, 2)));
			trees.add(new JScrollPane(tree));
			trees.add(mds);
		}
		for(final String db : W7XSignalAccess.getDataBaseList()){
			final W7XSignalAccess sa = W7XSignalAccess.getAccess(db);
			if(sa == null) break;
			top.add(new W7XDataBase(db, sa));
		}
		tree.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		tree.setTransferHandler(new FromTransferHandler());
		tree.setDragEnabled(true);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.addTreeWillExpandListener(new TreeWillExpandListener(){
			@Override
			public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException {/*stub*/}

			@Override
			public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
				final TreePath path = event.getPath();
				if(path.getLastPathComponent() instanceof w7xNode){
					final w7xNode node = (w7xNode)path.getLastPathComponent();
					if(!node.loaded) node.loadChildren();
				}
			}
		});
		tree.setToggleClickCount(0);
		tree.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseReleased(final MouseEvent e) {
				if(e.getClickCount() != 1) return;
				final int selRow = tree.getRowForLocation(e.getX(), e.getY());
				if(selRow < 0) return;
				final TreePath path = tree.getPathForRow(selRow);
				if(((w7xNode)path.getLastPathComponent()).loaded) tree.expandRow(selRow);
				else new Thread(){
					@Override
					public final void run() {
						tree.expandRow(selRow);
					}
				}.start();
			}
		});
		final long[] t = this.wdp.getTiming();
		if(t == null || t[0] == 0) calendarEdit.setToday();
		else calendarEdit.setTiming(t[0], t[1]);
		tree.expandPath(new TreePath(top));
		return panel;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(440, 600);
	}

	public final TimeInterval getTimeInterval() {
		final long[] t = this.wdp.getTiming();
		if(t == null) return TimeInterval.NULL;
		return TimeInterval.with(t[0], t[1]);
	}
}