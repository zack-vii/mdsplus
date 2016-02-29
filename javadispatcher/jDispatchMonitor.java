import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import jScope.ConnectionEvent;
import jScope.ConnectionListener;
import jScope.Descriptor;
import mds.mdsConnection;
import mds.mdsMessage;

public class jDispatchMonitor extends JFrame implements MdsServerListener, ConnectionListener{
    class BuildCellRenderer extends JLabel implements ListCellRenderer{
        private static final long serialVersionUID = -8012327117859005660L;

        public BuildCellRenderer(){
            this.setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            // Setting Font
            // for a JList item in a renderer does not work.
            this.setFont(jDispatchMonitor.done_font);
            final MdsMonitorEvent me = (MdsMonitorEvent)value;
            this.setText(me.getMonitorString());
            if(me.on == 0) this.setForeground(Color.lightGray);
            else this.setForeground(Color.black);
            if(isSelected){
                this.setBackground(list.getSelectionBackground());
            }else{
                this.setBackground(list.getBackground());
            }
            return this;
        }
    }
    // End BuildCellRenderer class
    public class ErrorMgr extends MdsIp{
        // The constructor
        public ErrorMgr(final int port){
            super(port);
        }

        // The message handler: the expression and arguments are passed as a vector of mdsMessage
        public mdsMessage handleMessage(final mdsMessage[] messages) {
            int nids[] = null;
            int nid = 0;
            String message = null;
            // method mdsMessage.asString returns the argument as a string
            // in this as messages[0] contains the expression to be evaluated
            // the other elements of messages contain additional arguments
            // see the definition of mdsMessage (or ask me)
            // if you need to handle other types of arguments
            try{
                nids = messages[1].asIntArray();
                nid = nids[0];
                message = messages[2].asString();
            }catch(final Exception exc){
                System.out.println("Error receiving error message : " + exc);
                return new mdsMessage((byte)1);
            }
            if(nids == null || message == null) return new mdsMessage((byte)1);
            Hashtable<Integer, MdsMonitorEvent> actions = null;
            if(jDispatchMonitor.this.phase_failed.containsKey(new Integer(jDispatchMonitor.this.curr_phase))) actions = jDispatchMonitor.this.phase_failed.get(new Integer(jDispatchMonitor.this.curr_phase));
            else{
                actions = new Hashtable<Integer, MdsMonitorEvent>();
                jDispatchMonitor.this.phase_failed.put(new Integer(jDispatchMonitor.this.curr_phase), actions);
            }
            if(actions == null) return new mdsMessage((byte)1);
            if(!actions.containsKey(new Integer(nids[0]))){
                final MdsMonitorEvent me = new MdsMonitorEvent(this, jDispatchMonitor.this.curr_phase, nids[0], message);
                actions.put(new Integer(nids[0]), me);
                // System.out.println("Add error on "+nids[0] + " " + message);
            }
            // In this example the mdsip returns a string value
            return new mdsMessage((byte)1);
            // if a integer number has to be returned instead,
            // you can type return new mdsMessage((Byte)<number>)
        }
    }// End ErrorMgr class
    class ExecutingCellRenderer extends JLabel implements ListCellRenderer{
        private static final long serialVersionUID = 547975834097141593L;

        public ExecutingCellRenderer(){
            this.setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            // Setting Font
            // for a JList item in a renderer does not work.
            if(value == null) return this;
            this.setFont(jDispatchMonitor.done_font);
            final MdsMonitorEvent me = (MdsMonitorEvent)value;
            if(value == null){
                int i = 0;
                i = 1;
            }
            this.setText(me.getMonitorString());
            if(isSelected){
                this.setBackground(list.getSelectionBackground());
            }else{
                this.setBackground(list.getBackground());
            }
            switch(me.mode){
                case MdsMonitorEvent.MonitorDispatched:
                    this.setForeground(Color.lightGray);
                    // this.setFont(disp_font);
                    break;
                case MdsMonitorEvent.MonitorDoing:
                    this.setForeground(Color.blue);
                    // setFont(doing_font);
                    break;
                case MdsMonitorEvent.MonitorDone:
                    if((me.ret_status & 1) == 1){
                        this.setForeground(Color.green);
                        // setFont(done_font);
                    }else{
                        this.setForeground(Color.red);
                        // setFont(done_failed_font);
                    }
                    break;
            }
            return this;
        }
    }// End ExecutingCellRenderer class
    class FailedCellRenderer extends JLabel implements ListCellRenderer{
        /**
         *
         */
        private static final long serialVersionUID = -5753164789682638497L;

        public FailedCellRenderer(){
            this.setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            // Setting Font
            // for a JList item in a renderer does not work.
            this.setFont(jDispatchMonitor.done_font);
            final MdsMonitorEvent me = (MdsMonitorEvent)value;
            this.setText(me.getMonitorString());
            if(isSelected){
                this.setBackground(list.getSelectionBackground());
            }else{
                this.setBackground(list.getBackground());
            }
            return this;
        }
    }
    // End FailedCellRenderer class
    public class ShotsPerformance{
        Vector<String>                  labelText = new Vector<String>();
        PrintStream                     log;
        Vector<String>                  phaseDuration;
        Hashtable<Long, Vector<String>> shotsInfo = new Hashtable<Long, Vector<String>>();

        public ShotsPerformance(){
            this.labelText.add("SHOT");
        }

