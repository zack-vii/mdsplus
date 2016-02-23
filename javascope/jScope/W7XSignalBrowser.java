package jScope;

import jScope.W7XDataProvider.signalaccess;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTree;
import javax.swing.SpinnerDateModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.calendar.SingleDaySelectionModel;
import de.mpg.ipp.codac.signalaccess.SignalAddress;
import de.mpg.ipp.codac.w7xtime.TimeInterval;

public final class W7XSignalBrowser extends jScopeBrowseSignals{
    static final class DateTimePicker extends JXDatePicker{
        public static final DateFormat  format           = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSS");
        private static final long       serialVersionUID = 77777773L;
        private static final DateFormat timeFormat       = new SimpleDateFormat("HH:mm:ss.SSS");
        public static final TimeZone    UTC              = TimeZone.getTimeZone("UTC");
        private JPanel                  timePanel;
        private JSpinner                timeSpinner;

        public DateTimePicker(final Date date, final int mm, final int ss, final int SSS){
            super();
            this.getMonthView().setSelectionModel(new SingleDaySelectionModel());
            this.setTimeZone(DateTimePicker.UTC);
            DateTimePicker.format.setTimeZone(DateTimePicker.UTC);
            this.setFormats(DateTimePicker.format);
            this.updateTextFieldFormat();
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeZone(DateTimePicker.UTC);
            calendar.setTime(date);
            calendar.set(Calendar.MINUTE, mm);
            calendar.set(Calendar.SECOND, ss);
            calendar.set(Calendar.MILLISECOND, SSS);
            this.setDate(calendar.getTime());
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            this.setTimeSpinners();
        }

        @Override
        public void commitEdit() throws ParseException {
            this.commitTime();
            super.commitEdit();
        }

        private void commitTime() {
            final Date date = this.getDate();
            if(date == null) return;
            final GregorianCalendar timeCalendar = new GregorianCalendar();
            DateTimePicker.format.setTimeZone(DateTimePicker.UTC);
            timeCalendar.setTime((Date)this.timeSpinner.getValue());
            final GregorianCalendar calendar = new GregorianCalendar();
            DateTimePicker.format.setTimeZone(DateTimePicker.UTC);
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, timeCalendar.get(Calendar.MILLISECOND));
            this.setDate(calendar.getTime());
        }

        private JPanel createTimePanel() {
            final JPanel newPanel = new JPanel();
            newPanel.setLayout(new FlowLayout());
            final SpinnerDateModel dateModel = new SpinnerDateModel();
            this.timeSpinner = new JSpinner(dateModel);
            DateTimePicker.timeFormat.setTimeZone(DateTimePicker.UTC);
            this.updateTextFieldFormat();
            newPanel.add(new JLabel("Time:"));
            newPanel.add(this.timeSpinner);
            this.timeSpinner.addChangeListener(e -> {this.commitTime();});
            newPanel.setBackground(Color.WHITE);
            return newPanel;
        }

        @Override
        public JPanel getLinkPanel() {
            super.getLinkPanel();
            if(this.timePanel == null) this.timePanel = this.createTimePanel();
            this.setTimeSpinners();
            return this.timePanel;
        }

        private void setTimeSpinners() {
            final Date date = this.getDate();
            if(date != null) this.timeSpinner.setValue(date);
        }