        public void add(final long shot, final String phase, final String duration) {
            if(phase == null) return;
            final Long key = new Long(shot);
            if(!this.labelText.contains(phase)) this.labelText.add(phase);
            if(!this.shotsInfo.containsKey(key)){
                this.phaseDuration = new Vector<String>();
                this.shotsInfo.put(key, this.phaseDuration);
            }else{
                this.phaseDuration = this.shotsInfo.get(key);
            }
            this.phaseDuration.add(duration);
        }

        public void print() {
            for(int i = 0; i < this.labelText.size(); i++)
                System.out.print("\t" + this.labelText.elementAt(i));
            System.out.println();
            final Enumeration shots = this.shotsInfo.keys();
            final Enumeration e = this.shotsInfo.elements();
            while(e.hasMoreElements()){
                final String shotStr = "\t" + shots.nextElement();
                final Vector phDur = (Vector)e.nextElement();
                for(int i = 0; i < phDur.size(); i++){
                    if((i % (this.labelText.size() - 1)) == 0){
                        System.out.println(shotStr);
                    }
                    System.out.print("\t" + phDur.elementAt(i));
                }
            }
        }

        public void printHeader(final PrintStream out) {
            for(int i = 0; i < this.labelText.size(); i++)
                out.print("\t" + this.labelText.elementAt(i));
            out.println();
        }

        public void printValues(final PrintStream out) {
            final Vector phDur = this.shotsInfo.get(new Long(jDispatchMonitor.this.shot));
            for(int i = 0; i < phDur.size(); i++){
                if((i % (this.labelText.size() - 1)) == 0){
                    out.println("\t" + jDispatchMonitor.this.shot);
                }
                out.print("\t" + phDur.elementAt(i));
            }
        }

        public void saveTimePhaseExecution() {
            final String fname = "PulsesPhasesEsecutionTime.log";
            final File f = new File(fname);
            try{
                if(this.log == null){
                    if(f.exists()){
                        this.log = new PrintStream(new FileOutputStream(fname, true));
                        this.log.println();
                        return;
                    }else{
                        if(this.labelText.size() > 1){
                            this.log = new PrintStream(new FileOutputStream(fname, true));
                            this.printHeader(this.log);
                        }else return;
                    }
                }
                this.printValues(this.log);
            }catch(final Exception exc){
                System.out.println("ShotsPerformance : " + exc);
            }
        }
    }
    class ShowMessage implements Runnable{
        String msg;