        private void updateTextFieldFormat() {
            if(this.timeSpinner == null) return;
            final JFormattedTextField tf = ((JSpinner.DefaultEditor)this.timeSpinner.getEditor()).getTextField();
            final DefaultFormatterFactory factory = (DefaultFormatterFactory)tf.getFormatterFactory();
            final DateFormatter formatter = (DateFormatter)factory.getDefaultFormatter();
            formatter.setFormat(DateTimePicker.timeFormat);
        }
    }
    public final class W7XDataBase extends W7XNode{
        private static final long                 serialVersionUID = 77777772L;
        private final String                      name;
        public final W7XDataProvider.signalaccess sa;

        public W7XDataBase(final String name, final W7XDataProvider.signalaccess sa){
            super(sa.sab.newBuilder().path("").build());
            this.name = name;
            this.sa = sa;
        }

        @Override
        public final String toString() {
            return this.name;
        }
    }
    public class W7XNode extends DefaultMutableTreeNode{
        private static final long serialVersionUID = 77777771L;
        private boolean           loaded           = false;

        public W7XNode(final SignalAddress userObject){
            super(userObject);
            this.setAllowsChildren(true);
        }

        public void addSignal() {
            if(W7XSignalBrowser.this.wave_panel == null) return;
            final TreeNode[] path = this.getPath();
            if(path == null || path.length < 2) return;
            final String sig_path = "/" + ((W7XDataBase)path[1]).name + this.getSignalPath();
            if(sig_path != null) W7XSignalBrowser.this.wave_panel.AddSignal(null, null, "", sig_path, true, W7XSignalBrowser.this.is_image);
        }

        @SuppressWarnings("unchecked")
        public final List<SignalAddress> getChildren() {
            final TreeNode[] path = this.getPath();
            if(path == null || path.length < 2) return null;
            final TimeInterval ti = TimeInterval.ALL.withStart(W7XSignalBrowser.this.from.getDate().getTime() * 1000000L).withEnd(W7XSignalBrowser.this.upto.getDate().getTime() * 1000000L);
            return (List<SignalAddress>)((W7XDataBase)path[1]).sa.stl.listFor(ti, this.getSignalPath());
        }

        private String getSignalPath() {
            return ((SignalAddress)this.getUserObject()).toString();
        }

        @Override
        public boolean isLeaf() {
            return this.loaded && !this.getAllowsChildren();
        }

        private void loadChildren() {
            this.setChildren(this.getChildren());
        }

        private void setChildren(final List<SignalAddress> children) {
            if(children == null) return;
            this.removeAllChildren();
            this.setAllowsChildren(children.size() > 0);
            for(final SignalAddress node : children)
                this.add(new W7XNode(node));
            this.loaded = true;
        }

        @Override
        public String toString() {
            return ((SignalAddress)this.getUserObject()).tail();
        }
    }
    private static final long serialVersionUID = 77777770L;

    public static void main(final String[] args) {
        EventQueue.invokeLater(() -> {
            try{
                final W7XSignalBrowser frame = new W7XSignalBrowser();
                frame.setVisible(true);
            }catch(final Exception e){
                e.printStackTrace();
            }
        });
    }
    private final JPanel                contentPane;
    private final DateTimePicker        from, upto;
    public boolean                      is_image = false;
    private String                      server_url;
    private String                      shot;
    public final DefaultMutableTreeNode top;
    private String                      tree;
    public jScopeWaveContainer          wave_panel;

    /**
     * Create the frame.
     */
    public W7XSignalBrowser(){
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.contentPane = new JPanel();
        this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.contentPane.setLayout(new BorderLayout(0, 0));
        this.setContentPane(this.contentPane);
        this.setPreferredSize(new Dimension(424, 550));
        JPanel jp, ejp;
        JButton but;
        final JCheckBox cb;
        final GridLayout grid = new GridLayout(2, 1);
        grid.setVgap(-10);
        this.contentPane.add(ejp = new JPanel(grid), BorderLayout.NORTH);
        ejp.add(jp = new JPanel());
        jp.add(but = new JButton("setTime"));
        but.addActionListener(e -> {
            W7XDataProvider.setTiming(W7XSignalBrowser.this.from.getDate().getTime(), W7XSignalBrowser.this.upto.getDate().getTime());
            jScopeFacade.instance.UpdateAllWaves();
        });
        jp.add(but = new JButton("clearTime"));
        but.addActionListener(e -> {
            W7XDataProvider.setTiming();
        });
        jp.add(cb = new JCheckBox("asImage"));
        cb.addActionListener(e -> {
            this.is_image = cb.isSelected();
        });
        ejp.add(ejp = new JPanel());
        ejp.add(jp = new JPanel());
        final Date date = new Date();
        jp.add(this.from = new DateTimePicker(date, 0, 0, 0));
        jp.add(this.upto = new DateTimePicker(date, 59, 59, 999));
        this.top = new DefaultMutableTreeNode("DataBase");
        final JTree tree = new JTree(this.top);
        this.contentPane.add(new JScrollPane(tree));
        tree.setRootVisible(true);
        final W7XDataBase db = new W7XDataBase("ArchiveDB", signalaccess.arch);
        this.top.add(db);
        this.top.add(new W7XDataBase("Test", signalaccess.test));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.addTreeWillExpandListener(new TreeWillExpandListener(){
            @Override
            public void treeWillCollapse(final TreeExpansionEvent event) throws ExpandVetoException {}

            @Override
            public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
                final TreePath path = event.getPath();
                if(path.getLastPathComponent() instanceof W7XNode){
                    final W7XNode node = (W7XNode)path.getLastPathComponent();
                    if(!node.loaded) node.loadChildren();
                }
            }
        });
        tree.setToggleClickCount(0);
        tree.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(final MouseEvent e) {
                final int selRow = tree.getRowForLocation(e.getX(), e.getY());
                final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if(path == null) return;
                final W7XNode node = (W7XNode)path.getLastPathComponent();
                if(selRow != -1) if(e.getClickCount() == 1) tree.expandRow(selRow);
                else if(e.getClickCount() == 2 && node.isLeaf()) node.addSignal();
            }
        });
        tree.expandPath(new TreePath(this.top));
        this.pack();
    }

    @Override
    protected String getServerAddr() {
        return this.server_url;
    }

    @Override
    protected String getShot() {
        return this.shot;
    }

    @Override
    protected String getSignal(final String url_name) {
        return null;
    }

    @Override
    protected String getTree() {
        return this.tree;
    }

    @Override
    public void setWaveContainer(final jScopeWaveContainer wave_panel) {
        this.wave_panel = wave_panel;
    }
}