        public ShowMessage(final String msg){
            this.msg = msg;
        }

        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, this.msg, "alert", JOptionPane.ERROR_MESSAGE);
        }
    }// End ShowMessage class
    class ToolTipJList extends JList{
        /**
         *
         */
        private static final long serialVersionUID = 5293177113054224034L;

        public ToolTipJList(final ListModel lm){
            super(lm);
            this.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(final MouseEvent e) {
                    if(e.getClickCount() == 2){
                        final int index = ToolTipJList.this.locationToIndex(e.getPoint());
                        final ListModel dlm = ToolTipJList.this.getModel();
                        final MdsMonitorEvent item = (MdsMonitorEvent)dlm.getElementAt(index);
                        ToolTipJList.this.ensureIndexIsVisible(index);
                        if((item.ret_status & 1) == 0) JOptionPane.showMessageDialog(null, item.error_message, "alert", JOptionPane.ERROR_MESSAGE);
                        else{
                            if(item.error_message != null && item.error_message.length() != 0) JOptionPane.showMessageDialog(null, item.error_message, "alert", JOptionPane.WARNING_MESSAGE);
                        }
                        // System.out.println("Double clicked on " + item);
                    }
                }
            });
        }

        @Override
        public String getToolTipText(final MouseEvent event) {
            String out = null;
            final int st = this.getFirstVisibleIndex();
            int end = this.getLastVisibleIndex();
            if(end == -1) end = ((DefaultListModel)this.getModel()).size() - 1;
            if(st == -1 || end == -1) return null;
            Rectangle r;
            for(int i = st; i <= end; i++){
                r = this.getCellBounds(i, i);
                if(r.contains(event.getPoint())){
                    final MdsMonitorEvent me = (MdsMonitorEvent)this.getModel().getElementAt(i);
                    out = me.error_message;
                    break;
                }
            }
            return out;
        }
    } // End ToolTipJList class
    class UpdateHandler implements Runnable{
        int   idx;
        JList list;

        UpdateHandler(final JList list, final int idx){
            this.list = list;
            this.idx = idx;
        }

        @Override
        public void run() {
            jDispatchMonitor.this.curr_list = this.list;
            jDispatchMonitor.this.item_idx = this.idx;
            if(jDispatchMonitor.this.auto_scroll && jDispatchMonitor.this.show_phase == jDispatchMonitor.this.curr_phase){
                if(jDispatchMonitor.this.item_idx == -1) jDispatchMonitor.this.item_idx = jDispatchMonitor.this.curr_list.getModel().getSize() - 1;
                jDispatchMonitor.this.curr_list.ensureIndexIsVisible(jDispatchMonitor.this.item_idx);
            }
            this.list.repaint();
        }
    }
    class UpdateList implements Runnable{
        MdsMonitorEvent me;

        UpdateList(final MdsMonitorEvent me){
            this.me = me;
        }

        @Override
        public void run() {
            int status = 1;
            // System.out.println("-------------- Action : "+me);
            switch(this.me.mode){
                case MdsMonitorEvent.MonitorStartPhase:
                    jDispatchMonitor.this.startPhase(this.me);
                    break;
                case MdsMonitorEvent.MonitorEndPhase:
                    jDispatchMonitor.this.endPhase(this.me);
                    break;
                case MdsMonitorEvent.MonitorServerConnected:
                    // System.out.println(me);
                    jDispatchMonitor.this.serversInfoPanel.updateServerState(this.me.server_address, true);
                    break;
                case MdsMonitorEvent.MonitorServerDisconnected:
                    final ServerInfo si = jDispatchMonitor.this.serversInfo.get(this.me.server_address);
                    String msg;
                    if(si != null){
                        final String serverClass = si.getClassName();
                        msg = "Disconnect from server class " + serverClass + " address :" + this.me.server_address;
                    }else{
                        msg = "Disconnect from server class address :" + this.me.server_address;
                    }
                    JOptionPane.showMessageDialog(jDispatchMonitor.this, msg, "Alert", JOptionPane.ERROR_MESSAGE);
                    jDispatchMonitor.this.serversInfoPanel.updateServerState(this.me.server_address, false);
                    break;
                case MdsMonitorEvent.MonitorBuildBegin:
                    jDispatchMonitor.this.resetAll(this.me);
                case MdsMonitorEvent.MonitorBuild:
                case MdsMonitorEvent.MonitorBuildEnd:
                    jDispatchMonitor.this.build_list.addElement(this.me);
                    break;
                case MdsMonitorEvent.MonitorCheckin:
                    break;
                case MdsMonitorEvent.MonitorDone:
                    status = this.me.ret_status;
                    Hashtable<Integer, MdsMonitorEvent> actions = null;
                    if(jDispatchMonitor.this.phase_failed.containsKey(new Integer(this.me.phase))){
                        actions = jDispatchMonitor.this.phase_failed.get(new Integer(this.me.phase));
                        if(actions.containsKey(new Integer(this.me.nid))){
                            final MdsMonitorEvent me_failed = actions.get(new Integer(this.me.nid));
                            if(this.me.error_message == null || this.me.error_message.indexOf("SS-W-NOMSG") != -1) this.me.error_message = me_failed.error_message;
                            else this.me.error_message += " " + me_failed.error_message;
                            me_failed.error_message = new String(this.me.error_message);
                        }
                    }
                case MdsMonitorEvent.MonitorDispatched:
                case MdsMonitorEvent.MonitorDoing:
                    synchronized(jDispatchMonitor.this.executing_list){
                        SwingUtilities.invokeLater(new Runnable(){
                            @Override
                            public void run() {
                                jDispatchMonitor.this.serversInfoPanel.updateServersInfoAction(UpdateList.this.me);
                            }
                        });
                        final int idx = jDispatchMonitor.this.getIndex(this.me, jDispatchMonitor.this.executing_list.size());
                        if(idx == jDispatchMonitor.ACTION_NOT_FOUND){
                            JOptionPane.showMessageDialog(null, "Invalid message received", "Alert", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        // NOTE: executing_list may be empty in case an error message
                        // has been received before a dispatched message
                        if(idx == -1 || jDispatchMonitor.this.executing_list.size() == 0) jDispatchMonitor.this.executing_list.addElement(this.me);
                        else jDispatchMonitor.this.executing_list.set(idx, this.me);
                        if((status & 1) == 0){
                            jDispatchMonitor.this.failed_list.addElement(this.me);
                            jDispatchMonitor.this.showUpdateAction(jDispatchMonitor.this.failedList, -1);
                        }
                        jDispatchMonitor.this.showUpdateAction(jDispatchMonitor.this.executingList, idx);
                    }
                    break;
            }
            jDispatchMonitor.this.counter(this.me);
        }
    }// End UpdateList class
    public static final int   ACTION_NOT_FOUND = -2;
    static Font               disp_font        = new Font("Platino Linotype", Font.ITALIC, 12);
    static Font               doing_font       = new Font("Platino Linotype", Font.ITALIC | Font.BOLD, 12);
    static Font               done_failed_font = new Font("Platino Linotype", Font.ITALIC, 12);
    static Font               done_font        = new Font("Platino Linotype", Font.PLAIN, 12);
    private static final long serialVersionUID = 5213140059293512936L;

    public static void main(final String[] args) {
        jDispatchMonitor dm;
        int i;
        String experiment = null;
        String monitor_server = null;
        if(args != null && args.length > 3){
            System.out.println("jDispatchMonitor [monitor_server] [-e experiment ] \n");
            System.exit(0);
        }
        if(args.length > 0){
            for(i = 0; i < args.length && !args[i].equals("-e"); i++);
            if(i < args.length){
                experiment = args[i + 1];
                if(args.length >= 3) monitor_server = ((i == 0) ? args[i + 2] : args[0]);
            }else{
                monitor_server = args[0];
            }
        }
        System.out.println("jDispatchMonitor " + monitor_server + " " + experiment);
        // MdsHelper.initialization(experiment);
        dm = new jDispatchMonitor(monitor_server, experiment);
        /*
        if(args != null &&  args.length > 0 && args[0].length() != 0)
            dm = new jDispatchMonitor(args[0]);
        else
            dm = new jDispatchMonitor();
        */
        dm.pack();
        dm.setSize(600, 700);
        dm.setVisible(true);
    }
    boolean                                                 auto_scroll      = true;
    JInternalFrame                                          build, executing, failed;
    DefaultListModel                                        build_list       = new DefaultListModel();
    JList                                                   buildList, executingList, failedList;
    JList                                                   curr_list;
    int                                                     curr_phase       = -1;
    JDesktopPane                                            desktop          = null;
    int                                                     disp_count       = 0, doing_count = 0, done_count = 0, failed_count = 0, total_count;
    mdsConnection                                           dispatcher       = null;
    JMenuItem                                               do_abort;
    JPopupMenu                                              do_command_menu  = new JPopupMenu();
    JMenuItem                                               do_redispatch;
    ErrorMgr                                                error_mgr;
    DefaultListModel                                        executing_list   = new DefaultListModel();
    String                                                  experiment;
    DefaultListModel                                        failed_list      = new DefaultListModel();
    int                                                     info_port;
    int                                                     item_idx;
    MdsServer                                               mds_server       = null;
    String                                                  monitor_server;
    private int                                             num_window       = 0;
    Hashtable<Integer, Hashtable<Integer, MdsMonitorEvent>> phase_failed     = new Hashtable<Integer, Hashtable<Integer, MdsMonitorEvent>>();
    ButtonGroup                                             phase_group      = new ButtonGroup();
    Hashtable<Integer, Hashtable<Integer, MdsMonitorEvent>> phase_hash       = new Hashtable<Integer, Hashtable<Integer, MdsMonitorEvent>>();
    JMenu                                                   phase_m          = new JMenu("Phase");
    Hashtable<Integer, Hashtable<Integer, MdsMonitorEvent>> phase_name       = new Hashtable<Integer, Hashtable<Integer, MdsMonitorEvent>>();
    Hashtable<String, ServerInfo>                           serversInfo      = new Hashtable<String, ServerInfo>();
    ServersInfoPanel                                        serversInfoPanel = null;
    long                                                    shot;
    ShotsPerformance                                        shotsPerformance = new ShotsPerformance();
    int                                                     show_phase       = -1;
    long                                                    startPhaseTime   = 0;
    JLabel                                                  total_actions_l, dispatched_l, doing_l, done_l, failed_l, exp_l, shot_l, phase_l;
    String                                                  tree;

    public jDispatchMonitor(){
        this(null, null);
    }

    public jDispatchMonitor(final String monitor_server, final String experiment){
        this.experiment = experiment;
        this.addWindowListener(new java.awt.event.WindowAdapter(){
            @Override
            public void windowClosing(final java.awt.event.WindowEvent e) {
                jDispatchMonitor.this.shotsPerformance.saveTimePhaseExecution();
            }
        });
        this.addKeyListener(new java.awt.event.KeyAdapter(){
            @Override
            public void keyTyped(final java.awt.event.KeyEvent e) {
                if(e.isAltDown() && e.isShiftDown() && e.getKeyChar() == 'P') jDispatchMonitor.this.shotsPerformance.print();
            }
        });
        final JMenuBar mb = new JMenuBar();
        this.setJMenuBar(mb);
        this.setWindowTitle();
        this.monitor_server = monitor_server;
        final Properties properties = MdsHelper.initialization(experiment);
        if(properties == null) System.exit(0);
        int error_port = -1;
        try{
            error_port = Integer.parseInt(properties.getProperty("jDispatcher.error_port"));
        }catch(final Exception exc){
            // System.out.println("Cannot read error port");
        }
        if(error_port > 0){
            this.error_mgr = new ErrorMgr(error_port);
            this.error_mgr.start();
        }
        this.info_port = 0;
        try{
            this.info_port = Integer.parseInt(properties.getProperty("jDispatcher.info_port"));
        }catch(final Exception exc){
            System.out.println("Cannot read info port");
        }
        this.loadServersInfo(properties);
        this.serversInfoPanel = new ServersInfoPanel();
        this.serversInfoPanel.setServersInfo(this.serversInfo);
        try{
            if(monitor_server != null && monitor_server.length() != 0) this.openConnection(monitor_server);
        }catch(final IOException e){
            System.out.println(e.getMessage());
        }
        final JMenu file = new JMenu("File");
        final JMenuItem open = new JMenuItem("Open Connection ...");
        open.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jDispatchMonitor.this.openConnection();
            }
        });
        file.add(open);
        final JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jDispatchMonitor.this.exitApplication();
            }
        });
        file.add(exit);
        final JMenu view = new JMenu("View");
        final JCheckBoxMenuItem build_cb = new JCheckBoxMenuItem("Build", true);
        build_cb.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jDispatchMonitor.this.num_window += jDispatchMonitor.this.build.isShowing() ? -1 : 1;
                jDispatchMonitor.this.build.setVisible(!jDispatchMonitor.this.build.isShowing());
            }
        });
        view.add(build_cb);
        final JCheckBoxMenuItem executing_cb = new JCheckBoxMenuItem("Executing", true);
        executing_cb.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jDispatchMonitor.this.num_window += jDispatchMonitor.this.executing.isShowing() ? -1 : 1;
                jDispatchMonitor.this.executing.setVisible(!jDispatchMonitor.this.executing.isShowing());
            }
        });
        view.add(executing_cb);
        final JCheckBoxMenuItem failed_cb = new JCheckBoxMenuItem("Failed", true);
        failed_cb.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jDispatchMonitor.this.num_window += jDispatchMonitor.this.failed.isShowing() ? -1 : 1;
                jDispatchMonitor.this.failed.setVisible(!jDispatchMonitor.this.failed.isShowing());
            }
        });
        view.add(failed_cb);
        view.add(new JSeparator());
        final JCheckBoxMenuItem auto_scroll_cb = new JCheckBoxMenuItem("Auto Scroll", true);
        auto_scroll_cb.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jDispatchMonitor.this.auto_scroll = !jDispatchMonitor.this.auto_scroll;
            }
        });
        view.add(auto_scroll_cb);
        final JMenu info_m = new JMenu("Info");
        /*
        JMenuItem showServer_cb = new JMenuItem("Show Server");
        showServer_cb.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                   showServers();
                }
            }
        );
        info_m.add(showServer_cb);
        */
        final JMenuItem statistics_cb = new JMenuItem("Statistics");
        statistics_cb.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                jDispatchMonitor.this.shotsPerformance.print();
            }
        });
        info_m.add(statistics_cb);
        mb.add(file);
        mb.add(view);
        mb.add(this.phase_m);
        // mb.add(info_m);
        final JPanel p0 = new JPanel();
        p0.setBorder(BorderFactory.createRaisedBevelBorder());
        final Box box = new Box(BoxLayout.Y_AXIS);
        final JPanel p = new JPanel();
        p.add(new JLabel("Active actions"));
        p.add((this.total_actions_l = new JLabel("    0")));
        p.add(new JLabel("Dispatched"));
        p.add((this.dispatched_l = new JLabel("    0")));
        p.add(new JLabel("Doing"));
        p.add((this.doing_l = new JLabel("    0")));
        p.add(new JLabel("Done"));
        p.add((this.done_l = new JLabel("    0")));
        p.add(new JLabel("Failed"));
        p.add((this.failed_l = new JLabel("    0")));
        JPanel p1 = new JPanel();
        p1.add((this.exp_l = new JLabel("Experiment:          ")));
        p1.add((this.shot_l = new JLabel("Shot:        ")));
        p1.add((this.phase_l = new JLabel("Phase:        ")));
        box.add(p1);
        box.add(p);
        p0.add(box);
        this.getContentPane().add(p0, BorderLayout.NORTH);
        this.desktop = new JDesktopPane(){
            private void positionWindow() {
                if(jDispatchMonitor.this.num_window == 0) return;
                final Dimension dim = jDispatchMonitor.this.desktop.getSize();
                dim.height = dim.height / jDispatchMonitor.this.num_window;
                int y = 0;
                if(jDispatchMonitor.this.build.isVisible()){
                    jDispatchMonitor.this.build.setBounds(0, y, dim.width, dim.height);
                    y += dim.height;
                }
                if(jDispatchMonitor.this.executing.isVisible()){
                    jDispatchMonitor.this.executing.setBounds(0, y, dim.width, dim.height);
                    y += dim.height;
                }
                if(jDispatchMonitor.this.failed.isVisible()){
                    jDispatchMonitor.this.failed.setBounds(0, y, dim.width, dim.height);
                }
            }

            @Override
            public void setBounds(final int x, final int y, final int width, final int height) {
                super.setBounds(x, y, width, height);
                this.positionWindow();
            }
        };
        this.buildList = new JList(this.build_list);
        this.buildList.setCellRenderer(new BuildCellRenderer());
        final JScrollPane sp_build = new JScrollPane();
        sp_build.getViewport().setView(this.buildList);
        this.executingList = new ToolTipJList(this.executing_list);
        this.executingList.setToolTipText("");
        this.executingList.setCellRenderer(new ExecutingCellRenderer());
        final JScrollPane sp_executing = new JScrollPane();
        sp_executing.getViewport().setView(this.executingList);
        this.do_redispatch = new JMenuItem("Redispatch");
        this.do_redispatch.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try{
                    final Object ob[] = jDispatchMonitor.this.executingList.getSelectedValues();
                    if(ob != null && ob.length != 0){
                        for(final Object element : ob)
                            jDispatchMonitor.this.redispatch((MdsMonitorEvent)element);
                    }
                }catch(final Exception exc){
                    JOptionPane.showMessageDialog(null, "Error dispatching action(s) : " + exc, "Alert", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        this.do_command_menu.add(this.do_redispatch);
        this.do_abort = new JMenuItem("Abort");
        this.do_abort.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                try{
                    final Object ob[] = jDispatchMonitor.this.executingList.getSelectedValues();
                    if(ob != null && ob.length == 1){
                        jDispatchMonitor.this.abortAction((MdsMonitorEvent)ob[0]);
                    }
                }catch(final Exception exc){
                    JOptionPane.showMessageDialog(null, "Error Aborting Action", "Alert", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        this.do_command_menu.add(this.do_abort);
        this.executingList.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(final MouseEvent e) {
                if(MdsHelper.getDispatcher() != null && (e.getModifiers() & Event.META_MASK) != 0){
                    final Object ob[] = jDispatchMonitor.this.executingList.getSelectedValues();
                    if(ob == null || ob.length == 0){
                        if(jDispatchMonitor.this.executingList.getModel().getSize() != 0) JOptionPane.showMessageDialog(null, "Please select the action(s) to re-dispatch or abort", "Warning", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if(ob.length == 1 && ((MdsMonitorEvent)ob[0]).mode == MdsMonitorEvent.MonitorDoing){
                        jDispatchMonitor.this.do_redispatch.setEnabled(false);
                        jDispatchMonitor.this.do_abort.setEnabled(true);
                    }else{
                        jDispatchMonitor.this.do_redispatch.setEnabled(true);
                        jDispatchMonitor.this.do_abort.setEnabled(false);
                    }
                    final int x = e.getX();
                    final int y = e.getY();
                    jDispatchMonitor.this.do_command_menu.show(jDispatchMonitor.this.executingList, x, y);
                }
            }
        });
        this.failedList = new ToolTipJList(this.failed_list);
        this.failedList.setToolTipText("");
        this.failedList.setCellRenderer(new FailedCellRenderer());
        final JScrollPane sp_failed = new JScrollPane();
        sp_failed.getViewport().setView(this.failedList);
        this.build = this.createInternalFrame("Build Table", sp_build);
        this.executing = this.createInternalFrame("Executing", sp_executing);
        this.failed = this.createInternalFrame("Failed", sp_failed);
        final JTabbedPane dispatchMonitorPane = new JTabbedPane();
        // this.getContentPane().add(desktop, BorderLayout.CENTER);
        this.getContentPane().add(dispatchMonitorPane, BorderLayout.CENTER);
        dispatchMonitorPane.addTab("Actions Monitor", this.desktop);
        /*
        serversInfoPanel.addComponentListener(new ComponentAdapter()
        {
            public void componentShown(ComponentEvent e)
            {
                showServers();
            }
        });
        */
        dispatchMonitorPane.addTab("Servers Info", this.serversInfoPanel);
        p1 = new JPanel();
        p1.setBackground(Color.white);
        p1.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel l1 = new JLabel("Dispatched           ");
        l1.setForeground(Color.lightGray);
        p1.add(l1);
        l1 = new JLabel("Doing               ");
        l1.setForeground(Color.blue);
        p1.add(l1);
        l1 = new JLabel("Done                ");
        l1.setForeground(Color.green);
        p1.add(l1);
        l1 = new JLabel("Failed              ");
        l1.setForeground(Color.red);
        p1.add(l1);
        this.getContentPane().add(p1, BorderLayout.SOUTH);
    }

    private void abortAction(final MdsMonitorEvent me) throws IOException {
        if(me.mode == MdsMonitorEvent.MonitorDoing) this.doCommand("ABORT", me);
    }

    private boolean addPhaseItem(final int phase, final String phase_st) {
        for(final Enumeration e = this.phase_group.getElements(); e.hasMoreElements();){
            final JCheckBoxMenuItem c = (JCheckBoxMenuItem)e.nextElement();
            if(c.getText().equals("Phase_" + phase)) return false;
        }
        final JCheckBoxMenuItem phase_cb = new JCheckBoxMenuItem(phase_st);
        phase_cb.setActionCommand("" + phase);
        phase_cb.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                final String cmd = ((JCheckBoxMenuItem)e.getSource()).getActionCommand();
                jDispatchMonitor.this.show_phase = Integer.parseInt(cmd);
                jDispatchMonitor.this.showPhase(cmd);
            }
        });
        this.phase_group.add(phase_cb);
        this.phase_m.add(phase_cb);
        return true;
    }

    private void collectServersInfo() {
        try{
            final String address = new StringTokenizer(this.monitor_server).nextToken(":");
            if(!this.serversInfoPanel.checkInfoServer(address, this.info_port)){
                this.serversInfoPanel.loadServerState(address, this.info_port);
            }else this.serversInfoPanel.updateServersState();
        }catch(final Exception exc){
            final ShowMessage alert = new ShowMessage(exc.getMessage());
            SwingUtilities.invokeLater(alert);
        }
    }

    private void counter(final MdsMonitorEvent me) {
        switch(me.mode){
            case MdsMonitorEvent.MonitorBuildBegin:
                this.total_count++;
                break;
            case MdsMonitorEvent.MonitorBuild:
            case MdsMonitorEvent.MonitorBuildEnd:
                this.total_count++;
                break;
            case MdsMonitorEvent.MonitorDispatched:
                this.disp_count++;
                break;
            case MdsMonitorEvent.MonitorDoing:
                this.doing_count++;
                break;
            case MdsMonitorEvent.MonitorDone:
                this.doing_count--;
                this.done_count++;
                if((me.ret_status & 1) == 0) this.failed_count++;
                break;
        }
        this.dispatched_l.setText(" " + this.disp_count);
        this.doing_l.setText(" " + this.doing_count);
        this.done_l.setText(" " + this.done_count);
        this.total_actions_l.setText(" " + this.total_count);
        this.failed_l.setText(" " + this.failed_count);
    }

    /**
     * Create an internal frame
     */
    public JInternalFrame createInternalFrame(final String title, final Container panel) {
        final JInternalFrame jif = new JInternalFrame();
        jif.setTitle(title);
        // set properties
        jif.setClosable(false);
        jif.setMaximizable(true);
        jif.setIconifiable(false);
        jif.setResizable(true);
        jif.setContentPane(panel);
        this.desktop.add(jif, 1);
        jif.setVisible(true);
        this.num_window++;
        return jif;
    }

    private void doCommand(final String command, final MdsMonitorEvent me) throws IOException {
        if(this.dispatcher == null){
            if(this.monitor_server == null) return;
            final StringTokenizer st = new StringTokenizer(this.monitor_server, ":");
            this.dispatcher = new mdsConnection(st.nextToken() + ":" + MdsHelper.getDispatcherPort());
            this.dispatcher.ConnectToMds(false);
        }
        final Descriptor reply = this.dispatcher.mdsValue(command + " " + me.nid + " " + MdsHelper.toPhaseName(me.phase));
    }

    private void endPhase(final MdsMonitorEvent me) {
        final long phaseDuration = System.currentTimeMillis() - this.startPhaseTime;
        System.out.println(" ---- End  phase : " + MdsHelper.toPhaseName(me.phase) + " duration : " + MdsMonitorEvent.msecToString(phaseDuration));
        this.phase_m.setEnabled(true);
        this.shotsPerformance.add(this.shot, MdsHelper.toPhaseName(me.phase), MdsMonitorEvent.msecToString(phaseDuration));
    }

    /*    private  void showUpdateAction(JList list, int idx)
    {
        curr_list = list;
        item_idx = idx;

        if(auto_scroll && show_phase == -1)
        {
            if(item_idx == -1)
               item_idx = curr_list.getModel().getSize() - 1;
            curr_list.ensureIndexIsVisible(item_idx);
        }
    }
    */
    public void exitApplication() {
        this.shotsPerformance.saveTimePhaseExecution();
        System.exit(0);
    }

    private int getIndex(final MdsMonitorEvent me, int idx) {
        Hashtable<Integer, MdsMonitorEvent> actions = null;
        int ph = -1;
        if(this.show_phase != this.curr_phase) ph = me.phase = this.show_phase;
        else ph = me.phase;
        if(this.phase_hash.containsKey(new Integer(ph))) actions = this.phase_hash.get(new Integer(ph));
        else{
            final String phase_st = MdsHelper.toPhaseName(me.phase);
            final boolean new_phase = this.addPhaseItem(me.phase, phase_st);
            /*
            executing_list.removeAllElements();
            phase_l.setText("Phase: "+phase_st);
            curr_phase = show_phase = me.phase;
            */
            actions = new Hashtable<Integer, MdsMonitorEvent>();
            this.phase_hash.put(new Integer(me.phase), actions);
            idx = 0;
        }
        if(actions == null) return jDispatchMonitor.ACTION_NOT_FOUND;
        if(actions.containsKey(new Integer(me.nid))){
            final MdsMonitorEvent e = actions.get(new Integer(me.nid));
            // System.out.println("Replace action nid "+ e.nid + " " + e.error_message);
            // Check if the current MdsMonitorActionEvenr is added
            // by ErrorMgr thread
            if(e.tree == null){
                e.tree = me.tree;
                e.shot = me.shot;
                e.name = me.name;
                e.on = me.on;
                e.mode = me.mode;
                e.server = me.server;
            }
            if(me.mode == MdsMonitorEvent.MonitorDone){
                // If there is an error message in the stored MdsMonitorEvent
                // it must be appended at the error message field of the
                // current MdsMonitorEvent
                if(e.error_message != null && e.error_message.indexOf("SS-W-NOMSG") == -1){
                    // me.error_message += " " + e.error_message;
                    e.error_message = new String(e.error_message + me.error_message);
                }else{
                    e.error_message = new String(me.error_message);
                }
                final long start_date = e.date.getTime();
                final long end_date = me.date.getTime();
                e.execution_time = end_date - start_date;
                me.execution_time = e.execution_time;
            }
            e.ret_status = me.ret_status;
            e.mode = me.mode;
            e.date = me.date;
            return e.jobid;
        }else{
            me.jobid = idx;
            actions.put(new Integer(me.nid), me);
            return -1;
        }
    }

    public void loadServersInfo(final Properties properties) {
        int i = 1;
        while(true){
            final String server_class = properties.getProperty("jDispatcher.server_" + i + ".class");
            if(server_class == null) break;
            final String server_ip = properties.getProperty("jDispatcher.server_" + i + ".address").trim();
            if(server_ip == null) break;
            final String server_subtree = properties.getProperty("jDispatcher.server_" + i + ".subtree");
            boolean useJavaServer;
            try{
                useJavaServer = properties.getProperty("jDispatcher.server_" + i + ".use_jserver").equals("true");
            }catch(final Exception exc){
                useJavaServer = true;
            }
            int watchdogPort;
            try{
                watchdogPort = Integer.parseInt(properties.getProperty("jDispatcher.server_" + i + ".watchdog_port"));
            }catch(final Exception exc){
                watchdogPort = -1;
            }
            final String startScript = properties.getProperty("jDispatcher.server_" + i + ".start_script");
            final String stopScript = properties.getProperty("jDispatcher.server_" + i + ".stop_script");
            final ServerInfo srvInfo = new ServerInfo(server_class, server_ip, server_subtree, useJavaServer, watchdogPort, startScript, stopScript);
            this.serversInfo.put(server_ip, srvInfo);
            i++;
        }
    }

    public void openConnection() {
        try{
            if(this.mds_server != null){
                if(this.mds_server.connected){
                    final Object[] options = {"DISCONNECT", "CANCEL"};
                    final int val = JOptionPane.showOptionDialog(null, "Dispatch monitor is already connected to " + this.mds_server.getProvider(), "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if(val == 0){
                        this.mds_server.shutdown();
                        this.mds_server = null;
                        this.setWindowTitle();
                    }else return;
                }else this.mds_server.shutdown();
            }
            final JComboBox cb = new JComboBox();
            cb.setEditable(true);
            // monitor_server = JOptionPane.showInputDialog("Dispatch monitor server");
            this.monitor_server = (String)JOptionPane.showInputDialog(null, "Dispatch monitor server ip:port address", "Connection", JOptionPane.QUESTION_MESSAGE, null, null, this.monitor_server);
            if(this.monitor_server == null) return;
            this.openConnection(this.monitor_server);
        }catch(final IOException exc){
            JOptionPane.showMessageDialog(jDispatchMonitor.this, exc.getMessage(), "alert", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openConnection(final String mon_srv) throws IOException {
        if(mon_srv == null) throw(new IOException("Can't connect to null address"));
        this.mds_server = new MdsServer(mon_srv, true, -1);
        this.mds_server.addMdsServerListener(jDispatchMonitor.this);
        this.mds_server.addConnectionListener(jDispatchMonitor.this);
        final Descriptor reply = this.mds_server.monitorCheckin();
        this.setWindowTitle();
        this.dispatcher = null;
        this.collectServersInfo();
    }

    @Override
    public synchronized void processConnectionEvent(final ConnectionEvent e) {
        if(e.getSource() == this.mds_server){
            if(e.getID() == ConnectionEvent.LOST_CONNECTION){
                // do the following on the gui thread
                final ShowMessage alert = new ShowMessage(e.getInfo());
                SwingUtilities.invokeLater(alert);
                this.setWindowTitle();
                this.mds_server = null;
                /*               try
                {
                    if(dispatcher != null);
                        dispatcher.shutdown();
                    dispatcher = null;
                } catch (Exception exc) {dispatcher = null;}
                    return;
                */ }
        }
        if(e.getSource() == this.dispatcher){
            if(e.getID() == ConnectionEvent.LOST_CONNECTION) this.dispatcher = null;
        }
    }

    @Override
    public synchronized void processMdsServerEvent(final MdsServerEvent e) {
        if(e instanceof MdsMonitorEvent){
            final MdsMonitorEvent me = (MdsMonitorEvent)e;
            final UpdateList ul = new UpdateList(me);
            try{
                // SwingUtilities.invokeAndWait(ul);
                SwingUtilities.invokeLater(ul);
            }catch(final Exception exc){
                System.out.println("processMdsServerEvent " + exc);
            }
        }
    }

    private void redispatch(final MdsMonitorEvent me) throws IOException {
        if(me.mode == MdsMonitorEvent.MonitorDone) this.doCommand("REDISPATCH", me);
    }

    private void resetAll(final MdsMonitorEvent me) {
        this.total_count = 0;
        this.doing_count = 0;
        this.done_count = 0;
        this.failed_count = 0;
        this.disp_count = 0;
        this.phase_m.removeAll();
        this.build_list.removeAllElements();
        this.failed_list.removeAllElements();
        this.executing_list.removeAllElements();
        this.phase_hash.clear();
        this.phase_name.clear();
        this.phase_failed.clear();
        if(me != null){
            this.shotsPerformance.saveTimePhaseExecution();
            this.exp_l.setText("Experiment: " + me.tree);
            this.shot_l.setText("Shot: " + me.shot);
            this.phase_l.setText("Phase: " + MdsHelper.toPhaseName(me.phase));
            this.tree = new String(me.tree);
            this.shot = me.shot;
        }
    }

    private void setWindowTitle() {
        if(this.mds_server != null && this.mds_server.connected) this.setTitle("jDispatchMonitor - Connected to " + this.mds_server.getProvider() + " receive on port " + this.mds_server.rcv_port);
        else this.setTitle("jDispatchMonitor - Not Connected");
    }

    private void showPhase(final String phase) {
        Hashtable<Integer, MdsMonitorEvent> actions;
        MdsMonitorEvent me;
        // int phase_id = Integer.parseInt(phase);
        if(this.phase_hash.containsKey(new Integer(phase))){
            this.show_phase = Integer.parseInt(phase);
            this.executing_list.removeAllElements();
            this.failed_list.removeAllElements();
            actions = this.phase_hash.get(new Integer(phase));
            this.executing_list.setSize(actions.size());
            int s_done = 0, s_failed = 0;
            for(final Enumeration e = actions.elements(); e.hasMoreElements();){
                me = (MdsMonitorEvent)e.nextElement();
                this.executing_list.set(me.jobid, me);
                s_done++;
                if((me.ret_status & 1) == 0){
                    this.failed_list.addElement(me);
                    s_failed++;
                }
            }
            if(this.curr_phase != this.show_phase){
                this.phase_l.setText("Phase : " + MdsHelper.toPhaseName(this.show_phase));
                this.executing.setTitle("Executed in " + MdsHelper.toPhaseName(this.show_phase) + " phase Done :" + s_done + " Failed: " + s_failed);
                this.failed.setTitle("Failed in " + MdsHelper.toPhaseName(this.show_phase));
            }else{
                this.phase_l.setText("Phase : " + MdsHelper.toPhaseName(this.curr_phase));
                this.executing.setTitle("Executing");
                this.failed.setTitle("Failed");
                this.show_phase = this.curr_phase;
            }
        }
    }

    private void showUpdateAction(final JList list, final int idx) {
        SwingUtilities.invokeLater(new UpdateHandler(list, idx));
    }

    private void startPhase(final MdsMonitorEvent me) {
        this.startPhaseTime = System.currentTimeMillis();
        System.out.println("-------------- Start phase : " + me);
        final String phase_st = MdsHelper.toPhaseName(me.phase);
        this.executing_list.removeAllElements();
        this.phase_l.setText("Phase: " + phase_st);
        this.phase_m.setEnabled(false);
        this.curr_phase = this.show_phase = me.phase;
    }
}
